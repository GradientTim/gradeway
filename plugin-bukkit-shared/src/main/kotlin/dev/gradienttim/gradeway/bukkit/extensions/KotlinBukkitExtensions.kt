/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.extensions

import java.util.concurrent.TimeUnit

// Minecraft tick duration in milliseconds
private const val TICK_DURATION_MS = 50L

fun Long.toTicks(unit: TimeUnit): Long = (unit.toMillis(this) / TICK_DURATION_MS).coerceAtLeast(1)
