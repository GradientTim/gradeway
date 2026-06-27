/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import java.time.Duration
import kotlin.reflect.KClass

object DurationAttributeType : AttributeType<Duration> {
    override val klass: KClass<Duration> = Duration::class
    override fun key() = Key.key("attribute", "duration")

    override fun serialize(value: Duration): String = value.toMillis().toString()
    override fun deserialize(value: String): Duration = Duration.ofMillis(value.toLong())

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is DurationAttributeType) return false
        return other.key() == key()
    }

    override fun hashCode(): Int =key().hashCode()
}
