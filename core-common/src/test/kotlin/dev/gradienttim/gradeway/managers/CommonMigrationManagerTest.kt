/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.TestScheduler
import dev.gradienttim.gradeway.platform.CommonLogger
import dev.gradienttim.gradeway.registries.MigrationStrategyRegistry
import dev.gradienttim.gradeway.strategy.MigrationStrategy
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeMigrationStrategy(override val type: String) : MigrationStrategy {
    var migratedFile: File? = null

    override fun migrate(file: File): Either<Throwable, Unit> {
        migratedFile = file
        return Either.Right(Unit)
    }
}

class CommonMigrationManagerTest {
    private fun createManager(): Pair<CommonGradeway<TestPlatformConfig>, CommonMigrationManager> {
        val gradeway = CommonGradeway(
            logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}, onPanic = {}),
            scheduler = TestScheduler(),
            directory = Files.createTempDirectory("migration-manager-test"),
            defaultPlatformConfig = TestPlatformConfig(),
            platformConfigSerializer = TestPlatformConfig.serializer(),
        )
        return gradeway to CommonMigrationManager(gradeway)
    }

    @Test
    fun `migrate dispatches to the strategy registered for the given type`() {
        val (gradeway, manager) = createManager()
        val type = "fake-${UUID.randomUUID()}"
        val strategy = FakeMigrationStrategy(type)
        MigrationStrategyRegistry.register(strategy)
        val file = gradeway.directory.resolve("migrations").createDirectories().resolve("data.tar.gz")
        file.writeText("data")

        manager.migrate(type, "data.tar.gz").getOrElse { error(it.toString()) }

        assertEquals(file.toFile(), strategy.migratedFile)
    }

    @Test
    fun `migrate fails when no strategy is registered for the type`() {
        val (_, manager) = createManager()

        val result = manager.migrate("unregistered-${UUID.randomUUID()}", "data.tar.gz")

        assertEquals(MigrationManager.MigrateError.StrategyNotRegistered, result.leftOrNull())
    }

    @Test
    fun `migrate fails when the file does not exist`() {
        val (_, manager) = createManager()
        val type = "fake-${UUID.randomUUID()}"
        MigrationStrategyRegistry.register(FakeMigrationStrategy(type))

        val result = manager.migrate(type, "does-not-exist.tar.gz")

        assertEquals(MigrationManager.MigrateError.FileNotFound, result.leftOrNull())
    }

    @Test
    fun `migrate rejects a file name that would escape the migrations directory`() {
        val (_, manager) = createManager()
        val type = "fake-${UUID.randomUUID()}"
        MigrationStrategyRegistry.register(FakeMigrationStrategy(type))

        val result = manager.migrate(type, "../../etc/passwd")

        assertEquals(MigrationManager.MigrateError.FileNotFound, result.leftOrNull())
    }
}
