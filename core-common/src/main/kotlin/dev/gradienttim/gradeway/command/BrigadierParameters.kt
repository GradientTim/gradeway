/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.command

import com.mojang.brigadier.context.CommandContext

fun <TCommandSource> CommandContext<TCommandSource>.stringParam(
    name: String,
) = param(name, String::class)

fun <TCommandSource> CommandContext<TCommandSource>.intParam(
    name: String,
) = param(name, Int::class)

fun <TCommandSource> CommandContext<TCommandSource>.floatParam(
    name: String,
) = param(name, Float::class)

fun <TCommandSource> CommandContext<TCommandSource>.longParam(
    name: String,
) = param(name, Long::class)

fun <TCommandSource> CommandContext<TCommandSource>.doubleParam(
    name: String,
) = param(name, Double::class)

fun <TCommandSource> CommandContext<TCommandSource>.booleanParam(
    name: String,
) = param(name, Boolean::class)
