/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.listeners

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.permission.PermissionsSetupEvent
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.velocity.permission.GradewayPermissionProvider

class PermissionListener(val gradeway: CommonGradeway<*>) {
    @Subscribe(order = PostOrder.EARLY, priority = Short.MAX_VALUE)
    fun onPermissionSetup(event: PermissionsSetupEvent) {
        event.provider = GradewayPermissionProvider(gradeway)
    }
}
