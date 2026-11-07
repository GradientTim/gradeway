/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key

/**
 * Serializes a [Key] as its `namespace:value` string representation.
 */
object KeySerializer : KSerializer<Key> {
    override val descriptor = PrimitiveSerialDescriptor("gradeway.Key", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Key) = encoder.encodeString(value.asString())

    override fun deserialize(decoder: Decoder): Key = Key.key(decoder.decodeString())
}
