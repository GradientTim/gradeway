/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private val PRETTY_JSON = Json { prettyPrint = true }

internal fun JsonObject.toPrettyString(maxLength: Int): String {
    val text = PRETTY_JSON.encodeToString(JsonObject.serializer(), this)
    return if (text.length <= maxLength) text else text.take(maxLength - 1) + "…"
}
