/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.source.decodeFromStream
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.config.GradewayConfig
import dev.gradienttim.gradeway.constants.TableConstants
import kotlinx.serialization.encodeToString
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import java.io.File

class CommonConfigManager<TPlatformConfig>(val gradeway: CommonGradeway<TPlatformConfig>) :
    ConfigManager<TPlatformConfig> {
    override var config: GradewayConfig<TPlatformConfig> = GradewayConfig(platform = gradeway.defaultPlatformConfig)

    override fun load(): Either<Throwable, Unit> = either {
        try {
            val configFile = File(gradeway.directory, "config.toml")
            if (configFile.exists()) {
                configFile.inputStream().use {
                    config = Toml.decodeFromStream<GradewayConfig<TPlatformConfig>>(it)
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

            initializeMiniMessage()
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    private fun initializeMiniMessage() {
        val appearance = config.appearance

        gradeway.miniMessage = MiniMessage.builder()
            .editTags { builder ->
                builder.tag("prefix", Tag.inserting(DEFAULT_MINIMESSAGE.deserialize(appearance.prefix)))
                builder.tag("primary", Tag.styling {
                    it.color(TextColor.fromHexString(appearance.primaryColor) ?: NamedTextColor.WHITE)
                })
                builder.tag("secondary", Tag.styling {
                    it.color(TextColor.fromHexString(appearance.secondaryColor) ?: NamedTextColor.WHITE)
                })
            }
            .build()
    }

    companion object {
        val DEFAULT_MINIMESSAGE: MiniMessage = MiniMessage.miniMessage()
    }
}
