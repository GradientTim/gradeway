/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.messaging.payloads.GroupRoleChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingAction
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CommonGroupServiceTest {
    private val gradeway: CommonGradeway = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    @Test
    fun `create rejects a blank name`() {
        val result = gradeway.groups.create("")

        assertEquals(GroupService.CreateGroupError.InvalidName, result.leftOrNull())
    }

    @Test
    fun `create rejects a name exceeding the max length`() {
        val result = gradeway.groups.create("a".repeat(64))

        assertEquals(GroupService.CreateGroupError.InvalidName, result.leftOrNull())
    }

    @Test
    fun `delete fails when the group does not exist`() {
        val result = gradeway.groups.delete(UUID.randomUUID())

        assertEquals(GroupService.DeleteGroupError.EntityNotFound, result.leftOrNull())
    }

    @Test
    fun `delete removes an existing group`() {
        val group = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }

        gradeway.groups.delete(group).getOrElse { error(it.toString()) }

        assertEquals(null, gradeway.groups.findById(group.id.value))
    }

    @Test
    fun `setName rejects setting the same name`() {
        val name = uniqueName("group")
        val group = gradeway.groups.create(name).getOrElse { error(it.toString()) }

        val result = gradeway.groups.setName(group, name)

        assertEquals(GroupService.SetNameError.NameAlreadySet, result.leftOrNull())
    }

    @Test
    fun `setName rejects an invalid new name`() {
        val group = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }

        val result = gradeway.groups.setName(group, "")

        assertEquals(GroupService.SetNameError.InvalidName, result.leftOrNull())
    }

    @Test
    fun `setDefaultWeight rejects setting the same weight`() {
        val group = gradeway.groups.create(uniqueName("group")) { defaultWeight = 5 }.getOrElse { error(it.toString()) }

        val result = gradeway.groups.setDefaultWeight(group, 5)

        assertEquals(GroupService.SetDefaultWeightError.WeightAlreadySet, result.leftOrNull())
    }

    @Test
    fun `findByIdOrName matches by both id and name`() {
        val name = uniqueName("group")
        val group = gradeway.groups.create(name).getOrElse { error(it.toString()) }

        assertEquals(group.id.value, gradeway.groups.findByIdOrName(group.id.value.toString())?.id?.value)
        assertEquals(group.id.value, gradeway.groups.findByIdOrName(name)?.id?.value)
    }

    @Test
    fun `addRoleToGroup rejects adding the same role to the same group twice`() {
        val group = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }
        val role = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }

        gradeway.groups.addRoleToGroup(group, role).getOrElse { error(it.toString()) }
        val result = gradeway.groups.addRoleToGroup(group, role)

        assertEquals(GroupService.AddTargetError.AlreadyInGroup, result.leftOrNull())
    }

    @Test
    fun `addRoleToGroup publishes a GroupRoleChangedPayload`() {
        val group = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }
        val role = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.groups.addRoleToGroup(group, role).getOrElse { error(it.toString()) }

        assertEquals(
            listOf<MessagingPayload>(
                GroupRoleChangedPayload(group.id.value.toString(), role.id.value.toString(), MessagingAction.CREATED)
            ),
            received
        )
    }

    @Test
    fun `removeRoleFromGroup fails when the role is not in the group`() {
        val group = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }
        val role = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }

        val result = gradeway.groups.removeRoleFromGroup(group, role)

        assertEquals(GroupService.RemoveTargetError.NotInGroup, result.leftOrNull())
    }

    @Test
    fun `removeRoleFromGroup publishes a GroupRoleChangedPayload`() {
        val group = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }
        val role = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }
        gradeway.groups.addRoleToGroup(group, role).getOrElse { error(it.toString()) }

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.groups.removeRoleFromGroup(group, role).getOrElse { error(it.toString()) }

        assertEquals(
            listOf<MessagingPayload>(
                GroupRoleChangedPayload(group.id.value.toString(), role.id.value.toString(), MessagingAction.DELETED)
            ),
            received
        )
    }

    @Test
    fun `list respects the limit`() {
        repeat(3) { gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) } }

        // list() returns a lazy SizedIterable that must still be consumed within a transaction.
        val count = org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            gradeway.groups.list(limit = 2).count()
        }

        assertEquals(2, count)
    }

    @Test
    fun `list returns every created group`() {
        val first = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }
        val second = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }

        val results = org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            gradeway.groups.list().toList()
        }

        assertNotNull(results.find { it.id.value == first.id.value })
        assertNotNull(results.find { it.id.value == second.id.value })
    }
}
