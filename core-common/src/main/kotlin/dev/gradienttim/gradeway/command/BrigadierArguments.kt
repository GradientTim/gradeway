/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.command

import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder

inline fun <TCommandSource, reified T> ArgumentBuilder<TCommandSource, *>.argument(
    name: String,
    type: ArgumentType<T>,
    builder: RequiredArgumentBuilder<TCommandSource, T>.() -> Unit,
): RequiredArgumentBuilder<TCommandSource, T> = RequiredArgumentBuilder.argument<TCommandSource, T>(name, type)
    .apply(builder)
    .also(::then)

fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.string(
    name: String,
    type: StringArgumentType = StringArgumentType.string(),
    builder: RequiredArgumentBuilder<TCommandSource, String>.() -> Unit,
): RequiredArgumentBuilder<TCommandSource, String> = argument<TCommandSource, String>(name, type, builder)

fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.integer(
    name: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    type: IntegerArgumentType = IntegerArgumentType.integer(min, max),
    builder: RequiredArgumentBuilder<TCommandSource, Int>.() -> Unit,
): RequiredArgumentBuilder<TCommandSource, Int> = argument<TCommandSource, Int>(name, type, builder)

fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.float(
    name: String,
    min: Float = Float.MIN_VALUE,
    max: Float = Float.MAX_VALUE,
    type: FloatArgumentType = FloatArgumentType.floatArg(min, max),
    builder: RequiredArgumentBuilder<TCommandSource, Float>.() -> Unit,
): RequiredArgumentBuilder<TCommandSource, Float> = argument<TCommandSource, Float>(name, type, builder)

fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.long(
    name: String,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    type: LongArgumentType = LongArgumentType.longArg(min, max),
    builder: RequiredArgumentBuilder<TCommandSource, Long>.() -> Unit,
): RequiredArgumentBuilder<TCommandSource, Long> = argument<TCommandSource, Long>(name, type, builder)

fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.double(
    name: String,
    min: Double = Double.MIN_VALUE,
    max: Double = Double.MAX_VALUE,
    type: DoubleArgumentType = DoubleArgumentType.doubleArg(min, max),
    builder: RequiredArgumentBuilder<TCommandSource, Double>.() -> Unit,
): RequiredArgumentBuilder<TCommandSource, Double> = argument<TCommandSource, Double>(name, type, builder)

fun <TCommandSource> ArgumentBuilder<TCommandSource, *>.boolean(
    name: String,
    type: BoolArgumentType = BoolArgumentType.bool(),
    builder: RequiredArgumentBuilder<TCommandSource, Boolean>.() -> Unit,
): RequiredArgumentBuilder<TCommandSource, Boolean> = argument<TCommandSource, Boolean>(name, type, builder)
