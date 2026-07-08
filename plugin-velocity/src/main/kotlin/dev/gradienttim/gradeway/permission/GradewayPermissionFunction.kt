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
        return Tristate.fromBoolean(gradeway.permissions.hasEffectivePlayerPermission(playerId, permission))
    }
}
