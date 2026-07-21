/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupEntity
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleGroupEntity
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RoleGroupEntity
import dev.gradienttim.gradeway.extensions.eqAsStr
import dev.gradienttim.gradeway.extensions.isUuid
import dev.gradienttim.gradeway.extensions.isNameValid
import dev.gradienttim.gradeway.messaging.payloads.GroupRoleChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingAction
import dev.gradienttim.gradeway.services.GroupService.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.emptySized
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class CommonGroupService(val gradeway: CommonGradeway) : GroupService, KoinComponent {
    private val roleService: RoleService by inject()
    private val permissionService: PermissionService by inject()

    override fun create(
        name: String,
        builder: GroupEntity.() -> Unit
    ): Either<CreateGroupError, GroupEntity> = either {
        if (!name.isNameValid(TableConstants.GROUPS_TABLE_MAX_NAME_LENGTH)) {
            raise(CreateGroupError.InvalidName)
        }

        transaction(gradeway.database) {
            try {
                DatabaseGroupEntity.new {
                    builder()

                    this.name = name
                }
            } catch (throwable: Throwable) {
                raise(CreateGroupError.Unexpected(throwable))
            }
        }
    }

    override fun delete(id: UUID): Either<DeleteGroupError, Unit> = either {
        val entity = findById(id) ?: raise(DeleteGroupError.EntityNotFound)
        return delete(entity)
    }

    override fun delete(entity: GroupEntity): Either<DeleteGroupError, Unit> = either {
        if (entity !is DatabaseGroupEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseGroupEntity")
            raise(DeleteGroupError.Unexpected(throwable))
        }

        transaction(gradeway.database) {
            try {
                entity.delete()
            } catch (throwable: Throwable) {
                raise(DeleteGroupError.Unexpected(throwable))
            }
        }
    }

    override fun delete(idOrName: String): Either<DeleteGroupError, Unit> = either {
        val entity = findByIdOrName(idOrName) ?: raise(DeleteGroupError.EntityNotFound)
        return delete(entity)
    }

    override fun setName(id: UUID, name: String): Either<SetNameError, Unit> = either {
        val entity = findById(id) ?: raise(SetNameError.EntityNotFound)
        return setName(entity, name)
    }

    override fun setName(entity: GroupEntity, name: String): Either<SetNameError, Unit> = either {
        if (!name.isNameValid(TableConstants.GROUPS_TABLE_MAX_NAME_LENGTH)) {
            raise(SetNameError.InvalidName)
        }

        if (entity.name == name) {
            raise(SetNameError.NameAlreadySet)
        }

        if (entity !is DatabaseGroupEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseGroupEntity")
            raise(SetNameError.Unexpected(throwable))
        }

        transaction(gradeway.database) {
            try {
                entity.name = name
                entity.flush()
            } catch (throwable: Throwable) {
                raise(SetNameError.Unexpected(throwable))
            }
        }
    }

    override fun setName(idOrName: String, name: String): Either<SetNameError, Unit> = either {
        val entity = findByIdOrName(idOrName) ?: raise(SetNameError.EntityNotFound)
        return setName(entity, name)
    }

    override fun setDefaultWeight(id: UUID, defaultWeight: Int): Either<SetDefaultWeightError, Unit> = either {
        val entity = findById(id) ?: raise(SetDefaultWeightError.EntityNotFound)
        return setDefaultWeight(entity, defaultWeight)
    }

    override fun setDefaultWeight(
        entity: GroupEntity,
        defaultWeight: Int
    ): Either<SetDefaultWeightError, Unit> = either {
        if (entity.defaultWeight == defaultWeight) {
            raise(SetDefaultWeightError.WeightAlreadySet)
        }

        if (entity !is DatabaseGroupEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseGroupEntity")
            raise(SetDefaultWeightError.Unexpected(throwable))
        }

        transaction(gradeway.database) {
            try {
                entity.defaultWeight = defaultWeight
                entity.flush()
            } catch (throwable: Throwable) {
                raise(SetDefaultWeightError.Unexpected(throwable))
            }
        }
    }

    override fun setDefaultWeight(idOrName: String, defaultWeight: Int): Either<SetDefaultWeightError, Unit> = either {
        val entity = findByIdOrName(idOrName) ?: raise(SetDefaultWeightError.EntityNotFound)
        return setDefaultWeight(entity, defaultWeight)
    }

    override fun findById(id: UUID): GroupEntity? {
        return try {
            transaction(gradeway.database) {
                DatabaseGroupEntity.findById(id)
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to find group by id '$id': ${throwable.localizedMessage}")
            null
        }
    }

    override fun findByIdOrName(value: String): GroupEntity? {
        if (!value.isUuid() && !value.isNameValid(TableConstants.GROUPS_TABLE_MAX_NAME_LENGTH)) {
            return null
        }
        return try {
            transaction(gradeway.database) {
                DatabaseGroupEntity.find {
                    (GroupsTable.id eqAsStr value) or (GroupsTable.name eq value)
                }.limit(1).firstOrNull()
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to find group by id or name '$value': ${throwable.localizedMessage}")
            null
        }
    }

    override fun list(
        where: (() -> Op<Boolean>)?,
        orderBy: Set<Pair<Expression<*>, SortOrder>>,
        limit: Int
    ): SizedIterable<GroupEntity> {
        return try {
            transaction(gradeway.database) {
                var iterable = if (where == null)
                    DatabaseGroupEntity.all() else DatabaseGroupEntity.find(where)

                iterable = if (orderBy.isNotEmpty()) {
                    iterable.orderBy(*orderBy.toTypedArray())
                } else {
                    iterable.orderBy(GroupsTable.createdAt to SortOrder.DESC)
                }

                iterable.limit(limit)
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to list groups: ${throwable.localizedMessage}")
            emptySized()
        }
    }

    override fun addRoleToGroup(groupId: UUID, roleId: UUID): Either<AddTargetError, RoleGroupEntity> = either {
        val group = findById(groupId) ?: raise(AddTargetError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(AddTargetError.TargetNotFound)
        return addRoleToGroup(group, role)
    }

    override fun addRoleToGroup(groupId: UUID, role: RoleEntity): Either<AddTargetError, RoleGroupEntity> = either {
        val group = findById(groupId) ?: raise(AddTargetError.EntityNotFound)
        return addRoleToGroup(group, role)
    }

    override fun addRoleToGroup(groupIdOrName: String, roleId: UUID): Either<AddTargetError, RoleGroupEntity> = either {
        val group = findByIdOrName(groupIdOrName) ?: raise(AddTargetError.EntityNotFound)
        return addRoleToGroup(group, roleId)
    }

    override fun addRoleToGroup(
        groupIdOrName: String,
        role: RoleEntity
    ): Either<AddTargetError, RoleGroupEntity> = either {
        val group = findByIdOrName(groupIdOrName) ?: raise(AddTargetError.EntityNotFound)
        return addRoleToGroup(group, role)
    }

    override fun addRoleToGroup(group: GroupEntity, roleId: UUID): Either<AddTargetError, RoleGroupEntity> = either {
        val role = roleService.findById(roleId) ?: raise(AddTargetError.TargetNotFound)
        return addRoleToGroup(group, role)
    }

    override fun addRoleToGroup(
        group: GroupEntity,
        role: RoleEntity
    ): Either<AddTargetError, RoleGroupEntity> = either {
        transaction(gradeway.database) {
            if (role.groups.any { it.groupId.value == group.id.value }) {
                raise(AddTargetError.AlreadyInGroup)
            }

            try {
                DatabaseRoleGroupEntity.new {
                    this.roleId = role.id
                    this.groupId = group.id
                }
            } catch (throwable: Throwable) {
                raise(AddTargetError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            GroupRoleChangedPayload(group.id.value.toString(), role.id.value.toString(), MessagingAction.CREATED)
        )
    }

    override fun removeRoleFromGroup(groupId: UUID, roleId: UUID): Either<RemoveTargetError, Unit> = either {
        val group = findById(groupId) ?: raise(RemoveTargetError.EntityNotFound)
        val role = roleService.findById(roleId) ?: raise(RemoveTargetError.TargetNotFound)
        return removeRoleFromGroup(group, role)
    }

    override fun removeRoleFromGroup(groupId: UUID, role: RoleEntity): Either<RemoveTargetError, Unit> = either {
        val group = findById(groupId) ?: raise(RemoveTargetError.EntityNotFound)
        return removeRoleFromGroup(group, role)
    }

    override fun removeRoleFromGroup(
        groupIdOrName: String,
        roleId: UUID
    ): Either<RemoveTargetError, Unit> = either {
        val group = findByIdOrName(groupIdOrName) ?: raise(RemoveTargetError.EntityNotFound)
        return removeRoleFromGroup(group, roleId)
    }

    override fun removeRoleFromGroup(
        groupIdOrName: String,
        role: RoleEntity
    ): Either<RemoveTargetError, Unit> = either {
        val group = findByIdOrName(groupIdOrName) ?: raise(RemoveTargetError.EntityNotFound)
        return removeRoleFromGroup(group, role)
    }

    override fun removeRoleFromGroup(group: GroupEntity, roleId: UUID): Either<RemoveTargetError, Unit> = either {
        val role = roleService.findById(roleId) ?: raise(RemoveTargetError.TargetNotFound)
        return removeRoleFromGroup(group, role)
    }

    override fun removeRoleFromGroup(group: GroupEntity, role: RoleEntity): Either<RemoveTargetError, Unit> = either {
        transaction(gradeway.database) {
            val roleGroupEntity = role.groups.find { it.groupId == group.id }
            if (roleGroupEntity == null) {
                raise(RemoveTargetError.NotInGroup)
            }

            if (roleGroupEntity !is DatabaseRoleGroupEntity) {
                val throwable = Throwable("Entity is not a type of DatabaseRoleGroupEntity")
                raise(RemoveTargetError.Unexpected(throwable))
            }

            try {
                roleGroupEntity.delete()
            } catch (throwable: Throwable) {
                raise(RemoveTargetError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            GroupRoleChangedPayload(group.id.value.toString(), role.id.value.toString(), MessagingAction.DELETED)
        )
    }

    override fun setPermission(id: UUID, permission: String, enabled: Boolean) =
        permissionService.setGroupPermission(id, permission, enabled)

    override fun setPermission(entity: GroupEntity, permission: String, enabled: Boolean) =
        permissionService.setGroupPermission(entity, permission, enabled)

    override fun setPermission(idOrName: String, permission: String, enabled: Boolean) =
        permissionService.setGroupPermission(idOrName, permission, enabled)

    override fun setPermissions(id: UUID, permissions: Map<String, Boolean>) =
        permissionService.setGroupPermissions(id, permissions)

    override fun setPermissions(entity: GroupEntity, permissions: Map<String, Boolean>) =
        permissionService.setGroupPermissions(entity, permissions)

    override fun setPermissions(idOrName: String, permissions: Map<String, Boolean>) =
        permissionService.setGroupPermissions(idOrName, permissions)

    override fun unsetPermission(id: UUID, permission: String) =
        permissionService.unsetGroupPermission(id, permission)

    override fun unsetPermission(entity: GroupEntity, permission: String) =
        permissionService.unsetGroupPermission(entity, permission)

    override fun unsetPermission(idOrName: String, permission: String) =
        permissionService.unsetGroupPermission(idOrName, permission)

    override fun unsetPermissions(id: UUID, permissions: List<String>) =
        permissionService.unsetGroupPermissions(id, permissions)

    override fun unsetPermissions(entity: GroupEntity, permissions: List<String>) =
        permissionService.unsetGroupPermissions(entity, permissions)

    override fun unsetPermissions(idOrName: String, permissions: List<String>) =
        permissionService.unsetGroupPermissions(idOrName, permissions)

    override fun clearPermissions(id: UUID) =
        permissionService.clearGroupPermissions(id)

    override fun clearPermissions(entity: GroupEntity) =
        permissionService.clearGroupPermissions(entity)

    override fun clearPermissions(idOrName: String) =
        permissionService.clearGroupPermissions(idOrName)

    override fun hasPermission(id: UUID, permission: String) =
        permissionService.hasGroupPermission(id, permission)

    override fun hasPermission(entity: GroupEntity, permission: String) =
        permissionService.hasGroupPermission(entity, permission)

    override fun hasPermission(idOrName: String, permission: String) =
        permissionService.hasGroupPermission(idOrName, permission)

    override fun hasAnyPermissions(id: UUID, permissions: List<String>) =
        permissionService.hasGroupAnyPermissions(id, permissions)

    override fun hasAnyPermissions(entity: GroupEntity, permissions: List<String>) =
        permissionService.hasGroupAnyPermissions(entity, permissions)

    override fun hasAnyPermissions(idOrName: String, permissions: List<String>) =
        permissionService.hasGroupAnyPermissions(idOrName, permissions)

    override fun hasAllPermissions(id: UUID, permissions: List<String>) =
        permissionService.hasGroupAllPermissions(id, permissions)

    override fun hasAllPermissions(entity: GroupEntity, permissions: List<String>) =
        permissionService.hasGroupAllPermissions(entity, permissions)

    override fun hasAllPermissions(idOrName: String, permissions: List<String>) =
        permissionService.hasGroupAllPermissions(idOrName, permissions)

    override fun getPermissions(id: UUID) =
        permissionService.getGroupPermissions(id)

    override fun getPermissions(entity: GroupEntity) =
        permissionService.getGroupPermissions(entity)

    override fun getPermissions(idOrName: String) =
        permissionService.getGroupPermissions(idOrName)
}
