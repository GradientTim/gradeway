/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config.gradeway

import com.akuleshov7.ktoml.annotations.TomlComments
import kotlinx.serialization.Serializable

@Serializable
data class EnvConfig(
    @TomlComments(
        "Specifies where Gradeway should find the .env file containing the credentials.",
        "By default, the file is located in the same directory as this configuration file.",
        "If the path is changed, make sure that Gradeway or the application itself has access to this file so it can read its contents."
    )
    val file: String = "./.env",

    @TomlComments(
        "If this option is enabled, Gradeway will attempt to read environment variables from the configured .env file above.",
        "Variables found in the .env file will override Variables defined in the Gradeway configuration."
    )
    val readFromFile: Boolean = true,

    @TomlComments(
        "If this option is enabled, Gradeway will attempt to read environment variables from global system environment variables.",
    )
    val readFromSystem: Boolean = false,

    @TomlComments(
        "If this option is enabled, Gradeway will attempt to read environment variables from the running application that defines properties via the '-Dkey=value' flag.",
    )
    val readFromProperties: Boolean = false,
)
