/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import dev.gradienttim.gradeway.audit.Actor
import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.configs.GradewayConfig
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.database.models.player.DatabasePlayerRoleEntity
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.managers.CommonConfigManager
import dev.gradienttim.gradeway.messaging.payloads.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.*

class CommonPlayerServiceTest {
    private val gradeway: CommonGradeway<TestPlatformConfig> = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    private fun createPlayer(): PlayerEntity =
        gradeway.players.create(
            Actor.System,
            UUID.randomUUID(),
            uniqueName("player"),
        ).getOrElse { error(it.toString()) }

    private fun createRole(): RoleEntity = gradeway.roles.create(
        Actor.System,
        uniqueName("role"),
    ).getOrElse { error(it.toString()) }

    private fun playerRoleEntity(player: PlayerEntity, role: RoleEntity): DatabasePlayerRoleEntity =
        transaction(gradeway.database) {
            player.roles.first { it.roleId == role.id } as DatabasePlayerRoleEntity
        }

    @Test
    fun `create rejects a duplicate id`() {
        val id = UUID.randomUUID()
        gradeway.players.create(Actor.System, id, uniqueName("player")).getOrElse { error(it.toString()) }

        val result = gradeway.players.create(Actor.System, id, uniqueName("player"))

        assertEquals(PlayerService.CreatePlayerError.EntityAlreadyExists, result.leftOrNull())
    }

    @Test
    fun `create rejects an invalid name`() {
        val result = gradeway.players.create(Actor.System, UUID.randomUUID(), "")

        assertEquals(PlayerService.CreatePlayerError.InvalidName, result.leftOrNull())
    }

    @Test
    fun `delete fails when the player does not exist`() {
        val result = gradeway.players.delete(Actor.System, UUID.randomUUID())

        assertEquals(PlayerService.DeletePlayerError.EntityNotFound, result.leftOrNull())
    }

    @Test
    fun `addRole rejects an until timestamp in the past`() {
        val player = createPlayer()
        val role = createRole()

        val result = gradeway.players.addRole(Actor.System, player, role, Instant.now().minusSeconds(60))

        assertEquals(PlayerService.AddRoleError.UntilInPast, result.leftOrNull())
    }

    @Test
    fun `addRole rejects a role the player already has`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }

        val result = gradeway.players.addRole(Actor.System, player, role, null)

        assertEquals(PlayerService.AddRoleError.AlreadyExists, result.leftOrNull())
    }

    @Test
    fun `addRole publishes a PlayerRoleChangedPayload`() {
        val player = createPlayer()
        val role = createRole()

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }

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
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }

        val result = gradeway.players.setRoleUntilAt(Actor.System, player, role, Instant.now().minusSeconds(60))

        assertEquals(PlayerService.SetRoleUntilAtError.UntilInPast, result.leftOrNull())
    }

    @Test
    fun `setRoleUntilAt fails when the player does not have the role`() {
        val player = createPlayer()
        val role = createRole()

        val result = gradeway.players.setRoleUntilAt(Actor.System, player, role, Instant.now().plusSeconds(60))

        assertEquals(PlayerService.SetRoleUntilAtError.RelationNotFound, result.leftOrNull())
    }

    @Test
    fun `setRolePausedAt rejects a timestamp in the past`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }

        val result = gradeway.players.setRolePausedAt(Actor.System, player, role, Instant.now().minusSeconds(60))

        assertEquals(PlayerService.SetRolePausedAtError.PauseInPast, result.leftOrNull())
    }

    @Test
    fun `pauseRole rejects a role that is already paused`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }
        gradeway.players.pauseRole(Actor.System, player, role).getOrElse { error(it.toString()) }

        val result = gradeway.players.pauseRole(Actor.System, player, role)

        assertEquals(PlayerService.PauseRoleError.AlreadyPaused, result.leftOrNull())
    }

    @Test
    fun `resumeRole rejects a role that is not paused`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }

        val result = gradeway.players.resumeRole(Actor.System, player, role)

        assertEquals(PlayerService.ResumeRoleError.NotPaused, result.leftOrNull())
    }

    @Test
    fun `resumeRole shifts untilAt forward by the paused duration`() {
        val player = createPlayer()
        val role = createRole()
        val originalUntil = Instant.now().plusSeconds(3600)
        gradeway.players.addRole(Actor.System, player, role, originalUntil).getOrElse { error(it.toString()) }

        val pausedAt = Instant.now().minus(Duration.ofMinutes(10))
        transaction(gradeway.database) {
            playerRoleEntity(player, role).pausedAt = pausedAt
        }

        val before = Instant.now()
        gradeway.players.resumeRole(Actor.System, player, role).getOrElse { error(it.toString()) }
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
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }
        gradeway.players.setPrimaryRole(Actor.System, player, role).getOrElse { error(it.toString()) }

        val result = gradeway.players.setPrimaryRole(Actor.System, player, role)

        assertEquals(PlayerService.SetPrimaryRoleError.AlreadyPrimary, result.leftOrNull())
    }

    @Test
    fun `setPrimaryRole is reflected by getPrimaryRole`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }

        gradeway.players.setPrimaryRole(Actor.System, player, role).getOrElse { error(it.toString()) }

        assertEquals(role.id.value, gradeway.players.getPrimaryRole(player.id.value)?.id?.value)
    }

    private fun configureDefaultRole(assignOnFirstJoin: Boolean, assignWhenNoPrimaryRole: Boolean) {
        val entry = gradeway.configs.gradewayEntry
        val config = entry.config.copy(
            defaultRole = GradewayConfig.DefaultRoleConfig(assignOnFirstJoin, assignWhenNoPrimaryRole)
        )
        Files.writeString(
            gradeway.directory.resolve("config.toml"),
            CommonConfigManager.TOML.encodeToString(GradewayConfig.serializer(), config)
        )
        entry.load().getOrElse { error(it.toString()) }
    }

    private fun createDefaultRole(): RoleEntity =
        createRole().also { role -> gradeway.roles.setDefault(Actor.System, role).getOrElse { error(it.toString()) } }

    @Test
    fun `applyDefaultRole assigns the default role as primary role on first join`() {
        configureDefaultRole(assignOnFirstJoin = true, assignWhenNoPrimaryRole = false)
        val role = createDefaultRole()
        val player = createPlayer()

        val assigned = gradeway.players.applyDefaultRole(Actor.System, player.id.value, firstJoin = true)
            .getOrElse { error(it.toString()) }

        assertEquals(role.id.value, assigned?.id?.value)
        assertEquals(role.id.value, gradeway.players.getPrimaryRole(player.id.value)?.id?.value)
        assertNotNull(playerRoleEntity(player, role))
    }

    @Test
    fun `applyDefaultRole does nothing on a later join when only first join is enabled`() {
        configureDefaultRole(assignOnFirstJoin = true, assignWhenNoPrimaryRole = false)
        createDefaultRole()
        val player = createPlayer()

        val assigned = gradeway.players.applyDefaultRole(Actor.System, player.id.value, firstJoin = false)
            .getOrElse { error(it.toString()) }

        assertNull(assigned)
        assertNull(gradeway.players.getPrimaryRole(player.id.value))
    }

    @Test
    fun `applyDefaultRole assigns the default role to existing players without a primary role`() {
        configureDefaultRole(assignOnFirstJoin = false, assignWhenNoPrimaryRole = true)
        val role = createDefaultRole()
        val player = createPlayer()
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }

        gradeway.players.applyDefaultRole(
            Actor.System,
            player.id.value,
            firstJoin = false,
        ).getOrElse { error(it.toString()) }

        assertEquals(role.id.value, gradeway.players.getPrimaryRole(player.id.value)?.id?.value)
    }

    @Test
    fun `applyDefaultRole keeps an existing primary role`() {
        configureDefaultRole(assignOnFirstJoin = false, assignWhenNoPrimaryRole = true)
        createDefaultRole()
        val primaryRole = createRole()
        val player = createPlayer()
        gradeway.players.addRole(Actor.System, player, primaryRole, null).getOrElse { error(it.toString()) }
        gradeway.players.setPrimaryRole(Actor.System, player, primaryRole).getOrElse { error(it.toString()) }

        val assigned = gradeway.players.applyDefaultRole(Actor.System, player.id.value, firstJoin = false)
            .getOrElse { error(it.toString()) }

        assertNull(assigned)
        assertEquals(primaryRole.id.value, gradeway.players.getPrimaryRole(player.id.value)?.id?.value)
    }

    @Test
    fun `applyDefaultRole does nothing without a default role or when disabled`() {
        configureDefaultRole(assignOnFirstJoin = true, assignWhenNoPrimaryRole = true)
        val player = createPlayer()
        assertNull(
            gradeway.players.applyDefaultRole(
                Actor.System,
                player.id.value,
                firstJoin = true,
            ).getOrElse { error(it.toString()) }
        )

        configureDefaultRole(assignOnFirstJoin = false, assignWhenNoPrimaryRole = false)
        createDefaultRole()
        assertNull(
            gradeway.players.applyDefaultRole(
                Actor.System,
                player.id.value,
                firstJoin = true,
            ).getOrElse { error(it.toString()) }
        )
        assertNull(gradeway.players.getPrimaryRole(player.id.value))
    }

    @Test
    fun `applyDefaultRole fails for an unknown player`() {
        val result = gradeway.players.applyDefaultRole(Actor.System, UUID.randomUUID(), firstJoin = true)

        assertEquals(PlayerService.ApplyDefaultRoleError.EntityNotFound, result.leftOrNull())
    }

    @Test
    fun `clearPrimaryRole removes the primary role but keeps the role assigned`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }
        gradeway.players.setPrimaryRole(Actor.System, player, role).getOrElse { error(it.toString()) }

        gradeway.players.clearPrimaryRole(Actor.System, player.id.value).getOrElse { error(it.toString()) }

        assertNull(gradeway.players.getPrimaryRole(player.id.value))
        assertNotNull(playerRoleEntity(player, role))
    }

    @Test
    fun `clearPrimaryRole fails for a player without a primary role`() {
        val player = createPlayer()

        val result = gradeway.players.clearPrimaryRole(Actor.System, player)

        assertEquals(PlayerService.ClearPrimaryRoleError.NoPrimaryRole, result.leftOrNull())
    }

    @Test
    fun `clearPrimaryRole fails for an unknown player`() {
        val result = gradeway.players.clearPrimaryRole(Actor.System, UUID.randomUUID())

        assertEquals(PlayerService.ClearPrimaryRoleError.EntityNotFound, result.leftOrNull())
    }

    @Test
    fun `removeExpiredRoles only removes expired non-paused roles`() {
        val player = createPlayer()
        val expiredRole = createRole()
        val activeRole = createRole()
        gradeway.players.addRole(
            Actor.System,
            player,
            expiredRole,
            Instant.now().plusSeconds(60),
        ).getOrElse { error(it.toString()) }
        gradeway.players.addRole(Actor.System, player, activeRole, null).getOrElse { error(it.toString()) }

        transaction(gradeway.database) {
            playerRoleEntity(player, expiredRole).untilAt = Instant.now().minusSeconds(1)
        }

        val removed = gradeway.players.removeExpiredRoles(Actor.System, player).getOrElse { error(it.toString()) }

        assertEquals(listOf(expiredRole.id.value), removed.map { it.id.value })
        assertTrue(transaction(gradeway.database) { player.roles.any { it.roleId == activeRole.id } })
        assertFalse(transaction(gradeway.database) { player.roles.any { it.roleId == expiredRole.id } })
    }

    @Test
    fun `removeExpiredRoles does not remove a paused role even if expired`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.players.addRole(
            Actor.System,
            player,
            role,
            Instant.now().plusSeconds(60),
        ).getOrElse { error(it.toString()) }

        transaction(gradeway.database) {
            val entity = playerRoleEntity(player, role)
            entity.untilAt = Instant.now().minusSeconds(1)
            entity.pausedAt = Instant.now().minusSeconds(30)
        }

        val removed = gradeway.players.removeExpiredRoles(Actor.System, player).getOrElse { error(it.toString()) }

        assertTrue(removed.isEmpty())
    }

    @Test
    fun `removeExpiredRoles bulk overload removes expired roles across multiple players`() {
        val playerOne = createPlayer()
        val playerTwo = createPlayer()
        val role = createRole()
        gradeway.players.addRole(
            Actor.System,
            playerOne,
            role,
            Instant.now().plusSeconds(60),
        ).getOrElse { error(it.toString()) }
        gradeway.players.addRole(
            Actor.System,
            playerTwo,
            role,
            Instant.now().plusSeconds(60),
        ).getOrElse { error(it.toString()) }

        transaction(gradeway.database) {
            playerRoleEntity(playerOne, role).untilAt = Instant.now().minusSeconds(1)
            playerRoleEntity(playerTwo, role).untilAt = Instant.now().minusSeconds(1)
        }

        val removed =
            gradeway.players.removeExpiredRoles(Actor.System, listOf(playerOne.id.value, playerTwo.id.value))
                .getOrElse { error(it.toString()) }

        assertEquals(setOf(playerOne.id.value, playerTwo.id.value), removed.map { it.first }.toSet())
    }

    @Test
    fun `effective weight cache is invalidated by a RoleChangedPayload`() {
        val player = createPlayer()
        val role = createRole()
        gradeway.roles.setWeight(Actor.System, role, 15).getOrElse { error(it.toString()) }
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }
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
        gradeway.roles.setWeight(Actor.System, role, 15).getOrElse { error(it.toString()) }
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }
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
        gradeway.roles.setWeight(Actor.System, role, 15).getOrElse { error(it.toString()) }
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }
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
        gradeway.roles.setWeight(Actor.System, role, 15).getOrElse { error(it.toString()) }
        gradeway.players.addRole(Actor.System, player, role, null).getOrElse { error(it.toString()) }
        assertEquals(15, gradeway.players.getEffectiveWeight(player.id.value))

        transaction(gradeway.database) {
            (role as dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity).weight = 5
        }
        gradeway.messaging.publish(CacheFlushPayload)

        assertEquals(5, gradeway.players.getEffectiveWeight(player.id.value))
    }
}
