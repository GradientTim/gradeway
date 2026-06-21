/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.ksp.resolvers

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import dev.gradienttim.gradeway.driver.DriverConfig
import dev.gradienttim.gradeway.driver.meta.CreateDriver
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream

class DriverResolver(
    private val environment: SymbolProcessorEnvironment,
) {
    @OptIn(KspExperimental::class, ExperimentalSerializationApi::class)
    @Suppress("ReturnCount")
    fun process(resolver: Resolver) {
        val symbols = resolver.getSymbolsWithAnnotation(DRIVER_ANNOTATION).filterIsInstance<KSClassDeclaration>()
        val symbol = symbols.firstOrNull() ?: return

        val implementsDriver = symbol.superTypes.any {
            it.resolve().declaration.qualifiedName?.asString() == DRIVER_INSTANCE
        }

        if (!implementsDriver) {
            environment.logger.warn(
                "Class ${symbol.simpleName.asString()} has @CreateDriver but does not extends Driver.",
                symbol
            )
            return
        }

        val driverAnnotation = symbol.getAnnotationsByType(CreateDriver::class).firstOrNull() ?: return
        val className = symbol.qualifiedName?.asString() ?: return

        val config = DriverConfig(
            id = driverAnnotation.id,
            type = driverAnnotation.type,
            entry = className,
        )

        val containingFile = symbol.containingFile ?: return
        val fileStream = environment.codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, containingFile),
            packageName = "",
            fileName = DRIVER_CONFIG_NAME,
            extensionName = DRIVER_CONFIG_EXTENSION
        )

        fileStream.use { stream ->
            Json.encodeToStream(config, stream)
        }
    }

    companion object {
        const val DRIVER_INSTANCE: String = "dev.gradienttim.gradeway.driver.Driver"
        const val DRIVER_ANNOTATION: String = "dev.gradienttim.gradeway.driver.meta.CreateDriver"
        const val DRIVER_CONFIG_NAME: String = "driver"
        const val DRIVER_CONFIG_EXTENSION: String = "json"
        const val DRIVER_CONFIG_FILE: String = "$DRIVER_CONFIG_NAME.$DRIVER_CONFIG_EXTENSION"
    }
}
