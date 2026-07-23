/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config.gradeway

import com.akuleshov7.ktoml.annotations.TomlComments
import dev.gradienttim.gradeway.config.Variables
import kotlinx.serialization.Serializable

@Serializable
data class MessagingConfig(
    @TomlComments(
        "Controls whether messaging should be enabled or disabled.",
        "If messaging is disabled, data can no longer be synchronized in real time."
    )
    val enabled: Boolean = false,

    @TomlComments(
        "Defines the type of messaging service.",
        "Use the 'id' of the installed messaging driver."
    )
    val driver: String = "",

    @TomlComments(
        "Defines variables for the messaging connection of the driver.",
        "Each available driver should expose its available environment variables you can use here.",
        "When a variable in the driver is not set and has not a default fallback, an error will be thrown.",
        "You can decide of defining the variables here or inside the .env file / current application."
    )
    val variables: Variables = emptyMap()
)
