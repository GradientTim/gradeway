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
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerAttributeEntity
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleAttributeEntity
import dev.gradienttim.gradeway.entity.SharedAttributeEntity
import dev.gradienttim.gradeway.entity.player.PlayerAttributeEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleAttributeEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.messaging.payloads.MessagingAction
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import dev.gradienttim.gradeway.messaging.payloads.PlayerAttributeChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.RoleAttributeChangedPayload
import dev.gradienttim.gradeway.reference.AttributeReference
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class CommonAttributeService(val gradeway: CommonGradeway) : AttributeService, KoinComponent {
    private val roleService: RoleService by inject()
    private val playerService: PlayerService by inject()

    override fun <TValue : Any> addRoleAttribute(
        id: UUID,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = either {
        val entity = roleService.findById(id) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addRoleAttribute(entity, attribute)
    }

    override fun <TValue : Any> addRoleAttribute(
        entity: RoleEntity,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = addEntityAttribute(entity, attribute) {
        DatabaseRoleAttributeEntity.new {
            this.roleId = entity.id
            this.type = attribute.type.type
            this.key = attribute.key
            this.value = attribute.type.serialize(attribute.value)
        }
    }

    override fun <TValue : Any> addRoleAttribute(
        idOrName: String,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = either {
        val entity = roleService.findByIdOrName(idOrName) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addRoleAttribute(entity, attribute)
    }

    override fun <TValue : Any> updateRoleAttribute(
        id: UUID,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = either {
        val entity = roleService.findById(id) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updateRoleAttribute(entity, key, value)
    }

    override fun <TValue : Any> updateRoleAttribute(
        entity: RoleEntity,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = updateEntityAttribute(entity, key, value)

    override fun <TValue : Any> updateRoleAttribute(
        idOrName: String,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = either {
        val entity = roleService.findByIdOrName(idOrName) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updateRoleAttribute(entity, key, value)
    }

    override fun removeRoleAttribute(
        id: UUID,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        val entity = roleService.findById(id) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removeRoleAttribute(entity, key)
    }

    override fun removeRoleAttribute(
        entity: RoleEntity,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Unit> = removeEntityAttribute(entity, key)

    override fun removeRoleAttribute(
        idOrName: String,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        val entity = roleService.findByIdOrName(idOrName) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removeRoleAttribute(entity, key)
    }

    override fun clearRoleAttributes(
        id: UUID
    ): Either<AttributeService.ClearAttributesError, Unit> = either {
        val entity = roleService.findById(id) ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearRoleAttributes(entity)
    }

    override fun clearRoleAttributes(
        entity: RoleEntity
    ): Either<AttributeService.ClearAttributesError, Unit> = clearEntityAttributes(entity)

    override fun clearRoleAttributes(
        idOrName: String
    ): Either<AttributeService.ClearAttributesError, Unit> = either {
        val entity = roleService.findByIdOrName(idOrName) ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearRoleAttributes(entity)
    }

    override fun hasRoleAttribute(id: UUID, key: Key): Boolean {
        val entity = roleService.findById(id) ?: return false
        return hasRoleAttribute(entity, key)
    }

    override fun hasRoleAttribute(entity: RoleEntity, key: Key): Boolean {
        return entity.attributes.any { it.key == key }
    }

    override fun hasRoleAttribute(idOrName: String, key: Key): Boolean {
        val entity = roleService.findByIdOrName(idOrName) ?: return false
        return hasRoleAttribute(entity, key)
    }

    override fun getRoleAttribute(id: UUID, key: Key): RoleAttributeEntity? {
        val entity = roleService.findById(id) ?: return null
        return getRoleAttribute(entity, key)
    }

    override fun getRoleAttribute(entity: RoleEntity, key: Key): RoleAttributeEntity? {
        return entity.attributes.find { it.key == key }
    }

    override fun getRoleAttribute(idOrName: String, key: Key): RoleAttributeEntity? {
        val entity = roleService.findByIdOrName(idOrName) ?: return null
        return getRoleAttribute(entity, key)
    }

    override fun <TValue : Any> addPlayerAttribute(
        id: UUID,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = either {
        val entity = playerService.findById(id) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addPlayerAttribute(entity, attribute)
    }

    override fun <TValue : Any> addPlayerAttribute(
        entity: PlayerEntity,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = addEntityAttribute(entity, attribute) {
        DatabasePlayerAttributeEntity.new {
            this.playerId = entity.id
            this.type = attribute.type.type
            this.key = attribute.key
            this.value = attribute.type.serialize(attribute.value)
        }
    }

    override fun <TValue : Any> addPlayerAttribute(
        idOrName: String,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, SharedAttributeEntity> = either {
        val entity = playerService.findByIdOrName(idOrName) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addPlayerAttribute(entity, attribute)
    }

    override fun <TValue : Any> updatePlayerAttribute(
        id: UUID,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = either {
        val entity = playerService.findById(id) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updatePlayerAttribute(entity, key, value)
    }

    override fun <TValue : Any> updatePlayerAttribute(
        entity: PlayerEntity,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = updateEntityAttribute(entity, key, value)

    override fun <TValue : Any> updatePlayerAttribute(
        idOrName: String,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> = either {
        val entity =
            playerService.findByIdOrName(idOrName) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updatePlayerAttribute(entity, key, value)
    }

    override fun removePlayerAttribute(
        id: UUID,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        val entity = playerService.findById(id) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removePlayerAttribute(entity, key)
    }

    override fun removePlayerAttribute(
        entity: PlayerEntity,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Unit> = removeEntityAttribute(entity, key)

    override fun removePlayerAttribute(
        idOrName: String,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        val entity =
            playerService.findByIdOrName(idOrName) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removePlayerAttribute(entity, key)
    }

    override fun clearPlayerAttributes(
        id: UUID
    ): Either<AttributeService.ClearAttributesError, Unit> = either {
        val entity = playerService.findById(id) ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearPlayerAttributes(entity)
    }

    override fun clearPlayerAttributes(
        entity: PlayerEntity,
    ): Either<AttributeService.ClearAttributesError, Unit> = clearEntityAttributes(entity)

    override fun clearPlayerAttributes(
        idOrName: String,
    ): Either<AttributeService.ClearAttributesError, Unit> = either {
        val entity =
            playerService.findByIdOrName(idOrName) ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearPlayerAttributes(entity)
    }

    override fun hasPlayerAttribute(id: UUID, key: Key): Boolean {
        val entity = playerService.findById(id) ?: return false
        return hasPlayerAttribute(entity, key)
    }

    override fun hasPlayerAttribute(entity: PlayerEntity, key: Key): Boolean {
        return entity.attributes.any { it.key == key }
    }

    override fun hasPlayerAttribute(idOrName: String, key: Key): Boolean {
        val entity = playerService.findByIdOrName(idOrName) ?: return false
        return hasPlayerAttribute(entity, key)
    }

    override fun getPlayerAttribute(id: UUID, key: Key): PlayerAttributeEntity? {
        val entity = playerService.findById(id) ?: return null
        return getPlayerAttribute(entity, key)
    }

    override fun getPlayerAttribute(entity: PlayerEntity, key: Key): PlayerAttributeEntity? {
        return entity.attributes.find { it.key == key }
    }

    override fun getPlayerAttribute(idOrName: String, key: Key): PlayerAttributeEntity? {
        val entity = playerService.findByIdOrName(idOrName) ?: return null
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

    private fun <TValue : Any, TAttributeEntity> addEntityAttribute(
        entity: AttributeReference<out SharedAttributeEntity>,
        attribute: Attribute<TValue>,
        createEntityAttribute: () -> TAttributeEntity
    ): Either<AttributeService.AddAttributeError, TAttributeEntity> = either {
        transaction(gradeway.database) {
            if (entity.attributes.find { it.key == attribute.key } != null) {
                raise(AttributeService.AddAttributeError.AttributeAlreadyExists)
            }
            try {
                createEntityAttribute()
            } catch (throwable: Throwable) {
                raise(AttributeService.AddAttributeError.Unexpected(throwable))
            }
        }
    }.onRight { publishAttributeChanged(entity, attribute.key, MessagingAction.CREATED) }

    private fun <TValue : Any> updateEntityAttribute(
        entity: AttributeReference<out SharedAttributeEntity>,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, SharedAttributeEntity> =
        either<AttributeService.UpdateAttributeError, SharedAttributeEntity> {
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

                try {
                    var tempValue = value
                    if (attributeType.unsafe) {
                        tempValue = attributeType.deserialize(value.toString()) ?: attributeType.fallback(key)
                    }

                    attribute.value = attributeType.serialize(tempValue)
                    attribute.flush()

                    attribute
                } catch (throwable: Throwable) {
                    raise(AttributeService.UpdateAttributeError.Unexpected(throwable))
                }
            }
        }.onRight { publishAttributeChanged(entity, key, MessagingAction.UPDATED) }

    private fun removeEntityAttribute(
        entity: AttributeReference<out SharedAttributeEntity>,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Unit> = either {
        transaction(gradeway.database) {
            val attribute = entity.attributes.find { it.key == key }
            if (attribute == null) {
                raise(AttributeService.RemoveAttributeError.AttributeNotExists)
            }

            if (attribute !is Entity<*>) {
                val throwable = Throwable("Attribute is not a database Entity")
                raise(AttributeService.RemoveAttributeError.Unexpected(throwable))
            }

            try {
                attribute.delete()
            } catch (throwable: Throwable) {
                raise(AttributeService.RemoveAttributeError.Unexpected(throwable))
            }
        }
    }.onRight { publishAttributeChanged(entity, key, MessagingAction.DELETED) }

    private fun clearEntityAttributes(
        entity: AttributeReference<out SharedAttributeEntity>
    ): Either<AttributeService.ClearAttributesError, Unit> {
        val clearedKeys = transaction(gradeway.database) { entity.attributes.map { it.key } }

        return either {
            transaction(gradeway.database) {
                if (entity.attributes.empty()) {
                    raise(AttributeService.ClearAttributesError.NoAttributesFound)
                }

                try {
                    entity.attributes.forEach { attributeEntity ->
                        if (attributeEntity is Entity<*>) {
                            attributeEntity.delete()
                        }
                    }
                } catch (throwable: Throwable) {
                    raise(AttributeService.ClearAttributesError.Unexpected(throwable))
                }
            }
        }.onRight { clearedKeys.forEach { publishAttributeChanged(entity, it, MessagingAction.DELETED) } }
    }
}
