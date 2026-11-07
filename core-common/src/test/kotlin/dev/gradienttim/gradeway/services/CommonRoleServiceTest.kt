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
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.messaging.payloads.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.*
import kotlin.test.*

class CommonRoleServiceTest {
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

    @Test
    fun `create rejects a duplicate name`() {
        val name = uniqueName("role")
        gradeway.roles.create(Actor.System, name).getOrElse { error(it.toString()) }

        val result = gradeway.roles.create(Actor.System, name)

        assertEquals(RoleService.CreateRoleError.EntityAlreadyExists, result.leftOrNull())
    }

    @Test
    fun `create rejects an invalid name`() {
        val result = gradeway.roles.create(Actor.System, "")

        assertEquals(RoleService.CreateRoleError.InvalidName, result.leftOrNull())
    }

    @Test
    fun `getEffectiveWeight reflects an explicitly set weight`() {
        val role = createRole()

        gradeway.roles.setWeight(Actor.System, role, 42).getOrElse { error(it.toString()) }

        assertEquals(42, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `addParent rejects a self reference`() {
        val role = createRole()

        val result = gradeway.roles.addParent(Actor.System, role, role)

        assertEquals(RoleService.AddParentError.SelfReference, result.leftOrNull())
    }

    @Test
    fun `addParent rejects a relation that already exists`() {
        val role = createRole()
        val parent = createRole()
        gradeway.roles.addParent(Actor.System, role, parent).getOrElse { error(it.toString()) }

        val result = gradeway.roles.addParent(Actor.System, role, parent)

        assertEquals(RoleService.AddParentError.AlreadyParent, result.leftOrNull())
    }

    @Test
    fun `addParent rejects a cyclic relation`() {
        val grandparent = createRole()
        val parent = createRole()
        val child = createRole()

        gradeway.roles.addParent(Actor.System, parent, grandparent).getOrElse { error(it.toString()) }
        gradeway.roles.addParent(Actor.System, child, parent).getOrElse { error(it.toString()) }

        val result = gradeway.roles.addParent(Actor.System, grandparent, child)

        assertEquals(RoleService.AddParentError.CyclicRelation, result.leftOrNull())
    }

    @Test
    fun `addParent publishes a RoleParentChangedPayload`() {
        val role = createRole()
        val parent = createRole()

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { if (it !is EventPayload) received.add(it) }

        gradeway.roles.addParent(Actor.System, role, parent).getOrElse { error(it.toString()) }

        assertEquals(
            listOf<MessagingPayload>(
                RoleParentChangedPayload(role.id.value.toString(), parent.id.value.toString(), MessagingAction.CREATED)
            ),
            received
        )
    }

    @Test
    fun `removeParent fails when the relation does not exist`() {
        val role = createRole()
        val parent = createRole()

        val result = gradeway.roles.removeParent(Actor.System, role, parent)

        assertEquals(RoleService.RemoveParentError.NotParent, result.leftOrNull())
    }

    @Test
    fun `effective weight cache is invalidated by a RoleChangedPayload for that role`() {
        val role = createRole()
        gradeway.roles.setWeight(Actor.System, role, 10).getOrElse { error(it.toString()) }
        assertEquals(10, gradeway.roles.getEffectiveWeight(role.id.value))

        // A direct DAO mutation of a RoleEntity column (unlike a join-row mutation) is itself
        // picked up by Exposed's EntityHook and auto-published as a RoleChangedPayload, so this
        // only confirms the end state rather than an intermediate "still stale" step.
        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 99
        }
        gradeway.messaging.publish(RoleChangedPayload(role.id.value.toString(), MessagingAction.UPDATED))

        assertEquals(99, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `effective weight cache is fully invalidated by a GroupRoleChangedPayload`() {
        val role = createRole()
        gradeway.roles.setWeight(Actor.System, role, 10).getOrElse { error(it.toString()) }
        assertEquals(10, gradeway.roles.getEffectiveWeight(role.id.value))

        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 77
        }
        gradeway.messaging.publish(
            GroupRoleChangedPayload(UUID.randomUUID().toString(), UUID.randomUUID().toString(), MessagingAction.CREATED)
        )

        assertEquals(77, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `effective weight cache is fully invalidated by a GroupChangedPayload`() {
        val role = createRole()
        gradeway.roles.setWeight(Actor.System, role, 10).getOrElse { error(it.toString()) }
        assertEquals(10, gradeway.roles.getEffectiveWeight(role.id.value))

        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 55
        }
        gradeway.messaging.publish(GroupChangedPayload(UUID.randomUUID().toString(), MessagingAction.UPDATED))

        assertEquals(55, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `effective weight cache is fully invalidated by a CacheFlushPayload`() {
        val role = createRole()
        gradeway.roles.setWeight(Actor.System, role, 10).getOrElse { error(it.toString()) }
        assertEquals(10, gradeway.roles.getEffectiveWeight(role.id.value))

        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 33
        }
        gradeway.messaging.publish(CacheFlushPayload)

        assertEquals(33, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `effective weight falls back to the highest group default weight`() {
        val role = createRole()
        val lowGroup = gradeway.groups.create(Actor.System, uniqueName("group")) { defaultWeight = 3 }
            .getOrElse { error(it.toString()) }
        val highGroup = gradeway.groups.create(Actor.System, uniqueName("group")) { defaultWeight = 8 }
            .getOrElse { error(it.toString()) }
        gradeway.groups.addRoleToGroup(Actor.System, lowGroup, role).getOrElse { error(it.toString()) }
        gradeway.groups.addRoleToGroup(Actor.System, highGroup, role).getOrElse { error(it.toString()) }

        assertEquals(8, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `setDefault makes the role the only default role`() {
        val first = createRole()
        val second = createRole()

        gradeway.roles.setDefault(Actor.System, first).getOrElse { error(it.toString()) }
        gradeway.roles.setDefault(Actor.System, second.id.value.toString()).getOrElse { error(it.toString()) }

        assertEquals(second.id.value, gradeway.roles.getDefaultRole()?.id?.value)
        assertFalse(gradeway.roles.findById(first.id.value)?.isDefault ?: error("expected role"))
        assertTrue(gradeway.roles.findById(second.id.value)?.isDefault ?: error("expected role"))
    }

    @Test
    fun `setDefault rejects a role that is already the default or does not exist`() {
        val role = createRole()
        gradeway.roles.setDefault(Actor.System, role).getOrElse { error(it.toString()) }

        assertEquals(
            RoleService.SetDefaultError.AlreadyDefault,
            gradeway.roles.setDefault(Actor.System, role).leftOrNull(),
        )
        assertEquals(
            RoleService.SetDefaultError.EntityNotFound,
            gradeway.roles.setDefault(Actor.System, UUID.randomUUID().toString()).leftOrNull()
        )
    }

    @Test
    fun `clearDefault removes the default role and fails when there is none`() {
        val role = createRole()
        gradeway.roles.setDefault(Actor.System, role).getOrElse { error(it.toString()) }

        gradeway.roles.clearDefault(Actor.System).getOrElse { error(it.toString()) }

        assertNull(gradeway.roles.getDefaultRole())
        assertEquals(
            RoleService.ClearDefaultError.NoDefaultRole,
            gradeway.roles.clearDefault(Actor.System).leftOrNull(),
        )
    }

    @Test
    fun `deleting the default role leaves no default role`() {
        val role = createRole()
        gradeway.roles.setDefault(Actor.System, role).getOrElse { error(it.toString()) }

        gradeway.roles.delete(Actor.System, role.id.value).getOrElse { error(it.toString()) }

        assertNull(gradeway.roles.getDefaultRole())
    }

    private fun flagAsDefault(vararg roles: RoleEntity) {
        transaction(gradeway.database) {
            roles.forEach { role ->
                RolesTable.update({ RolesTable.id eq role.id.value }) { it[isDefault] = true }
            }
        }
    }

    @Test
    fun `getDefaultRole picks the highest weight when several roles are flagged`() {
        val low = createRole()
        val high = createRole()
        gradeway.roles.setWeight(Actor.System, low, 1).getOrElse { error(it.toString()) }
        gradeway.roles.setWeight(Actor.System, high, 5).getOrElse { error(it.toString()) }
        flagAsDefault(low, high)

        assertEquals(high.id.value, gradeway.roles.getDefaultRole()?.id?.value)
    }

    @Test
    fun `setDefault leaves exactly one default role when several were flagged`() {
        val first = createRole()
        val second = createRole()
        flagAsDefault(first, second)

        gradeway.roles.setDefault(Actor.System, first).getOrElse { error(it.toString()) }

        assertTrue(gradeway.roles.findById(first.id.value)?.isDefault ?: error("expected role"))
        assertFalse(gradeway.roles.findById(second.id.value)?.isDefault ?: error("expected role"))
    }

    @Test
    fun `new roles are not the default role`() {
        assertFalse(createRole().isDefault)
        assertNull(gradeway.roles.getDefaultRole())
    }

    @Test
    fun `setDefaultFlag marks a role without unmarking the other default roles`() {
        val low = createRole()
        val high = createRole()
        gradeway.roles.setWeight(Actor.System, low, 1).getOrElse { error(it.toString()) }
        gradeway.roles.setWeight(Actor.System, high, 5).getOrElse { error(it.toString()) }

        gradeway.roles.setDefaultFlag(Actor.System, low, true).getOrElse { error(it.toString()) }
        gradeway.roles.setDefaultFlag(Actor.System, high.name, true).getOrElse { error(it.toString()) }

        assertEquals(listOf(high.id.value, low.id.value), gradeway.roles.getDefaultRoles().map { it.id.value })
        assertEquals(high.id.value, gradeway.roles.getDefaultRole()?.id?.value)
    }

    @Test
    fun `setDefaultFlag unmarks a role and rejects an unchanged flag or unknown role`() {
        val role = createRole()
        gradeway.roles.setDefaultFlag(Actor.System, role, true).getOrElse { error(it.toString()) }

        assertEquals(
            RoleService.SetDefaultFlagError.FlagAlreadySet,
            gradeway.roles.setDefaultFlag(Actor.System, role, true).leftOrNull()
        )
        gradeway.roles.setDefaultFlag(Actor.System, role, false).getOrElse { error(it.toString()) }
        assertTrue(gradeway.roles.getDefaultRoles().isEmpty())
        assertEquals(
            RoleService.SetDefaultFlagError.FlagAlreadySet,
            gradeway.roles.setDefaultFlag(Actor.System, role, false).leftOrNull()
        )
        assertEquals(
            RoleService.SetDefaultFlagError.EntityNotFound,
            gradeway.roles.setDefaultFlag(Actor.System, UUID.randomUUID().toString(), true).leftOrNull()
        )
    }

    @Test
    fun `setName renames the role so it resolves by its new name`() {
        val role = createRole()
        val newName = uniqueName("renamed")

        gradeway.roles.setName(Actor.System, role.name, newName).getOrElse { error(it.toString()) }

        assertEquals(role.id.value, gradeway.roles.findByName(newName)?.id?.value)
    }

    @Test
    fun `setName rejects an invalid, unchanged or already used name and an unknown role`() {
        val role = createRole()
        val other = createRole()

        assertEquals(RoleService.SetNameError.InvalidName, gradeway.roles.setName(Actor.System, role, "").leftOrNull())
        assertEquals(
            RoleService.SetNameError.NameAlreadySet,
            gradeway.roles.setName(Actor.System, role, role.name).leftOrNull(),
        )
        assertEquals(
            RoleService.SetNameError.NameAlreadyExists,
            gradeway.roles.setName(Actor.System, role, other.name).leftOrNull(),
        )
        assertEquals(
            RoleService.SetNameError.EntityNotFound,
            gradeway.roles.setName(Actor.System, UUID.randomUUID().toString(), uniqueName("role")).leftOrNull()
        )
    }
}
