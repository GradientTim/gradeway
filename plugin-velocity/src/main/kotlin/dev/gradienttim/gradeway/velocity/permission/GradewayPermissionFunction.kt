/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.permission

import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.Tristate
import dev.gradienttim.gradeway.CommonGradeway
import java.util.*

class GradewayPermissionFunction(val gradeway: CommonGradeway<*>, val playerId: UUID) : PermissionFunction {
    override fun getPermissionValue(permission: String): Tristate {
        gradeway.knownPermissions.record(permission)
        return Tristate.fromBoolean(gradeway.permissions.hasEffectivePlayerPermission(playerId, permission))
    }
}
