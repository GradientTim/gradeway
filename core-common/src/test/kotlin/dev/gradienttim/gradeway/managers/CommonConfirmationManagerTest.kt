/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.TestScheduler
import dev.gradienttim.gradeway.managers.ConfirmationManager
import dev.gradienttim.gradeway.platform.CommonLogger
import net.kyori.adventure.audience.Audience
import java.nio.file.Files
import kotlin.test.*

class CommonConfirmationManagerTest {
    private var gradeway: CommonGradeway<TestPlatformConfig>? = null
    private lateinit var manager: ConfirmationManager
    private val sender = object : Audience {}

    @BeforeTest
    fun setUp() {
        val instance = CommonGradeway(
            logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}),
            scheduler = TestScheduler(),
            directory = Files.createTempDirectory("confirmation-manager-test").toFile(),
            defaultPlatformConfig = TestPlatformConfig(),
            platformConfigSerializer = TestPlatformConfig.serializer(),
        )
        instance.load().getOrElse { error(it.toString()) }
        gradeway = instance
        manager = instance.confirmations
    }

    @AfterTest
    fun tearDown() {
        manager.disable().getOrElse { error(it.toString()) }
        gradeway?.unload()?.getOrElse { error(it.toString()) }
        gradeway = null
    }

    @Test
    fun `request registers a job with an id from the unambiguous alphabet`() {
        val id = manager.request(sender, handler = {}, onTimeout = {}).getOrElse { error(it.toString()) }

        assertEquals(6, id.length)
        assertTrue(id.all { it in "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" })
        assertEquals(1, manager.jobs.size)
    }

    @Test
    fun `confirm runs the task and removes the job`() {
        var ran = false
        val id = manager.request(sender, handler = { ran = true }, onTimeout = {}).getOrElse { error(it.toString()) }

        manager.confirm(sender, id).getOrElse { error(it.toString()) }

        assertTrue(ran)
        assertTrue(manager.jobs.isEmpty())
    }

    @Test
    fun `confirm from the wrong sender fails without running the task`() {
        var ran = false
        val id = manager.request(sender, handler = { ran = true }, onTimeout = {}).getOrElse { error(it.toString()) }
        val impostor = object : Audience {}

        val result = manager.confirm(impostor, id)

        assertEquals(ConfirmationManager.ConfirmJobError.WrongSender, result.leftOrNull())
        assertFalse(ran)
        assertEquals(1, manager.jobs.size)
    }

    @Test
    fun `confirm with an unregistered id fails`() {
        val result = manager.confirm(sender, "NOPE99")

        assertEquals(ConfirmationManager.ConfirmJobError.NotRegistered, result.leftOrNull())
    }

    @Test
    fun `cancel removes the job without running the task`() {
        var ran = false
        val id = manager.request(sender, handler = { ran = true }, onTimeout = {}).getOrElse { error(it.toString()) }

        manager.cancel(sender, id).getOrElse { error(it.toString()) }

        assertFalse(ran)
        assertTrue(manager.jobs.isEmpty())
    }

    @Test
    fun `cancel from the wrong sender fails`() {
        val id = manager.request(sender, handler = {}, onTimeout = {}).getOrElse { error(it.toString()) }
        val impostor = object : Audience {}

        val result = manager.cancel(impostor, id)

        assertEquals(ConfirmationManager.CancelJobError.WrongSender, result.leftOrNull())
        assertEquals(1, manager.jobs.size)
    }

    @Test
    fun `cancel with an unregistered id fails`() {
        val result = manager.cancel(sender, "NOPE99")

        assertEquals(ConfirmationManager.CancelJobError.NotRegistered, result.leftOrNull())
    }

    @Test
    fun `disable cancels and clears every outstanding job`() {
        manager.request(sender, handler = {}, onTimeout = {}).getOrElse { error(it.toString()) }
        manager.request(sender, handler = {}, onTimeout = {}).getOrElse { error(it.toString()) }

        manager.disable().getOrElse { error(it.toString()) }

        assertTrue(manager.jobs.isEmpty())
    }
}
