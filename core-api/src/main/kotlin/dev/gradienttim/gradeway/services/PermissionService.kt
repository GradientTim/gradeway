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
    /**
     * Creates a new permission entity with the given value and type.
     *
     * @param value The string value representing the permission.
     * @param type The type of the permission. Defaults to `PermissionEntity.Type.EQUALS`.
     * @return Either a `CreatePermissionError` if the creation fails, or the created `PermissionEntity`.
     */
    fun createPermission(
        value: String,
        type: PermissionEntity.Type = PermissionEntity.Type.EQUALS
    ): Either<CreatePermissionError, PermissionEntity>

    /**
     * Deletes a permission entity identified by the given UUID.
     *
     * @param id The unique identifier (UUID) of the permission entity to be deleted.
     * @return Either a `DeletePermissionError` if the deletion fails, or `Unit` if the operation succeeds.
     */
    fun deletePermission(id: UUID): Either<DeletePermissionError, Unit>

    /**
     * Deletes the given permission entity from the system.
     *
     * @param entity The permission entity to be deleted.
     * @return Either a `DeletePermissionError` if the deletion fails, or `Unit` if the operation succeeds.
     */
    fun deletePermission(entity: PermissionEntity): Either<DeletePermissionError, Unit>

    /**
     * Deletes a permission entity identified by the provided string, which can represent either the
     * unique identifier (UUID) of the entity or its value.
     *
     * @param idOrValue The unique identifier (UUID) or the value associated with the permission entity to be deleted.
     * @return Either a [DeletePermissionError] if the deletion fails or [Unit] if the operation succeeds.
     */
    fun deletePermission(idOrValue: String): Either<DeletePermissionError, Unit>

    /**
     * Updates the value of a permission entity identified by the given UUID.
     *
     * @param id The unique identifier (UUID) of the permission entity to be updated.
     * @param value The new value to be set for the permission entity.
     * @return Either an [UpdatePermissionValueError] if the update operation fails, or a [Boolean] indicating
     *         success (`true`) or failure (`false`) of the update.
     */
    fun updatePermissionValue(id: UUID, value: String): Either<UpdatePermissionValueError, Boolean>

    /**
     * Updates the value of the given permission entity.
     *
     * @param entity The permission entity whose value is to be updated.
     * @param value The new value to assign to the permission entity.
     * @return Either an [UpdatePermissionValueError] if the update operation fails,
     *         or a [Boolean] indicating success (`true`) or failure (`false`) of the update.
     */
    fun updatePermissionValue(entity: PermissionEntity, value: String): Either<UpdatePermissionValueError, Boolean>

    /**
     * Updates the value of a permission entity identified by either its unique identifier (UUID)
     * or its current value.
     *
     * @param idOrValue A string representing either the unique identifier (UUID) or the current
     *                  value of the permission entity to be updated.
     * @param value The new value to set for the permission entity.
     * @return Either an [UpdatePermissionValueError] if the update operation fails, or a
     *         [Boolean] indicating success (`true`) or failure (`false`) of the update.
     */
    fun updatePermissionValue(idOrValue: String, value: String): Either<UpdatePermissionValueError, Boolean>

    /**
     * Updates the type of permission entity identified by the given unique identifier (UUID).
     *
     * @param id The unique identifier (UUID) of the permission entity whose type is to be updated.
     * @param type The new type to assign to the permission entity.
     * @return Either an [UpdatePermissionTypeError] if the update operation fails, or a [Boolean]
     *         indicating success (`true`) or failure (`false`) of the update.
     */
    fun updatePermissionType(id: UUID, type: PermissionEntity.Type): Either<UpdatePermissionTypeError, Boolean>

    /**
     * Updates the type of the given permission entity.
     *
     * @param entity The permission entity whose type is to be updated.
     * @param type The new type to assign to the permission entity.
     * @return Either an [UpdatePermissionTypeError] if the update operation fails,
     *         or a [Boolean] indicating success (`true`) or failure (`false`) of the update.
     */
    fun updatePermissionType(
        entity: PermissionEntity,
        type: PermissionEntity.Type
    ): Either<UpdatePermissionTypeError, Boolean>

    /**
     * Updates the type of permission entity identified by either its unique identifier (UUID)
     * or its current value.
     *
     * @param idOrValue A string representing either the unique identifier (UUID) or the current
     *                  value of the permission entity whose type is to be updated.
     * @param type The new type to assign to the permission entity.
     * @return Either an [UpdatePermissionTypeError] if the update operation fails, or a [Boolean]
     *         indicating success (`true`) or failure (`false`) of the update.
     */
    fun updatePermissionType(
        idOrValue: String,
        type: PermissionEntity.Type
    ): Either<UpdatePermissionTypeError, Boolean>

    /**
     * Retrieves a permission entity based on its unique identifier (UUID).
     *
     * @param id The unique identifier (UUID) of the permission entity to be retrieved.
     * @return The `PermissionEntity` if found, or `null` if no entity exists with the given ID.
     */
    fun findPermissionById(id: UUID): PermissionEntity?

    /**
     * Finds a permission entity by its value.
     *
     * @param value The string value of the permission to search for.
     * @return The `PermissionEntity` if found, or `null` if no entity with the given value exists.
     */
    fun findPermissionByValue(value: String): PermissionEntity?

    /**
     * Finds a permission entity based on its unique identifier or string value.
     *
     * @param value A string representing either the unique identifier (UUID) or the value
     *              of the permission entity to be searched for.
     * @return The `PermissionEntity` if found, or `null` if no entity matches the given identifier or value.
     */
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
