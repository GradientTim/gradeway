/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object StringAttributeType : AttributeType<String> {
    override val klass: KClass<String> = String::class
    override fun key() = Key.key("attribute", "string")

    override fun serialize(value: String): String = value
    override fun deserialize(value: String): String = value

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is AttributeType<*>) return false
        return other.key() == key()
    }

    override fun hashCode(): Int = key().hashCode()
}
