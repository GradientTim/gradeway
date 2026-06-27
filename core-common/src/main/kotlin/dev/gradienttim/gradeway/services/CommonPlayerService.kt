/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerEntity
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.extensions.eqAsStr
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class CommonPlayerService(val gradeway: CommonGradeway) : PlayerService, KoinComponent {
    private val attributeService: AttributeService by inject()
    private val permissionService: PermissionService by inject()

    override fun create(id: UUID, name: String): Either<PlayerService.CreatePlayerError, DatabasePlayerEntity> =
        either {
            if (!isNameValid(name)) {
                raise(PlayerService.CreatePlayerError.InvalidName)
            }
            if (existsById(id)) {
                raise(PlayerService.CreatePlayerError.EntityAlreadyExists)
            }
            try {
                transaction(gradeway.database) {
                    DatabasePlayerEntity.new(id) {
                        this.name = name
                    }
                }
            } catch (throwable: Throwable) {
                raise(PlayerService.CreatePlayerError.Unexpected(throwable))
            }
        }

    override fun delete(id: UUID): Either<PlayerService.DeletePlayerError, Unit> = either {
        val entity = findById(id) ?: raise(PlayerService.DeletePlayerError.EntityNotFound)
        try {
            transaction(gradeway.database) {
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(PlayerService.DeletePlayerError.Unexpected(throwable))
        }
    }

    override fun setName(id: UUID, name: String): Either<PlayerService.SetNameError, Boolean> = either {
        val entity = findById(id) ?: raise(PlayerService.SetNameError.EntityNotFound)
        return setName(entity, name)
    }

    override fun setName(entity: PlayerEntity, name: String): Either<PlayerService.SetNameError, Boolean> =
        either {
            if (!isNameValid(name)) {
                raise(PlayerService.SetNameError.InvalidName)
            }
            try {
                transaction(gradeway.database) {
                    entity.name = name
                    entity.flush()
                }
            } catch (throwable: Throwable) {
                raise(PlayerService.SetNameError.Unexpected(throwable))
            }
        }

    override fun findById(id: UUID): DatabasePlayerEntity? {
        return transaction(gradeway.database) {
            DatabasePlayerEntity.findById(id)
        }
    }

    override fun findByName(name: String): DatabasePlayerEntity? {
        if (!isNameValid(name)) {
            return null
        }
        return transaction(gradeway.database) {
            DatabasePlayerEntity.find { PlayersTable.name eq name }.limit(1).firstOrNull()
        }
    }

    override fun findByIdOrName(value: String): DatabasePlayerEntity? {
        if (value.length <= TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH && !isNameValid(value)) {
            return null
        }
        return transaction(gradeway.database) {
            DatabasePlayerEntity.find {
                (PlayersTable.id eqAsStr value) or (PlayersTable.name eq value)
            }.limit(1).firstOrNull()
        }
    }

    override fun existsById(id: UUID): Boolean =
        findById(id) != null

    override fun existsByName(name: String): Boolean =
        findByName(name) != null

    override fun existsByIdOrName(value: String): Boolean =
        findByIdOrName(value) != null

    override fun <TValue : Any> addAttribute(id: UUID, attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(id, attribute)

    override fun <TValue : Any> addAttribute(entity: PlayerEntity, attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(entity, attribute)

    override fun <TValue : Any> addAttribute(idOrName: String, attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(idOrName, attribute)

    override fun <TValue : Any> updateAttribute(id: UUID, key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(id, key, value)

    override fun <TValue : Any> updateAttribute(entity: PlayerEntity, key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(entity, key, value)

    override fun <TValue : Any> updateAttribute(idOrName: String, key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(idOrName, key, value)

    override fun removeAttribute(id: UUID, key: Key) =
        attributeService.removePlayerAttribute(id, key)

    override fun removeAttribute(entity: PlayerEntity, key: Key) =
        attributeService.removePlayerAttribute(entity, key)

    override fun removeAttribute(idOrName: String, key: Key) =
        attributeService.removePlayerAttribute(idOrName, key)

    override fun clearAttributes(id: UUID) =
        attributeService.clearPlayerAttributes(id)

    override fun clearAttributes(entity: PlayerEntity) =
        attributeService.clearPlayerAttributes(entity)

    override fun clearAttributes(idOrName: String) =
        attributeService.clearPlayerAttributes(idOrName)

    override fun hasAttribute(id: UUID, key: Key) =
        attributeService.hasPlayerAttribute(id, key)

    override fun hasAttribute(entity: PlayerEntity, key: Key) =
        attributeService.hasPlayerAttribute(entity, key)

    override fun hasAttribute(idOrName: String, key: Key) =
        attributeService.hasPlayerAttribute(idOrName, key)

    override fun getAttribute(id: UUID, key: Key) =
        attributeService.getPlayerAttribute(id, key)

    override fun getAttribute(entity: PlayerEntity, key: Key) =
        attributeService.getPlayerAttribute(entity, key)

    override fun getAttribute(idOrName: String, key: Key) =
        attributeService.getPlayerAttribute(idOrName, key)

    override fun setPermission(id: UUID, permission: String, enabled: Boolean) =
        permissionService.setPlayerPermission(id, permission, enabled)

    override fun setPermission(entity: PlayerEntity, permission: String, enabled: Boolean) =
        permissionService.setPlayerPermission(entity, permission, enabled)

    override fun setPermission(idOrName: String, permission: String, enabled: Boolean) =
        permissionService.setPlayerPermission(idOrName, permission, enabled)

    override fun setPermissions(id: UUID, permissions: Map<String, Boolean>) =
        permissionService.setPlayerPermissions(id, permissions)

    override fun setPermissions(entity: PlayerEntity, permissions: Map<String, Boolean>) =
        permissionService.setPlayerPermissions(entity, permissions)

    override fun setPermissions(idOrName: String, permissions: Map<String, Boolean>) =
        permissionService.setPlayerPermissions(idOrName, permissions)

    override fun unsetPermission(id: UUID, permission: String) =
        permissionService.unsetPlayerPermission(id, permission)

    override fun unsetPermission(entity: PlayerEntity, permission: String) =
        permissionService.unsetPlayerPermission(entity, permission)

    override fun unsetPermission(idOrName: String, permission: String) =
        permissionService.unsetPlayerPermission(idOrName, permission)

    override fun unsetPermissions(id: UUID, permissions: List<String>) =
        permissionService.unsetPlayerPermissions(id, permissions)

    override fun unsetPermissions(entity: PlayerEntity, permissions: List<String>) =
        permissionService.unsetPlayerPermissions(entity, permissions)

    override fun unsetPermissions(idOrName: String, permissions: List<String>) =
        permissionService.unsetPlayerPermissions(idOrName, permissions)

    override fun clearPermissions(id: UUID) =
        permissionService.clearPlayerPermissions(id)

    override fun clearPermissions(entity: PlayerEntity) =
        permissionService.clearPlayerPermissions(entity)

    override fun clearPermissions(idOrName: String) =
        permissionService.clearPlayerPermissions(idOrName)

    override fun hasPermission(id: UUID, permission: String) =
        permissionService.hasPlayerPermission(id, permission)

    override fun hasPermission(entity: PlayerEntity, permission: String) =
        permissionService.hasPlayerPermission(entity, permission)

    override fun hasPermission(idOrName: String, permission: String) =
        permissionService.hasPlayerPermission(idOrName, permission)

    override fun hasAnyPermissions(id: UUID, permissions: List<String>) =
        permissionService.hasPlayerAnyPermissions(id, permissions)

    override fun hasAnyPermissions(entity: PlayerEntity, permissions: List<String>) =
        permissionService.hasPlayerAnyPermissions(entity, permissions)

    override fun hasAnyPermissions(idOrName: String, permissions: List<String>) =
        permissionService.hasPlayerAnyPermissions(idOrName, permissions)

    override fun hasAllPermissions(id: UUID, permissions: List<String>) =
        permissionService.hasPlayerAllPermissions(id, permissions)

    override fun hasAllPermissions(entity: PlayerEntity, permissions: List<String>) =
        permissionService.hasPlayerAllPermissions(entity, permissions)

    override fun hasAllPermissions(idOrName: String, permissions: List<String>) =
        permissionService.hasPlayerAllPermissions(idOrName, permissions)

    override fun getPermissions(id: UUID) =
        permissionService.getPlayerPermissions(id)

    override fun getPermissions(entity: PlayerEntity) =
        permissionService.getPlayerPermissions(entity)

    override fun getPermissions(idOrName: String) =
        permissionService.getPlayerPermissions(idOrName)

    private fun isNameValid(name: String): Boolean {
        if (name.isNotBlank()) return true
        if (name.none { it.isWhitespace() }) return true
        if (name.length in 1..TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH) return true
        return false
    }
}
