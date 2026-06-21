/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import dev.gradienttim.gradeway.services.permission.PlayerPermissionService
import dev.gradienttim.gradeway.services.permission.RolePermissionService

/**
 * Service interface for managing permissions within a system.
 *
 * This service defines methods and behaviors for handling permissions
 * in relation to roles and players. It provides structured types to
 * represent errors that can occur during permission operations such as
 * setting, unsetting, or clearing permissions.
 */
interface PermissionService : RolePermissionService, PlayerPermissionService {
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
