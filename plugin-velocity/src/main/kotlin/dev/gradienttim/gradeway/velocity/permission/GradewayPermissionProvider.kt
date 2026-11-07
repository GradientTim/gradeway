/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.permission

import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.PermissionProvider
import com.velocitypowered.api.permission.PermissionSubject
import com.velocitypowered.api.proxy.Player
import dev.gradienttim.gradeway.CommonGradeway

class GradewayPermissionProvider(val gradeway: CommonGradeway<*>) : PermissionProvider {
    override fun createFunction(subject: PermissionSubject): PermissionFunction {
        if (subject is Player) {
            return GradewayPermissionFunction(gradeway, subject.uniqueId)
        }
        return PermissionFunction.ALWAYS_UNDEFINED
    }
}
