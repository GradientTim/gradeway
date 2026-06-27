/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object BooleanAttributeType : AttributeType<Boolean> {
    override val klass: KClass<Boolean> = Boolean::class
    override fun key() = Key.key("attribute", "boolean")

    override fun serialize(value: Boolean): String = value.toString()
    override fun deserialize(value: String): Boolean = value.toBoolean()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is BooleanAttributeType) return false
        return other.key() == key()
    }

    override fun hashCode(): Int = key().hashCode()
}
