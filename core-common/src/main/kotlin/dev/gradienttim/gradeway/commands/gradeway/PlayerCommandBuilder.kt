/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayerAttributesTable
import dev.gradienttim.gradeway.database.models.player.PlayerPermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.extensions.formatUTC
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.PlayerService
import dev.gradienttim.gradeway.utilities.TimeParser
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import java.time.Instant
import java.util.*

internal fun <TSource> ArgumentBuilder<TSource, *>.playerBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("player") {
        requires { hasPermission(it, "gradeway.player") }

        literal("create") {
            requires { hasPermission(it, "gradeway.player.create") }

            string("id") {
                string("name") {
                    execute {
                        val audience = sourceToAudience(source)

                        val id = stringParam("id")
                        val name = stringParam("name")

                        val uniqueId = runCatching {
                            UUID.fromString(id)
                        }.getOrNull()

                        if (uniqueId == null) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.create.invalidUuid",
                                    Component.text(id)
                                )
                            )
                            return@execute
                        }

                        gradeway.players.create(uniqueId, name)
                            .onLeft { error ->
                                if (error is PlayerService.CreatePlayerError.EntityAlreadyExists) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.player.create.entityAlreadyExists",
                                            Component.text(id)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is PlayerService.CreatePlayerError.InvalidName) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.player.create.invalidName",
                                            Component.text(id)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is PlayerService.CreatePlayerError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.player.create.unexpectedError",
                                            Component.text(id),
                                            Component.text(error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@execute
                                }
                            }
                            .onRight {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.create.success",
                                        Component.text(id),
                                        Component.text(name)
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("delete") {
            requires { hasPermission(it, "gradeway.player.delete") }

            string("id") {
                suggestsDebounced(gradeway) { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestPlayers(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val id = stringParam("id")

                    val uniqueId = runCatching {
                        UUID.fromString(id)
                    }.getOrNull()

                    if (uniqueId == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.player.delete.invalidUuid",
                                Component.text(id)
                            )
                        )
                        return@execute
                    }

                    gradeway.players.delete(uniqueId)
                        .onLeft { error ->
                            if (error is PlayerService.DeletePlayerError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.delete.entityNotFound",
                                        Component.text(id)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.DeletePlayerError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.delete.unexpectedError",
                                        Component.text(id),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.delete.success",
                                    Component.text(id)
                                )
                            )
                        }
                }
            }
        }

        literal("modify") {
            string("idOrName") {
                suggestsDebounced(gradeway) { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestPlayers(gradeway, remaining.lowercase())
                    }
                }

                playerRolesBuilder(gradeway, hasPermission, sourceToAudience)
                playerAttributesBuilder(gradeway, hasPermission, sourceToAudience)
                playerPermissionsBuilder(gradeway, hasPermission, sourceToAudience)

                literal("setWeight") {
                    requires { hasPermission(it, "gradeway.player.setWeight") }

                    integer("value") {
                        execute {
                            val audience = sourceToAudience(source)

                            val idOrName = stringParam("idOrName")
                            val weight = intParam("value")

                            gradeway.players.setWeight(idOrName, weight)
                                .onLeft { error ->
                                    if (error is PlayerService.SetWeightError.EntityNotFound) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.player.setWeight.entityNotFound",
                                                Component.text(idOrName),
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is PlayerService.SetWeightError.Unexpected) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.command.player.setWeight.unexpectedError",
                                                Component.text(idOrName),
                                                Component.text(error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.command.player.setWeight.success",
                                            Component.text(idOrName),
                                            Component.text(weight)
                                        )
                                    )
                                }
                        }
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")

                    val entity = gradeway.players.findByIdOrName(idOrName)
                    if (entity == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.player.notFound",
                                Component.text(idOrName)
                            )
                        )
                        return@execute
                    }
                }
            }
        }

        createGlobalListHandler(
            gradeway = gradeway,
            permission = "gradeway.player.list",
            hasPermission = hasPermission,
            sourceToAudience = sourceToAudience,
            query = { page, limit ->
                PlayersTable
                    .select(
                        PlayersTable.id,
                        PlayersTable.name,
                        PlayersTable.weight,
                        PlayersTable.createdAt,
                        PlayersTable.updatedAt
                    )
                    .limit(limit)
                    .offset((page - 1).toLong())
                    .map { row ->
                        object {
                            val id = row[PlayersTable.id].value
                            val name = row[PlayersTable.name]
                            val weight = row[PlayersTable.weight]
                            val createdAt = row[PlayersTable.createdAt]
                            val updatedAt = row[PlayersTable.updatedAt]
                        }
                    }
            },
            render = { audience, page, limit, result ->
                if (result.isEmpty()) {
                    audience.sendMessage(Component.translatable("gradeway.command.player.list.empty"))
                    return@createGlobalListHandler
                }

                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.player.list.header",
                        Component.text(page),
                        Component.text(limit)
                    )
                )

                result.forEach { player ->
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.list.entry",
                            Component.text(player.id.toString()),
                            Component.text(player.name),
                            Component.text(player.weight),
                            Component.text(player.createdAt.formatUTC()),
                            Component.text(player.updatedAt.formatUTC())
                        )
                    )
                }
            }
        )
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.playerRolesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    @Suppress("ReturnCount")
    fun handleAddRole(audience: Audience, playerIdOrName: String, roleId: String, until: Instant? = null) {
        val roleUniqueId = runCatching {
            UUID.fromString(roleId)
        }.getOrNull()

        if (roleUniqueId == null) {
            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.player.addRole.invalidUuid",
                    Component.text(playerIdOrName),
                    Component.text(roleId)
                )
            )
            return
        }

        gradeway.players.addRole(playerIdOrName, roleUniqueId, until)
            .onLeft { error ->
                if (error is PlayerService.AddRoleError.EntityNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.entityNotFound",
                            Component.text(playerIdOrName)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.TargetNotFound) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.targetNotFound",
                            Component.text(playerIdOrName),
                            Component.text(roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.AlreadyExists) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.alreadyExists",
                            Component.text(playerIdOrName),
                            Component.text(roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.UntilInPast) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.untilInPast",
                            Component.text(playerIdOrName),
                            Component.text(roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.Unexpected) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.unexpectedError",
                            Component.text(playerIdOrName),
                            Component.text(roleId),
                            Component.text(error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
            }
            .onRight {
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.player.addRole.success",
                        Component.text(playerIdOrName),
                        Component.text(roleId)
                    )
                )
            }
    }

    literal("roles") {
        requires { hasPermission(it, "gradeway.player.roles") }

        literal("add") {
            requires { hasPermission(it, "gradeway.player.roles.add") }

            string("roleId") {
                suggestsDebounced(gradeway) { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                string("until") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val roleId = stringParam("roleId")
                        val until = stringParam("until")

                        val untilInstant = TimeParser.parseToInstant(until, gradeway.now())
                        if (untilInstant == null) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.addRole.invalidTimeFormat",
                                    Component.text(idOrName),
                                    Component.text(until)
                                )
                            )
                            return@execute
                        }

                        handleAddRole(audience, idOrName, roleId, untilInstant)
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("roleId")

                    handleAddRole(audience, idOrName, roleId)
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "gradeway.player.roles.remove") }

            string("roleId") {
                suggestsDebounced(gradeway) { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("roleId")

                    val roleUniqueId = runCatching {
                        UUID.fromString(roleId)
                    }.getOrNull()

                    if (roleUniqueId == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.player.removeRole.invalidUuid",
                                Component.text(idOrName),
                                Component.text(roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.players.removeRole(idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (error is PlayerService.RemoveRoleError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.TargetNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.targetNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.NotExists) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.notExists",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(roleId),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.removeRole.success",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                        }
                }
            }
        }

        literal("setPrimary") {
            requires { hasPermission(it, "gradeway.player.roles.setPrimary") }

            string("roleId") {
                suggestsDebounced(gradeway) { builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        builder.suggestRoles(gradeway, remaining.lowercase())
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("roleId")

                    val roleUniqueId = runCatching {
                        UUID.fromString(roleId)
                    }.getOrNull()

                    if (roleUniqueId == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.player.setPrimaryRole.invalidUuid",
                                Component.text(idOrName),
                                Component.text(roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.players.setPrimaryRole(idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (error is PlayerService.SetPrimaryRoleError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.entityNotFound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.TargetNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.targetNotFound",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.AlreadyPrimary) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.alreadyPrimary",
                                        Component.text(idOrName),
                                        Component.text(roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(roleId),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.setPrimaryRole.success",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                        }
                }
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.playerAttributesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    createEntityAttributeHandler(
        gradeway = gradeway,
        entityType = "player",
        hasPermission = hasPermission,
        sourceToAudience = sourceToAudience,
        handleAddAttribute = { idOrName, attribute -> gradeway.players.addAttribute(idOrName, attribute) },
        handleUpdateAttribute = { idOrName, key, value -> gradeway.players.updateAttribute(idOrName, key, value) },
        handleRemoveAttribute = { idOrName, key -> gradeway.players.removeAttribute(idOrName, key) },
        handleClearAttributes = { idOrName -> gradeway.players.clearAttributes(idOrName) },
        handleListQuery = { scope, page, limit ->
            PlayerAttributesTable
                .innerJoin(PlayersTable, { playerId }, { id })
                .select(
                    PlayerAttributesTable.type,
                    PlayerAttributesTable.key,
                    PlayerAttributesTable.value
                )
                .where {
                    (PlayersTable.id likeAsStr "$scope%") or
                            (PlayersTable.name.lowerCase() like "${scope.toString().lowercase()}%")
                }
                .limit(limit)
                .offset((page - 1).toLong())
                .map { row ->
                    object {
                        val type = row[PlayerAttributesTable.type]
                        val key = row[PlayerAttributesTable.key]
                        val value = row[PlayerAttributesTable.value]
                    }
                }
        },
        handleListRender = { audience, page, limit, result ->
            if (result.isEmpty()) {
                audience.sendMessage(
                    Component.translatable("gradeway.command.player.listAttributes.empty")
                )
                return@createEntityAttributeHandler
            }

            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.player.listAttributes.header",
                    Component.text(page),
                    Component.text(limit)
                )
            )

            result.forEach { attributeEntity ->
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.player.listAttributes.entry",
                        Component.text(attributeEntity.type),
                        Component.text(attributeEntity.key.asString()),
                        Component.text(attributeEntity.value)
                    )
                )
            }
        }
    )
}

internal fun <TSource> ArgumentBuilder<TSource, *>.playerPermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    createEntityPermissionHandler(
        gradeway = gradeway,
        entityType = "player",
        hasPermission = hasPermission,
        sourceToAudience = sourceToAudience,
        handleSetPermission = { idOrName, permission, status ->
            gradeway.players.setPermission(idOrName, permission, status)
        },
        handleUnsetPermission = { idOrName, permission -> gradeway.players.unsetPermission(idOrName, permission) },
        handleClearPermissions = { idOrName -> gradeway.players.clearPermissions(idOrName) },
        handleLinkTemplate = { idOrName, templateIdOrName ->
            gradeway.permissions.linkTemplateToPlayer(templateIdOrName, idOrName)
        },
        handleUnlinkTemplate = { idOrName, templateIdOrName ->
            gradeway.permissions.unlinkTemplateFromPlayer(templateIdOrName, idOrName)
        },
        handleApplyTemplate = { idOrName, templateIdOrName ->
            gradeway.permissions.applyTemplateToPlayer(templateIdOrName, idOrName)
        },
        handleRevokeTemplate = { idOrName, templateIdOrName ->
            gradeway.permissions.revokeTemplateFromPlayer(templateIdOrName, idOrName)
        },
        handleListQuery = { scope, page, limit ->
            PlayerPermissionsTable
                .innerJoin(PlayersTable, { playerId }, { id })
                .innerJoin(PermissionsTable, { PlayerPermissionsTable.permissionId }, { id })
                .select(PermissionsTable.value, PermissionsTable.type, PlayerPermissionsTable.isEnabled)
                .where {
                    (PlayersTable.id likeAsStr "$scope%") or
                            (PlayersTable.name.lowerCase() like "${scope.toString().lowercase()}%")
                }
                .limit(limit)
                .offset((page - 1).toLong())
                .map { row ->
                    object {
                        val value = row[PermissionsTable.value]
                        val type = row[PermissionsTable.type]
                        val isEnabled = row[PlayerPermissionsTable.isEnabled]
                    }
                }
        },
        handleListRender = { audience, page, limit, result ->
            if (result.isEmpty()) {
                audience.sendMessage(
                    Component.translatable("gradeway.command.player.listPermissions.empty")
                )
                return@createEntityPermissionHandler
            }

            audience.sendMessage(
                Component.translatable(
                    "gradeway.command.player.listPermissions.header",
                    Component.text(page),
                    Component.text(limit)
                )
            )

            result.forEach { permissionEntity ->
                audience.sendMessage(
                    Component.translatable(
                        "gradeway.command.player.listPermissions.entry",
                        Component.text(permissionEntity.value),
                        Component.text(permissionEntity.type.name),
                        Component.text(permissionEntity.isEnabled)
                    )
                )
            }
        }
    )
}
