/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonBackupManagerTest {
    private val gradeway: CommonGradeway = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniqueName(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}"

    @Test
    fun `export then import restores the exported graph`() {
        val parentRole = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }
        val childRole = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }
        gradeway.roles.addParent(childRole, parentRole).getOrElse { error(it.toString()) }
        val permissionValue = "gradeway.test.${UUID.randomUUID()}"
        gradeway.roles.setPermission(parentRole, permissionValue, true).getOrElse { error(it.toString()) }
        val player = gradeway.players.create(UUID.randomUUID(), uniqueName("player")).getOrElse { error(it.toString()) }
        gradeway.players.addRole(player, childRole, null).getOrElse { error(it.toString()) }
        val group = gradeway.groups.create(uniqueName("group")).getOrElse { error(it.toString()) }
        gradeway.groups.addRoleToGroup(group, parentRole).getOrElse { error(it.toString()) }

        val file = gradeway.backups.export().getOrElse { error(it.toString()) }

        // Added only after the export, so it must be gone once wipe=true import restores the archive.
        val extraneousRole = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }

        gradeway.backups.import(file.name, wipe = true).getOrElse { error(it.toString()) }

        assertTrue(gradeway.roles.existsById(parentRole.id.value))
        assertTrue(gradeway.roles.existsById(childRole.id.value))
        assertTrue(gradeway.players.existsById(player.id.value))
        assertTrue(gradeway.groups.findById(group.id.value) != null)
        assertTrue(gradeway.permissions.hasEffectiveRolePermission(childRole.id.value, permissionValue))
        assertFalse(gradeway.roles.existsById(extraneousRole.id.value))
    }

    @Test
    fun `import publishes a CacheFlushPayload`() {
        gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }
        val file = gradeway.backups.export().getOrElse { error(it.toString()) }

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.backups.import(file.name, wipe = true).getOrElse { error(it.toString()) }

        assertContains(received, CacheFlushPayload)
    }

    @Test
    fun `import fails when the file does not exist`() {
        val result = gradeway.backups.import("does-not-exist.tar.gz")

        assertEquals(BackupManager.ImportError.FileNotFound, result.leftOrNull())
    }

    @Test
    fun `import fails on a corrupt archive`() {
        // Referencing gradeway.backups first forces CommonBackupManager's lazy Koin instantiation,
        // which is what actually creates the "backups" directory.
        val backupsDirectory = java.io.File(gradeway.directory, "backups").apply { mkdirs() }
        val corruptFile = java.io.File(backupsDirectory, "corrupt.tar.gz")
        corruptFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        val result = gradeway.backups.import("corrupt.tar.gz")

        assertTrue(result.leftOrNull() is BackupManager.ImportError.CorruptArchive)
    }
}
