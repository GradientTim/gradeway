/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import dev.gradienttim.gradeway.event.EventCancelledError
import dev.gradienttim.gradeway.services.attribute.PlayerAttributeService
import dev.gradienttim.gradeway.services.attribute.RoleAttributeService

/**
 * Interface for managing attributes across entities such as roles and players.
 * Combines functionality provided by both [RoleAttributeService] and [PlayerAttributeService].
 *
 * Offers error handling through sealed error types for operations such as adding,
 * updating, removing, or clearing attributes. These errors provide detailed
 * granular failure information for each operation.
 */
interface AttributeService : RoleAttributeService, PlayerAttributeService {
    sealed interface AddAttributeError {
        object Cancelled : AddAttributeError, EventCancelledError
        object EntityNotFound : AddAttributeError
        object AttributeAlreadyExists : AddAttributeError
        data class Unexpected(val throwable: Throwable) : AddAttributeError
    }

    sealed interface UpdateAttributeError {
        object Cancelled : UpdateAttributeError, EventCancelledError
        object EntityNotFound : UpdateAttributeError
        object AttributeNotExists : UpdateAttributeError
        data class AttributeTypeNotRegistered(val type: String) : UpdateAttributeError
        data class Unexpected(val throwable: Throwable) : UpdateAttributeError
    }

    sealed interface RemoveAttributeError {
        object Cancelled : RemoveAttributeError, EventCancelledError
        object EntityNotFound : RemoveAttributeError
        object AttributeNotExists : RemoveAttributeError
        data class Unexpected(val throwable: Throwable) : RemoveAttributeError
    }

    sealed interface ClearAttributesError {
        object Cancelled : ClearAttributesError, EventCancelledError
        object EntityNotFound : ClearAttributesError
        object NoAttributesFound : ClearAttributesError
        data class Unexpected(val throwable: Throwable) : ClearAttributesError
    }
}
