/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.attribute

import dev.gradienttim.gradeway.attribute.types.*
import dev.gradienttim.gradeway.registries.AttributeTypeRegistry
import net.kyori.adventure.key.Key
import java.time.Duration
import java.time.Instant
import java.util.*

interface AttributeBuilder {
    /**
     * Creates an instance of `Attribute` for the specified key and value if a matching attribute type is registered.
     *
     * @param key The key associated with the attribute.
     * @param value The value to associate with the key.
     * @return An instance of `Attribute` if the attribute type for the value's class is found, or `null` otherwise.
     */
    fun <TValue : Any> of(key: Key, value: TValue): Attribute<TValue>? {
        @Suppress("UNCHECKED_CAST")
        val type = AttributeTypeRegistry.findByKlass(value::class) as? AttributeType<TValue> ?: return null
        return create(type, key, value)
    }

    /**
     * Creates an instance of `Attribute` for the specified key and value, throwing an exception if no matching
     * attribute type is registered for the value's class.
     *
     * @param key The key associated with the attribute.
     * @param value The value to associate with the key.
     * @return An instance of `Attribute` with the specified key and value.
     * @throws IllegalStateException if no matching attribute type is registered for the value's class.
     */
    fun <TValue : Any> ofOrThrow(key: Key, value: TValue): Attribute<TValue> =
        of(key, value) ?: error("Attribute type for '${value::class.simpleName}' is not registered.")

    /**
     * Creates an instance of `Attribute` for the specified type, key, and value.
     *
     * @param type The attribute type that defines the value's class and serialization/deserialization behavior.
     * @param key The key associated with the attribute.
     * @param value The value to associate with the attribute.
     * @return An instance of `Attribute` with the specified type, key, and value.
     */
    fun <TValue : Any> create(type: AttributeType<TValue>, key: Key, value: TValue) = Attribute(type, key, value)

    /**
     * Creates an `Attribute` instance for the specified type key, key, and value.
     * Validates that the `AttributeType` associated with the `typeKey` matches the runtime type of the provided `value`.
     *
     * @param type The type identifying the attribute type to be used.
     * @param key The key associated with the attribute.
     * @param value The value to associate with the attribute. Must match the type defined by the attribute type.
     * @return An instance of `Attribute` with the specified type key, key, and value.
     * @throws IllegalStateException if no `AttributeType` is registered for the provided `typeKey` or
     *                               if the runtime type of `value` does not match the expected type of the `AttributeType`.
     */
    fun <TValue : Any> create(
        type: String,
        key: Key,
        value: TValue
    ): Attribute<TValue> {
        val attributeType = AttributeTypeRegistry.find(type)
            ?: error("Attribute type with key '$type' is not registered.")

        if (attributeType.klass != value::class) {
            error("Attribute type '${type::class.simpleName}' cannot be used for value '$value'.")
        }

        @Suppress("UNCHECKED_CAST")
        return create(attributeType as AttributeType<TValue>, key, value)
    }

    /**
     * Creates an `Attribute` instance for the specified type, key, and value in an unsafe manner.
     * If the attribute type is not registered or if the type casting is invalid, this method
     * will throw an exception at runtime.
     *
     * @param type The string identifier representing the attribute type.
     * @param key The key associated with the attribute.
     * @param value The value to associate with the key.
     * @return An instance of `Attribute` with the specified type, key, and value.
     * @throws IllegalStateException if the attribute type with the provided identifier is not registered.
     */
    fun <TValue : Any> createUnsafe(type: String, key: Key, value: TValue): Attribute<TValue> {
        @Suppress("UNCHECKED_CAST")
        val attributeType = AttributeTypeRegistry.find(type) as? AttributeType<TValue>
            ?: error("Attribute type with key '$type' is not registered.")

        return createUnsafe(attributeType, key, value)
    }

    /**
     * Creates an `Attribute` instance for the specified attribute type, key, and value in an unsafe manner.
     * Performs type deserialization if the attribute type is not a string. If deserialization fails, a fallback value is used.
     *
     * @param type The attribute type defining the expected value type and serialization/deserialization behavior.
     * @param key The key associated with the attribute.
     * @param value The value to associate with the specified key and type.
     * @return An instance of `Attribute` containing the specified key and value, potentially transformed by the attribute type.
     */
    fun <TValue : Any> createUnsafe(type: AttributeType<TValue>, key: Key, value: TValue): Attribute<TValue> {
        var tempValue = value
        if (type.unsafe) {
            tempValue = type.deserialize(value.toString()) ?: type.fallback(key)
        }

        return create(type, key, tempValue)
    }

    /**
     * Creates an `Attribute` instance for the given key and string value.
     *
     * @param key The key associated with the attribute.
     * @param value The string value to associate with the key.
     * @return An instance of `Attribute` with the specified key and string value.
     */
    fun string(key: Key, value: String): Attribute<String> = create(StringAttributeType, key, value)

    /**
     * Creates a boolean `Attribute` instance for the specified key and value.
     *
     * @param key The key associated with the attribute.
     * @param value The boolean value to associate with the key.
     * @return An instance of `Attribute` with the specified key and boolean value.
     */
    fun boolean(key: Key, value: Boolean): Attribute<Boolean> = create(BooleanAttributeType, key, value)

    /**
     * Creates an integer `Attribute` instance for the specified key and value.
     *
     * @param key The key associated with the attribute.
     * @param value The integer value to associate with the key.
     * @return An instance of `Attribute` with the specified key and integer value.
     */
    fun integer(key: Key, value: Int): Attribute<Int> = create(IntegerAttributeType, key, value)

    /**
     * Creates a `Long` attribute for the specified key and value.
     *
     * @param key The key associated with the attribute.
     * @param value The long value to associate with the specified key.
     * @return An instance of `Attribute` with the specified key and long value.
     */
    fun long(key: Key, value: Long): Attribute<Long> = create(LongAttributeType, key, value)

    /**
     * Creates a `Double` attribute for the specified key and value.
     *
     * @param key The key associated with the attribute.
     * @param value The double value to associate with the specified key.
     * @return An instance of `Attribute` with the specified key and double value.
     */
    fun double(key: Key, value: Double): Attribute<Double> = create(DoubleAttributeType, key, value)

    /**
     * Creates a `Float` attribute for the specified key and value.
     *
     * @param key The key associated with the attribute.
     * @param value The float value to associate with the specified key.
     * @return An instance of `Attribute` with the specified key and float value.
     */
    fun float(key: Key, value: Float): Attribute<Float> = create(FloatAttributeType, key, value)

    /**
     * Creates a UUID-based `Attribute` instance for the specified key and value.
     *
     * @param key The key associated with the attribute.
     * @param value The UUID value to associate with the specified key.
     * @return An instance of `Attribute` with the specified key and UUID value.
     */
    fun uuid(key: Key, value: UUID): Attribute<UUID> = create(UUIDAttributeType, key, value)

    /**
     * Creates an `Attribute` instance for the specified key and `Instant` value.
     *
     * @param key The key associated with the attribute.
     * @param value The `Instant` value to associate with the specified key.
     * @return An instance of `Attribute` with the specified key and `Instant` value.
     */
    fun instant(key: Key, value: Instant): Attribute<Instant> = create(InstantAttributeType, key, value)

    /**
     * Creates a `Duration` attribute for the specified key and value.
     *
     * @param key The key associated with the attribute.
     * @param value The `Duration` value to associate with the specified key.
     * @return An instance of `Attribute` with the specified key and `Duration` value.
     */
    fun duration(key: Key, value: Duration): Attribute<Duration> = create(DurationAttributeType, key, value)
}
