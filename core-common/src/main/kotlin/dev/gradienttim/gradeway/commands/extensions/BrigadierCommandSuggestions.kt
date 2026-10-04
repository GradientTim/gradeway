/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.extensions

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import java.util.*

private const val MAX_SUGGESTIONS = 10

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestDynamic(
    block: (remaining: String) -> Map<String, String?>,
) = this.apply {
    suggests { _, builder ->
        block(builder.remainingLowerCase).forEach { (id, tooltip) ->
            if (id.startsWith(builder.remainingLowerCase)) {
                if (tooltip == null) {
                    builder.suggest(id)
                } else {
                    builder.suggest(id, LiteralMessage(tooltip))
                }
            }
        }
        builder.buildFuture()
    }
}

private fun suggestFromIndex(index: Map<UUID, String>, remaining: String): Map<String, String?> {
    val remainingLowercase = remaining.lowercase()
    var entries = index.entries.asSequence()
    if (remainingLowercase.isNotEmpty()) {
        entries = entries.filter { (id, name) ->
            id.toString().startsWith(remainingLowercase) || name.lowercase().startsWith(remainingLowercase)
        }
    }
    return entries
        .take(MAX_SUGGESTIONS)
        .associate { (id, name) -> id.toString() to name }
}

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestAttributeTypes() = suggestDynamic { remaining ->
    AttributeTypeRegistry.items
        .map { it.type.lowercase() }
        .filter { it.startsWith(remaining) }
        .associateWith { null }
}

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestPlayers(gradeway: CommonGradeway<*>) =
    suggestDynamic { remaining -> suggestFromIndex(gradeway.caches.suggestions.players, remaining) }

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestRoles(gradeway: CommonGradeway<*>) =
    suggestDynamic { remaining -> suggestFromIndex(gradeway.caches.suggestions.roles, remaining) }

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestGroups(gradeway: CommonGradeway<*>) =
    suggestDynamic { remaining -> suggestFromIndex(gradeway.caches.suggestions.groups, remaining) }

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestPermissions(gradeway: CommonGradeway<*>) =
    suggestDynamic { remaining -> suggestFromIndex(gradeway.caches.suggestions.permissions, remaining) }

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestPermissionTemplates(gradeway: CommonGradeway<*>) =
    suggestDynamic { remaining -> suggestFromIndex(gradeway.caches.suggestions.permissionTemplates, remaining) }

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestTracks(gradeway: CommonGradeway<*>) =
    suggestDynamic { remaining -> suggestFromIndex(gradeway.caches.suggestions.tracks, remaining) }

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestTrackStages(gradeway: CommonGradeway<*>) =
    suggestDynamic { remaining -> suggestFromIndex(gradeway.caches.suggestions.trackStages, remaining) }

internal fun <C, T> RequiredArgumentBuilder<C, T>.suggestStrings(values: () -> Collection<String>) =
    suggestDynamic { remaining ->
        values()
            .filter { it.lowercase().startsWith(remaining) }
            .associateWith { null }
    }
