/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config.gradeway

import com.akuleshov7.ktoml.annotations.TomlComments
import kotlinx.serialization.Serializable

@Serializable
data class SweepConfig(
    @TomlComments(
        "Defines how often, in seconds, Gradeway scans for and removes expired player roles.",
        "Lower values shrink the window where an expired role's permissions can still apply to an already-cached online player.",
        "Clamped to a minimum of 60 seconds regardless of the configured value."
    )
    val expiredRoleSweepIntervalSeconds: Long = 60,
)
