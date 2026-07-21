/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import net.kyori.adventure.audience.Audience
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonConfirmationManagerTest {
    private val manager = CommonConfirmationManager()
    private val sender = object : Audience {}

    @BeforeTest
    fun setUp() {
        manager.load().getOrElse { error(it.toString()) }
    }

    @AfterTest
    fun tearDown() {
        manager.disable()
        manager.unload()
    }

    @Test
    fun `request registers a job with an id from the unambiguous alphabet`() {
        val id = manager.request(sender, task = {}, onTimeout = {}).getOrElse { error(it.toString()) }

        assertEquals(6, id.length)
        assertTrue(id.all { it in "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" })
        assertEquals(1, manager.jobs.size)
    }

    @Test
    fun `confirm runs the task and removes the job`() {
        var ran = false
        val id = manager.request(sender, task = { ran = true }, onTimeout = {}).getOrElse { error(it.toString()) }

        manager.confirm(sender, id).getOrElse { error(it.toString()) }

        assertTrue(ran)
        assertTrue(manager.jobs.isEmpty())
    }

    @Test
    fun `confirm from the wrong sender fails without running the task`() {
        var ran = false
        val id = manager.request(sender, task = { ran = true }, onTimeout = {}).getOrElse { error(it.toString()) }
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
        val id = manager.request(sender, task = { ran = true }, onTimeout = {}).getOrElse { error(it.toString()) }

        manager.cancel(sender, id).getOrElse { error(it.toString()) }

        assertFalse(ran)
        assertTrue(manager.jobs.isEmpty())
    }

    @Test
    fun `cancel from the wrong sender fails`() {
        val id = manager.request(sender, task = {}, onTimeout = {}).getOrElse { error(it.toString()) }
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
        manager.request(sender, task = {}, onTimeout = {}).getOrElse { error(it.toString()) }
        manager.request(sender, task = {}, onTimeout = {}).getOrElse { error(it.toString()) }

        manager.disable().getOrElse { error(it.toString()) }

        assertTrue(manager.jobs.isEmpty())
    }
}
