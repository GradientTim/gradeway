/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import dev.gradienttim.gradeway.strategy.MigrationStrategy

/**
 * Interface for managing migration operations.
 *
 * The `MigrationManager` interface provides methods for performing migrations
 * based on a specified type or using a custom-defined migration strategy. It ensures
 * support for error handling during migration processes through the `MigrateError` sealed interface.
 *
 * Migrations can be invoked by either specifying a type as a string or providing a concrete
 * implementation of the `MigrationStrategy` interface. The interface supports extensible error
 * handling for various failure scenarios that may arise during migrations.
 *
 * Responsibilities:
 * - Migrating data based on a predefined type or strategy.
 * - Handling errors through the `MigrateError` sealed interface, which categorizes issues such as
 *   file not found, unregistered strategies, or unexpected exceptions.
 *
 * Error Handling:
 * - `FileNotFound`: Indicates that the specified file could not be located.
 * - `StrategyNotRegistered`: Indicates that a strategy for the specified type has not been registered.
 * - `Unexpected`: Encapsulates an exception that occurred during the migration process.
 */
interface MigrationManager {
    /**
     * Performs a migration operation based on the specified migration type and file name.
     *
     * The migration process is executed by identifying a registered migration strategy for the given
     * type and applying it to the specified file. If no strategy is found for the provided type,
     * or if any other error occurs during the migration process, an appropriate `MigrateError` is returned.
     *
     * @param type A string representing the type of migration to be performed. The type is used to
     *             identify the appropriate migration strategy.
     * @param fileName The name of the file to which the migration operation will be applied.
     *                 The file serves as the input for the migration process.
     * @return An `Either` containing a `MigrateError` on failure, or `Unit` on successful migration.
     */
    fun migrate(type: String, fileName: String): Either<MigrateError, Unit>

    /**
     * Executes a migration operation based on the provided migration strategy and file name.
     *
     * This method identifies and applies the specified `MigrationStrategy` to the given file,
     * performing the necessary migration steps. If the migration fails due to issues such as
     * a missing file, an unregistered strategy, or unexpected errors, a corresponding `MigrateError`
     * is returned.
     *
     * @param strategy The migration strategy to be applied. This parameter defines the specific rules
     *                 and behavior for processing the migration.
     * @param fileName The name of the file on which the migration operation is to be performed.
     * @return An `Either` containing a `MigrateError` if the migration fails, or `Unit` if it succeeds.
     */
    fun migrate(strategy: MigrationStrategy, fileName: String): Either<MigrateError, Unit>

    sealed interface MigrateError {
        object FileNotFound : MigrateError
        object StrategyNotRegistered : MigrateError
        data class Unexpected(val throwable: Throwable) : MigrateError
    }
}
