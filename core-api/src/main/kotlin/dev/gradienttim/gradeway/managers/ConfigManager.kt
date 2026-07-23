/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.config.GradewayConfig
import dev.gradienttim.gradeway.utilities.lifecycle.Loadable

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
interface ConfigManager<TPlatformConfig> : Loadable {
    /**
     * Represents the primary configuration object for the application.
     *
     * This property provides access to the application's global settings and configurations,
     * which are encapsulated within the `GradewayConfig` class. The configuration includes
     * details such as database settings, messaging service configurations, appearance customization,
     * and environment variable management. Implementations of the containing interface
     * `ConfigManager` typically handle the loading, validation, and updating of this configuration.
     *
     * Usage of this property assumes that the loading mechanism has been invoked to initialize
     * the configuration before accessing its properties.
     */
    val config: GradewayConfig<TPlatformConfig>
}
