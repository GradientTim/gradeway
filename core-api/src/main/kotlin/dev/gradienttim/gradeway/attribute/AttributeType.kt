/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute

import dev.gradienttim.gradeway.utilities.Typed
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass

abstract class AttributeType<T : Any> : Typed {
    /**
     * Represents the Kotlin class reference for the generic type [T] associated with this attribute type.
     * This property provides metadata about the type, enabling reflective operations and type-related processing.
     */
    abstract val klass: KClass<T>

    /**
     * Indicates whether the attribute type can be considered unsafe.
     *
     * This property is typically used to signify that the handling or usage of this
     * attribute type comes with potential risks and should be approached with care.
     * It may influence serialization, deserialization, or other behaviors.
     */
    abstract val unsafe: Boolean

    /**
     * Serializes the given value into its string representation.
     *
     * @param value The value to serialize.
     * @return A string representation of the given value.
     */
    abstract fun serialize(value: T): String

    /**
     * Deserializes the given string into a value of type [T].
     *
     * @param value The string to deserialize.
     * @return The deserialized value of type [T].
     */
    abstract fun deserialize(value: String): T?

    /**
     * A function that determines a default fallback value for a given attribute key.
     *
     * The fallback mechanism ensures that a valid value of type [T] can always be obtained
     * even if the key does not directly correspond to an existing value in the attribute system.
     *
     * @param attributeKey The key for which the fallback value is required.
     * @return The fallback value of type [T] associated with the specified key.
     */
    abstract fun fallback(attributeKey: Key): T

    /**
     * Checks whether this attribute type is equal to another object.
     *
     * @param other The object to compare against.
     * @return True if this attribute type is equal to the given object, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is AttributeType<*>) return false
        return other.type == type
    }

    /**
     * Returns the hash code of this attribute type.
     *
     * @return The hash code value for this attribute type.
     */
    override fun hashCode(): Int = type.hashCode()
}
