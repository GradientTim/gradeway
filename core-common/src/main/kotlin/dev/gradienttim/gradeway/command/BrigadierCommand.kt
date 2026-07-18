/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.gradienttim.gradeway.CommonGradeway
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

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

fun <S, T> RequiredArgumentBuilder<S, T>.suggestsDebounced(
    gradeway: CommonGradeway,
    delayMillis: Long = 250,
    block: suspend (builder: SuggestionsBuilder) -> Unit
): RequiredArgumentBuilder<S, T> {
    var searchJob: Job? = null

    return this.suggests { _, builder ->
        searchJob?.cancel()

        val future = CompletableFuture<Suggestions>()

        searchJob = gradeway.backgroundScope.launch {
            try {
                delay(delayMillis.milliseconds)
                block(builder)
            } finally {
                future.complete(builder.build())
            }
        }

        return@suggests future
    }
}
