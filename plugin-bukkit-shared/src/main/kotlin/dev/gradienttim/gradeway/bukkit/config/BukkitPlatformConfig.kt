/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.config

import com.akuleshov7.ktoml.annotations.TomlComments
import dev.gradienttim.gradeway.configs.PlatformConfig
import kotlinx.serialization.Serializable

@Serializable
data class BukkitPlatformConfig(
    override var version: Int = PlatformConfig.LATEST_VERSION,

    @TomlComments(
        "When enabled, server operators will not have access to all permission protected commands."
    )
    val disableOp: Boolean = true,
) : PlatformConfig
