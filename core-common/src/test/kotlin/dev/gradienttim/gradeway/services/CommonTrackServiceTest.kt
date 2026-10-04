/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.messaging.payloads.MessagingAction
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import dev.gradienttim.gradeway.messaging.payloads.TrackChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.TrackStageChangedPayload
import dev.gradienttim.gradeway.services.track.PlayerTrackService
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.*

class CommonTrackServiceTest {
    private val gradeway: CommonGradeway<TestPlatformConfig> = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    private fun createTrack(): TrackEntity =
        gradeway.tracks.createTrack(uniqueName("track")).getOrElse { error(it.toString()) }

    private fun createRole(): RoleEntity = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }

    private fun createPlayer(): PlayerEntity =
        gradeway.players.create(UUID.randomUUID(), uniqueName("player")).getOrElse { error(it.toString()) }

    private fun createTrackWithStages(vararg roles: RoleEntity): TrackEntity {
        val track = createTrack()
        roles.forEach { role -> gradeway.tracks.addStage(track, role).getOrElse { error(it.toString()) } }
        return track
    }

    private fun primaryRoleIdOf(player: PlayerEntity): UUID? =
        gradeway.players.findById(player.id.value)?.primaryRoleId?.value

    private fun roleIdsOf(player: PlayerEntity): Set<UUID> {
        val reloaded = gradeway.players.findById(player.id.value) ?: error("expected player to exist")
        return transaction(gradeway.database) { reloaded.roles.map { it.roleId.value }.toSet() }
    }

    @Test
    fun `createTrack rejects a duplicate slug`() {
        val slug = uniqueName("track")
        gradeway.tracks.createTrack(slug).getOrElse { error(it.toString()) }

        val result = gradeway.tracks.createTrack(slug)

        assertEquals(TrackService.CreateTrackError.EntityAlreadyExists, result.leftOrNull())
    }

    @Test
    fun `createTrack rejects an invalid slug`() {
        assertEquals(TrackService.CreateTrackError.InvalidSlug, gradeway.tracks.createTrack("").leftOrNull())
        assertEquals(
            TrackService.CreateTrackError.InvalidSlug,
            gradeway.tracks.createTrack("a".repeat(64)).leftOrNull()
        )
    }

    @Test
    fun `createTrack publishes a TrackChangedPayload`() {
        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        val track = createTrack()

        assertContains(received, TrackChangedPayload(track.id.value.toString(), MessagingAction.CREATED))
    }

    @Test
    fun `findTrackByIdOrSlug resolves both the id and the slug`() {
        val track = createTrack()

        assertEquals(track.id, gradeway.tracks.findTrackByIdOrSlug(track.id.value.toString())?.id)
        assertEquals(track.id, gradeway.tracks.findTrackByIdOrSlug(track.slug)?.id)
        assertNull(gradeway.tracks.findTrackByIdOrSlug(uniqueName("missing")))
    }

    @Test
    fun `setSlug changes the slug and keeps the track resolvable by its new slug`() {
        val track = createTrack()
        val newSlug = uniqueName("renamed")

        gradeway.tracks.setSlug(track.id.value, newSlug).getOrElse { error(it.toString()) }

        assertEquals(track.id.value, gradeway.tracks.findTrackBySlug(newSlug)?.id?.value)
        assertNull(gradeway.tracks.findTrackBySlug(track.slug))
    }

    @Test
    fun `setSlug rejects an invalid, unchanged or already used slug`() {
        val track = createTrack()
        val other = createTrack()

        assertEquals(TrackService.SetSlugError.InvalidSlug, gradeway.tracks.setSlug(track, "").leftOrNull())
        assertEquals(TrackService.SetSlugError.SlugAlreadySet, gradeway.tracks.setSlug(track, track.slug).leftOrNull())
        assertEquals(
            TrackService.SetSlugError.SlugAlreadyExists,
            gradeway.tracks.setSlug(track, other.slug).leftOrNull()
        )
    }

    @Test
    fun `setSlug fails for an unknown track`() {
        val result = gradeway.tracks.setSlug(UUID.randomUUID(), uniqueName("track"))

        assertEquals(TrackService.SetSlugError.EntityNotFound, result.leftOrNull())
    }

    @Test
    fun `setSlug publishes an updated TrackChangedPayload`() {
        val track = createTrack()
        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.tracks.setSlug(track, uniqueName("renamed")).getOrElse { error(it.toString()) }

        assertContains(received, TrackChangedPayload(track.id.value.toString(), MessagingAction.UPDATED))
    }

    @Test
    fun `deleteTrack removes the track and its stages`() {
        val role = createRole()
        val track = createTrackWithStages(role)
        val stage = gradeway.tracks.findStageByRole(track.id.value, role.id.value) ?: error("expected stage")

        gradeway.tracks.deleteTrack(track.slug).getOrElse { error(it.toString()) }

        assertNull(gradeway.tracks.findTrackById(track.id.value))
        assertNull(gradeway.tracks.findStageById(stage.id.value))
        assertTrue(gradeway.roles.existsById(role.id.value))
    }

    @Test
    fun `deleteTrack fails for an unknown track`() {
        val result = gradeway.tracks.deleteTrack(UUID.randomUUID())

        assertEquals(TrackService.DeleteTrackError.EntityNotFound, result.leftOrNull())
    }

    @Test
    fun `deleting a role removes its stage from the track`() {
        val role = createRole()
        val track = createTrackWithStages(role)
        val stage = gradeway.tracks.findStageByRole(track.id.value, role.id.value) ?: error("expected stage")

        gradeway.roles.delete(role.id.value).getOrElse { error(it.toString()) }

        assertNull(gradeway.tracks.findStageById(stage.id.value))
        assertNotNull(gradeway.tracks.findTrackById(track.id.value))
    }

    @Test
    fun `addStage appends stages at increasing positions`() {
        val first = createRole()
        val second = createRole()
        val third = createRole()
        val track = createTrackWithStages(first, second, third)

        listOf(first, second, third).forEachIndexed { position, role ->
            val stage = gradeway.tracks.findStageByRole(track.id.value, role.id.value) ?: error("expected stage")
            assertEquals(position, stage.position)
        }
    }

    @Test
    fun `addStage appends after the highest position even when an earlier stage was removed`() {
        val first = createRole()
        val second = createRole()
        val track = createTrackWithStages(first, second)
        val firstStage = gradeway.tracks.findStageByRole(track.id.value, first.id.value) ?: error("expected stage")
        gradeway.tracks.removeStage(firstStage).getOrElse { error(it.toString()) }

        val third = gradeway.tracks.addStage(track, createRole()).getOrElse { error(it.toString()) }

        assertEquals(2, third.position)
    }

    @Test
    fun `addStage rejects a role that is already on the track`() {
        val role = createRole()
        val track = createTrackWithStages(role)

        val result = gradeway.tracks.addStage(track, role)

        assertEquals(TrackService.AddStageError.AlreadyExists, result.leftOrNull())
    }

    @Test
    fun `addStage allows the same role on different tracks`() {
        val role = createRole()
        createTrackWithStages(role)
        val otherTrack = createTrack()

        val result = gradeway.tracks.addStage(otherTrack, role)

        assertTrue(result.isRight())
    }

    @Test
    fun `addStage fails for an unknown track or role`() {
        val track = createTrack()
        val role = createRole()

        assertEquals(
            TrackService.AddStageError.EntityNotFound,
            gradeway.tracks.addStage(UUID.randomUUID(), role.id.value).leftOrNull()
        )
        assertEquals(
            TrackService.AddStageError.TargetNotFound,
            gradeway.tracks.addStage(track.id.value, UUID.randomUUID()).leftOrNull()
        )
    }

    @Test
    fun `addStage and removeStage publish TrackStageChangedPayloads`() {
        val track = createTrack()
        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        val stage = gradeway.tracks.addStage(track, createRole()).getOrElse { error(it.toString()) }
        gradeway.tracks.removeStage(stage.id.value).getOrElse { error(it.toString()) }

        val trackId = track.id.value.toString()
        val stageId = stage.id.value.toString()
        assertContains(received, TrackStageChangedPayload(trackId, stageId, MessagingAction.CREATED))
        assertContains(received, TrackStageChangedPayload(trackId, stageId, MessagingAction.DELETED))
    }

    @Test
    fun `removeStage fails for an unknown stage`() {
        val result = gradeway.tracks.removeStage(UUID.randomUUID())

        assertEquals(TrackService.RemoveStageError.EntityNotFound, result.leftOrNull())
    }

    private fun stageOrderOf(track: TrackEntity, vararg roles: RoleEntity): List<Pair<UUID, Int>> =
        roles.map { role ->
            val stage = gradeway.tracks.findStageByRole(track.id.value, role.id.value) ?: error("expected stage")
            role.id.value to stage.position
        }.sortedBy { it.second }

    @Test
    fun `moveStage moves a stage forward and shifts the others back`() {
        val first = createRole()
        val second = createRole()
        val third = createRole()
        val track = createTrackWithStages(first, second, third)
        val stage = gradeway.tracks.findStageByRole(track.id.value, first.id.value) ?: error("expected stage")

        val moved = gradeway.tracks.moveStage(stage, 2).getOrElse { error(it.toString()) }

        assertEquals(2, moved.position)
        assertEquals(
            listOf(second.id.value to 0, third.id.value to 1, first.id.value to 2),
            stageOrderOf(track, first, second, third)
        )
    }

    @Test
    fun `moveStage moves a stage backward and shifts the others forward`() {
        val first = createRole()
        val second = createRole()
        val third = createRole()
        val track = createTrackWithStages(first, second, third)
        val stage = gradeway.tracks.findStageByRole(track.id.value, third.id.value) ?: error("expected stage")

        gradeway.tracks.moveStage(stage.id.value, 0).getOrElse { error(it.toString()) }

        assertEquals(
            listOf(third.id.value to 0, first.id.value to 1, second.id.value to 2),
            stageOrderOf(track, first, second, third)
        )
    }

    @Test
    fun `moveStage closes gaps left by removed stages`() {
        val first = createRole()
        val second = createRole()
        val third = createRole()
        val track = createTrackWithStages(first, second, third)
        val secondStage = gradeway.tracks.findStageByRole(track.id.value, second.id.value) ?: error("expected stage")
        gradeway.tracks.removeStage(secondStage).getOrElse { error(it.toString()) }
        val thirdStage = gradeway.tracks.findStageByRole(track.id.value, third.id.value) ?: error("expected stage")

        gradeway.tracks.moveStage(thirdStage, 0).getOrElse { error(it.toString()) }

        assertEquals(listOf(third.id.value to 0, first.id.value to 1), stageOrderOf(track, first, third))
    }

    @Test
    fun `moveStage rejects an out of range or unchanged position`() {
        val first = createRole()
        val second = createRole()
        val track = createTrackWithStages(first, second)
        val stage = gradeway.tracks.findStageByRole(track.id.value, first.id.value) ?: error("expected stage")

        assertEquals(TrackService.MoveStageError.InvalidPosition(2), gradeway.tracks.moveStage(stage, 2).leftOrNull())
        assertEquals(TrackService.MoveStageError.InvalidPosition(2), gradeway.tracks.moveStage(stage, -1).leftOrNull())
        assertEquals(TrackService.MoveStageError.AlreadyAtPosition, gradeway.tracks.moveStage(stage, 0).leftOrNull())
    }

    @Test
    fun `moveStage fails for an unknown stage`() {
        val result = gradeway.tracks.moveStage(UUID.randomUUID(), 0)

        assertEquals(TrackService.MoveStageError.EntityNotFound, result.leftOrNull())
    }

    @Test
    fun `moveStage publishes an updated TrackStageChangedPayload`() {
        val track = createTrackWithStages(createRole(), createRole())
        val stage = gradeway.tracks.addStage(track, createRole()).getOrElse { error(it.toString()) }
        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.tracks.moveStage(stage, 0).getOrElse { error(it.toString()) }

        assertContains(
            received,
            TrackStageChangedPayload(track.id.value.toString(), stage.id.value.toString(), MessagingAction.UPDATED)
        )
    }

    @Test
    fun `promotePlayer puts a player without a stage onto the first stage`() {
        val first = createRole()
        val second = createRole()
        val track = createTrackWithStages(first, second)
        val player = createPlayer()

        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }

        assertEquals(first.id.value, primaryRoleIdOf(player))
        assertContains(roleIdsOf(player), first.id.value)
        assertEquals(first.id, gradeway.tracks.findPlayersCurrentStage(player.id.value, track.id.value)?.roleId)
    }

    @Test
    fun `promotePlayer moves the player to the next stage and drops the previous role`() {
        val first = createRole()
        val second = createRole()
        val track = createTrackWithStages(first, second)
        val player = createPlayer()
        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }

        gradeway.tracks.promotePlayer(player.id.value, track.id.value).getOrElse { error(it.toString()) }

        assertEquals(second.id.value, primaryRoleIdOf(player))
        val roleIds = roleIdsOf(player)
        assertContains(roleIds, second.id.value)
        assertFalse(first.id.value in roleIds)
    }

    @Test
    fun `promotePlayer skips gaps left by removed stages`() {
        val first = createRole()
        val middle = createRole()
        val last = createRole()
        val track = createTrackWithStages(first, middle, last)
        val player = createPlayer()
        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }
        val middleStage = gradeway.tracks.findStageByRole(track.id.value, middle.id.value) ?: error("expected stage")
        gradeway.tracks.removeStage(middleStage).getOrElse { error(it.toString()) }

        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }

        assertEquals(last.id.value, primaryRoleIdOf(player))
    }

    @Test
    fun `promotePlayer fails at the last stage`() {
        val track = createTrackWithStages(createRole())
        val player = createPlayer()
        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }

        val result = gradeway.tracks.promotePlayer(player, track)

        assertEquals(PlayerTrackService.PromotePlayerError.MaximumStageReached, result.leftOrNull())
    }

    @Test
    fun `promotePlayer fails on a track without stages`() {
        val result = gradeway.tracks.promotePlayer(createPlayer(), createTrack())

        assertEquals(PlayerTrackService.PromotePlayerError.NoStages, result.leftOrNull())
    }

    @Test
    fun `promotePlayer fails for an unknown player or track`() {
        val track = createTrack()
        val player = createPlayer()

        assertEquals(
            PlayerTrackService.PromotePlayerError.EntityNotFound,
            gradeway.tracks.promotePlayer(UUID.randomUUID(), track.id.value).leftOrNull()
        )
        assertEquals(
            PlayerTrackService.PromotePlayerError.TargetNotFound,
            gradeway.tracks.promotePlayer(player, UUID.randomUUID()).leftOrNull()
        )
    }

    @Test
    fun `demotePlayer moves the player to the previous stage and drops the current role`() {
        val first = createRole()
        val second = createRole()
        val track = createTrackWithStages(first, second)
        val player = createPlayer()
        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }
        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }

        gradeway.tracks.demotePlayer(player, track).getOrElse { error(it.toString()) }

        assertEquals(first.id.value, primaryRoleIdOf(player))
        val roleIds = roleIdsOf(player)
        assertContains(roleIds, first.id.value)
        assertFalse(second.id.value in roleIds)
    }

    @Test
    fun `demotePlayer fails at the first stage`() {
        val track = createTrackWithStages(createRole(), createRole())
        val player = createPlayer()
        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }

        val result = gradeway.tracks.demotePlayer(player, track)

        assertEquals(PlayerTrackService.DemotePlayerError.MinimumStageReached, result.leftOrNull())
    }

    @Test
    fun `demotePlayer fails for a player who is not on the track`() {
        val track = createTrackWithStages(createRole())

        val result = gradeway.tracks.demotePlayer(createPlayer(), track)

        assertEquals(PlayerTrackService.DemotePlayerError.MinimumStageReached, result.leftOrNull())
    }

    @Test
    fun `findPlayersNextStage and findPlayersPreviousStage return the adjacent stages`() {
        val first = createRole()
        val second = createRole()
        val third = createRole()
        val track = createTrackWithStages(first, second, third)
        val player = createPlayer()
        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }
        gradeway.tracks.promotePlayer(player, track).getOrElse { error(it.toString()) }

        assertEquals(third.id, gradeway.tracks.findPlayersNextStage(player.id.value, track.id.value)?.roleId)
        assertEquals(first.id, gradeway.tracks.findPlayersPreviousStage(player.id.value, track.id.value)?.roleId)
    }

    @Test
    fun `stage lookups return null for a player who is not on the track`() {
        val track = createTrackWithStages(createRole())
        val player = createPlayer()

        assertNull(gradeway.tracks.findPlayersCurrentStage(player, track))
        assertNull(gradeway.tracks.findPlayersNextStage(player, track))
        assertNull(gradeway.tracks.findPlayersPreviousStage(player, track))
        assertFalse(gradeway.tracks.isPlayerOnTrack(player, track))
    }

    @Test
    fun `listPlayerTracks returns every track containing the players primary role sorted by slug`() {
        val role = createRole()
        val trackB = gradeway.tracks.createTrack("b-${UUID.randomUUID().toString().take(8)}")
            .getOrElse { error(it.toString()) }
        val trackA = gradeway.tracks.createTrack("a-${UUID.randomUUID().toString().take(8)}")
            .getOrElse { error(it.toString()) }
        gradeway.tracks.addStage(trackB, role).getOrElse { error(it.toString()) }
        gradeway.tracks.addStage(trackA, role).getOrElse { error(it.toString()) }
        createTrackWithStages(createRole())
        val player = createPlayer()
        gradeway.tracks.promotePlayer(player, trackA).getOrElse { error(it.toString()) }

        val reloaded = gradeway.players.findById(player.id.value) ?: error("expected player to exist")
        val tracks = transaction(gradeway.database) {
            gradeway.tracks.listPlayerTracks(reloaded).map { it.id.value }
        }

        assertEquals(listOf(trackA.id.value, trackB.id.value), tracks)
        assertTrue(gradeway.tracks.isPlayerOnTrack(reloaded, trackB))
    }
}
