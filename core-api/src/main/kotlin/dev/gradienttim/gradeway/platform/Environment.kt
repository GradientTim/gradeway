/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.platform

interface Environment {
    /**
     * Retrieves an integer value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The integer value associated with the first matching name, or null if none is found.
     */
    fun int(vararg names: String): Int?

    /**
     * Retrieves a long value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The long value associated with the first matching name, or null if none is found.
     */
    fun long(vararg names: String): Long?

    /**
     * Retrieves a double value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The double value associated with the first matching name, or null if none is found.
     */
    fun double(vararg names: String): Double?

    /**
     * Retrieves a string value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The string value associated with the first matching name, or null if none is found.
     */
    fun string(vararg names: String): String?

    /**
     * Retrieves a boolean value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The boolean value associated with the first matching name, or null if none is found.
     */
    fun boolean(vararg names: String): Boolean?

    /**
     * Retrieves a required integer value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The integer value associated with the first matching name.
     * @throws IllegalStateException if no matching name is found in the environment.
     */
    fun intRequired(vararg names: String): Int

    /**
     * Retrieves a required long value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The long value associated with the first matching name.
     * @throws IllegalStateException if no matching name is found in the environment.
     */
    fun longRequired(vararg names: String): Long

    /**
     * Retrieves a required double value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The double value associated with the first matching name.
     * @throws IllegalStateException if no matching name is found in the environment.
     */
    fun doubleRequired(vararg names: String): Double

    /**
     * Retrieves a required string value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The string value associated with the first matching name.
     * @throws IllegalStateException if no matching name is found in the environment.
     */
    fun stringRequired(vararg names: String): String

    /**
     * Retrieves a required boolean value from the environment by searching the given names in order.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @return The boolean value associated with the first matching name.
     * @throws IllegalStateException if no matching name is found in the environment.
     */
    fun booleanRequired(vararg names: String): Boolean

    /**
     * Retrieves an integer value from the environment, returning a default if no match is found.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @param default The value to return if no matching name is found.
     * @return The integer value associated with the first matching name, or [default].
     */
    fun intDefault(vararg names: String, default: Int): Int = int(*names) ?: default

    /**
     * Retrieves a long value from the environment, returning a default if no match is found.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @param default The value to return if no matching name is found.
     * @return The long value associated with the first matching name, or [default].
     */
    fun longDefault(vararg names: String, default: Long): Long = long(*names) ?: default

    /**
     * Retrieves a double value from the environment, returning a default if no match is found.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @param default The value to return if no matching name is found.
     * @return The double value associated with the first matching name, or [default].
     */
    fun doubleDefault(vararg names: String, default: Double): Double = double(*names) ?: default

    /**
     * Retrieves a string value from the environment, returning a default if no match is found.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @param default The value to return if no matching name is found.
     * @return The string value associated with the first matching name, or [default].
     */
    fun stringDefault(vararg names: String, default: String): String = string(*names) ?: default

    /**
     * Retrieves a boolean value from the environment, returning a default if no match is found.
     *
     * @param names The names to look up, searched in the order they are provided.
     * @param default The value to return if no matching name is found.
     * @return The boolean value associated with the first matching name, or [default].
     */
    fun booleanDefault(vararg names: String, default: Boolean): Boolean = boolean(*names) ?: default
}
