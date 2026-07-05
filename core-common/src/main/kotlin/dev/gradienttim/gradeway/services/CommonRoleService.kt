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
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.extensions.eqAsStr
import dev.gradienttim.gradeway.extensions.isValidName
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class CommonRoleService(val gradeway: CommonGradeway) : RoleService, KoinComponent {
    private val attributeService: AttributeService by inject()
    private val permissionService: PermissionService by inject()

    override fun create(name: String): Either<RoleService.CreateRoleError, DatabaseRoleEntity> = either {
        if (!name.isValidName(TableConstants.ROLES_TABLE_MAX_NAME_LENGTH)) {
            raise(RoleService.CreateRoleError.InvalidName)
        }
        if (existsByName(name)) {
            raise(RoleService.CreateRoleError.EntityAlreadyExists)
        }
        try {
            transaction(gradeway.database) {
                DatabaseRoleEntity.new {
                    this.name = name
                }
            }
        } catch (throwable: Throwable) {
            raise(RoleService.CreateRoleError.Unexpected(throwable))
        }
    }

    override fun delete(id: UUID): Either<RoleService.DeleteRoleError, Unit> = either {
        val entity = findById(id) ?: raise(RoleService.DeleteRoleError.EntityNotFound)
        try {
            transaction(gradeway.database) {
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(RoleService.DeleteRoleError.Unexpected(throwable))
        }
    }

    override fun setName(id: UUID, name: String): Either<RoleService.SetNameError, Boolean> = either {
        val entity = findById(id) ?: raise(RoleService.SetNameError.EntityNotFound)
        return setName(entity, name)
    }

    override fun setName(entity: RoleEntity, name: String): Either<RoleService.SetNameError, Boolean> = either {
        if (!name.isValidName(TableConstants.ROLES_TABLE_MAX_NAME_LENGTH)) {
            raise(RoleService.SetNameError.InvalidName)
        }
        if (entity !is DatabaseRoleEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseRoleEntity")
            raise(RoleService.SetNameError.Unexpected(throwable))
        }
        try {
            transaction(gradeway.database) {
                entity.name = name
                entity.flush()
            }
        } catch (throwable: Throwable) {
            raise(RoleService.SetNameError.Unexpected(throwable))
        }
    }

    override fun setWeight(id: UUID, weight: Int): Either<RoleService.SetWeightError, Boolean> = either {
        val entity = findById(id) ?: raise(RoleService.SetWeightError.EntityNotFound)
        return setWeight(entity, weight)
    }

    override fun setWeight(entity: RoleEntity, weight: Int): Either<RoleService.SetWeightError, Boolean> = either {
        if (entity !is DatabaseRoleEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseRoleEntity")
            raise(RoleService.SetWeightError.Unexpected(throwable))
        }
        try {
            transaction(gradeway.database) {
                entity.weight = weight
                entity.flush()
            }
        } catch (throwable: Throwable) {
            raise(RoleService.SetWeightError.Unexpected(throwable))
        }
    }

    override fun setWeight(idOrName: String, weight: Int): Either<RoleService.SetWeightError, Boolean> = either {
        val entity = findByIdOrName(idOrName) ?: raise(RoleService.SetWeightError.EntityNotFound)
        return setWeight(entity, weight)
    }

    override fun findById(id: UUID): DatabaseRoleEntity? {
        return transaction {
            DatabaseRoleEntity.findById(id)
        }
    }

    override fun findByName(name: String): DatabaseRoleEntity? {
        if (!name.isValidName(TableConstants.ROLES_TABLE_MAX_NAME_LENGTH)) {
            return null
        }
        return transaction(gradeway.database) {
            DatabaseRoleEntity.find { RolesTable.name eq name }.limit(1).firstOrNull()
        }
    }

    override fun findByIdOrName(value: String): DatabaseRoleEntity? {
        if (
            value.length <= TableConstants.ROLES_TABLE_MAX_NAME_LENGTH &&
            !value.isValidName(TableConstants.ROLES_TABLE_MAX_NAME_LENGTH)
        ) {
            return null
        }
        return transaction(gradeway.database) {
            DatabaseRoleEntity.find {
                (RolesTable.id eqAsStr value) or (RolesTable.name eq value)
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
        attributeService.addRoleAttribute(id, attribute)

    override fun <TValue : Any> addAttribute(entity: RoleEntity, attribute: Attribute<TValue>) =
        attributeService.addRoleAttribute(entity, attribute)

    override fun <TValue : Any> addAttribute(idOrName: String, attribute: Attribute<TValue>) =
        attributeService.addRoleAttribute(idOrName, attribute)

    override fun <TValue : Any> updateAttribute(id: UUID, key: Key, value: TValue) =
        attributeService.updateRoleAttribute(id, key, value)

    override fun <TValue : Any> updateAttribute(entity: RoleEntity, key: Key, value: TValue) =
        attributeService.updateRoleAttribute(entity, key, value)

    override fun <TValue : Any> updateAttribute(idOrName: String, key: Key, value: TValue) =
        attributeService.updateRoleAttribute(idOrName, key, value)

    override fun removeAttribute(id: UUID, key: Key) =
        attributeService.removeRoleAttribute(id, key)

    override fun removeAttribute(entity: RoleEntity, key: Key) =
        attributeService.removeRoleAttribute(entity, key)

    override fun removeAttribute(idOrName: String, key: Key) =
        attributeService.removeRoleAttribute(idOrName, key)

    override fun clearAttributes(id: UUID) =
        attributeService.clearRoleAttributes(id)

    override fun clearAttributes(entity: RoleEntity) =
        attributeService.clearRoleAttributes(entity)

    override fun clearAttributes(idOrName: String) =
        attributeService.clearRoleAttributes(idOrName)

    override fun hasAttribute(id: UUID, key: Key) =
        attributeService.hasRoleAttribute(id, key)

    override fun hasAttribute(entity: RoleEntity, key: Key) =
        attributeService.hasRoleAttribute(entity, key)

    override fun hasAttribute(idOrName: String, key: Key) =
        attributeService.hasRoleAttribute(idOrName, key)

    override fun getAttribute(id: UUID, key: Key) =
        attributeService.getRoleAttribute(id, key)

    override fun getAttribute(entity: RoleEntity, key: Key) =
        attributeService.getRoleAttribute(entity, key)

    override fun getAttribute(idOrName: String, key: Key) =
        attributeService.getRoleAttribute(idOrName, key)

    override fun setPermission(id: UUID, permission: String, enabled: Boolean) =
        permissionService.setRolePermission(id, permission, enabled)

    override fun setPermission(entity: RoleEntity, permission: String, enabled: Boolean) =
        permissionService.setRolePermission(entity, permission, enabled)

    override fun setPermission(idOrName: String, permission: String, enabled: Boolean) =
        permissionService.setRolePermission(idOrName, permission, enabled)

    override fun setPermissions(id: UUID, permissions: Map<String, Boolean>) =
        permissionService.setRolePermissions(id, permissions)

    override fun setPermissions(entity: RoleEntity, permissions: Map<String, Boolean>) =
        permissionService.setRolePermissions(entity, permissions)

    override fun setPermissions(idOrName: String, permissions: Map<String, Boolean>) =
        permissionService.setRolePermissions(idOrName, permissions)

    override fun unsetPermission(id: UUID, permission: String) =
        permissionService.unsetRolePermission(id, permission)

    override fun unsetPermission(entity: RoleEntity, permission: String) =
        permissionService.unsetRolePermission(entity, permission)

    override fun unsetPermission(idOrName: String, permission: String) =
        permissionService.unsetRolePermission(idOrName, permission)

    override fun unsetPermissions(id: UUID, permissions: List<String>) =
        permissionService.unsetRolePermissions(id, permissions)

    override fun unsetPermissions(entity: RoleEntity, permissions: List<String>) =
        permissionService.unsetRolePermissions(entity, permissions)

    override fun unsetPermissions(idOrName: String, permissions: List<String>) =
        permissionService.unsetRolePermissions(idOrName, permissions)

    override fun clearPermissions(id: UUID) =
        permissionService.clearRolePermissions(id)

    override fun clearPermissions(entity: RoleEntity) =
        permissionService.clearRolePermissions(entity)

    override fun clearPermissions(idOrName: String) =
        permissionService.clearRolePermissions(idOrName)

    override fun hasPermission(id: UUID, permission: String) =
        permissionService.hasRolePermission(id, permission)

    override fun hasPermission(entity: RoleEntity, permission: String) =
        permissionService.hasRolePermission(entity, permission)

    override fun hasPermission(idOrName: String, permission: String) =
        permissionService.hasRolePermission(idOrName, permission)

    override fun hasAnyPermissions(id: UUID, permissions: List<String>) =
        permissionService.hasRoleAnyPermissions(id, permissions)

    override fun hasAnyPermissions(entity: RoleEntity, permissions: List<String>) =
        permissionService.hasRoleAnyPermissions(entity, permissions)

    override fun hasAnyPermissions(idOrName: String, permissions: List<String>) =
        permissionService.hasRoleAnyPermissions(idOrName, permissions)

    override fun hasAllPermissions(id: UUID, permissions: List<String>) =
        permissionService.hasRoleAllPermissions(id, permissions)

    override fun hasAllPermissions(entity: RoleEntity, permissions: List<String>) =
        permissionService.hasRoleAllPermissions(entity, permissions)

    override fun hasAllPermissions(idOrName: String, permissions: List<String>) =
        permissionService.hasRoleAllPermissions(idOrName, permissions)

    override fun getPermissions(id: UUID) =
        permissionService.getRolePermissions(id)

    override fun getPermissions(entity: RoleEntity) =
        permissionService.getRolePermissions(entity)

    override fun getPermissions(idOrName: String) =
        permissionService.getRolePermissions(idOrName)
}
