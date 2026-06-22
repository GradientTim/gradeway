/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.services.PermissionService.SetPermissionError
import dev.gradienttim.gradeway.services.PermissionService.UnsetPermissionError
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun <TSource> gradewayCommandBuilder(
    gradeway: CommonGradeway,
    literal: String,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
): LiteralArgumentBuilder<TSource> {
    return command(literal) {
        roleBuilder(gradeway, hasPermission, sourceToAudience)
        playerBuilder(gradeway, hasPermission, sourceToAudience)

        execute {
            val audience = sourceToAudience(source)
            audience.sendMessage(Component.translatable("gradeway.commands.usage.version"))
            if (hasPermission(source, "commands.gradeway.usage")) {
                audience.sendMessage(Component.translatable("gradeway.commands.usage.help"))
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.roleBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("role") {
        string("idOrName") {
            suggestsDebounced { builder ->
                val remaining = builder.remaining
                if (remaining.isNotEmpty()) {
                    suggestRoles(builder, gradeway, remaining)
                }
            }

            roleAttributesBuilder(gradeway, hasPermission, sourceToAudience)
            rolePermissionsBuilder(gradeway, hasPermission, sourceToAudience)

            execute {
                val audience = sourceToAudience(source)

                val (idOrName, entity) = roleEntityParam("idOrName", gradeway)
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
}

@Suppress("UnusedParameter", "LongMethod")
internal fun <TSource> ArgumentBuilder<TSource, *>.roleAttributesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("attributes") {
        requires { hasPermission(it, "commands.gradeway.role.attributes") }

        literal("set") {
            requires { hasPermission(it, "commands.gradeway.role.attributes.set") }

            string("key") {
                literal("string") {
                    string("value") {
                        execute {}
                    }
                }
                literal("char") {
                    string("value") {
                        execute {}
                    }
                }
                literal("boolean") {
                    boolean("value") {
                        execute {}
                    }
                }
                literal("integer") {
                    integer("value") {
                        execute {}
                    }
                }
                literal("long") {
                    long("value") {
                        execute {}
                    }
                }
                literal("double") {
                    double("value") {
                        execute {}
                    }
                }
                literal("float") {
                    float("value") {
                        execute {}
                    }
                }
                literal("short") {
                    integer("value") {
                        execute {}
                    }
                }
                literal("byte") {
                    integer("value") {
                        execute {}
                    }
                }
                literal("uuid") {
                    string("value") {
                        execute {}
                    }
                }
                literal("instant") {
                    long("value") {
                        execute {}
                    }
                }
                literal("duration") {
                    long("value") {
                        execute {}
                    }
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "commands.gradeway.role.attributes.remove") }

            string("key") {
                execute {}
            }
        }

        literal("list") {
            requires { hasPermission(it, "commands.gradeway.role.attributes.list") }
            execute {}
        }
    }
}

@Suppress("UnusedParameter", "LongMethod")
internal fun <TSource> ArgumentBuilder<TSource, *>.rolePermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("permissions") {
        requires { hasPermission(it, "commands.gradeway.role.permissions") }

        literal("set") {
            requires { hasPermission(it, "commands.gradeway.role.permissions.set") }

            string("permission") {
                boolean("status") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val permission = stringParam("permission")
                        val status = booleanParam("status")

                        gradeway.roles.setPermission(idOrName, permission, status)
                            .onLeft { error ->
                                if (error is SetPermissionError.EntityNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.role.setPermission.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
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
                                if (error is SetPermissionError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.role.setPermission.unexpectedError",
                                            Component.text(idOrName),
                                            Component.text(permission),
                                            Component.text(error.throwable.localizedMessage)
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

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    gradeway.players.setPermission(idOrName, permission, true)
                        .onLeft { error ->
                            if (error is SetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.role.setPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
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
                            if (error is SetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.role.setPermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permission),
                                        Component.text(error.throwable.localizedMessage)
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
        }

        literal("unset") {
            requires { hasPermission(it, "commands.gradeway.role.permissions.unset") }

            string("permission") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    gradeway.roles.unsetPermission(idOrName, permission)
                        .onLeft { error ->
                            if (error is UnsetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.role.unsetPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.PermissionNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.role.setPermission.permissionNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.role.unsetPermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permission),
                                        Component.text(error.throwable.localizedMessage)
                                    ),
                                )
                                return@execute
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
            requires { hasPermission(it, "commands.gradeway.role.permissions.clear") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                gradeway.roles.clearPermissions(idOrName)
                    .onLeft { error ->
                        if (error is PermissionService.ClearPermissionError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.commands.role.clearPermission.entityNotFound",
                                    Component.text(idOrName)
                                ),
                            )
                            return@execute
                        }
                        if (error is PermissionService.ClearPermissionError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.commands.role.clearPermissions.unexpectedError",
                                    Component.text(idOrName),
                                    Component.text(error.throwable.localizedMessage)
                                ),
                            )
                            return@execute
                        }
                    }
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
            requires { hasPermission(it, "commands.gradeway.role.permissions.list") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                val entity = gradeway.roles.findByIdOrName(idOrName)
                if (entity == null) {
                    audience.sendMessage(
                        Component.translatable(
                            "gradeway.commands.role.notNound",
                            Component.text(idOrName)
                        )
                    )
                    return@execute
                }
            }
        }
    }
}

internal fun <TSource> ArgumentBuilder<TSource, *>.playerBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("player") {
        requires { hasPermission(it, "commands.gradeway.player") }

        string("idOrName") {
            suggestsDebounced { builder ->
                val remaining = builder.remaining
                if (remaining.isNotEmpty()) {
                    suggestPlayers(builder, gradeway, remaining)
                }
            }

            playerRolesBuilder(gradeway, hasPermission, sourceToAudience)
            playerAttributesBuilder(gradeway, hasPermission, sourceToAudience)
            playerPermissionsBuilder(gradeway, hasPermission, sourceToAudience)

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                val entity = gradeway.players.findByIdOrName(idOrName)
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
}

@Suppress("UnusedParameter")
internal fun <TSource> ArgumentBuilder<TSource, *>.playerRolesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("roles") {
        requires { hasPermission(it, "commands.gradeway.player.roles") }

        literal("add") {
            requires { hasPermission(it, "commands.gradeway.player.roles.add") }

            string("id") {
                execute {}
            }
        }

        literal("remove") {
            requires { hasPermission(it, "commands.gradeway.player.roles.remove") }

            string("id") {
                execute {}
            }
        }

        literal("list") {
            requires { hasPermission(it, "commands.gradeway.player.roles.list") }
            execute {}
        }

        literal("primary") {
            literal("set") {}
            execute {}
        }
    }
}

@Suppress("UnusedParameter", "LongMethod")
internal fun <TSource> ArgumentBuilder<TSource, *>.playerAttributesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("attributes") {
        requires { hasPermission(it, "commands.gradeway.player.attributes") }

        literal("set") {
            requires { hasPermission(it, "commands.gradeway.player.attributes.set") }

            string("key") {
                literal("string") {
                    string("value") {
                        execute {}
                    }
                }
                literal("char") {
                    string("value") {
                        execute {}
                    }
                }
                literal("boolean") {
                    boolean("value") {
                        execute {}
                    }
                }
                literal("integer") {
                    integer("value") {
                        execute {}
                    }
                }
                literal("long") {
                    long("value") {
                        execute {}
                    }
                }
                literal("double") {
                    double("value") {
                        execute {}
                    }
                }
                literal("float") {
                    float("value") {
                        execute {}
                    }
                }
                literal("short") {
                    integer("value") {
                        execute {}
                    }
                }
                literal("byte") {
                    integer("value") {
                        execute {}
                    }
                }
                literal("uuid") {
                    string("value") {
                        execute {}
                    }
                }
                literal("instant") {
                    long("value") {
                        execute {}
                    }
                }
                literal("duration") {
                    long("value") {
                        execute {}
                    }
                }
            }
        }

        literal("remove") {
            requires { hasPermission(it, "commands.gradeway.player.attributes.remove") }

            string("key") {
                execute {}
            }
        }

        literal("list") {
            requires { hasPermission(it, "commands.gradeway.player.attributes.list") }
            execute {}
        }
    }
}

@Suppress("UnusedParameter", "LongMethod")
internal fun <TSource> ArgumentBuilder<TSource, *>.playerPermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("permissions") {
        requires { hasPermission(it, "commands.gradeway.player.permissions") }

        literal("set") {
            requires { hasPermission(it, "commands.gradeway.player.permissions.set") }

            string("permission") {
                boolean("status") {
                    execute {
                        val audience = sourceToAudience(source)

                        val idOrName = stringParam("idOrName")
                        val permission = stringParam("permission")
                        val status = booleanParam("status")

                        gradeway.players.setPermission(idOrName, permission, status)
                            .onLeft { error ->
                                if (error is SetPermissionError.EntityNotFound) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.player.setPermission.entityNotFound",
                                            Component.text(idOrName),
                                            Component.text(permission)
                                        ),
                                    )
                                    return@execute
                                }
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
                                if (error is SetPermissionError.Unexpected) {
                                    audience.sendMessage(
                                        Component.translatable(
                                            "gradeway.commands.player.setPermission.unexpectedError",
                                            Component.text(idOrName),
                                            Component.text(permission),
                                            Component.text(error.throwable.localizedMessage)
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

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    gradeway.players.setPermission(idOrName, permission, true)
                        .onLeft { error ->
                            if (error is SetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.player.setPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
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
                            if (error is SetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.player.setPermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permission),
                                        Component.text(error.throwable.localizedMessage)
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
        }

        literal("unset") {
            requires { hasPermission(it, "commands.gradeway.player.permissions.unset") }

            string("permission") {
                execute {
                    val audience = sourceToAudience(source)

                    val idOrName = stringParam("idOrName")
                    val permission = stringParam("permission")

                    gradeway.players.unsetPermission(idOrName, permission)
                        .onLeft { error ->
                            if (error is UnsetPermissionError.EntityNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.player.unsetPermission.entityNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.PermissionNotFound) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.player.setPermission.permissionNotFound",
                                        Component.text(idOrName),
                                        Component.text(permission)
                                    ),
                                )
                                return@execute
                            }
                            if (error is UnsetPermissionError.Unexpected) {
                                audience.sendMessage(
                                    Component.translatable(
                                        "gradeway.commands.player.unsetPermission.unexpectedError",
                                        Component.text(idOrName),
                                        Component.text(permission),
                                        Component.text(error.throwable.localizedMessage)
                                    ),
                                )
                                return@execute
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
            requires { hasPermission(it, "commands.gradeway.player.permissions.clear") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                gradeway.players.clearPermissions(idOrName)
                    .onLeft { error ->
                        if (error is PermissionService.ClearPermissionError.EntityNotFound) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.commands.player.clearPermission.entityNotFound",
                                    Component.text(idOrName),
                                ),
                            )
                            return@execute
                        }
                        if (error is PermissionService.ClearPermissionError.Unexpected) {
                            audience.sendMessage(
                                Component.translatable(
                                    "gradeway.commands.player.clearPermissions.unexpectedError",
                                    Component.text(idOrName),
                                    Component.text(error.throwable.localizedMessage)
                                ),
                            )
                            return@execute
                        }
                    }
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
            requires { hasPermission(it, "commands.gradeway.player.permissions.list") }

            execute {
                val audience = sourceToAudience(source)

                val idOrName = stringParam("idOrName")

                val entity = gradeway.players.findByIdOrName(idOrName)
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
}

internal fun suggestPlayers(builder: SuggestionsBuilder, gradeway: CommonGradeway, remaining: String) {
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
        builder.suggest(entity.id.toString(), LiteralMessage(entity.name))
    }
}

internal fun suggestRoles(builder: SuggestionsBuilder, gradeway: CommonGradeway, remaining: String) {
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
        builder.suggest(entity.id.toString(), LiteralMessage(entity.name))
    }
}
