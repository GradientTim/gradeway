/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute

import net.kyori.adventure.key.Key

class Attribute<TValue : Any>(
    val type: AttributeType<TValue>,
    val key: Key,
    val value: TValue
) {
    companion object : AttributeBuilder
}
