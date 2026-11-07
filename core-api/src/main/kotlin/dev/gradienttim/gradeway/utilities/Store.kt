/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.utilities.Store.SetError

/**
 * Represents a generic storage abstraction with basic key-value management capabilities.
 *
 * This interface defines operations for storing, retrieving, and removing values associated with keys,
 * as well as clearing the entire store. The implementation is expected to define how the key-value
 * mapping is maintained and persisted.
 *
 * @param TKey The type of keys used to associate values in the store.
 * @param TValue The type of values that will be stored and managed.
 */
interface Store<in TKey, TValue> {
    /**
     * Associates the specified value with the given key in the store.
     *
     * If the key already exists, its associated value will be updated. If the operation
     * fails, an appropriate `SetError` is returned.
     *
     * @param key The key with which the specified value is to be associated.
     * @param value The value to be associated with the specified key.
     * @return An `Either` containing `true` if the value was stored, `false` if the store could not hold
     * a value for this key (e.g. the key refers to something that is currently unavailable), or a
     * `SetError` if an error occurs.
     */
    fun set(
        key: TKey,
        value: TValue,
    ): Either<SetError, Boolean>

    /**
     * Removes the entry associated with the specified key from the store.
     *
     * If the key exists, it will be removed. If an error occurs during the operation,
     * an `UnsetError` is returned.
     *
     * @param key The key whose associated entry is to be removed from the store.
     * @return An `Either` containing `Boolean` indicating the success of the operation,
     * or an `UnsetError` if an error occurs.
     */
    fun unset(
        key: TKey,
    ): Either<UnsetError, Boolean>

    /**
     * Retrieves the value associated with the specified key from the store.
     *
     * If the key exists, its associated value is returned. If the key does not exist,
     * this method returns null.
     *
     * @param key The key whose associated value is to be retrieved from the store.
     * @return The value associated with the specified key, or null if the key does not exist.
     */
    fun get(
        key: TKey,
    ): TValue?

    /**
     * Clears all entries from the store.
     *
     * This operation attempts to remove all keys and their associated values
     * from the store. If an error occurs during the process, a `ClearError`
     * indicating the failure will be returned.
     *
     * @return An `Either` containing `Unit` if the operation is successful,
     * or a `ClearError` if an error occurs.
     */
    fun clear(): Either<ClearError, Unit>

    sealed interface SetError {
        data class Unexpected(val throwable: Throwable) : SetError
    }

    sealed interface UnsetError {
        data class Unexpected(val throwable: Throwable) : UnsetError
    }

    sealed interface ClearError {
        data class Unexpected(val throwable: Throwable) : ClearError
    }
}

/**
 * Toggles the boolean value associated with a given key in the store.
 *
 * If the key does not exist or is associated with null, it is treated as `false`.
 * The new value is stored and then returned.
 *
 * @param key The key whose associated boolean value is to be toggled.
 * @return An `Either` containing the new boolean value after toggling, or a `SetError` if storing it
 * fails or the store could not hold a value for this key.
 */
fun <TKey> Store<TKey, Boolean>.toggle(key: TKey): Either<SetError, Boolean> = either {
    val newValue = !(get(key) ?: false)
    if (!set(key, newValue).bind()) {
        raise(SetError.Unexpected(IllegalStateException("The value for key '$key' could not be stored.")))
    }
    newValue
}
