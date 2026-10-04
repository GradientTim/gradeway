/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.configs

import com.akuleshov7.ktoml.annotations.TomlComments
import kotlinx.serialization.Serializable

@Serializable
data class DriversConfig(
    override var version: Int = LATEST_VERSION,
    val database: DatabaseDriverConfig = DatabaseDriverConfig(),
    val messaging: MessagingDriverConfig = MessagingDriverConfig(),
) : BaseConfig {
    @Serializable
    data class DatabaseDriverConfig(
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
    )

    @Serializable
    data class MessagingDriverConfig(
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
    )

    companion object {
        const val LATEST_VERSION: Int = 1
    }
}
