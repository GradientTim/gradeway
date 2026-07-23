/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.config

import com.akuleshov7.ktoml.annotations.TomlComments
import kotlinx.serialization.Serializable

@Serializable
data class BukkitPlatformConfig(
    @TomlComments(
        "When enabled, server operators will not have access to all permission protected commands."
    )
    val disableOp: Boolean = true
)
