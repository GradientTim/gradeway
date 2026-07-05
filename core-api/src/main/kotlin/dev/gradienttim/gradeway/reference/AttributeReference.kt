/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.reference

import arrow.core.Either
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.services.AttributeService
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.jdbc.SizedIterable

/**
 * Represents an entity or object that maintains a reference to a collection of attributes.
 *
 * This interface provides mechanisms to query and manage attributes associated with the
 * entity. Each attribute represents a specific property or characteristic, enabling
 * efficient handling of attribute data while preserving flexibility for further extension
 * or customization.
 *
 * @param TReference The type of the attribute references managed by this interface.
 */
interface AttributeReference<TReference> {
    /**
     * Represents a collection of attributes associated with the entity. Each attribute defines
     * a specific property or characteristic of the entity and can be queried or manipulated
     * to manage the entity's state effectively. This collection supports lazy queries and provides
     * efficient handling of attribute data.
     */
    val attributes: SizedIterable<TReference>

    /**
     * Adds a new attribute to the collection of attributes.
     *
     * This method attempts to add the specified attribute to the collection. If the
     * operation is successful, it returns a success result. If it fails, an error
     * describing the issue is returned.
     *
     * @param attribute The attribute to be added. It contains the type, key, and value
     *                  necessary to represent the attribute being added.
     * @return An [Either] instance which:
     *         - Contains an [AttributeService.AddAttributeError] if the operation fails,
     *           detailing reasons such as the entity not being found, the attribute
     *           already existing, or an unexpected error.
     *         - Contains [Unit] upon successful addition of the attribute.
     */
    fun <TValue : Any> addAttribute(attribute: Attribute<TValue>): Either<AttributeService.AddAttributeError, Unit>

    /**
     * Updates the value of an attribute associated with the specified key.
     *
     * This function attempts to update an attribute identified by the given key
     * to the provided value. If the operation succeeds, it returns a success result.
     * If it fails, an error detailing the reason for the failure is returned.
     *
     * @param key The unique identifier for the attribute to be updated.
     * @param value The new value to associate with the specified attribute key.
     * @return An [Either] instance which:
     *         - Contains a [AttributeService.UpdateAttributeError] if the operation fails, detailing reasons
     *           such as the entity not being found, the attribute not existing, the type not being registered,
     *           or an unexpected error.
     *         - Contains [Unit] upon successful update of the attribute.
     */
    fun <TValue : Any> updateAttribute(key: Key, value: TValue): Either<AttributeService.UpdateAttributeError, Unit>

    /**
     * Removes the attribute associated with the specified key.
     *
     * This function attempts to remove an attribute identified by the given key from the collection of attributes.
     * If the operation succeeds, it returns a success result. If it fails, it provides an error detailing the reason
     * for the failure, such as the entity not being found, the attribute not existing, or an unexpected error.
     *
     * @param key The unique identifier of the attribute to be removed.
     * @return An [Either] instance which:
     *         - Contains a [AttributeService.RemoveAttributeError] if the operation fails, providing details
     *           about the failure.
     *         - Contains [Unit] upon successful removal of the attribute.
     */
    fun removeAttribute(key: Key): Either<AttributeService.RemoveAttributeError, Unit>

    /**
     * Clears all attributes associated with the current instance.
     *
     * This method removes all attributes currently managed by the instance.
     * It returns a result indicating the success or failure of the operation.
     * The operation may fail if there are no attributes to clear, the entity
     * associated with the attributes is not found, or due to an unexpected error.
     *
     * @return An [Either] instance which:
     *         - Contains a [AttributeService.ClearAttributesError] if the operation fails, detailing
     *           reasons such as no attributes found, the entity not being found, or an unexpected error.
     *         - Contains [Unit] on successful removal of all attributes.
     */
    fun clearAttributes(): Either<AttributeService.ClearAttributesError, Unit>

    /**
     * Checks if an attribute with the specified key exists in the collection of attributes.
     *
     * This method queries the internal collection of attributes to determine whether an
     * attribute associated with the given key is present. It provides a way to verify
     * the existence of an attribute without retrieving its value or reference.
     *
     * @param key The unique identifier of the attribute to check for existence.
     * @return `true` if an attribute with the specified key exists, otherwise `false`.
     */
    fun hasAttribute(key: Key): Boolean

    /**
     * Retrieves the reference to the attribute associated with the specified key.
     *
     * This method looks up an attribute using the provided key, allowing access to its
     * associated reference. If the attribute does not exist or is not found, it returns null.
     *
     * @param key The unique key identifying the attribute to retrieve.
     * @return The reference to the attribute of type [TReference] if found, or null otherwise.
     */
    fun getAttribute(key: Key): TReference?
}
