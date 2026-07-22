/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.*

class CommonSuggestionIndexTest {
    private val gradeway: CommonGradeway = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    @Test
    fun `creating a role adds it to the suggestion index`() {
        val roleName = "role-${UUID.randomUUID().toString().take(8)}"
        val role = gradeway.roles.create(roleName).getOrElse { error(it.toString()) }

        assertEquals(roleName, gradeway.caches.suggestions.roles[role.id.value])
    }

    @Test
    fun `deleting a role removes it from the suggestion index`() {
        val roleName = "role-${UUID.randomUUID().toString().take(8)}"
        val role = gradeway.roles.create(roleName).getOrElse { error(it.toString()) }
        assertTrue(gradeway.caches.suggestions.roles.containsKey(role.id.value))

        gradeway.roles.delete(role.id.value).getOrElse { error(it.toString()) }

        assertFalse(gradeway.caches.suggestions.roles.containsKey(role.id.value))
    }

    @Test
    fun `creating a player adds it to the suggestion index`() {
        val playerId = UUID.randomUUID()
        val playerName = "player-${playerId.toString().take(8)}"
        gradeway.players.create(playerId, playerName).getOrElse { error(it.toString()) }

        assertEquals(playerName, gradeway.caches.suggestions.players[playerId])
    }

    @Test
    fun `a cache flush payload rebuilds the index from the database`() {
        val roleName = "role-${UUID.randomUUID().toString().take(8)}"
        val role = gradeway.roles.create(roleName).getOrElse { error(it.toString()) }

        // Bypasses CommonRoleService entirely, so no RoleChangedPayload is published for this
        // rename - if the index only relied on per-entity payloads, it would never see it.
        val renamedTo = "renamed-${UUID.randomUUID().toString().take(8)}"
        transaction(gradeway.database) {
            DatabaseRoleEntity.findById(role.id.value)!!.name = renamedTo
        }

        gradeway.messaging.publish(CacheFlushPayload)

        assertEquals(renamedTo, gradeway.caches.suggestions.roles[role.id.value])
    }
}
