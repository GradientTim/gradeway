/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.extensions

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.database.models.group.GroupsTable
import dev.gradienttim.gradeway.database.models.permission.PermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal fun SuggestionsBuilder.suggestPlayers(gradeway: CommonGradeway, remaining: String) {
    val entities = transaction(gradeway.database) {
        PlayersTable
            .select(PlayersTable.id, PlayersTable.name)
            .where {
                (PlayersTable.id likeAsStr "$remaining%") or
                        (PlayersTable.name.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row ->
                object {
                    val id = row[PlayersTable.id].value
                    val name = row[PlayersTable.name]
                }
            }
    }

    entities.forEach { entity ->
        suggest(entity.id.toString(), LiteralMessage(entity.name))
    }
}

internal fun SuggestionsBuilder.suggestRoles(gradeway: CommonGradeway, remaining: String) {
    val entities = transaction(gradeway.database) {
        RolesTable
            .select(RolesTable.id, RolesTable.name)
            .where {
                (RolesTable.id likeAsStr "$remaining%") or
                        (RolesTable.name.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row ->
                object {
                    val id = row[RolesTable.id].value
                    val name = row[RolesTable.name]
                }
            }
    }

    entities.forEach { entity ->
        suggest(entity.id.toString(), LiteralMessage(entity.name))
    }
}

internal fun SuggestionsBuilder.suggestGroups(gradeway: CommonGradeway, remaining: String) {
    val entities = transaction(gradeway.database) {
        GroupsTable
            .select(GroupsTable.id, GroupsTable.name)
            .where {
                (GroupsTable.id likeAsStr "$remaining%") or
                        (GroupsTable.name.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row ->
                object {
                    val id = row[GroupsTable.id].value
                    val name = row[GroupsTable.name]
                }
            }
    }

    entities.forEach { entity ->
        suggest(entity.id.toString(), LiteralMessage(entity.name))
    }
}

internal fun SuggestionsBuilder.suggestPermissions(gradeway: CommonGradeway, remaining: String) {
    val entities = transaction(gradeway.database) {
        PermissionsTable
            .select(PermissionsTable.id, PermissionsTable.value)
            .where {
                (PermissionsTable.id likeAsStr "$remaining%") or
                        (PermissionsTable.value.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row ->
                object {
                    val id = row[PermissionsTable.id].value
                    val value = row[PermissionsTable.value]
                }
            }
    }

    entities.forEach { entity ->
        suggest(entity.id.toString(), LiteralMessage(entity.value))
    }
}

internal fun SuggestionsBuilder.suggestPermissionTemplates(gradeway: CommonGradeway, remaining: String) {
    val entities = transaction(gradeway.database) {
        PermissionTemplatesTable
            .select(PermissionTemplatesTable.id, PermissionTemplatesTable.name)
            .where {
                (PermissionTemplatesTable.id likeAsStr "$remaining%") or
                        (PermissionTemplatesTable.name.lowerCase() like "$remaining%")
            }
            .limit(10)
            .map { row ->
                object {
                    val id = row[PermissionTemplatesTable.id].value
                    val name = row[PermissionTemplatesTable.name]
                }
            }
    }

    entities.forEach { entity ->
        suggest(entity.id.toString(), LiteralMessage(entity.name))
    }
}
