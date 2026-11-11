/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.TestSqliteDatabaseDriver
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayerPermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.extensions.eqUuidStr
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqliteIdOrNameLookupTest {
    private val gradeway: CommonGradeway<TestPlatformConfig> = createTestGradeway(TestSqliteDatabaseDriver())

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    @Test
    fun `player findByIdOrName matches by id on sqlite`() {
        val id = UUID.randomUUID()
        gradeway.players.create(Actor.System, id, "player").getOrElse { error(it.toString()) }

        assertEquals(id, gradeway.players.findByIdOrName(id.toString())?.id?.value)
        assertEquals(id, gradeway.players.findByIdOrName("player")?.id?.value)
        assertNull(gradeway.players.findByIdOrName(UUID.randomUUID().toString()))
    }

    @Test
    fun `player permissions can be set by id on sqlite`() {
        val id = UUID.randomUUID()
        gradeway.players.create(Actor.System, id, "player").getOrElse { error(it.toString()) }

        val result = gradeway.players.setPermission(Actor.System, id.toString(), "bla.test", true)

        assertNull(result.leftOrNull())
    }

    @Test
    fun `role group track permission and template lookups match by id on sqlite`() {
        val role = gradeway.roles.create(Actor.System, "role").getOrElse { error(it.toString()) }
        val group = gradeway.groups.create(Actor.System, "group").getOrElse { error(it.toString()) }
        val track = gradeway.tracks.createTrack(Actor.System, "track").getOrElse { error(it.toString()) }
        val permission = gradeway.permissions
            .createPermission(Actor.System, "bla.test", PermissionEntity.Type.EQUALS)
            .getOrElse { error(it.toString()) }
        val template = gradeway.permissions.createTemplate(Actor.System, "template").getOrElse { error(it.toString()) }

        assertEquals(role.id, gradeway.roles.findByIdOrName(role.id.value.toString())?.id)
        assertEquals(group.id, gradeway.groups.findByIdOrName(group.id.value.toString())?.id)
        assertEquals(track.id, gradeway.tracks.findTrackByIdOrSlug(track.id.value.toString())?.id)
        assertEquals(permission.id, gradeway.permissions.findPermissionByIdOrValue(permission.id.value.toString())?.id)
        assertEquals(template.id, gradeway.permissions.findTemplateByIdOrName(template.id.value.toString())?.id)
    }

    @Test
    fun `scoped permission list query matches the player by id or name on sqlite`() {
        val id = UUID.randomUUID()
        gradeway.players.create(Actor.System, id, "player").getOrElse { error(it.toString()) }
        gradeway.players.create(Actor.System, UUID.randomUUID(), "player2").getOrElse { error(it.toString()) }
        gradeway.players.setPermission(Actor.System, id, "bla.test", true).getOrElse { error(it.toString()) }

        fun listFor(scope: String) = transaction(gradeway.database) {
            PlayerPermissionsTable
                .innerJoin(PlayersTable, { playerId }, { PlayersTable.id })
                .innerJoin(PermissionsTable, { PlayerPermissionsTable.permissionId }, { PermissionsTable.id })
                .select(PermissionsTable.value)
                .where { (PlayersTable.id eqUuidStr scope) or (PlayersTable.name.lowerCase() eq scope.lowercase()) }
                .map { it[PermissionsTable.value] }
        }

        assertEquals(listOf("bla.test"), listFor(id.toString()))
        assertEquals(listOf("bla.test"), listFor("PLAYER"))
        assertEquals(emptyList(), listFor("player2"))
    }
}
