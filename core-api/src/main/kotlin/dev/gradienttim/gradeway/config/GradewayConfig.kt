/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config

import com.akuleshov7.ktoml.annotations.TomlComments
import dev.gradienttim.gradeway.serializers.TomlAnySerializer
import kotlinx.serialization.Serializable

@Serializable
@Suppress("MaxLineLength")
data class GradewayConfig(
    var version: Int = LATEST_VERSION,
    val database: DatabaseConfig = DatabaseConfig(),
    val messaging: MessagingConfig = MessagingConfig(),
    val appearance: AppearanceConfig = AppearanceConfig(),
    val env: EnvConfig = EnvConfig(),
) {
    typealias Variables = Map<String, @Serializable(with = TomlAnySerializer::class) Any>

    @Serializable
    data class AppearanceConfig(
        val prefix: String = "<dark_gray>•</dark_gray> <gradient:#ed751f:#e89e1e>Gradeway</gradient> <dark_gray>›</dark_gray> ",
        val primaryColor: String = "#ed751f",
        val secondaryColor: String = "#e89e1e"
    )

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

    companion object {
        const val LATEST_VERSION: Int = 1
    }
}
