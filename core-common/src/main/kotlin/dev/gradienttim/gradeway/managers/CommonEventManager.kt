/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.event.EventPriority
import dev.gradienttim.gradeway.event.EventSubscription
import dev.gradienttim.gradeway.platform.Logger
import kotlin.reflect.KClass

class CommonEventManager(private val logger: Logger) : EventManager {
    private val lock = Any()

    @Volatile
    private var subscriptions: List<Subscription<*>> = emptyList()

    override fun <TEvent : Event<*>> subscribe(
        owner: Any,
        type: KClass<TEvent>,
        priority: EventPriority,
        listener: (TEvent) -> Unit,
    ): EventSubscription {
        val subscription = Subscription(owner, type, priority, listener)
        synchronized(lock) {
            subscriptions = (subscriptions + subscription).sortedBy { it.priority }
        }
        return subscription
    }

    override fun unsubscribeAll(owner: Any) {
        synchronized(lock) {
            subscriptions = subscriptions.filterNot { it.owner == owner }
        }
    }

    override fun <TEvent : Event<*>> fire(event: TEvent) {
        subscriptions.forEach { subscription ->
            try {
                subscription.deliver(event)
            } catch (throwable: Throwable) {
                logger.error(
                    "${subscription.owner} failed to handle ${event::class.simpleName}: ${throwable.localizedMessage}",
                )
            }
        }
    }

    private fun remove(subscription: Subscription<*>) {
        synchronized(lock) {
            subscriptions = subscriptions - subscription
        }
    }

    private inner class Subscription<TEvent : Event<*>>(
        val owner: Any,
        val type: KClass<TEvent>,
        val priority: EventPriority,
        val listener: (TEvent) -> Unit,
    ) : EventSubscription {
        override val isActive: Boolean
            get() = this in subscriptions

        fun deliver(event: Event<*>) {
            if (type.isInstance(event)) {
                @Suppress("UNCHECKED_CAST")
                listener(event as TEvent)
            }
        }

        override fun unsubscribe() = remove(this)
    }
}
