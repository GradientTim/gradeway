/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import dev.gradienttim.gradeway.audit.Actor
import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerRoleEntity
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import java.util.*
import kotlin.test.*

class CommonPermissionServiceTest {
    private val gradeway: CommonGradeway<TestPlatformConfig> = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    private fun createRole(): RoleEntity = gradeway.roles.create(
        Actor.System,
        uniqueName("role"),
    ).getOrElse { error(it.toString()) }

    private fun createPlayer(): PlayerEntity =
        gradeway.players.create(
            Actor.System,
            UUID.randomUUID(),
            uniqueName("player"),
        ).getOrElse { error(it.toString()) }

    // --- Permission CRUD ---

    @Test
    fun `createPermission rejects a duplicate value`() {
        val value = "gradeway.test.${UUID.randomUUID()}"
        gradeway.permissions.createPermission(Actor.System, value).getOrElse { error(it.toString()) }

        val result = gradeway.permissions.createPermission(Actor.System, value)

        assertEquals(PermissionService.CreatePermissionError.AlreadyExists, result.leftOrNull())
    }

    @Test
    fun `updatePermissionValue rejects setting the same value`() {
        val permission = gradeway.permissions.createPermission(Actor.System, "gradeway.test.${UUID.randomUUID()}")
            .getOrElse { error(it.toString()) }

        val result = gradeway.permissions.updatePermissionValue(Actor.System, permission, permission.value)

        assertEquals(PermissionService.UpdatePermissionValueError.ValueAlreadySet, result.leftOrNull())
    }

    @Test
    fun `updatePermissionType rejects setting the same type`() {
        val permission = gradeway.permissions.createPermission(Actor.System, 
            "gradeway.test.${UUID.randomUUID()}",
            PermissionEntity.Type.EQUALS
        ).getOrElse { error(it.toString()) }

        val result = gradeway.permissions.updatePermissionType(Actor.System, permission, PermissionEntity.Type.EQUALS)

        assertEquals(PermissionService.UpdatePermissionTypeError.TypeAlreadySet, result.leftOrNull())
    }

    // --- Template CRUD ---

    @Test
    fun `createTemplate rejects an invalid name`() {
        val result = gradeway.permissions.createTemplate(Actor.System, "")

        assertEquals(PermissionService.CreateTemplateError.InvalidName, result.leftOrNull())
    }

    @Test
    fun `addPermissionToTemplate rejects adding the same permission twice`() {
        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        val permission = gradeway.permissions.createPermission(Actor.System, "gradeway.test.${UUID.randomUUID()}")
            .getOrElse { error(it.toString()) }
        gradeway.permissions.addPermissionToTemplate(
            Actor.System,
            template,
            permission,
        ).getOrElse { error(it.toString()) }

        val result = gradeway.permissions.addPermissionToTemplate(Actor.System, template, permission)

        assertEquals(PermissionService.AddPermissionToTemplateError.PermissionAlreadyExists, result.leftOrNull())
    }

    @Test
    fun `removePermissionFromTemplate fails when the permission is not on the template`() {
        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        val permission = gradeway.permissions.createPermission(Actor.System, "gradeway.test.${UUID.randomUUID()}")
            .getOrElse { error(it.toString()) }

        val result = gradeway.permissions.removePermissionFromTemplate(Actor.System, template, permission)

        assertEquals(PermissionService.RemovePermissionFromTemplateError.PermissionNotExists, result.leftOrNull())
    }

    // --- Template linking / applying / revoking (role) ---

    @Test
    fun `linkTemplateToRole rejects a template not assignable to roles`() {
        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        gradeway.permissions.setTemplateAssignedTo(Actor.System, template, PermissionTemplateEntity.AssignedTo.PLAYER)
            .getOrElse { error(it.toString()) }
        val role = createRole()

        val result = gradeway.permissions.linkTemplateToRole(Actor.System, template, role)

        assertEquals(PermissionService.LinkTemplateError.WrongAssignedTo, result.leftOrNull())
    }

    @Test
    fun `linkTemplateToRole rejects linking the same template twice`() {
        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        val role = createRole()
        gradeway.permissions.linkTemplateToRole(Actor.System, template, role).getOrElse { error(it.toString()) }

        val result = gradeway.permissions.linkTemplateToRole(Actor.System, template, role)

        assertEquals(PermissionService.LinkTemplateError.AlreadyLinked, result.leftOrNull())
    }

    @Test
    fun `unlinkTemplateFromRole fails when not linked`() {
        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        val role = createRole()

        val result = gradeway.permissions.unlinkTemplateFromRole(Actor.System, template, role)

        assertEquals(PermissionService.UnlinkTemplateError.NotLinked, result.leftOrNull())
    }

    @Test
    fun `a role linked to a template resolves the template's permissions as effective without owning them`() {
        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        val permissionValue = "gradeway.test.${UUID.randomUUID()}"
        val permission = gradeway.permissions.createPermission(
            Actor.System,
            permissionValue,
        ).getOrElse { error(it.toString()) }
        gradeway.permissions.addPermissionToTemplate(
            Actor.System,
            template,
            permission,
        ).getOrElse { error(it.toString()) }
        val role = createRole()

        gradeway.permissions.linkTemplateToRole(Actor.System, template, role).getOrElse { error(it.toString()) }

        assertTrue(gradeway.permissions.hasEffectiveRolePermission(role, permissionValue))
        assertTrue(gradeway.roles.getPermissions(role).isEmpty())
    }

    @Test
    fun `applying a template to a role copies its permissions onto the role's own set`() {
        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        val permissionValue = "gradeway.test.${UUID.randomUUID()}"
        val permission = gradeway.permissions.createPermission(
            Actor.System,
            permissionValue,
        ).getOrElse { error(it.toString()) }
        gradeway.permissions.addPermissionToTemplate(
            Actor.System,
            template,
            permission,
        ).getOrElse { error(it.toString()) }
        val role = createRole()

        gradeway.permissions.applyTemplateToRole(Actor.System, template, role).getOrElse { error(it.toString()) }

        val hasPermission = org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            gradeway.roles.getPermissions(role).any { it.permission.value == permissionValue }
        }
        assertTrue(hasPermission)
    }

    @Test
    fun `revoking a template from a role removes its previously applied permissions`() {
        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        val permissionValue = "gradeway.test.${UUID.randomUUID()}"
        val permission = gradeway.permissions.createPermission(
            Actor.System,
            permissionValue,
        ).getOrElse { error(it.toString()) }
        gradeway.permissions.addPermissionToTemplate(
            Actor.System,
            template,
            permission,
        ).getOrElse { error(it.toString()) }
        val role = createRole()
        gradeway.permissions.applyTemplateToRole(Actor.System, template, role).getOrElse { error(it.toString()) }

        gradeway.permissions.revokeTemplateFromRole(Actor.System, template, role).getOrElse { error(it.toString()) }

        assertFalse(gradeway.roles.getPermissions(role).any { it.permission.value == permissionValue })
    }

    // --- Set/unset permission error semantics ---

    @Test
    fun `setPermission rejects enabling an already-enabled permission`() {
        val role = createRole()
        val permissionValue = "gradeway.test.${UUID.randomUUID()}"
        gradeway.roles.setPermission(Actor.System, role, permissionValue, true).getOrElse { error(it.toString()) }

        val result = gradeway.roles.setPermission(Actor.System, role, permissionValue, true)

        assertEquals(PermissionService.SetPermissionError.PermissionAlreadyEnabled, result.leftOrNull())
    }

    @Test
    fun `unsetPermission fails when the permission was never set`() {
        val role = createRole()

        val result = gradeway.roles.unsetPermission(Actor.System, role, "gradeway.test.${UUID.randomUUID()}")

        assertEquals(PermissionService.UnsetPermissionError.PermissionNotFound, result.leftOrNull())
    }

    // --- Effective permission resolution (the core resolution algorithm) ---

    @Test
    fun `a role inherits effective permissions from a multi-level parent chain`() {
        val grandparent = createRole()
        val parent = createRole()
        val child = createRole()
        val permissionValue = "gradeway.test.${UUID.randomUUID()}"
        gradeway.roles.setPermission(
            Actor.System,
            grandparent,
            permissionValue,
            true,
        ).getOrElse { error(it.toString()) }
        gradeway.roles.addParent(Actor.System, parent, grandparent).getOrElse { error(it.toString()) }
        gradeway.roles.addParent(Actor.System, child, parent).getOrElse { error(it.toString()) }

        assertTrue(gradeway.permissions.hasEffectiveRolePermission(child, permissionValue))
    }

    @Test
    fun `a role inherits effective permissions from its groups`() {
        val role = createRole()
        val group = gradeway.groups.create(Actor.System, uniqueName("group")).getOrElse { error(it.toString()) }
        val permissionValue = "gradeway.test.${UUID.randomUUID()}"
        gradeway.groups.setPermission(Actor.System, group, permissionValue, true).getOrElse { error(it.toString()) }
        gradeway.groups.addRoleToGroup(Actor.System, group, role).getOrElse { error(it.toString()) }

        assertTrue(gradeway.permissions.hasEffectiveRolePermission(role, permissionValue))
    }

    @Test
    fun `a permission cycle in role parents does not infinite-loop`() {
        val roleA = createRole()
        val roleB = createRole()
        val permissionValue = "gradeway.test.${UUID.randomUUID()}"
        gradeway.roles.setPermission(Actor.System, roleB, permissionValue, true).getOrElse { error(it.toString()) }
        // roleB is a parent of roleA; directly wire roleA back as a parent of roleB to defeat the
        // service-level cycle guard and exercise CommonCaches' own visitedRoleIds recursion guard.
        gradeway.roles.addParent(Actor.System, roleA, roleB).getOrElse { error(it.toString()) }
        org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            dev.gradienttim.gradeway.database.models.role.DatabaseRoleParentEntity.new {
                this.parentId = roleA.id
                this.childId = roleB.id
            }
        }

        // Would stack-overflow instead of completing if the visitedRoleIds guard were missing.
        assertTrue(gradeway.permissions.hasEffectiveRolePermission(roleA, permissionValue))
    }

    @Test
    fun `a player's effective permissions combine own, template, and active role permissions`() {
        val player = createPlayer()
        val ownPermission = "gradeway.test.own.${UUID.randomUUID()}"
        val templatePermission = "gradeway.test.template.${UUID.randomUUID()}"
        val rolePermission = "gradeway.test.role.${UUID.randomUUID()}"

        gradeway.players.setPermission(Actor.System, player, ownPermission, true).getOrElse { error(it.toString()) }

        val template = gradeway.permissions.createTemplate(
            Actor.System,
            uniqueName("template"),
        ).getOrElse { error(it.toString()) }
        gradeway.permissions.setTemplateAssignedTo(Actor.System, template, PermissionTemplateEntity.AssignedTo.PLAYER)
            .getOrElse { error(it.toString()) }
        val templatePermissionEntity =
            gradeway.permissions.createPermission(Actor.System, templatePermission).getOrElse { error(it.toString()) }
        gradeway.permissions.addPermissionToTemplate(Actor.System, template, templatePermissionEntity)
            .getOrElse { error(it.toString()) }
        gradeway.permissions.linkTemplateToPlayer(Actor.System, template, player).getOrElse { error(it.toString()) }

        val role = createRole()
        gradeway.roles.setPermission(Actor.System, role, rolePermission, true).getOrElse { error(it.toString()) }
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }

        val effective = gradeway.permissions.getEffectivePlayerPermissions(player).map { it.value }.toSet()
        assertEquals(setOf(ownPermission, templatePermission, rolePermission), effective)
    }

    @Test
    fun `a paused role does not contribute to a player's effective permissions`() {
        val player = createPlayer()
        val role = createRole()
        val rolePermission = "gradeway.test.${UUID.randomUUID()}"
        gradeway.roles.setPermission(Actor.System, role, rolePermission, true).getOrElse { error(it.toString()) }
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }
        gradeway.players.pauseRole(Actor.System, player, role).getOrElse { error(it.toString()) }

        assertFalse(gradeway.permissions.hasEffectivePlayerPermission(player, rolePermission))
    }

    @Test
    fun `an expired role does not contribute to a player's effective permissions`() {
        val player = createPlayer()
        val role = createRole()
        val rolePermission = "gradeway.test.${UUID.randomUUID()}"
        gradeway.roles.setPermission(Actor.System, role, rolePermission, true).getOrElse { error(it.toString()) }
        gradeway.players.addRole(Actor.System, player, role, java.time.Instant.now().plusSeconds(60))
            .getOrElse { error(it.toString()) }
        org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            (player.roles.first { it.roleId == role.id } as DatabasePlayerRoleEntity)
                .untilAt = java.time.Instant.now().minusSeconds(1)
        }

        assertFalse(gradeway.permissions.hasEffectivePlayerPermission(player, rolePermission))
    }

    @Test
    fun `hasRoleAllPermissions requires every permission to be enabled`() {
        val role = createRole()
        val permissionA = "gradeway.test.a.${UUID.randomUUID()}"
        val permissionB = "gradeway.test.b.${UUID.randomUUID()}"
        gradeway.roles.setPermission(Actor.System, role, permissionA, true).getOrElse { error(it.toString()) }
        gradeway.roles.setPermission(Actor.System, role, permissionB, false).getOrElse { error(it.toString()) }

        assertFalse(gradeway.roles.hasAllPermissions(role, listOf(permissionA, permissionB)))
        assertTrue(gradeway.roles.hasAnyPermissions(role, listOf(permissionA, permissionB)))
    }
}
