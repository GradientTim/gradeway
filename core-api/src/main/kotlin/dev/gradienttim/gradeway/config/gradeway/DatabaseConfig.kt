/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config.gradeway

import com.akuleshov7.ktoml.annotations.TomlComments
import dev.gradienttim.gradeway.config.Variables
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    @TomlComments(
        "Defines the database type that Gradeway should use.",
        "Use the 'id' of the installed database driver."
    )
    var driver: String = "postgres",

    @TomlComments(
        "Defines the prefix for all Gradeway database tables.",
        "Set the value to an empty string to not use a prefix."
    )
    val prefix: String = "gradeway_",

    @TomlComments(
        "Defines variables for the database connection of the driver.",
        "Each available driver should expose its available environment variables you can use here.",
        "When a variable in the driver is not set and has not a default fallback, an error will be thrown.",
        "You can decide of defining the variables here or inside the .env file / current application."
    )
    val variables: Variables = emptyMap()
)
