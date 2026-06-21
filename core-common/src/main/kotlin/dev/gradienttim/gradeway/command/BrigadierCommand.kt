/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
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
): ArgumentBuilder<TCommandSource, *> = executeWith {
    execute(this)
    Command.SINGLE_SUCCESS
}

inline fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.executeWith(
    crossinline execute: CommandContext<TCommandSource>.() -> Int,
): ArgumentBuilder<TCommandSource, *> = this.executes {
    execute(it)
}
