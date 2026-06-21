/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.command

import com.mojang.brigadier.context.CommandContext
import dev.gradienttim.gradeway.Gradeway
import dev.gradienttim.gradeway.database.models.player.PlayerEntity
import dev.gradienttim.gradeway.database.models.role.RoleEntity
import java.util.*

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

fun <TCommandSource> CommandContext<TCommandSource>.playerEntityParam(
    name: String,
    gradeway: Gradeway,
): Pair<String, PlayerEntity?> {
    val idOrName = stringParam(name)

    return try {
        val uniqueId = UUID.fromString(idOrName)
        idOrName to gradeway.players.findById(uniqueId)
    } catch (_: IllegalArgumentException) {
        idOrName to gradeway.players.findByName(idOrName)
    }
}

fun <TCommandSource> CommandContext<TCommandSource>.roleEntityParam(
    name: String,
    gradeway: Gradeway,
): Pair<String, RoleEntity?> {
    val idOrName = stringParam(name)

    return try {
        val uniqueId = UUID.fromString(idOrName)
        idOrName to gradeway.roles.findById(uniqueId)
    } catch (_: IllegalArgumentException) {
        idOrName to gradeway.roles.findByName(idOrName)
    }
}
