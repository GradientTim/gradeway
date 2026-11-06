/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.events.permission.PermissionsAssignEvent
import dev.gradienttim.gradeway.events.permission.PermissionsAssignedEvent
import dev.gradienttim.gradeway.events.player.PlayerRoleAddEvent
import dev.gradienttim.gradeway.events.role.RoleCreateEvent
import dev.gradienttim.gradeway.events.role.RoleCreatedEvent
import dev.gradienttim.gradeway.events.role.RoleRenamedEvent
import dev.gradienttim.gradeway.managers.subscribe
import dev.gradienttim.gradeway.messaging.payloads.EventPayload
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.services.RoleService
import dev.gradienttim.gradeway.services.track.PlayerTrackService
import java.util.*
import kotlin.test.*

class EventDispatchTest {
    private val gradeway: CommonGradeway<TestPlatformConfig> = createTestGradeway()
    private val fired = mutableListOf<Event<*>>()

    init {
        gradeway.events.subscribe<Event<*>>(this, EventPriority.MONITOR) { fired += it }
    }

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    private inline fun <reified T : Event<*>> firedOf(): List<T> = fired.filterIsInstance<T>()

    private inline fun <reified T : Event<Event.CancelledResult>> cancel() {
        gradeway.events.subscribe<T>(this) { it.result = Event.CancelledResult.Cancel }
    }

    @Test
    fun `creating a role fires the pre and post event`() {
        val name = uniqueName("role")
        val role = gradeway.roles.create(Actor.Console, name) { weight = 10 }.getOrElse { error(it.toString()) }

        val pre = firedOf<RoleCreateEvent>().single()
        assertEquals(name, pre.spec.name)
        assertEquals(10, pre.spec.weight)
        assertEquals(Actor.Console, pre.actor)

        val post = firedOf<RoleCreatedEvent>().single()
        assertEquals(role.id.value, post.roleId)
        assertEquals(10, post.weight)
        assertEquals(EventOrigin.Local, post.origin)
    }

    @Test
    fun `cancelling the pre event prevents the change and the post event`() {
        cancel<RoleCreateEvent>()
        val name = uniqueName("role")

        val result = gradeway.roles.create(Actor.System, name)

        assertEquals(RoleService.CreateRoleError.Cancelled, result.leftOrNull())
        assertNull(gradeway.roles.findByName(name))
        assertTrue(firedOf<RoleCreatedEvent>().isEmpty())
    }

    @Test
    fun `listeners can change the create spec`() {
        val changedName = uniqueName("changed")
        gradeway.events.subscribe<RoleCreateEvent>(this) { it.spec.name = changedName }

        val role = gradeway.roles.create(Actor.System, uniqueName("role")).getOrElse { error(it.toString()) }

        assertEquals(changedName, role.name)
        assertEquals(changedName, firedOf<RoleCreatedEvent>().single().name)
    }

    @Test
    fun `failed operations do not fire post events`() {
        val result = gradeway.roles.create(Actor.System, " ")

        assertEquals(RoleService.CreateRoleError.InvalidName, result.leftOrNull())
        assertTrue(firedOf<RoleCreatedEvent>().isEmpty())
    }

    @Test
    fun `update post events carry the previous and new value`() {
        val name = uniqueName("role")
        val role = gradeway.roles.create(Actor.System, name).getOrElse { error(it.toString()) }
        val newName = uniqueName("renamed")

        gradeway.roles.setName(Actor.System, role, newName).getOrElse { error(it.toString()) }

        val renamed = firedOf<RoleRenamedEvent>().single()
        assertEquals(role.id.value, renamed.roleId)
        assertEquals(name, renamed.from)
        assertEquals(newName, renamed.to)
    }

    @Test
    fun `permission events identify their subject`() {
        val role = gradeway.roles.create(Actor.System, uniqueName("role")).getOrElse { error(it.toString()) }

        gradeway.permissions.setRolePermission(Actor.System, role, "gradeway.test", true)
            .getOrElse { error(it.toString()) }

        val assigned = firedOf<PermissionsAssignedEvent>().single()
        assertEquals(EventSubject.Role(role.id.value), assigned.subject)
        assertEquals(mapOf("gradeway.test" to true), assigned.permissions)
    }

    @Test
    fun `cancelling a permission assignment returns the cancelled error`() {
        val role = gradeway.roles.create(Actor.System, uniqueName("role")).getOrElse { error(it.toString()) }
        cancel<PermissionsAssignEvent>()

        val result = gradeway.permissions.setRolePermission(Actor.System, role, "gradeway.test", true)

        assertEquals(PermissionService.SetPermissionError.Cancelled, result.leftOrNull())
        assertFalse(gradeway.permissions.hasRolePermission(role, "gradeway.test"))
    }

    @Test
    fun `cancelling a role change during a promotion cancels the promotion`() {
        val role = gradeway.roles.create(Actor.System, uniqueName("role")).getOrElse { error(it.toString()) }
        val track = gradeway.tracks.createTrack(Actor.System, uniqueName("track")).getOrElse { error(it.toString()) }
        gradeway.tracks.addStage(Actor.System, track, role).getOrElse { error(it.toString()) }
        val player = gradeway.players.create(Actor.System, UUID.randomUUID(), uniqueName("player"))
            .getOrElse { error(it.toString()) }
        cancel<PlayerRoleAddEvent>()

        val result = gradeway.tracks.promotePlayer(Actor.System, player, track)

        assertEquals(PlayerTrackService.PromotePlayerError.Cancelled, result.leftOrNull())
    }

    @Test
    fun `post events are relayed and fired with a remote origin on other servers`() {
        val payloads = mutableListOf<EventPayload>()
        gradeway.messaging.subscribe { payload -> if (payload is EventPayload) payloads += payload }
        val role = gradeway.roles.create(Actor.System, uniqueName("role")).getOrElse { error(it.toString()) }
        val relayed = payloads.single()
        assertEquals(gradeway.messaging.serverId, relayed.serverId)
        fired.clear()

        gradeway.handleRemoteEvent(relayed.copy(serverId = "other-server"))

        val remote = firedOf<RoleCreatedEvent>().single()
        assertEquals(role.id.value, remote.roleId)
        assertEquals(Actor.System, remote.actor)
        assertEquals(EventOrigin.Remote("other-server"), remote.origin)
    }

    @Test
    fun `relayed events from this server are ignored`() {
        val payloads = mutableListOf<EventPayload>()
        gradeway.messaging.subscribe { payload -> if (payload is EventPayload) payloads += payload }
        gradeway.roles.create(Actor.System, uniqueName("role")).getOrElse { error(it.toString()) }
        fired.clear()

        gradeway.handleRemoteEvent(payloads.single())

        assertTrue(fired.isEmpty())
    }
}
