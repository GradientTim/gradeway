/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object IntegerAttributeType : AttributeType<Int> {
    override val klass: KClass<Int> = Int::class
    override fun key() = Key.key("attribute", "integer")

    override fun serialize(value: Int): String = value.toString()
    override fun deserialize(value: String): Int = value.toInt()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is AttributeType<*>) return false
        return other.key() == key()
    }

    override fun hashCode(): Int = key().hashCode()
}
