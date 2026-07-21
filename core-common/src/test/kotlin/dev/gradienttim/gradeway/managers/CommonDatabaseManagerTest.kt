/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestDatabaseDriver
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import dev.gradienttim.gradeway.throwables.driver.DriverBlankIdentifierThrowable
import dev.gradienttim.gradeway.throwables.driver.DriverNotFoundThrowable
import dev.gradienttim.gradeway.throwables.driver.DriverUnsupportedAdapterThrowable
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class NotADatabaseDriver : Driver()

class CommonDatabaseManagerTest {
    private var gradeway: CommonGradeway? = null

    private fun createLoadedGradeway(): CommonGradeway {
        val instance = CommonGradeway(
            logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}),
            directory = Files.createTempDirectory("database-manager-test").toFile(),
        )
        instance.load().getOrElse { error(it.toString()) }
        gradeway = instance
        return instance
    }

    @AfterTest
    fun tearDown() {
        gradeway?.unload()?.getOrElse { error(it.toString()) }
        gradeway = null
    }

    @Test
    fun `enable fails with a blank driver id`() {
        val gradeway = createLoadedGradeway()
        gradeway.configs.config.database.driver = ""

        val result = gradeway.databases.enable()

        assertIs<DriverBlankIdentifierThrowable>(result.leftOrNull())
    }

    @Test
    fun `enable fails when the driver is not registered`() {
        val gradeway = createLoadedGradeway()
        gradeway.configs.config.database.driver = "does-not-exist"

        val result = gradeway.databases.enable()

        assertIs<DriverNotFoundThrowable>(result.leftOrNull())
    }

    @Test
    fun `enable fails when the registered driver is not a DatabaseAdapter`() {
        val gradeway = createLoadedGradeway()
        gradeway.drivers.registerDriver(
            id = "not-a-db-adapter",
            type = DriverType.DATABASE,
            driver = NotADatabaseDriver()
        )
        gradeway.configs.config.database.driver = "not-a-db-adapter"

        val result = gradeway.databases.enable()

        assertIs<DriverUnsupportedAdapterThrowable>(result.leftOrNull())
    }

    @Test
    fun `enable with a valid driver creates the expected tables`() {
        val gradeway = createLoadedGradeway()
        gradeway.drivers.registerDriver(id = "test", type = DriverType.DATABASE, driver = TestDatabaseDriver())
        gradeway.configs.config.database.driver = "test"

        gradeway.databases.enable().getOrElse { error(it.toString()) }

        assertTrue(transaction(gradeway.database) { RolesTable.exists() })
    }

    @Test
    fun `disable is idempotent`() {
        val gradeway = createLoadedGradeway()
        gradeway.drivers.registerDriver(id = "test", type = DriverType.DATABASE, driver = TestDatabaseDriver())
        gradeway.configs.config.database.driver = "test"
        gradeway.databases.enable().getOrElse { error(it.toString()) }

        gradeway.databases.disable().getOrElse { error(it.toString()) }
        gradeway.databases.disable().getOrElse { error(it.toString()) }
    }
}
