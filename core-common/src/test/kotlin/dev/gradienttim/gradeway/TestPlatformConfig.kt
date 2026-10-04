/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway

import dev.gradienttim.gradeway.configs.PlatformConfig
import kotlinx.serialization.Serializable

@Serializable
data class TestPlatformConfig(
    override var version: Int = PlatformConfig.LATEST_VERSION,
) : PlatformConfig
