/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import dev.gradienttim.gradeway.services.AttributeService
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.services.PlayerService
import dev.gradienttim.gradeway.services.RoleService

/**
 * Core interface representing the Gradeway system, which provides services for managing roles
 * and players. This serves as the main access point for interacting with the Gradeway infrastructure.
 */
interface Gradeway {
    /**
     * Provides functionality for managing permissions within the Gradeway system.
     *
     * This instance of `PermissionService` is associated with the Gradeway class and combines
     * the capabilities of three distinct permission services:
     * - `RolePermissionService`: Permissions specific to roles.
     * - `PlayerPermissionService`: Permissions associated with individual players.
     *
     * The `permissions` property serves as the central access point for handling and delegating
     * permission-related operations. It encompasses all permission management logic required to
     * support the Gradeway infrastructure, facilitating consistent and extendable behavior for
     * permission handling across roles, and players.
     */
    val permissions: PermissionService

    /**
     * Provides access to attribute-related operations combining both player and role attribute functionality.
     *
     * This property exposes an instance of [dev.gradienttim.gradeway.services.AttributeService], which acts as a unified interface
     * for managing attributes associated with players and roles within the Gradeway system.
     *
     * By bridging the capabilities of [dev.gradienttim.gradeway.services.attribute.PlayerAttributeService] and
     * [dev.gradienttim.gradeway.services.attribute.RoleAttributeService], it enables
     * streamlined management of attributes, offering cohesive operations for both entity types.
     *
     * Typical operations may include:
     * - Retrieving attributes linked to players or roles.
     * - Modifying existing attributes.
     * - Managing attribute metadata shared across the Gradeway ecosystem.
     */
    val attributes: AttributeService

    /**
     * Provides access to player-related operations within the system.
     *
     * This property exposes an instance of [dev.gradienttim.gradeway.services.PlayerService], enabling the management of players
     * through operations such as creation, deletion, modification, and querying of player entities.
     * It serves as a core component for player management functionality in the Gradeway ecosystem,
     * abstracting lower-level operations into a cohesive interface.
     *
     * Typical operations include:
     * - Creating players with unique IDs and names.
     * - Deleting players by their identifiers.
     * - Updating attributes like player names.
     * - Querying player existence and retrieving player details by ID or name.
     */
    val players: PlayerService

    /**
     * Provides access to role-related operations within the system.
     *
     * This property exposes an instance of [dev.gradienttim.gradeway.services.RoleService], which allows managing roles such as
     * creating, deleting, updating, and querying role entities. It serves as a core component
     * for role management functionality in the Gradeway ecosystem, abstracting lower-level
     * operations into a cohesive interface.
     *
     * Typical operations include:
     * - Creating roles with unique names.
     * - Managing role attributes like weight and name.
     * - Checking the existence of roles by ID or name.
     * - Finding roles by their identifiers or names.
     */
    val roles: RoleService
}
