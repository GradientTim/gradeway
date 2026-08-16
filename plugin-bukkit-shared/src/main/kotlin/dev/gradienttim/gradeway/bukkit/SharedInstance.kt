/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit

import dev.gradienttim.gradeway.bukkit.scheduler.SharedScheduler
import org.bukkit.Server

interface SharedInstance {
    val server: Server
    val scheduler: SharedScheduler
}
