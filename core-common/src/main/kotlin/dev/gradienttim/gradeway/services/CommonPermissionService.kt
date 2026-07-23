/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupPermissionEntity
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupPermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.group.GroupPermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.*
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerPermissionEntity
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerPermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.player.PlayerPermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.role.DatabaseRolePermissionEntity
import dev.gradienttim.gradeway.database.models.role.DatabaseRolePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.role.RolePermissionTemplatesTable
import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.group.GroupPermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplatePermissionEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.player.PlayerPermissionEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionTemplateEntity
import dev.gradienttim.gradeway.extensions.eqAsStr
import dev.gradienttim.gradeway.extensions.isNameValid
import dev.gradienttim.gradeway.extensions.isUuid
import dev.gradienttim.gradeway.messaging.payloads.*
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
class CommonPermissionService<TPlatformConfig>(val gradeway: CommonGradeway<TPlatformConfig>) : PermissionService,
    KoinComponent {
    private val rolesService: RoleService by inject()
    private val groupsService: GroupService by inject()
    private val playersService: PlayerService by inject()

    init {
        gradeway.messaging.subscribe { payload -> invalidateFor(payload) }
    }

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
        val entity = findPermissionByIdOrValue(idOrValue)
            ?: raise(PermissionService.DeletePermissionError.EntityNotFound)
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
        val entity = findPermissionByIdOrValue(idOrValue)
            ?: raise(PermissionService.UpdatePermissionTypeError.EntityNotFound)
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
        if (!name.isNameValid(TableConstants.PERMISSION_TEMPLATES_TABLE_MAX_NAME_LENGTH)) {
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

        if (!name.isNameValid(TableConstants.PERMISSION_TEMPLATES_TABLE_MAX_NAME_LENGTH)) {
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
        if (!name.isNameValid(TableConstants.PERMISSION_TEMPLATES_TABLE_MAX_NAME_LENGTH)) {
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
        if (!value.isUuid() && !value.isNameValid(TableConstants.PERMISSION_TEMPLATES_TABLE_MAX_NAME_LENGTH)) {
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
        templateId: UUID,
        permissionIdOrValue: String
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template =
            findTemplateById(templateId) ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(template, permission)
    }

    override fun addPermissionToTemplate(
        template: PermissionTemplateEntity,
        permissionIdOrValue: String
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(template, permission)
    }

    override fun addPermissionToTemplate(
        templateIdOrName: String,
        permissionIdOrValue: String
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(template, permission)
    }

    override fun addPermissionToTemplate(
        template: PermissionTemplateEntity,
        permission: PermissionEntity
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        transaction(gradeway.database) {
            if (template.permissions.any { it.permissionId == permission.id }) {
                raise(PermissionService.AddPermissionToTemplateError.PermissionAlreadyExists)
            }

            try {
                DatabasePermissionTemplatePermissionEntity.new {
                    this.templateId = template.id
                    this.permissionId = permission.id
                }
            } catch (throwable: Throwable) {
                raise(PermissionService.AddPermissionToTemplateError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PermissionTemplatePermissionChangedPayload(
                template.id.value.toString(),
                permission.id.value.toString(),
                MessagingAction.CREATED
            )
        )
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
        templateId: UUID,
        permissionIdOrValue: String
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template =
            findTemplateById(templateId) ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(template, permission)
    }

    override fun removePermissionFromTemplate(
        template: PermissionTemplateEntity,
        permissionIdOrValue: String
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(template, permission)
    }

    override fun removePermissionFromTemplate(
        templateIdOrName: String,
        permissionIdOrValue: String
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(template, permission)
    }

    override fun removePermissionFromTemplate(
        template: PermissionTemplateEntity,
        permission: PermissionEntity
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        transaction(gradeway.database) {
            val templatePermission = template.permissions.find { it.permissionId == permission.id }
            if (templatePermission == null) {
                raise(PermissionService.RemovePermissionFromTemplateError.PermissionNotExists)
            }

            if (templatePermission !is DatabasePermissionTemplatePermissionEntity) {
                val throwable = Throwable("Entity is not a type of DatabasePermissionTemplatePermissionEntity")
                raise(PermissionService.RemovePermissionFromTemplateError.Unexpected(throwable))
            }

            try {
                templatePermission.delete()
            } catch (throwable: Throwable) {
                raise(PermissionService.RemovePermissionFromTemplateError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PermissionTemplatePermissionChangedPayload(
                template.id.value.toString(),
                permission.id.value.toString(),
                MessagingAction.DELETED
            )
        )
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
        transaction(gradeway.database) {
            try {
                template.permissions.filterIsInstance<DatabasePermissionTemplatePermissionEntity>().forEach {
                    it.delete()
                }

                gradeway.messaging.publish(
                    PermissionTemplatePermissionsClearedPayload(template.id.value.toString())
                )
            } catch (throwable: Throwable) {
                raise(PermissionService.ClearPermissionsFromTemplateError.Unexpected(throwable))
            }
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

        transaction(gradeway.database) {
            val templateEntitiesCount = DatabaseRolePermissionTemplateEntity.find {
                (RolePermissionTemplatesTable.roleId eq role.id) and
                        (RolePermissionTemplatesTable.permissionTemplateId eq template.id)
            }.limit(1).count()

            if (templateEntitiesCount != 0L) {
                raise(PermissionService.LinkTemplateError.AlreadyLinked)
            }

            try {
                DatabaseRolePermissionTemplateEntity.new {
                    this.roleId = role.id
                    this.permissionTemplateId = template.id
                }
            } catch (throwable: Throwable) {
                raise(PermissionService.LinkTemplateError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PermissionTemplateRoleLinkChangedPayload(
                template.id.value.toString(),
                role.id.value.toString(),
                MessagingAction.CREATED
            )
        )
    }

    override fun linkTemplateToRole(
        templateIdOrName: String,
        roleId: UUID
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToRole(templateEntity, roleId)
    }

    override fun linkTemplateToRole(
        templateIdOrName: String,
        role: RoleEntity
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToRole(templateEntity, role)
    }

    override fun linkTemplateToRole(
        templateId: UUID,
        roleIdOrName: String
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val roleEntity =
            rolesService.findByIdOrName(roleIdOrName) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(templateId, roleEntity)
    }

    override fun linkTemplateToRole(
        template: PermissionTemplateEntity,
        roleIdOrName: String
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val roleEntity =
            rolesService.findByIdOrName(roleIdOrName) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(template, roleEntity)
    }

    override fun linkTemplateToRole(
        templateIdOrName: String,
        roleIdOrName: String
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val roleEntity =
            rolesService.findByIdOrName(roleIdOrName) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(templateEntity, roleEntity)
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
        transaction(gradeway.database) {
            val entityTemplateEntity = DatabaseRolePermissionTemplateEntity.find {
                (RolePermissionTemplatesTable.roleId eq role.id) and
                        (RolePermissionTemplatesTable.permissionTemplateId eq template.id)
            }.limit(1).firstOrNull()

            if (entityTemplateEntity == null) {
                raise(PermissionService.UnlinkTemplateError.NotLinked)
            }

            try {
                entityTemplateEntity.delete()

                gradeway.messaging.publish(
                    PermissionTemplateRoleLinkChangedPayload(
                        template.id.value.toString(),
                        role.id.value.toString(),
                        MessagingAction.DELETED
                    )
                )
            } catch (throwable: Throwable) {
                raise(PermissionService.UnlinkTemplateError.Unexpected(throwable))
            }
        }
    }

    override fun unlinkTemplateFromRole(
        templateIdOrName: String,
        roleId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromRole(templateEntity, roleId)
    }

    override fun unlinkTemplateFromRole(
        templateIdOrName: String,
        role: RoleEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromRole(templateEntity, role)
    }

    override fun unlinkTemplateFromRole(
        templateId: UUID,
        roleIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val roleEntity = rolesService.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(templateId, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        template: PermissionTemplateEntity,
        roleIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val roleEntity = rolesService.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(template, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        templateIdOrName: String,
        roleIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val roleEntity = rolesService.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(templateEntity, roleEntity)
    }

    override fun applyTemplateToRole(
        templateId: UUID,
        roleId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateById(templateId)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val roleEntity = rolesService.findById(roleId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(templateEntity, roleEntity)
    }

    override fun applyTemplateToRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateById(templateId)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
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

    override fun applyTemplateToRole(
        templateIdOrName: String,
        roleId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToRole(templateEntity, roleId)
    }

    override fun applyTemplateToRole(
        templateIdOrName: String,
        role: RoleEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToRole(templateEntity, role)
    }

    override fun applyTemplateToRole(
        templateId: UUID,
        roleIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val roleEntity =
            rolesService.findByIdOrName(roleIdOrName) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(templateId, roleEntity)
    }

    override fun applyTemplateToRole(
        template: PermissionTemplateEntity,
        roleIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val roleEntity =
            rolesService.findByIdOrName(roleIdOrName) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(template, roleEntity)
    }

    override fun applyTemplateToRole(
        templateIdOrName: String,
        roleIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val roleEntity =
            rolesService.findByIdOrName(roleIdOrName) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(templateEntity, roleEntity)
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

    override fun revokeTemplateFromRole(
        templateIdOrName: String,
        roleId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromRole(templateEntity, roleId)
    }

    override fun revokeTemplateFromRole(
        templateIdOrName: String,
        role: RoleEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromRole(templateEntity, role)
    }

    override fun revokeTemplateFromRole(
        templateId: UUID,
        roleIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val roleEntity =
            rolesService.findByIdOrName(roleIdOrName) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(templateId, roleEntity)
    }

    override fun revokeTemplateFromRole(
        template: PermissionTemplateEntity,
        roleIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val roleEntity =
            rolesService.findByIdOrName(roleIdOrName) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(template, roleEntity)
    }

    override fun revokeTemplateFromRole(
        templateIdOrName: String,
        roleIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val roleEntity = rolesService.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(templateEntity, roleEntity)
    }

    override fun linkTemplateToPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val playerEntity = playersService.findById(playerId)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
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
        val playerEntity = playersService.findById(playerId)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(template, playerEntity)
    }

    override fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit> = either<PermissionService.LinkTemplateError, Unit> {
        if (!template.assignedTo.allowForPlayer) {
            raise(PermissionService.LinkTemplateError.WrongAssignedTo)
        }

        transaction(gradeway.database) {
            val templateEntitiesCount = DatabasePlayerPermissionTemplateEntity.find {
                (PlayerPermissionTemplatesTable.playerId eq player.id) and
                        (PlayerPermissionTemplatesTable.permissionTemplateId eq template.id)
            }.limit(1).count()

            if (templateEntitiesCount != 0L) {
                raise(PermissionService.LinkTemplateError.AlreadyLinked)
            }

            try {
                DatabasePlayerPermissionTemplateEntity.new {
                    this.playerId = player.id
                    this.permissionTemplateId = template.id
                }
            } catch (throwable: Throwable) {
                raise(PermissionService.LinkTemplateError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PermissionTemplatePlayerLinkChangedPayload(
                template.id.value.toString(),
                player.id.value.toString(),
                MessagingAction.CREATED
            )
        )
    }

    override fun linkTemplateToPlayer(
        templateIdOrName: String,
        playerId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToPlayer(templateEntity, playerId)
    }

    override fun linkTemplateToPlayer(
        templateIdOrName: String,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToPlayer(templateEntity, player)
    }

    override fun linkTemplateToPlayer(
        templateId: UUID,
        playerIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(templateId, playerEntity)
    }

    override fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(template, playerEntity)
    }

    override fun linkTemplateToPlayer(
        templateIdOrName: String,
        playerIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(templateEntity, playerEntity)
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
    }.onRight {
        gradeway.messaging.publish(
            PermissionTemplatePlayerLinkChangedPayload(
                template.id.value.toString(),
                player.id.value.toString(),
                MessagingAction.DELETED
            )
        )
    }

    override fun unlinkTemplateFromPlayer(
        templateIdOrName: String,
        playerId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromPlayer(templateEntity, playerId)
    }

    override fun unlinkTemplateFromPlayer(
        templateIdOrName: String,
        player: PlayerEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromPlayer(templateEntity, player)
    }

    override fun unlinkTemplateFromPlayer(
        templateId: UUID,
        playerIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(templateId, playerEntity)
    }

    override fun unlinkTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(template, playerEntity)
    }

    override fun unlinkTemplateFromPlayer(
        templateIdOrName: String,
        playerIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(templateEntity, playerEntity)
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

    override fun applyTemplateToPlayer(
        templateIdOrName: String,
        playerId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToPlayer(templateEntity, playerId)
    }

    override fun applyTemplateToPlayer(
        templateIdOrName: String,
        player: PlayerEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToPlayer(templateEntity, player)
    }

    override fun applyTemplateToPlayer(
        templateId: UUID,
        playerIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(templateId, playerEntity)
    }

    override fun applyTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(template, playerEntity)
    }

    override fun applyTemplateToPlayer(
        templateIdOrName: String,
        playerIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(templateEntity, playerEntity)
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

    override fun revokeTemplateFromPlayer(
        templateIdOrName: String,
        playerId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromPlayer(templateEntity, playerId)
    }

    override fun revokeTemplateFromPlayer(
        templateIdOrName: String,
        player: PlayerEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromPlayer(templateEntity, player)
    }

    override fun revokeTemplateFromPlayer(
        templateId: UUID,
        playerIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(templateId, playerEntity)
    }

    override fun revokeTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(template, playerEntity)
    }

    override fun revokeTemplateFromPlayer(
        templateIdOrName: String,
        playerIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val playerEntity = playersService.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(templateEntity, playerEntity)
    }

    override fun linkTemplateToGroup(
        templateId: UUID,
        groupId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val groupEntity = groupsService.findById(groupId) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(templateEntity, groupEntity)
    }

    override fun linkTemplateToGroup(
        templateId: UUID,
        group: GroupEntity
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToGroup(templateEntity, group)
    }

    override fun linkTemplateToGroup(
        template: PermissionTemplateEntity,
        groupId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val groupEntity = groupsService.findById(groupId)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(template, groupEntity)
    }

    override fun linkTemplateToGroup(
        template: PermissionTemplateEntity,
        group: GroupEntity
    ): Either<PermissionService.LinkTemplateError, Unit> = either<PermissionService.LinkTemplateError, Unit> {
        if (!template.assignedTo.allowForGroup) {
            raise(PermissionService.LinkTemplateError.WrongAssignedTo)
        }

        transaction(gradeway.database) {
            val templateEntitiesCount = DatabaseGroupPermissionTemplateEntity.find {
                (GroupPermissionTemplatesTable.groupId eq group.id) and
                        (GroupPermissionTemplatesTable.permissionTemplateId eq template.id)
            }.limit(1).count()

            if (templateEntitiesCount != 0L) {
                raise(PermissionService.LinkTemplateError.AlreadyLinked)
            }

            try {
                DatabaseGroupPermissionTemplateEntity.new {
                    this.groupId = group.id
                    this.permissionTemplateId = template.id
                }
            } catch (throwable: Throwable) {
                raise(PermissionService.LinkTemplateError.Unexpected(throwable))
            }
        }
    }.onRight {
        gradeway.messaging.publish(
            PermissionTemplateGroupLinkChangedPayload(
                template.id.value.toString(),
                group.id.value.toString(),
                MessagingAction.CREATED
            )
        )
    }

    override fun linkTemplateToGroup(
        templateIdOrName: String,
        groupId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToGroup(templateEntity, groupId)
    }

    override fun linkTemplateToGroup(
        templateIdOrName: String,
        group: GroupEntity
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToGroup(templateEntity, group)
    }

    override fun linkTemplateToGroup(
        templateId: UUID,
        groupIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(templateId, groupEntity)
    }

    override fun linkTemplateToGroup(
        template: PermissionTemplateEntity,
        groupIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(template, groupEntity)
    }

    override fun linkTemplateToGroup(
        templateIdOrName: String,
        groupIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(templateEntity, groupEntity)
    }

    override fun unlinkTemplateFromGroup(
        templateId: UUID,
        groupId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val group = groupsService.findById(groupId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(template, group)
    }

    override fun unlinkTemplateFromGroup(
        templateId: UUID,
        group: GroupEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromGroup(template, group)
    }

    override fun unlinkTemplateFromGroup(
        template: PermissionTemplateEntity,
        groupId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val group = groupsService.findById(groupId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(template, group)
    }

    override fun unlinkTemplateFromGroup(
        template: PermissionTemplateEntity,
        group: GroupEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val entityTemplateEntity = DatabaseGroupPermissionTemplateEntity.find {
            (GroupPermissionTemplatesTable.groupId eq group.id) and
                    (GroupPermissionTemplatesTable.permissionTemplateId eq template.id)
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
    }.onRight {
        gradeway.messaging.publish(
            PermissionTemplateGroupLinkChangedPayload(
                template.id.value.toString(),
                group.id.value.toString(),
                MessagingAction.DELETED
            )
        )
    }

    override fun unlinkTemplateFromGroup(
        templateIdOrName: String,
        groupId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromGroup(templateEntity, groupId)
    }

    override fun unlinkTemplateFromGroup(
        templateIdOrName: String,
        group: GroupEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromGroup(templateEntity, group)
    }

    override fun unlinkTemplateFromGroup(
        templateId: UUID,
        groupIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(templateId, groupEntity)
    }

    override fun unlinkTemplateFromGroup(
        template: PermissionTemplateEntity,
        groupIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(template, groupEntity)
    }

    override fun unlinkTemplateFromGroup(
        templateIdOrName: String,
        groupIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(templateEntity, groupEntity)
    }

    override fun applyTemplateToGroup(
        templateId: UUID,
        groupId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val group = groupsService.findById(groupId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(template, group)
    }

    override fun applyTemplateToGroup(
        templateId: UUID,
        group: GroupEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToGroup(template, group)
    }

    override fun applyTemplateToGroup(
        template: PermissionTemplateEntity,
        groupId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val group = groupsService.findById(groupId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(template, group)
    }

    override fun applyTemplateToGroup(
        template: PermissionTemplateEntity,
        group: GroupEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        if (!template.assignedTo.allowForGroup) {
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

                setGroupPermissions(group, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ApplyTemplateError.Unexpected(throwable))
        }
    }

    override fun applyTemplateToGroup(
        templateIdOrName: String,
        groupId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToGroup(templateEntity, groupId)
    }

    override fun applyTemplateToGroup(
        templateIdOrName: String,
        group: GroupEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToGroup(templateEntity, group)
    }

    override fun applyTemplateToGroup(
        templateId: UUID,
        groupIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(templateId, groupEntity)
    }

    override fun applyTemplateToGroup(
        template: PermissionTemplateEntity,
        groupIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(template, groupEntity)
    }

    override fun applyTemplateToGroup(
        templateIdOrName: String,
        groupIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(templateEntity, groupEntity)
    }

    override fun revokeTemplateFromGroup(
        templateId: UUID,
        groupId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val group = groupsService.findById(groupId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(template, group)
    }

    override fun revokeTemplateFromGroup(
        templateId: UUID,
        group: GroupEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromGroup(template, group)
    }

    override fun revokeTemplateFromGroup(
        template: PermissionTemplateEntity,
        groupId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val group = groupsService.findById(groupId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(template, group)
    }

    override fun revokeTemplateFromGroup(
        template: PermissionTemplateEntity,
        group: GroupEntity
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

                unsetGroupPermissions(group, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.RevokeTemplateError.Unexpected(throwable))
        }
    }

    override fun revokeTemplateFromGroup(
        templateIdOrName: String,
        groupId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromGroup(templateEntity, groupId)
    }

    override fun revokeTemplateFromGroup(
        templateIdOrName: String,
        group: GroupEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromGroup(templateEntity, group)
    }

    override fun revokeTemplateFromGroup(
        templateId: UUID,
        groupIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(templateId, groupEntity)
    }

    override fun revokeTemplateFromGroup(
        template: PermissionTemplateEntity,
        groupIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(template, groupEntity)
    }

    override fun revokeTemplateFromGroup(
        templateIdOrName: String,
        groupIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val groupEntity =
            groupsService.findByIdOrName(groupIdOrName) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(templateEntity, groupEntity)
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
                this.isEnabled = enabled
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
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetRolePermissions(entity, permissions)
    }

    override fun unsetRolePermissions(
        entity: RoleEntity,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = unsetEntityPermissions(entity, permissions)

    override fun unsetRolePermissions(
        idOrName: String,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity =
            rolesService.findByIdOrName(idOrName) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetRolePermissions(entity, permissions)
    }

    override fun clearRolePermissions(id: UUID): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = rolesService.findById(id) ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearRolePermissions(entity)
    }

    override fun clearRolePermissions(
        entity: RoleEntity
    ): Either<PermissionService.ClearPermissionsError, Unit> = clearEntityPermissions(entity)

    override fun clearRolePermissions(
        idOrName: String
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity =
            rolesService.findByIdOrName(idOrName) ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
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
        return transaction(gradeway.database) {
            entity.permissions.toSet()
        }
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
                this.isEnabled = enabled
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
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetPlayerPermissions(entity, permissions)
    }

    override fun unsetPlayerPermissions(
        entity: PlayerEntity,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = unsetEntityPermissions(entity, permissions)

    override fun unsetPlayerPermissions(
        idOrName: String,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity =
            playersService.findByIdOrName(idOrName) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetPlayerPermissions(entity, permissions)
    }

    override fun clearPlayerPermissions(id: UUID): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = playersService.findById(id) ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearPlayerPermissions(entity)
    }

    override fun clearPlayerPermissions(
        entity: PlayerEntity
    ): Either<PermissionService.ClearPermissionsError, Unit> = clearEntityPermissions(entity)

    override fun clearPlayerPermissions(
        idOrName: String
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity =
            playersService.findByIdOrName(idOrName) ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
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
        return transaction(gradeway.database) {
            entity.permissions.toSet()
        }
    }

    override fun getPlayerPermissions(idOrName: String): Set<PlayerPermissionEntity> {
        val entity = playersService.findByIdOrName(idOrName) ?: return emptySet()
        return getPlayerPermissions(entity)
    }

    override fun setGroupPermission(
        id: UUID,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = groupsService.findById(id) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setGroupPermission(entity, permission, enabled)
    }

    override fun setGroupPermission(
        entity: GroupEntity,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> =
        setEntityPermission(entity, permission, enabled) { permissionEntity ->
            DatabaseGroupPermissionEntity.new {
                this.groupId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setGroupPermission(
        idOrName: String,
        permission: String,
        enabled: Boolean
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = groupsService.findByIdOrName(idOrName)
            ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setGroupPermission(entity, permission, enabled)
    }

    override fun setGroupPermissions(
        id: UUID,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = groupsService.findById(id) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setGroupPermissions(entity, permissions)
    }

    override fun setGroupPermissions(
        entity: GroupEntity,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> =
        setEntityPermissions(entity, permissions) { permissionEntity, enabled ->
            DatabaseGroupPermissionEntity.new {
                this.groupId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setGroupPermissions(
        idOrName: String,
        permissions: Map<String, Boolean>
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = groupsService.findByIdOrName(idOrName)
            ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setGroupPermissions(entity, permissions)
    }

    override fun unsetGroupPermission(
        id: UUID,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = groupsService.findById(id) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetGroupPermission(entity, permission)
    }

    override fun unsetGroupPermission(
        entity: GroupEntity,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = unsetEntityPermission(entity, permission)

    override fun unsetGroupPermission(
        idOrName: String,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = groupsService.findByIdOrName(idOrName)
            ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetGroupPermission(entity, permission)
    }

    override fun unsetGroupPermissions(
        id: UUID,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = groupsService.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetGroupPermissions(entity, permissions)
    }

    override fun unsetGroupPermissions(
        entity: GroupEntity,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = unsetEntityPermissions(entity, permissions)

    override fun unsetGroupPermissions(
        idOrName: String,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = groupsService.findByIdOrName(idOrName)
            ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetGroupPermissions(entity, permissions)
    }

    override fun clearGroupPermissions(id: UUID): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = groupsService.findById(id) ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearGroupPermissions(entity)
    }

    override fun clearGroupPermissions(
        entity: GroupEntity
    ): Either<PermissionService.ClearPermissionsError, Unit> = clearEntityPermissions(entity)

    override fun clearGroupPermissions(
        idOrName: String
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = groupsService.findByIdOrName(idOrName)
            ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearGroupPermissions(entity)
    }

    override fun hasGroupPermission(id: UUID, permission: String): Boolean {
        val entity = groupsService.findById(id) ?: return false
        return hasGroupPermission(entity, permission)
    }

    override fun hasGroupPermission(
        entity: GroupEntity,
        permission: String
    ): Boolean = hasEntityPermission(entity, permission)

    override fun hasGroupPermission(idOrName: String, permission: String): Boolean {
        val entity = groupsService.findByIdOrName(idOrName) ?: return false
        return hasGroupPermission(entity, permission)
    }

    override fun hasGroupAnyPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = groupsService.findById(id) ?: return false
        return hasGroupAnyPermissions(entity, permissions)
    }

    override fun hasGroupAnyPermissions(
        entity: GroupEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAnyPermissions(entity, permissions)

    override fun hasGroupAnyPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = groupsService.findByIdOrName(idOrName) ?: return false
        return hasGroupAnyPermissions(entity, permissions)
    }

    override fun hasGroupAllPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = groupsService.findById(id) ?: return false
        return hasGroupAllPermissions(entity, permissions)
    }

    override fun hasGroupAllPermissions(
        entity: GroupEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAllPermissions(entity, permissions)

    override fun hasGroupAllPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = groupsService.findByIdOrName(idOrName) ?: return false
        return hasGroupAllPermissions(entity, permissions)
    }

    override fun getGroupPermissions(id: UUID): Set<GroupPermissionEntity> {
        val entity = groupsService.findById(id) ?: return emptySet()
        return getGroupPermissions(entity)
    }

    override fun getGroupPermissions(entity: GroupEntity): Set<GroupPermissionEntity> {
        return transaction(gradeway.database) {
            entity.permissions.toSet()
        }
    }

    override fun getGroupPermissions(idOrName: String): Set<GroupPermissionEntity> {
        val entity = groupsService.findByIdOrName(idOrName) ?: return emptySet()
        return getGroupPermissions(entity)
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
            permissions.all { permission -> entityPermissions.any { it.permission.validatePermission(permission) } }
        }
    }

    private fun hasEntityAnyPermissions(
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: Collection<String>
    ): Boolean {
        return transaction(gradeway.database) {
            val entityPermissions = entity.permissions.filter { it.isEnabled }
            permissions.any { permission -> entityPermissions.any { it.permission.validatePermission(permission) } }
        }
    }

    /**
     * Reacts to a [MessagingPayload] (whether it originated locally or on another server) by
     * invalidating the effective-permission caches it affects.
     *
     * Anything that isn't specific to a single player (a role, group, permission, or permission
     * template changing) clears all three caches in full rather than tracing which players are
     * actually affected through arbitrary role/group inheritance chains — edits are rare relative
     * to permission checks, and a full clear only costs a lazy recomputing on next access, not eager
     * work. Player-specific payloads only remove that one player's cached entry.
     */
    private fun invalidateFor(payload: MessagingPayload) {
        when (payload) {
            is PlayerChangedPayload -> invalidatePlayer(payload.playerId)
            is PlayerRoleChangedPayload -> invalidatePlayer(payload.playerId)
            is PlayerPermissionChangedPayload -> invalidatePlayer(payload.playerId)
            is PlayerAttributeChangedPayload -> invalidatePlayer(payload.playerId)

            is PlayerPermissionsClearedPayload -> invalidatePlayer(payload.playerId)
            is PlayerAttributesClearedPayload -> invalidatePlayer(payload.playerId)

            is RoleChangedPayload,
            is RolePermissionChangedPayload,
            is RolePermissionsClearedPayload,
            is RoleParentChangedPayload,
            is RoleAttributeChangedPayload,
            is RoleAttributesClearedPayload,
            is GroupChangedPayload,
            is GroupRoleChangedPayload,
            is GroupPermissionChangedPayload,
            is GroupPermissionsClearedPayload,
            is PermissionChangedPayload,
            is PermissionValueChangedPayload,
            is PermissionTypeChangedPayload,
            is PermissionTemplateChangedPayload,
            is PermissionTemplatePermissionChangedPayload,
            is PermissionTemplatePermissionsClearedPayload,
            is PermissionTemplateRoleLinkChangedPayload,
            is PermissionTemplateGroupLinkChangedPayload,
            is PermissionTemplatePlayerLinkChangedPayload,
            is CacheFlushPayload -> gradeway.caches.invalidateEntityEffectivePermissions()
        }
    }

    private fun invalidatePlayer(playerId: String) {
        val uuid = runCatching { UUID.fromString(playerId) }.getOrNull() ?: return
        gradeway.caches.playerPermissions.invalidate(uuid)
        gradeway.caches.playerEffectivePermissions.invalidate(uuid)
    }

    override fun getEffectiveRolePermissions(id: UUID): Set<PermissionEntity> {
        val entity = rolesService.findById(id) ?: return emptySet()
        return getEffectiveRolePermissions(entity)
    }

    override fun getEffectiveRolePermissions(entity: RoleEntity): Set<PermissionEntity> {
        return gradeway.caches.roleEffectivePermissions.get(entity.id.value)
    }

    override fun getEffectiveRolePermissions(idOrName: String): Set<PermissionEntity> {
        val entity = rolesService.findByIdOrName(idOrName) ?: return emptySet()
        return getEffectiveRolePermissions(entity)
    }

    override fun hasEffectiveRolePermission(id: UUID, permission: String): Boolean {
        val entity = rolesService.findById(id) ?: return false
        return hasEffectiveRolePermission(entity, permission)
    }

    override fun hasEffectiveRolePermission(entity: RoleEntity, permission: String): Boolean {
        return getEffectiveRolePermissions(entity).any { it.validatePermission(permission) }
    }

    override fun hasEffectiveRolePermission(idOrName: String, permission: String): Boolean {
        val entity = rolesService.findByIdOrName(idOrName) ?: return false
        return hasEffectiveRolePermission(entity, permission)
    }

    override fun getEffectiveGroupPermissions(id: UUID): Set<PermissionEntity> {
        val entity = groupsService.findById(id) ?: return emptySet()
        return getEffectiveGroupPermissions(entity)
    }

    override fun getEffectiveGroupPermissions(entity: GroupEntity): Set<PermissionEntity> {
        return gradeway.caches.groupEffectivePermissions.get(entity.id.value)
    }

    override fun getEffectiveGroupPermissions(idOrName: String): Set<PermissionEntity> {
        val entity = groupsService.findByIdOrName(idOrName) ?: return emptySet()
        return getEffectiveGroupPermissions(entity)
    }

    override fun hasEffectiveGroupPermission(id: UUID, permission: String): Boolean {
        val entity = groupsService.findById(id) ?: return false
        return hasEffectiveGroupPermission(entity, permission)
    }

    override fun hasEffectiveGroupPermission(entity: GroupEntity, permission: String): Boolean {
        return getEffectiveGroupPermissions(entity).any { it.validatePermission(permission) }
    }

    override fun hasEffectiveGroupPermission(idOrName: String, permission: String): Boolean {
        val entity = groupsService.findByIdOrName(idOrName) ?: return false
        return hasEffectiveGroupPermission(entity, permission)
    }

    override fun getEffectivePlayerPermissions(id: UUID): Set<PermissionEntity> {
        val entity = playersService.findById(id) ?: return emptySet()
        return getEffectivePlayerPermissions(entity)
    }

    override fun getEffectivePlayerPermissions(entity: PlayerEntity): Set<PermissionEntity> {
        return gradeway.caches.playerEffectivePermissions.get(entity.id.value)
    }

    override fun getEffectivePlayerPermissions(idOrName: String): Set<PermissionEntity> {
        val entity = playersService.findByIdOrName(idOrName) ?: return emptySet()
        return getEffectivePlayerPermissions(entity)
    }

    override fun hasEffectivePlayerPermission(id: UUID, permission: String): Boolean {
        val entity = playersService.findById(id) ?: return false
        return hasEffectivePlayerPermission(entity, permission)
    }

    override fun hasEffectivePlayerPermission(entity: PlayerEntity, permission: String): Boolean {
        return getEffectivePlayerPermissions(entity).any { it.validatePermission(permission) }
    }

    override fun hasEffectivePlayerPermission(idOrName: String, permission: String): Boolean {
        val entity = playersService.findByIdOrName(idOrName) ?: return false
        return hasEffectivePlayerPermission(entity, permission)
    }

    /**
     * Publishes the [MessagingPayload] matching the concrete kind of [entity] (role, group, or
     * player) for a single permission change. Shared by every set/unset/bulk/clear code path
     * below, since they all ultimately operate on a [PermissionReference] without otherwise
     * knowing which concrete entity kind they were called for.
     */
    private fun publishPermissionChanged(entity: PermissionReference<out SharedPermissionEntity>, permission: String) {
        val payload: MessagingPayload = when (entity) {
            is RoleEntity -> RolePermissionChangedPayload(entity.id.value.toString(), permission)
            is GroupEntity -> GroupPermissionChangedPayload(entity.id.value.toString(), permission)
            is PlayerEntity -> PlayerPermissionChangedPayload(entity.id.value.toString(), permission)
            else -> return
        }
        gradeway.messaging.publish(payload)
    }

    /**
     * Publishes a single general "all permissions cleared" [MessagingPayload] for [entity],
     * matching its concrete kind (role, group, or player). Used by bulk-clear code paths instead
     * of publishing one [publishPermissionChanged] per removed permission, since receivers treat
     * any payload as an invalidation signal and re-read current state rather than apply the
     * payload's fields directly (see [MessagingPayload]).
     */
    private fun publishPermissionsCleared(entity: PermissionReference<out SharedPermissionEntity>) {
        val payload: MessagingPayload = when (entity) {
            is RoleEntity -> RolePermissionsClearedPayload(entity.id.value.toString())
            is GroupEntity -> GroupPermissionsClearedPayload(entity.id.value.toString())
            is PlayerEntity -> PlayerPermissionsClearedPayload(entity.id.value.toString())
            else -> return
        }
        gradeway.messaging.publish(payload)
    }

    private fun setEntityPermission(
        entity: PermissionReference<out SharedPermissionEntity>,
        permission: String,
        enabled: Boolean,
        createNewPermission: (PermissionEntity) -> Unit
    ): Either<PermissionService.SetPermissionError, Unit> = either {
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

                try {
                    existingPermission.isEnabled = enabled
                    existingPermission.flush()

                    publishPermissionChanged(entity, permission)
                } catch (throwable: Throwable) {
                    raise(PermissionService.SetPermissionError.Unexpected(throwable))
                }
                return@transaction
            }

            try {
                val permissionEntity = findOrCreatePermissionEntity(permission)
                createNewPermission(permissionEntity)

                publishPermissionChanged(entity, permission)
            } catch (throwable: Throwable) {
                raise(PermissionService.SetPermissionError.Unexpected(throwable))
            }
        }
    }

    private fun setEntityPermissions(
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: Map<String, Boolean>,
        createNewPermission: (PermissionEntity, Boolean) -> Unit
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val createdPermissions = try {
            transaction(gradeway.database) {
                val existingPermissions = entity.permissions
                val created = mutableListOf<String>()
                for ((permissionValue, enabled) in permissions) {
                    if (existingPermissions.any { it.permission.value == permissionValue }) continue
                    val permissionEntity = findOrCreatePermissionEntity(permissionValue)
                    createNewPermission(permissionEntity, enabled)
                    created += permissionValue
                }
                created
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.BulkSetPermissionError.Unexpected(throwable))
        }

        createdPermissions.forEach { publishPermissionChanged(entity, it) }
    }

    private fun unsetEntityPermission(
        entity: PermissionReference<out SharedPermissionEntity>,
        permission: String
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        transaction(gradeway.database) {
            val entityPermission = entity.permissions.firstOrNull { it.permission.value == permission }
            if (entityPermission == null) {
                raise(PermissionService.UnsetPermissionError.PermissionNotFound)
            }
            if (entityPermission !is Entity<*>) {
                val throwable = Throwable("Entity is not a database Entity")
                raise(PermissionService.UnsetPermissionError.Unexpected(throwable))
            }
            try {
                entityPermission.delete()

                publishPermissionChanged(entity, permission)
            } catch (throwable: Throwable) {
                raise(PermissionService.UnsetPermissionError.Unexpected(throwable))
            }
        }
    }

    private fun unsetEntityPermissions(
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: Collection<String>
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val deletedPermissions = try {
            transaction(gradeway.database) {
                val entityPermissions = entity.permissions.filter { it.permission.value in permissions }
                entityPermissions.forEach { entityPermissionEntity ->
                    if (entityPermissionEntity is Entity<*>) {
                        entityPermissionEntity.delete()
                    }
                }
                entityPermissions.map { it.permission.value }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.BulkUnsetPermissionError.Unexpected(throwable))
        }

        deletedPermissions.forEach { publishPermissionChanged(entity, it) }
    }

    private fun clearEntityPermissions(
        entity: PermissionReference<out SharedPermissionEntity>
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        transaction(gradeway.database) {
            try {
                entity.permissions.forEach { entityPermissionEntity ->
                    if (entityPermissionEntity is Entity<*>) {
                        entityPermissionEntity.delete()
                    }
                }

                publishPermissionsCleared(entity)
            } catch (throwable: Throwable) {
                raise(PermissionService.ClearPermissionsError.Unexpected(throwable))
            }
        }
    }
}
