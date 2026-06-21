/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.permission

import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.Tristate
import dev.gradienttim.gradeway.Gradeway
import java.util.UUID

class GradewayPermissionFunction(val gradeway: Gradeway, val playerId: UUID) : PermissionFunction {
    override fun getPermissionValue(permission: String): Tristate {
        val permissions = gradeway.players.getPermissions(playerId)
        val status = permissions[permission] ?: return Tristate.UNDEFINED
        return Tristate.fromBoolean(status)
    }
}
