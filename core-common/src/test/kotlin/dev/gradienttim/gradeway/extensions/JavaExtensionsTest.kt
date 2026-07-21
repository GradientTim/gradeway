/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JavaExtensionsTest {
    @Test
    fun `isUuid accepts valid uuids and rejects everything else`() {
        assertTrue("123e4567-e89b-12d3-a456-426614174000".isUuid())
        assertTrue("123E4567-E89B-12D3-A456-426614174000".isUuid())
        assertFalse("not-a-uuid".isUuid())
        assertFalse("123e4567e89b12d3a456426614174000".isUuid())
        assertFalse("".isUuid())
    }

    @Test
    fun `isNameValid rejects blank and over-length names`() {
        assertFalse("".isNameValid(10))
        assertFalse("   ".isNameValid(10))
        assertFalse("a".repeat(11).isNameValid(10))
        assertTrue("a".repeat(10).isNameValid(10))
        assertTrue("valid".isNameValid(10))
    }

    @Test
    fun `formatUTC renders the expected pattern`() {
        val instant = Instant.parse("2024-01-15T10:30:00Z")
        assertEquals("2024-01-15 10:30:00 UTC", instant.formatUTC())
    }

    @Test
    fun `resolveWithinDirectory resolves a normal child path`() {
        val directory = Files.createTempDirectory("resolve-test").toFile()
        val resolved = directory.resolveWithinDirectory("backup.tar.gz")

        assertNotNull(resolved)
        assertEquals(directory.toPath().resolve("backup.tar.gz").normalize().toFile(), resolved)
    }

    @Test
    fun `resolveWithinDirectory rejects path traversal`() {
        val directory = Files.createTempDirectory("resolve-test").toFile()

        assertNull(directory.resolveWithinDirectory("../../etc/passwd"))
        assertNull(directory.resolveWithinDirectory("/etc/passwd"))
    }

    @Test
    fun `createDirectoryIfNotExists creates the directory and is idempotent`() {
        val parent = Files.createTempDirectory("create-dir-test").toFile()

        val first = parent.createDirectoryIfNotExists("child", requiresRead = true, requiresWrite = true)
        assertTrue(first.exists())
        assertTrue(first.isDirectory)

        val second = parent.createDirectoryIfNotExists("child", requiresRead = true, requiresWrite = true)
        assertEquals(first, second)
    }

    @Test
    fun `createDirectoryIfNotExists throws if the target path is a file`() {
        val parent = Files.createTempDirectory("create-dir-test").toFile()
        val conflictingFile = parent.resolve("child")
        conflictingFile.writeText("not a directory")

        assertFailsWith<IllegalStateException> {
            parent.createDirectoryIfNotExists("child")
        }
    }

    @Test
    fun `limitedTo allows reads under the limit`() {
        val data = "hello".toByteArray()
        val limited = data.inputStream().limitedTo(data.size.toLong())

        assertEquals("hello", limited.readBytes().decodeToString())
    }

    @Test
    fun `limitedTo throws once the byte limit is exceeded`() {
        val data = "hello world".toByteArray()
        val limited = data.inputStream().limitedTo(5)

        assertFailsWith<IOException> {
            limited.readBytes()
        }
    }
}
