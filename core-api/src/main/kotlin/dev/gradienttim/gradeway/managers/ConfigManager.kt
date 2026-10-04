/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import dev.gradienttim.gradeway.configs.BaseConfig
import dev.gradienttim.gradeway.configs.DriversConfig
import dev.gradienttim.gradeway.configs.GradewayConfig
import dev.gradienttim.gradeway.configs.PlatformConfig
import dev.gradienttim.gradeway.utilities.lifecycle.Loadable
import kotlinx.serialization.KSerializer

/**
 * Interface for managing the application's configuration system.
 *
 * The `ConfigManager` interface provides access to the application's configuration
 * details through the `config` property. It also extends the `Loadable` interface,
 * suggesting that implementations of this interface require a loading mechanism
 * to initialize or prepare the configuration before it can be used.
 *
 * Implementers of this interface may include functionalities such as parsing configuration
 * files, validating configuration data, or dynamically updating configuration at runtime.
 */
interface ConfigManager<TPlatformConfig : PlatformConfig> : Loadable {
    /**
     * Represents a configuration entry for managing and persisting Gradeway-specific settings.
     *
     * The `gradewayEntry` variable provides an instance of `ConfigEntry` that encapsulates
     * the `GradewayConfig` object. This configuration includes options like message prefix,
     * color schemes, and import size constraints which are essential for customizing and
     * managing the behavior of the Gradeway framework.
     *
     * Through the `ConfigEntry` interface, the `gradewayEntry` facilitates loading and saving
     * of the `GradewayConfig`, ensuring that the configuration is persistently stored and
     * retrievable across application lifecycles.
     */
    val gradewayEntry: ConfigEntry<GradewayConfig>

    /**
     * Configuration entry for managing driver-specific settings within the Gradeway framework.
     *
     * The `driversEntry` variable represents a configuration entry for the `DriversConfig` class,
     * which encapsulates driver-related settings for both database and messaging systems. This entry
     * allows the loading and saving of configuration data related to drivers. The `config` property
     * within the entry provides access to the configuration object, enabling retrieval and modification
     * of driver-specific settings.
     *
     * This entry is used as part of the configuration management system, ensuring that driver settings
     * are persisted and can be dynamically managed and updated during runtime.
     */
    val driversEntry: ConfigEntry<DriversConfig>

    /**
     * Represents the configuration entry for platform-specific settings and properties.
     *
     * This variable is a specific instance of the `ConfigEntry` interface, specialized with
     * the `TPlatformConfig` type. It provides access to platform configuration data and
     * operations such as loading and saving the configuration.
     *
     * Typical use cases for this configuration entry include:
     * - Accessing platform-specific configurations in a structured way.
     * - Persisting platform-related settings to storage or loading them from storage.
     * - Handling potential errors that may occur during configuration loading or saving
     *   through the use of the `Either` type.
     *
     * The `platformEntry` serves as a centralized point for managing platform settings
     * within the system.
     */
    val platformEntry: ConfigEntry<TPlatformConfig>

    /**
     * Represents a configuration entry that provides mechanisms for loading and saving
     * configuration data of type [TConfig].
     *
     * This interface is designed to be used within systems that require configurable
     * components, allowing them to load configurations from a data source and persist
     * changes back to the source.
     *
     * @param TConfig The type of the configuration class, which must extend [BaseConfig].
     */
    interface ConfigEntry<TConfig : BaseConfig> {
        /**
         * Represents the current configuration instance of type [TConfig].
         *
         * This property holds the in-memory representation of the configuration,
         * which can be dynamically loaded or saved through the operations defined
         * within the containing class or interface.
         *
         * The [TConfig] type must extend [BaseConfig], ensuring that it adheres
         * to the required structure and functionality for configuration management.
         */
        val config: TConfig

        /**
         * Defines the serializer used for transforming configuration data into a serializable format and vice versa.
         *
         * This property serves as the serialization mechanism for the [ConfigEntry] class, enabling the encoding and decoding
         * of configuration data. The serializer is specifically typed to work with `TConfig`, the generic type representing
         * the configuration structure.
         *
         * It ensures compatibility with external serialization libraries, facilitating persistence and retrieval of
         * configuration data in formats such as JSON, XML, or other supported serialization formats.
         */
        val serializer: KSerializer<TConfig>

        /**
         * Loads the configuration data from the source.
         *
         * This method attempts to retrieve and populate the current configuration instance
         * from the underlying data source. It encapsulates the outcome in an `Either` type,
         * representing a success or a failure scenario.
         *
         * @return An `Either` containing a `Throwable` in case of a failure during the load
         *         operation, or `Unit` if the configuration is successfully loaded.
         */
        fun load(): Either<Throwable, Unit>

        /**
         * Persists the current configuration state back to the source.
         *
         * This method attempts to save the current in-memory configuration represented by the [config]
         * property to the underlying data source. The outcome of the save operation is encapsulated in
         * an `Either` type, allowing for safe handling of both success and failure scenarios.
         *
         * @return An `Either` that contains a `Throwable` if an error occurs during the save
         *         operation, or `Unit` if the configuration is successfully saved.
         */
        fun save(): Either<Throwable, Unit>
    }
}
