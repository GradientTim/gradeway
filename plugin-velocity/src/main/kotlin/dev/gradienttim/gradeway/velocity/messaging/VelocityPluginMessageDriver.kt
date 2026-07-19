/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.messaging

import com.velocitypowered.api.proxy.ProxyServer
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.messaging.MessagingAuthenticator
import dev.gradienttim.gradeway.messaging.MessagingBroker
import dev.gradienttim.gradeway.platform.Environment

class VelocityPluginMessageDriver(
    private val server: ProxyServer,
    private val plugin: Any,
) : Driver(), MessagingAdapter {
    override fun createMessagingBroker(environment: Environment): MessagingBroker {
        val sharedSecret = environment.stringRequired(MessagingAuthenticator.SHARED_SECRET_VARIABLE)
        return VelocityPluginMessageMessagingBroker(server, plugin, MessagingAuthenticator(sharedSecret))
    }
}
