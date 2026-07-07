/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import java.io.File

/**
 * Interface for managing backup-related operations, including exporting and importing backup data.
 */
interface BackupManager {
    /**
     * Exports backup data.
     *
     * @return An Either containing an ExportError if the export operation fails,
     *         or a File representing the exported backup on success.
     */
    fun export(): Either<ExportError, File>

    /**
     * Imports backup data from the specified file.
     *
     * @param fileName The name of the file from which backup data will be imported.
     * @param wipe If true, the existing backup data will be wiped before importing. Defaults to true.
     * @return Either an ImportError if the import operation fails, or Unit if the import is successful.
     */
    fun import(fileName: String, wipe: Boolean = true): Either<ImportError, Unit>

    sealed interface ExportError {
        object FileAlreadyExists : ExportError
        data class Unexpected(val throwable: Throwable) : ExportError
    }

    sealed interface ImportError {
        object FileNotFound : ImportError
        data class CorruptArchive(val throwable: Throwable) : ImportError
        data class Unexpected(val throwable: Throwable) : ImportError
    }
}
