/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.configs

import com.akuleshov7.ktoml.annotations.TomlComments
import kotlinx.serialization.Serializable

@Serializable
data class GradewayConfig(
    override var version: Int = LATEST_VERSION,

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
    val secondaryColor: String = "#e89e1e",

    @TomlComments(
        "Defines the maximum decompressed size, in bytes, allowed when importing a backup or migration file.",
        "Protects against decompression-bomb archives that expand to consume excessive memory once decompressed.",
        "Increase this if your server has a very large dataset and legitimate imports are being rejected."
    )
    @Suppress("MagicNumber")
    val maxImportSizeBytes: Long = 2L * 1024 * 1024 * 1024,

    @TomlComments(
        "Controls when players automatically receive the default role.",
        "The default role is selected with '/gradeway role default set <role>'."
    )
    val defaultRole: DefaultRoleConfig = DefaultRoleConfig(),
) : BaseConfig {
    @Serializable
    data class DefaultRoleConfig(
        @TomlComments(
            "Assigns the default role to players when they join for the first time",
            "and also makes it their primary role."
        )
        val assignOnFirstJoin: Boolean = true,

        @TomlComments(
            "Assigns the default role on every join to players who have no primary role.",
            "This also covers existing players, but re-applies the default role after",
            "their primary role has been cleared."
        )
        val assignWhenNoPrimaryRole: Boolean = false,
    )

    companion object {
        const val LATEST_VERSION: Int = 2
    }
}
