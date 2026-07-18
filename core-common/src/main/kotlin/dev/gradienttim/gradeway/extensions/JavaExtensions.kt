/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

fun String.isUuid(): Boolean = UUID_REGEX.matches(this)
fun String.isNameValid(maxLength: Int): Boolean = isNotBlank() && length in 1..maxLength

fun Instant.formatUTC(): String = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
    .withZone(ZoneOffset.UTC)
    .format(this)

/**
 * Resolves [fileName] as a child of this directory, rejecting any result that would escape it.
 *
 * This is intended for use whenever a file name originates from an untrusted source, such as a
 * Brigadier command argument supplied by a player. A naive `File(directory, fileName)` join does
 * not protect against path traversal: if `fileName` contains `..` segments or is itself an
 * absolute path, the resulting `File` can point anywhere on the file system rather than staying
 * within the intended directory. This function guards against that by normalizing both the
 * directory and the resolved candidate path and verifying that the candidate still resides
 * underneath the directory before returning it.
 *
 * @param fileName the untrusted, caller-supplied file name to resolve against this directory.
 * @return the resolved [File] within this directory, or `null` if [fileName] would resolve outside it.
 */
fun File.resolveWithinDirectory(fileName: String): File? {
    val normalizedDirectory = toPath().normalize()
    val candidate = normalizedDirectory.resolve(fileName).normalize()

    return if (candidate.startsWith(normalizedDirectory)) candidate.toFile() else null
}

/**
 * Creates a directory with the specified name within the current file path if it does not already exist.
 * Performs additional checks for read and write permissions if required.
 *
 * @param name The name of the directory to be created.
 * @param requiresRead Set to true if the directory should be checked for read permissions.
 * @param requiresWrite Set to true if the directory should be checked for write permissions.
 * @return The File object representing the created or existing directory.
 * @throws IllegalStateException If the directory could not be created, if it exists but is a file,
 * or if the required read or write permissions are not met.
 */
fun File.createDirectoryIfNotExists(
    name: String,
    requiresRead: Boolean = false,
    requiresWrite: Boolean = false
): File {
    val directory = File(this, name)

    if (!directory.exists() && !directory.mkdirs()) {
        error("Failed to create directory '${directory.absolutePath}'.")
    }

    if (directory.isFile) {
        error("Expected '${directory.absolutePath}' to be a directory, but it is a file.")
    }

    if (requiresRead && !directory.canRead()) {
        error("Unable to read data from '${directory.absolutePath}'.")
    }

    if (requiresWrite && !directory.canWrite()) {
        error("Unable to write data in '${directory.absolutePath}'.")
    }

    return directory
}

/**
 * Wraps this stream so that reading more than [maxBytes] from it throws an [IOException].
 *
 * This is intended for use whenever a stream's content originates from an untrusted source and
 * may itself be, or sit downstream of, a decompression step, such as reading a gzip-compressed
 * backup or migration archive supplied by an admin. A crafted archive can be tiny on the disk while
 * expanding to gigabytes once decompressed ("decompression bomb"). Naively reading such a
 * stream to completion (e.g., to deserialize it as JSON) can exhaust available memory before any
 * validation of its contents happens. Wrapping the stream with this function bounds the amount of
 * data that can ever be read from it, so a malicious or corrupt stream fails fast with a bounded
 * amount of memory used instead of growing without limit.
 *
 * @param maxBytes the maximum number of bytes that may be read from this stream before an [IOException] is thrown.
 * @return an [InputStream] delegating to this stream that enforces the [maxBytes] limit.
 */
fun InputStream.limitedTo(maxBytes: Long): InputStream = SizeLimitedInputStream(this, maxBytes)

private class SizeLimitedInputStream(
    private val delegate: InputStream,
    private val maxBytes: Long,
) : InputStream() {
    private var bytesRead = 0L

    override fun read(): Int {
        val byte = delegate.read()
        if (byte != -1) {
            accumulate(1)
        }
        return byte
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = delegate.read(b, off, len)
        if (count > 0) {
            accumulate(count.toLong())
        }
        return count
    }

    override fun close() = delegate.close()

    private fun accumulate(count: Long) {
        bytesRead += count
        if (bytesRead > maxBytes) {
            throw IOException("Decompressed stream exceeds the maximum allowed size of $maxBytes bytes.")
        }
    }
}
