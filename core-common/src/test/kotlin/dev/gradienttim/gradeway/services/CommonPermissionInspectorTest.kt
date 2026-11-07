/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerRoleEntity
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.services.permission.PermissionInspectionService.GrantSource
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.*

class CommonPermissionInspectorTest {
    private val gradeway: CommonGradeway<TestPlatformConfig> = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    private fun uniquePermission() = "gradeway.test.${UUID.randomUUID().toString().take(8)}"

    private fun createRole(): RoleEntity =
        gradeway.roles.create(Actor.System, uniqueName("role")).getOrElse { error(it.toString()) }

    private fun createGroup(): GroupEntity =
        gradeway.groups.create(Actor.System, uniqueName("group")).getOrElse { error(it.toString()) }

    private fun createPlayer(): PlayerEntity =
        gradeway.players.create(Actor.System, UUID.randomUUID(), uniqueName("player"))
            .getOrElse { error(it.toString()) }

    private fun createTemplate(vararg permissions: String): PermissionTemplateEntity {
        val template = gradeway.permissions.createTemplate(Actor.System, uniqueName("template"))
            .getOrElse { error(it.toString()) }
        permissions.forEach { value ->
            val permission = gradeway.permissions.findPermissionByValue(value)
                ?: gradeway.permissions.createPermission(Actor.System, value).getOrElse { error(it.toString()) }
            gradeway.permissions.addPermissionToTemplate(Actor.System, template, permission)
                .getOrElse { error(it.toString()) }
        }
        return template
    }

    private fun addParent(child: RoleEntity, parent: RoleEntity) {
        gradeway.roles.addParent(Actor.System, child, parent).getOrElse { error(it.toString()) }
    }

    private fun assignRole(player: PlayerEntity, role: RoleEntity) {
        gradeway.players.addRole(Actor.System, player.id.value, role).getOrElse { error(it.toString()) }
    }

    private fun names(path: List<GrantSource>) = path.map { it.name }

    // --- why ---

    @Test
    fun `explainPlayerPermission traces a permission through roles, parents and a template`() {
        val permission = uniquePermission()
        val template = createTemplate(permission)
        val member = createRole()
        gradeway.permissions.linkTemplateToRole(Actor.System, template, member).getOrElse { error(it.toString()) }
        val vip = createRole()
        addParent(vip, member)
        val player = createPlayer()
        assignRole(player, vip)

        val grants = gradeway.permissions.explainPlayerPermission(player, permission)

        assertEquals(1, grants.size)
        val grant = grants.single()
        assertEquals(listOf(player.name, vip.name, member.name, template.name), names(grant.path))
        assertIs<GrantSource.Player>(grant.path[0])
        assertIs<GrantSource.Role>(grant.path[1])
        assertIs<GrantSource.Role>(grant.path[2])
        assertIs<GrantSource.Template>(grant.path[3])
        assertEquals(permission, grant.permission.value)
    }

    @Test
    fun `explainPlayerPermission includes direct and group grants, shortest first`() {
        val permission = uniquePermission()
        val player = createPlayer()
        gradeway.permissions.setPlayerPermission(Actor.System, player, permission).getOrElse { error(it.toString()) }
        val group = createGroup()
        gradeway.permissions.setGroupPermission(Actor.System, group, permission).getOrElse { error(it.toString()) }
        val role = createRole()
        gradeway.groups.addRoleToGroup(Actor.System, group.id.value, role).getOrElse { error(it.toString()) }
        assignRole(player, role)

        val grants = gradeway.permissions.explainPlayerPermission(player, permission)

        assertEquals(
            listOf(listOf(player.name), listOf(player.name, role.name, group.name)),
            grants.map { names(it.path) }
        )
        assertIs<GrantSource.Group>(grants[1].path.last())
    }

    @Test
    fun `explainPlayerPermission reports the wildcard node that matched`() {
        val prefix = "gradeway.test.${UUID.randomUUID().toString().take(8)}."
        gradeway.permissions.createPermission(Actor.System, prefix, PermissionEntity.Type.STARTS_WITH)
            .getOrElse { error(it.toString()) }
        val player = createPlayer()
        gradeway.permissions.setPlayerPermission(Actor.System, player, prefix).getOrElse { error(it.toString()) }

        val grants = gradeway.permissions.explainPlayerPermission(player, "${prefix}home")

        assertEquals(prefix, grants.single().permission.value)
        assertEquals(PermissionEntity.Type.STARTS_WITH, grants.single().permission.type)
    }

    @Test
    fun `explainPlayerPermission ignores disabled permissions and paused roles`() {
        val permission = uniquePermission()
        val player = createPlayer()
        gradeway.permissions.setPlayerPermission(Actor.System, player, permission, enabled = false)
            .getOrElse { error(it.toString()) }
        val role = createRole()
        gradeway.permissions.setRolePermission(Actor.System, role, permission).getOrElse { error(it.toString()) }
        assignRole(player, role)
        gradeway.players.pauseRole(Actor.System, player, role).getOrElse { error(it.toString()) }

        assertTrue(gradeway.permissions.explainPlayerPermission(player, permission).isEmpty())
    }

    @Test
    fun `explainPlayerPermission lists each path of diamond inheritance`() {
        val permission = uniquePermission()
        val base = createRole()
        gradeway.permissions.setRolePermission(Actor.System, base, permission).getOrElse { error(it.toString()) }
        val left = createRole()
        val right = createRole()
        addParent(left, base)
        addParent(right, base)
        val top = createRole()
        addParent(top, left)
        addParent(top, right)
        val player = createPlayer()
        assignRole(player, top)

        val paths = gradeway.permissions.explainPlayerPermission(player, permission).map { names(it.path) }.toSet()

        assertEquals(
            setOf(
                listOf(player.name, top.name, left.name, base.name),
                listOf(player.name, top.name, right.name, base.name),
            ),
            paths
        )
    }

    @Test
    fun `explainRolePermission starts the path at the role`() {
        val permission = uniquePermission()
        val parent = createRole()
        gradeway.permissions.setRolePermission(Actor.System, parent, permission).getOrElse { error(it.toString()) }
        val child = createRole()
        addParent(child, parent)

        val grants = gradeway.permissions.explainRolePermission(child, permission)

        assertEquals(listOf(child.name, parent.name), names(grants.single().path))
    }

    // --- who can ---

    @Test
    fun `findPermissionHolders includes inherited roles and players, marking direct holders`() {
        val permission = uniquePermission()
        val parent = createRole()
        gradeway.permissions.setRolePermission(Actor.System, parent, permission).getOrElse { error(it.toString()) }
        val child = createRole()
        addParent(child, parent)
        val unrelated = createRole()

        val viaRole = createPlayer()
        assignRole(viaRole, child)
        val direct = createPlayer()
        gradeway.permissions.setPlayerPermission(Actor.System, direct, permission).getOrElse { error(it.toString()) }
        val viaTemplate = createPlayer()
        val template = createTemplate(permission)
        gradeway.permissions.linkTemplateToPlayer(Actor.System, template, viaTemplate)
            .getOrElse { error(it.toString()) }
        val paused = createPlayer()
        assignRole(paused, parent)
        gradeway.players.pauseRole(Actor.System, paused, parent).getOrElse { error(it.toString()) }
        val without = createPlayer()
        assignRole(without, unrelated)

        val holders = gradeway.permissions.findPermissionHolders(permission)

        assertEquals(
            mapOf(parent.id.value to true, child.id.value to false),
            holders.roles.associate { it.id to it.direct }
        )
        assertEquals(
            mapOf(viaRole.id.value to false, direct.id.value to true, viaTemplate.id.value to true),
            holders.players.associate { it.id to it.direct }
        )
    }

    @Test
    fun `findPermissionHolders excludes players whose holder role is paused or expired`() {
        val permission = uniquePermission()
        val role = createRole()
        gradeway.permissions.setRolePermission(Actor.System, role, permission).getOrElse { error(it.toString()) }

        val active = createPlayer()
        assignRole(active, role)
        val temporary = createPlayer()
        gradeway.players.addRole(Actor.System, temporary.id.value, role, gradeway.now().plusSeconds(3600))
            .getOrElse { error(it.toString()) }
        val expired = createPlayer()
        val expiredAssignment = gradeway.players.addRole(
            Actor.System,
            expired.id.value,
            role,
            gradeway.now().plusSeconds(3600)
        ).getOrElse { error(it.toString()) }
        // Roles can't be assigned with a past expiry, so backdate the stored assignment instead.
        transaction(gradeway.database) {
            (expiredAssignment as DatabasePlayerRoleEntity).untilAt = gradeway.now().minusSeconds(60)
        }
        val paused = createPlayer()
        assignRole(paused, role)
        gradeway.players.pauseRole(Actor.System, paused, role).getOrElse { error(it.toString()) }

        val holders = gradeway.permissions.findPermissionHolders(permission)

        assertEquals(setOf(active.id.value, temporary.id.value), holders.players.map { it.id }.toSet())
        assertTrue(holders.players.none { it.direct })
    }

    @Test
    fun `findPermissionHolders agrees with effective permission resolution`() {
        val permission = uniquePermission()
        val group = createGroup()
        gradeway.permissions.setGroupPermission(Actor.System, group, permission).getOrElse { error(it.toString()) }
        val role = createRole()
        gradeway.groups.addRoleToGroup(Actor.System, group.id.value, role).getOrElse { error(it.toString()) }
        val child = createRole()
        addParent(child, role)
        val players = List(5) { createPlayer() }
        assignRole(players[0], role)
        assignRole(players[1], child)
        gradeway.permissions.setPlayerPermission(Actor.System, players[2], permission, enabled = false)
            .getOrElse { error(it.toString()) }

        val holderIds = gradeway.permissions.findPermissionHolders(permission).players.map { it.id }.toSet()

        players.forEach { player ->
            assertEquals(
                gradeway.permissions.hasEffectivePlayerPermission(player.id.value, permission),
                player.id.value in holderIds,
                "mismatch for ${player.name}"
            )
        }
        assertEquals(setOf(players[0].id.value, players[1].id.value), holderIds)
    }

    @Test
    fun `findPermissionHolders returns nothing for an unused permission`() {
        val holders = gradeway.permissions.findPermissionHolders(uniquePermission())

        assertTrue(holders.roles.isEmpty())
        assertTrue(holders.players.isEmpty())
    }
}
