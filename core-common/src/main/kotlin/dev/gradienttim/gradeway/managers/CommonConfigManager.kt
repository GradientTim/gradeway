/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlOutputConfig
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.configs.BaseConfig
import dev.gradienttim.gradeway.configs.DriversConfig
import dev.gradienttim.gradeway.configs.GradewayConfig
import dev.gradienttim.gradeway.configs.PlatformConfig
import dev.gradienttim.gradeway.constants.TableConstants
import kotlinx.serialization.KSerializer
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import java.nio.file.Files

class CommonConfigManager<TPlatformConfig : PlatformConfig>(
    val gradeway: CommonGradeway<TPlatformConfig>
) : ConfigManager<TPlatformConfig> {
    override var gradewayEntry: ConfigManager.ConfigEntry<GradewayConfig> = CommonConfigEntry(
        gradeway = gradeway,
        fileName = "config.toml",
        config = GradewayConfig(),
        serializer = GradewayConfig.serializer(),
        receiveLatestVersion = { GradewayConfig.LATEST_VERSION }
    )

    override var driversEntry: ConfigManager.ConfigEntry<DriversConfig> = CommonConfigEntry(
        gradeway = gradeway,
        fileName = "drivers.toml",
        config = DriversConfig(),
        serializer = DriversConfig.serializer(),
        receiveLatestVersion = { DriversConfig.LATEST_VERSION }
    )

    override var platformEntry: ConfigManager.ConfigEntry<TPlatformConfig> = CommonConfigEntry(
        gradeway = gradeway,
        fileName = "platform.toml",
        config = gradeway.defaultPlatformConfig,
        serializer = gradeway.platformConfigSerializer,
        receiveLatestVersion = { PlatformConfig.LATEST_VERSION }
    )

    override fun load(): Either<Throwable, Unit> = either {
        gradewayEntry.load().bind()
        driversEntry.load().bind()
        platformEntry.load().bind()

        TableConstants.TABLE_PREFIX = driversEntry.config.database.prefix

        initializeMiniMessage()
    }

    private fun initializeMiniMessage() {
        gradeway.miniMessage = MiniMessage.builder()
            .editTags { builder ->
                builder.tag("prefix", Tag.inserting(DEFAULT_MINIMESSAGE.deserialize(gradewayEntry.config.prefix)))
                builder.tag("primary", Tag.styling {
                    it.color(TextColor.fromHexString(gradewayEntry.config.primaryColor) ?: NamedTextColor.WHITE)
                })
                builder.tag("secondary", Tag.styling {
                    it.color(TextColor.fromHexString(gradewayEntry.config.secondaryColor) ?: NamedTextColor.WHITE)
                })
            }
            .build()
    }

    class CommonConfigEntry<TConfig : BaseConfig>(
        gradeway: CommonGradeway<*>,
        fileName: String,
        override var config: TConfig,
        override val serializer: KSerializer<TConfig>,
        private val receiveLatestVersion: () -> Int,
    ) : ConfigManager.ConfigEntry<TConfig> {
        private var filePath = gradeway.directory.resolve(fileName)

        override fun load(): Either<Throwable, Unit> = either {
            if (!Files.exists(filePath)) {
                save().bind()
            }

            try {
                config = TOML.decodeFromString(serializer, Files.readString(filePath))

                val latestVersion = receiveLatestVersion()
                if (config.version < latestVersion) {
                    config.version = latestVersion
                    save().bind()
                }
            } catch (throwable: Throwable) {
                raise(throwable)
            }
        }

        override fun save(): Either<Throwable, Unit> = either {
            try {
                Files.writeString(filePath, TOML.encodeToString(serializer, config))
            } catch (throwable: Throwable) {
                raise(throwable)
            }
        }
    }

    companion object {
        val DEFAULT_MINIMESSAGE: MiniMessage = MiniMessage.miniMessage()
        val TOML = Toml(
            outputConfig = TomlOutputConfig(
                indentation = TomlIndentation.TWO_SPACES,
                ignoreNullValues = false,
                ignoreDefaultValues = false,
            )
        )
    }
}
