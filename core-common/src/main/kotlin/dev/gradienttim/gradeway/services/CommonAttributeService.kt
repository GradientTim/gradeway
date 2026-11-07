/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.attribute.AttributeType
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.Auditable
import dev.gradienttim.gradeway.audit.audited
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerAttributeEntity
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleAttributeEntity
import dev.gradienttim.gradeway.entity.SharedAttributeEntity
import dev.gradienttim.gradeway.entity.player.PlayerAttributeEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleAttributeEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.event.eventSubjectOf
import dev.gradienttim.gradeway.event.fireCancellable
import dev.gradienttim.gradeway.events.attribute.*
import dev.gradienttim.gradeway.extensions.isIntegrityConstraintViolation
import dev.gradienttim.gradeway.messaging.payloads.*
import dev.gradienttim.gradeway.reference.AttributeReference
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class CommonAttributeService(
    val gradeway: CommonGradeway<*>
) : AttributeService {
    override fun <TValue : Any> addRoleAttribute(
        actor: Actor,
        id: UUID,
        attribute: Attribute<TValue>,
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = either {
        val entity = gradeway.roles.findById(id) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addRoleAttribute(actor, entity, attribute)
    }

    override fun <TValue : Any> addRoleAttribute(
        actor: Actor,
        entity: RoleEntity,
        attribute: Attribute<TValue>,
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = addEntityAttribute(
        actor,
        entity,
        attribute,
    ) {
        DatabaseRoleAttributeEntity.new {
            this.roleId = entity.id
            this.type = attribute.type.type
            this.key = attribute.key
            this.value = attribute.type.serialize(attribute.value)
        }
    }

    override fun <TValue : Any> addRoleAttribute(
        actor: Actor,
        idOrName: String,
        attribute: Attribute<TValue>,
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addRoleAttribute(actor, entity, attribute)
    }

    override fun <TValue : Any> updateRoleAttribute(
        actor: Actor,
        id: UUID,
        key: Key,
        value: TValue,
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = either {
        val entity = gradeway.roles.findById(id) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updateRoleAttribute(actor, entity, key, value)
    }

    override fun <TValue : Any> updateRoleAttribute(
        actor: Actor,
        entity: RoleEntity,
        key: Key,
        value: TValue,
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> =
        updateEntityAttribute(actor, entity, key, value)

    override fun <TValue : Any> updateRoleAttribute(
        actor: Actor,
        idOrName: String,
        key: Key,
        value: TValue,
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName)
            ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updateRoleAttribute(actor, entity, key, value)
    }

    override fun removeRoleAttribute(
        actor: Actor,
        id: UUID,
        key: Key,
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        val entity = gradeway.roles.findById(id) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removeRoleAttribute(actor, entity, key)
    }

    override fun removeRoleAttribute(
        actor: Actor,
        entity: RoleEntity,
        key: Key,
    ): Either<AttributeService.RemoveAttributeError, Unit> = removeEntityAttribute(actor, entity, key)

    override fun removeRoleAttribute(
        actor: Actor,
        idOrName: String,
        key: Key,
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName)
            ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removeRoleAttribute(actor, entity, key)
    }

    override fun clearRoleAttributes(
        actor: Actor,
        id: UUID,
    ): Either<AttributeService.ClearAttributesError, Unit> = either {
        val entity = gradeway.roles.findById(id) ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearRoleAttributes(actor, entity)
    }

    override fun clearRoleAttributes(
        actor: Actor,
        entity: RoleEntity,
    ): Either<AttributeService.ClearAttributesError, Unit> = clearEntityAttributes(actor, entity)

    override fun clearRoleAttributes(
        actor: Actor,
        idOrName: String,
    ): Either<AttributeService.ClearAttributesError, Unit> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName)
            ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearRoleAttributes(actor, entity)
    }

    override fun hasRoleAttribute(id: UUID, key: Key): Boolean {
        val entity = gradeway.roles.findById(id) ?: return false
        return hasRoleAttribute(entity, key)
    }

    override fun hasRoleAttribute(entity: RoleEntity, key: Key): Boolean {
        return transaction(gradeway.database) { entity.attributes.any { it.key == key } }
    }

    override fun hasRoleAttribute(idOrName: String, key: Key): Boolean {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: return false
        return hasRoleAttribute(entity, key)
    }

    override fun getRoleAttribute(id: UUID, key: Key): RoleAttributeEntity? {
        val entity = gradeway.roles.findById(id) ?: return null
        return getRoleAttribute(entity, key)
    }

    override fun getRoleAttribute(entity: RoleEntity, key: Key): RoleAttributeEntity? {
        return transaction(gradeway.database) { entity.attributes.find { it.key == key } }
    }

    override fun getRoleAttribute(idOrName: String, key: Key): RoleAttributeEntity? {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: return null
        return getRoleAttribute(entity, key)
    }

    override fun <TValue : Any> addPlayerAttribute(
        actor: Actor,
        id: UUID,
        attribute: Attribute<TValue>,
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = either {
        val entity = gradeway.players.findById(id) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addPlayerAttribute(actor, entity, attribute)
    }

    override fun <TValue : Any> addPlayerAttribute(
        actor: Actor,
        entity: PlayerEntity,
        attribute: Attribute<TValue>,
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = addEntityAttribute(
        actor,
        entity,
        attribute,
    ) {
        DatabasePlayerAttributeEntity.new {
            this.playerId = entity.id
            this.type = attribute.type.type
            this.key = attribute.key
            this.value = attribute.type.serialize(attribute.value)
        }
    }

    override fun <TValue : Any> addPlayerAttribute(
        actor: Actor,
        idOrName: String,
        attribute: Attribute<TValue>,
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addPlayerAttribute(actor, entity, attribute)
    }

    override fun <TValue : Any> updatePlayerAttribute(
        actor: Actor,
        id: UUID,
        key: Key,
        value: TValue,
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = either {
        val entity = gradeway.players.findById(id) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updatePlayerAttribute(actor, entity, key, value)
    }

    override fun <TValue : Any> updatePlayerAttribute(
        actor: Actor,
        entity: PlayerEntity,
        key: Key,
        value: TValue,
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> =
        updateEntityAttribute(actor, entity, key, value)

    override fun <TValue : Any> updatePlayerAttribute(
        actor: Actor,
        idOrName: String,
        key: Key,
        value: TValue,
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updatePlayerAttribute(actor, entity, key, value)
    }

    override fun removePlayerAttribute(
        actor: Actor,
        id: UUID,
        key: Key,
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        val entity = gradeway.players.findById(id) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removePlayerAttribute(actor, entity, key)
    }

    override fun removePlayerAttribute(
        actor: Actor,
        entity: PlayerEntity,
        key: Key,
    ): Either<AttributeService.RemoveAttributeError, Unit> = removeEntityAttribute(actor, entity, key)

    override fun removePlayerAttribute(
        actor: Actor,
        idOrName: String,
        key: Key,
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removePlayerAttribute(actor, entity, key)
    }

    override fun clearPlayerAttributes(
        actor: Actor,
        id: UUID,
    ): Either<AttributeService.ClearAttributesError, Unit> = either {
        val entity = gradeway.players.findById(id) ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearPlayerAttributes(actor, entity)
    }

    override fun clearPlayerAttributes(
        actor: Actor,
        entity: PlayerEntity,
    ): Either<AttributeService.ClearAttributesError, Unit> = clearEntityAttributes(actor, entity)

    override fun clearPlayerAttributes(
        actor: Actor,
        idOrName: String,
    ): Either<AttributeService.ClearAttributesError, Unit> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearPlayerAttributes(actor, entity)
    }

    override fun hasPlayerAttribute(id: UUID, key: Key): Boolean {
        val entity = gradeway.players.findById(id) ?: return false
        return hasPlayerAttribute(entity, key)
    }

    override fun hasPlayerAttribute(entity: PlayerEntity, key: Key): Boolean {
        return transaction(gradeway.database) { entity.attributes.any { it.key == key } }
    }

    override fun hasPlayerAttribute(idOrName: String, key: Key): Boolean {
        val entity = gradeway.players.findByIdOrName(idOrName) ?: return false
        return hasPlayerAttribute(entity, key)
    }

    override fun getPlayerAttribute(id: UUID, key: Key): PlayerAttributeEntity? {
        val entity = gradeway.players.findById(id) ?: return null
        return getPlayerAttribute(entity, key)
    }

    override fun getPlayerAttribute(entity: PlayerEntity, key: Key): PlayerAttributeEntity? {
        return transaction(gradeway.database) { entity.attributes.find { it.key == key } }
    }

    override fun getPlayerAttribute(idOrName: String, key: Key): PlayerAttributeEntity? {
        val entity = gradeway.players.findByIdOrName(idOrName) ?: return null
        return getPlayerAttribute(entity, key)
    }

    /**
     * Publishes the [MessagingPayload] matching the concrete kind of [entity] (role or player)
     * for a single attribute change. Shared by every add/update/remove/clear code path below,
     * since they all ultimately operate on an [AttributeReference] without otherwise knowing
     * which concrete entity kind they were called for.
     */
    private fun publishAttributeChanged(
        entity: AttributeReference<out SharedAttributeEntity>,
        key: Key,
        action: MessagingAction
    ) {
        val payload: MessagingPayload = when (entity) {
            is RoleEntity -> RoleAttributeChangedPayload(entity.id.value.toString(), key.asString(), action)
            is PlayerEntity -> PlayerAttributeChangedPayload(entity.id.value.toString(), key.asString(), action)
            else -> return
        }
        gradeway.messaging.publish(payload)
    }

    /**
     * Publishes a single general "all attributes cleared" [MessagingPayload] for [entity],
     * matching its concrete kind (role or player). Used by [clearEntityAttributes] instead of
     * publishing one [publishAttributeChanged] per removed key, since receivers treat any
     * payload as an invalidation signal and re-read current state rather than apply the
     * payload's fields directly (see [MessagingPayload]).
     */
    private fun publishAttributesCleared(entity: AttributeReference<out SharedAttributeEntity>) {
        val payload: MessagingPayload = when (entity) {
            is RoleEntity -> RoleAttributesClearedPayload(entity.id.value.toString())
            is PlayerEntity -> PlayerAttributesClearedPayload(entity.id.value.toString())
            else -> return
        }
        gradeway.messaging.publish(payload)
    }

    private fun <TValue : Any, TAttributeEntity : Auditable> addEntityAttribute(
        actor: Actor,
        entity: AttributeReference<out SharedAttributeEntity>,
        attribute: Attribute<TValue>,
        createEntityAttribute: () -> TAttributeEntity,
    ): Either<AttributeService.AddAttributeError, TAttributeEntity> = gradeway.audited(actor) { audit ->
        val subject = eventSubjectOf(entity)
        fireCancellable(
            gradeway,
            AttributeAddEvent(actor, subject, attribute),
            AttributeService.AddAttributeError.Cancelled,
        )
        try {
            transaction(gradeway.database) {
                createEntityAttribute().also { created ->
                    audit.created(created)
                    if (created is SharedAttributeEntity) {
                        audit.emit(AttributeAddedEvent(actor, subject, created.key, created.type, created.value))
                    }
                }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(AttributeService.AddAttributeError.AttributeAlreadyExists)
            }
            raise(AttributeService.AddAttributeError.Unexpected(throwable))
        }
    }.onRight { publishAttributeChanged(entity, attribute.key, MessagingAction.CREATED) }

    private fun <TValue : Any> updateEntityAttribute(
        actor: Actor,
        entity: AttributeReference<out SharedAttributeEntity>,
        key: Key,
        value: TValue,
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> =
        gradeway.audited(actor) { audit ->
            transaction(gradeway.database) {
                val attribute = entity.attributes.find { it.key == key }
                if (attribute == null) {
                    raise(AttributeService.UpdateAttributeError.AttributeNotExists)
                }

                if (attribute !is Entity<*>) {
                    val throwable = Throwable("Attribute is not a database Entity")
                    raise(AttributeService.UpdateAttributeError.Unexpected(throwable))
                }

                @Suppress("UNCHECKED_CAST")
                val attributeType = AttributeTypeRegistry.find(attribute.type) as? AttributeType<TValue>
                    ?: raise(AttributeService.UpdateAttributeError.AttributeTypeNotRegistered(attribute.type))

                val subject = eventSubjectOf(entity)
                fireCancellable(
                    gradeway,
                    AttributeUpdateEvent(actor, subject, key, value),
                    AttributeService.UpdateAttributeError.Cancelled,
                )

                try {
                    val before = (attribute as? Auditable)?.snapshot()
                    val previousValue = attribute.value
                    var tempValue = value
                    if (attributeType.unsafe) {
                        tempValue = attributeType.deserialize(value.toString()) ?: attributeType.fallback(key)
                    }

                    attribute.value = attributeType.serialize(tempValue)
                    attribute.flush()
                    if (attribute is Auditable && before != null) {
                        audit.updated(attribute, before)
                    }
                    audit.emit(AttributeUpdatedEvent(actor, subject, key, previousValue, attribute.value))

                    attribute
                } catch (throwable: Throwable) {
                    raise(AttributeService.UpdateAttributeError.Unexpected(throwable))
                }
            }
        }.onRight { publishAttributeChanged(entity, key, MessagingAction.UPDATED) }

    private fun removeEntityAttribute(
        actor: Actor,
        entity: AttributeReference<out SharedAttributeEntity>,
        key: Key,
    ): Either<AttributeService.RemoveAttributeError, Unit> = gradeway.audited(actor) { audit ->
        transaction(gradeway.database) {
            val attribute = entity.attributes.find { it.key == key }
            if (attribute == null) {
                raise(AttributeService.RemoveAttributeError.AttributeNotExists)
            }

            if (attribute !is Entity<*>) {
                val throwable = Throwable("Attribute is not a database Entity")
                raise(AttributeService.RemoveAttributeError.Unexpected(throwable))
            }

            val subject = eventSubjectOf(entity)
            fireCancellable(
                gradeway,
                AttributeRemoveEvent(actor, subject, key),
                AttributeService.RemoveAttributeError.Cancelled,
            )

            try {
                (attribute as? Auditable)?.let { audit.deleted(it) }
                audit.emit(AttributeRemovedEvent(actor, subject, key))
                attribute.delete()
            } catch (throwable: Throwable) {
                raise(AttributeService.RemoveAttributeError.Unexpected(throwable))
            }
        }
    }.onRight { publishAttributeChanged(entity, key, MessagingAction.DELETED) }

    private fun clearEntityAttributes(
        actor: Actor,
        entity: AttributeReference<out SharedAttributeEntity>,
    ): Either<AttributeService.ClearAttributesError, Unit> = gradeway.audited(actor) { audit ->
        transaction(gradeway.database) {
            if (entity.attributes.empty()) {
                raise(AttributeService.ClearAttributesError.NoAttributesFound)
            }

            val subject = eventSubjectOf(entity)
            fireCancellable(
                gradeway,
                AttributesClearEvent(actor, subject),
                AttributeService.ClearAttributesError.Cancelled,
            )

            try {
                audit.emit(AttributesClearedEvent(actor, subject, entity.attributes.map { it.key }))
                entity.attributes.forEach { attributeEntity ->
                    if (attributeEntity is Entity<*>) {
                        (attributeEntity as? Auditable)?.let { audit.deleted(it) }
                        attributeEntity.delete()
                    }
                }
            } catch (throwable: Throwable) {
                raise(AttributeService.ClearAttributesError.Unexpected(throwable))
            }
        }
    }.onRight { publishAttributesCleared(entity) }
}
