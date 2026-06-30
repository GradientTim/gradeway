/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import dev.gradienttim.gradeway.commands.gradeway.permissionBuilder
import dev.gradienttim.gradeway.commands.gradeway.playerBuilder
import dev.gradienttim.gradeway.commands.gradeway.roleBuilder
import dev.gradienttim.gradeway.database.models.player.PlayersTable
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.extensions.likeAsStr
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.reflect.KClass

fun <TSource> gradewayCommandBuilder(
    gradeway: CommonGradeway,
    literal: String,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
): LiteralArgumentBuilder<TSource> {
    return command(literal) {
        roleBuilder(gradeway, hasPermission, sourceToAudience)
        playerBuilder(gradeway, hasPermission, sourceToAudience)
        permissionBuilder(gradeway, hasPermission, sourceToAudience)

        literal("reload") {
            requires { hasPermission(it, "gradeway.reload") }

            execute {
                val audience = sourceToAudience(source)

                gradeway.reload()
                    .onLeft { error ->
                        audience.sendMessage(
                            Component.translatable(
                                "gradeway.command.reload.failed",
                                Component.text(error.localizedMessage)
                            )
                        )
                    }
                    .onRight {
                        audience.sendMessage(Component.translatable("gradeway.command.reload.success"))
                    }
            }
        }

        execute {
            val audience = sourceToAudience(source)
            audience.sendMessage(Component.translatable("gradeway.command.usage.version"))
            if (hasPermission(source, "gradeway.usage")) {
                audience.sendMessage(Component.translatable("gradeway.command.usage.help"))
            }
        }
    }
}

internal fun <TSource, TResult> ArgumentBuilder<TSource, *>.createGlobalListHandler(
    gradeway: CommonGradeway,
    permission: String,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
    query: (page: Int, limit: Int) -> TResult,
    render: (audience: Audience, page: Int, limit: Int, result: TResult) -> Unit,
) {
    fun handleList(audience: Audience, page: Int, limit: Int = 10) {
        val result = transaction(gradeway.database) {
            query(page, limit)
        }
        render(audience, page, limit, result)
    }

    literal("list") {
        requires { hasPermission(it, permission) }

        integer("page", min = 1) {
            execute {
                val audience = sourceToAudience(source)
                val page = intParam("page")
                handleList(audience, page)
            }
        }

        execute {
            val audience = sourceToAudience(source)
            handleList(audience, 1)
        }
    }
}

internal fun <TSource, TResult> ArgumentBuilder<TSource, *>.createScopedListHandler(
    gradeway: CommonGradeway,
    permission: String,
    scopeKey: String,
    scopeType: KClass<*> = String::class,
    hasPermission: (source: TSource, permission: String) -> Boolean,
    sourceToAudience: (source: TSource) -> Audience,
    query: (scope: Any, page: Int, limit: Int) -> TResult,
    render: (audience: Audience, page: Int, limit: Int, result: TResult) -> Unit,
) {
    fun handleList(audience: Audience, scope: Any, page: Int, limit: Int = 10) {
        val result = transaction(gradeway.database) {
            query(scope, page, limit)
        }
        render(audience, page, limit, result)
    }

    literal("list") {
        requires { hasPermission(it, permission) }

        integer("page", min = 1) {
            execute {
                val audience = sourceToAudience(source)
                val scope = param(scopeKey, scopeType)
                val page = intParam("page")
                handleList(audience, scope, page)
            }
        }

        execute {
            val audience = sourceToAudience(source)
            val scope = param(scopeKey, scopeType)
            handleList(audience, scope, 1)
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
