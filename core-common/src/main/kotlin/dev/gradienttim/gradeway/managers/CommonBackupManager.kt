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
import dev.gradienttim.gradeway.extensions.createDirectoryIfNotExists
import dev.gradienttim.gradeway.extensions.limitedTo
import dev.gradienttim.gradeway.extensions.resolveWithinDirectory
import dev.gradienttim.gradeway.messaging.payloads.CacheFlushPayload
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CommonBackupManager<TPlatformConfig>(val gradeway: CommonGradeway<TPlatformConfig>) : BackupManager {
    private val directory = gradeway.directory.createDirectoryIfNotExists(
        name = "backups",
        requiresRead = true,
        requiresWrite = true
    )

    // Parent-before-child order: every table only ever references tables earlier in this list, so
    // the same list drives export, import and (reversed) the pre-import wipe without needing a
    // separately maintained ordering for any of the three.
    private val backupEntries by lazy {
        listOf<BackupEntry>(
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
        )
    }

    override fun export(): Either<BackupManager.ExportError, File> = either {
        val format = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

        val file = File(directory, "$format.tar.gz")
        if (file.exists()) {
            raise(BackupManager.ExportError.FileAlreadyExists)
        }

        try {
            TarArchiveOutputStream(GzipCompressorOutputStream(file.outputStream())).use { stream ->
                stream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)

                transaction(gradeway.database) {
                    backupEntries.forEach { entry ->
                        stream.putJsonEntry("${entry.name}.json", entry.write())
                    }
                }
            }

            file
        } catch (throwable: Throwable) {
            raise(BackupManager.ExportError.Unexpected(throwable))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun import(fileName: String, wipe: Boolean): Either<BackupManager.ImportError, Unit> = either {
        val file = directory.resolveWithinDirectory(fileName)
        if (file == null || !file.exists()) {
            raise(BackupManager.ImportError.FileNotFound)
        }

        try {
            TarArchiveInputStream(
                GzipCompressorInputStream(file.inputStream())
                    .limitedTo(gradeway.configs.config.backup.maxImportSizeBytes)
            ).use { stream ->
                transaction(gradeway.database) {
                    if (wipe) {
                        backupEntries.asReversed().forEach { it.wipe() }
                    }

                    generateSequence { stream.nextEntry }.forEach { entry ->
                        if (!entry.isFile || entry.realSize == 0L) {
                            gradeway.logger.warn("Skipping import of $fileName: Entry is not a file or is empty.")
                            return@forEach
                        }

                        val fileName = entry.name.substringBeforeLast('.')
                        val backupEntry = backupEntries.find { it.name == fileName }
                        if (backupEntry == null) {
                            gradeway.logger.warn("Skipping import of $fileName: No backup entry found for this file")
                            return@forEach
                        }

                        val data = runCatching {
                            Json.decodeFromStream<List<JsonObject>>(stream)
                        }.getOrNull()

                        if (data == null) {
                            gradeway.logger.warn("Skipping import of $fileName: Data could not be successfully read.")
                            return@forEach
                        }

                        data.forEach { backupEntry.read(it) }
                    }
                }
            }
        } catch (throwable: Throwable) {
            if (throwable is IOException) {
                raise(BackupManager.ImportError.CorruptArchive(throwable))
            }
            raise(BackupManager.ImportError.Unexpected(throwable))
        }

        // Entries are deserialized directly via the DAO and never publish the fine-grained
        // payloads the services normally would, so every server's effective-permission and
        // effective-weight caches need to be dropped in full now that the import committed.
        gradeway.messaging.publish(CacheFlushPayload)
    }

    private data class BackupEntry(
        val name: String,
        val write: () -> List<JsonObject>,
        val read: (JsonObject) -> Unit,
        val wipe: () -> Unit,
    )

    private fun <ID : Any, T : Entity<ID>, C> backupEntry(entityClass: C): BackupEntry
            where C : EntityClass<ID, T>, C : JsonSerializable<T> = BackupEntry(
        name = entityClass.table.tableName.replaceFirst(gradeway.configs.config.database.prefix, ""),
        write = { entityClass.all().map(entityClass::serialize) },
        read = { json -> entityClass.deserialize(json) },
        wipe = { entityClass.table.deleteAll() }
    )

    private inline fun <reified T> TarArchiveOutputStream.putJsonEntry(name: String, value: T) {
        val bytes = Json.encodeToString(value).encodeToByteArray()
        val entry = TarArchiveEntry(name)
        entry.size = bytes.size.toLong()
        putArchiveEntry(entry)
        write(bytes)
        closeArchiveEntry()
    }
}
