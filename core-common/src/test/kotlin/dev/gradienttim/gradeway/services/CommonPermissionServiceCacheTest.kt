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
import dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import dev.gradienttim.gradeway.messaging.payloads.RolePermissionsClearedPayload
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.*

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
    fun `clearing a player's permissions invalidates that player's effective permission cache`() {
        val permission = "gradeway.test.clear"
        val player = gradeway.players.create(UUID.randomUUID(), "player-${UUID.randomUUID().toString().take(8)}")
            .getOrElse { error(it.toString()) }
        gradeway.permissions.setPlayerPermission(player, permission, true).getOrElse { error(it.toString()) }

        assertTrue(gradeway.permissions.hasEffectivePlayerPermission(player, permission))

        gradeway.permissions.clearPlayerPermissions(player).getOrElse { error(it.toString()) }

        assertFalse(
            gradeway.permissions.hasEffectivePlayerPermission(player, permission),
            "expected the PlayerPermissionsClearedPayload published from clearPlayerPermissions to invalidate the cache"
        )
    }

    @Test
    fun `clearing a role's permissions publishes a single general cleared payload`() {
        val role = createRoleWithPermission("gradeway.test.clear-role")
        gradeway.roles.setPermission(role, "gradeway.test.clear-role.other").getOrElse { error(it.toString()) }

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.permissions.clearRolePermissions(role).getOrElse { error(it.toString()) }

        assertEquals(
            listOf<MessagingPayload>(RolePermissionsClearedPayload(role.id.value.toString())),
            received,
            "expected exactly one general cleared payload instead of one per removed permission",
        )
    }

    @Test
    fun `publishing a cache flush invalidates the player cache`() {
        val permission = "gradeway.test.cacheflush"
        val role = createRoleWithPermission(permission)
        val player = createPlayerWithRole(role)

        assertTrue(gradeway.permissions.hasEffectivePlayerPermission(player, permission))

        // Simulates the bulk-write path (migration/backup import) that bypasses the services and
        // their per-mutation payloads entirely, so it can only rely on an explicit full flush.
        transaction(gradeway.database) {
            role.permissions.first().isEnabled = false
        }
        gradeway.messaging.publish(CacheFlushPayload)

        assertFalse(
            gradeway.permissions.hasEffectivePlayerPermission(player, permission),
            "expected CacheFlushPayload to invalidate the cache with no broker connected",
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
