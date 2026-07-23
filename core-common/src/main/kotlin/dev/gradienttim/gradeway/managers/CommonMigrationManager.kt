/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.extensions.createDirectoryIfNotExists
import dev.gradienttim.gradeway.extensions.resolveWithinDirectory
import dev.gradienttim.gradeway.registries.MigrationStrategyRegistry
import dev.gradienttim.gradeway.strategies.LuckPermsMigrationStrategy
import dev.gradienttim.gradeway.strategy.MigrationStrategy

class CommonMigrationManager<TPlatformConfig>(val gradeway: CommonGradeway<TPlatformConfig>) : MigrationManager {
    private val directory = gradeway.directory.createDirectoryIfNotExists(
        name = "migrations",
        requiresRead = true,
        requiresWrite = false
    )

    override fun migrate(
        type: String,
        fileName: String
    ): Either<MigrationManager.MigrateError, Unit> = either {
        val strategy = MigrationStrategyRegistry.find(type)
            ?: raise(MigrationManager.MigrateError.StrategyNotRegistered)

        return migrate(strategy, fileName)
    }

    override fun migrate(
        strategy: MigrationStrategy,
        fileName: String
    ): Either<MigrationManager.MigrateError, Unit> = either {
        val file = directory.resolveWithinDirectory(fileName)
        if (file == null || !file.exists()) {
            raise(MigrationManager.MigrateError.FileNotFound)
        }

        strategy.migrate(file)
            .onLeft { raise(MigrationManager.MigrateError.Unexpected(it)) }
    }

    init {
        MigrationStrategyRegistry.register(LuckPermsMigrationStrategy(gradeway))
    }
}
