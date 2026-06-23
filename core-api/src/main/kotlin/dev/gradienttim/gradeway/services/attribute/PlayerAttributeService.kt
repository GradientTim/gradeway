/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.attribute

import arrow.core.Either
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.services.AttributeService
import net.kyori.adventure.key.Key
import java.util.*

/**
 * Service interface for managing player attributes.
 * Provides methods to add, update, remove, clear, and query player attributes using either
 * player IDs or player entities.
 */
interface PlayerAttributeService {
    /**
     * Adds a new attribute to a player identified by their unique ID.
     *
     * @param id The unique identifier of the player.
     * @param attribute The attribute to be added to the player.
     * @return An [Either] containing [AttributeService.AddAttributeError] in case of a failure, or [Unit] on success.
     */
    fun <TValue : Any> addPlayerAttribute(
        id: UUID,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean>

    /**
     * Adds a new attribute to the given player entity.
     *
     * @param entity The player entity to which the attribute will be added.
     * @param attribute The attribute to be added to the player.
     * @return An [Either] containing [AttributeService.AddAttributeError] if the operation fails, or [Unit] if successful.
     */
    fun <TValue : Any> addPlayerAttribute(
        entity: PlayerEntity,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean>

    /**
     * Adds a new attribute to a player identified by their unique ID or name.
     *
     * @param idOrName The unique identifier or name of the player.
     * @param attribute The attribute to be added to the player.
     * @return An [Either] containing [AttributeService.AddAttributeError] in case of a failure, or [Unit] on success.
     */
    fun <TValue : Any> addPlayerAttribute(
        idOrName: String,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean>

    /**
     * Updates the attribute of a player identified by their unique ID.
     *
     * @param id The unique identifier of the player whose attribute is being updated.
     * @param key The key identifying the attribute to be updated.
     * @param value The new value to set for the specified attribute.
     * @return An [Either] containing [AttributeService.UpdateAttributeError] in case of an error, or [Unit] if the operation succeeds.
     */
    fun <TValue : Any> updatePlayerAttribute(
        id: UUID,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean>

    /**
     * Updates the specified attribute of a player entity with a new value.
     *
     * @param entity The player entity whose attribute is being updated.
     * @param key The key that identifies the attribute to be updated.
     * @param value The new value to set for the specified attribute.
     * @return An [Either] containing [AttributeService.UpdateAttributeError] if the operation fails,
     *         or [Unit] if the update is successful.
     */
    fun <TValue : Any> updatePlayerAttribute(
        entity: PlayerEntity,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean>

    /**
     * Updates the attribute of a player identified by their unique ID or name.
     *
     * @param idOrName The unique identifier or name of the player whose attribute is being updated.
     * @param key The key identifying the attribute to be updated.
     * @param value The new value to set for the specified attribute.
     * @return An [Either] containing [AttributeService.UpdateAttributeError] in case of an error, or [Unit] if the operation succeeds.
     */
    fun <TValue : Any> updatePlayerAttribute(
        idOrName: String,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean>

    /**
     * Removes a specific attribute from a player identified by their unique ID.
     *
     * @param id The unique identifier of the player whose attribute is to be removed.
     * @param key The key that identifies the attribute to be removed.
     * @return An [Either] containing [AttributeService.RemoveAttributeError] in case of a failure,
     *         or [Unit] if the attribute is successfully removed.
     */
    fun removePlayerAttribute(id: UUID, key: Key): Either<AttributeService.RemoveAttributeError, Boolean>

    /**
     * Removes a specific attribute from the specified player entity.
     *
     * @param entity The player entity from which the attribute is to be removed.
     * @param key The key that identifies the attribute to be removed.
     * @return An [Either] containing [AttributeService.RemoveAttributeError] if the operation fails,
     *         or [Unit] if the attribute is successfully removed.
     */
    fun removePlayerAttribute(entity: PlayerEntity, key: Key): Either<AttributeService.RemoveAttributeError, Boolean>

    /**
     * Removes a specific attribute from a player identified by their unique ID or name.
     *
     * @param idOrName The unique identifier or name of the player whose attribute is to be removed.
     * @param key The key that identifies the attribute to be removed.
     * @return An [Either] containing [AttributeService.RemoveAttributeError] in case of a failure,
     *         or [Unit] if the attribute is successfully removed.
     */
    fun removePlayerAttribute(idOrName: String, key: Key): Either<AttributeService.RemoveAttributeError, Boolean>

    /**
     * Clears all attributes associated with the player identified by their unique ID.
     *
     * @param id The unique identifier of the player whose attributes are to be cleared.
     * @return An [Either] containing [AttributeService.ClearAttributesError] if the operation fails,
     *         or [Unit] if the attributes are successfully cleared.
     */
    fun clearPlayerAttributes(id: UUID): Either<AttributeService.ClearAttributesError, Boolean>

    /**
     * Clears all attributes associated with the given player entity.
     *
     * @param entity The player entity whose attributes are to be cleared.
     * @return An [Either] containing [AttributeService.ClearAttributesError] if the operation fails,
     *         or [Unit] if the attributes are successfully cleared.
     */
    fun clearPlayerAttributes(entity: PlayerEntity): Either<AttributeService.ClearAttributesError, Boolean>

    /**
     * Clears all attributes associated with the player identified by their unique ID or name.
     *
     * @param idOrName The unique identifier or name of the player whose attributes are to be cleared.
     * @return An [Either] containing [AttributeService.ClearAttributesError] if the operation fails,
     *         or [Unit] if the attributes are successfully cleared.
     */
    fun clearPlayerAttributes(idOrName: String): Either<AttributeService.ClearAttributesError, Boolean>

    /**
     * Checks if a player identified by their unique ID has a specific attribute.
     *
     * @param id The unique identifier of the player.
     * @param key The key identifying the attribute to check.
     * @return `true` if the player has the specified attribute, otherwise `false`.
     */
    fun hasPlayerAttribute(id: UUID, key: Key): Boolean

    /**
     * Checks if the given player entity has a specific attribute.
     *
     * @param entity The player entity to check for the attribute.
     * @param key The key identifying the attribute to check.
     * @return `true` if the player entity has the specified attribute, otherwise `false`.
     */
    fun hasPlayerAttribute(entity: PlayerEntity, key: Key): Boolean

    /**
     * Checks if a player identified by their unique ID or name has a specific attribute.
     *
     * @param idOrName The unique identifier or name of the player.
     * @param key The key identifying the attribute to check.
     * @return `true` if the player has the specified attribute, otherwise `false`.
     */
    fun hasPlayerAttribute(idOrName: String, key: Key): Boolean

    /**
     * Retrieves a specific attribute associated with a player identified by their unique ID.
     *
     * @param id The unique identifier of the player whose attribute is being retrieved.
     * @param key The key that identifies the attribute to retrieve.
     * @return The attribute associated with the given key, or `null` if no such attribute exists.
     */
    fun getPlayerAttribute(id: UUID, key: Key): Attribute<*>?

    /**
     * Retrieves a specific attribute associated with the given player entity.
     *
     * @param entity The player entity whose attribute is being retrieved.
     * @param key The key that identifies the attribute to retrieve.
     * @return The attribute associated with the specified key, or `null` if no such attribute exists.
     */
    fun getPlayerAttribute(entity: PlayerEntity, key: Key): Attribute<*>?

    /**
     * Retrieves a specific attribute associated with a player identified by their unique ID or name.
     *
     * @param idOrName The unique identifier or name of the player whose attribute is being retrieved.
     * @param key The key that identifies the attribute to retrieve.
     * @return The attribute associated with the given key, or `null` if no such attribute exists.
     */
    fun getPlayerAttribute(idOrName: String, key: Key): Attribute<*>?
}
