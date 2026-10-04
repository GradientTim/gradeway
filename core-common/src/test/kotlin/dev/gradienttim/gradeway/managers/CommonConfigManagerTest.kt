/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.TestScheduler
import dev.gradienttim.gradeway.configs.GradewayConfig
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.platform.CommonLogger
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonConfigManagerTest {
    private fun createGradeway(): CommonGradeway<TestPlatformConfig> = CommonGradeway(
        logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}, onPanic = {}),
        scheduler = TestScheduler(),
        directory = Files.createTempDirectory("config-manager-test"),
        defaultPlatformConfig = TestPlatformConfig(),
        platformConfigSerializer = TestPlatformConfig.serializer(),
    )

    @Test
    fun `load writes the default config file when none exists`() {
        val gradeway = createGradeway()
        val manager = CommonConfigManager(gradeway)

        manager.load().getOrElse { error(it.toString()) }

        val configFile = gradeway.directory.resolve("config.toml")
        assertTrue(configFile.exists())
        assertEquals(GradewayConfig.LATEST_VERSION, manager.gradewayEntry.config.version)
        assertEquals("gradeway_", manager.driversEntry.config.database.prefix)
    }

    @Test
    fun `load bumps an older config version and rewrites the file`() {
        val gradeway = createGradeway()
        val configFile = gradeway.directory.resolve("config.toml")
        configFile.writeText("version = 0\n")

        val manager = CommonConfigManager(gradeway)
        manager.load().getOrElse { error(it.toString()) }

        assertEquals(GradewayConfig.LATEST_VERSION, manager.gradewayEntry.config.version)
        val versionPattern = Regex("""version\s*=\s*${GradewayConfig.LATEST_VERSION}\b""")
        assertTrue(versionPattern.containsMatchIn(configFile.readText()))
    }

    @Test
    fun `load fills in the default role section for a config written before it existed`() {
        val gradeway = createGradeway()
        val configFile = gradeway.directory.resolve("config.toml")
        configFile.writeText("version = 1\nprimaryColor = \"#123456\"\n")

        val manager = CommonConfigManager(gradeway)
        manager.load().getOrElse { error(it.toString()) }

        assertEquals("#123456", manager.gradewayEntry.config.primaryColor)
        assertEquals(GradewayConfig.DefaultRoleConfig(), manager.gradewayEntry.config.defaultRole)
        assertTrue(configFile.readText().contains("assignWhenNoPrimaryRole"))
    }

    @Test
    fun `load sets TableConstants TABLE_PREFIX from the loaded config`() {
        val originalPrefix = TableConstants.TABLE_PREFIX
        try {
            val gradeway = createGradeway()
            val driversFile = gradeway.directory.resolve("drivers.toml")
            driversFile.writeText("[database]\nprefix = \"custom_\"\n")

            val manager = CommonConfigManager(gradeway)
            manager.load().getOrElse { error(it.toString()) }

            assertEquals("custom_", TableConstants.TABLE_PREFIX)
        } finally {
            TableConstants.TABLE_PREFIX = originalPrefix
        }
    }
}
