/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config.gradeway

import com.akuleshov7.ktoml.annotations.TomlComments
import kotlinx.serialization.Serializable

@Serializable
data class AppearanceConfig(
    @TomlComments(
        "Defines the prefix that is prepended to messages sent by Gradeway.",
        "Supports MiniMessage formatting."
    )
    val prefix: String = "<gradient:#ed751f:#e89e1e>Gradeway</gradient> <dark_gray>›</dark_gray> ",

    @TomlComments(
        "Defines the primary color used throughout Gradeway messages.",
        "Must be a hex color code."
    )
    val primaryColor: String = "#ed751f",

    @TomlComments(
        "Defines the secondary color used throughout Gradeway messages.",
        "Must be a hex color code."
    )
    val secondaryColor: String = "#e89e1e"
)
