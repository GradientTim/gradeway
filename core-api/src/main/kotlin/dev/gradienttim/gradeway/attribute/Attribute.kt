/*
MIT License
Copyright (c) 2026 GradientTim
*/
@file:UseContextualSerialization(UUID::class, Instant::class, Duration::class)

package dev.gradienttim.gradeway.attribute

import kotlinx.serialization.*
import kotlinx.serialization.json.JsonClassDiscriminator
import net.kyori.adventure.key.Key
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Represents a generic attribute with a key-value pair where the value can be of any specified type.
 * This is a sealed interface that provides a framework for handling different types of attributes.
 *
 * @param TValue The type of value associated with the attribute. Must be a non-null type.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class Attribute<TValue : Any> {
    companion object : AttributeBuilder

    /**
     * Represents the unique identifier associated with the attribute.
     * This key is used to map and reference the specific attribute instance
     * across different contexts or structures.
     */
    abstract val key: Key

    /**
     * Represents the value associated with an attribute. The type of the value is generic and depends on the specific
     * implementation of the attribute class, such as String, Char, Int, Long, Double, Float, Short, Byte, and more.
     */
    abstract var value: TValue

    /**
     * Indicates whether the current attribute is an instance of [StringAttribute].
     * Returns true if the attribute represents a [String] value, false otherwise.
     */
    val isString: Boolean get() = this is StringAttribute

    /**
     * Indicates whether the current attribute is an instance of [BooleanAttribute].
     * Returns true if the attribute represents a [Boolean] value, false otherwise.
     */
    val isBoolean: Boolean get() = this is BooleanAttribute

    /**
     * Indicates whether the current attribute is an instance of [IntegerAttribute].
     * Returns true if the attribute represents an [Int] value, false otherwise.
     */
    val isInteger: Boolean get() = this is IntegerAttribute

    /**
     * Indicates whether the current attribute is an instance of [LongAttribute].
     * Returns true if the attribute represents a [Long] value, false otherwise.
     */
    val isLong: Boolean get() = this is LongAttribute

    /**
     * Indicates whether the current attribute is an instance of [DoubleAttribute].
     * Returns true if the attribute represents a [Double] value, false otherwise.
     */
    val isDouble: Boolean get() = this is DoubleAttribute

    /**
     * Indicates whether the current attribute is an instance of [FloatAttribute].
     * Returns true if the attribute represents a [Float] value, false otherwise.
     */
    val isFloat: Boolean get() = this is FloatAttribute

    /**
     * Indicates whether the current attribute is an instance of [UuidAttribute].
     * Returns true if the attribute represents an [java.util.UUID] value, false otherwise.
     */
    val isUuid: Boolean get() = this is UuidAttribute

    /**
     * Indicates whether the current attribute is an instance of [InstantAttribute].
     * Returns true if the attribute represents an [java.time.Instant] value, false otherwise.
     */
    val isInstant: Boolean get() = this is InstantAttribute

    /**
     * Indicates whether the current attribute is an instance of [DurationAttribute].
     * Returns true if the attribute represents a [java.time.Duration] value, false otherwise.
     */
    val isDuration: Boolean get() = this is DurationAttribute

    /**
     * Indicates whether the current attribute represents a numeric type.
     *
     * The attribute is considered numeric if it is of one of the following types:
     * - IntegerAttribute
     * - LongAttribute
     * - DoubleAttribute
     * - FloatAttribute
     * - ShortAttribute
     * - ByteAttribute
     */
    val isNumber: Boolean
        get() = isInteger || isLong || isDouble || isFloat

    /**
     * Indicates whether the current attribute represents a special type.
     *
     * The attribute is considered special if it is of one of the following types:
     * - UuidAttribute
     * - InstantAttribute
     * - DurationAttribute
     */
    val isSpecial: Boolean
        get() = isUuid || isInstant || isDuration

    abstract fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Attribute<*>) return false
        return key == other.key
    }

    final override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun toString(): String {
        return "${this::class.simpleName}(key=$key, value=$value)"
    }

    @Serializable
    @SerialName("string")
    class StringAttribute(
        @Contextual override val key: Key,
        override var value: String,
    ) : Attribute<String>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is String) return false
            value = fromValue
            return true
        }
    }

    @Serializable
    @SerialName("boolean")
    class BooleanAttribute(
        @Contextual override val key: Key,
        override var value: Boolean,
    ) : Attribute<Boolean>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is Boolean) return false
            value = fromValue
            return true
        }
    }

    @Serializable
    @SerialName("integer")
    class IntegerAttribute(
        @Contextual override val key: Key,
        override var value: Int,
    ) : Attribute<Int>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is Int) return false
            value = fromValue
            return true
        }
    }

    @Serializable
    @SerialName("long")
    class LongAttribute(
        @Contextual override val key: Key,
        override var value: Long,
    ) : Attribute<Long>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is Long) return false
            value = fromValue
            return true
        }
    }

    @Serializable
    @SerialName("double")
    class DoubleAttribute(
        @Contextual override val key: Key,
        override var value: Double,
    ) : Attribute<Double>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is Double) return false
            value = fromValue
            return true
        }
    }

    @Serializable
    @SerialName("float")
    class FloatAttribute(
        @Contextual override val key: Key,
        override var value: Float,
    ) : Attribute<Float>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is Float) return false
            value = fromValue
            return true
        }
    }

    @Serializable
    @SerialName("uuid")
    class UuidAttribute(
        @Contextual override val key: Key,
        @Contextual override var value: UUID,
    ) : Attribute<UUID>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is UUID) return false
            value = fromValue
            return true
        }
    }

    @Serializable
    @SerialName("instant")
    class InstantAttribute(
        @Contextual override val key: Key,
        @Contextual override var value: Instant,
    ) : Attribute<Instant>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is Instant) return false
            value = fromValue
            return true
        }
    }

    @Serializable
    @SerialName("duration")
    class DurationAttribute(
        @Contextual override val key: Key,
        @Contextual override var value: Duration,
    ) : Attribute<Duration>() {
        override fun <TFromValue : Any> updateFrom(fromValue: TFromValue): Boolean {
            if (fromValue !is Duration) return false
            value = fromValue
            return true
        }
    }
}
