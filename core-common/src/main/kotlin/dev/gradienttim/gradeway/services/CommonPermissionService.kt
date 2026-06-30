/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.permission.*
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerPermissionEntity
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerPermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.player.PlayerPermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.role.DatabaseRolePermissionEntity
import dev.gradienttim.gradeway.database.models.role.DatabaseRolePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.role.RolePermissionTemplatesTable
import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplatePermissionEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerPermissionEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionTemplateEntity
import dev.gradienttim.gradeway.extensions.eqAsStr
import dev.gradienttim.gradeway.extensions.isUuid
import dev.gradienttim.gradeway.reference.PermissionReference
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.emptySized
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

@Suppress("LargeClass", "TooManyFunctions")
class CommonPermissionService(val gradeway: CommonGradeway) : PermissionService, KoinComponent {
    private val rolesService: RoleService by inject()
    private val playersService: PlayerService by inject()

    override fun createPermission(
        value: String,
        type: PermissionEntity.Type
    ): Either<PermissionService.CreatePermissionError, PermissionEntity> = either {
        if (findPermissionByValue(value) != null) {
            raise(PermissionService.CreatePermissionError.AlreadyExists)
        }

        try {
            transaction(gradeway.database) {
                DatabasePermissionEntity.new {
                    this.value = value
                    this.type = type
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.CreatePermissionError.Unexpected(throwable))
        }
    }

    override fun deletePermission(id: UUID): Either<PermissionService.DeletePermissionError, Unit> = either {
        val entity = findPermissionById(id) ?: raise(PermissionService.DeletePermissionError.EntityNotFound)
        return deletePermission(entity)
    }

    override fun deletePermission(
        entity: PermissionEntity
    ): Either<PermissionService.DeletePermissionError, Unit> = either {
        if (entity !is DatabasePermissionEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionEntity")
            raise(PermissionService.DeletePermissionError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.DeletePermissionError.Unexpected(throwable))
        }
    }

    override fun deletePermission(idOrValue: String): Either<PermissionService.DeletePermissionError, Unit> = either {
        val entity =
            findPermissionByIdOrValue(idOrValue) ?: raise(PermissionService.DeletePermissionError.EntityNotFound)
        return deletePermission(entity)
    }

    override fun updatePermissionValue(
        id: UUID,
        value: String
    ): Either<PermissionService.UpdatePermissionValueError, Boolean> = either {
        val entity = findPermissionById(id) ?: raise(PermissionService.UpdatePermissionValueError.EntityNotFound)
        return updatePermissionValue(entity, value)
    }

    override fun updatePermissionValue(
        entity: PermissionEntity,
        value: String
    ): Either<PermissionService.UpdatePermissionValueError, Boolean> = either {
        if (entity.value == value) {
            raise(PermissionService.UpdatePermissionValueError.ValueAlreadySet)
        }

        if (entity !is DatabasePermissionEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionEntity")
            raise(PermissionService.UpdatePermissionValueError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                entity.value = value
                entity.flush()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.UpdatePermissionValueError.Unexpected(throwable))
        }
    }

    override fun updatePermissionValue(
        idOrValue: String,
        value: String
    ): Either<PermissionService.UpdatePermissionValueError, Boolean> = either {
        val entity =
            findPermissionByIdOrValue(idOrValue) ?: raise(PermissionService.UpdatePermissionValueError.EntityNotFound)
        return updatePermissionValue(entity, value)
    }

    override fun updatePermissionType(
        id: UUID,
        type: PermissionEntity.Type
    ): Either<PermissionService.UpdatePermissionTypeError, Boolean> = either {
        val entity = findPermissionById(id) ?: raise(PermissionService.UpdatePermissionTypeError.EntityNotFound)
        return updatePermissionType(entity, type)
    }

    override fun updatePermissionType(
        entity: PermissionEntity,
        type: PermissionEntity.Type
    ): Either<PermissionService.UpdatePermissionTypeError, Boolean> = either {
        if (entity.type == type) {
            raise(PermissionService.UpdatePermissionTypeError.TypeAlreadySet)
        }

        if (entity !is DatabasePermissionEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionEntity")
            raise(PermissionService.UpdatePermissionTypeError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                entity.type = type
                entity.flush()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.UpdatePermissionTypeError.Unexpected(throwable))
        }
    }

    override fun updatePermissionType(
        idOrValue: String,
        type: PermissionEntity.Type
    ): Either<PermissionService.UpdatePermissionTypeError, Boolean> = either {
        val entity =
            findPermissionByIdOrValue(idOrValue) ?: raise(PermissionService.UpdatePermissionTypeError.EntityNotFound)
        return updatePermissionType(entity, type)
    }

    override fun findPermissionById(id: UUID): PermissionEntity? {
        return try {
            transaction(gradeway.database) {
                DatabasePermissionEntity.findById(id)
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to find permission by id '$id': ${throwable.localizedMessage}")
            null
        }
    }

    override fun findPermissionByValue(value: String): PermissionEntity? {
        return try {
            transaction(gradeway.database) {
                DatabasePermissionEntity.find { PermissionsTable.value eq value }.firstOrNull()
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to find permission by value '$value': ${throwable.localizedMessage}")
            null
        }
    }

    override fun findPermissionByIdOrValue(value: String): PermissionEntity? {
        return try {
            transaction(gradeway.database) {
                DatabasePermissionEntity.find {
                    (PermissionsTable.id eqAsStr value) or (PermissionsTable.value eq value)
                }.limit(1).firstOrNull()
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to find permission by id or name '$value': ${throwable.localizedMessage}")
            null
        }
    }

    override fun createTemplate(
        name: String
    ): Either<PermissionService.CreateTemplateError, PermissionTemplateEntity> = either {
        if (!isTemplateNameValid(name)) {
            raise(PermissionService.CreateTemplateError.InvalidName)
        }

        try {
            transaction(gradeway.database) {
                DatabasePermissionTemplateEntity.new {
                    this.name = name
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.CreateTemplateError.Unexpected(throwable))
        }
    }

    override fun deleteTemplate(
        id: UUID
    ): Either<PermissionService.DeleteTemplateError, PermissionTemplateEntity> = either {
        val entity = findTemplateById(id) as? DatabasePermissionTemplateEntity
            ?: raise(PermissionService.DeleteTemplateError.EntityNotFound)

        try {
            transaction(gradeway.database) {
                entity.delete()
                entity
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.DeleteTemplateError.Unexpected(throwable))
        }
    }

    override fun setTemplateName(
        id: UUID,
        name: String
    ): Either<PermissionService.SetNameTemplateError, Boolean> = either {
        val entity = findTemplateById(id) ?: raise(PermissionService.SetNameTemplateError.EntityNotFound)
        return setTemplateName(entity, name)
    }

    override fun setTemplateName(
        entity: PermissionTemplateEntity,
        name: String
    ): Either<PermissionService.SetNameTemplateError, Boolean> = either {
        if (entity !is DatabasePermissionTemplateEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionTemplateEntity")
            raise(PermissionService.SetNameTemplateError.Unexpected(throwable))
        }

        if (!isTemplateNameValid(name)) {
            raise(PermissionService.SetNameTemplateError.InvalidName)
        }

        if (entity.name == name) {
            raise(PermissionService.SetNameTemplateError.NameAlreadySet)
        }

        try {
            transaction(gradeway.database) {
                entity.name = name
                entity.flush()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.SetNameTemplateError.Unexpected(throwable))
        }
    }

    override fun setTemplateName(
        idOrName: String,
        name: String
    ): Either<PermissionService.SetNameTemplateError, Boolean> = either {
        val entity = findTemplateByIdOrName(idOrName) ?: raise(PermissionService.SetNameTemplateError.EntityNotFound)
        return setTemplateName(entity, name)
    }

    override fun setTemplateAssignedTo(
        id: UUID,
        assignedTo: PermissionTemplateEntity.AssignedTo
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean> = either {
        val entity = findTemplateById(id) ?: raise(PermissionService.SetAssignedToTemplateError.EntityNotFound)
        return setTemplateAssignedTo(entity, assignedTo)
    }

    override fun setTemplateAssignedTo(
        entity: PermissionTemplateEntity,
        assignedTo: PermissionTemplateEntity.AssignedTo
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean> = either {
        if (entity !is DatabasePermissionTemplateEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionTemplateEntity")
            raise(PermissionService.SetAssignedToTemplateError.Unexpected(throwable))
        }

        if (entity.assignedTo == assignedTo) {
            raise(PermissionService.SetAssignedToTemplateError.AlreadyAssignedTo)
        }

        try {
            transaction(gradeway.database) {
                entity.assignedTo = assignedTo
                entity.flush()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.SetAssignedToTemplateError.Unexpected(throwable))
        }
    }

    override fun setTemplateAssignedTo(
        idOrName: String,
        assignedTo: PermissionTemplateEntity.AssignedTo
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean> = either {
        val entity =
            findTemplateByIdOrName(idOrName) ?: raise(PermissionService.SetAssignedToTemplateError.EntityNotFound)
        return setTemplateAssignedTo(entity, assignedTo)
    }

    override fun findTemplateById(id: UUID): PermissionTemplateEntity? {
        return try {
            transaction(gradeway.database) {
                DatabasePermissionTemplateEntity.findById(id)
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to find template by id '$id': ${throwable.localizedMessage}")
            null
        }
    }

    override fun findTemplateByName(name: String): PermissionTemplateEntity? {
        if (!isTemplateNameValid(name)) {
            return null
        }
        return try {
            transaction(gradeway.database) {
                DatabasePermissionTemplateEntity.find { PermissionTemplatesTable.name eq name }.limit(1).firstOrNull()
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to find template by name '$name': ${throwable.localizedMessage}")
            null
        }
    }

    override fun findTemplateByIdOrName(value: String): PermissionTemplateEntity? {
        if (!value.isUuid() && !isTemplateNameValid(value)) {
            return null
        }
        return try {
            transaction(gradeway.database) {
                DatabasePermissionTemplateEntity.find {
                    (PermissionTemplatesTable.id eqAsStr value) or (PermissionTemplatesTable.name eq value)
                }.limit(1).firstOrNull()
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to find template by id or name '$value': ${throwable.localizedMessage}")
            null
        }
    }

    override fun addPermissionToTemplate(
        templateId: UUID,
        permissionId: UUID
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template =
            findTemplateById(templateId) ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        val permission =
            findPermissionById(permissionId) ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(template, permission)
    }

    override fun addPermissionToTemplate(
        template: PermissionTemplateEntity,
        permissionId: UUID
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val permission =
            findPermissionById(permissionId) ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(template, permission)
    }

    override fun addPermissionToTemplate(
        templateId: UUID,
        permission: PermissionEntity
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template =
            findTemplateById(templateId) ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        return addPermissionToTemplate(template, permission)
    }

    override fun addPermissionToTemplate(
        templateIdOrName: String,
        permissionId: UUID
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        val permission = findPermissionById(permissionId)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(template, permission)
    }

    override fun addPermissionToTemplate(
        templateIdOrName: String,
        permission: PermissionEntity
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        return addPermissionToTemplate(template, permission)
    }

    override fun addPermissionToTemplate(
        template: PermissionTemplateEntity,
        permission: PermissionEntity
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        if (template.permissions.any { it.permissionId == permission.id }) {
            raise(PermissionService.AddPermissionToTemplateError.PermissionAlreadyExists)
        }

        try {
            transaction(gradeway.database) {
                DatabasePermissionTemplatePermissionEntity.new {
                    this.templateId = template.id
                    this.permissionId = permission.id
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.AddPermissionToTemplateError.Unexpected(throwable))
        }
    }

    override fun removePermissionFromTemplate(
        templateId: UUID,
        permissionId: UUID
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template =
            findTemplateById(templateId) ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        val permission =
            findPermissionById(permissionId)
                ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(template, permission)
    }

    override fun removePermissionFromTemplate(
        template: PermissionTemplateEntity,
        permissionId: UUID
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val permission =
            findPermissionById(permissionId)
                ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(template, permission)
    }

    override fun removePermissionFromTemplate(
        templateId: UUID,
        permission: PermissionEntity
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template =
            findTemplateById(templateId) ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        return removePermissionFromTemplate(template, permission)
    }

    override fun removePermissionFromTemplate(
        templateIdOrName: String,
        permissionId: UUID
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        val permission = findPermissionById(permissionId)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(template, permission)
    }

    override fun removePermissionFromTemplate(
        templateIdOrName: String,
        permission: PermissionEntity
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        return removePermissionFromTemplate(template, permission)
    }

    override fun removePermissionFromTemplate(
        template: PermissionTemplateEntity,
        permission: PermissionEntity
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val templatePermission = template.permissions.find { it.permissionId == permission.id }
        if (templatePermission == null) {
            raise(PermissionService.RemovePermissionFromTemplateError.PermissionNotExists)
        }

        if (templatePermission !is DatabasePermissionTemplatePermissionEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionTemplatePermissionEntity")
            raise(PermissionService.RemovePermissionFromTemplateError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                templatePermission.delete()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.RemovePermissionFromTemplateError.Unexpected(throwable))
        }
    }

    override fun clearPermissionsFromTemplate(
        templateId: UUID
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit> = either {
        val template =
            findTemplateById(templateId) ?: raise(PermissionService.ClearPermissionsFromTemplateError.EntityNotFound)
        return clearPermissionsFromTemplate(template)
    }

    override fun clearPermissionsFromTemplate(
        template: PermissionTemplateEntity
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit> = either {
        try {
            transaction(gradeway.database) {
                template.permissions.filterIsInstance<DatabasePermissionTemplatePermissionEntity>().forEach {
                    it.delete()
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ClearPermissionsFromTemplateError.Unexpected(throwable))
        }
    }

    override fun clearPermissionsFromTemplate(
        idOrName: String
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit> = either {
        val template = findTemplateByIdOrName(idOrName)
            ?: raise(PermissionService.ClearPermissionsFromTemplateError.EntityNotFound)
        return clearPermissionsFromTemplate(template)
    }

    override fun listTemplates(
        where: (() -> Op<Boolean>)?,
        orderBy: Set<Pair<Expression<*>, SortOrder>>,
        limit: Int
    ): SizedIterable<PermissionTemplateEntity> {
        return try {
            transaction(gradeway.database) {
                var iterable = if (where == null)
                    DatabasePermissionTemplateEntity.all() else DatabasePermissionTemplateEntity.find(where)

                iterable = if (orderBy.isNotEmpty()) {
                    iterable.orderBy(*orderBy.toTypedArray())
                } else {
                    iterable.orderBy(PermissionTemplatesTable.createdAt to SortOrder.DESC)
                }

                iterable.limit(limit)
            }
        } catch (throwable: Throwable) {
            gradeway.logger.error("Failed to list templates: ${throwable.localizedMessage}")
            emptySized()
        }
    }

    override fun linkTemplateToRole(
        templateId: UUID,
        roleId: UUID
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(templateEntity, roleEntity)
    }

    override fun linkTemplateToRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToRole(templateEntity, role)
    }

    override fun linkTemplateToRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(template, roleEntity)
    }

    override fun linkTemplateToRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        if (!template.assignedTo.allowForRole) {
            raise(PermissionService.LinkTemplateError.WrongAssignedTo)
        }

        val templateEntitiesCount = DatabaseRolePermissionTemplateEntity.find {
            (RolePermissionTemplatesTable.roleId eq role.id) and
                    (RolePermissionTemplatesTable.permissionTemplateId eq template.id)
        }.limit(1).count()

        if (templateEntitiesCount != 0L) {
            raise(PermissionService.LinkTemplateError.AlreadyLinked)
        }

        try {
            transaction(gradeway.database) {
                DatabaseRolePermissionTemplateEntity.new {
                    this.roleId = role.id
                    this.permissionTemplateId = template.id
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.LinkTemplateError.Unexpected(throwable))
        }
    }

    override fun unlinkTemplateFromRole(
        templateId: UUID,
        roleId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(templateEntity, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromRole(templateEntity, role)
    }

    override fun unlinkTemplateFromRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(template, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val entityTemplateEntity = DatabaseRolePermissionTemplateEntity.find {
            (RolePermissionTemplatesTable.roleId eq role.id) and
                    (RolePermissionTemplatesTable.permissionTemplateId eq template.id)
        }.limit(1).firstOrNull()

        if (entityTemplateEntity == null) {
            raise(PermissionService.UnlinkTemplateError.NotLinked)
        }

        try {
            transaction(gradeway.database) {
                entityTemplateEntity.delete()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.UnlinkTemplateError.Unexpected(throwable))
        }
    }

    override fun applyTemplateToRole(
        templateId: UUID,
        roleId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(templateEntity, roleEntity)
    }

    override fun applyTemplateToRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToRole(templateEntity, role)
    }

    override fun applyTemplateToRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(template, roleEntity)
    }

    override fun applyTemplateToRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        if (!template.assignedTo.allowForRole) {
            raise(PermissionService.ApplyTemplateError.WrongAssignedTo)
        }

        if (template !is DatabasePermissionTemplateEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionTemplateEntity")
            raise(PermissionService.ApplyTemplateError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                val permissions = template.permissions
                    .map { it.permission }
                    .associate { it.value to true }

                setRolePermissions(role, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ApplyTemplateError.Unexpected(throwable))
        }
    }

    override fun revokeTemplateFromRole(
        templateId: UUID,
        roleId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(templateEntity, roleEntity)
    }

    override fun revokeTemplateFromRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromRole(templateEntity, role)
    }

    override fun revokeTemplateFromRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(template, roleEntity)
    }

    override fun revokeTemplateFromRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        if (template !is DatabasePermissionTemplateEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionTemplateEntity")
            raise(PermissionService.RevokeTemplateError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                val permissions = template.permissions
                    .map { it.permission }
                    .map { it.value }

                unsetRolePermissions(role, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.RevokeTemplateError.Unexpected(throwable))
        }
    }

    override fun linkTemplateToPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val playerEntity =
            playersService.findById(playerId) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(templateEntity, playerEntity)
    }

    override fun linkTemplateToPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToPlayer(templateEntity, player)
    }

    override fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val playerEntity =
            playersService.findById(playerId) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(template, playerEntity)
    }

    override fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        if (!template.assignedTo.allowForPlayer) {
            raise(PermissionService.LinkTemplateError.WrongAssignedTo)
        }

        val templateEntitiesCount = DatabasePlayerPermissionTemplateEntity.find {
            (PlayerPermissionTemplatesTable.playerId eq player.id) and
                    (PlayerPermissionTemplatesTable.permissionTemplateId eq template.id)
        }.limit(1).count()

        if (templateEntitiesCount != 0L) {
            raise(PermissionService.LinkTemplateError.AlreadyLinked)
        }

        try {
            transaction(gradeway.database) {
                DatabasePlayerPermissionTemplateEntity.new {
                    this.playerId = player.id
                    this.permissionTemplateId = template.id
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.LinkTemplateError.Unexpected(throwable))
        }
    }

    override fun unlinkTemplateFromPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val player = playersService.findById(playerId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(template, player)
    }

    override fun unlinkTemplateFromPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromPlayer(template, player)
    }

    override fun unlinkTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val player = playersService.findById(playerId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(template, player)
    }

    override fun unlinkTemplateFromPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val entityTemplateEntity = DatabasePlayerPermissionTemplateEntity.find {
            (PlayerPermissionTemplatesTable.playerId eq player.id) and
                    (PlayerPermissionTemplatesTable.permissionTemplateId eq template.id)
        }.limit(1).firstOrNull()

        if (entityTemplateEntity == null) {
            raise(PermissionService.UnlinkTemplateError.NotLinked)
        }

        try {
            transaction(gradeway.database) {
                entityTemplateEntity.delete()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.UnlinkTemplateError.Unexpected(throwable))
        }
    }

    override fun applyTemplateToPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val player = playersService.findById(playerId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(template, player)
    }

    override fun applyTemplateToPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToPlayer(template, player)
    }

    override fun applyTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val player = playersService.findById(playerId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(template, player)
    }

    override fun applyTemplateToPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        if (!template.assignedTo.allowForPlayer) {
            raise(PermissionService.ApplyTemplateError.WrongAssignedTo)
        }

        if (template !is DatabasePermissionTemplateEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionTemplateEntity")
            raise(PermissionService.ApplyTemplateError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                val permissions = template.permissions
                    .map { it.permission }
                    .associate { it.value to true }

                setPlayerPermissions(player, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ApplyTemplateError.Unexpected(throwable))
        }
    }

    override fun revokeTemplateFromPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val player = playersService.findById(playerId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(template, player)
    }

    override fun revokeTemplateFromPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromPlayer(template, player)
    }

    override fun revokeTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val player = playersService.findById(playerId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(template, player)
    }

    override fun revokeTemplateFromPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        if (template !is DatabasePermissionTemplateEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionTemplateEntity")
            raise(PermissionService.RevokeTemplateError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                val permissions = template.permissions
                    .map { it.permission }
                    .map { it.value }

                unsetPlayerPermissions(player, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.RevokeTemplateError.Unexpected(throwable))
        }
    }

    override fun setRolePermission(
        id: UUID,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setRolePermission(entity, permission, enabled)
    }

    override fun setRolePermission(
        entity: RoleEntity,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> =
        setEntityPermission(entity, permission, enabled) { permissionEntity ->
            DatabaseRolePermissionEntity.new {
                this.roleId = entity.id
                this.permissionId = permissionEntity.id
            }
        }

    override fun setRolePermission(
        idOrName: String,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = rolesService.findByIdOrName(idOrName) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setRolePermission(entity, permission, enabled)
    }

    override fun setRolePermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setRolePermissions(entity, permissions)
    }

    override fun setRolePermissions(
        entity: RoleEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> =
        setEntityPermissions(entity, permissions) { permissionEntity, enabled ->
            DatabaseRolePermissionEntity.new {
                this.roleId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setRolePermissions(
        idOrName: String,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity =
            rolesService.findByIdOrName(idOrName) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setRolePermissions(entity, permissions)
    }

    override fun unsetRolePermission(
        id: UUID,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetRolePermission(entity, permission)
    }

    override fun unsetRolePermission(
        entity: RoleEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = unsetEntityPermission(entity, permission)

    override fun unsetRolePermission(
        idOrName: String,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity =
            rolesService.findByIdOrName(idOrName) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetRolePermission(entity, permission)
    }

    override fun unsetRolePermissions(
        id: UUID,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetRolePermissions(entity, permissions)
    }

    override fun unsetRolePermissions(
        entity: RoleEntity,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = unsetEntityPermissions(entity, permissions)

    override fun unsetRolePermissions(
        idOrName: String,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity =
            rolesService.findByIdOrName(idOrName) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetRolePermissions(entity, permissions)
    }

    override fun clearRolePermissions(id: UUID): Either<PermissionService.ClearPermissionError, Unit> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.ClearPermissionError.EntityNotFound)
        return clearRolePermissions(entity)
    }

    override fun clearRolePermissions(
        entity: RoleEntity
    ): Either<PermissionService.ClearPermissionError, Unit> = clearEntityPermissions(entity)

    override fun clearRolePermissions(
        idOrName: String
    ): Either<PermissionService.ClearPermissionError, Unit> = either {
        val entity =
            rolesService.findByIdOrName(idOrName) ?: raise(PermissionService.ClearPermissionError.EntityNotFound)
        return clearRolePermissions(entity)
    }

    override fun hasRolePermission(id: UUID, permission: String): Boolean {
        val entity = rolesService.findById(id) ?: return false
        return hasRolePermission(entity, permission)
    }

    override fun hasRolePermission(
        entity: RoleEntity,
        permission: String
    ): Boolean = hasEntityPermission(entity, permission)

    override fun hasRolePermission(idOrName: String, permission: String): Boolean {
        val entity = rolesService.findByIdOrName(idOrName) ?: return false
        return hasRolePermission(entity, permission)
    }

    override fun hasRoleAnyPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = rolesService.findById(id) ?: return false
        return hasRoleAnyPermissions(entity, permissions)
    }

    override fun hasRoleAnyPermissions(
        entity: RoleEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAnyPermissions(entity, permissions)

    override fun hasRoleAnyPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = rolesService.findByIdOrName(idOrName) ?: return false
        return hasRoleAnyPermissions(entity, permissions)
    }

    override fun hasRoleAllPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = rolesService.findById(id) ?: return false
        return hasRoleAllPermissions(entity, permissions)
    }

    override fun hasRoleAllPermissions(
        entity: RoleEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAllPermissions(entity, permissions)

    override fun hasRoleAllPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = rolesService.findByIdOrName(idOrName) ?: return false
        return hasRoleAllPermissions(entity, permissions)
    }

    override fun getRolePermissions(id: UUID): Set<RolePermissionEntity> {
        val entity = rolesService.findById(id) ?: return emptySet()
        return getRolePermissions(entity)
    }

    override fun getRolePermissions(entity: RoleEntity): Set<RolePermissionEntity> {
        return entity.permissions.toSet()
    }

    override fun getRolePermissions(idOrName: String): Set<RolePermissionEntity> {
        val entity = rolesService.findByIdOrName(idOrName) ?: return emptySet()
        return getRolePermissions(entity)
    }

    override fun setPlayerPermission(
        id: UUID,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setPlayerPermission(entity, permission, enabled)
    }

    override fun setPlayerPermission(
        entity: PlayerEntity,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> =
        setEntityPermission(entity, permission, enabled) { permissionEntity ->
            DatabasePlayerPermissionEntity.new {
                this.playerId = entity.id
                this.permissionId = permissionEntity.id
            }
        }

    override fun setPlayerPermission(
        idOrName: String,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity =
            playersService.findByIdOrName(idOrName) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setPlayerPermission(entity, permission, enabled)
    }

    override fun setPlayerPermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setPlayerPermissions(entity, permissions)
    }

    override fun setPlayerPermissions(
        entity: PlayerEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> =
        setEntityPermissions(entity, permissions) { permissionEntity, enabled ->
            DatabasePlayerPermissionEntity.new {
                this.playerId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setPlayerPermissions(
        idOrName: String,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity =
            playersService.findByIdOrName(idOrName) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setPlayerPermissions(entity, permissions)
    }

    override fun unsetPlayerPermission(
        id: UUID,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetPlayerPermission(entity, permission)
    }

    override fun unsetPlayerPermission(
        entity: PlayerEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = unsetEntityPermission(entity, permission)

    override fun unsetPlayerPermission(
        idOrName: String,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity =
            playersService.findByIdOrName(idOrName) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetPlayerPermission(entity, permission)
    }

    override fun unsetPlayerPermissions(
        id: UUID,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetPlayerPermissions(entity, permissions)
    }

    override fun unsetPlayerPermissions(
        entity: PlayerEntity,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = unsetEntityPermissions(entity, permissions)

    override fun unsetPlayerPermissions(
        idOrName: String,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity =
            playersService.findByIdOrName(idOrName) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetPlayerPermissions(entity, permissions)
    }

    override fun clearPlayerPermissions(id: UUID): Either<PermissionService.ClearPermissionError, Unit> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.ClearPermissionError.EntityNotFound)
        return clearPlayerPermissions(entity)
    }

    override fun clearPlayerPermissions(
        entity: PlayerEntity
    ): Either<PermissionService.ClearPermissionError, Unit> = clearEntityPermissions(entity)

    override fun clearPlayerPermissions(
        idOrName: String
    ): Either<PermissionService.ClearPermissionError, Unit> = either {
        val entity =
            playersService.findByIdOrName(idOrName) ?: raise(PermissionService.ClearPermissionError.EntityNotFound)
        return clearPlayerPermissions(entity)
    }

    override fun hasPlayerPermission(id: UUID, permission: String): Boolean {
        val entity = playersService.findById(id) ?: return false
        return hasPlayerPermission(entity, permission)
    }

    override fun hasPlayerPermission(
        entity: PlayerEntity,
        permission: String
    ): Boolean = hasEntityPermission(entity, permission)

    override fun hasPlayerPermission(idOrName: String, permission: String): Boolean {
        val entity = playersService.findByIdOrName(idOrName) ?: return false
        return hasPlayerPermission(entity, permission)
    }

    override fun hasPlayerAnyPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = playersService.findById(id) ?: return false
        return hasPlayerAnyPermissions(entity, permissions)
    }

    override fun hasPlayerAnyPermissions(
        entity: PlayerEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAnyPermissions(entity, permissions)

    override fun hasPlayerAnyPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = playersService.findByIdOrName(idOrName) ?: return false
        return hasPlayerAnyPermissions(entity, permissions)
    }

    override fun hasPlayerAllPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = playersService.findById(id) ?: return false
        return hasPlayerAllPermissions(entity, permissions)
    }

    override fun hasPlayerAllPermissions(
        entity: PlayerEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAllPermissions(entity, permissions)

    override fun hasPlayerAllPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = playersService.findByIdOrName(idOrName) ?: return false
        return hasPlayerAllPermissions(entity, permissions)
    }

    override fun getPlayerPermissions(id: UUID): Set<PlayerPermissionEntity> {
        val entity = playersService.findById(id) ?: return emptySet()
        return getPlayerPermissions(entity)
    }

    override fun getPlayerPermissions(entity: PlayerEntity): Set<PlayerPermissionEntity> {
        return entity.permissions.toSet()
    }

    override fun getPlayerPermissions(idOrName: String): Set<PlayerPermissionEntity> {
        val entity = playersService.findByIdOrName(idOrName) ?: return emptySet()
        return getPlayerPermissions(entity)
    }

    private fun findOrCreatePermissionEntity(permission: String): PermissionEntity {
        return findPermissionByValue(permission) ?: DatabasePermissionEntity.new {
            this.value = permission
            this.type = PermissionEntity.Type.EQUALS
        }
    }

    private fun hasEntityPermission(
        entity: PermissionReference<out SharedPermissionEntity>,
        permission: String
    ): Boolean {
        return transaction(gradeway.database) {
            val entityPermissions = entity.permissions.filter { it.isEnabled }
            for (entityPermission in entityPermissions) {
                if (entityPermission.permission.validatePermission(permission)) {
                    return@transaction true
                }
            }
            false
        }
    }

    private fun hasEntityAllPermissions(
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: Collection<String>
    ): Boolean {
        return transaction(gradeway.database) {
            val entityPermissions = entity.permissions.filter { it.isEnabled }
            for (entityPermission in entityPermissions) {
                if (!permissions.all { entityPermission.permission.validatePermission(it) }) {
                    return@transaction false
                }
            }
            true
        }
    }

    private fun hasEntityAnyPermissions(
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: Collection<String>
    ): Boolean {
        return transaction(gradeway.database) {
            val entityPermissions = entity.permissions.filter { it.isEnabled }
            for (entityPermission in entityPermissions) {
                if (!permissions.any { entityPermission.permission.validatePermission(it) }) {
                    return@transaction false
                }
            }
            true
        }
    }

    private fun setEntityPermission(
        entity: PermissionReference<out SharedPermissionEntity>,
        permission: String,
        enabled: Boolean,
        createNewPermission: (PermissionEntity) -> Unit
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        try {
            transaction(gradeway.database) {
                val existingPermission = entity.permissions.find { it.permission.value == permission }
                if (existingPermission != null) {
                    if (existingPermission !is Entity<*>) {
                        val throwable = Throwable("Entity is not a database Entity")
                        raise(PermissionService.SetPermissionError.Unexpected(throwable))
                    }

                    val isEnabled = existingPermission.isEnabled
                    if (enabled && isEnabled) {
                        raise(PermissionService.SetPermissionError.PermissionAlreadyEnabled)
                    }
                    if (!enabled && !isEnabled) {
                        raise(PermissionService.SetPermissionError.PermissionAlreadyDisabled)
                    }

                    existingPermission.isEnabled = enabled
                    existingPermission.flush()
                    return@transaction
                }
                val permissionEntity = findOrCreatePermissionEntity(permission)
                createNewPermission(permissionEntity)
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.SetPermissionError.Unexpected(throwable))
        }
    }

    private fun setEntityPermissions(
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: Map<String, Boolean>,
        createNewPermission: (PermissionEntity, Boolean) -> Unit
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        try {
            transaction(gradeway.database) {
                val existingPermissions = entity.permissions
                for ((permissionValue, enabled) in permissions) {
                    if (existingPermissions.any { it.permission.value == permissionValue }) continue
                    val permissionEntity = findOrCreatePermissionEntity(permissionValue)
                    createNewPermission(permissionEntity, enabled)
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.BulkSetPermissionError.Unexpected(throwable))
        }
    }

    private fun unsetEntityPermission(
        entity: PermissionReference<out SharedPermissionEntity>,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        try {
            transaction(gradeway.database) {
                val entityPermission = entity.permissions.firstOrNull { it.permission.value == permission }
                if (entityPermission == null) {
                    raise(PermissionService.UnsetPermissionError.PermissionNotFound)
                }
                if (entityPermission !is Entity<*>) {
                    val throwable = Throwable("Entity is not a database Entity")
                    raise(PermissionService.UnsetPermissionError.Unexpected(throwable))
                }
                entityPermission.delete()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.UnsetPermissionError.Unexpected(throwable))
        }
    }

    private fun unsetEntityPermissions(
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: List<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        try {
            transaction(gradeway.database) {
                val entityPermissions = entity.permissions.filter { it.permission.value in permissions }
                entityPermissions.forEach { entityPermissionEntity ->
                    if (entityPermissionEntity is Entity<*>) {
                        entityPermissionEntity.delete()
                    }
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.BulkUnsetPermissionError.Unexpected(throwable))
        }
    }

    private fun clearEntityPermissions(
        entity: PermissionReference<out SharedPermissionEntity>
    ): Either<PermissionService.ClearPermissionError, Unit> = either {
        try {
            transaction(gradeway.database) {
                entity.permissions.forEach { entityPermissionEntity ->
                    if (entityPermissionEntity is Entity<*>) {
                        entityPermissionEntity.delete()
                    }
                }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ClearPermissionError.Unexpected(throwable))
        }
    }

    private fun isTemplateNameValid(name: String): Boolean {
        if (name.isNotBlank()) return true
        if (name.none { it.isWhitespace() }) return true
        if (name.length in 1..TableConstants.PERMISSION_TEMPLATES_TABLE_MAX_NAME_LENGTH) return true
        return false
    }
}
