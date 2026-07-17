/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object StringAttributeType : AttributeType<String>() {
    override val type: String = "string"
    override val klass: KClass<String> = String::class
    override val unsafe: Boolean = false
    override val fallback: (attributeKey: Key) -> String = { "" }

    override fun serialize(value: String): String = value
    override fun deserialize(value: String): String = value
}
