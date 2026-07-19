/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import java.util.*
import kotlin.reflect.KClass

object UUIDAttributeType : AttributeType<UUID>() {
    override val type: String = "uuid"
    override val klass: KClass<UUID> = UUID::class
    override val unsafe: Boolean = true

    override fun serialize(value: UUID): String = value.toString()
    override fun deserialize(value: String): UUID? = runCatching {
        UUID.fromString(value)
    }.getOrNull()

    override fun fallback(attributeKey: Key): UUID = EMPTY_UUID

    val EMPTY_UUID = UUID(0, 0)
}
