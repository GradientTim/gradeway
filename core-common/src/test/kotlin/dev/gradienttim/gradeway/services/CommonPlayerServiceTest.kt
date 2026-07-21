/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerRoleEntity
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.messaging.payloads.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.*

class CommonPlayerServiceTest {
    private val gradeway: CommonGradeway = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    private fun createPlayer(): PlayerEntity =
        gradeway.players.create(UUID.randomUUID(), uniqueName("player")).getOrElse { error(it.toString()) }

    private fun createRole(): RoleEntity = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }

    private fun playerRoleEntity(player: PlayerEntity, role: RoleEntity): DatabasePlayerRoleEntity =
        transaction(gradeway.database) {
            player.roles.first { it.roleId == role.id } as DatabasePlayerRoleEntity
        }

    @Test
    fun `create rejects a duplicate id`() {
        val id = UUID.randomUUID()
        gradeway.players.create(id, uniqueName("player")).getOrElse { error(it.toString()) }

        val result = gradeway.players.create(id, uniqueName("player"))

        assertEquals(PlayerService.CreatePlayerError.EntityAlreadyExists, result.leftOrNull())
    }

    @Test
    fun `create rejects an invalid name`() {
        val result = gradeway.players.create(UUID.randomUUID(), "")

        assertEquals(PlayerService.CreatePlayerError.InvalidName, result.leftOrNull())
    }

    @Test
    fun `delete fails when the player does not exist`() {
        val result = gradeway.players.delete(UUID.randomUUID())

        assertEquals(PlayerService.DeletePlayerError.EntityNotFound, result.leftOrNull())
    }

    @Test
    fun `addRole rejects an until timestamp in the past`() {
        val player = createPlayer()
        val role = createRole()

        val result = gradeway.players.addRole(player, role, Instant.now().minusSeconds(60))

        assertEquals(PlayerService.AddRoleError.UntilInPast, result.leftOrNull())
    }

    @Test
    fun `addRole rejects a role the player already has`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }

        val result = gradeway.players.addRole(player, role, null)

        assertEquals(PlayerService.AddRoleError.AlreadyExists, result.leftOrNull())
    }

    @Test
    fun `addRole publishes a PlayerRoleChangedPayload`() {
        val player = createPlayer()
        val role = createRole()

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }

        assertEquals(
            listOf<MessagingPayload>(
                PlayerRoleChangedPayload(player.id.value.toString(), role.id.value.toString(), MessagingAction.CREATED)
            ),
            received
        )
    }

    @Test
    fun `setRoleUntilAt rejects a timestamp in the past`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }

        val result = gradeway.players.setRoleUntilAt(player, role, Instant.now().minusSeconds(60))

        assertEquals(PlayerService.SetRoleUntilAtError.UntilInPast, result.leftOrNull())
    }

    @Test
    fun `setRoleUntilAt fails when the player does not have the role`() {
        val player = createPlayer()
        val role = createRole()

        val result = gradeway.players.setRoleUntilAt(player, role, Instant.now().plusSeconds(60))

        assertEquals(PlayerService.SetRoleUntilAtError.RelationNotFound, result.leftOrNull())
    }

    @Test
    fun `setRolePausedAt rejects a timestamp in the past`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }

        val result = gradeway.players.setRolePausedAt(player, role, Instant.now().minusSeconds(60))

        assertEquals(PlayerService.SetRolePausedAtError.PauseInPast, result.leftOrNull())
    }

    @Test
    fun `pauseRole rejects a role that is already paused`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }
        gradeway.players.pauseRole(player, role).getOrElse { error(it.toString()) }

        val result = gradeway.players.pauseRole(player, role)

        assertEquals(PlayerService.PauseRoleError.AlreadyPaused, result.leftOrNull())
    }

    @Test
    fun `resumeRole rejects a role that is not paused`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }

        val result = gradeway.players.resumeRole(player, role)

        assertEquals(PlayerService.ResumeRoleError.NotPaused, result.leftOrNull())
    }

    @Test
    fun `resumeRole shifts untilAt forward by the paused duration`() {
        val player = createPlayer()
        val role = createRole()
        val originalUntil = Instant.now().plusSeconds(3600)
        gradeway.players.addRole(player, role, originalUntil).getOrElse { error(it.toString()) }

        val pausedAt = Instant.now().minus(Duration.ofMinutes(10))
        transaction(gradeway.database) {
            playerRoleEntity(player, role).pausedAt = pausedAt
        }

        val before = Instant.now()
        gradeway.players.resumeRole(player, role).getOrElse { error(it.toString()) }
        val after = Instant.now()

        val updatedUntil = transaction(gradeway.database) { playerRoleEntity(player, role).untilAt!! }
        val expectedMin = originalUntil.plus(Duration.between(pausedAt, before))
        val expectedMax = originalUntil.plus(Duration.between(pausedAt, after))

        assertTrue(
            !updatedUntil.isBefore(expectedMin) && !updatedUntil.isAfter(expectedMax),
            "expected untilAt ($updatedUntil) to be shifted forward by roughly the paused duration " +
                "(expected between $expectedMin and $expectedMax)"
        )
        assertNull(transaction(gradeway.database) { playerRoleEntity(player, role).pausedAt })
    }

    @Test
    fun `setPrimaryRole rejects setting the same primary role again`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }
        gradeway.players.setPrimaryRole(player, role).getOrElse { error(it.toString()) }

        val result = gradeway.players.setPrimaryRole(player, role)

        assertEquals(PlayerService.SetPrimaryRoleError.AlreadyPrimary, result.leftOrNull())
    }

    @Test
    fun `setPrimaryRole is reflected by getPrimaryRole`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }

        gradeway.players.setPrimaryRole(player, role).getOrElse { error(it.toString()) }

        assertEquals(role.id.value, gradeway.players.getPrimaryRole(player.id.value)?.id?.value)
    }

    @Test
    fun `removeExpiredRoles only removes expired non-paused roles`() {
        val player = createPlayer()
        val expiredRole = createRole()
        val activeRole = createRole()
        gradeway.players.addRole(player, expiredRole, Instant.now().plusSeconds(60)).getOrElse { error(it.toString()) }
        gradeway.players.addRole(player, activeRole, null).getOrElse { error(it.toString()) }

        transaction(gradeway.database) {
            playerRoleEntity(player, expiredRole).untilAt = Instant.now().minusSeconds(1)
        }

        val removed = gradeway.players.removeExpiredRoles(player).getOrElse { error(it.toString()) }

        assertEquals(listOf(expiredRole.id.value), removed.map { it.id.value })
        assertTrue(transaction(gradeway.database) { player.roles.any { it.roleId == activeRole.id } })
        assertFalse(transaction(gradeway.database) { player.roles.any { it.roleId == expiredRole.id } })
    }

    @Test
    fun `removeExpiredRoles does not remove a paused role even if expired`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(player, role, Instant.now().plusSeconds(60)).getOrElse { error(it.toString()) }

        transaction(gradeway.database) {
            val entity = playerRoleEntity(player, role)
            entity.untilAt = Instant.now().minusSeconds(1)
            entity.pausedAt = Instant.now().minusSeconds(30)
        }

        val removed = gradeway.players.removeExpiredRoles(player).getOrElse { error(it.toString()) }

        assertTrue(removed.isEmpty())
    }

    @Test
    fun `removeExpiredRoles bulk overload removes expired roles across multiple players`() {
        val playerOne = createPlayer()
        val playerTwo = createPlayer()
        val role = createRole()
        gradeway.players.addRole(playerOne, role, Instant.now().plusSeconds(60)).getOrElse { error(it.toString()) }
        gradeway.players.addRole(playerTwo, role, Instant.now().plusSeconds(60)).getOrElse { error(it.toString()) }

        transaction(gradeway.database) {
            playerRoleEntity(playerOne, role).untilAt = Instant.now().minusSeconds(1)
            playerRoleEntity(playerTwo, role).untilAt = Instant.now().minusSeconds(1)
        }

        val removed =
            gradeway.players.removeExpiredRoles(listOf(playerOne.id.value, playerTwo.id.value))
                .getOrElse { error(it.toString()) }

        assertEquals(setOf(playerOne.id.value, playerTwo.id.value), removed.map { it.first }.toSet())
    }

    @Test
    fun `effective weight cache is invalidated by a RoleChangedPayload`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.roles.setWeight(role, 15).getOrElse { error(it.toString()) }
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }
        assertEquals(15, gradeway.players.getEffectiveWeight(player.id.value))

        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 60
        }
        gradeway.messaging.publish(RoleChangedPayload(role.id.value.toString(), MessagingAction.UPDATED))

        assertEquals(60, gradeway.players.getEffectiveWeight(player.id.value))
    }

    @Test
    fun `effective weight cache is fully invalidated by a GroupRoleChangedPayload`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.roles.setWeight(role, 15).getOrElse { error(it.toString()) }
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }
        assertEquals(15, gradeway.players.getEffectiveWeight(player.id.value))

        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 45
        }
        gradeway.messaging.publish(
            GroupRoleChangedPayload(UUID.randomUUID().toString(), UUID.randomUUID().toString(), MessagingAction.CREATED)
        )

        assertEquals(45, gradeway.players.getEffectiveWeight(player.id.value))
    }

    @Test
    fun `effective weight cache is fully invalidated by a GroupChangedPayload`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.roles.setWeight(role, 15).getOrElse { error(it.toString()) }
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }
        assertEquals(15, gradeway.players.getEffectiveWeight(player.id.value))

        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 20
        }
        gradeway.messaging.publish(GroupChangedPayload(UUID.randomUUID().toString(), MessagingAction.UPDATED))

        assertEquals(20, gradeway.players.getEffectiveWeight(player.id.value))
    }

    @Test
    fun `effective weight cache is fully invalidated by a CacheFlushPayload`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.roles.setWeight(role, 15).getOrElse { error(it.toString()) }
        gradeway.players.addRole(player, role, null).getOrElse { error(it.toString()) }
        assertEquals(15, gradeway.players.getEffectiveWeight(player.id.value))

        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 5
        }
        gradeway.messaging.publish(CacheFlushPayload)

        assertEquals(5, gradeway.players.getEffectiveWeight(player.id.value))
    }
}
