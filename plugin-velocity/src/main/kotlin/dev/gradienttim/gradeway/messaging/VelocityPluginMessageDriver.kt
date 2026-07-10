/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

import com.velocitypowered.api.proxy.ProxyServer
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.platform.Environment

class VelocityPluginMessageDriver(
    private val server: ProxyServer,
    private val plugin: Any,
) : Driver(), MessagingAdapter {
    override fun createMessagingBroker(environment: Environment): MessagingBroker =
        VelocityPluginMessageMessagingBroker(server, plugin)
}
