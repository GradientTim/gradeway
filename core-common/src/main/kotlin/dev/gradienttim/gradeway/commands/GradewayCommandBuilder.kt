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

@Suppress("ForbiddenComment", "UnusedParameter")
internal fun <TSource> ArgumentBuilder<TSource, *>.roleAttributesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("attributes") {
        requires { hasPermission(it, "commands.gradeway.role.attributes") }

        literal("set") {
            string("key") {
                literal("string") {
                    string("value") {}
                }
                literal("char") {
                    string("value") {}
                }
                literal("boolean") {
                    boolean("value") {}
                }
                literal("integer") {
                    integer("value") {}
                }
                literal("long") {
                    long("value") {}
                }
                literal("double") {
                    double("value") {}
                }
                literal("float") {
                    float("value") {}
                }
                literal("short") {
                    integer("value") {}
                }
                literal("byte") {
                    integer("value") {}
                }
                literal("uuid") {
                    string("value") {}
                }
                literal("instant") {
                    long("value") {}
                }
                literal("duration") {
                    long("value") {}
                }
            }
        }

        literal("remove") {
            string("key") {
                execute {

                }
            }
        }

        literal("list") {}

        execute {
            // TODO: PROVIDE HELP
        }
    }
}

@Suppress("ForbiddenComment", "UnusedParameter", "LongMethod")
internal fun <TSource> ArgumentBuilder<TSource, *>.rolePermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("permissions") {
        requires { hasPermission(it, "commands.gradeway.role.permissions") }

        literal("set") {
            string("permission") {
                boolean("status") {
                    execute {
                        val audience = sourceToAudience(source)

                        val status = booleanParam("status")
                        val permission = stringParam("permission")
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

                    val permission = stringParam("permission")
                    val idOrName = stringParam("idOrName")

                    val entity = gradeway.players.findByIdOrName(idOrName)
                    if (entity == null) {
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.commands.role.notNound",
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
            string("permission") {
                execute {
                    val audience = sourceToAudience(source)

                    val permission = stringParam("permission")
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

                gradeway.roles.clearPermissions(entity)
                    .onLeft { error ->
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

        execute {
            // TODO: PROVIDE HELP
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

@Suppress("ForbiddenComment", "UnusedParameter")
internal fun <TSource> ArgumentBuilder<TSource, *>.playerRolesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("roles") {
        requires { hasPermission(it, "commands.gradeway.player.roles") }

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
}

@Suppress("ForbiddenComment", "UnusedParameter")
internal fun <TSource> ArgumentBuilder<TSource, *>.playerAttributesBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("attributes") {
        requires { hasPermission(it, "commands.gradeway.player.attributes") }

        literal("set") {
            string("key") {
                literal("string") {
                    string("value") {}
                }
                literal("char") {
                    string("value") {}
                }
                literal("boolean") {
                    boolean("value") {}
                }
                literal("integer") {
                    integer("value") {}
                }
                literal("long") {
                    long("value") {}
                }
                literal("double") {
                    double("value") {}
                }
                literal("float") {
                    float("value") {}
                }
                literal("short") {
                    integer("value") {}
                }
                literal("byte") {
                    integer("value") {}
                }
                literal("uuid") {
                    string("value") {}
                }
                literal("instant") {
                    long("value") {}
                }
                literal("duration") {
                    long("value") {}
                }
            }
        }

        literal("remove") {
            string("key") {
                execute {

                }
            }
        }

        literal("list") {}

        execute {
            // TODO: PROVIDE HELP
        }
    }
}

@Suppress("ForbiddenComment", "UnusedParameter", "LongMethod")
internal fun <TSource> ArgumentBuilder<TSource, *>.playerPermissionsBuilder(
    gradeway: CommonGradeway,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
) {
    literal("permissions") {
        requires { hasPermission(it, "commands.gradeway.player.permissions") }

        literal("set") {
            string("permission") {
                boolean("status") {
                    execute {
                        val audience = sourceToAudience(source)

                        val status = booleanParam("status")
                        val permission = stringParam("permission")
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

                    val permission = stringParam("permission")
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
            string("permission") {
                execute {
                    val audience = sourceToAudience(source)

                    val permission = stringParam("permission")
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

                gradeway.players.clearPermissions(entity)
                    .onLeft { error ->
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

        execute {
            // TODO: PROVIDE HELP
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
