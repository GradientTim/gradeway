/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.scheduler

interface SharedScheduler {
    fun runTask(action: () -> Unit)
}
