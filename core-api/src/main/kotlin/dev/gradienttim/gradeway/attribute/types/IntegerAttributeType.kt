/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object IntegerAttributeType : AttributeType<Int>() {
    override val type: String = "integer"
    override val klass: KClass<Int> = Int::class
    override val unsafe: Boolean = true
    override val fallback: (attributeKey: Key) -> Int = { 0 }

    override fun serialize(value: Int): String = value.toString()
    override fun deserialize(value: String): Int? = value.toIntOrNull()
}
