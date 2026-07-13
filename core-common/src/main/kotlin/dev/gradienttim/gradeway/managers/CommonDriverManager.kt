/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.driver.Driver
import dev.gradienttim.gradeway.driver.DriverConfig
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.extensions.createDirectoryIfNotExists
import dev.gradienttim.gradeway.ksp.resolvers.DriverResolver
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipFile

class CommonDriverManager(val gradeway: CommonGradeway) : DriverManager {
    private val drivers = mutableSetOf<Driver>()
    private val directory = gradeway.directory.createDirectoryIfNotExists("drivers")

    override fun load(): Either<Throwable, Unit> = either {
        try {
            directory.listFiles { it.extension == "jar" }.forEach { file ->
                loadDriver(file)
            }
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun unload(): Either<Throwable, Unit> = either {
        try {
            drivers.forEach { driver ->
                driver.unload()
                driver.classLoader?.close()
            }
            drivers.clear()
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun findDriver(id: String, type: DriverType): Driver? {
        return drivers.find { driver ->
            val (configId, configType) = driver.config
            configId == id && configType == type
        }
    }

    override fun registerDriver(id: String, type: DriverType, driver: Driver): Boolean {
        driver.config = DriverConfig(id = id, type = type, entry = driver::class.qualifiedName ?: id)

        if (!drivers.add(driver)) {
            gradeway.logger.warn("Unable to register driver '$id': Driver already registered.")
            return false
        }

        driver.load()
        gradeway.logger.info("Registered driver '$id'.")
        return true
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadDriver(file: File) {
        val zipFile = ZipFile(file)
        val configEntry = zipFile.getEntry(DriverResolver.DRIVER_CONFIG_FILE)
        if (configEntry == null) {
            gradeway.logger.error("Unable to load driver '${file.name}': No driver config found.")
            return
        }

        val configStream = zipFile.getInputStream(configEntry)
        if (configStream == null) {
            gradeway.logger.error("Unable to load driver '${file.name}': Failed to open stream of config.")
            return
        }

        try {
            val config = configStream.use { Json.decodeFromStream<DriverConfig>(it) }
            val driverClassLoader = URLClassLoader(
                arrayOf(file.toURI().toURL()),
                this::class.java.classLoader
            )

            val driverClass = Class.forName(config.entry, true, driverClassLoader)
            val driver = driverClass.getDeclaredConstructor().newInstance() as Driver

            driver.config = config
            driver.classLoader = driverClassLoader

            if (!drivers.add(driver)) {
                gradeway.logger.warn("Unable to load driver '${file.name}': Driver already loaded.")
                return
            }

            driver.load()
            gradeway.logger.info("Loaded driver '${file.name}'.")
        } catch (throwable: Throwable) {
            gradeway.logger.error("Unable to load driver '${file.name}': ${throwable.localizedMessage}")
        } finally {
            zipFile.close()
        }
    }
}
