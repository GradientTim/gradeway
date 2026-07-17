/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute.types

import dev.gradienttim.gradeway.attribute.AttributeType
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

object DoubleAttributeType : AttributeType<Double>() {
    override val type: String = "double"
    override val klass: KClass<Double> = Double::class
    override val unsafe: Boolean = true
    override val fallback: (attributeKey: Key) -> Double = { 0.0 }

    override fun serialize(value: Double): String = value.toString()
    override fun deserialize(value: String): Double? = value.toDoubleOrNull()
}
