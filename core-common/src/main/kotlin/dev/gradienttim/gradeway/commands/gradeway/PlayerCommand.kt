/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.commands.extensions.*
import dev.gradienttim.gradeway.database.models.permission.PermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayerAttributesTable
import dev.gradienttim.gradeway.database.models.player.PlayerPermissionTemplatesTable
import dev.gradienttim.gradeway.database.models.player.PlayerPermissionsTable
import dev.gradienttim.gradeway.database.models.player.PlayerRolesTable
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.eqId
import dev.gradienttim.gradeway.extensions.formatUTC
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.extensions.toIdArgument
import dev.gradienttim.gradeway.services.PlayerService
import dev.gradienttim.gradeway.utilities.TimeParser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.*

internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.playerCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    literal("player") {
        requires { commandContext.hasPermission(it, "gradeway.player") }

        literal("create") {
            requires { commandContext.hasPermission(it, "gradeway.player.create") }

            string("id") {
                string("name") {
                    execute {
                        val id = stringParam("id")
                        val name = stringParam("name")

                        val uniqueId = runCatching { UUID.fromString(id) }.getOrNull()

                        if (uniqueId == null) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.create.invalidUuid",
                                    Argument.string("player", id)
                                )
                            )
                            return@execute
                        }

                        gradeway.players.create(uniqueId, name)
                            .onLeft { error ->
                                if (error is PlayerService.CreatePlayerError.EntityAlreadyExists) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.player.create.entityAlreadyExists",
                                            Argument.string("player", id)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is PlayerService.CreatePlayerError.InvalidName) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.player.create.invalidName",
                                            Argument.string("player", id)
                                        )
                                    )
                                    return@execute
                                }
                                if (error is PlayerService.CreatePlayerError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.player.create.unexpectedError",
                                            Argument.string("player", id),
                                            Argument.string("error", error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@execute
                                }
                            }
                            .onRight {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.create.success",
                                        Argument.string("player", id),
                                        Argument.string("name", name)
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("delete") {
            requires { commandContext.hasPermission(it, "gradeway.player.delete") }

            string("idOrName") {
                suggestPlayers(gradeway)

                execute {
                    val id = stringParam("idOrName")

                    val player = gradeway.players.findByIdOrName(id)
                    if (player == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.player.delete.entityNotFound",
                                Argument.string("player", id)
                            )
                        )
                        return@execute
                    }

                    requestConfirmation(source, commandContext, gradeway, rootLiteral) {
                        gradeway.players.delete(player.id.value)
                            .onLeft { error ->
                                if (error is PlayerService.DeletePlayerError.EntityNotFound) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.player.delete.entityNotFound",
                                            Argument.string("player", id)
                                        )
                                    )
                                    return@requestConfirmation
                                }
                                if (error is PlayerService.DeletePlayerError.Unexpected) {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.player.delete.unexpectedError",
                                            Argument.string("player", id),
                                            Argument.string("error", error.throwable.message ?: "Unknown")
                                        )
                                    )
                                    return@requestConfirmation
                                }
                            }
                            .onRight {
                                commandContext.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.delete.success",
                                        Argument.string("player", id)
                                    )
                                )
                            }
                    }
                }
            }
        }

        literal("info") {
            requires { commandContext.hasPermission(it, "gradeway.player.info") }

            string("idOrName") {
                suggestPlayers(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")

                    val player = gradeway.players.findByIdOrName(idOrName)
                    if (player == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.player.info.entityNotFound",
                                Argument.string("player", idOrName)
                            )
                        )
                        return@execute
                    }

                    val playerId = player.id.value
                    val primaryRole = player.primaryRoleId?.let { gradeway.roles.findById(it.value) }?.name

                    val details = transaction(gradeway.database) {
                        object {
                            val roles = PlayerRolesTable
                                .innerJoin(RolesTable, { roleId }, { id })
                                .select(RolesTable.name, PlayerRolesTable.untilAt, PlayerRolesTable.pausedAt)
                                .where { PlayerRolesTable.playerId eqId playerId }
                                .orderBy(RolesTable.weight to SortOrder.DESC)
                                .map { row ->
                                    val role = Argument.string("role", row[RolesTable.name])
                                    val until = row[PlayerRolesTable.untilAt]
                                    when {
                                        row[PlayerRolesTable.pausedAt] != null -> Component.translatable(
                                            "gradeway.command.player.info.rolePaused",
                                            role
                                        )

                                        until != null -> Component.translatable(
                                            "gradeway.command.player.info.roleTemporary",
                                            role,
                                            Argument.string("until", until.formatUTC())
                                        )

                                        else -> Component.translatable("gradeway.command.player.info.role", role)
                                    }
                                }
                            val permissions = PlayerPermissionsTable
                                .selectAll()
                                .where { PlayerPermissionsTable.playerId eqId playerId }
                                .count()
                            val templates = PlayerPermissionTemplatesTable
                                .selectAll()
                                .where { PlayerPermissionTemplatesTable.playerId eqId playerId }
                                .count()
                            val attributes = PlayerAttributesTable
                                .selectAll()
                                .where { PlayerAttributesTable.playerId eqId playerId }
                                .count()
                        }
                    }

                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.player.info",
                            playerId.toIdArgument(),
                            Argument.string("name", player.name),
                            Argument.component(
                                "primary_role",
                                primaryRole?.let { Component.text(it) }
                                    ?: Component.translatable("gradeway.command.info.noEntries")
                            ),
                            Argument.numeric("weight", player.weight),
                            *infoListArguments("roles", details.roles),
                            Argument.numeric("permissions", details.permissions),
                            Argument.numeric("templates", details.templates),
                            Argument.numeric("attributes", details.attributes),
                            Argument.string("created", player.createdAt.formatUTC()),
                            Argument.string("updated", player.updatedAt.formatUTC())
                        )
                    )
                }
            }
        }

        literal("modify") {
            string("idOrName") {
                suggestPlayers(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")

                    val entity = gradeway.players.findByIdOrName(idOrName)
                    if (entity == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.player.notFound",
                                Argument.string("player", idOrName)
                            )
                        )
                        return@execute
                    }
                }

                registerPlayerRolesCommand(gradeway, commandContext)

                registerEntityAttributeCommands(
                    rootLiteral = rootLiteral,
                    gradeway = gradeway,
                    entityType = "player",
                    context = commandContext,
                    handleAddAttribute = { idOrName, attribute -> gradeway.players.addAttribute(idOrName, attribute) },
                    handleUpdateAttribute = { idOrName, key, value ->
                        gradeway.players.updateAttribute(
                            idOrName,
                            key,
                            value
                        )
                    },
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
                                        (PlayersTable.name.lowerCase() like "${scope.lowercase()}%")
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
                    handleListRender = { source, page, limit, result ->
                        if (result.isEmpty()) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable("gradeway.command.player.listAttributes.empty")
                            )
                            return@registerEntityAttributeCommands
                        }

                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.player.listAttributes.header",
                                Argument.numeric("page", page),
                                Argument.numeric("limit", limit)
                            )
                        )

                        result.forEach { attributeEntity ->
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.listAttributes.entry",
                                    Argument.string("type", attributeEntity.type),
                                    Argument.string("attribute", attributeEntity.key.asString()),
                                    Argument.string("value", attributeEntity.value)
                                )
                            )
                        }
                    }
                )

                registerEntityPermissionCommands(
                    rootLiteral = rootLiteral,
                    gradeway = gradeway,
                    entityType = "player",
                    context = commandContext,
                    handleSetPermission = { idOrName, permission, status ->
                        gradeway.players.setPermission(idOrName, permission, status)
                    },
                    handleUnsetPermission = { idOrName, permission ->
                        gradeway.players.unsetPermission(
                            idOrName,
                            permission
                        )
                    },
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
                                        (PlayersTable.name.lowerCase() like "${scope.lowercase()}%")
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
                    handleListRender = { source, page, limit, result ->
                        if (result.isEmpty()) {
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable("gradeway.command.player.listPermissions.empty")
                            )
                            return@registerEntityPermissionCommands
                        }

                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.player.listPermissions.header",
                                Argument.numeric("page", page),
                                Argument.numeric("limit", limit)
                            )
                        )

                        result.forEach { permissionEntity ->
                            commandContext.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.listPermissions.entry",
                                    Argument.string("permission", permissionEntity.value),
                                    Argument.string("type", permissionEntity.type.name),
                                    Argument.bool("enabled", permissionEntity.isEnabled)
                                )
                            )
                        }
                    }
                )

                literal("setWeight") {
                    requires { commandContext.hasPermission(it, "gradeway.player.setWeight") }

                    integer("value") {
                        execute {
                            val idOrName = stringParam("idOrName")
                            val weight = intParam("value")

                            gradeway.players.setWeight(idOrName, weight)
                                .onLeft { error ->
                                    if (error is PlayerService.SetWeightError.EntityNotFound) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.player.setWeight.entityNotFound",
                                                Argument.string("player", idOrName),
                                            )
                                        )
                                        return@execute
                                    }
                                    if (error is PlayerService.SetWeightError.Unexpected) {
                                        commandContext.sendTranslatedMessage(
                                            source,
                                            Component.translatable(
                                                "gradeway.command.player.setWeight.unexpectedError",
                                                Argument.string("player", idOrName),
                                                Argument.string("error", error.throwable.message ?: "Unknown")
                                            )
                                        )
                                        return@execute
                                    }
                                }
                                .onRight {
                                    commandContext.sendTranslatedMessage(
                                        source,
                                        Component.translatable(
                                            "gradeway.command.player.setWeight.success",
                                            Argument.string("player", idOrName),
                                            Argument.numeric("weight", weight)
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }

        registerWeightedListCommand(
            gradeway = gradeway,
            permission = "gradeway.player.list",
            context = commandContext,
            query = { page, limit, order ->
                PlayersTable
                    .select(
                        PlayersTable.id,
                        PlayersTable.name,
                        PlayersTable.weight,
                        PlayersTable.createdAt,
                        PlayersTable.updatedAt
                    )
                    .orderBy(PlayersTable.weight to order, PlayersTable.name to SortOrder.ASC)
                    .limit(limit)
                    .offset(((page - 1) * limit).toLong())
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
            render = { source, page, limit, result ->
                if (result.isEmpty()) {
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable("gradeway.command.player.list.empty")
                    )
                    return@registerWeightedListCommand
                }

                commandContext.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.player.list.header",
                        Argument.numeric("page", page),
                        Argument.numeric("limit", limit)
                    )
                )

                result.forEach { player ->
                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.player.list.entry",
                            player.id.toIdArgument(),
                            Argument.string("name", player.name),
                            Argument.numeric("weight", player.weight),
                            Argument.string("created", player.createdAt.formatUTC()),
                            Argument.string("updated", player.updatedAt.formatUTC())
                        )
                    )
                }
            }
        )
    }
}

internal fun <TCommandContext> ArgumentBuilder<TCommandContext, *>.registerPlayerRolesCommand(
    gradeway: CommonGradeway<*>,
    context: CommandContext<TCommandContext>,
) {
    @Suppress("ReturnCount")
    fun handleAddRole(source: TCommandContext, playerIdOrName: String, roleId: String, until: Instant? = null) {
        val roleUniqueId = gradeway.roles.findByIdOrName(roleId)?.id?.value

        if (roleUniqueId == null) {
            context.sendTranslatedMessage(
                source,
                Component.translatable(
                    "gradeway.command.player.addRole.targetNotFound",
                    Argument.string("player", playerIdOrName),
                    Argument.string("role", roleId)
                )
            )
            return
        }

        gradeway.players.addRole(playerIdOrName, roleUniqueId, until)
            .onLeft { error ->
                if (error is PlayerService.AddRoleError.EntityNotFound) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.player.addRole.entityNotFound",
                            Argument.string("player", playerIdOrName)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.TargetNotFound) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.player.addRole.targetNotFound",
                            Argument.string("player", playerIdOrName),
                            Argument.string("role", roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.AlreadyExists) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.player.addRole.alreadyExists",
                            Argument.string("player", playerIdOrName),
                            Argument.string("role", roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.UntilInPast) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.player.addRole.untilInPast",
                            Argument.string("player", playerIdOrName),
                            Argument.string("role", roleId)
                        )
                    )
                    return
                }
                if (error is PlayerService.AddRoleError.Unexpected) {
                    context.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.player.addRole.unexpectedError",
                            Argument.string("player", playerIdOrName),
                            Argument.string("role", roleId),
                            Argument.string("error", error.throwable.message ?: "Unknown")
                        )
                    )
                    return
                }
            }
            .onRight {
                context.sendTranslatedMessage(
                    source,
                    Component.translatable(
                        "gradeway.command.player.addRole.success",
                        Argument.string("player", playerIdOrName),
                        Argument.string("role", roleId)
                    )
                )
            }
    }

    literal("roles") {
        literal("add") {
            requires { context.hasPermission(it, "gradeway.player.roles.add") }

            string("role") {
                suggestRoles(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("role")

                    handleAddRole(source, idOrName, roleId)
                }

                string("until") {
                    execute {
                        val idOrName = stringParam("idOrName")
                        val roleId = stringParam("role")
                        val until = stringParam("until")

                        val untilInstant = TimeParser.parseToInstant(until, gradeway.now())
                        if (untilInstant == null) {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.addRole.invalidTimeFormat",
                                    Argument.string("player", idOrName),
                                    Argument.string("until", until)
                                )
                            )
                            return@execute
                        }

                        handleAddRole(source, idOrName, roleId, untilInstant)
                    }
                }
            }
        }

        literal("remove") {
            requires { context.hasPermission(it, "gradeway.player.roles.remove") }

            string("role") {
                suggestRoles(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("role")

                    val roleUniqueId = gradeway.roles.findByIdOrName(roleId)?.id?.value

                    if (roleUniqueId == null) {
                        context.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.player.removeRole.targetNotFound",
                                Argument.string("player", idOrName),
                                Argument.string("role", roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.players.removeRole(idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (error is PlayerService.RemoveRoleError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.entityNotFound",
                                        Argument.string("player", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.TargetNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.targetNotFound",
                                        Argument.string("player", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.NotExists) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.notExists",
                                        Argument.string("player", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.RemoveRoleError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.removeRole.unexpectedError",
                                        Argument.string("player", idOrName),
                                        Argument.string("role", roleId),
                                        Argument.string("error", error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.removeRole.success",
                                    Argument.string("player", idOrName),
                                    Argument.string("role", roleId)
                                )
                            )
                        }
                }
            }
        }

        literal("setPrimary") {
            requires { context.hasPermission(it, "gradeway.player.roles.setPrimary") }

            string("role") {
                suggestRoles(gradeway)

                execute {
                    val idOrName = stringParam("idOrName")
                    val roleId = stringParam("role")

                    val roleUniqueId = gradeway.roles.findByIdOrName(roleId)?.id?.value

                    if (roleUniqueId == null) {
                        context.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.player.setPrimaryRole.targetNotFound",
                                Argument.string("player", idOrName),
                                Argument.string("role", roleId)
                            )
                        )
                        return@execute
                    }

                    gradeway.players.setPrimaryRole(idOrName, roleUniqueId)
                        .onLeft { error ->
                            if (error is PlayerService.SetPrimaryRoleError.EntityNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.entityNotFound",
                                        Argument.string("player", idOrName)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.TargetNotFound) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.targetNotFound",
                                        Argument.string("player", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.AlreadyPrimary) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.alreadyPrimary",
                                        Argument.string("player", idOrName),
                                        Argument.string("role", roleId)
                                    )
                                )
                                return@execute
                            }
                            if (error is PlayerService.SetPrimaryRoleError.Unexpected) {
                                context.sendTranslatedMessage(
                                    source,
                                    Component.translatable(
                                        "gradeway.command.player.setPrimaryRole.unexpectedError",
                                        Argument.string("player", idOrName),
                                        Argument.string("role", roleId),
                                        Argument.string("error", error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@execute
                            }
                        }
                        .onRight {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.setPrimaryRole.success",
                                    Argument.string("player", idOrName),
                                    Argument.string("role", roleId)
                                )
                            )
                        }
                }
            }
        }

        literal("clearPrimary") {
            requires { context.hasPermission(it, "gradeway.player.roles.clearPrimary") }

            execute {
                val idOrName = stringParam("idOrName")

                gradeway.players.clearPrimaryRole(idOrName)
                    .onLeft { error ->
                        if (error is PlayerService.ClearPrimaryRoleError.EntityNotFound) {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.clearPrimaryRole.entityNotFound",
                                    Argument.string("player", idOrName)
                                )
                            )
                            return@execute
                        }
                        if (error is PlayerService.ClearPrimaryRoleError.NoPrimaryRole) {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.clearPrimaryRole.noPrimaryRole",
                                    Argument.string("player", idOrName)
                                )
                            )
                            return@execute
                        }
                        if (error is PlayerService.ClearPrimaryRoleError.Unexpected) {
                            context.sendTranslatedMessage(
                                source,
                                Component.translatable(
                                    "gradeway.command.player.clearPrimaryRole.unexpectedError",
                                    Argument.string("player", idOrName),
                                    Argument.string("error", error.throwable.message ?: "Unknown")
                                )
                            )
                            return@execute
                        }
                    }
                    .onRight {
                        context.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.player.clearPrimaryRole.success",
                                Argument.string("player", idOrName)
                            )
                        )
                    }
            }
        }
    }
}
