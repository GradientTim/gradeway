/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.platform

import dev.gradienttim.gradeway.bukkit.platform.stores.BukkitPersistentNotificationStore
import dev.gradienttim.gradeway.platform.Stores
import dev.gradienttim.gradeway.platform.stores.MemoryUuidBooleanStore
import dev.gradienttim.gradeway.utilities.Store
import java.util.*

/**
 * The Bukkit/Paper stores.
 *
 * The stores are created before Gradeway loads its config, so the config-dependent choice of
 * store is made on every access instead of at construction. This also lets a config reload
 * switch stores without recreating them.
 *
 * @param persistentNotifications Returns whether notification states should be stored in the
 *        players' persistent data containers (surviving restarts) instead of in memory. Only
 *        called once the config is loaded.
 */
class BukkitStores(
    private val persistentNotifications: () -> Boolean,
) : Stores {
    private val persistentNotificationStore = BukkitPersistentNotificationStore()
    private val memoryNotificationStore = MemoryUuidBooleanStore()

    override val notification: Store<UUID, Boolean>
        get() = if (persistentNotifications()) persistentNotificationStore else memoryNotificationStore
}
