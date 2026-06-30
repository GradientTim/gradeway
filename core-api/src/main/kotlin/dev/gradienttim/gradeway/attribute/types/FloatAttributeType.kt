/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object FloatAttributeType : AttributeType<Float> {
    override val klass: KClass<Float> = Float::class
    override fun key() = Key.key("attribute", "float")

    override fun serialize(value: Float): String = value.toString()
    override fun deserialize(value: String): Float = value.toFloat()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is AttributeType<*>) return false
        return other.key() == key()
    }

    override fun hashCode(): Int = key().hashCode()
}
