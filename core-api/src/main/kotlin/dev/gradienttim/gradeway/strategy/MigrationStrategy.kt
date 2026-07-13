/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.strategy

import arrow.core.Either
import dev.gradienttim.gradeway.utilities.Typed
import java.io.File

/**
 * Defines a strategy interface for migrating data represented as files.
 * Migration strategies implementing this interface can specify how the migration
 * process is executed and handle success or failure scenarios appropriately.
 */
interface MigrationStrategy : Typed {
    /**
     * Migrates the contents of the given file according to the implemented migration strategy.
     *
     * @param file The file to be migrated.
     * @return An Either instance that represents the result of the migration.
     *         It is a success result with Unit if the migration completes successfully,
     *         or a failure result with a Throwable if an error occurs during migration.
     */
    fun migrate(file: File): Either<Throwable, Unit>
}
