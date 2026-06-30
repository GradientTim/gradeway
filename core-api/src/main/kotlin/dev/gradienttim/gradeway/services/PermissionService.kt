/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.Either
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.services.permission.PlayerPermissionService
import dev.gradienttim.gradeway.services.permission.RolePermissionService
import dev.gradienttim.gradeway.services.permission.TemplatePermissionService
import java.util.*

/**
 * Service interface for managing permissions within a system.
 *
 * This service defines methods and behaviors for handling permissions
 * in relation to roles and players. It provides structured types to
 * represent errors that can occur during permission operations such as
 * setting, unsetting, or clearing permissions.
 */
interface PermissionService : TemplatePermissionService, RolePermissionService, PlayerPermissionService {
    fun createPermission(
        value: String,
        type: PermissionEntity.Type = PermissionEntity.Type.EQUALS
    ): Either<CreatePermissionError, PermissionEntity>

    fun deletePermission(id: UUID): Either<DeletePermissionError, Unit>

    fun deletePermission(entity: PermissionEntity): Either<DeletePermissionError, Unit>

    fun deletePermission(idOrValue: String): Either<DeletePermissionError, Unit>

    fun updatePermissionValue(id: UUID, value: String): Either<UpdatePermissionValueError, Boolean>

    fun updatePermissionValue(entity: PermissionEntity, value: String): Either<UpdatePermissionValueError, Boolean>

    fun updatePermissionValue(idOrValue: String, value: String): Either<UpdatePermissionValueError, Boolean>

    fun updatePermissionType(id: UUID, type: PermissionEntity.Type): Either<UpdatePermissionTypeError, Boolean>

    fun updatePermissionType(
        entity: PermissionEntity,
        type: PermissionEntity.Type
    ): Either<UpdatePermissionTypeError, Boolean>

    fun updatePermissionType(
        idOrValue: String,
        type: PermissionEntity.Type
    ): Either<UpdatePermissionTypeError, Boolean>

    fun findPermissionById(id: UUID): PermissionEntity?

    fun findPermissionByValue(value: String): PermissionEntity?

    fun findPermissionByIdOrValue(value: String): PermissionEntity?

    sealed interface CreatePermissionError {
        object AlreadyExists : CreatePermissionError
        data class Unexpected(val throwable: Throwable) : CreatePermissionError
    }

    sealed interface DeletePermissionError {
        object EntityNotFound : DeletePermissionError
        data class Unexpected(val throwable: Throwable) : DeletePermissionError
    }

    sealed interface UpdatePermissionValueError {
        object EntityNotFound : UpdatePermissionValueError
        object ValueAlreadySet : UpdatePermissionValueError
        data class Unexpected(val throwable: Throwable) : UpdatePermissionValueError
    }

    sealed interface UpdatePermissionTypeError {
        object EntityNotFound : UpdatePermissionTypeError
        object TypeAlreadySet : UpdatePermissionTypeError
        data class Unexpected(val throwable: Throwable) : UpdatePermissionTypeError
    }

    sealed interface CreateTemplateError {
        object InvalidName : CreateTemplateError
        data class Unexpected(val throwable: Throwable) : CreateTemplateError
    }

    sealed interface DeleteTemplateError {
        object EntityNotFound : DeleteTemplateError
        data class Unexpected(val throwable: Throwable) : DeleteTemplateError
    }

    sealed interface SetNameTemplateError {
        object EntityNotFound : SetNameTemplateError
        object InvalidName : SetNameTemplateError
        object NameAlreadySet : SetNameTemplateError
        data class Unexpected(val throwable: Throwable) : SetNameTemplateError
    }

    sealed interface SetAssignedToTemplateError {
        object EntityNotFound : SetAssignedToTemplateError
        object AlreadyAssignedTo : SetAssignedToTemplateError
        data class Unexpected(val throwable: Throwable) : SetAssignedToTemplateError
    }

    sealed interface AddPermissionToTemplateError {
        object EntityNotFound : AddPermissionToTemplateError
        object TargetNotFound : AddPermissionToTemplateError
        object PermissionAlreadyExists : AddPermissionToTemplateError
        data class Unexpected(val throwable: Throwable) : AddPermissionToTemplateError
    }

    sealed interface RemovePermissionFromTemplateError {
        object EntityNotFound : RemovePermissionFromTemplateError
        object TargetNotFound : RemovePermissionFromTemplateError
        object PermissionNotExists : RemovePermissionFromTemplateError
        data class Unexpected(val throwable: Throwable) : RemovePermissionFromTemplateError
    }

    sealed interface ClearPermissionsFromTemplateError {
        object EntityNotFound : ClearPermissionsFromTemplateError
        data class Unexpected(val throwable: Throwable) : ClearPermissionsFromTemplateError
    }

    sealed interface LinkTemplateError {
        object TargetNotFound : LinkTemplateError
        object TemplateNotFound : LinkTemplateError
        object AlreadyLinked : LinkTemplateError
        object WrongAssignedTo : LinkTemplateError
        data class Unexpected(val throwable: Throwable) : LinkTemplateError
    }

    sealed interface UnlinkTemplateError {
        object TargetNotFound : UnlinkTemplateError
        object TemplateNotFound : UnlinkTemplateError
        object NotLinked : UnlinkTemplateError
        data class Unexpected(val throwable: Throwable) : UnlinkTemplateError
    }

    sealed interface ApplyTemplateError {
        object TargetNotFound : ApplyTemplateError
        object TemplateNotFound : ApplyTemplateError
        object WrongAssignedTo : ApplyTemplateError
        data class Unexpected(val throwable: Throwable) : ApplyTemplateError
    }

    sealed interface RevokeTemplateError {
        object TargetNotFound : RevokeTemplateError
        object TemplateNotFound : RevokeTemplateError
        data class Unexpected(val throwable: Throwable) : RevokeTemplateError
    }

    sealed interface SetPermissionError {
        object EntityNotFound : SetPermissionError
        object PermissionAlreadyEnabled : SetPermissionError
        object PermissionAlreadyDisabled : SetPermissionError
        data class Unexpected(val throwable: Throwable) : SetPermissionError
    }

    sealed interface BulkSetPermissionError {
        object EntityNotFound : BulkSetPermissionError
        data class Unexpected(val throwable: Throwable) : BulkSetPermissionError
    }

    sealed interface UnsetPermissionError {
        object EntityNotFound : UnsetPermissionError
        object PermissionNotFound : UnsetPermissionError
        data class Unexpected(val throwable: Throwable) : UnsetPermissionError
    }

    sealed interface BulkUnsetPermissionError {
        object EntityNotFound : BulkUnsetPermissionError
        data class Unexpected(val throwable: Throwable) : BulkUnsetPermissionError
    }

    sealed interface ClearPermissionError {
        object EntityNotFound : ClearPermissionError
        data class Unexpected(val throwable: Throwable) : ClearPermissionError
    }
}
