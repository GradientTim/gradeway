/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore
import net.kyori.adventure.text.flattener.ComponentFlattener
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import java.util.Locale
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText
import kotlin.test.*

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
        val directory = Files.createTempDirectory("resolve-test")
        val resolved = directory.resolveWithinDirectory("backup.tar.gz")

        assertNotNull(resolved)
        assertEquals(directory.resolve("backup.tar.gz").normalize(), resolved)
    }

    @Test
    fun `resolveWithinDirectory rejects path traversal`() {
        val directory = Files.createTempDirectory("resolve-test")

        assertNull(directory.resolveWithinDirectory("../../etc/passwd"))
        assertNull(directory.resolveWithinDirectory("/etc/passwd"))
    }

    @Test
    fun `createDirectoryIfNotExists creates the directory and is idempotent`() {
        val parent = Files.createTempDirectory("create-dir-test")

        val first = parent.createDirectoryIfNotExists("child", requiresRead = true, requiresWrite = true)
        assertTrue(first.exists())
        assertTrue(first.isDirectory())

        val second = parent.createDirectoryIfNotExists("child", requiresRead = true, requiresWrite = true)
        assertEquals(first, second)
    }

    @Test
    fun `createDirectoryIfNotExists throws if the target path is a file`() {
        val parent = Files.createTempDirectory("create-dir-test")
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

    @Test
    fun `replacePositionalArguments replaces values still using positional arguments`() {
        val template = Properties().apply {
            this["named"] = "<cache> flushed"
            this["positional"] = "<arg:0> flushed"
            this["customized"] = "<cache> flushed"
        }
        val destination = Properties().apply {
            this["named"] = "<arg:0> flushed"
            this["positional"] = "custom <arg:0>"
            this["customized"] = "custom <cache>"
            this["unknown"] = "<arg:0>"
        }

        assertEquals(1, destination.replacePositionalArguments(template))
        assertEquals("<cache> flushed", destination["named"])
        assertEquals("custom <arg:0>", destination["positional"])
        assertEquals("custom <cache>", destination["customized"])
        assertEquals("<arg:0>", destination["unknown"])
    }

    @Test
    fun `named translation arguments are resolved by the MiniMessage translation store`() {
        val store = MiniMessageTranslationStore.create(Key.key("gradeway", "test"))
        store.register("test.flush", Locale.US, "Cache <cache> failed: <error>")

        val component = Component.translatable(
            "test.flush",
            Argument.string("cache", "ALL"),
            Argument.string("error", "<red>boom")
        )
        val translated = store.translate(component, Locale.US)

        assertNotNull(translated)
        val text = StringBuilder()
        ComponentFlattener.basic().flatten(translated) { text.append(it) }
        assertEquals("Cache ALL failed: <red>boom", text.toString())
    }
}
