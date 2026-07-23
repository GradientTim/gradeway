/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config.gradeway

import com.akuleshov7.ktoml.annotations.TomlComments
import kotlinx.serialization.Serializable

@Serializable
data class BackupConfig(
    @TomlComments(
        "Defines the maximum decompressed size, in bytes, allowed when importing a backup or migration file.",
        "Protects against decompression-bomb archives that expand to consume excessive memory once decompressed.",
        "Increase this if your server has a very large dataset and legitimate imports are being rejected."
    )
    val maxImportSizeBytes: Long = 2L * 1024 * 1024 * 1024,
)
