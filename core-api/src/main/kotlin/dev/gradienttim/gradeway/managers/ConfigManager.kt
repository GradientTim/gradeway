/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.config.GradewayConfig

interface ConfigManager {
    val config: GradewayConfig

    fun load()
}
