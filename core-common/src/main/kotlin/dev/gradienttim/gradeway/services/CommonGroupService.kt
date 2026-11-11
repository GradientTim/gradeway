/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.audited
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupEntity
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleGroupEntity
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RoleGroupEntity
import dev.gradienttim.gradeway.event.fireCancellable
import dev.gradienttim.gradeway.events.group.*
import dev.gradienttim.gradeway.extensions.eqUuidStr
import dev.gradienttim.gradeway.extensions.isIntegrityConstraintViolation
import dev.gradienttim.gradeway.extensions.isNameValid
import dev.gradienttim.gradeway.extensions.isUuid
import dev.gradienttim.gradeway.messaging.payloads.GroupRoleChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingAction
import dev.gradienttim.gradeway.services.GroupService.*
import dev.gradienttim.gradeway.spec.GroupCreateSpec
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.emptySized
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class CommonGroupService(
    val gradeway: CommonGradeway<*>
) : GroupService {
    override fun create(
        actor: Actor,
        name: String,
        builder: GroupCreateSpec.() -> Unit,
    ): Either<CreateGroupError, GroupEntity> = gradeway.audited(actor) { audit ->
        val spec = GroupCreateSpec(name).apply(builder)
        fireCancellable(gradeway, GroupCreateEvent(actor, spec), CreateGroupError.Cancelled)
        if (!spec.name.isNameValid(TableConstants.GROUPS_TABLE_MAX_NAME_LENGTH)) {
            raise(CreateGroupError.InvalidName)
        }

        transaction(gradeway.database) {
            try {
                DatabaseGroupEntity.new {
                    this.name = spec.name
                    this.defaultWeight = spec.defaultWeight
                }.also {
                    audit.created(it)
                    audit.emit(GroupCreatedEvent(actor, it.id.value, it.name, it.defaultWeight))
                }
            } catch (throwable: Throwable) {
                raise(CreateGroupError.Unexpected(throwable))
            }
        }
    }

    override fun delete(
        actor: Actor,
        id: UUID,
    ): Either<DeleteGroupError, Unit> = either {
        val entity = findById(id) ?: raise(DeleteGroupError.EntityNotFound)
        return delete(actor, entity)
    }

    override fun delete(
        actor: Actor,
        entity: GroupEntity,
    ): Either<DeleteGroupError, Unit> = gradeway.audited(actor) { audit ->
        if (entity !is DatabaseGroupEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseGroupEntity")
            raise(DeleteGroupError.Unexpected(throwable))
        }
        fireCancellable(gradeway, GroupDeleteEvent(actor, entity), DeleteGroupError.Cancelled)

        try {
            transaction(gradeway.database) {
                audit.deleted(entity)
                audit.emit(GroupDeletedEvent(actor, entity.id.value, entity.name))
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(DeleteGroupError.Unexpected(throwable))
        }
    }

    override fun delete(
        actor: Actor,
        idOrName: String,
    ): Either<DeleteGroupError, Unit> = either {
        val entity = findByIdOrName(idOrName) ?: raise(DeleteGroupError.EntityNotFound)
        return delete(actor, entity)
    }

    override fun setName(
        actor: Actor,
        id: UUID,
        name: String,
    ): Either<SetNameError, Unit> = either {
        val entity = findById(id) ?: raise(SetNameError.EntityNotFound)
        return setName(actor, entity, name)
    }

    override fun setName(
        actor: Actor,
        entity: GroupEntity,
        name: String,
    ): Either<SetNameError, Unit> = gradeway.audited(actor) { audit ->
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
        fireCancellable(gradeway, GroupRenameEvent(actor, entity, name), SetNameError.Cancelled)

        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                audit.emit(GroupRenamedEvent(actor, entity.id.value, entity.name, name))
                entity.name = name
                entity.flush()
                audit.updated(entity, before)
            }
        } catch (throwable: Throwable) {
            raise(SetNameError.Unexpected(throwable))
        }
    }

    override fun setName(
        actor: Actor,
        idOrName: String,
        name: String,
    ): Either<SetNameError, Unit> = either {
        val entity = findByIdOrName(idOrName) ?: raise(SetNameError.EntityNotFound)
        return setName(actor, entity, name)
    }

    override fun setDefaultWeight(
        actor: Actor,
        id: UUID,
        defaultWeight: Int,
    ): Either<SetDefaultWeightError, Unit> = either {
        val entity = findById(id) ?: raise(SetDefaultWeightError.EntityNotFound)
        return setDefaultWeight(actor, entity, defaultWeight)
    }

    override fun setDefaultWeight(
        actor: Actor,
        entity: GroupEntity,
        defaultWeight: Int,
    ): Either<SetDefaultWeightError, Unit> = gradeway.audited(actor) { audit ->
        if (entity.defaultWeight == defaultWeight) {
            raise(SetDefaultWeightError.WeightAlreadySet)
        }

        if (entity !is DatabaseGroupEntity) {
            val throwable = Throwable("Entity is not a type of DatabaseGroupEntity")
            raise(SetDefaultWeightError.Unexpected(throwable))
        }
        fireCancellable(
            gradeway,
            GroupDefaultWeightChangeEvent(actor, entity, defaultWeight),
            SetDefaultWeightError.Cancelled,
        )

        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                audit.emit(GroupDefaultWeightChangedEvent(actor, entity.id.value, entity.defaultWeight, defaultWeight))
                entity.defaultWeight = defaultWeight
                entity.flush()
                audit.updated(entity, before)
            }
        } catch (throwable: Throwable) {
            raise(SetDefaultWeightError.Unexpected(throwable))
        }
    }

    override fun setDefaultWeight(
        actor: Actor,
        idOrName: String,
        defaultWeight: Int,
    ): Either<SetDefaultWeightError, Unit> = either {
        val entity = findByIdOrName(idOrName) ?: raise(SetDefaultWeightError.EntityNotFound)
        return setDefaultWeight(actor, entity, defaultWeight)
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
                    (GroupsTable.id eqUuidStr value) or (GroupsTable.name eq value)
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

    override fun addRoleToGroup(
        actor: Actor,
        groupId: UUID,
        roleId: UUID,
    ): Either<AddTargetError, RoleGroupEntity> = either {
        val group = findById(groupId) ?: raise(AddTargetError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(AddTargetError.TargetNotFound)
        return addRoleToGroup(actor, group, role)
    }

    override fun addRoleToGroup(
        actor: Actor,
        groupId: UUID,
        role: RoleEntity,
    ): Either<AddTargetError, RoleGroupEntity> = either {
        val group = findById(groupId) ?: raise(AddTargetError.EntityNotFound)
        return addRoleToGroup(actor, group, role)
    }

    override fun addRoleToGroup(
        actor: Actor,
        groupIdOrName: String,
        roleId: UUID,
    ): Either<AddTargetError, RoleGroupEntity> = either {
        val group = findByIdOrName(groupIdOrName) ?: raise(AddTargetError.EntityNotFound)
        return addRoleToGroup(actor, group, roleId)
    }

    override fun addRoleToGroup(
        actor: Actor,
        groupIdOrName: String,
        role: RoleEntity,
    ): Either<AddTargetError, RoleGroupEntity> = either {
        val group = findByIdOrName(groupIdOrName) ?: raise(AddTargetError.EntityNotFound)
        return addRoleToGroup(actor, group, role)
    }

    override fun addRoleToGroup(
        actor: Actor,
        group: GroupEntity,
        roleId: UUID,
    ): Either<AddTargetError, RoleGroupEntity> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(AddTargetError.TargetNotFound)
        return addRoleToGroup(actor, group, role)
    }

    override fun addRoleToGroup(
        actor: Actor,
        group: GroupEntity,
        role: RoleEntity,
    ): Either<AddTargetError, RoleGroupEntity> = gradeway.audited(actor) { audit ->
        fireCancellable(gradeway, GroupRoleAddEvent(actor, group, role), AddTargetError.Cancelled)
        try {
            transaction(gradeway.database) {
                DatabaseRoleGroupEntity.new {
                    this.roleId = role.id
                    this.groupId = group.id
                }.also {
                    audit.created(it)
                    audit.emit(GroupRoleAddedEvent(actor, group.id.value, role.id.value))
                }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(AddTargetError.AlreadyInGroup)
            }
            raise(AddTargetError.Unexpected(throwable))
        }
    }.onRight {
        gradeway.messaging.publish(
            GroupRoleChangedPayload(group.id.value.toString(), role.id.value.toString(), MessagingAction.CREATED)
        )
    }

    override fun removeRoleFromGroup(
        actor: Actor,
        groupId: UUID,
        roleId: UUID,
    ): Either<RemoveTargetError, Unit> = either {
        val group = findById(groupId) ?: raise(RemoveTargetError.EntityNotFound)
        val role = gradeway.roles.findById(roleId) ?: raise(RemoveTargetError.TargetNotFound)
        return removeRoleFromGroup(actor, group, role)
    }

    override fun removeRoleFromGroup(
        actor: Actor,
        groupId: UUID,
        role: RoleEntity,
    ): Either<RemoveTargetError, Unit> = either {
        val group = findById(groupId) ?: raise(RemoveTargetError.EntityNotFound)
        return removeRoleFromGroup(actor, group, role)
    }

    override fun removeRoleFromGroup(
        actor: Actor,
        groupIdOrName: String,
        roleId: UUID,
    ): Either<RemoveTargetError, Unit> = either {
        val group = findByIdOrName(groupIdOrName) ?: raise(RemoveTargetError.EntityNotFound)
        return removeRoleFromGroup(actor, group, roleId)
    }

    override fun removeRoleFromGroup(
        actor: Actor,
        groupIdOrName: String,
        role: RoleEntity,
    ): Either<RemoveTargetError, Unit> = either {
        val group = findByIdOrName(groupIdOrName) ?: raise(RemoveTargetError.EntityNotFound)
        return removeRoleFromGroup(actor, group, role)
    }

    override fun removeRoleFromGroup(
        actor: Actor,
        group: GroupEntity,
        roleId: UUID,
    ): Either<RemoveTargetError, Unit> = either {
        val role = gradeway.roles.findById(roleId) ?: raise(RemoveTargetError.TargetNotFound)
        return removeRoleFromGroup(actor, group, role)
    }

    override fun removeRoleFromGroup(
        actor: Actor,
        group: GroupEntity,
        role: RoleEntity,
    ): Either<RemoveTargetError, Unit> = gradeway.audited(actor) { audit ->
        transaction(gradeway.database) {
            val roleGroupEntity = role.groups.find { it.groupId == group.id }
            if (roleGroupEntity == null) {
                raise(RemoveTargetError.NotInGroup)
            }

            if (roleGroupEntity !is DatabaseRoleGroupEntity) {
                val throwable = Throwable("Entity is not a type of DatabaseRoleGroupEntity")
                raise(RemoveTargetError.Unexpected(throwable))
            }

            fireCancellable(gradeway, GroupRoleRemoveEvent(actor, group, role), RemoveTargetError.Cancelled)
            try {
                audit.deleted(roleGroupEntity)
                audit.emit(GroupRoleRemovedEvent(actor, group.id.value, role.id.value))
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

    override fun setPermission(
        actor: Actor,
        id: UUID,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setGroupPermission(actor, id, permission, enabled)

    override fun setPermission(
        actor: Actor,
        entity: GroupEntity,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setGroupPermission(actor, entity, permission, enabled)

    override fun setPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
        enabled: Boolean,
    ) = gradeway.permissions.setGroupPermission(actor, idOrName, permission, enabled)

    override fun setPermissions(
        actor: Actor,
        id: UUID,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setGroupPermissions(actor, id, permissions)

    override fun setPermissions(
        actor: Actor,
        entity: GroupEntity,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setGroupPermissions(actor, entity, permissions)

    override fun setPermissions(
        actor: Actor,
        idOrName: String,
        permissions: Map<String, Boolean>,
    ) = gradeway.permissions.setGroupPermissions(actor, idOrName, permissions)

    override fun unsetPermission(
        actor: Actor,
        id: UUID,
        permission: String,
    ) = gradeway.permissions.unsetGroupPermission(actor, id, permission)

    override fun unsetPermission(
        actor: Actor,
        entity: GroupEntity,
        permission: String,
    ) = gradeway.permissions.unsetGroupPermission(actor, entity, permission)

    override fun unsetPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
    ) = gradeway.permissions.unsetGroupPermission(actor, idOrName, permission)

    override fun unsetPermissions(
        actor: Actor,
        id: UUID,
        permissions: List<String>,
    ) = gradeway.permissions.unsetGroupPermissions(actor, id, permissions)

    override fun unsetPermissions(
        actor: Actor,
        entity: GroupEntity,
        permissions: List<String>,
    ) = gradeway.permissions.unsetGroupPermissions(actor, entity, permissions)

    override fun unsetPermissions(
        actor: Actor,
        idOrName: String,
        permissions: List<String>,
    ) = gradeway.permissions.unsetGroupPermissions(actor, idOrName, permissions)

    override fun clearPermissions(
        actor: Actor,
        id: UUID,
    ) = gradeway.permissions.clearGroupPermissions(actor, id)

    override fun clearPermissions(
        actor: Actor,
        entity: GroupEntity,
    ) = gradeway.permissions.clearGroupPermissions(actor, entity)

    override fun clearPermissions(
        actor: Actor,
        idOrName: String,
    ) = gradeway.permissions.clearGroupPermissions(actor, idOrName)

    override fun hasPermission(
        id: UUID,
        permission: String,
    ) = gradeway.permissions.hasGroupPermission(id, permission)

    override fun hasPermission(
        entity: GroupEntity,
        permission: String,
    ) = gradeway.permissions.hasGroupPermission(entity, permission)

    override fun hasPermission(
        idOrName: String,
        permission: String,
    ) = gradeway.permissions.hasGroupPermission(idOrName, permission)

    override fun hasAnyPermissions(
        id: UUID,
        permissions: List<String>,
    ) = gradeway.permissions.hasGroupAnyPermissions(id, permissions)

    override fun hasAnyPermissions(
        entity: GroupEntity,
        permissions: List<String>,
    ) = gradeway.permissions.hasGroupAnyPermissions(entity, permissions)

    override fun hasAnyPermissions(
        idOrName: String,
        permissions: List<String>,
    ) = gradeway.permissions.hasGroupAnyPermissions(idOrName, permissions)

    override fun hasAllPermissions(
        id: UUID,
        permissions: List<String>,
    ) = gradeway.permissions.hasGroupAllPermissions(id, permissions)

    override fun hasAllPermissions(
        entity: GroupEntity,
        permissions: List<String>,
    ) = gradeway.permissions.hasGroupAllPermissions(entity, permissions)

    override fun hasAllPermissions(
        idOrName: String,
        permissions: List<String>,
    ) = gradeway.permissions.hasGroupAllPermissions(idOrName, permissions)

    override fun getPermissions(
        id: UUID,
    ) = gradeway.permissions.getGroupPermissions(id)

    override fun getPermissions(
        entity: GroupEntity,
    ) = gradeway.permissions.getGroupPermissions(entity)

    override fun getPermissions(
        idOrName: String,
    ) = gradeway.permissions.getGroupPermissions(idOrName)
}
