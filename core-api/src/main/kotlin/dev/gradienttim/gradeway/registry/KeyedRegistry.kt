/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.registry

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Keyed

abstract class KeyedRegistry<TItem> where TItem : Any, TItem : Keyed {
    val items: Set<TItem>
        field = mutableSetOf<TItem>()

    fun register(item: TItem) = items.add(item)
    fun unregister(key: Key) = items.removeIf { it.key() == key }

    fun find(key: Key) = items.find { it.key() == key }
    fun findOrElse(key: Key, fallback: TItem) = find(key) ?: fallback
}
