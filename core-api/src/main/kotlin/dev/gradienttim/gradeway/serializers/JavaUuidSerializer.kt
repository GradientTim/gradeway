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
import java.util.*

@OptIn(InternalSerializationApi::class)
object JavaUuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("JavaUuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}
