/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute

import dev.gradienttim.gradeway.attribute.Attribute.*
import net.kyori.adventure.key.Key
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Defines a builder interface for creating various types of attributes based on the key-value pair provided.
 * This builder supports multiple primitive types, collections, and custom types like `UUID`, `Instant`, and `Duration`.
 * It also handles the creation of attributes for `Enum` values.
 */
internal interface AttributeBuilder {
    /**
     * Creates an instance of [Attribute] for the given key and value based on the value type.
     * Returns null if the value type is unsupported.
     *
     * @param key The key associated with the attribute.
     * @param value The value to be stored in the attribute.
     * @return An instance of [Attribute] encapsulating the given key and value,
     * or null if the value type is unsupported.
     */
    @Suppress("UNCHECKED_CAST")
    fun <TValue : Any> of(key: Key, value: TValue): Attribute<TValue>? {
        val attribute = when (value) {
            is String -> string(key, value)
            is Boolean -> boolean(key, value)
            is Int -> integer(key, value)
            is Long -> long(key, value)
            is Double -> double(key, value)
            is Float -> float(key, value)
            is UUID -> uuid(key, value)
            is Instant -> instant(key, value)
            is Duration -> duration(key, value)
            else -> null
        }
        return attribute as? Attribute<TValue>
    }

    /**
     * Creates an instance of [Attribute] for the given key and value. If the value type is unsupported,
     * throws an [IllegalStateException] with a message indicating the unsupported type.
     *
     * @param key The key associated with the attribute.
     * @param value The value to be stored in the attribute.
     * @return An instance of [Attribute] encapsulating the given key and value.
     * @throws IllegalStateException If the value type is not supported.
     */
    fun <TValue : Any> ofOrThrow(key: Key, value: TValue): Attribute<TValue> {
        return of(key, value) ?: error("Unsupported attribute type: ${value::class.simpleName}")
    }

    /**
     * Creates a `StringAttribute` instance with the specified key and string value.
     *
     * @param key The key associated with the attribute.
     * @param value The string value to be stored in the attribute.
     * @return A `StringAttribute` containing the given key and value.
     */
    fun string(key: Key, value: String) = StringAttribute(key, value)

    /**
     * Creates a `BooleanAttribute` instance with the specified key and boolean value.
     *
     * @param key The key associated with the attribute.
     * @param value The boolean value to be stored in the attribute.
     * @return A `BooleanAttribute` containing the given key and value.
     */
    fun boolean(key: Key, value: Boolean) = BooleanAttribute(key, value)

    /**
     * Creates an `IntegerAttribute` instance with the specified key and integer value.
     *
     * @param key The key associated with the attribute.
     * @param value The integer value to be stored in the attribute.
     * @return An `IntegerAttribute` containing the given key and value.
     */
    fun integer(key: Key, value: Int) = IntegerAttribute(key, value)

    /**
     * Creates a `LongAttribute` instance with the specified key and long value.
     *
     * @param key The key associated with the attribute.
     * @param value The long value to be stored in the attribute.
     * @return A `LongAttribute` containing the given key and value.
     */
    fun long(key: Key, value: Long) = LongAttribute(key, value)

    /**
     * Creates a `DoubleAttribute` instance with the specified key and double value.
     *
     * @param key The key associated with the attribute.
     * @param value The double value to be stored in the attribute.
     * @return A `DoubleAttribute` containing the given key and value.
     */
    fun double(key: Key, value: Double) = DoubleAttribute(key, value)

    /**
     * Creates a `FloatAttribute` instance with the specified key and float value.
     *
     * @param key The key associated with the attribute.
     * @param value The float value to be stored in the attribute.
     * @return A `FloatAttribute` containing the given key and value.
     */
    fun float(key: Key, value: Float) = FloatAttribute(key, value)

    /**
     * Creates a `UuidAttribute` instance with the specified key and UUID value.
     *
     * @param key The key associated with the attribute.
     * @param value The UUID value to be stored in the attribute.
     * @return A `UuidAttribute` containing the given key and value.
     */
    fun uuid(key: Key, value: UUID) = UuidAttribute(key, value)

    /**
     * Creates an `InstantAttribute` instance with the specified key and instant value.
     *
     * @param key The key associated with the attribute.
     * @param value The instant value to be stored in the attribute.
     * @return An `InstantAttribute` containing the given key and value.
     */
    fun instant(key: Key, value: Instant) = InstantAttribute(key, value)

    /**
     * Creates a `DurationAttribute` instance with the specified key and duration value.
     *
     * @param key The key associated with the attribute.
     * @param value The duration value to be stored in the attribute.
     * @return A `DurationAttribute` containing the given key and value.
     */
    fun duration(key: Key, value: Duration) = DurationAttribute(key, value)
}
