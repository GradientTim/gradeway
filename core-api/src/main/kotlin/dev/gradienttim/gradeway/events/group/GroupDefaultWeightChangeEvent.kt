/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.events.group

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.event.Event

/**
 * Fired before the default weight of a group changes.
 */
class GroupDefaultWeightChangeEvent(
    val actor: Actor,
    val group: GroupEntity,
    val defaultWeight: Int,
) : Event<Event.CancelledResult>(CancelledResult.Allow)
