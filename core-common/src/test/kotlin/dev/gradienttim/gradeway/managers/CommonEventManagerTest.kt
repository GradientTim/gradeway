/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventPriority
import dev.gradienttim.gradeway.event.PostEvent
import dev.gradienttim.gradeway.platform.CommonLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CommonEventManagerTest {
    private val errors = mutableListOf<String>()
    private val events = CommonEventManager(
        CommonLogger(onInfo = {}, onWarn = {}, onError = { errors += it }, onPanic = {}),
    )

    private class TestEvent : Event<Event.CancelledResult>(CancelledResult.Allow)
    private class OtherEvent : PostEvent()

    @Test
    fun `listeners only receive events of their type`() {
        val received = mutableListOf<Event<*>>()
        events.subscribe<TestEvent>(this) { received += it }

        events.fire(OtherEvent())
        val event = TestEvent()
        events.fire(event)

        assertEquals(listOf<Event<*>>(event), received)
    }

    @Test
    fun `listeners of a supertype receive every subtype`() {
        val received = mutableListOf<Event<*>>()
        events.subscribe<Event<*>>(this) { received += it }

        events.fire(TestEvent())
        events.fire(OtherEvent())

        assertEquals(2, received.size)
    }

    @Test
    fun `listeners run in priority order and later ones decide the result`() {
        val order = mutableListOf<EventPriority>()
        events.subscribe<TestEvent>(this, EventPriority.HIGH) {
            order += EventPriority.HIGH
            it.result = Event.CancelledResult.Allow
        }
        events.subscribe<TestEvent>(this, EventPriority.LOW) {
            order += EventPriority.LOW
            it.result = Event.CancelledResult.Cancel
        }
        events.subscribe<TestEvent>(this, EventPriority.MONITOR) { order += EventPriority.MONITOR }

        val event = TestEvent()
        events.fire(event)

        assertEquals(listOf(EventPriority.LOW, EventPriority.HIGH, EventPriority.MONITOR), order)
        assertEquals(Event.CancelledResult.Allow, event.result)
    }

    @Test
    fun `unsubscribed listeners no longer receive events`() {
        var calls = 0
        val subscription = events.subscribe<TestEvent>(this) { calls++ }

        subscription.unsubscribe()
        events.fire(TestEvent())

        assertFalse(subscription.isActive)
        assertEquals(0, calls)
    }

    @Test
    fun `unsubscribeAll only removes the listeners of that owner`() {
        val owner = Any()
        var ownerCalls = 0
        var otherCalls = 0
        events.subscribe<TestEvent>(owner) { ownerCalls++ }
        events.subscribe<TestEvent>(this) { otherCalls++ }

        events.unsubscribeAll(owner)
        events.fire(TestEvent())

        assertEquals(0, ownerCalls)
        assertEquals(1, otherCalls)
    }

    @Test
    fun `a failing listener does not stop the others`() {
        var calls = 0
        events.subscribe<TestEvent>(this, EventPriority.LOW) { error("boom") }
        events.subscribe<TestEvent>(this, EventPriority.HIGH) { calls++ }

        events.fire(TestEvent())

        assertEquals(1, calls)
        assertEquals(1, errors.size)
    }
}
