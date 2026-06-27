/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import java.util.UUID
import kotlin.reflect.KClass

object UUIDAttributeType : AttributeType<UUID> {
    override val klass: KClass<UUID> = UUID::class
    override fun key() = Key.key("attribute", "uuid")

    override fun serialize(value: UUID): String = value.toString()
    override fun deserialize(value: String): UUID = UUID.fromString(value)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is UUIDAttributeType) return false
        return other.key() == key()
    }

    override fun hashCode(): Int = key().hashCode()
}
