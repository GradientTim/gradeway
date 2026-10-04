/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupEntity
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupPermissionEntity
import dev.gradienttim.gradeway.database.models.group.DatabaseGroupPermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionEntity
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionTemplateEntity
import dev.gradienttim.gradeway.database.models.permission.DatabasePermissionTemplatePermissionEntity
import dev.gradienttim.gradeway.database.models.player.*
import dev.gradienttim.gradeway.database.models.role.*
import dev.gradienttim.gradeway.database.models.track.DatabaseTrackEntity
import dev.gradienttim.gradeway.database.models.track.DatabaseTrackStageEntity
import dev.gradienttim.gradeway.extensions.createDirectoryIfNotExists
import dev.gradienttim.gradeway.extensions.limitedTo
import dev.gradienttim.gradeway.extensions.resolveWithinDirectory
import dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import dev.gradienttim.gradeway.ProjectMeta
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.decodeToSequence
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class CommonBackupManager(val gradeway: CommonGradeway<*>) : BackupManager {
    private val directory = gradeway.directory.createDirectoryIfNotExists(
        name = "backups",
        requiresRead = true,
        requiresWrite = true
    )

    // Parent-before-child order: every table only ever references tables earlier in this list, so
    // the same list drives export, import and (reversed) the pre-import wipe without needing a
    // separately maintained ordering for any of the three.
    private val backupEntries by lazy {
        listOf(
            backupEntry(DatabaseGroupEntity),
            backupEntry(DatabasePermissionEntity),
            backupEntry(DatabasePermissionTemplateEntity),
            backupEntry(DatabaseRoleEntity),
            backupEntry(DatabasePlayerEntity),
            backupEntry(DatabaseRoleAttributeEntity),
            backupEntry(DatabasePlayerAttributeEntity),
            backupEntry(DatabaseRoleGroupEntity),
            backupEntry(DatabaseRoleParentEntity),
            backupEntry(DatabaseRolePermissionEntity),
            backupEntry(DatabaseRolePermissionTemplateEntity),
            backupEntry(DatabasePlayerRoleEntity),
            backupEntry(DatabasePlayerPermissionEntity),
            backupEntry(DatabasePlayerPermissionTemplateEntity),
            backupEntry(DatabaseGroupPermissionEntity),
            backupEntry(DatabaseGroupPermissionTemplateEntity),
            backupEntry(DatabasePermissionTemplatePermissionEntity),
            backupEntry(DatabaseTrackEntity),
            backupEntry(DatabaseTrackStageEntity),
        )
    }

    override fun export(): Either<BackupManager.ExportError, Path> = either {
        val format = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

        val file = directory.resolve("$format.tar.gz")
        if (Files.exists(file)) {
            raise(BackupManager.ExportError.FileAlreadyExists)
        }

        val temporaryArchive = directory.resolve("$format.tar.gz.tmp")
        val temporaryFiles = mutableListOf<Path>()

        try {
            val exportedTables = transaction(gradeway.database) {
                backupEntries.map { entry ->
                    val temporaryFile = Files.createTempFile(directory, "export-${entry.name}-", ".jsonl.tmp")
                        .also(temporaryFiles::add)
                    val rows = temporaryFile.outputStream().buffered().use { entry.write(it) }
                    ExportedTable(entry.name, temporaryFile, rows)
                }
            }

            val manifest = BackupManifest(
                formatVersion = FORMAT_VERSION,
                gradewayVersion = ProjectMeta.VERSION,
                createdAt = Instant.now().toEpochMilli(),
                tables = exportedTables.associate { it.name to it.rows }
            )

            val archiveStream = GzipCompressorOutputStream(temporaryArchive.outputStream().buffered())
            TarArchiveOutputStream(archiveStream).use { stream ->
                stream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
                stream.putBytesEntry(MANIFEST_FILE_NAME, json.encodeToString(manifest).encodeToByteArray())
                exportedTables.forEach { table ->
                    stream.putFileEntry("${table.name}.$TABLE_FILE_EXTENSION", table.file)
                }
            }

            Files.move(temporaryArchive, file, StandardCopyOption.ATOMIC_MOVE)
        } catch (throwable: Throwable) {
            Files.deleteIfExists(temporaryArchive)
            raise(BackupManager.ExportError.Unexpected(throwable))
        } finally {
            temporaryFiles.forEach(Files::deleteIfExists)
        }

        file
    }

    override fun import(fileName: String, wipe: Boolean): Either<BackupManager.ImportError, Unit> = either {
        val file = directory.resolveWithinDirectory(fileName)
        if (file == null || !Files.exists(file)) {
            raise(BackupManager.ImportError.FileNotFound)
        }

        try {
            TarArchiveInputStream(
                GzipCompressorInputStream(file.inputStream().buffered())
                    .limitedTo(gradeway.configs.gradewayEntry.config.maxImportSizeBytes)
            ).use { stream ->
                transaction(gradeway.database) {
                    var entry = stream.nextEntry
                    val manifest = if (entry?.name == MANIFEST_FILE_NAME) {
                        stream.readManifest().also { entry = stream.nextEntry }
                    } else {
                        null
                    }

                    if (wipe) {
                        backupEntries.asReversed().forEach { it.wipe() }
                    }

                    val importedTables = mutableSetOf<String>()
                    while (entry != null) {
                        importEntry(fileName, stream, entry, manifest)?.let(importedTables::add)
                        entry = stream.nextEntry
                    }

                    val missingTables = manifest?.tables?.keys.orEmpty() - importedTables
                    if (missingTables.isNotEmpty()) {
                        throw IOException("The backup is missing the entries for ${missingTables.joinToString()}.")
                    }
                }
            }
        } catch (throwable: Throwable) {
            when (throwable) {
                is UnsupportedFormatVersionException -> raise(
                    BackupManager.ImportError.UnsupportedFormatVersion(throwable.version, FORMAT_VERSION)
                )

                is IOException, is IllegalArgumentException ->
                    raise(BackupManager.ImportError.CorruptArchive(throwable))

                else -> raise(BackupManager.ImportError.Unexpected(throwable))
            }
        }

        // Entries are deserialized directly via the DAO and never publish the fine-grained
        // payloads the services normally would, so every server's effective-permission and
        // effective-weight caches need to be dropped in full now that the import committed.
        gradeway.messaging.publish(CacheFlushPayload)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun JdbcTransaction.importEntry(
        fileName: String,
        stream: TarArchiveInputStream,
        entry: TarArchiveEntry,
        manifest: BackupManifest?,
    ): String? {
        if (!entry.isFile) {
            gradeway.logger.warn("Skipping entry ${entry.name} of $fileName: Entry is not a file.")
            return null
        }

        val extension = entry.name.substringAfterLast('.')
        val mode = when (extension) {
            TABLE_FILE_EXTENSION -> DecodeSequenceMode.WHITESPACE_SEPARATED
            LEGACY_TABLE_FILE_EXTENSION -> DecodeSequenceMode.ARRAY_WRAPPED
            else -> {
                gradeway.logger.warn("Skipping entry ${entry.name} of $fileName: Unknown file extension.")
                return null
            }
        }

        val tableName = entry.name.substringBeforeLast('.')
        val backupEntry = backupEntries.find { it.name == tableName }
        if (backupEntry == null) {
            gradeway.logger.warn("Skipping entry ${entry.name} of $fileName: No backup entry found for this file.")
            return null
        }

        var rows = 0L
        json.decodeToSequence(stream, JsonObject.serializer(), mode).forEach { row ->
            backupEntry.read(row)
            if (++rows % BATCH_SIZE == 0L) {
                entityCache.clear()
            }
        }
        entityCache.clear()

        val expectedRows = manifest?.tables?.get(tableName)
        if (expectedRows != null && expectedRows != rows) {
            throw IOException("Entry ${entry.name} contains $rows rows, but the manifest expects $expectedRows.")
        }
        return tableName
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun TarArchiveInputStream.readManifest(): BackupManifest {
        val manifest = json.decodeFromStream<JsonObject>(this)
        val version = manifest["formatVersion"]?.jsonPrimitive?.intOrNull
            ?: throw IOException("The backup manifest does not declare a format version.")
        if (version > FORMAT_VERSION) {
            throw UnsupportedFormatVersionException(version)
        }
        return json.decodeFromJsonElement(BackupManifest.serializer(), manifest)
    }

    private data class BackupEntry(
        val name: String,
        val write: (OutputStream) -> Long,
        val read: (JsonObject) -> Unit,
        val wipe: () -> Unit,
    )

    private data class ExportedTable(val name: String, val file: Path, val rows: Long)

    @Serializable
    private data class BackupManifest(
        val formatVersion: Int,
        val gradewayVersion: String,
        val createdAt: Long,
        val tables: Map<String, Long>,
    )

    private class UnsupportedFormatVersionException(val version: Int) :
        IllegalStateException("Unsupported backup format version $version")

    private fun <ID : Any, T : Entity<ID>, C> backupEntry(entityClass: C): BackupEntry
            where C : EntityClass<ID, T>, C : JsonSerializable<T> = BackupEntry(
        name = entityClass.table.tableName.replaceFirst(gradeway.configs.driversEntry.config.database.prefix, ""),
        write = { output -> writeRows(entityClass, output) },
        read = { json -> entityClass.deserialize(json) },
        wipe = { entityClass.table.deleteAll() }
    )

    @OptIn(ExperimentalSerializationApi::class)
    private fun <ID : Any, T : Entity<ID>, C> writeRows(entityClass: C, output: OutputStream): Long
            where C : EntityClass<ID, T>, C : JsonSerializable<T> {
        val transaction = TransactionManager.current()
        val query = entityClass.table.selectAll().notForUpdate().fetchSize(BATCH_SIZE)

        var rows = 0L
        entityClass.wrapRows(query).forEach { entity ->
            json.encodeToStream(JsonObject.serializer(), entityClass.serialize(entity), output)
            output.write(LINE_SEPARATOR)
            if (++rows % BATCH_SIZE == 0L) {
                transaction.entityCache.clear()
            }
        }
        transaction.entityCache.clear()
        return rows
    }

    private fun TarArchiveOutputStream.putBytesEntry(name: String, bytes: ByteArray) {
        val entry = TarArchiveEntry(name)
        entry.size = bytes.size.toLong()
        putArchiveEntry(entry)
        write(bytes)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.putFileEntry(name: String, file: Path) {
        val entry = TarArchiveEntry(name)
        entry.size = Files.size(file)
        putArchiveEntry(entry)
        file.inputStream().use { it.copyTo(this) }
        closeArchiveEntry()
    }

    private companion object {
        const val FORMAT_VERSION = 1
        const val BATCH_SIZE = 1000
        const val MANIFEST_FILE_NAME = "manifest.json"
        const val TABLE_FILE_EXTENSION = "jsonl"
        const val LEGACY_TABLE_FILE_EXTENSION = "json"
        const val LINE_SEPARATOR = '\n'.code

        val json = Json { ignoreUnknownKeys = true }
    }
}
