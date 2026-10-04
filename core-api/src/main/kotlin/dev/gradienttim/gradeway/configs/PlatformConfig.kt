/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.configs

/**
 * Represents the platform-specific configuration for Gradeway.
 *
 * This interface extends the BaseConfig interface, requiring the implementation
 * of the version property to track the configuration schema version. PlatformConfig
 * is intended to serve as a base for defining platform-specific settings that
 * customize the framework's behavior according to the particular platform it operates on.
 */
interface PlatformConfig : BaseConfig {
    companion object {
        const val LATEST_VERSION = 1
    }
}
