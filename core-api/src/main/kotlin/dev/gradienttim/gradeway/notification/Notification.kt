/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification

import dev.gradienttim.gradeway.utilities.Typed
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface Notification : Typed {
    fun toJson(): JsonObject = buildJsonObject {
        put("type", this@Notification.type)
    }
}
