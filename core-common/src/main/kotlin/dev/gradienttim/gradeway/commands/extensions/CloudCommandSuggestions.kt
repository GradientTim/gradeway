/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.extensions

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds

fun <C, T> CommandComponent.Builder<C, T>.suggestsDebounced(
    gradeway: CommonGradeway,
    delayMillis: Long = 250,
    block: suspend (remaining: String) -> Iterable<Suggestion>
): CommandComponent.Builder<C, T> {
    var searchJob: Job? = null

    return suggestionProvider(SuggestionProvider { _: CommandContext<C>, input ->
        searchJob?.cancel()

        val future = CompletableFuture<Iterable<Suggestion>>()

        searchJob = gradeway.backgroundScope.launch {
            try {
                delay(delayMillis.milliseconds)
                future.complete(block(input.remainingInput()))
            } catch (_: Throwable) {
                future.complete(emptyList())
            }
        }

        future
    })
}

internal fun suggestAttributeTypes(remaining: String): List<Suggestion> {
    var itemKeys = AttributeTypeRegistry.items.map { it.type.lowercase() }

    if (remaining.isNotEmpty()) {
        itemKeys = itemKeys.filter { it.startsWith(remaining) }
    }

    return itemKeys.map { Suggestion.suggestion(it) }
}

internal fun suggestPlayers(gradeway: CommonGradeway, remaining: String): List<Suggestion> {
    return transaction(gradeway.database) {
        PlayersTable
            .select(PlayersTable.id, PlayersTable.name)
            .where {
                (PlayersTable.id likeAsStr "$remaining%") or
                        (PlayersTable.name.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row -> Suggestion.suggestion(row[PlayersTable.id].value.toString()) }
    }
}

internal fun suggestRoles(gradeway: CommonGradeway, remaining: String): List<Suggestion> {
    return transaction(gradeway.database) {
        RolesTable
            .select(RolesTable.id, RolesTable.name)
            .where {
                (RolesTable.id likeAsStr "$remaining%") or
                        (RolesTable.name.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row -> Suggestion.suggestion(row[RolesTable.id].value.toString()) }
    }
}

internal fun suggestGroups(gradeway: CommonGradeway, remaining: String): List<Suggestion> {
    return transaction(gradeway.database) {
        GroupsTable
            .select(GroupsTable.id, GroupsTable.name)
            .where {
                (GroupsTable.id likeAsStr "$remaining%") or
                        (GroupsTable.name.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row -> Suggestion.suggestion(row[GroupsTable.id].value.toString()) }
    }
}

internal fun suggestPermissions(gradeway: CommonGradeway, remaining: String): List<Suggestion> {
    return transaction(gradeway.database) {
        PermissionsTable
            .select(PermissionsTable.id, PermissionsTable.value)
            .where {
                (PermissionsTable.id likeAsStr "$remaining%") or
                        (PermissionsTable.value.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row -> Suggestion.suggestion(row[PermissionsTable.id].value.toString()) }
    }
}

internal fun suggestPermissionTemplates(gradeway: CommonGradeway, remaining: String): List<Suggestion> {
    return transaction(gradeway.database) {
        PermissionTemplatesTable
            .select(PermissionTemplatesTable.id, PermissionTemplatesTable.name)
            .where {
                (PermissionTemplatesTable.id likeAsStr "$remaining%") or
                        (PermissionTemplatesTable.name.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row -> Suggestion.suggestion(row[PermissionTemplatesTable.id].value.toString()) }
    }
}
