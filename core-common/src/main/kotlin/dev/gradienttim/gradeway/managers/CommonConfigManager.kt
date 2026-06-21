/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.source.decodeFromStream
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.config.GradewayConfig
import dev.gradienttim.gradeway.constants.TableConstants
import kotlinx.serialization.encodeToString

class CommonConfigManager(val gradeway: CommonGradeway) : ConfigManager {
    override var config: GradewayConfig = GradewayConfig()

    override fun load() {
        val configFile = gradeway.directory.resolve("config.toml").toFile()
        if (configFile.exists()) {
            configFile.inputStream().use {
                config = Toml.decodeFromStream<GradewayConfig>(it)
            }
        } else {
            configFile.createNewFile()
            configFile.writeText(Toml.encodeToString(config))
        }

        TableConstants.TABLE_PREFIX = config.database.prefix

        if (config.version < GradewayConfig.LATEST_VERSION) {
            config.version = GradewayConfig.LATEST_VERSION
            configFile.writeText(Toml.encodeToString(config))
        }
    }
}
