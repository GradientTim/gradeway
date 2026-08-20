/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import arrow.core.getOrElse
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.platform.CommonLogger
import dev.gradienttim.gradeway.platform.Scheduler
import java.nio.file.Files
import java.util.concurrent.TimeUnit

fun createTestGradeway(): CommonGradeway<TestPlatformConfig> {
    val gradeway = CommonGradeway(
        logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}),
        scheduler = TestScheduler(),
        directory = Files.createTempDirectory("gradeway-test").toFile(),
        defaultPlatformConfig = TestPlatformConfig(),
        platformConfigSerializer = TestPlatformConfig.serializer(),
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

fun CommonGradeway<TestPlatformConfig>.disposeTestGradeway() {
    disable()
        .onLeft {
            error("Failed to disable test Gradeway: $it")
        }
        .onRight {
            unload().getOrElse { error("Failed to unload test Gradeway: $it") }
        }
}

class TestScheduler : Scheduler {
    override fun runTask(task: () -> Unit) {
        task()
    }

    override fun runTaskLater(delay: Long, unit: TimeUnit, task: () -> Unit): Scheduler.Task {
        task()
        return TestGradewayTask()
    }

    override fun runTaskTimer(
        delay: Long,
        delayUnit: TimeUnit,
        interval: Long,
        intervalUnit: TimeUnit,
        task: () -> Unit
    ): Scheduler.Task {
        task()
        return TestGradewayTask()
    }

    class TestGradewayTask : Scheduler.Task {
        override fun cancel(): Boolean = true
    }
}
