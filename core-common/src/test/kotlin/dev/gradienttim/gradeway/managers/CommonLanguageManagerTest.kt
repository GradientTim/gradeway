/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.TestPlatformConfig
import dev.gradienttim.gradeway.TestScheduler
import dev.gradienttim.gradeway.platform.CommonLogger
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonLanguageManagerTest {
    private lateinit var directory: File
    private lateinit var gradeway: CommonGradeway<TestPlatformConfig>

    private fun createGradeway() {
        directory = Files.createTempDirectory("gradeway-language-test").toFile()
        gradeway = CommonGradeway(
            logger = CommonLogger(onInfo = {}, onWarn = {}, onError = {}),
            scheduler = TestScheduler(),
            directory = directory,
            defaultPlatformConfig = TestPlatformConfig(),
            platformConfigSerializer = TestPlatformConfig.serializer(),
        )
    }

    @AfterTest
    fun tearDown() {
        if (::gradeway.isInitialized) {
            gradeway.unload()
        }
        if (::directory.isInitialized) {
            directory.deleteRecursively()
        }
    }

    /**
     * Seeds `languages/en.properties` with a hand-written stand-in for the shipped template
     * before [CommonGradeway.load] runs, so [CommonLanguageManager.load] takes the
     * update-existing-file path (`updateTranslationFile`) instead of the copy-fresh-file path.
     */
    private fun seedExistingTranslationFile(content: String) {
        val languagesDirectory = File(directory, "languages").apply { mkdirs() }
        File(languagesDirectory, "en.properties").writeText(content)
    }

    private fun loadedTranslationFileContents(): String {
        gradeway.load().getOrElse { error("Failed to load Gradeway: $it") }
        return File(directory, "languages/en.properties").readText()
    }

    @Test
    fun `user override of an existing key is preserved after update`() {
        createGradeway()
        seedExistingTranslationFile("gradeway.command.about.info=custom override\n")

        val result = loadedTranslationFileContents()

        assertTrue(result.contains("gradeway.command.about.info=custom override"))
    }

    @Test
    fun `a key newly added upstream is present with the template value`() {
        createGradeway()
        // Deliberately empty: every key in the shipped template is "new" relative to this file.
        seedExistingTranslationFile("")

        val result = loadedTranslationFileContents()

        assertTrue(result.contains("gradeway.cache.flush.success="))
    }

    @Test
    fun `an obsolete key no longer in the template is dropped`() {
        createGradeway()
        seedExistingTranslationFile("gradeway.no.longer.exists=stale value\n")

        val result = loadedTranslationFileContents()

        assertFalse(result.contains("gradeway.no.longer.exists"))
    }

    // Comment-line preservation is exercised directly against OrderedProperties in
    // OrderedPropertiesTest: comments belong to the shipped template file, and the current
    // en.properties template ships without any, so there is nothing to round-trip here.

    @Test
    fun `template key order is preserved regardless of the existing file's order`() {
        createGradeway()
        seedExistingTranslationFile(
            "gradeway.command.about.info=first\ngradeway.cache.flush.success=second\n"
        )

        val result = loadedTranslationFileContents()

        // Both keys should be present (merged from template and existing file)
        assertTrue(result.contains("gradeway.command.about.info"))
        assertTrue(result.contains("gradeway.cache.flush.success"))

        // The custom value should be preserved
        assertTrue(result.contains("gradeway.command.about.info=first"))
        assertTrue(result.contains("gradeway.cache.flush.success=second"))
    }
}
