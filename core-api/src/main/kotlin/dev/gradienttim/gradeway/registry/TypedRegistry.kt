/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.registry

import dev.gradienttim.gradeway.utilities.Typed

abstract class TypedRegistry<TItem> where TItem : Any, TItem : Typed {
    val items: Set<TItem>
        field = mutableSetOf<TItem>()

    fun register(item: TItem) = items.add(item)
    fun unregister(type: String) = items.removeIf { it.type == type }

    fun find(type: String) = items.find { it.type == type }
    fun findOrElse(type: String, fallback: TItem) = find(type) ?: fallback
}
