/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object FloatAttributeType : AttributeType<Float>() {
    override val type: String = "float"
    override val klass: KClass<Float> = Float::class
    override val unsafe: Boolean = true

    override fun serialize(value: Float): String = value.toString()
    override fun deserialize(value: String): Float? = value.toFloatOrNull()
    override fun fallback(attributeKey: Key): Float = 0F
}
