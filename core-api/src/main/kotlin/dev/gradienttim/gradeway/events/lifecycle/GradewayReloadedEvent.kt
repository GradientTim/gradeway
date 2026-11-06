/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.lifecycle

import dev.gradienttim.gradeway.event.Event

/**
 * Fired on this server after the Gradeway configuration was reloaded.
 */
class GradewayReloadedEvent : Event<Event.EmptyResult>(EmptyResult.None)
