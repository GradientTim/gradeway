/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.template

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.event.Event
import dev.gradienttim.gradeway.spec.TemplateCreateSpec

/**
 * Fired before a permission template is created. Listeners may change the [spec].
 */
class TemplateCreateEvent(
    val actor: Actor,
    val spec: TemplateCreateSpec,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
