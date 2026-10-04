/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.command

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlin.reflect.KClass

fun <TCommandSource> command(
    name: String,
    builder: LiteralArgumentBuilder<TCommandSource>.() -> Unit,
): LiteralArgumentBuilder<TCommandSource> = LiteralArgumentBuilder
    .literal<TCommandSource>(name)
    .apply(builder)

fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.literal(
    name: String,
    builder: LiteralArgumentBuilder<TCommandSource>.() -> Unit,
) = command(name, builder).also(::then)

fun <TCommandSource, T : Any> CommandContext<TCommandSource>.param(
    name: String,
    type: KClass<T>,
): T = getArgument(name, type.java)

inline fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.execute(
    crossinline execute: CommandContext<TCommandSource>.() -> Unit,
) = this.apply {
    executes {
        execute(it)
        0
    }
}

fun <TCommandSource, T> RequiredArgumentBuilder<TCommandSource, T>.suggest(
    vararg suggestions: T,
    transform: (T) -> String = { it.toString() },
) = suggest(suggestions.toList(), transform)

fun <TCommandSource, T> RequiredArgumentBuilder<TCommandSource, T>.suggest(
    vararg suggestions: Pair<Int, T>,
    transform: (T) -> String,
) = suggest(suggestions.toMap(), transform)

fun <TCommandSource, T> RequiredArgumentBuilder<TCommandSource, T>.suggest(
    suggestions: List<T> = emptyList(),
    transform: (T) -> String = { it.toString() },
) = this.apply {
    suggests { _, builder ->
        suggestions.map { transform(it) }.forEach { suggestion ->
            if (suggestion.startsWith(builder.remainingLowerCase)) {
                builder.suggest(suggestion)
            }
        }
        builder.buildFuture()
    }
}

fun <TCommandSource, T> RequiredArgumentBuilder<TCommandSource, T>.suggest(
    suggestions: Map<Int, T> = emptyMap(),
    transform: (T) -> String,
) = this.apply {
    suggests { _, builder ->
        suggestions.forEach { (key, value) ->
            val stringValue = transform(value)
            if (stringValue.startsWith(builder.remainingLowerCase)) {
                builder.suggest(key) { stringValue }
            }
        }
        builder.buildFuture()
    }
}
