/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.group

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before a group is deleted.
 */
class GroupDeleteEvent(
    val actor: Actor,
    val group: GroupEntity,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
