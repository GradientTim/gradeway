/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.platform.Caches
import net.kyori.adventure.text.Component
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.EnumParser.enumParser

internal fun <C : Any> MutableCommandBuilder<C>.registerCacheCommand(
    gradeway: CommonGradeway<*>,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("cache") {
        permission("gradeway.cache")

        registerCopy("flush") {
            permission("gradeway.cache.flush")

            optional("type", enumParser(Caches.Type::class.java))

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val type = context.getOrDefault("type", Caches.Type.ALL)

                type.run(gradeway.caches)
                    .onLeft { throwable ->
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.cache.flush.failed",
                                Component.text(type.name),
                                Component.text(throwable.message ?: throwable::class.java.simpleName)
                            )
                        )
                    }
                    .onRight {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.cache.flush.success",
                                Component.text(type.name)
                            )
                        )
                    }
            }
        }
    }
}
