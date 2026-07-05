/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.commands.extensions

import com.mojang.brigadier.builder.ArgumentBuilder
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.command.*
import net.kyori.adventure.audience.Audience
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.reflect.KClass

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
