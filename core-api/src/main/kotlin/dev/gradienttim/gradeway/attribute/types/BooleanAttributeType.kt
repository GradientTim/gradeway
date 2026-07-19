/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object BooleanAttributeType : AttributeType<Boolean>() {
    override val type: String = "boolean"
    override val klass: KClass<Boolean> = Boolean::class
    override val unsafe: Boolean = true

    override fun serialize(value: Boolean): String = value.toString()
    override fun deserialize(value: String): Boolean? = value.toBooleanOrNull()
    override fun fallback(attributeKey: Key): Boolean = true

    private fun String.toBooleanOrNull(): Boolean? = when (trim().lowercase()) {
        "true", "yes", "on", "1", "t", "y", "enabled", "active" -> true
        "false", "no", "off", "0", "f", "n", "disabled", "inactive" -> false
        else -> null
    }
}
