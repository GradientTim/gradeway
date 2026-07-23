/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config

import dev.gradienttim.gradeway.config.gradeway.*
import kotlinx.serialization.Serializable

@Serializable
data class GradewayConfig<TPlatformConfig>(
    var version: Int = LATEST_VERSION,
    val database: DatabaseConfig = DatabaseConfig(),
    val messaging: MessagingConfig = MessagingConfig(),
    val appearance: AppearanceConfig = AppearanceConfig(),
    val backup: BackupConfig = BackupConfig(),
    val env: EnvConfig = EnvConfig(),
    val platform: TPlatformConfig,
) {
    companion object {
        const val LATEST_VERSION: Int = 2
    }
}
