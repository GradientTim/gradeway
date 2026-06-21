/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.gradienttim.gradeway.Gradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.PermissionService.SetPermissionError
import dev.gradienttim.gradeway.services.PermissionService.UnsetPermissionError
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select

@Suppress("CyclomaticComplexMethod", "LongMethod", "ForbiddenComment")
// TODO: SPLIT COMMANDS INTO DIFFERENT FUNCTIONS
fun <TSource> gradewayCommandBuilder(
    gradeway: Gradeway,
    literal: String,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
): LiteralArgumentBuilder<TSource> {
    return command(literal) {
        literal("player") {
            string("idOrName") {
                suggests { _, builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        val entities = PlayersTable
                            .select(PlayersTable.id, PlayersTable.name)
                            .where {
                                (PlayersTable.id likeAsStr "%$remaining") or (PlayersTable.name like "%$remaining")
                            }
                            .limit(10)
                            .map { row ->
                                object {
                                    val id = row[PlayersTable.id].value
                                    val name = row[PlayersTable.name]
                                }
                            }

                        entities.forEach { entity ->
                            builder.suggest(entity.id.toString(), LiteralMessage(entity.name))
                        }
                    }
                    return@suggests builder.buildFuture()
                }

                literal("permission") {
                    literal("set") {
                        string("permission") {
                            boolean("status") {
                                execute {
                                    val audience = sourceToAudience(source)

                                    val status = booleanParam("status")
                                    val permission = stringParam("permission")
                                    val (idOrName, entity) = playerEntityParam("idOrName", gradeway)
                                    if (entity == null) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.commands.player.notNound",
                                                Component.text(idOrName)
                                            )
                                        )
                                        return@execute
                                    }

                                    gradeway.players.setPermission(entity, permission, status)
                                        .onLeft { error ->
                                            if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                                audience.sendMessage(
                                                    Component.translatable(
                                                        "gradeway.commands.player.setPermission.alreadyEnabled",
                                                        Component.text(idOrName),
                                                        Component.text(permission)
                                                    ),
                                                )
                                                return@execute
                                            }
                                            if (error is SetPermissionError.PermissionAlreadyDisabled) {
                                                audience.sendMessage(
                                                    Component.translatable(
                                                        "gradeway.commands.player.setPermission.alreadyDisabled",
                                                        Component.text(idOrName),
                                                        Component.text(permission)
                                                    ),
                                                )
                                                return@execute
                                            }
                                        }
                                        .onRight {
                                            audience.sendMessage(
                                                Component.translatable(
                                                    "gradeway.commands.player.setPermission.success",
                                                    Component.text(idOrName),
                                                    Component.text(permission)
                                                ),
                                            )
                                        }
                                }
                            }

                            execute {
                                val audience = sourceToAudience(source)

                                val permission = stringParam("permission")
                                val (idOrName, entity) = playerEntityParam("idOrName", gradeway)
                                if (entity == null) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.player.notNound",
                                            Component.text(idOrName)
                                        )
                                    )
                                    return@execute
                                }

                                gradeway.players.setPermission(entity, permission, true)
                                    .onLeft { error ->
                                        if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                            audience.sendMessage(
                                                Component.translatable(
                                                    "gradeway.commands.player.setPermission.alreadyEnabled",
                                                    Component.text(idOrName),
                                                    Component.text(permission)
                                                ),
                                            )
                                        }
                                    }
                                    .onRight {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.commands.player.setPermission.success",
                                                Component.text(idOrName),
                                                Component.text(permission)
                                            ),
                                        )
                                    }
                            }
                        }
                    }

                    literal("unset") {
                        string("permission") {
                            execute {
                                val audience = sourceToAudience(source)

                                val permission = stringParam("permission")
                                val (idOrName, entity) = playerEntityParam("idOrName", gradeway)
                                if (entity == null) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.player.notNound",
                                            Component.text(idOrName)
                                        )
                                    )
                                    return@execute
                                }

                                gradeway.players.unsetPermission(entity, permission)
                                    .onLeft { error ->
                                        if (error is UnsetPermissionError.PermissionNotFound) {
                                            audience.sendMessage(
                                                Component.translatable(
                                                    "gradeway.commands.player.setPermission.permissionNotFound",
                                                    Component.text(idOrName),
                                                    Component.text(permission)
                                                ),
                                            )
                                        }
                                    }
                                    .onRight {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.commands.player.unsetPermission.success",
                                                Component.text(idOrName),
                                                Component.text(permission)
                                            ),
                                        )
                                    }
                            }
                        }
                    }

                    literal("clear") {
                        execute {
                            val audience = sourceToAudience(source)

                            val (idOrName, entity) = playerEntityParam("idOrName", gradeway)
                            if (entity == null) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.player.notNound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@execute
                            }

                            gradeway.players.clearPermissions(entity)
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.player.clearPermissions.success",
                                            Component.text(idOrName),
                                        ),
                                    )
                                }
                        }
                    }

                    literal("list") {
                        execute {
                            val audience = sourceToAudience(source)

                            val (idOrName, entity) = playerEntityParam("idOrName", gradeway)
                            if (entity == null) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.player.notNound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@execute
                            }
                        }
                    }

                    execute {
                        // TODO: PROVIDE HELP
                    }
                }

                literal("attribute") {
                    literal("add") {}

                    literal("update") {}

                    literal("remove") {}

                    literal("list") {}

                    execute {
                        // TODO: PROVIDE HELP
                    }
                }

                literal("roles") {
                    literal("add") {}
                    literal("remove") {}
                    literal("list") {}

                    literal("primary") {
                        literal("set") {}
                    }

                    execute {
                        // TODO: PROVIDE HELP
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val (idOrName, entity) = playerEntityParam("idOrName", gradeway)
                    if (entity == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.commands.player.notNound",
                                Component.text(idOrName)
                            )
                        )
                        return@execute
                    }
                }
            }
        }

        literal("role") {
            string("idOrName") {
                suggests { _, builder ->
                    val remaining = builder.remaining
                    if (remaining.isNotEmpty()) {
                        val entities = RolesTable
                            .select(RolesTable.id, RolesTable.name)
                            .where {
                                (RolesTable.id likeAsStr "%$remaining") or (RolesTable.name like "%$remaining")
                            }
                            .limit(10)
                            .map { row ->
                                object {
                                    val id = row[RolesTable.id].value
                                    val name = row[RolesTable.name]
                                }
                            }

                        entities.forEach { entity ->
                            builder.suggest(entity.id.toString(), LiteralMessage(entity.name))
                        }
                    }
                    return@suggests builder.buildFuture()
                }

                literal("permission") {
                    literal("set") {
                        string("permission") {
                            boolean("status") {
                                execute {
                                    val audience = sourceToAudience(source)

                                    val status = booleanParam("status")
                                    val permission = stringParam("permission")
                                    val (idOrName, entity) = roleEntityParam("idOrName", gradeway)
                                    if (entity == null) {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.commands.role.notNound",
                                                Component.text(idOrName)
                                            )
                                        )
                                        return@execute
                                    }

                                    gradeway.roles.setPermission(entity, permission, status)
                                        .onLeft { error ->
                                            if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                                audience.sendMessage(
                                                    Component.translatable(
                                                        "gradeway.commands.role.setPermission.alreadyEnabled",
                                                        Component.text(idOrName),
                                                        Component.text(permission)
                                                    ),
                                                )
                                                return@execute
                                            }
                                            if (error is SetPermissionError.PermissionAlreadyDisabled) {
                                                audience.sendMessage(
                                                    Component.translatable(
                                                        "gradeway.commands.role.setPermission.alreadyDisabled",
                                                        Component.text(idOrName),
                                                        Component.text(permission)
                                                    ),
                                                )
                                                return@execute
                                            }
                                        }
                                        .onRight {
                                            audience.sendMessage(
                                                Component.translatable(
                                                    "gradeway.commands.role.setPermission.success",
                                                    Component.text(idOrName),
                                                    Component.text(permission)
                                                ),
                                            )
                                        }
                                }
                            }

                            execute {
                                val audience = sourceToAudience(source)

                                val permission = stringParam("permission")
                                val (idOrName, entity) = roleEntityParam("idOrName", gradeway)
                                if (entity == null) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.role.notNound",
                                            Component.text(idOrName)
                                        )
                                    )
                                    return@execute
                                }

                                gradeway.roles.setPermission(entity, permission, true)
                                    .onLeft { error ->
                                        if (error is SetPermissionError.PermissionAlreadyEnabled) {
                                            audience.sendMessage(
                                                Component.translatable(
                                                    "gradeway.commands.role.setPermission.alreadyEnabled",
                                                    Component.text(idOrName),
                                                    Component.text(permission)
                                                ),
                                            )
                                        }
                                    }
                                    .onRight {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.commands.role.setPermission.success",
                                                Component.text(idOrName),
                                                Component.text(permission)
                                            ),
                                        )
                                    }
                            }
                        }
                    }

                    literal("unset") {
                        string("permission") {
                            execute {
                                val audience = sourceToAudience(source)

                                val permission = stringParam("permission")
                                val (idOrName, entity) = roleEntityParam("idOrName", gradeway)
                                if (entity == null) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.role.notNound",
                                            Component.text(idOrName)
                                        )
                                    )
                                    return@execute
                                }

                                gradeway.roles.unsetPermission(entity, permission)
                                    .onLeft { error ->
                                        if (error is UnsetPermissionError.PermissionNotFound) {
                                            audience.sendMessage(
                                                Component.translatable(
                                                    "gradeway.commands.role.setPermission.permissionNotFound",
                                                    Component.text(idOrName),
                                                    Component.text(permission)
                                                ),
                                            )
                                        }
                                    }
                                    .onRight {
                                        audience.sendMessage(
                                            Component.translatable(
                                                "gradeway.commands.role.unsetPermission.success",
                                                Component.text(idOrName),
                                                Component.text(permission)
                                            ),
                                        )
                                    }
                            }
                        }
                    }

                    literal("clear") {
                        execute {
                            val audience = sourceToAudience(source)

                            val (idOrName, entity) = roleEntityParam("idOrName", gradeway)
                            if (entity == null) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.role.notNound",
                                        Component.text(idOrName)
                                    )
                                )
                                return@execute
                            }

                            gradeway.roles.clearPermissions(entity)
                                .onRight {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.role.clearPermissions.success",
                                            Component.text(idOrName),
                                        ),
                                    )
                                }
                        }
                    }

                    literal("list") {
                        execute {

                        }
                    }
                }

                literal("attribute") {
                    literal("add") {}

                    literal("update") {}

                    literal("remove") {}

                    literal("list") {}

                    execute {
                        // TODO: PROVIDE HELP
                    }
                }

                execute {
                    val audience = sourceToAudience(source)

                    val (idOrName, entity) = playerEntityParam("idOrName", gradeway)
                    if (entity == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.commands.player.notNound",
                                Component.text(idOrName)
                            )
                        )
                        return@execute
                    }
                }
            }
        }

        execute {
            val audience = sourceToAudience(source)
            audience.sendMessage(Component.translatable("gradeway.commands.usage.version"))
            if (hasPermission(source, "commands.gradeway.usage")) {
                audience.sendMessage(Component.translatable("gradeway.commands.usage.help"))
            }
        }
    }
}
