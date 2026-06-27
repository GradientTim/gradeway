/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import java.time.Instant
import kotlin.reflect.KClass

object InstantAttributeType : AttributeType<Instant> {
    override val klass: KClass<Instant> = Instant::class
    override fun key() = Key.key("attribute", "boolean")

    override fun serialize(value: Instant): String = value.toEpochMilli().toString()
    override fun deserialize(value: String): Instant = Instant.ofEpochMilli(value.toLong())

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is InstantAttributeType) return false
        return other.key() == key()
    }

    override fun hashCode(): Int = key().hashCode()
}
