/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupEntity
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionEntity
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerEntity
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.messaging.MessagingBroker
import dev.gradienttim.gradeway.messaging.NetworkPayload
import dev.gradienttim.gradeway.messaging.payloads.*
import dev.gradienttim.gradeway.throwables.driver.DriverBlankIdentifierThrowable
import dev.gradienttim.gradeway.throwables.driver.DriverNotFoundThrowable
import dev.gradienttim.gradeway.throwables.driver.DriverUnsupportedAdapterThrowable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.exposed.v1.dao.EntityChange
import org.jetbrains.exposed.v1.dao.EntityChangeType
import org.jetbrains.exposed.v1.dao.EntityHook
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class CommonMessagingManager(val gradeway: CommonGradeway) : MessagingManager {
    private val serverId: String = UUID.randomUUID().toString()
    private val listeners = CopyOnWriteArrayList<(MessagingPayload) -> Unit>()

    private var broker: MessagingBroker? = null
    private var entityHookListener: ((EntityChange) -> Unit)? = null

    override fun load(): Either<Throwable, Unit> = either {
        try {
            // Registered unconditionally, regardless of whether messaging is enabled or a broker
            // ends up connecting: publish() already dispatches to local listeners synchronously
            // without needing a broker, so whole-entity change sync (role rename/delete/etc.) must
            // work on a single, non-clustered server too, not just when a broker is connected.
            if (entityHookListener == null) {
                entityHookListener = EntityHook.subscribe { change -> handleEntityChange(change) }
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun unload(): Either<Throwable, Unit> = either {
        try {
            entityHookListener?.let { EntityHook.unsubscribe(it) }
            entityHookListener = null
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun enable(): Either<Throwable, Unit> = either {
        val config = gradeway.configs.config.messaging
        if (!config.enabled) {
            return@either
        }

        val driverId = config.driver
        if (driverId.isBlank()) {
            raise(DriverBlankIdentifierThrowable())
        }

        val messagingDriver = gradeway.drivers.findDriver(driverId, DriverType.MESSAGING)
            ?: raise(DriverNotFoundThrowable(id = driverId))

        if (messagingDriver !is MessagingAdapter) {
            raise(DriverUnsupportedAdapterThrowable(id = driverId, adapter = MessagingAdapter::class))
        }

        try {
            val newBroker = messagingDriver.createMessagingBroker(gradeway.environment)
            newBroker.open()

            if (!newBroker.subscribe(MessagingConstants.SYNC_CHANNEL) { bytes -> handleIncoming(bytes) }) {
                gradeway.logger.warn(
                    "Failed to subscribe to the '${MessagingConstants.SYNC_CHANNEL}' messaging channel.",
                )
            }

            broker = newBroker
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun disable(): Either<Throwable, Unit> = either {
        try {
            broker?.close()
            broker = null
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun reload(): Either<Throwable, Unit> = either {
        disable()
            .onLeft { raise(it) }
            .onRight {
                enable().onLeft { raise(it) }
            }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun publish(payload: MessagingPayload): Boolean {
        // Dispatched locally and synchronously regardless of whether a broker is connected, so
        // this server's own listeners (e.g., cache invalidation) react immediately, and messaging
        // being disabled/unavailable never leaves this server's own state stale. Messages that
        // come back over the broker are filtered out by serverId in handleIncoming to avoid a
        // second, duplicate local dispatch.
        listeners.forEach { listener -> runCatching { listener(payload) } }

        val currentBroker = broker ?: return false

        return runCatching {
            val encodedPayload = ProtoBuf.encodeToByteArray<MessagingPayload>(payload)
            val networkPayload = NetworkPayload(serverId, encodedPayload)
            val encodedNetworkPayload = ProtoBuf.encodeToByteArray(networkPayload)

            currentBroker.publish(MessagingConstants.SYNC_CHANNEL, encodedNetworkPayload)
        }.getOrDefault(false)
    }

    override fun subscribe(listener: (MessagingPayload) -> Unit) {
        listeners.add(listener)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun handleIncoming(bytes: ByteArray) {
        runCatching {
            val networkPayload = ProtoBuf.decodeFromByteArray<NetworkPayload>(bytes)
            if (networkPayload.serverId == serverId) {
                return@runCatching
            }

            val payload = ProtoBuf.decodeFromByteArray<MessagingPayload>(networkPayload.payload)
            listeners.forEach { listener -> runCatching { listener(payload) } }
        }.onFailure { throwable ->
            gradeway.logger.error("Failed to process an incoming messaging payload: ${throwable.localizedMessage}")
        }
    }

    /**
     * Translates a whole-entity change reported by Exposed's [EntityHook] into the matching
     * [MessagingPayload] and publishes it, covering the five primary entity types (role, group,
     * player, permission, permission template) without requiring any per-service-method
     * instrumentation. Relationship and attribute mutations are published explicitly at their
     * mutation sites instead, since the foreign keys they need are not recoverable from a bare
     * [EntityChange] once the underlying row has been removed.
     */
    private fun handleEntityChange(change: EntityChange) {
        val id = (change.entityId.value as? UUID)?.toString() ?: return
        val action = change.changeType.toMessagingAction()

        val payload: MessagingPayload = when (change.entityClass) {
            DatabaseRoleEntity -> RoleChangedPayload(id, action)
            DatabaseGroupEntity -> GroupChangedPayload(id, action)
            DatabasePlayerEntity -> PlayerChangedPayload(id, action)
            DatabasePermissionEntity -> PermissionChangedPayload(id, action)
            DatabasePermissionTemplateEntity -> PermissionTemplateChangedPayload(id, action)
            else -> return
        }

        publish(payload)
    }

    private fun EntityChangeType.toMessagingAction(): MessagingAction = when (this) {
        EntityChangeType.Created -> MessagingAction.CREATED
        EntityChangeType.Updated -> MessagingAction.UPDATED
        EntityChangeType.Removed -> MessagingAction.DELETED
    }
}
