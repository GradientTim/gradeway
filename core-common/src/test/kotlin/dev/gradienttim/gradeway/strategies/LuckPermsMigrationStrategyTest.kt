/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.strategies

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import net.kyori.adventure.key.Key
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LuckPermsMigrationStrategyTest {
    private val gradeway: CommonGradeway = createTestGradeway()
    private val strategy = LuckPermsMigrationStrategy(gradeway)

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun gzippedExportFile(json: String): File {
        val file = Files.createTempFile("luckperms-export", ".tar.gz").toFile()
        GZIPOutputStream(file.outputStream()).use { it.write(json.toByteArray()) }
        return file
    }

    private fun exportJson(groups: String, users: String, tracks: String = "{}") = """
        {
            "metadata": {"generatedBy": "test", "generatedAt": "2024-01-01T00:00:00Z"},
            "groups": $groups,
            "tracks": $tracks,
            "users": $users
        }
    """.trimIndent()

    @Test
    fun `migrate imports permission and wildcard permission nodes onto the role`() {
        val json = exportJson(
            groups = """
                {
                    "admin": {"nodes": [
                        {"type": "permission", "key": "gradeway.test.exact", "value": true},
                        {"type": "permission", "key": "gradeway.test.wild.*", "value": true}
                    ]}
                }
            """.trimIndent(),
            users = "{}"
        )

        strategy.migrate(gzippedExportFile(json)).getOrElse { error(it.toString()) }

        val role = gradeway.roles.findByName("admin") ?: error("expected role 'admin' to be imported")
        assertTrue(gradeway.permissions.hasEffectiveRolePermission(role, "gradeway.test.exact"))
        assertTrue(gradeway.permissions.hasEffectiveRolePermission(role, "gradeway.test.wild.anything"))
        assertFalse(gradeway.permissions.hasEffectiveRolePermission(role, "gradeway.test.other"))
    }

    @Test
    fun `migrate links a user to their inherited group as a role`() {
        val userId = UUID.randomUUID()
        val json = exportJson(
            groups = """{"admin": {"nodes": []}}""",
            users = """
                {
                    "$userId": {"username": "Steve", "nodes": [
                        {"type": "inheritance", "key": "group.admin", "value": true}
                    ]}
                }
            """.trimIndent()
        )

        strategy.migrate(gzippedExportFile(json)).getOrElse { error(it.toString()) }

        val player = gradeway.players.findById(userId) ?: error("expected player to be imported")
        val role = gradeway.roles.findByName("admin") ?: error("expected role 'admin' to be imported")
        assertEquals("Steve", player.name)
        val hasRole = org.jetbrains.exposed.v1.jdbc.transactions.transaction(gradeway.database) {
            player.roles.any { it.roleId == role.id }
        }
        assertTrue(hasRole)
    }

    @Test
    fun `migrate applies only the highest-priority prefix and weight nodes`() {
        val json = exportJson(
            groups = """
                {
                    "admin": {"nodes": [
                        {"type": "prefix", "key": "prefix.5.Low", "value": true},
                        {"type": "prefix", "key": "prefix.10.High", "value": true},
                        {"type": "weight", "key": "weight.3", "value": true},
                        {"type": "weight", "key": "weight.9", "value": true}
                    ]}
                }
            """.trimIndent(),
            users = "{}"
        )

        strategy.migrate(gzippedExportFile(json)).getOrElse { error(it.toString()) }

        val role = gradeway.roles.findByName("admin") ?: error("expected role 'admin' to be imported")
        assertEquals("High", gradeway.roles.getAttribute(role, Key.key("gradeway", "prefix"))?.value)
        assertEquals(9, role.weight)
    }

    @Test
    fun `migrate fails when a user inherits from an unknown group`() {
        val userId = UUID.randomUUID()
        val json = exportJson(
            groups = "{}",
            users = """
                {
                    "$userId": {"username": "Steve", "nodes": [
                        {"type": "inheritance", "key": "group.unknown-group", "value": true}
                    ]}
                }
            """.trimIndent()
        )

        val result = strategy.migrate(gzippedExportFile(json))

        assertTrue(result.isLeft())
    }

    @Test
    fun `migrate wipes existing data before importing`() {
        val preExistingRole = gradeway.roles.create("pre-existing-${UUID.randomUUID().toString().take(8)}")
            .getOrElse { error(it.toString()) }

        val json = exportJson(groups = """{"admin": {"nodes": []}}""", users = "{}")
        strategy.migrate(gzippedExportFile(json)).getOrElse { error(it.toString()) }

        assertFalse(gradeway.roles.existsById(preExistingRole.id.value))
    }

    @Test
    fun `migrate publishes a CacheFlushPayload on success`() {
        val json = exportJson(groups = """{"admin": {"nodes": []}}""", users = "{}")

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        strategy.migrate(gzippedExportFile(json)).getOrElse { error(it.toString()) }

        assertContains(received, CacheFlushPayload)
    }
}
