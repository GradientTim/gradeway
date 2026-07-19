/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.messaging

import dev.gradienttim.gradeway.bungee.GradewayPlugin
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.messaging.MessagingAuthenticator
import dev.gradienttim.gradeway.messaging.MessagingBroker
import dev.gradienttim.gradeway.platform.Environment

class PluginMessageDriver(val plugin: GradewayPlugin) : Driver(), MessagingAdapter {
    override fun createMessagingBroker(environment: Environment): MessagingBroker {
        val sharedSecret = environment.stringRequired(MessagingAuthenticator.SHARED_SECRET_VARIABLE)
        return PluginMessageMessagingBroker(plugin, MessagingAuthenticator(sharedSecret))
    }
}
