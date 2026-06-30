/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object LongAttributeType : AttributeType<Long> {
    override val klass: KClass<Long> = Long::class
    override fun key() = Key.key("attribute", "long")

    override fun serialize(value: Long): String = value.toString()
    override fun deserialize(value: String): Long = value.toLong()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is AttributeType<*>) return false
        return other.key() == key()
    }

    override fun hashCode(): Int = key().hashCode()
}
