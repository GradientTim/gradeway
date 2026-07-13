/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import dev.gradienttim.gradeway.strategy.MigrationStrategy

interface MigrationManager {
    fun migrate(type: String, fileName: String): Either<MigrateError, Unit>
    fun migrate(strategy: MigrationStrategy, fileName: String): Either<MigrateError, Unit>

    sealed interface MigrateError {
        object FileNotFound : MigrateError
        object StrategyNotRegistered : MigrateError
        data class Unexpected(val throwable: Throwable) : MigrateError
    }
}
