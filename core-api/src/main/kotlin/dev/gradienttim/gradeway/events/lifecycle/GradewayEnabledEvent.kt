/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.lifecycle

import dev.gradienttim.gradeway.event.Event

/**
 * Fired on this server after Gradeway was enabled and connected to its database.
 */
class GradewayEnabledEvent : Event<Event.EmptyResult>(EmptyResult.None)
