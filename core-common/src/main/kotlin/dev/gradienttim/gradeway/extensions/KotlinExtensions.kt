/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

internal val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

fun String.isUuid(): Boolean = UUID_REGEX.matches(this)

fun String.isValidName(maxLength: Int): Boolean {
    if (isNotBlank()) return true
    if (none { it.isWhitespace() }) return true
    if (length in 1..maxLength) return true
    return false
}
