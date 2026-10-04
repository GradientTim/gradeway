/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.config

import dev.gradienttim.gradeway.configs.PlatformConfig
import kotlinx.serialization.Serializable

@Serializable
data class VelocityPlatformConfig(
    override var version: Int = PlatformConfig.LATEST_VERSION,
) : PlatformConfig
