/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import dev.gradienttim.gradeway.platform.CommonEnvironment

internal inline fun <reified T> CommonEnvironment.get(
    vararg names: String,
    transform: (Any) -> T?,
): T? {
    for (name in names) {
        val value = resolveVariableValue(name) ?: continue
        return transform(value) ?: error("Failed to convert '$name' to ${T::class.simpleName}.")
    }
    return null
}
