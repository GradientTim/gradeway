/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

/**
 * Represents an abstraction for objects that have an associated type.
 *
 * Classes implementing this interface are expected to define a `type` property
 * that provides a string representation of the object's type. This can be used
 * for categorization, identification, or handling polymorphic behavior based on type.
 */
interface Typed {
    /**
     * Represents the type of the object as a string.
     *
     * This property provides a textual categorization or identification
     * of the object's type, typically used for distinguishing between
     * various implementations or instances in systems where polymorphism
     * or type-based behaviors are relevant.
     */
    val type: String
}
