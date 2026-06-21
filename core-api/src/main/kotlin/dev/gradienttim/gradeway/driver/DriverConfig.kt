/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver

import dev.gradienttim.gradeway.driver.meta.DriverType
import kotlinx.serialization.Serializable

@Serializable
data class DriverConfig(
    val id: String,
    val type: DriverType,
    val entry: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DriverConfig) return false

        return id == other.id && type == other.type
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
