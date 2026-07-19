/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import java.time.Duration
import kotlin.reflect.KClass

object DurationAttributeType : AttributeType<Duration>() {
    override val type: String = "duration"
    override val klass: KClass<Duration> = Duration::class
    override val unsafe: Boolean = true

    override fun serialize(value: Duration): String = value.toMillis().toString()
    override fun deserialize(value: String): Duration? {
        val millis = value.toLongOrNull() ?: return null
        return Duration.ofMillis(millis)
    }
    override fun fallback(attributeKey: Key): Duration = Duration.ZERO
}
