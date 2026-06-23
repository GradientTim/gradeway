/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.shared

import arrow.core.Either
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.services.AttributeService
import net.kyori.adventure.key.Key
import java.util.*

/**
 * Service interface for managing shared attributes associated with entities.
 *
 * @param TEntity The type of the entity to which the attributes belong.
 */
interface SharedAttributeService<TEntity> {
    /**
     * Adds an attribute to an entity identified by the given UUID.
     *
     * @param id The unique identifier of the entity to which the attribute will be added.
     * @param attribute The attribute to be added to the entity.
     * @return Either an error of type [AttributeService.AddAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> addAttribute(
        id: UUID,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean>

    /**
     * Adds an attribute to the specified entity.
     *
     * @param entity The entity to which the attribute will be added.
     * @param attribute The attribute to be added to the entity.
     * @return Either an error of type [AttributeService.AddAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> addAttribute(
        entity: TEntity,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean>

    /**
     * Adds an attribute to an entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity to which the attribute will be added.
     * @param attribute The attribute to be added to the entity.
     * @return Either an error of type [AttributeService.AddAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> addAttribute(
        idOrName: String,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean>

    /**
     * Updates the value of an existing attribute for a specific entity.
     *
     * @param id The unique identifier of the entity whose attribute should be updated.
     * @param key The key identifying the attribute to be updated.
     * @param value The new value to assign to the attribute.
     * @return Either an error of type [AttributeService.UpdateAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> updateAttribute(
        id: UUID,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean>

    /**
     * Updates the value of an existing attribute for the specified entity.
     *
     * @param entity The entity whose attribute is to be updated.
     * @param key The key identifying the attribute to be updated.
     * @param value The new value to assign to the attribute.
     * @return Either an error of type [AttributeService.UpdateAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> updateAttribute(
        entity: TEntity,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean>

    /**
     * Updates the value of an existing attribute for an entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity whose attribute should be updated.
     * @param key The key identifying the attribute to be updated.
     * @param value The new value to assign to the attribute.
     * @return Either an error of type [AttributeService.UpdateAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> updateAttribute(
        idOrName: String,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean>

    /**
     * Removes an attribute identified by the given key from an entity specified by its unique identifier.
     *
     * @param id The unique identifier of the entity from which the attribute should be removed.
     * @param key The key identifying the attribute to be removed.
     * @return Either an error of type [AttributeService.RemoveAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun removeAttribute(id: UUID, key: Key): Either<AttributeService.RemoveAttributeError, Boolean>

    /**
     * Removes an attribute identified by the given key from the specified entity.
     *
     * @param entity The entity from which the attribute should be removed.
     * @param key The key identifying the attribute to be removed.
     * @return Either an error of type [AttributeService.RemoveAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun removeAttribute(entity: TEntity, key: Key): Either<AttributeService.RemoveAttributeError, Boolean>

    /**
     * Removes an attribute identified by the given key from an entity specified by its unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity from which the attribute should be removed.
     * @param key The key identifying the attribute to be removed.
     * @return Either an error of type [AttributeService.RemoveAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun removeAttribute(idOrName: String, key: Key): Either<AttributeService.RemoveAttributeError, Boolean>

    /**
     * Clears all attributes associated with the entity identified by the given UUID.
     *
     * @param id The unique identifier of the entity whose attributes are to be cleared.
     * @return Either an error of type [AttributeService.ClearAttributesError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun clearAttributes(id: UUID): Either<AttributeService.ClearAttributesError, Boolean>

    /**
     * Clears all attributes associated with the specified entity.
     *
     * @param entity The entity whose attributes are to be cleared.
     * @return Either an error of type [AttributeService.ClearAttributesError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun clearAttributes(entity: TEntity): Either<AttributeService.ClearAttributesError, Boolean>

    /**
     * Clears all attributes associated with the entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity whose attributes are to be cleared.
     * @return Either an error of type [AttributeService.ClearAttributesError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun clearAttributes(idOrName: String): Either<AttributeService.ClearAttributesError, Boolean>

    /**
     * Checks if an attribute with the specified key exists for the entity identified by the given UUID.
     *
     * @param id The unique identifier of the entity to check for the attribute.
     * @param key The key identifying the attribute to check.
     * @return `true` if the attribute exists for the specified entity, otherwise `false`.
     */
    fun hasAttribute(id: UUID, key: Key): Boolean

    /**
     * Checks if an attribute exists for the specified entity using the given key.
     *
     * @param entity The entity to check for the attribute.
     * @param key The key identifying the attribute to check.
     * @return `true` if the attribute exists for the provided entity and key, otherwise `false`.
     */
    fun hasAttribute(entity: TEntity, key: Key): Boolean

    /**
     * Checks if an attribute exists for the entity identified by the given unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity to check for the attribute.
     * @param key The key identifying the attribute to check.
     * @return `true` if the attribute exists for the specified entity, otherwise `false`.
     */
    fun hasAttribute(idOrName: String, key: Key): Boolean

    /**
     * Retrieves the attribute identified by the given key for the entity specified by the unique identifier.
     *
     * @param id The unique identifier of the entity whose attribute is to be retrieved.
     * @param key The key identifying the attribute to retrieve.
     * @return The attribute associated with the given key for the specified entity, or null if the attribute is not found.
     */
    fun getAttribute(id: UUID, key: Key): Attribute<*>?

    /**
     * Retrieves the attribute identified by the given key for the specified entity.
     *
     * @param entity The entity from which the attribute is to be retrieved.
     * @param key The key identifying the attribute to retrieve.
     * @return The attribute associated with the given key for the specified entity, or null if the attribute is not found.
     */
    fun getAttribute(entity: TEntity, key: Key): Attribute<*>?

    /**
     * Retrieves the attribute identified by the given key for the entity specified by the unique identifier or name.
     *
     * @param idOrName The unique identifier or name of the entity whose attribute is to be retrieved.
     * @param key The key identifying the attribute to retrieve.
     * @return The attribute associated with the given key for the specified entity, or null if the attribute is not found.
     */
    fun getAttribute(idOrName: String, key: Key): Attribute<*>?
}
