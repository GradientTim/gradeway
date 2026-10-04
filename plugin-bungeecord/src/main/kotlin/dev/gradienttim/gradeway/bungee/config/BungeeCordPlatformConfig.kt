/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.config

import dev.gradienttim.gradeway.configs.PlatformConfig
import kotlinx.serialization.Serializable

@Serializable
data class BungeeCordPlatformConfig(
    override var version: Int = PlatformConfig.LATEST_VERSION,
) : PlatformConfig
