/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.meta

/**
 * Enum representing the types of drivers within the system.
 *
 * The `DriverType` enum is used to categorize drivers based on their functional purpose.
 * These types are typically used with the `CreateDriver` annotation to define
 * drivers and their respective functionalities, such as database or messaging capabilities.
 */
enum class DriverType {
    /**
     * Represents a driver type for database-related functionality.
     *
     * This enum constant is used to identify drivers that interact with databases. It is often used
     * in conjunction with the `CreateDriver` annotation to mark and configure database drivers within the system.
     */
    DATABASE,

    /**
     * Represents the MESSAGING driver type in the system.
     *
     * This driver type is used for messaging-related functionalities, such as
     * creating, configuring, and managing messaging brokers. Drivers of this type
     * enable distributed communication between system components using various
     * messaging protocols or services.
     *
     * The MESSAGING driver type is commonly used in workflows that require
     * event notifications, message dispatching, or inter-service communication.
     * It supports integration with specific messaging frameworks or platforms
     * through concrete implementations, such as Redis or plugin messaging systems.
     */
    MESSAGING
}
