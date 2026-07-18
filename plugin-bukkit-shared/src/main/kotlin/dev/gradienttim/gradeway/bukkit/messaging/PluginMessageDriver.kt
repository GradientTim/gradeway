/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.messaging

import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.messaging.MessagingBroker
import dev.gradienttim.gradeway.platform.Environment
import org.bukkit.plugin.java.JavaPlugin

class PluginMessageDriver(private val plugin: JavaPlugin) : Driver(), MessagingAdapter {
    override fun createMessagingBroker(environment: Environment): MessagingBroker =
        PluginMessageMessagingBroker(plugin)
}
