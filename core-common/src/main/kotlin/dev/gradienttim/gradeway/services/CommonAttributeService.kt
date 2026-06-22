/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.database.entities.AttributeEntity
import dev.gradienttim.gradeway.database.models.player.PlayerEntity
import dev.gradienttim.gradeway.database.models.role.RoleEntity
import net.kyori.adventure.key.Key
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
    ): Either<AttributeService.AddAttributeError, Boolean> = either {
        val entity = roleService.findById(id) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addRoleAttribute(entity, attribute)
    }

    override fun <TValue : Any> addRoleAttribute(
        entity: RoleEntity,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean> = addEntityAttribute(entity, attribute)

    override fun <TValue : Any> addRoleAttribute(
        idOrName: String,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean> = either {
        val entity = roleService.findByIdOrName(idOrName) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addRoleAttribute(entity, attribute)
    }

    override fun <TValue : Any> updateRoleAttribute(
        id: UUID,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean> = either {
        val entity = roleService.findById(id) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updateRoleAttribute(entity, key, value)
    }

    override fun <TValue : Any> updateRoleAttribute(
        entity: RoleEntity,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean> = updateEntityAttribute(entity, key, value)

    override fun <TValue : Any> updateRoleAttribute(
        idOrName: String,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean> = either {
        val entity = roleService.findByIdOrName(idOrName) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updateRoleAttribute(entity, key, value)
    }

    override fun removeRoleAttribute(
        id: UUID,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Boolean> = either {
        val entity = roleService.findById(id) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removeRoleAttribute(entity, key)
    }

    override fun removeRoleAttribute(
        entity: RoleEntity,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Boolean> = removeEntityAttribute(entity, key)

    override fun removeRoleAttribute(
        idOrName: String,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Boolean> = either {
        val entity = roleService.findByIdOrName(idOrName) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removeRoleAttribute(entity, key)
    }

    override fun clearRoleAttributes(
        id: UUID
    ): Either<AttributeService.ClearAttributesError, Boolean> = either {
        val entity = roleService.findById(id) ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearRoleAttributes(entity)
    }

    override fun clearRoleAttributes(
        entity: RoleEntity
    ): Either<AttributeService.ClearAttributesError, Boolean> = clearEntityAttributes(entity)

    override fun clearRoleAttributes(
        idOrName: String
    ): Either<AttributeService.ClearAttributesError, Boolean> = either {
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

    override fun getRoleAttribute(id: UUID, key: Key): Attribute<*>? {
        val entity = roleService.findById(id) ?: return null
        return getRoleAttribute(entity, key)
    }

    override fun getRoleAttribute(entity: RoleEntity, key: Key): Attribute<*>? {
        return entity.attributes.find { it.key == key }
    }

    override fun getRoleAttribute(idOrName: String, key: Key): Attribute<*>? {
        val entity = roleService.findByIdOrName(idOrName) ?: return null
        return getRoleAttribute(entity, key)
    }

    override fun <TValue : Any> addPlayerAttribute(
        id: UUID,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean> = either {
        val entity = playerService.findById(id) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addPlayerAttribute(entity, attribute)
    }

    override fun <TValue : Any> addPlayerAttribute(
        entity: PlayerEntity,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean> = addEntityAttribute(entity, attribute)

    override fun <TValue : Any> addPlayerAttribute(
        idOrName: String,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean> = either {
        val entity = playerService.findByIdOrName(idOrName) ?: raise(AttributeService.AddAttributeError.EntityNotFound)
        return addPlayerAttribute(entity, attribute)
    }

    override fun <TValue : Any> updatePlayerAttribute(
        id: UUID,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean> = either {
        val entity = playerService.findById(id) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updatePlayerAttribute(entity, key, value)
    }

    override fun <TValue : Any> updatePlayerAttribute(
        entity: PlayerEntity,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean> = updateEntityAttribute(entity, key, value)

    override fun <TValue : Any> updatePlayerAttribute(
        idOrName: String,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean> = either {
        val entity =
            playerService.findByIdOrName(idOrName) ?: raise(AttributeService.UpdateAttributeError.EntityNotFound)
        return updatePlayerAttribute(entity, key, value)
    }

    override fun removePlayerAttribute(
        id: UUID,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Boolean> = either {
        val entity = playerService.findById(id) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removePlayerAttribute(entity, key)
    }

    override fun removePlayerAttribute(
        entity: PlayerEntity,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Boolean> = removeEntityAttribute(entity, key)

    override fun removePlayerAttribute(
        idOrName: String,
        key: Key
    ): Either<AttributeService.RemoveAttributeError, Boolean> = either {
        val entity =
            playerService.findByIdOrName(idOrName) ?: raise(AttributeService.RemoveAttributeError.EntityNotFound)
        return removePlayerAttribute(entity, key)
    }

    override fun clearPlayerAttributes(
        id: UUID
    ): Either<AttributeService.ClearAttributesError, Boolean> = either {
        val entity = playerService.findById(id) ?: raise(AttributeService.ClearAttributesError.EntityNotFound)
        return clearPlayerAttributes(entity)
    }

    override fun clearPlayerAttributes(
        entity: PlayerEntity,
    ): Either<AttributeService.ClearAttributesError, Boolean> = clearEntityAttributes(entity)

    override fun clearPlayerAttributes(
        idOrName: String,
    ): Either<AttributeService.ClearAttributesError, Boolean> = either {
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

    override fun getPlayerAttribute(id: UUID, key: Key): Attribute<*>? {
        val entity = playerService.findById(id) ?: return null
        return getPlayerAttribute(entity, key)
    }

    override fun getPlayerAttribute(entity: PlayerEntity, key: Key): Attribute<*>? {
        return entity.attributes.find { it.key == key }
    }

    override fun getPlayerAttribute(idOrName: String, key: Key): Attribute<*>? {
        val entity = playerService.findByIdOrName(idOrName) ?: return null
        return getPlayerAttribute(entity, key)
    }

    private fun <TValue : Any> addEntityAttribute(
        entity: AttributeEntity,
        attribute: Attribute<TValue>,
    ): Either<AttributeService.AddAttributeError, Boolean> = either {
        val attributes = entity.attributes.toMutableSet()
        if (attributes.contains(attribute)) {
            raise(AttributeService.AddAttributeError.AttributeAlreadyExists)
        }
        attributes.add(attribute)
        try {
            transaction(gradeway.database) {
                entity.attributes = attributes
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(AttributeService.AddAttributeError.Unexpected(throwable))
        }
    }

    private fun <TValue : Any> updateEntityAttribute(
        entity: AttributeEntity,
        key: Key,
        value: TValue,
    ): Either<AttributeService.UpdateAttributeError, Boolean> = either {
        val attributes = entity.attributes.toMutableSet()
        val attribute = attributes.find { it.key == key }
        if (attribute == null) {
            raise(AttributeService.UpdateAttributeError.AttributeNotExists)
        }
        if (!attribute.updateFrom(value)) {
            raise(AttributeService.UpdateAttributeError.WrongAttributeType)
        }
        try {
            transaction(gradeway.database) {
                entity.attributes = attributes
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(AttributeService.UpdateAttributeError.Unexpected(throwable))
        }
    }

    private fun removeEntityAttribute(
        entity: AttributeEntity,
        key: Key,
    ): Either<AttributeService.RemoveAttributeError, Boolean> = either {
        val attributes = entity.attributes.toMutableSet()
        val attribute = attributes.find { it.key == key }
        if (attribute == null) {
            raise(AttributeService.RemoveAttributeError.AttributeNotExists)
        }
        attributes.remove(attribute)
        try {
            transaction(gradeway.database) {
                entity.attributes = attributes
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(AttributeService.RemoveAttributeError.Unexpected(throwable))
        }
    }

    private fun clearEntityAttributes(
        entity: AttributeEntity
    ): Either<AttributeService.ClearAttributesError, Boolean> = either {
        if (entity.attributes.isEmpty()) {
            raise(AttributeService.ClearAttributesError.NoAttributesFound)
        }
        try {
            transaction(gradeway.database) {
                entity.attributes = emptySet()
                entity.flush(null)
            }
        } catch (throwable: Throwable) {
            raise(AttributeService.ClearAttributesError.Unexpected(throwable))
        }
    }
}
