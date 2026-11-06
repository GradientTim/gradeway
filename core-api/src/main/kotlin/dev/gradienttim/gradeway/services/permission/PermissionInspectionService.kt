/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.permission

import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import java.util.*

/**
 * Read-only service answering questions about *effective* permissions: why an entity has a
 * permission ("why") and which entities have it ("who can"). Both follow exactly the same rules
 * as effective permission resolution: own enabled permissions, linked permission templates,
 * active (not paused, not expired) player roles, role groups, and role parents.
 */
interface PermissionInspectionService {
    /**
     * Explains why a player effectively has a permission.
     *
     * @param id The UUID of the player.
     * @param permission The permission being checked, e.g. `essentials.home`.
     * @return Every grant path leading to the permission, shortest first. Empty if the player
     * does not exist or does not have the permission.
     */
    fun explainPlayerPermission(id: UUID, permission: String): List<PermissionGrant>

    /**
     * Explains why a player effectively has a permission.
     *
     * @param entity The player entity.
     * @param permission The permission being checked.
     * @return Every grant path leading to the permission, shortest first.
     */
    fun explainPlayerPermission(entity: PlayerEntity, permission: String): List<PermissionGrant>

    /**
     * Explains why a role effectively has a permission.
     *
     * @param id The UUID of the role.
     * @param permission The permission being checked.
     * @return Every grant path leading to the permission, shortest first. Empty if the role
     * does not exist or does not have the permission.
     */
    fun explainRolePermission(id: UUID, permission: String): List<PermissionGrant>

    /**
     * Explains why a role effectively has a permission.
     *
     * @param entity The role entity.
     * @param permission The permission being checked.
     * @return Every grant path leading to the permission, shortest first.
     */
    fun explainRolePermission(entity: RoleEntity, permission: String): List<PermissionGrant>

    /**
     * Finds every role and player that effectively has a permission, including those that only
     * inherit it through roles, parents, groups, or templates.
     *
     * @param permission The permission being checked, e.g. `essentials.fly`.
     * @return The roles and players holding the permission, each sorted by name.
     */
    fun findPermissionHolders(permission: String): PermissionHolders

    /**
     * A single node on a [PermissionGrant] path.
     */
    sealed interface GrantSource {
        val id: UUID
        val name: String

        data class Player(override val id: UUID, override val name: String) : GrantSource
        data class Role(override val id: UUID, override val name: String) : GrantSource
        data class Group(override val id: UUID, override val name: String) : GrantSource
        data class Template(override val id: UUID, override val name: String) : GrantSource
    }

    /**
     * One way an entity obtains a permission.
     *
     * @property path The chain from the inspected entity (first) to the entity or template that
     * holds the matching permission node (last), e.g. `Steve -> VIP -> Member -> basic-cmds`.
     * @property permission The stored permission node that matched, which may differ from the
     * checked permission for wildcard, prefix, suffix, or regex nodes.
     */
    data class PermissionGrant(
        val path: List<GrantSource>,
        val permission: PermissionEntity,
    )

    /**
     * An entity effectively holding a permission.
     *
     * @property direct `true` when the entity itself holds a matching permission or links a
     * template containing one; `false` when the permission is only inherited.
     */
    data class PermissionHolder(
        val id: UUID,
        val name: String,
        val direct: Boolean,
    )

    data class PermissionHolders(
        val roles: List<PermissionHolder>,
        val players: List<PermissionHolder>,
    )
}
