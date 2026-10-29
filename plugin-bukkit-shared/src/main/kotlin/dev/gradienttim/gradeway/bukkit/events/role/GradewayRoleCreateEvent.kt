/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.events.role

import dev.gradienttim.gradeway.bukkit.event.CancellableEvent
import dev.gradienttim.gradeway.entity.role.RoleEntity
import org.bukkit.event.HandlerList

class GradewayRoleCreateEvent(
    val role: RoleEntity,
) : CancellableEvent() {
    override fun getHandlers(): HandlerList = HANDLER_LIST

    companion object {
        @JvmStatic
        val HANDLER_LIST = HandlerList()
    }
}
