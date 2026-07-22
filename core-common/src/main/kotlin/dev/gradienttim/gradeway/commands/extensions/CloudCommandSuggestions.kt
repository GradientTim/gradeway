/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.extensions

import com.mojang.brigadier.LiteralMessage
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.*

private const val MAX_SUGGESTIONS = 10

fun <C, T> CommandComponent.Builder<C, T>.suggests(
    block: (remaining: String) -> Iterable<Suggestion>
): CommandComponent.Builder<C, T> {
    return suggestionProvider(SuggestionProvider.blocking { _: CommandContext<C>, input ->
        block(input.remainingInput())
    })
}

private fun suggestFromIndex(index: Map<UUID, String>, remaining: String): List<Suggestion> {
    val remainingLowercase = remaining.lowercase()
    var entries = index.entries.asSequence()
    if (remainingLowercase.isNotEmpty()) {
        entries = entries.filter { (id, name) ->
            id.toString().startsWith(remainingLowercase) || name.lowercase().startsWith(remainingLowercase)
        }
    }
    return entries
        .take(MAX_SUGGESTIONS)
        .map { (id, name) -> TooltipSuggestion.suggestion(id.toString(), LiteralMessage(name)) }
        .toList()
}

internal fun suggestAttributeTypes(remaining: String): List<Suggestion> {
    var itemKeys = AttributeTypeRegistry.items.map { it.type.lowercase() }

    if (remaining.isNotEmpty()) {
        itemKeys = itemKeys.filter { it.startsWith(remaining) }
    }

    return itemKeys.map { Suggestion.suggestion(it) }
}

internal fun suggestPlayers(gradeway: CommonGradeway, remaining: String): List<Suggestion> =
    suggestFromIndex(gradeway.caches.suggestions.players, remaining)

internal fun suggestRoles(gradeway: CommonGradeway, remaining: String): List<Suggestion> =
    suggestFromIndex(gradeway.caches.suggestions.roles, remaining)

internal fun suggestGroups(gradeway: CommonGradeway, remaining: String): List<Suggestion> =
    suggestFromIndex(gradeway.caches.suggestions.groups, remaining)

internal fun suggestPermissions(gradeway: CommonGradeway, remaining: String): List<Suggestion> =
    suggestFromIndex(gradeway.caches.suggestions.permissions, remaining)

internal fun suggestPermissionTemplates(gradeway: CommonGradeway, remaining: String): List<Suggestion> =
    suggestFromIndex(gradeway.caches.suggestions.permissionTemplates, remaining)
