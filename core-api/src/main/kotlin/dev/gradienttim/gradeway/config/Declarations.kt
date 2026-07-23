/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.config

import dev.gradienttim.gradeway.serializers.TomlAnySerializer
import kotlinx.serialization.Serializable

typealias Variables = Map<String, @Serializable(with = TomlAnySerializer::class) Any>
