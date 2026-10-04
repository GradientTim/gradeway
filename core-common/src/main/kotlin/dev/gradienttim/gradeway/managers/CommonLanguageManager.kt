/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.extensions.cleanUnusedKeys
import dev.gradienttim.gradeway.extensions.createDirectoryIfNotExists
import dev.gradienttim.gradeway.extensions.fillMissingKeys
import dev.gradienttim.gradeway.extensions.replacePositionalArguments
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore
import net.kyori.adventure.translation.GlobalTranslator
import java.io.InputStreamReader
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

class CommonLanguageManager(val gradeway: CommonGradeway<*>) : LanguageManager {
    private val directory = gradeway.directory.createDirectoryIfNotExists(
        name = "languages",
        requiresRead = true,
        requiresWrite = true
    )
    private lateinit var translator: MiniMessageTranslationStore

    override fun load(): Either<Throwable, Unit> = either {
        try {
            saveResourceLanguages()

            translator = MiniMessageTranslationStore.create(Key.key("gradeway", "languages"), gradeway.miniMessage)

            val availableLocales = Locale.availableLocales().toList()
            directory.listDirectoryEntries("*.properties").forEach { filePath ->
                val name = filePath.name.removeSuffix(".properties")
                val locale = Locale.of(name)

                if (!availableLocales.contains(locale)) {
                    gradeway.logger.warn("Skipping registering locale '$name'. Locale is not available.")
                    return@forEach
                }

                val properties = Properties()
                filePath.inputStream().use { properties.load(it) }

                val entries = properties.entries
                    .associate { (key, value) -> key.toString() to value.toString() }

                runCatching {
                    translator.registerAll(locale, entries)
                }
            }

            if (!GlobalTranslator.translator().addSource(translator)) {
                gradeway.logger.error("Failed to add MiniMessage translation store.")
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun unload(): Either<Throwable, Unit> = either {
        try {
            if (::translator.isInitialized) {
                GlobalTranslator.translator().removeSource(translator)
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun reload(): Either<Throwable, Unit> = either {
        unload()
            .onLeft { raise(it) }
            .onRight {
                load().bind()
            }
    }

    private fun saveResourceLanguages() {
        val uri = this::class.java.classLoader.getResource("languages")?.toURI()
            ?: error("No local language files found.")

        if (uri.scheme == "jar") {
            FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use { fileSystem ->
                copyResourceLanguages(fileSystem.getPath("/languages"))
            }
        } else {
            copyResourceLanguages(uri.toPath())
        }
    }

    private fun copyResourceLanguages(languagesPath: Path) {
        languagesPath.listDirectoryEntries().filter { it.extension == "properties" }.forEach { path ->
            val translationFile = directory.resolve(path.name)
            if (translationFile.exists()) {
                updateTranslationFile(path, translationFile)
                return@forEach
            }
            saveResourceTranslationFile(path, translationFile)
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
        val templateProperties = Properties()
        val destinationProperties = Properties()

        source.inputStream().use { templateProperties.load(InputStreamReader(it)) }
        destination.inputStream().use { destinationProperties.load(InputStreamReader(it)) }

        val unusedKeysCount = destinationProperties.cleanUnusedKeys(templateProperties)
        val missingKeysCount = destinationProperties.fillMissingKeys(templateProperties)
        val outdatedKeysCount = destinationProperties.replacePositionalArguments(templateProperties)

        if (unusedKeysCount > 0 || missingKeysCount > 0 || outdatedKeysCount > 0) {
            gradeway.logger.info(
                "Updated translation file: ${destination.name}. " +
                        "Unused keys: $unusedKeysCount, missing keys: $missingKeysCount, " +
                        "outdated keys: $outdatedKeysCount"
            )

            destination.outputStream().use { destinationProperties.store(it, "Updated unused/missing/outdated keys") }
        }
    }
}
