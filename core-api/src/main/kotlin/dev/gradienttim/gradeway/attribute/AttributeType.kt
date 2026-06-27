/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute

import net.kyori.adventure.key.Keyed
import kotlin.reflect.KClass

interface AttributeType<T : Any> : Keyed {
    val klass: KClass<T>

    fun serialize(value: T): String
    fun deserialize(value: String): T

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}
