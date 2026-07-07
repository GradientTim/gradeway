/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import kotlinx.serialization.json.JsonObject

/**
 * Represents a contract for serializing objects into JSON format and
 * deserializing JSON data back into objects of a specified type.
 *
 * This interface serves as an abstraction for handling serialization
 * and deserialization processes, ensuring consistent conversion
 * between data representations and domain objects. Implementing classes
 * are expected to define specific mechanisms and rules for converting
 * data to and from `JsonObject` format.
 *
 * @param T The type of object that this interface handles for serialization
 *          and deserialization.
 */
interface Serializable<T> {
    /**
     * Serializes the given instance of type `T` into a `JsonObject` representation.
     *
     * This method is responsible for converting the provided object into a JSON structure
     * that can be stored, transmitted, or processed further. The serialization process
     * should adhere to the rules and format specified by the implementation.
     *
     * @param instance The object of type `T` to be serialized into a `JsonObject`.
     * @return A `JsonObject` representing the serialized form of the input object.
     */
    fun serialize(instance: T): JsonObject

    /**
     * Deserializes the given `JsonObject` into an instance of type `T`.
     *
     * This method is responsible for converting the provided JSON data into a corresponding
     * object of type `T`. The deserialization process should follow the rules and structure
     * expected by the implementation to ensure accurate reconstruction of the object.
     *
     * @param json The `JsonObject` containing the data to be deserialized into an instance of type `T`.
     * @return An instance of type `T` reconstructed from the given `JsonObject`.
     */
    fun deserialize(json: JsonObject): T
}
