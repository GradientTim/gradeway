/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.attribute

import arrow.core.Either
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.database.models.role.RoleEntity
import dev.gradienttim.gradeway.services.AttributeService
import net.kyori.adventure.key.Key
import java.util.*

/**
 * Service interface for managing attributes associated with roles.
 * Provides functionality to add, update, remove, and query attributes tied to roles,
 * identified either by their unique ID or their entity representation.
 */
interface RoleAttributeService {
    /**
     * Adds an attribute to a role identified by its unique ID.
     *
     * @param id The unique identifier of the role to which the attribute will be added.
     * @param attribute The attribute to be added to the role.
     * @return Either an error encapsulated in [AttributeService.AddAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> addRoleAttribute(
        id: UUID,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean>

    /**
     * Adds an attribute to a specific role entity.
     *
     * @param entity The [RoleEntity] instance representing the role to which the attribute will be added.
     * @param attribute The attribute to be associated with the specified role entity.
     * @return An [Either] containing [AttributeService.AddAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> addRoleAttribute(
        entity: RoleEntity,
        attribute: Attribute<TValue>
    ): Either<AttributeService.AddAttributeError, Boolean>

    /**
     * Updates the value of an existing attribute for a role identified by its unique ID.
     *
     * @param id The unique identifier of the role for which the attribute value will be updated.
     * @param key The key identifying the attribute to be updated.
     * @param value The new value to assign to the specified attribute.
     * @return An [Either] instance containing [AttributeService.UpdateAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> updateRoleAttribute(
        id: UUID,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean>

    /**
     * Updates the value of an existing attribute for a specific role entity.
     *
     * @param entity The [RoleEntity] instance representing the role whose attribute needs to be updated.
     * @param key The [Key] identifying the attribute to be updated.
     * @param value The new value of type [TValue] to assign to the specified attribute.
     * @return An [Either] containing [AttributeService.UpdateAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun <TValue : Any> updateRoleAttribute(
        entity: RoleEntity,
        key: Key,
        value: TValue
    ): Either<AttributeService.UpdateAttributeError, Boolean>

    /**
     * Removes an attribute associated with a role identified by its unique ID.
     *
     * @param id The unique identifier of the role from which the attribute will be removed.
     * @param key The key identifying the attribute to be removed.
     * @return An [Either] containing [AttributeService.RemoveAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun removeRoleAttribute(id: UUID, key: Key): Either<AttributeService.RemoveAttributeError, Boolean>

    /**
     * Removes an attribute associated with a specific role entity.
     *
     * @param entity The [RoleEntity] instance representing the role from which the attribute will be removed.
     * @param key The [Key] identifying the attribute to be removed.
     * @return An [Either] containing [AttributeService.RemoveAttributeError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun removeRoleAttribute(entity: RoleEntity, key: Key): Either<AttributeService.RemoveAttributeError, Boolean>

    /**
     * Removes all attributes associated with a role identified by its unique ID.
     *
     * @param id The unique identifier of the role whose attributes should be cleared.
     * @return An [Either] containing [AttributeService.ClearAttributesError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun clearRoleAttributes(id: UUID): Either<AttributeService.ClearAttributesError, Boolean>

    /**
     * Removes all attributes associated with a specific role entity.
     *
     * @param entity The [RoleEntity] instance representing the role whose attributes should be cleared.
     * @return An [Either] containing [AttributeService.ClearAttributesError] if the operation fails,
     *         or `true` if the update succeeds.
     */
    fun clearRoleAttributes(entity: RoleEntity): Either<AttributeService.ClearAttributesError, Boolean>

    /**
     * Determines if a role, identified by its unique ID, has an attribute associated with the specified key.
     *
     * @param id The unique identifier of the role to check for the presence of the attribute.
     * @param key The key representing the attribute to check for.
     * @return True if the attribute exists for the specified role and key, false otherwise.
     */
    fun hasRoleAttribute(id: UUID, key: Key): Boolean

    /**
     * Checks whether a specified role entity has an attribute associated with the given key.
     *
     * @param entity The RoleEntity instance representing the role to check for the attribute.
     * @param key The key representing the attribute to check for presence.
     * @return True if the specified attribute exists for the given role entity, false otherwise.
     */
    fun hasRoleAttribute(entity: RoleEntity, key: Key): Boolean

    /**
     * Retrieves the attribute associated with a specific role, identified by its unique ID and the given key.
     *
     * @param id The unique identifier of the role whose attribute needs to be retrieved.
     * @param key The key representing the attribute to retrieve for the specified role.
     * @return The attribute of the specified key associated with the role, or null if no such attribute exists.
     */
    fun getRoleAttribute(id: UUID, key: Key): Attribute<*>?

    /**
     * Retrieves the attribute associated with a specific role entity, identified by the given key.
     *
     * @param entity The [RoleEntity] instance representing the role from which the attribute will be retrieved.
     * @param key The [Key] representing the attribute to retrieve for the specified role entity.
     * @return The attribute of the specified key associated with the role entity, or null if no such attribute exists.
     */
    fun getRoleAttribute(entity: RoleEntity, key: Key): Attribute<*>?
}
