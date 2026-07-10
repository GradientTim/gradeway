/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.getOrElse
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import java.nio.file.Files

fun createTestGradeway(): CommonGradeway {
    val gradeway = CommonGradeway(
        logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}),
        directory = Files.createTempDirectory("gradeway-test").toFile(),
    )

    gradeway.load()
        .onLeft { error("Failed to load test Gradeway: $it") }
        .onRight {
            gradeway.drivers.registerDriver(
                id = "test",
                type = DriverType.DATABASE,
                driver = TestDatabaseDriver()
            )
            gradeway.configs.config.database.driver = "test"

            gradeway.enable().onLeft { error("Failed to enable test Gradeway: $it") }
        }

    return gradeway
}

fun CommonGradeway.disposeTestGradeway() {
    disable()
        .onLeft {
            error("Failed to disable test Gradeway: $it")
        }
        .onRight {
            unload().getOrElse { error("Failed to unload test Gradeway: $it") }
        }
}
