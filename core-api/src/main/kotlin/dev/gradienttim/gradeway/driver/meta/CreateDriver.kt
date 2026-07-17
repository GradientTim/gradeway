/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.meta

/**
 * Annotation to define a driver class within the system.
 *
 * This annotation is used to mark classes that serve as drivers, specifying their unique identifier
 * and the type of functionality they provide, such as database or messaging capabilities.
 *
 * @property id The unique identifier for the driver. This ID is used to reference the driver in the system.
 * @property type The type of the driver, defining its category (e.g., database or messaging).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class CreateDriver(
    val id: String,
    val type: DriverType,
)
