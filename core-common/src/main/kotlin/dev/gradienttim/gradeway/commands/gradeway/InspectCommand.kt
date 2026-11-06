/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.gradeway

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.command.context.CommandContext
import dev.gradienttim.gradeway.commands.extensions.suggestKnownPermissions
import dev.gradienttim.gradeway.commands.extensions.suggestPlayers
import dev.gradienttim.gradeway.extensions.toIdArgument
import dev.gradienttim.gradeway.services.permission.PermissionInspectionService.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.translation.Argument

private const val WHO_CAN_PAGE_SIZE = 50

/**
 * `/<root> why <player> <permission>` - shows every path through which a player effectively has a
 * permission, e.g. `Steve -> VIP -> Member -> basic-cmds -> essentials.home`.
 */
internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.whyCommand(
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    literal("why") {
        requires { commandContext.hasPermission(it, "gradeway.why") }

        string("player") {
            suggestPlayers(gradeway)

            string("permission") {
                suggestKnownPermissions(gradeway)

                execute {
                    val playerValue = stringParam("player")
                    val permission = stringParam("permission")

                    val player = gradeway.players.findByIdOrName(playerValue)
                    if (player == null) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.why.entityNotFound",
                                Argument.string("player", playerValue)
                            )
                        )
                        return@execute
                    }

                    val grants = gradeway.permissions.explainPlayerPermission(player, permission)
                    if (grants.isEmpty()) {
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.why.notGranted",
                                Argument.string("player", player.name),
                                Argument.string("permission", permission)
                            )
                        )
                        return@execute
                    }

                    commandContext.sendTranslatedMessage(
                        source,
                        Component.translatable(
                            "gradeway.command.why.header",
                            Argument.string("player", player.name),
                            Argument.string("permission", permission),
                            Argument.numeric("count", grants.size)
                        )
                    )

                    grants.forEach { grant ->
                        commandContext.sendTranslatedMessage(
                            source,
                            Component.translatable(
                                "gradeway.command.why.entry",
                                Argument.component("path", grantPath(grant))
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * `/<root> whocan <permission> [page]` - lists every role and player that effectively has a
 * permission, including those that only inherit it. Both lists are rendered compactly as
 * comma-separated names (details on hover) so a permission held by hundreds of players stays
 * readable: roles always in full (there are few), players paginated.
 */
internal fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.whoCanCommand(
    rootLiteral: String,
    gradeway: CommonGradeway<*>,
    commandContext: CommandContext<TCommandSource>,
) {
    fun handleWhoCan(source: TCommandSource, permission: String, page: Int) {
        val holders = gradeway.permissions.findPermissionHolders(permission)
        if (holders.roles.isEmpty() && holders.players.isEmpty()) {
            commandContext.sendTranslatedMessage(
                source,
                Component.translatable(
                    "gradeway.command.whocan.empty",
                    Argument.string("permission", permission)
                )
            )
            return
        }

        val pages = maxOf(1, (holders.players.size + WHO_CAN_PAGE_SIZE - 1) / WHO_CAN_PAGE_SIZE)
        val currentPage = page.coerceIn(1, pages)
        val shownPlayers = holders.players.take(currentPage * WHO_CAN_PAGE_SIZE).drop((currentPage - 1) * WHO_CAN_PAGE_SIZE)
        val remainingPlayers = holders.players.size - currentPage * WHO_CAN_PAGE_SIZE
        val nextPageCommand = "/$rootLiteral whocan $permission ${currentPage + 1}"

        commandContext.sendTranslatedMessage(
            source,
            Component.translatable(
                "gradeway.command.whocan.header",
                Argument.string("permission", permission)
            )
        )
        commandContext.sendTranslatedMessage(
            source,
            Component.translatable(
                "gradeway.command.whocan.roles",
                Argument.numeric("count", holders.roles.size),
                Argument.component("roles", whoCanRoleList(holders.roles))
            )
        )
        commandContext.sendTranslatedMessage(
            source,
            Component.translatable(
                "gradeway.command.whocan.players",
                Argument.numeric("count", holders.players.size),
                Argument.component(
                    "players",
                    whoCanPlayerList(shownPlayers, rootLiteral, permission, remainingPlayers, nextPageCommand)
                )
            )
        )
    }

    literal("whocan") {
        requires { commandContext.hasPermission(it, "gradeway.whocan") }

        string("permission") {
            suggestKnownPermissions(gradeway)

            execute {
                handleWhoCan(source, stringParam("permission"), 1)
            }

            integer("page", min = 1) {
                execute {
                    handleWhoCan(source, stringParam("permission"), intParam("page"))
                }
            }
        }
    }
}

internal fun whoCanRoleList(roles: List<PermissionHolder>): Component = joinHolders(
    roles.map { role ->
        Component.translatable(
            if (role.direct) "gradeway.command.whocan.role.direct" else "gradeway.command.whocan.role.inherited",
            role.id.toIdArgument(),
            Argument.string("role", role.name)
        )
    }
)

/**
 * Each name runs `/<root> why <player> <permission>` when clicked. When [remaining] players don't
 * fit on this page, a trailing "... and N more" runs [nextPageCommand]. Click events are set on the
 * components rather than through MiniMessage tags, since the command texts are dynamic.
 */
internal fun whoCanPlayerList(
    players: List<PermissionHolder>,
    rootLiteral: String,
    permission: String,
    remaining: Int = 0,
    nextPageCommand: String = "",
): Component {
    val entries = players.map { player ->
        val whyCommand = "/$rootLiteral why ${player.name} $permission"
        Component.translatable(
            if (player.direct) "gradeway.command.whocan.player.direct" else "gradeway.command.whocan.player.inherited",
            Argument.string("player", player.name),
            Argument.string("command", whyCommand)
        ).clickEvent(ClickEvent.runCommand(whyCommand))
    }
    val more = if (remaining > 0) {
        listOf(
            Component.translatable(
                "gradeway.command.whocan.more",
                Argument.numeric("count", remaining)
            ).clickEvent(ClickEvent.runCommand(nextPageCommand))
        )
    } else {
        emptyList()
    }
    return joinHolders(entries + more)
}

private fun joinHolders(entries: List<Component>): Component =
    if (entries.isEmpty()) {
        Component.translatable("gradeway.command.info.noEntries")
    } else {
        Component.join(
            JoinConfiguration.separator(Component.translatable("gradeway.command.whocan.separator")),
            entries
        )
    }

private fun grantPath(grant: PermissionGrant): Component {
    val nodes = grant.path.map { node ->
        val key = when (node) {
            is GrantSource.Player -> "gradeway.command.why.node.player"
            is GrantSource.Role -> "gradeway.command.why.node.role"
            is GrantSource.Group -> "gradeway.command.why.node.group"
            is GrantSource.Template -> "gradeway.command.why.node.template"
        }
        Component.translatable(key, node.id.toIdArgument(), Argument.string("name", node.name))
    }
    val permissionNode = Component.translatable(
        "gradeway.command.why.node.permission",
        grant.permission.id.value.toIdArgument(),
        Argument.string("permission", grant.permission.value),
        Argument.string("type", grant.permission.type.name)
    )

    return Component.join(
        JoinConfiguration.separator(Component.translatable("gradeway.command.why.separator")),
        nodes + permissionNode
    )
}
