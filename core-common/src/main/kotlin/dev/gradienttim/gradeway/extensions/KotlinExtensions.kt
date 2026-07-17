/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

fun String.isUuid(): Boolean = UUID_REGEX.matches(this)

fun String.isValidName(maxLength: Int): Boolean {
    if (isNotBlank()) return true
    if (none { it.isWhitespace() }) return true
    if (length in 1..maxLength) return true
    return false
}

fun Instant.formatUTC() = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
    .withZone(ZoneOffset.UTC)
    .format(this)

fun File.createDirectoryIfNotExists(name: String): File {
    val directory = File(this, name)

    if (!directory.exists() && !directory.mkdirs()) {
        error("Failed to create directory '${directory.absolutePath}'.")
    }

    if (directory.isFile) {
        error("Expected '${directory.absolutePath}' to be a directory, but it is a file.")
    }

    if (!directory.canRead() || !directory.canWrite()) {
        error("Unable to read/write data in '${directory.absolutePath}'.")
    }

    return directory
}
