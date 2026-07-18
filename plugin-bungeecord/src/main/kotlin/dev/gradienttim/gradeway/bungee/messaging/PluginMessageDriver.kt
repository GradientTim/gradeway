/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.messaging

import dev.gradienttim.gradeway.bungee.GradewayPlugin
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.messaging.MessagingBroker
import dev.gradienttim.gradeway.platform.Environment

class PluginMessageDriver(val plugin: GradewayPlugin) : Driver(), MessagingAdapter {
    override fun createMessagingBroker(environment: Environment): MessagingBroker =
        PluginMessageMessagingBroker(plugin)
}
