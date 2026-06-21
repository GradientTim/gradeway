/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.CommonGradeway
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore
import net.kyori.adventure.translation.GlobalTranslator
import org.apache.commons.configuration2.PropertiesConfiguration
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

class CommonLanguageManager(val gradeway: CommonGradeway) : LanguageManager {
    private val directory: Path by lazy {
        val languagesDirectory = gradeway.directory.resolve("languages")
        if (!languagesDirectory.exists()) {
            languagesDirectory.createDirectory()
        }
        languagesDirectory
    }

    private lateinit var translator: MiniMessageTranslationStore

    override fun load() {
        saveResourceLanguages()

        translator = MiniMessageTranslationStore.create(Key.key("gradeway", "languages"))

        val availableLocales = Locale.availableLocales().toList()
        directory.listDirectoryEntries().filter { it.extension == ".properties" }.forEach { path ->
            val name = path.name.removeSuffix(".properties")
            val locale = Locale.of(name)

            if (!availableLocales.contains(locale)) {
                gradeway.logger.warn("Skipping registering locale '$name'. Locale is not available.")
                return@forEach
            }

            val properties = Properties()
            path.inputStream().use { properties.load(it) }

            val entries = properties.entries
                .associate { (key, value) -> key.toString() to value.toString() }

            runCatching {
                translator.registerAll(locale, entries)
            }
        }

        if (!GlobalTranslator.translator().addSource(translator)) {
            gradeway.logger.error("Failed to add MiniMessage translation store.")
        }
    }

    override fun unload() {
        if (::translator.isInitialized) {
            GlobalTranslator.translator().removeSource(translator)
        }
    }

    private fun saveResourceLanguages() {
        val languagesPath = this::class.java.classLoader.getResource("languages")?.toURI()?.toPath()
            ?: error("No local language files found.")

        languagesPath.listDirectoryEntries().filter { it.extension == ".properties" }.forEach { path ->
            val translationFilePath = directory.resolve(path.name)
            if (translationFilePath.exists()) {
                updateTranslationFile(path, translationFilePath)
                return@forEach
            }
            saveResourceTranslationFile(path, translationFilePath)
        }
    }

    private fun saveResourceTranslationFile(source: Path, destination: Path) {
        source.inputStream().use { inputStream ->
            destination.outputStream().use { outputStream ->
                inputStream.transferTo(outputStream)
            }
        }
    }

    private fun updateTranslationFile(source: Path, destination: Path) {
        val templateConfig = PropertiesConfiguration()
        val existingConfig = PropertiesConfiguration()

        source.inputStream().use { templateConfig.read(InputStreamReader(it)) }
        destination.inputStream().use { existingConfig.read(InputStreamReader(it)) }

        val templateKeys = templateConfig.keys.asSequence().toList()
        val existingKeys = existingConfig.keys.asSequence().toList()

        templateKeys.forEach { key ->
            if (existingConfig.containsKey(key)) {
                templateConfig.setProperty(key, existingConfig.getProperty(key))
            }
        }

        existingKeys.forEach { key ->
            if (!templateConfig.containsKey(key)) {
                existingConfig.clearProperty(key)
            }
        }

        destination.writer().use { templateConfig.write(it) }
    }
}
