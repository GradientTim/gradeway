/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.AuditRecorder
import dev.gradienttim.gradeway.audit.Auditable
import dev.gradienttim.gradeway.audit.audited
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupPermissionEntity
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupPermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.group.GroupPermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.*
import dev.gradienttim.gradeway.database.models.player.*
import dev.gradienttim.gradeway.database.models.role.DatabaseRolePermissionEntity
import dev.gradienttim.gradeway.database.models.role.DatabaseRolePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.role.RolePermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import dev.gradienttim.gradeway.entity.SharedPermissionTemplateEntity
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
import dev.gradienttim.gradeway.extensions.isIntegrityConstraintViolation
import dev.gradienttim.gradeway.extensions.isNameValid
import dev.gradienttim.gradeway.extensions.isUuid
import dev.gradienttim.gradeway.internal.PermissionGrantCollector
import dev.gradienttim.gradeway.messaging.payloads.*
import dev.gradienttim.gradeway.reference.PermissionReference
import dev.gradienttim.gradeway.reference.PermissionTemplateReference
import dev.gradienttim.gradeway.services.permission.PermissionInspectionService.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.emptySized
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

@Suppress("LargeClass", "TooManyFunctions")
class CommonPermissionService(
    val gradeway: CommonGradeway<*>
) : PermissionService {
    init {
        gradeway.messaging.subscribe { payload -> invalidateFor(payload) }
    }

    override fun createPermission(
        actor: Actor,
        value: String,
        type: PermissionEntity.Type,
    ): Either<PermissionService.CreatePermissionError, PermissionEntity> = gradeway.audited(actor) { audit ->
        try {
            transaction(gradeway.database) {
                DatabasePermissionEntity.new {
                    this.value = value
                    this.type = type
                }.also { audit.created(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(PermissionService.CreatePermissionError.AlreadyExists)
            }
            raise(PermissionService.CreatePermissionError.Unexpected(throwable))
        }
    }

    override fun deletePermission(
        actor: Actor,
        id: UUID,
    ): Either<PermissionService.DeletePermissionError, Unit> = either {
        val entity = findPermissionById(id) ?: raise(PermissionService.DeletePermissionError.EntityNotFound)
        return deletePermission(actor, entity)
    }

    override fun deletePermission(
        actor: Actor,
        entity: PermissionEntity,
    ): Either<PermissionService.DeletePermissionError, Unit> = gradeway.audited(actor) { audit ->
        if (entity !is DatabasePermissionEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionEntity")
            raise(PermissionService.DeletePermissionError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                audit.deleted(entity)
                entity.delete()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.DeletePermissionError.Unexpected(throwable))
        }
    }

    override fun deletePermission(
        actor: Actor,
        idOrValue: String,
    ): Either<PermissionService.DeletePermissionError, Unit> = either {
        val entity = findPermissionByIdOrValue(idOrValue)
            ?: raise(PermissionService.DeletePermissionError.EntityNotFound)
        return deletePermission(actor, entity)
    }

    override fun updatePermissionValue(
        actor: Actor,
        id: UUID,
        value: String,
    ): Either<PermissionService.UpdatePermissionValueError, Boolean> = either {
        val entity = findPermissionById(id) ?: raise(PermissionService.UpdatePermissionValueError.EntityNotFound)
        return updatePermissionValue(actor, entity, value)
    }

    override fun updatePermissionValue(
        actor: Actor,
        entity: PermissionEntity,
        value: String,
    ): Either<PermissionService.UpdatePermissionValueError, Boolean> = gradeway.audited(actor) { audit ->
        if (entity.value == value) {
            raise(PermissionService.UpdatePermissionValueError.ValueAlreadySet)
        }

        if (entity !is DatabasePermissionEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionEntity")
            raise(PermissionService.UpdatePermissionValueError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                entity.value = value
                val flushed = entity.flush()
                audit.updated(entity, before)
                flushed
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.UpdatePermissionValueError.Unexpected(throwable))
        }
    }

    override fun updatePermissionValue(
        actor: Actor,
        idOrValue: String,
        value: String,
    ): Either<PermissionService.UpdatePermissionValueError, Boolean> = either {
        val entity = findPermissionByIdOrValue(idOrValue)
            ?: raise(PermissionService.UpdatePermissionValueError.EntityNotFound)
        return updatePermissionValue(actor, entity, value)
    }

    override fun updatePermissionType(
        actor: Actor,
        id: UUID,
        type: PermissionEntity.Type,
    ): Either<PermissionService.UpdatePermissionTypeError, Boolean> = either {
        val entity = findPermissionById(id) ?: raise(PermissionService.UpdatePermissionTypeError.EntityNotFound)
        return updatePermissionType(actor, entity, type)
    }

    override fun updatePermissionType(
        actor: Actor,
        entity: PermissionEntity,
        type: PermissionEntity.Type,
    ): Either<PermissionService.UpdatePermissionTypeError, Boolean> = gradeway.audited(actor) { audit ->
        if (entity.type == type) {
            raise(PermissionService.UpdatePermissionTypeError.TypeAlreadySet)
        }

        if (entity !is DatabasePermissionEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionEntity")
            raise(PermissionService.UpdatePermissionTypeError.Unexpected(throwable))
        }

        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                entity.type = type
                val flushed = entity.flush()
                audit.updated(entity, before)
                flushed
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.UpdatePermissionTypeError.Unexpected(throwable))
        }
    }

    override fun updatePermissionType(
        actor: Actor,
        idOrValue: String,
        type: PermissionEntity.Type,
    ): Either<PermissionService.UpdatePermissionTypeError, Boolean> = either {
        val entity = findPermissionByIdOrValue(idOrValue)
            ?: raise(PermissionService.UpdatePermissionTypeError.EntityNotFound)
        return updatePermissionType(actor, entity, type)
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
        actor: Actor,
        name: String,
    ): Either<PermissionService.CreateTemplateError, PermissionTemplateEntity> = gradeway.audited(actor) { audit ->
        if (!name.isNameValid(TableConstants.PERMISSION_TEMPLATES_TABLE_MAX_NAME_LENGTH)) {
            raise(PermissionService.CreateTemplateError.InvalidName)
        }

        try {
            transaction(gradeway.database) {
                DatabasePermissionTemplateEntity.new {
                    this.name = name
                }.also { audit.created(it) }
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.CreateTemplateError.Unexpected(throwable))
        }
    }

    override fun deleteTemplate(
        actor: Actor,
        id: UUID,
    ): Either<PermissionService.DeleteTemplateError, PermissionTemplateEntity> = gradeway.audited(actor) { audit ->
        val entity = findTemplateById(id) as? DatabasePermissionTemplateEntity
            ?: raise(PermissionService.DeleteTemplateError.EntityNotFound)

        try {
            transaction(gradeway.database) {
                audit.deleted(entity)
                entity.delete()
                entity
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.DeleteTemplateError.Unexpected(throwable))
        }
    }

    override fun setTemplateName(
        actor: Actor,
        id: UUID,
        name: String,
    ): Either<PermissionService.SetNameTemplateError, Boolean> = either {
        val entity = findTemplateById(id) ?: raise(PermissionService.SetNameTemplateError.EntityNotFound)
        return setTemplateName(actor, entity, name)
    }

    override fun setTemplateName(
        actor: Actor,
        entity: PermissionTemplateEntity,
        name: String,
    ): Either<PermissionService.SetNameTemplateError, Boolean> = gradeway.audited(actor) { audit ->
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
                val before = entity.snapshot()
                entity.name = name
                val flushed = entity.flush()
                audit.updated(entity, before)
                flushed
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.SetNameTemplateError.Unexpected(throwable))
        }
    }

    override fun setTemplateName(
        actor: Actor,
        idOrName: String,
        name: String,
    ): Either<PermissionService.SetNameTemplateError, Boolean> = either {
        val entity = findTemplateByIdOrName(idOrName) ?: raise(PermissionService.SetNameTemplateError.EntityNotFound)
        return setTemplateName(actor, entity, name)
    }

    override fun setTemplateAssignedTo(
        actor: Actor,
        id: UUID,
        assignedTo: PermissionTemplateEntity.AssignedTo,
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean> = either {
        val entity = findTemplateById(id) ?: raise(PermissionService.SetAssignedToTemplateError.EntityNotFound)
        return setTemplateAssignedTo(actor, entity, assignedTo)
    }

    override fun setTemplateAssignedTo(
        actor: Actor,
        entity: PermissionTemplateEntity,
        assignedTo: PermissionTemplateEntity.AssignedTo,
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean> = gradeway.audited(actor) { audit ->
        if (entity !is DatabasePermissionTemplateEntity) {
            val throwable = Throwable("Entity is not a type of DatabasePermissionTemplateEntity")
            raise(PermissionService.SetAssignedToTemplateError.Unexpected(throwable))
        }

        if (entity.assignedTo == assignedTo) {
            raise(PermissionService.SetAssignedToTemplateError.AlreadyAssignedTo)
        }

        try {
            transaction(gradeway.database) {
                val before = entity.snapshot()
                entity.assignedTo = assignedTo
                val flushed = entity.flush()
                audit.updated(entity, before)
                flushed
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.SetAssignedToTemplateError.Unexpected(throwable))
        }
    }

    override fun setTemplateAssignedTo(
        actor: Actor,
        idOrName: String,
        assignedTo: PermissionTemplateEntity.AssignedTo,
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean> = either {
        val entity = findTemplateByIdOrName(idOrName)
            ?: raise(PermissionService.SetAssignedToTemplateError.EntityNotFound)
        return setTemplateAssignedTo(actor, entity, assignedTo)
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
        actor: Actor,
        templateId: UUID,
        permissionId: UUID,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateById(templateId)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        val permission = findPermissionById(permissionId)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(actor, template, permission)
    }

    override fun addPermissionToTemplate(
        actor: Actor,
        template: PermissionTemplateEntity,
        permissionId: UUID,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val permission = findPermissionById(permissionId)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(actor, template, permission)
    }

    override fun addPermissionToTemplate(
        actor: Actor,
        templateId: UUID,
        permission: PermissionEntity,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateById(templateId)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        return addPermissionToTemplate(actor, template, permission)
    }

    override fun addPermissionToTemplate(
        actor: Actor,
        templateIdOrName: String,
        permissionId: UUID,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        val permission = findPermissionById(permissionId)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(actor, template, permission)
    }

    override fun addPermissionToTemplate(
        actor: Actor,
        templateIdOrName: String,
        permission: PermissionEntity,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        return addPermissionToTemplate(actor, template, permission)
    }

    override fun addPermissionToTemplate(
        actor: Actor,
        templateId: UUID,
        permissionIdOrValue: String,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateById(templateId)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(actor, template, permission)
    }

    override fun addPermissionToTemplate(
        actor: Actor,
        template: PermissionTemplateEntity,
        permissionIdOrValue: String,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(actor, template, permission)
    }

    override fun addPermissionToTemplate(
        actor: Actor,
        templateIdOrName: String,
        permissionIdOrValue: String,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.AddPermissionToTemplateError.EntityNotFound)
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.AddPermissionToTemplateError.TargetNotFound)
        return addPermissionToTemplate(actor, template, permission)
    }

    override fun addPermissionToTemplate(
        actor: Actor,
        template: PermissionTemplateEntity,
        permission: PermissionEntity,
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity> =
        gradeway.audited(actor) { audit ->
            try {
                transaction(gradeway.database) {
                    DatabasePermissionTemplatePermissionEntity.new {
                        this.templateId = template.id
                        this.permissionId = permission.id
                    }.also { audit.created(it) }
                }
            } catch (throwable: Throwable) {
                if (throwable.isIntegrityConstraintViolation()) {
                    raise(PermissionService.AddPermissionToTemplateError.PermissionAlreadyExists)
                }
                raise(PermissionService.AddPermissionToTemplateError.Unexpected(throwable))
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
        actor: Actor,
        templateId: UUID,
        permissionId: UUID,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template = findTemplateById(templateId)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        val permission = findPermissionById(permissionId)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(actor, template, permission)
    }

    override fun removePermissionFromTemplate(
        actor: Actor,
        template: PermissionTemplateEntity,
        permissionId: UUID,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val permission = findPermissionById(permissionId)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(actor, template, permission)
    }

    override fun removePermissionFromTemplate(
        actor: Actor,
        templateId: UUID,
        permission: PermissionEntity,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template = findTemplateById(templateId)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        return removePermissionFromTemplate(actor, template, permission)
    }

    override fun removePermissionFromTemplate(
        actor: Actor,
        templateIdOrName: String,
        permissionId: UUID,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        val permission = findPermissionById(permissionId)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(actor, template, permission)
    }

    override fun removePermissionFromTemplate(
        actor: Actor,
        templateIdOrName: String,
        permission: PermissionEntity,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        return removePermissionFromTemplate(actor, template, permission)
    }

    override fun removePermissionFromTemplate(
        actor: Actor,
        templateId: UUID,
        permissionIdOrValue: String,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template =
            findTemplateById(templateId) ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(actor, template, permission)
    }

    override fun removePermissionFromTemplate(
        actor: Actor,
        template: PermissionTemplateEntity,
        permissionIdOrValue: String,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(actor, template, permission)
    }

    override fun removePermissionFromTemplate(
        actor: Actor,
        templateIdOrName: String,
        permissionIdOrValue: String,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = either {
        val template = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.EntityNotFound)
        val permission = findPermissionByIdOrValue(permissionIdOrValue)
            ?: raise(PermissionService.RemovePermissionFromTemplateError.TargetNotFound)
        return removePermissionFromTemplate(actor, template, permission)
    }

    override fun removePermissionFromTemplate(
        actor: Actor,
        template: PermissionTemplateEntity,
        permission: PermissionEntity,
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit> = gradeway.audited(actor) { audit ->
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
                audit.deleted(templatePermission)
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
        actor: Actor,
        templateId: UUID,
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit> = either {
        val template = findTemplateById(templateId)
            ?: raise(PermissionService.ClearPermissionsFromTemplateError.EntityNotFound)
        return clearPermissionsFromTemplate(actor, template)
    }

    override fun clearPermissionsFromTemplate(
        actor: Actor,
        template: PermissionTemplateEntity,
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit> = gradeway.audited(actor) { audit ->
        transaction(gradeway.database) {
            try {
                template.permissions.filterIsInstance<DatabasePermissionTemplatePermissionEntity>().forEach {
                    audit.deleted(it)
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
        actor: Actor,
        idOrName: String,
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit> = either {
        val template = findTemplateByIdOrName(idOrName)
            ?: raise(PermissionService.ClearPermissionsFromTemplateError.EntityNotFound)
        return clearPermissionsFromTemplate(actor, template)
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
        actor: Actor,
        templateId: UUID,
        roleId: UUID,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val roleEntity = gradeway.roles.findById(roleId) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(actor, templateEntity, roleEntity)
    }

    override fun linkTemplateToRole(
        actor: Actor,
        templateId: UUID,
        role: RoleEntity,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToRole(actor, templateEntity, role)
    }

    override fun linkTemplateToRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        roleId: UUID,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val roleEntity = gradeway.roles.findById(roleId) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(actor, template, roleEntity)
    }

    override fun linkTemplateToRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        role: RoleEntity,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = gradeway.audited(actor) { audit ->
        if (!template.assignedTo.allowForRole) {
            raise(PermissionService.LinkTemplateError.WrongAssignedTo)
        }

        try {
            transaction(gradeway.database) {
                DatabaseRolePermissionTemplateEntity.new {
                    this.roleId = role.id
                    this.permissionTemplateId = template.id
                }.also { audit.created(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(PermissionService.LinkTemplateError.AlreadyLinked)
            }
            raise(PermissionService.LinkTemplateError.Unexpected(throwable))
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
        actor: Actor,
        templateIdOrName: String,
        roleId: UUID,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToRole(actor, templateEntity, roleId)
    }

    override fun linkTemplateToRole(
        actor: Actor,
        templateIdOrName: String,
        role: RoleEntity,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToRole(actor, templateEntity, role)
    }

    override fun linkTemplateToRole(
        actor: Actor,
        templateId: UUID,
        roleIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(actor, templateId, roleEntity)
    }

    override fun linkTemplateToRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        roleIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(actor, template, roleEntity)
    }

    override fun linkTemplateToRole(
        actor: Actor,
        templateIdOrName: String,
        roleIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToRole(actor, templateEntity, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        actor: Actor,
        templateId: UUID,
        roleId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val roleEntity = gradeway.roles.findById(roleId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(actor, templateEntity, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        actor: Actor,
        templateId: UUID,
        role: RoleEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromRole(actor, templateEntity, role)
    }

    override fun unlinkTemplateFromRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        roleId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val roleEntity = gradeway.roles.findById(roleId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(actor, template, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        role: RoleEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = gradeway.audited(actor) { audit ->
        transaction(gradeway.database) {
            val entityTemplateEntity = DatabaseRolePermissionTemplateEntity.find {
                (RolePermissionTemplatesTable.roleId eq role.id) and
                        (RolePermissionTemplatesTable.permissionTemplateId eq template.id)
            }.limit(1).firstOrNull()

            if (entityTemplateEntity == null) {
                raise(PermissionService.UnlinkTemplateError.NotLinked)
            }

            try {
                audit.deleted(entityTemplateEntity)
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
        actor: Actor,
        templateIdOrName: String,
        roleId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromRole(actor, templateEntity, roleId)
    }

    override fun unlinkTemplateFromRole(
        actor: Actor,
        templateIdOrName: String,
        role: RoleEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromRole(actor, templateEntity, role)
    }

    override fun unlinkTemplateFromRole(
        actor: Actor,
        templateId: UUID,
        roleIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(actor, templateId, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        roleIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(actor, template, roleEntity)
    }

    override fun unlinkTemplateFromRole(
        actor: Actor,
        templateIdOrName: String,
        roleIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromRole(actor, templateEntity, roleEntity)
    }

    override fun applyTemplateToRole(
        actor: Actor,
        templateId: UUID,
        roleId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateById(templateId)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val roleEntity = gradeway.roles.findById(roleId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(actor, templateEntity, roleEntity)
    }

    override fun applyTemplateToRole(
        actor: Actor,
        templateId: UUID,
        role: RoleEntity,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateById(templateId)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToRole(actor, templateEntity, role)
    }

    override fun applyTemplateToRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        roleId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val roleEntity = gradeway.roles.findById(roleId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(actor, template, roleEntity)
    }

    override fun applyTemplateToRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        role: RoleEntity,
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

                setRolePermissions(actor, role, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ApplyTemplateError.Unexpected(throwable))
        }
    }

    override fun applyTemplateToRole(
        actor: Actor,
        templateIdOrName: String,
        roleId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToRole(actor, templateEntity, roleId)
    }

    override fun applyTemplateToRole(
        actor: Actor,
        templateIdOrName: String,
        role: RoleEntity,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToRole(actor, templateEntity, role)
    }

    override fun applyTemplateToRole(
        actor: Actor,
        templateId: UUID,
        roleIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(actor, templateId, roleEntity)
    }

    override fun applyTemplateToRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        roleIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(actor, template, roleEntity)
    }

    override fun applyTemplateToRole(
        actor: Actor,
        templateIdOrName: String,
        roleIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToRole(actor, templateEntity, roleEntity)
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        templateId: UUID,
        roleId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val roleEntity = gradeway.roles.findById(roleId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(actor, templateEntity, roleEntity)
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        templateId: UUID,
        role: RoleEntity,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromRole(actor, templateEntity, role)
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        roleId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val roleEntity = gradeway.roles.findById(roleId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(actor, template, roleEntity)
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        role: RoleEntity,
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

                unsetRolePermissions(actor, role, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.RevokeTemplateError.Unexpected(throwable))
        }
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        templateIdOrName: String,
        roleId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromRole(actor, templateEntity, roleId)
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        templateIdOrName: String,
        role: RoleEntity,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromRole(actor, templateEntity, role)
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        templateId: UUID,
        roleIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(actor, templateId, roleEntity)
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        template: PermissionTemplateEntity,
        roleIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(actor, template, roleEntity)
    }

    override fun revokeTemplateFromRole(
        actor: Actor,
        templateIdOrName: String,
        roleIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val roleEntity = gradeway.roles.findByIdOrName(roleIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromRole(actor, templateEntity, roleEntity)
    }

    override fun linkTemplateToPlayer(
        actor: Actor,
        templateId: UUID,
        playerId: UUID,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val playerEntity = gradeway.players.findById(playerId)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(actor, templateEntity, playerEntity)
    }

    override fun linkTemplateToPlayer(
        actor: Actor,
        templateId: UUID,
        player: PlayerEntity,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToPlayer(actor, templateEntity, player)
    }

    override fun linkTemplateToPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        playerId: UUID,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val playerEntity = gradeway.players.findById(playerId)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(actor, template, playerEntity)
    }

    override fun linkTemplateToPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        player: PlayerEntity,
    ): Either<PermissionService.LinkTemplateError, Unit> = gradeway.audited(actor) { audit ->
        if (!template.assignedTo.allowForPlayer) {
            raise(PermissionService.LinkTemplateError.WrongAssignedTo)
        }

        try {
            transaction(gradeway.database) {
                DatabasePlayerPermissionTemplateEntity.new {
                    this.playerId = player.id
                    this.permissionTemplateId = template.id
                }.let { audit.created(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(PermissionService.LinkTemplateError.AlreadyLinked)
            }
            raise(PermissionService.LinkTemplateError.Unexpected(throwable))
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
        actor: Actor,
        templateIdOrName: String,
        playerId: UUID,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToPlayer(actor, templateEntity, playerId)
    }

    override fun linkTemplateToPlayer(
        actor: Actor,
        templateIdOrName: String,
        player: PlayerEntity,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToPlayer(actor, templateEntity, player)
    }

    override fun linkTemplateToPlayer(
        actor: Actor,
        templateId: UUID,
        playerIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(actor, templateId, playerEntity)
    }

    override fun linkTemplateToPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        playerIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(actor, template, playerEntity)
    }

    override fun linkTemplateToPlayer(
        actor: Actor,
        templateIdOrName: String,
        playerIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToPlayer(actor, templateEntity, playerEntity)
    }

    override fun unlinkTemplateFromPlayer(
        actor: Actor,
        templateId: UUID,
        playerId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val player = gradeway.players.findById(playerId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(actor, template, player)
    }

    override fun unlinkTemplateFromPlayer(
        actor: Actor,
        templateId: UUID,
        player: PlayerEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromPlayer(actor, template, player)
    }

    override fun unlinkTemplateFromPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        playerId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val player = gradeway.players.findById(playerId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(actor, template, player)
    }

    override fun unlinkTemplateFromPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        player: PlayerEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = gradeway.audited(actor) { audit ->
        val entityTemplateEntity = DatabasePlayerPermissionTemplateEntity.find {
            (PlayerPermissionTemplatesTable.playerId eq player.id) and
                    (PlayerPermissionTemplatesTable.permissionTemplateId eq template.id)
        }.limit(1).firstOrNull()

        if (entityTemplateEntity == null) {
            raise(PermissionService.UnlinkTemplateError.NotLinked)
        }

        try {
            transaction(gradeway.database) {
                audit.deleted(entityTemplateEntity)
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
        actor: Actor,
        templateIdOrName: String,
        playerId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromPlayer(actor, templateEntity, playerId)
    }

    override fun unlinkTemplateFromPlayer(
        actor: Actor,
        templateIdOrName: String,
        player: PlayerEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromPlayer(actor, templateEntity, player)
    }

    override fun unlinkTemplateFromPlayer(
        actor: Actor,
        templateId: UUID,
        playerIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(actor, templateId, playerEntity)
    }

    override fun unlinkTemplateFromPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        playerIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(actor, template, playerEntity)
    }

    override fun unlinkTemplateFromPlayer(
        actor: Actor,
        templateIdOrName: String,
        playerIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromPlayer(actor, templateEntity, playerEntity)
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        templateId: UUID,
        playerId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val player = gradeway.players.findById(playerId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(actor, template, player)
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        templateId: UUID,
        player: PlayerEntity,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToPlayer(actor, template, player)
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        playerId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val player = gradeway.players.findById(playerId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(actor, template, player)
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        player: PlayerEntity,
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

                setPlayerPermissions(actor, player, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ApplyTemplateError.Unexpected(throwable))
        }
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        templateIdOrName: String,
        playerId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToPlayer(actor, templateEntity, playerId)
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        templateIdOrName: String,
        player: PlayerEntity,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToPlayer(actor, templateEntity, player)
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        templateId: UUID,
        playerIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(actor, templateId, playerEntity)
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        playerIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(actor, template, playerEntity)
    }

    override fun applyTemplateToPlayer(
        actor: Actor,
        templateIdOrName: String,
        playerIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToPlayer(actor, templateEntity, playerEntity)
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        templateId: UUID,
        playerId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val player = gradeway.players.findById(playerId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(actor, template, player)
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        templateId: UUID,
        player: PlayerEntity,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromPlayer(actor, template, player)
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        playerId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val player = gradeway.players.findById(playerId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(actor, template, player)
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        player: PlayerEntity,
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

                unsetPlayerPermissions(actor, player, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.RevokeTemplateError.Unexpected(throwable))
        }
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        templateIdOrName: String,
        playerId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromPlayer(actor, templateEntity, playerId)
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        templateIdOrName: String,
        player: PlayerEntity,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromPlayer(actor, templateEntity, player)
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        templateId: UUID,
        playerIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(actor, templateId, playerEntity)
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        template: PermissionTemplateEntity,
        playerIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(actor, template, playerEntity)
    }

    override fun revokeTemplateFromPlayer(
        actor: Actor,
        templateIdOrName: String,
        playerIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val playerEntity = gradeway.players.findByIdOrName(playerIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromPlayer(actor, templateEntity, playerEntity)
    }

    override fun linkTemplateToGroup(
        actor: Actor,
        templateId: UUID,
        groupId: UUID,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val groupEntity = gradeway.groups.findById(groupId) ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(actor, templateEntity, groupEntity)
    }

    override fun linkTemplateToGroup(
        actor: Actor,
        templateId: UUID,
        group: GroupEntity,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateById(templateId) ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToGroup(actor, templateEntity, group)
    }

    override fun linkTemplateToGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        groupId: UUID,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val groupEntity = gradeway.groups.findById(groupId)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(actor, template, groupEntity)
    }

    override fun linkTemplateToGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        group: GroupEntity,
    ): Either<PermissionService.LinkTemplateError, Unit> = gradeway.audited(actor) { audit ->
        if (!template.assignedTo.allowForGroup) {
            raise(PermissionService.LinkTemplateError.WrongAssignedTo)
        }

        try {
            transaction(gradeway.database) {
                DatabaseGroupPermissionTemplateEntity.new {
                    this.groupId = group.id
                    this.permissionTemplateId = template.id
                }.let { audit.created(it) }
            }
        } catch (throwable: Throwable) {
            if (throwable.isIntegrityConstraintViolation()) {
                raise(PermissionService.LinkTemplateError.AlreadyLinked)
            }
            raise(PermissionService.LinkTemplateError.Unexpected(throwable))
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
        actor: Actor,
        templateIdOrName: String,
        groupId: UUID,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToGroup(actor, templateEntity, groupId)
    }

    override fun linkTemplateToGroup(
        actor: Actor,
        templateIdOrName: String,
        group: GroupEntity,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        return linkTemplateToGroup(actor, templateEntity, group)
    }

    override fun linkTemplateToGroup(
        actor: Actor,
        templateId: UUID,
        groupIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(actor, templateId, groupEntity)
    }

    override fun linkTemplateToGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        groupIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(actor, template, groupEntity)
    }

    override fun linkTemplateToGroup(
        actor: Actor,
        templateIdOrName: String,
        groupIdOrName: String,
    ): Either<PermissionService.LinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TemplateNotFound)
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.LinkTemplateError.TargetNotFound)
        return linkTemplateToGroup(actor, templateEntity, groupEntity)
    }

    override fun unlinkTemplateFromGroup(
        actor: Actor,
        templateId: UUID,
        groupId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val group = gradeway.groups.findById(groupId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(actor, template, group)
    }

    override fun unlinkTemplateFromGroup(
        actor: Actor,
        templateId: UUID,
        group: GroupEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromGroup(actor, template, group)
    }

    override fun unlinkTemplateFromGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        groupId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val group = gradeway.groups.findById(groupId) ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(actor, template, group)
    }

    override fun unlinkTemplateFromGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        group: GroupEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = gradeway.audited(actor) { audit ->
        val entityTemplateEntity = DatabaseGroupPermissionTemplateEntity.find {
            (GroupPermissionTemplatesTable.groupId eq group.id) and
                    (GroupPermissionTemplatesTable.permissionTemplateId eq template.id)
        }.limit(1).firstOrNull()

        if (entityTemplateEntity == null) {
            raise(PermissionService.UnlinkTemplateError.NotLinked)
        }

        try {
            transaction(gradeway.database) {
                audit.deleted(entityTemplateEntity)
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
        actor: Actor,
        templateIdOrName: String,
        groupId: UUID,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromGroup(actor, templateEntity, groupId)
    }

    override fun unlinkTemplateFromGroup(
        actor: Actor,
        templateIdOrName: String,
        group: GroupEntity,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        return unlinkTemplateFromGroup(actor, templateEntity, group)
    }

    override fun unlinkTemplateFromGroup(
        actor: Actor,
        templateId: UUID,
        groupIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(actor, templateId, groupEntity)
    }

    override fun unlinkTemplateFromGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        groupIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(actor, template, groupEntity)
    }

    override fun unlinkTemplateFromGroup(
        actor: Actor,
        templateIdOrName: String,
        groupIdOrName: String,
    ): Either<PermissionService.UnlinkTemplateError, Unit> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TemplateNotFound)
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.UnlinkTemplateError.TargetNotFound)
        return unlinkTemplateFromGroup(actor, templateEntity, groupEntity)
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        templateId: UUID,
        groupId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val group = gradeway.groups.findById(groupId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(actor, template, group)
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        templateId: UUID,
        group: GroupEntity,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToGroup(actor, template, group)
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        groupId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val group = gradeway.groups.findById(groupId) ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(actor, template, group)
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        group: GroupEntity,
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

                setGroupPermissions(actor, group, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.ApplyTemplateError.Unexpected(throwable))
        }
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        templateIdOrName: String,
        groupId: UUID,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToGroup(actor, templateEntity, groupId)
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        templateIdOrName: String,
        group: GroupEntity,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        return applyTemplateToGroup(actor, templateEntity, group)
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        templateId: UUID,
        groupIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(actor, templateId, groupEntity)
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        groupIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(actor, template, groupEntity)
    }

    override fun applyTemplateToGroup(
        actor: Actor,
        templateIdOrName: String,
        groupIdOrName: String,
    ): Either<PermissionService.ApplyTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TemplateNotFound)
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.ApplyTemplateError.TargetNotFound)
        return applyTemplateToGroup(actor, templateEntity, groupEntity)
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        templateId: UUID,
        groupId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val group = gradeway.groups.findById(groupId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(actor, template, group)
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        templateId: UUID,
        group: GroupEntity,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val template = findTemplateById(templateId) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromGroup(actor, template, group)
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        groupId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val group = gradeway.groups.findById(groupId) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(actor, template, group)
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        group: GroupEntity,
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

                unsetGroupPermissions(actor, group, permissions).isRight()
            }
        } catch (throwable: Throwable) {
            raise(PermissionService.RevokeTemplateError.Unexpected(throwable))
        }
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        templateIdOrName: String,
        groupId: UUID,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromGroup(actor, templateEntity, groupId)
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        templateIdOrName: String,
        group: GroupEntity,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity =
            findTemplateByIdOrName(templateIdOrName) ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        return revokeTemplateFromGroup(actor, templateEntity, group)
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        templateId: UUID,
        groupIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val groupEntity =
            gradeway.groups.findByIdOrName(groupIdOrName) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(actor, templateId, groupEntity)
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        template: PermissionTemplateEntity,
        groupIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val groupEntity =
            gradeway.groups.findByIdOrName(groupIdOrName) ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(actor, template, groupEntity)
    }

    override fun revokeTemplateFromGroup(
        actor: Actor,
        templateIdOrName: String,
        groupIdOrName: String,
    ): Either<PermissionService.RevokeTemplateError, Boolean> = either {
        val templateEntity = findTemplateByIdOrName(templateIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TemplateNotFound)
        val groupEntity = gradeway.groups.findByIdOrName(groupIdOrName)
            ?: raise(PermissionService.RevokeTemplateError.TargetNotFound)
        return revokeTemplateFromGroup(actor, templateEntity, groupEntity)
    }

    override fun setRolePermission(
        actor: Actor,
        id: UUID,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = gradeway.roles.findById(id) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setRolePermission(actor, entity, permission, enabled)
    }

    override fun setRolePermission(
        actor: Actor,
        entity: RoleEntity,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> =
        setEntityPermission(actor, entity, permission, enabled) { permissionEntity ->
            DatabaseRolePermissionEntity.new {
                this.roleId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setRolePermission(
        actor: Actor,
        idOrName: String,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName)
            ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setRolePermission(actor, entity, permission, enabled)
    }

    override fun setRolePermissions(
        actor: Actor,
        id: UUID,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = gradeway.roles.findById(id) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setRolePermissions(actor, entity, permissions)
    }

    override fun setRolePermissions(
        actor: Actor,
        entity: RoleEntity,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> =
        setEntityPermissions(actor, entity, permissions) { permissionEntity, enabled ->
            DatabaseRolePermissionEntity.new {
                this.roleId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setRolePermissions(
        actor: Actor,
        idOrName: String,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName)
            ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setRolePermissions(actor, entity, permissions)
    }

    override fun unsetRolePermission(
        actor: Actor,
        id: UUID,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = gradeway.roles.findById(id) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetRolePermission(actor, entity, permission)
    }

    override fun unsetRolePermission(
        actor: Actor,
        entity: RoleEntity,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = unsetEntityPermission(actor, entity, permission)

    override fun unsetRolePermission(
        actor: Actor,
        idOrName: String,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName)
            ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetRolePermission(actor, entity, permission)
    }

    override fun unsetRolePermissions(
        actor: Actor,
        id: UUID,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = gradeway.roles.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetRolePermissions(actor, entity, permissions)
    }

    override fun unsetRolePermissions(
        actor: Actor,
        entity: RoleEntity,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = unsetEntityPermissions(actor, entity, permissions)

    override fun unsetRolePermissions(
        actor: Actor,
        idOrName: String,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName)
            ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetRolePermissions(actor, entity, permissions)
    }

    override fun clearRolePermissions(
        actor: Actor,
        id: UUID,
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = gradeway.roles.findById(id) ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearRolePermissions(actor, entity)
    }

    override fun clearRolePermissions(
        actor: Actor,
        entity: RoleEntity,
    ): Either<PermissionService.ClearPermissionsError, Unit> = clearEntityPermissions(actor, entity)

    override fun clearRolePermissions(
        actor: Actor,
        idOrName: String,
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = gradeway.roles.findByIdOrName(idOrName)
            ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearRolePermissions(actor, entity)
    }

    override fun hasRolePermission(id: UUID, permission: String): Boolean {
        val entity = gradeway.roles.findById(id) ?: return false
        return hasRolePermission(entity, permission)
    }

    override fun hasRolePermission(
        entity: RoleEntity,
        permission: String
    ): Boolean = hasEntityPermission(entity, permission)

    override fun hasRolePermission(idOrName: String, permission: String): Boolean {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: return false
        return hasRolePermission(entity, permission)
    }

    override fun hasRoleAnyPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = gradeway.roles.findById(id) ?: return false
        return hasRoleAnyPermissions(entity, permissions)
    }

    override fun hasRoleAnyPermissions(
        entity: RoleEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAnyPermissions(entity, permissions)

    override fun hasRoleAnyPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: return false
        return hasRoleAnyPermissions(entity, permissions)
    }

    override fun hasRoleAllPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = gradeway.roles.findById(id) ?: return false
        return hasRoleAllPermissions(entity, permissions)
    }

    override fun hasRoleAllPermissions(
        entity: RoleEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAllPermissions(entity, permissions)

    override fun hasRoleAllPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: return false
        return hasRoleAllPermissions(entity, permissions)
    }

    override fun getRolePermissions(id: UUID): Set<RolePermissionEntity> {
        val entity = gradeway.roles.findById(id) ?: return emptySet()
        return getRolePermissions(entity)
    }

    override fun getRolePermissions(entity: RoleEntity): Set<RolePermissionEntity> {
        return transaction(gradeway.database) {
            entity.permissions.toSet()
        }
    }

    override fun getRolePermissions(idOrName: String): Set<RolePermissionEntity> {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: return emptySet()
        return getRolePermissions(entity)
    }

    override fun setPlayerPermission(
        actor: Actor,
        id: UUID,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = gradeway.players.findById(id) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setPlayerPermission(actor, entity, permission, enabled)
    }

    override fun setPlayerPermission(
        actor: Actor,
        entity: PlayerEntity,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> =
        setEntityPermission(actor, entity, permission, enabled) { permissionEntity ->
            DatabasePlayerPermissionEntity.new {
                this.playerId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setPlayerPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setPlayerPermission(actor, entity, permission, enabled)
    }

    override fun setPlayerPermissions(
        actor: Actor,
        id: UUID,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = gradeway.players.findById(id) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setPlayerPermissions(actor, entity, permissions)
    }

    override fun setPlayerPermissions(
        actor: Actor,
        entity: PlayerEntity,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> =
        setEntityPermissions(actor, entity, permissions) { permissionEntity, enabled ->
            DatabasePlayerPermissionEntity.new {
                this.playerId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setPlayerPermissions(
        actor: Actor,
        idOrName: String,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setPlayerPermissions(actor, entity, permissions)
    }

    override fun unsetPlayerPermission(
        actor: Actor,
        id: UUID,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = gradeway.players.findById(id) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetPlayerPermission(actor, entity, permission)
    }

    override fun unsetPlayerPermission(
        actor: Actor,
        entity: PlayerEntity,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = unsetEntityPermission(actor, entity, permission)

    override fun unsetPlayerPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetPlayerPermission(actor, entity, permission)
    }

    override fun unsetPlayerPermissions(
        actor: Actor,
        id: UUID,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = gradeway.players.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetPlayerPermissions(actor, entity, permissions)
    }

    override fun unsetPlayerPermissions(
        actor: Actor,
        entity: PlayerEntity,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = unsetEntityPermissions(actor, entity, permissions)

    override fun unsetPlayerPermissions(
        actor: Actor,
        idOrName: String,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetPlayerPermissions(actor, entity, permissions)
    }

    override fun clearPlayerPermissions(
        actor: Actor,
        id: UUID,
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = gradeway.players.findById(id) ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearPlayerPermissions(actor, entity)
    }

    override fun clearPlayerPermissions(
        actor: Actor,
        entity: PlayerEntity,
    ): Either<PermissionService.ClearPermissionsError, Unit> = clearEntityPermissions(actor, entity)

    override fun clearPlayerPermissions(
        actor: Actor,
        idOrName: String,
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = gradeway.players.findByIdOrName(idOrName)
            ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearPlayerPermissions(actor, entity)
    }

    override fun hasPlayerPermission(id: UUID, permission: String): Boolean {
        val entity = gradeway.players.findById(id) ?: return false
        return hasPlayerPermission(entity, permission)
    }

    override fun hasPlayerPermission(
        entity: PlayerEntity,
        permission: String
    ): Boolean = hasEntityPermission(entity, permission)

    override fun hasPlayerPermission(idOrName: String, permission: String): Boolean {
        val entity = gradeway.players.findByIdOrName(idOrName) ?: return false
        return hasPlayerPermission(entity, permission)
    }

    override fun hasPlayerAnyPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = gradeway.players.findById(id) ?: return false
        return hasPlayerAnyPermissions(entity, permissions)
    }

    override fun hasPlayerAnyPermissions(
        entity: PlayerEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAnyPermissions(entity, permissions)

    override fun hasPlayerAnyPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = gradeway.players.findByIdOrName(idOrName) ?: return false
        return hasPlayerAnyPermissions(entity, permissions)
    }

    override fun hasPlayerAllPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = gradeway.players.findById(id) ?: return false
        return hasPlayerAllPermissions(entity, permissions)
    }

    override fun hasPlayerAllPermissions(
        entity: PlayerEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAllPermissions(entity, permissions)

    override fun hasPlayerAllPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = gradeway.players.findByIdOrName(idOrName) ?: return false
        return hasPlayerAllPermissions(entity, permissions)
    }

    override fun getPlayerPermissions(id: UUID): Set<PlayerPermissionEntity> {
        val entity = gradeway.players.findById(id) ?: return emptySet()
        return getPlayerPermissions(entity)
    }

    override fun getPlayerPermissions(entity: PlayerEntity): Set<PlayerPermissionEntity> {
        return transaction(gradeway.database) {
            entity.permissions.toSet()
        }
    }

    override fun getPlayerPermissions(idOrName: String): Set<PlayerPermissionEntity> {
        val entity = gradeway.players.findByIdOrName(idOrName) ?: return emptySet()
        return getPlayerPermissions(entity)
    }

    override fun setGroupPermission(
        actor: Actor,
        id: UUID,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = gradeway.groups.findById(id) ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setGroupPermission(actor, entity, permission, enabled)
    }

    override fun setGroupPermission(
        actor: Actor,
        entity: GroupEntity,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> =
        setEntityPermission(actor, entity, permission, enabled) { permissionEntity ->
            DatabaseGroupPermissionEntity.new {
                this.groupId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setGroupPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
        enabled: Boolean,
    ): Either<PermissionService.SetPermissionError, Unit> = either {
        val entity = gradeway.groups.findByIdOrName(idOrName)
            ?: raise(PermissionService.SetPermissionError.EntityNotFound)
        return setGroupPermission(actor, entity, permission, enabled)
    }

    override fun setGroupPermissions(
        actor: Actor,
        id: UUID,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = gradeway.groups.findById(id) ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setGroupPermissions(actor, entity, permissions)
    }

    override fun setGroupPermissions(
        actor: Actor,
        entity: GroupEntity,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> =
        setEntityPermissions(actor, entity, permissions) { permissionEntity, enabled ->
            DatabaseGroupPermissionEntity.new {
                this.groupId = entity.id
                this.permissionId = permissionEntity.id
                this.isEnabled = enabled
            }
        }

    override fun setGroupPermissions(
        actor: Actor,
        idOrName: String,
        permissions: Map<String, Boolean>,
    ): Either<PermissionService.BulkSetPermissionError, Unit> = either {
        val entity = gradeway.groups.findByIdOrName(idOrName)
            ?: raise(PermissionService.BulkSetPermissionError.EntityNotFound)
        return setGroupPermissions(actor, entity, permissions)
    }

    override fun unsetGroupPermission(
        actor: Actor,
        id: UUID,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = gradeway.groups.findById(id) ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetGroupPermission(actor, entity, permission)
    }

    override fun unsetGroupPermission(
        actor: Actor,
        entity: GroupEntity,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = unsetEntityPermission(actor, entity, permission)

    override fun unsetGroupPermission(
        actor: Actor,
        idOrName: String,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = either {
        val entity = gradeway.groups.findByIdOrName(idOrName)
            ?: raise(PermissionService.UnsetPermissionError.EntityNotFound)
        return unsetGroupPermission(actor, entity, permission)
    }

    override fun unsetGroupPermissions(
        actor: Actor,
        id: UUID,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = gradeway.groups.findById(id) ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetGroupPermissions(actor, entity, permissions)
    }

    override fun unsetGroupPermissions(
        actor: Actor,
        entity: GroupEntity,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = unsetEntityPermissions(actor, entity, permissions)

    override fun unsetGroupPermissions(
        actor: Actor,
        idOrName: String,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = either {
        val entity = gradeway.groups.findByIdOrName(idOrName)
            ?: raise(PermissionService.BulkUnsetPermissionError.EntityNotFound)
        return unsetGroupPermissions(actor, entity, permissions)
    }

    override fun clearGroupPermissions(
        actor: Actor,
        id: UUID,
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = gradeway.groups.findById(id) ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearGroupPermissions(actor, entity)
    }

    override fun clearGroupPermissions(
        actor: Actor,
        entity: GroupEntity,
    ): Either<PermissionService.ClearPermissionsError, Unit> = clearEntityPermissions(actor, entity)

    override fun clearGroupPermissions(
        actor: Actor,
        idOrName: String,
    ): Either<PermissionService.ClearPermissionsError, Unit> = either {
        val entity = gradeway.groups.findByIdOrName(idOrName)
            ?: raise(PermissionService.ClearPermissionsError.EntityNotFound)
        return clearGroupPermissions(actor, entity)
    }

    override fun hasGroupPermission(id: UUID, permission: String): Boolean {
        val entity = gradeway.groups.findById(id) ?: return false
        return hasGroupPermission(entity, permission)
    }

    override fun hasGroupPermission(
        entity: GroupEntity,
        permission: String
    ): Boolean = hasEntityPermission(entity, permission)

    override fun hasGroupPermission(idOrName: String, permission: String): Boolean {
        val entity = gradeway.groups.findByIdOrName(idOrName) ?: return false
        return hasGroupPermission(entity, permission)
    }

    override fun hasGroupAnyPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = gradeway.groups.findById(id) ?: return false
        return hasGroupAnyPermissions(entity, permissions)
    }

    override fun hasGroupAnyPermissions(
        entity: GroupEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAnyPermissions(entity, permissions)

    override fun hasGroupAnyPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = gradeway.groups.findByIdOrName(idOrName) ?: return false
        return hasGroupAnyPermissions(entity, permissions)
    }

    override fun hasGroupAllPermissions(id: UUID, permissions: Collection<String>): Boolean {
        val entity = gradeway.groups.findById(id) ?: return false
        return hasGroupAllPermissions(entity, permissions)
    }

    override fun hasGroupAllPermissions(
        entity: GroupEntity,
        permissions: Collection<String>
    ): Boolean = hasEntityAllPermissions(entity, permissions)

    override fun hasGroupAllPermissions(idOrName: String, permissions: Collection<String>): Boolean {
        val entity = gradeway.groups.findByIdOrName(idOrName) ?: return false
        return hasGroupAllPermissions(entity, permissions)
    }

    override fun getGroupPermissions(id: UUID): Set<GroupPermissionEntity> {
        val entity = gradeway.groups.findById(id) ?: return emptySet()
        return getGroupPermissions(entity)
    }

    override fun getGroupPermissions(entity: GroupEntity): Set<GroupPermissionEntity> {
        return transaction(gradeway.database) {
            entity.permissions.toSet()
        }
    }

    override fun getGroupPermissions(idOrName: String): Set<GroupPermissionEntity> {
        val entity = gradeway.groups.findByIdOrName(idOrName) ?: return emptySet()
        return getGroupPermissions(entity)
    }

    private fun findOrCreatePermissionEntity(audit: AuditRecorder, permission: String): PermissionEntity {
        return findPermissionByValue(permission) ?: DatabasePermissionEntity.new {
            this.value = permission
            this.type = PermissionEntity.Type.EQUALS
        }.also { audit.created(it) }
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

            is TrackChangedPayload, is TrackStageChangedPayload -> Unit
        }
    }

    private fun invalidatePlayer(playerId: String) {
        val uuid = runCatching { UUID.fromString(playerId) }.getOrNull() ?: return
        gradeway.caches.playerPermissions.invalidate(uuid)
        gradeway.caches.playerEffectivePermissions.invalidate(uuid)
    }

    override fun getEffectiveRolePermissions(id: UUID): Set<PermissionEntity> {
        return gradeway.caches.roleEffectivePermissions.get(id)
    }

    override fun getEffectiveRolePermissions(entity: RoleEntity): Set<PermissionEntity> {
        return getEffectiveRolePermissions(entity.id.value)
    }

    override fun getEffectiveRolePermissions(idOrName: String): Set<PermissionEntity> {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: return emptySet()
        return getEffectiveRolePermissions(entity)
    }

    override fun hasEffectiveRolePermission(id: UUID, permission: String): Boolean {
        return getEffectiveRolePermissions(id).any { it.validatePermission(permission) }
    }

    override fun hasEffectiveRolePermission(entity: RoleEntity, permission: String): Boolean {
        return hasEffectiveRolePermission(entity.id.value, permission)
    }

    override fun hasEffectiveRolePermission(idOrName: String, permission: String): Boolean {
        val entity = gradeway.roles.findByIdOrName(idOrName) ?: return false
        return hasEffectiveRolePermission(entity, permission)
    }

    override fun getEffectiveGroupPermissions(id: UUID): Set<PermissionEntity> {
        return gradeway.caches.groupEffectivePermissions.get(id)
    }

    override fun getEffectiveGroupPermissions(entity: GroupEntity): Set<PermissionEntity> {
        return getEffectiveGroupPermissions(entity.id.value)
    }

    override fun getEffectiveGroupPermissions(idOrName: String): Set<PermissionEntity> {
        val entity = gradeway.groups.findByIdOrName(idOrName) ?: return emptySet()
        return getEffectiveGroupPermissions(entity)
    }

    override fun hasEffectiveGroupPermission(id: UUID, permission: String): Boolean {
        return getEffectiveGroupPermissions(id).any { it.validatePermission(permission) }
    }

    override fun hasEffectiveGroupPermission(entity: GroupEntity, permission: String): Boolean {
        return hasEffectiveGroupPermission(entity.id.value, permission)
    }

    override fun hasEffectiveGroupPermission(idOrName: String, permission: String): Boolean {
        val entity = gradeway.groups.findByIdOrName(idOrName) ?: return false
        return hasEffectiveGroupPermission(entity, permission)
    }

    override fun getEffectivePlayerPermissions(id: UUID): Set<PermissionEntity> {
        return gradeway.caches.playerEffectivePermissions.get(id)
    }

    override fun getEffectivePlayerPermissions(entity: PlayerEntity): Set<PermissionEntity> {
        return getEffectivePlayerPermissions(entity.id.value)
    }

    override fun getEffectivePlayerPermissions(idOrName: String): Set<PermissionEntity> {
        val entity = gradeway.players.findByIdOrName(idOrName) ?: return emptySet()
        return getEffectivePlayerPermissions(entity)
    }

    override fun hasEffectivePlayerPermission(id: UUID, permission: String): Boolean {
        return getEffectivePlayerPermissions(id).any { it.validatePermission(permission) }
    }

    override fun hasEffectivePlayerPermission(entity: PlayerEntity, permission: String): Boolean {
        return hasEffectivePlayerPermission(entity.id.value, permission)
    }

    override fun hasEffectivePlayerPermission(idOrName: String, permission: String): Boolean {
        val entity = gradeway.players.findByIdOrName(idOrName) ?: return false
        return hasEffectivePlayerPermission(entity, permission)
    }

    override fun explainPlayerPermission(id: UUID, permission: String): List<PermissionGrant> {
        val entity = gradeway.players.findById(id) ?: return emptyList()
        return explainPlayerPermission(entity, permission)
    }

    override fun explainPlayerPermission(entity: PlayerEntity, permission: String): List<PermissionGrant> {
        if (!gradeway.permissions.hasEffectivePlayerPermission(entity.id.value, permission)) {
            return emptyList()
        }

        return transaction(gradeway.database) {
            val collector = PermissionGrantCollector(permission, gradeway)
            val path = listOf(GrantSource.Player(entity.id.value, entity.name))

            collector.collectOwn(path, entity)
            collector.collectTemplates(path, entity)

            entity.roles
                .filter { it.pausedAt == null && (it.untilAt == null || it.untilAt!! > gradeway.now()) }
                .forEach { playerRole -> collector.collectRole(path, playerRole.role, emptySet()) }

            collector.result()
        }
    }

    override fun explainRolePermission(id: UUID, permission: String): List<PermissionGrant> {
        val entity = gradeway.roles.findById(id) ?: return emptyList()
        return explainRolePermission(entity, permission)
    }

    override fun explainRolePermission(entity: RoleEntity, permission: String): List<PermissionGrant> {
        return transaction(gradeway.database) {
            val collector = PermissionGrantCollector(permission, gradeway)
            collector.collectRole(emptyList(), entity, emptySet())
            collector.result()
        }
    }

    override fun findPermissionHolders(permission: String): PermissionHolders {
        return transaction(gradeway.database) {
            val matchingPermissionIds = DatabasePermissionEntity.all()
                .filter { it.validatePermission(permission) }
                .map { it.id }
            val matchingTemplateIds = if (matchingPermissionIds.isEmpty()) {
                emptyList()
            } else {
                PermissionTemplatePermissionsTable
                    .select(PermissionTemplatePermissionsTable.templateId)
                    .where { PermissionTemplatePermissionsTable.permissionId inList matchingPermissionIds }
                    .map { it[PermissionTemplatePermissionsTable.templateId] }
                    .distinct()
            }

            val roles = findRoleHolders(permission)
            val players = findPlayerHolders(
                matchingPermissionIds = matchingPermissionIds.map { it.value },
                matchingTemplateIds = matchingTemplateIds.map { it.value },
                holderRoleIds = roles.map { it.id },
            )

            PermissionHolders(roles = roles, players = players)
        }
    }

    private fun findRoleHolders(permission: String): List<PermissionHolder> {
        return RolesTable
            .select(RolesTable.id)
            .map { it[RolesTable.id].value }
            .filter { roleId -> gradeway.permissions.hasEffectiveRolePermission(roleId, permission) }
            .mapNotNull { roleId -> gradeway.roles.findById(roleId) }
            .map { role ->
                PermissionHolder(
                    id = role.id.value,
                    name = role.name,
                    direct = holdsDirectly(role, permission),
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun findPlayerHolders(
        matchingPermissionIds: List<UUID>,
        matchingTemplateIds: List<UUID>,
        holderRoleIds: List<UUID>,
    ): List<PermissionHolder> {
        val directIds = mutableSetOf<UUID>()

        if (matchingPermissionIds.isNotEmpty()) {
            directIds += PlayerPermissionsTable
                .select(PlayerPermissionsTable.playerId)
                .where {
                    (PlayerPermissionsTable.permissionId inList matchingPermissionIds) and
                            (PlayerPermissionsTable.isEnabled eq true)
                }
                .map { it[PlayerPermissionsTable.playerId].value }
        }

        if (matchingTemplateIds.isNotEmpty()) {
            directIds += PlayerPermissionTemplatesTable
                .select(PlayerPermissionTemplatesTable.playerId)
                .where { PlayerPermissionTemplatesTable.permissionTemplateId inList matchingTemplateIds }
                .map { it[PlayerPermissionTemplatesTable.playerId].value }
        }

        val roleHolderIds = if (holderRoleIds.isEmpty()) {
            emptySet()
        } else {
            val now = gradeway.now()
            PlayerRolesTable
                .select(PlayerRolesTable.playerId)
                .where {
                    (PlayerRolesTable.roleId inList holderRoleIds) and
                            PlayerRolesTable.pausedAt.isNull() and
                            (PlayerRolesTable.untilAt.isNull() or (PlayerRolesTable.untilAt greater now))
                }
                .map { it[PlayerRolesTable.playerId].value }
                .toSet()
        }

        val holderIds = directIds + roleHolderIds
        if (holderIds.isEmpty()) {
            return emptyList()
        }

        return PlayersTable
            .select(PlayersTable.id, PlayersTable.name)
            .where { PlayersTable.id inList holderIds }
            .map { row ->
                val id = row[PlayersTable.id].value
                PermissionHolder(id = id, name = row[PlayersTable.name], direct = id in directIds)
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun <TEntity> holdsDirectly(entity: TEntity, permission: String): Boolean
            where TEntity : PermissionReference<out SharedPermissionEntity>,
                  TEntity : PermissionTemplateReference<out SharedPermissionTemplateEntity> {
        val ownMatch = entity.permissions.any { it.isEnabled && it.permission.validatePermission(permission) }
        return ownMatch || entity.permissionTemplates.any { link ->
            link.permissionTemplate.permissions.any { it.permission.validatePermission(permission) }
        }
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
        actor: Actor,
        entity: PermissionReference<out SharedPermissionEntity>,
        permission: String,
        enabled: Boolean,
        createNewPermission: (PermissionEntity) -> Auditable,
    ): Either<PermissionService.SetPermissionError, Unit> = gradeway.audited(actor) { audit ->
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
                    val before = (existingPermission as? Auditable)?.snapshot()
                    existingPermission.isEnabled = enabled
                    existingPermission.flush()
                    if (existingPermission is Auditable && before != null) {
                        audit.updated(existingPermission, before)
                    }

                    publishPermissionChanged(entity, permission)
                } catch (throwable: Throwable) {
                    raise(PermissionService.SetPermissionError.Unexpected(throwable))
                }
                return@transaction
            }

            try {
                val permissionEntity = findOrCreatePermissionEntity(audit, permission)
                audit.created(createNewPermission(permissionEntity))

                publishPermissionChanged(entity, permission)
            } catch (throwable: Throwable) {
                raise(PermissionService.SetPermissionError.Unexpected(throwable))
            }
        }
    }

    private fun setEntityPermissions(
        actor: Actor,
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: Map<String, Boolean>,
        createNewPermission: (PermissionEntity, Boolean) -> Auditable,
    ): Either<PermissionService.BulkSetPermissionError, Unit> = gradeway.audited(actor) { audit ->
        val createdPermissions = try {
            transaction(gradeway.database) {
                val existingPermissions = entity.permissions
                val created = mutableListOf<String>()
                for ((permissionValue, enabled) in permissions) {
                    if (existingPermissions.any { it.permission.value == permissionValue }) continue
                    val permissionEntity = findOrCreatePermissionEntity(audit, permissionValue)
                    audit.created(createNewPermission(permissionEntity, enabled))
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
        actor: Actor,
        entity: PermissionReference<out SharedPermissionEntity>,
        permission: String,
    ): Either<PermissionService.UnsetPermissionError, Unit> = gradeway.audited(actor) { audit ->
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
                (entityPermission as? Auditable)?.let { audit.deleted(it) }
                entityPermission.delete()

                publishPermissionChanged(entity, permission)
            } catch (throwable: Throwable) {
                raise(PermissionService.UnsetPermissionError.Unexpected(throwable))
            }
        }
    }

    private fun unsetEntityPermissions(
        actor: Actor,
        entity: PermissionReference<out SharedPermissionEntity>,
        permissions: Collection<String>,
    ): Either<PermissionService.BulkUnsetPermissionError, Unit> = gradeway.audited(actor) { audit ->
        val deletedPermissions = try {
            transaction(gradeway.database) {
                val entityPermissions = entity.permissions.filter { it.permission.value in permissions }
                entityPermissions.forEach { entityPermissionEntity ->
                    if (entityPermissionEntity is Entity<*>) {
                        (entityPermissionEntity as? Auditable)?.let { audit.deleted(it) }
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
        actor: Actor,
        entity: PermissionReference<out SharedPermissionEntity>,
    ): Either<PermissionService.ClearPermissionsError, Unit> = gradeway.audited(actor) { audit ->
        transaction(gradeway.database) {
            try {
                entity.permissions.forEach { entityPermissionEntity ->
                    if (entityPermissionEntity is Entity<*>) {
                        (entityPermissionEntity as? Auditable)?.let { audit.deleted(it) }
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
