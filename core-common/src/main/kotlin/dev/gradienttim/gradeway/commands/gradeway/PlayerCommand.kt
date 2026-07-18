/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import dev.gradienttim.gradeway.CommonGradeway
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
import org.incendo.cloud.kotlin.MutableCommandBuilder
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import java.time.Instant
import java.util.*

internal fun <C : Any> MutableCommandBuilder<C>.registerPlayerCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    registerCopy("player") {
        registerCopy("create") {
            permission("gradeway.player.create")

            required("id", stringParser())
            required("name", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val id = context.get<String>("id")
                val name = context.get<String>("name")

                val uniqueId = runCatching { UUID.fromString(id) }.getOrNull()

                if (uniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.create.invalidUuid",
                            Component.text(id)
                        )
                    )
                    return@handler
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
                            return@handler
                        }
                        if (error is PlayerService.CreatePlayerError.InvalidName) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.create.invalidName",
                                    Component.text(id)
                                )
                            )
                            return@handler
                        }
                        if (error is PlayerService.CreatePlayerError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.create.unexpectedError",
                                    Component.text(id),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
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

        registerCopy("delete") {
            permission("gradeway.player.delete")

            required("id", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestPlayers(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val id = context.get<String>("id")

                val uniqueId = runCatching { UUID.fromString(id) }.getOrNull()

                if (uniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.delete.invalidUuid",
                            Component.text(id)
                        )
                    )
                    return@handler
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
                            return@handler
                        }
                        if (error is PlayerService.DeletePlayerError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.delete.unexpectedError",
                                    Component.text(id),
                                    Component.text(error.throwable.message ?: "Unknown")
                                )
                            )
                            return@handler
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

        registerCopy("modify") {
            required("idOrName", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestPlayers(gradeway, remaining.lowercase()) }
            }

            registerPlayerRolesCommand(gradeway, audienceProvider)

            registerEntityAttributeCommands(
                gradeway = gradeway,
                entityType = "player",
                audienceProvider = audienceProvider,
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
                handleListRender = { audience, page, limit, result ->
                    if (result.isEmpty()) {
                        audience.sendMessage(
                            Component.translatable("gradeway.command.player.listAttributes.empty")
                        )
                        return@registerEntityAttributeCommands
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

            registerEntityPermissionCommands(
                gradeway = gradeway,
                entityType = "player",
                audienceProvider = audienceProvider,
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
                handleListRender = { audience, page, limit, result ->
                    if (result.isEmpty()) {
                        audience.sendMessage(
                            Component.translatable("gradeway.command.player.listPermissions.empty")
                        )
                        return@registerEntityPermissionCommands
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

            registerCopy("setWeight") {
                permission("gradeway.player.setWeight")

                required("value", integerParser())

                handler { context ->
                    val audience = audienceProvider.apply(context.sender())

                    val idOrName = context.get<String>("idOrName")
                    val weight = context.get<Int>("value")

                    gradeway.players.setWeight(idOrName, weight)
                        .onLeft { error ->
                            if (error is PlayerService.SetWeightError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setWeight.entityNotFound",
                                        Component.text(idOrName),
                                    )
                                )
                                return@handler
                            }
                            if (error is PlayerService.SetWeightError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.command.player.setWeight.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(error.throwable.message ?: "Unknown")
                                    )
                                )
                                return@handler
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

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")

                val entity = gradeway.players.findByIdOrName(idOrName)
                if (entity == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.notFound",
                            Component.text(idOrName)
                        )
                    )
                    return@handler
                }
            }
        }

        registerGlobalListCommand(
            gradeway = gradeway,
            permission = "gradeway.player.list",
            audienceProvider = audienceProvider,
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
                    return@registerGlobalListCommand
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

internal fun <C : Any> MutableCommandBuilder<C>.registerPlayerRolesCommand(
    gradeway: CommonGradeway,
    audienceProvider: AudienceProvider<C>,
) {
    @Suppress("ReturnCount")
    fun handleAddRole(audience: Audience, playerIdOrName: String, roleId: String, until: Instant? = null) {
        val roleUniqueId = runCatching { UUID.fromString(roleId) }.getOrNull()

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

    registerCopy("roles") {
        registerCopy("add") {
            permission("gradeway.player.roles.add")

            required("roleId", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }
            optional("until", stringParser())

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val roleId = context.get<String>("roleId")
                val until = context.optional<String>("until").orElse(null)

                if (until == null) {
                    handleAddRole(audience, idOrName, roleId)
                    return@handler
                }

                val untilInstant = TimeParser.parseToInstant(until, gradeway.now())
                if (untilInstant == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.addRole.invalidTimeFormat",
                            Component.text(idOrName),
                            Component.text(until)
                        )
                    )
                    return@handler
                }

                handleAddRole(audience, idOrName, roleId, untilInstant)
            }
        }

        registerCopy("remove") {
            permission("gradeway.player.roles.remove")

            required("roleId", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val roleId = context.get<String>("roleId")

                val roleUniqueId = runCatching { UUID.fromString(roleId) }.getOrNull()

                if (roleUniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.removeRole.invalidUuid",
                            Component.text(idOrName),
                            Component.text(roleId)
                        )
                    )
                    return@handler
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
                            return@handler
                        }
                        if (error is PlayerService.RemoveRoleError.TargetNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.removeRole.targetNotFound",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                            return@handler
                        }
                        if (error is PlayerService.RemoveRoleError.NotExists) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.removeRole.notExists",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                            return@handler
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
                            return@handler
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

        registerCopy("setPrimary") {
            permission("gradeway.player.roles.setPrimary")

            required("roleId", stringParser()) {
                suggestsDebounced(gradeway) { remaining -> suggestRoles(gradeway, remaining.lowercase()) }
            }

            handler { context ->
                val audience = audienceProvider.apply(context.sender())

                val idOrName = context.get<String>("idOrName")
                val roleId = context.get<String>("roleId")

                val roleUniqueId = runCatching { UUID.fromString(roleId) }.getOrNull()

                if (roleUniqueId == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.command.player.setPrimaryRole.invalidUuid",
                            Component.text(idOrName),
                            Component.text(roleId)
                        )
                    )
                    return@handler
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
                            return@handler
                        }
                        if (error is PlayerService.SetPrimaryRoleError.TargetNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.setPrimaryRole.targetNotFound",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                            return@handler
                        }
                        if (error is PlayerService.SetPrimaryRoleError.AlreadyPrimary) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.command.player.setPrimaryRole.alreadyPrimary",
                                    Component.text(idOrName),
                                    Component.text(roleId)
                                )
                            )
                            return@handler
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
                            return@handler
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
