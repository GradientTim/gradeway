/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.config.GradewayConfig
import dev.gradienttim.gradeway.utilities.Loadable

interface ConfigManager : Loadable {
    val config: GradewayConfig
}
