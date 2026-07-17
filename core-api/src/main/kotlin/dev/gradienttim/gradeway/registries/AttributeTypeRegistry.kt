/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.registries

import dev.gradienttim.gradeway.attribute.AttributeType
import dev.gradienttim.gradeway.attribute.types.*
import dev.gradienttim.gradeway.registry.TypedRegistry
import kotlin.reflect.KClass

object AttributeTypeRegistry : TypedRegistry<AttributeType<*>>() {
    fun findByKlass(klass: KClass<*>) = items.find { it.klass == klass }

    init {
        register(StringAttributeType)
        register(BooleanAttributeType)
        register(IntegerAttributeType)
        register(LongAttributeType)
        register(DoubleAttributeType)
        register(FloatAttributeType)
        register(UUIDAttributeType)
        register(InstantAttributeType)
        register(DurationAttributeType)
    }
}
