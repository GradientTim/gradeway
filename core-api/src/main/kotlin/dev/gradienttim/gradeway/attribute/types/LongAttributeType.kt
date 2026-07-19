/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object LongAttributeType : AttributeType<Long>() {
    override val type: String = "long"
    override val klass: KClass<Long> = Long::class
    override val unsafe: Boolean = true

    override fun serialize(value: Long): String = value.toString()
    override fun deserialize(value: String): Long? = value.toLongOrNull()
    override fun fallback(attributeKey: Key): Long = 0L
}
