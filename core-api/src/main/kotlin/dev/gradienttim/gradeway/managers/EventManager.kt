/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventPriority
import dev.gradienttim.gradeway.event.EventSubscription
import kotlin.reflect.KClass

/**
 * Gradeway's platform independent event bus.
 *
 * Listeners are plain functions subscribed to an event type. A listener receives every fired event
 * that is an instance of its type, so subscribing to [dev.gradienttim.gradeway.event.PostEvent] or
 * [Event] receives all post events or all events. Listeners run on the thread that fires the event,
 * ordered by [EventPriority], and an exception thrown by one listener does not affect the others.
 */
interface EventManager {
    /**
     * Subscribes [listener] to every event that is an instance of [type].
     *
     * @param owner The plugin or object the subscription belongs to, used by [unsubscribeAll].
     * @param type The event type to listen to.
     * @param priority When the listener runs relative to the other listeners.
     * @param listener The function called with each matching event.
     * @return A handle to cancel the subscription.
     */
    fun <TEvent : Event<*>> subscribe(
        owner: Any,
        type: KClass<TEvent>,
        priority: EventPriority = EventPriority.NORMAL,
        listener: (TEvent) -> Unit,
    ): EventSubscription

    /**
     * Cancels every subscription of [owner], e.g. when that plugin is disabled.
     */
    fun unsubscribeAll(owner: Any)

    /**
     * Fires [event] to every matching listener.
     *
     * Gradeway fires its own events; other plugins normally only subscribe.
     */
    fun <TEvent : Event<*>> fire(event: TEvent)
}

/**
 * Subscribes [listener] to every event that is an instance of [TEvent].
 *
 * @see EventManager.subscribe
 */
inline fun <reified TEvent : Event<*>> EventManager.subscribe(
    owner: Any,
    priority: EventPriority = EventPriority.NORMAL,
    noinline listener: (TEvent) -> Unit,
): EventSubscription = subscribe(owner, TEvent::class, priority, listener)
