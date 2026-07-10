/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonPermissionServiceCacheTest {
    private val gradeway: CommonGradeway = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun createRoleWithPermission(permission: String): RoleEntity {
        val roleName = "role-${UUID.randomUUID().toString().take(8)}"
        val role = gradeway.roles.create(roleName).getOrElse { error(it.toString()) }
        gradeway.roles.setPermission(role, permission).getOrElse { error(it.toString()) }
        return role
    }

    private fun createPlayerWithRole(role: RoleEntity): PlayerEntity {
        val playerName = "player-${UUID.randomUUID().toString().take(8)}"
        val player = gradeway.players.create(UUID.randomUUID(), playerName).getOrElse { error(it.toString()) }
        gradeway.players.addRole(player, role).getOrElse { error(it.toString()) }
        return player
    }

    @Test
    fun `effective player permissions are cached across calls`() {
        val permission = "gradeway.test.cache"
        val role = createRoleWithPermission(permission)
        val player = createPlayerWithRole(role)

        assertTrue(gradeway.permissions.hasEffectivePlayerPermission(player, permission))

        // Bypasses CommonRoleService.setPermission entirely, so no invalidation is published -
        // if the cache is actually being consulted, this change must not be visible yet.
        transaction(gradeway.database) {
            role.permissions.first().isEnabled = false
        }

        assertTrue(
            gradeway.permissions.hasEffectivePlayerPermission(player, permission),
            "expected the cached (stale) result, proving the cache is actually being consulted",
        )
    }

    @Test
    fun `explicit publish on role permission change invalidates the player cache`() {
        val permission = "gradeway.test.publish"
        val role = createRoleWithPermission(permission)
        val player = createPlayerWithRole(role)

        assertTrue(gradeway.permissions.hasEffectivePlayerPermission(player, permission))

        gradeway.roles.setPermission(role, permission, enabled = false).getOrElse { error(it.toString()) }

        assertFalse(
            gradeway.permissions.hasEffectivePlayerPermission(player, permission),
            "expected the cache to be invalidated by the RolePermissionChangedPayload published from setPermission",
        )
    }

    @Test
    fun `deleting a role through EntityHook invalidates the player cache`() {
        val permission = "gradeway.test.entityhook"
        val role = createRoleWithPermission(permission)
        val player = createPlayerWithRole(role)
        val roleId = role.id.value

        assertTrue(gradeway.permissions.hasEffectivePlayerPermission(player, permission))

        gradeway.roles.delete(roleId).getOrElse { error(it.toString()) }

        assertFalse(
            gradeway.permissions.hasEffectivePlayerPermission(player, permission),
            "expected EntityHook's RoleChangedPayload(DELETED) to invalidate the cache with no broker connected",
        )
    }
}
