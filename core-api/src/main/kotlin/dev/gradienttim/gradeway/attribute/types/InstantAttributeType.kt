/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import java.time.Instant
import kotlin.reflect.KClass

object InstantAttributeType : AttributeType<Instant>() {
    override val type: String = "instant"
    override val klass: KClass<Instant> = Instant::class
    override val unsafe: Boolean = true
    override val fallback: (attributeKey: Key) -> Instant = { Instant.EPOCH }

    override fun serialize(value: Instant): String = value.toEpochMilli().toString()
    override fun deserialize(value: String): Instant? {
        val millis = value.toLongOrNull() ?: return null
        return Instant.ofEpochMilli(millis)
    }
}
