/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute

import net.kyori.adventure.key.Keyed
import kotlin.reflect.KClass

interface AttributeType<T : Any> : Keyed {
    /**
     * Represents the Kotlin class reference for the generic type [T] associated with this attribute type.
     * This property provides metadata about the type, enabling reflective operations and type-related processing.
     */
    val klass: KClass<T>

    /**
     * Serializes the given value into its string representation.
     *
     * @param value The value to serialize.
     * @return A string representation of the given value.
     */
    fun serialize(value: T): String

    /**
     * Deserializes the given string into a value of type [T].
     *
     * @param value The string to deserialize.
     * @return The deserialized value of type [T].
     */
    fun deserialize(value: String): T

    /**
     * Checks whether this attribute type is equal to another object.
     *
     * @param other The object to compare against.
     * @return True if this attribute type is equal to the given object, false otherwise.
     */
    override fun equals(other: Any?): Boolean

    /**
     * Returns the hash code of this attribute type.
     *
     * @return The hash code value for this attribute type.
     */
    override fun hashCode(): Int
}
