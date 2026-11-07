/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

/**
 * Base type for events fired after a change has been committed.
 *
 * Post events cannot be canceled. Every post event is serializable so it can be relayed to the
 * other servers of a network, where it is fired again with a [EventOrigin.Remote] [origin].
 */
abstract class PostEvent : Event<Event.EmptyResult>(EmptyResult.None) {
    /**
     * Where the change described by this event was made.
     */
    var origin: EventOrigin = EventOrigin.Local
}
