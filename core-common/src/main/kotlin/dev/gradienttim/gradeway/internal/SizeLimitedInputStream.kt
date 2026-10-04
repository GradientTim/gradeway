/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.internal

import java.io.IOException
import java.io.InputStream

internal class SizeLimitedInputStream(
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
