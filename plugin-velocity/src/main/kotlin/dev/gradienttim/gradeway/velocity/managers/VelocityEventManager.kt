/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.managers

import dev.gradienttim.gradeway.event.CancellableEvent
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.managers.EventManager
import dev.gradienttim.gradeway.velocity.GradewayPlugin
import dev.gradienttim.gradeway.velocity.event.CancellableEvent as VelocityCancellableEvent
import dev.gradienttim.gradeway.velocity.event.Event as VelocityEvent

class VelocityEventManager(val plugin: GradewayPlugin) : EventManager {
    override fun <TEvent : Event> fire(event: TEvent) {
        val velocityEvent = convertGradewayToVelocityEvent(event)

        plugin.server.eventManager.fire(velocityEvent).thenAccept {
            if (it is VelocityCancellableEvent && event is CancellableEvent) {
                event.isCancelled = !it.result.isAllowed
            }
        }
    }

    private fun <TEvent : Event> convertGradewayToVelocityEvent(event: TEvent): VelocityEvent {
        throw IllegalArgumentException("Unhandled Gradeway event type: ${event::class.simpleName}")
//        return when (event) {
//            else -> throw IllegalArgumentException("Unhandled Gradeway event type: ${event::class.simpleName}")
//        }
    }
}
