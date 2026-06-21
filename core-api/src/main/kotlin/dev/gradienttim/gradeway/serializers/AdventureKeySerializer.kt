/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.serializers

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key

@OptIn(InternalSerializationApi::class)
object AdventureKeySerializer : KSerializer<Key> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("AdventureKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Key) {
        encoder.encodeString(value.asString())
    }

    override fun deserialize(decoder: Decoder): Key {
        return Key.key(decoder.decodeString())
    }
}
