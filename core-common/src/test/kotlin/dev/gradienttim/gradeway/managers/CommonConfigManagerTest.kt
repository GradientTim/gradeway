/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.config.GradewayConfig
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.platform.CommonLogger
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonConfigManagerTest {
    private fun createGradeway(): CommonGradeway = CommonGradeway(
        logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}),
        directory = Files.createTempDirectory("config-manager-test").toFile(),
    )

    @Test
    fun `load writes the default config file when none exists`() {
        val gradeway = createGradeway()
        val manager = CommonConfigManager(gradeway)

        manager.load().getOrElse { error(it.toString()) }

        val configFile = File(gradeway.directory, "config.toml")
        assertTrue(configFile.exists())
        assertEquals(GradewayConfig.LATEST_VERSION, manager.config.version)
        assertEquals("gradeway_", manager.config.database.prefix)
    }

    @Test
    fun `load bumps an older config version and rewrites the file`() {
        val gradeway = createGradeway()
        val configFile = File(gradeway.directory, "config.toml")
        configFile.writeText("version = 0\n")

        val manager = CommonConfigManager(gradeway)
        manager.load().getOrElse { error(it.toString()) }

        assertEquals(GradewayConfig.LATEST_VERSION, manager.config.version)
        assertTrue(configFile.readText().contains("version = ${GradewayConfig.LATEST_VERSION}"))
    }

    @Test
    fun `load sets TableConstants TABLE_PREFIX from the loaded config`() {
        val originalPrefix = TableConstants.TABLE_PREFIX
        try {
            val gradeway = createGradeway()
            val configFile = File(gradeway.directory, "config.toml")
            configFile.writeText("[database]\nprefix = \"custom_\"\n")

            val manager = CommonConfigManager(gradeway)
            manager.load().getOrElse { error(it.toString()) }

            assertEquals("custom_", TableConstants.TABLE_PREFIX)
        } finally {
            TableConstants.TABLE_PREFIX = originalPrefix
        }
    }
}
