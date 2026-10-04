/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.configs

/**
 * Represents the base configuration interface used for configuration classes
 * within the Gradeway framework.
 *
 * Classes implementing this interface are required to define a version property.
 * The version property allows tracking of configuration schema versions, enabling
 * backward compatibility and migration handling for configuration data.
 */
interface BaseConfig {
    var version: Int
}
