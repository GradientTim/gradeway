/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungeecord

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.platform.CommonLogger
import net.md_5.bungee.api.plugin.Plugin

class GradewayPlugin : Plugin() {
    val gradeway = CommonGradeway(
        logger = CommonLogger.fromJavaLogger(logger),
        directory = dataFolder.toPath()
    )

    @Suppress("ForbiddenComment")
    override fun onEnable() {
        gradeway.load()
            .onLeft {
                logger.severe("Failed to load Gradeway: ${it.localizedMessage}")
            }
            .onRight {
                // TODO: REGISTER COMMANDS AND LISTENERS
            }
    }

    override fun onDisable() {
        gradeway.unload()
    }
}
