/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.constants

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.serializers.AdventureKeySerializer
import dev.gradienttim.gradeway.serializers.JavaDurationSerializer
import dev.gradienttim.gradeway.serializers.JavaInstantSerializer
import dev.gradienttim.gradeway.serializers.JavaUuidSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.kyori.adventure.key.Key
import java.time.Duration
import java.time.Instant
import java.util.*

object SerializationConstants {
    val JSON = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true

        serializersModule = SerializersModule {
            contextual(Key::class, AdventureKeySerializer)
            contextual(UUID::class, JavaUuidSerializer)
            contextual(Instant::class, JavaInstantSerializer)
            contextual(Duration::class, JavaDurationSerializer)

            polymorphic(Attribute::class) {
                subclass(Attribute.StringAttribute::class)
                subclass(Attribute.CharAttribute::class)
                subclass(Attribute.BooleanAttribute::class)
                subclass(Attribute.IntegerAttribute::class)
                subclass(Attribute.LongAttribute::class)
                subclass(Attribute.DoubleAttribute::class)
                subclass(Attribute.FloatAttribute::class)
                subclass(Attribute.ShortAttribute::class)
                subclass(Attribute.ByteAttribute::class)
                subclass(Attribute.UuidAttribute::class)
                subclass(Attribute.InstantAttribute::class)
                subclass(Attribute.DurationAttribute::class)
            }
        }
    }
}
