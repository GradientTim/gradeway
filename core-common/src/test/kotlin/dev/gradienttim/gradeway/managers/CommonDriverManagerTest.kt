/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import java.nio.file.Files
import kotlin.test.*

private class FakeDriver : Driver() {
    var unloaded = false
        private set

    override fun unload() {
        unloaded = true
    }
}

class CommonDriverManagerTest {
    private fun createManager(): CommonDriverManager {
        val gradeway = CommonGradeway(
            logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}),
            directory = Files.createTempDirectory("driver-manager-test").toFile(),
        )
        return CommonDriverManager(gradeway)
    }

    @Test
    fun `registerDriver succeeds once and rejects a duplicate id and type`() {
        val manager = createManager()
        val driver = FakeDriver()

        assertTrue(manager.registerDriver("dupe", DriverType.DATABASE, driver))
        assertFalse(manager.registerDriver("dupe", DriverType.DATABASE, FakeDriver()))
    }

    @Test
    fun `findDriver filters by both id and type`() {
        val manager = createManager()
        val driver = FakeDriver()
        manager.registerDriver("my-driver", DriverType.DATABASE, driver)

        assertSame(driver, manager.findDriver("my-driver", DriverType.DATABASE))
        assertNull(manager.findDriver("my-driver", DriverType.MESSAGING))
        assertNull(manager.findDriver("other-driver", DriverType.DATABASE))
    }

    @Test
    fun `unload unloads and clears every registered driver`() {
        val manager = createManager()
        val driver = FakeDriver()
        manager.registerDriver("my-driver", DriverType.DATABASE, driver)

        manager.unload().getOrElse { error(it.toString()) }

        assertTrue(driver.unloaded)
        assertNull(manager.findDriver("my-driver", DriverType.DATABASE))
    }
}
