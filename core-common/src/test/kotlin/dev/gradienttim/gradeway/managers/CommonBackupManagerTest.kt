/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes
import kotlin.test.*

class CommonBackupManagerTest {
    private val gradeway: CommonGradeway<TestPlatformConfig> = createTestGradeway()

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
        val track = gradeway.tracks.createTrack(uniqueName("track")).getOrElse { error(it.toString()) }
        gradeway.tracks.addStage(track, childRole).getOrElse { error(it.toString()) }
        val stage = gradeway.tracks.addStage(track, parentRole).getOrElse { error(it.toString()) }

        val file = gradeway.backups.export().getOrElse { error(it.toString()) }

        // Added only after the export, so it must be gone once wipe=true import restores the archive.
        val extraneousRole = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }

        gradeway.backups.import(file.name, wipe = true).getOrElse { error(it.toString()) }

        assertTrue(gradeway.roles.existsById(parentRole.id.value))
        assertTrue(gradeway.roles.existsById(childRole.id.value))
//        assertTrue(gradeway.players.existsById(player.id.value))
        assertTrue(gradeway.groups.findById(group.id.value) != null)
        assertTrue(gradeway.permissions.hasEffectiveRolePermission(childRole.id.value, permissionValue))
        assertFalse(gradeway.roles.existsById(extraneousRole.id.value))
        assertNotNull(gradeway.tracks.findTrackById(track.id.value))
        val restoredStage = gradeway.tracks.findStageById(stage.id.value) ?: error("expected stage to be restored")
        assertEquals(track.id.value, restoredStage.trackId.value)
        assertEquals(parentRole.id.value, restoredStage.roleId.value)
        assertEquals(1, restoredStage.position)
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
        val backupsDirectory = gradeway.directory.resolve("backups").createDirectories()
        val corruptFile = backupsDirectory.resolve("corrupt.tar.gz")
        corruptFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        val result = gradeway.backups.import("corrupt.tar.gz")

        assertTrue(result.leftOrNull() is BackupManager.ImportError.CorruptArchive)
    }

    @Test
    fun `export writes a manifest followed by one json lines entry per table`() {
        repeat(3) { gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) } }

        val file = gradeway.backups.export().getOrElse { error(it.toString()) }
        val entries = readArchive(file)

        assertEquals("manifest.json", entries.keys.first())
        assertTrue(entries.keys.drop(1).all { it.endsWith(".jsonl") })

        val manifest = Json.parseToJsonElement(entries.getValue("manifest.json")).jsonObject
        assertEquals(1, manifest.getValue("formatVersion").jsonPrimitive.int)
        val tables = manifest.getValue("tables").jsonObject
        assertEquals(entries.keys.drop(1).map { it.removeSuffix(".jsonl") }, tables.keys.toList())

        tables.forEach { (table, rows) ->
            val lines = entries.getValue("$table.jsonl").lines().filter { it.isNotBlank() }
            assertEquals(rows.jsonPrimitive.long, lines.size.toLong())
        }
        assertTrue(tables.getValue("roles").jsonPrimitive.long >= 3)
    }

    @Test
    fun `export and import stream tables larger than one batch`() {
        val roles = List(2500) { gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) } }
        val file = gradeway.backups.export().getOrElse { error(it.toString()) }

        val manifest = Json.parseToJsonElement(readArchive(file).getValue("manifest.json")).jsonObject
        val exportedRoles = manifest.getValue("tables").jsonObject.getValue("roles").jsonPrimitive.long
        assertTrue(exportedRoles >= roles.size)

        gradeway.backups.import(file.name, wipe = true).getOrElse { error(it.toString()) }

        assertTrue(roles.all { gradeway.roles.existsById(it.id.value) })
    }

    @Test
    fun `export leaves no temporary files behind`() {
        val file = gradeway.backups.export().getOrElse { error(it.toString()) }

        val remaining = file.parent.toFile().list()!!.filter { it.endsWith(".tmp") }
        assertEquals(emptyList(), remaining)
    }

    @Test
    fun `import restores a legacy archive without a manifest`() {
        val role = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }
        val file = gradeway.backups.export().getOrElse { error(it.toString()) }
        val legacy = rewriteArchive(file, "legacy.tar.gz") { entries ->
            entries.filterKeys { it != "manifest.json" }
                .mapKeys { (name, _) -> name.replace(".jsonl", ".json") }
                .mapValues { (_, content) ->
                    content.lines().filter { it.isNotBlank() }.joinToString(",", "[", "]")
                }
        }

        gradeway.roles.delete(role.id.value).getOrElse { error(it.toString()) }
        gradeway.backups.import(legacy.name, wipe = true).getOrElse { error(it.toString()) }

        assertTrue(gradeway.roles.existsById(role.id.value))
    }

    @Test
    fun `import rejects a newer format version without touching existing data`() {
        val role = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }
        val file = gradeway.backups.export().getOrElse { error(it.toString()) }
        val future = rewriteArchive(file, "future.tar.gz") { entries ->
            entries + ("manifest.json" to rewriteManifest(entries) { put("formatVersion", 99) })
        }

        val result = gradeway.backups.import(future.name, wipe = true)

        assertEquals(BackupManager.ImportError.UnsupportedFormatVersion(99, 1), result.leftOrNull())
        assertTrue(gradeway.roles.existsById(role.id.value))
    }

    @Test
    fun `import rolls back when an entry has fewer rows than the manifest declares`() {
        val role = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }
        val file = gradeway.backups.export().getOrElse { error(it.toString()) }
        val truncated = rewriteArchive(file, "truncated.tar.gz") { entries ->
            entries + ("roles.jsonl" to entries.getValue("roles.jsonl").lines().drop(1).joinToString("\n"))
        }

        val result = gradeway.backups.import(truncated.name, wipe = true)

        assertTrue(result.leftOrNull() is BackupManager.ImportError.CorruptArchive)
        assertTrue(gradeway.roles.existsById(role.id.value))
    }

    @Test
    fun `import rolls back when an entry listed in the manifest is missing`() {
        val role = gradeway.roles.create(uniqueName("role")).getOrElse { error(it.toString()) }
        val file = gradeway.backups.export().getOrElse { error(it.toString()) }
        val incomplete = rewriteArchive(file, "incomplete.tar.gz") { entries ->
            entries.filterKeys { it != "tracks.jsonl" && it != "track_stages.jsonl" }
        }

        val result = gradeway.backups.import(incomplete.name, wipe = true)

        assertTrue(result.leftOrNull() is BackupManager.ImportError.CorruptArchive)
        assertTrue(gradeway.roles.existsById(role.id.value))
    }

    private fun readArchive(file: Path): LinkedHashMap<String, String> {
        val entries = LinkedHashMap<String, String>()
        TarArchiveInputStream(GzipCompressorInputStream(file.inputStream())).use { stream ->
            generateSequence { stream.nextEntry }.forEach { entry ->
                entries[entry.name] = stream.readBytes().decodeToString()
            }
        }
        return entries
    }

    private fun rewriteArchive(
        source: Path,
        targetName: String,
        transform: (Map<String, String>) -> Map<String, String>,
    ): Path {
        val target = source.resolveSibling(targetName)
        TarArchiveOutputStream(GzipCompressorOutputStream(target.outputStream())).use { stream ->
            transform(readArchive(source)).forEach { (name, content) ->
                val bytes = content.encodeToByteArray()
                stream.putArchiveEntry(TarArchiveEntry(name).apply { size = bytes.size.toLong() })
                stream.write(bytes)
                stream.closeArchiveEntry()
            }
        }
        return target
    }

    private fun rewriteManifest(
        entries: Map<String, String>,
        change: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
    ): String {
        val manifest = Json.parseToJsonElement(entries.getValue("manifest.json")).jsonObject
        return buildJsonObject {
            manifest.forEach { (key, value) -> put(key, value) }
            change()
        }.toString()
    }
}
