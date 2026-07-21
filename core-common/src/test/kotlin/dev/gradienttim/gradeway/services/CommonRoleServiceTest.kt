/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload
import dev.gradienttim.gradeway.messaging.payloads.GroupChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.GroupRoleChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingAction
import dev.gradienttim.gradeway.messaging.payloads.RoleChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.RoleParentChangedPayload
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonRoleServiceTest {
    private val gradeway: CommonGradeway = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    private fun createRole(): RoleEntity = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }

    @Test
    fun `create rejects a duplicate name`() {
        val name = uniqueName("role")
        gradeway.roles.create(name).getOrElse { error(it.toString()) }

        val result = gradeway.roles.create(name)

        assertEquals(RoleService.CreateRoleError.EntityAlreadyExists, result.leftOrNull())
    }

    @Test
    fun `create rejects an invalid name`() {
        val result = gradeway.roles.create("")

        assertEquals(RoleService.CreateRoleError.InvalidName, result.leftOrNull())
    }

    @Test
    fun `getEffectiveWeight reflects an explicitly set weight`() {
        val role = createRole()

        gradeway.roles.setWeight(role, 42).getOrElse { error(it.toString()) }

        assertEquals(42, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `addParent rejects a self reference`() {
        val role = createRole()

        val result = gradeway.roles.addParent(role, role)

        assertEquals(RoleService.AddParentError.SelfReference, result.leftOrNull())
    }

    @Test
    fun `addParent rejects a relation that already exists`() {
        val role = createRole()
        val parent = createRole()
        gradeway.roles.addParent(role, parent).getOrElse { error(it.toString()) }

        val result = gradeway.roles.addParent(role, parent)

        assertEquals(RoleService.AddParentError.AlreadyParent, result.leftOrNull())
    }

    @Test
    fun `addParent rejects a cyclic relation`() {
        val grandparent = createRole()
        val parent = createRole()
        val child = createRole()

        gradeway.roles.addParent(parent, grandparent).getOrElse { error(it.toString()) }
        gradeway.roles.addParent(child, parent).getOrElse { error(it.toString()) }

        val result = gradeway.roles.addParent(grandparent, child)

        assertEquals(RoleService.AddParentError.CyclicRelation, result.leftOrNull())
    }

    @Test
    fun `addParent publishes a RoleParentChangedPayload`() {
        val role = createRole()
        val parent = createRole()

        val received = mutableListOf<dev.gradienttim.gradeway.messaging.payloads.MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.roles.addParent(role, parent).getOrElse { error(it.toString()) }

        assertEquals(
            listOf<dev.gradienttim.gradeway.messaging.payloads.MessagingPayload>(
                RoleParentChangedPayload(role.id.value.toString(), parent.id.value.toString(), MessagingAction.CREATED)
            ),
            received
        )
    }

    @Test
    fun `removeParent fails when the relation does not exist`() {
        val role = createRole()
        val parent = createRole()

        val result = gradeway.roles.removeParent(role, parent)

        assertEquals(RoleService.RemoveParentError.NotParent, result.leftOrNull())
    }

    @Test
    fun `effective weight cache is invalidated by a RoleChangedPayload for that role`() {
        val role = createRole()
        gradeway.roles.setWeight(role, 10).getOrElse { error(it.toString()) }
        assertEquals(10, gradeway.roles.getEffectiveWeight(role.id.value))

        // A direct DAO mutation of a RoleEntity column (unlike a join-row mutation) is itself
        // picked up by Exposed's EntityHook and auto-published as a RoleChangedPayload, so this
        // only confirms the end state rather than an intermediate "still stale" step.
        org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 99
        }
        gradeway.messaging.publish(RoleChangedPayload(role.id.value.toString(), MessagingAction.UPDATED))

        assertEquals(99, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `effective weight cache is fully invalidated by a GroupRoleChangedPayload`() {
        val role = createRole()
        gradeway.roles.setWeight(role, 10).getOrElse { error(it.toString()) }
        assertEquals(10, gradeway.roles.getEffectiveWeight(role.id.value))

        org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
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
        gradeway.roles.setWeight(role, 10).getOrElse { error(it.toString()) }
        assertEquals(10, gradeway.roles.getEffectiveWeight(role.id.value))

        org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 55
        }
        gradeway.messaging.publish(GroupChangedPayload(UUID.randomUUID().toString(), MessagingAction.UPDATED))

        assertEquals(55, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `effective weight cache is fully invalidated by a CacheFlushPayload`() {
        val role = createRole()
        gradeway.roles.setWeight(role, 10).getOrElse { error(it.toString()) }
        assertEquals(10, gradeway.roles.getEffectiveWeight(role.id.value))

        org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 33
        }
        gradeway.messaging.publish(CacheFlushPayload)

        assertEquals(33, gradeway.roles.getEffectiveWeight(role.id.value))
    }

    @Test
    fun `effective weight falls back to the highest group default weight`() {
        val role = createRole()
        val lowGroup = gradeway.groups.create(uniqueName("group")) { defaultWeight = 3 }
            .getOrElse { error(it.toString()) }
        val highGroup = gradeway.groups.create(uniqueName("group")) { defaultWeight = 8 }
            .getOrElse { error(it.toString()) }
        gradeway.groups.addRoleToGroup(lowGroup, role).getOrElse { error(it.toString()) }
        gradeway.groups.addRoleToGroup(highGroup, role).getOrElse { error(it.toString()) }

        assertEquals(gradeway.roles.getEffectiveWeight(role.id.value), 8)
    }
}
