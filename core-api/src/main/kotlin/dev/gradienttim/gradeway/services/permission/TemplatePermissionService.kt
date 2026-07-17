/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services.permission

import arrow.core.Either
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplatePermissionEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.entity.role.RolePermissionTemplateEntity
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.services.PermissionService.CreateTemplateError
import dev.gradienttim.gradeway.services.PermissionService.DeleteTemplateError
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.*

@Suppress("TooManyFunctions", "LargeClass")
interface TemplatePermissionService {
    /**
     * Creates a permission template with the specified name.
     *
     * @param name The name of the template to be created.
     * @return Either a CreateTemplateError indicating why the template could not be created,
     * or a PermissionTemplateEntity representing the successfully created template.
     */
    fun createTemplate(name: String): Either<CreateTemplateError, PermissionTemplateEntity>

    /**
     * Deletes a template identified by the given ID.
     *
     * @param id The unique identifier of the template to be deleted.
     * @return Either an error of type DeleteTemplateError if the deletion fails, or the deleted PermissionTemplateEntity on success.
     */
    fun deleteTemplate(id: UUID): Either<DeleteTemplateError, PermissionTemplateEntity>

    /**
     * Updates the template name associated with a given identifier.
     *
     * @param id The unique identifier of the template to be updated.
     * @param name The new name to set for the template.
     * @return Either an error of type PermissionService.SetNameTemplateError if the operation fails,
     *         or a Boolean indicating success if the operation is completed.
     */
    fun setTemplateName(
        id: UUID,
        name: String
    ): Either<PermissionService.SetNameTemplateError, Boolean>

    /**
     * Updates the template name for the given permission template entity.
     *
     * @param entity The permission template entity whose name is to be updated.
     * @param name The new name to set for the permission template.
     * @return Either an error indicating why the update failed
     * or a boolean indicating whether the update was successful.
     */
    fun setTemplateName(
        entity: PermissionTemplateEntity,
        name: String
    ): Either<PermissionService.SetNameTemplateError, Boolean>

    /**
     * Sets the template name for a given identifier or name.
     *
     * @param idOrName The identifier or name of the template to update.
     * @param name The new name to be assigned to the template.
     * @return Either an error of type PermissionService.SetNameTemplateError if the operation fails,
     *         or a Boolean indicating success.
     */
    fun setTemplateName(
        idOrName: String,
        name: String
    ): Either<PermissionService.SetNameTemplateError, Boolean>

    /**
     * Updates the entity to assign a permission template to a specific target.
     *
     * @param id The unique identifier of the permission template to be updated.
     * @param assignedTo The target to which the permission template should be assigned.
     * @return Either an error of type SetAssignedToTemplateError if the operation fails,
     *         or a Boolean indicating whether the assignment was successful.
     */
    fun setTemplateAssignedTo(
        id: UUID,
        assignedTo: PermissionTemplateEntity.AssignedTo
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean>

    /**
     * Updates the assigned entity for a given permission template.
     *
     * @param entity The permission template entity to be updated.
     * @param assignedTo The entity to which the template should be assigned.
     * @return Either an error of type SetAssignedToTemplateError or a Boolean indicating success.
     */
    fun setTemplateAssignedTo(
        entity: PermissionTemplateEntity,
        assignedTo: PermissionTemplateEntity.AssignedTo
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean>

    /**
     * Sets the assigned-to entity for a specific permission template.
     *
     * @param idOrName The unique identifier or name of the permission template.
     * @param assignedTo The entity to which the template is being assigned.
     * @return Either an error if the assignment fails, or a boolean indicating success.
     */
    fun setTemplateAssignedTo(
        idOrName: String,
        assignedTo: PermissionTemplateEntity.AssignedTo
    ): Either<PermissionService.SetAssignedToTemplateError, Boolean>

    /**
     * Retrieves a permission template entity based on the provided template ID.
     *
     * @param id The unique identifier of the template to be retrieved.
     * @return The permission template entity if found, or null if no template with the specified ID exists.
     */
    fun findTemplateById(id: UUID): PermissionTemplateEntity?

    /**
     * Finds and retrieves a permission template based on the provided name.
     *
     * @param name The name of the template to search for.
     * @return The matching PermissionTemplateEntity if found, or null otherwise.
     */
    fun findTemplateByName(name: String): PermissionTemplateEntity?

    /**
     * Retrieves a permission template entity based on the provided ID or name.
     *
     * This method searches for a template entity that matches the given value
     * as either an ID or a name. If no matching entity is found, it returns null.
     *
     * @param value The identifier or name of the permission template to be retrieved.
     * @return The matching [PermissionTemplateEntity] if found, or null if no match exists.
     */
    fun findTemplateByIdOrName(value: String): PermissionTemplateEntity?

    /**
     * Adds a permission to a permission template by associating the given permission ID with the specified template ID.
     *
     * @param templateId The unique identifier of the permission template to which the permission will be added.
     * @param permissionId The unique identifier of the permission to be added to the template.
     * @return Either an error of type AddPermissionToTemplateError if the operation fails,
     *         or a PermissionTemplatePermissionEntity representing the association if the operation succeeds.
     */
    fun addPermissionToTemplate(
        templateId: UUID,
        permissionId: UUID
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Adds a specified permission to a given permission template.
     *
     * @param template The permission template entity to which the permission will be added.
     * @param permissionId The unique identifier of the permission to be added.
     * @return Either an error indicating why the operation failed
     * or the newly created PermissionTemplatePermissionEntity if the operation succeeds.
     */
    fun addPermissionToTemplate(
        template: PermissionTemplateEntity,
        permissionId: UUID
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Adds a permission to a specified permission template.
     *
     * @param templateId The unique identifier of the permission template to which the permission will be added.
     * @param permission The permission entity that will be added to the specified template.
     * @return Either an error of type PermissionService.AddPermissionToTemplateError if the operation fails,
     *         or the newly created PermissionTemplatePermissionEntity if the addition is successful.
     */
    fun addPermissionToTemplate(
        templateId: UUID,
        permission: PermissionEntity
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Adds a permission to a specified template.
     *
     * @param templateIdOrName The identifier or name of the template to which the permission will be added.
     * @param permission The permission entity that needs to be added to the template.
     * @return An `Either` containing either an error of type `PermissionService.AddPermissionToTemplateError`
     *         if the operation fails, or a `PermissionTemplatePermissionEntity` if the operation succeeds.
     */
    fun addPermissionToTemplate(
        templateIdOrName: String,
        permission: PermissionEntity
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Adds a permission to a specified permission template.
     *
     * @param templateIdOrName The identifier or name of the permission template to which the permission will be added.
     * @param permissionId The unique identifier of the permission to be added to the template.
     * @return Either an error of type AddPermissionToTemplateError if the operation fails,
     *         or the updated PermissionTemplatePermissionEntity if the operation succeeds.
     */
    fun addPermissionToTemplate(
        templateIdOrName: String,
        permissionId: UUID
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Adds a specified permission to a given permission template.
     *
     * @param template The permission template entity to which the permission will be added.
     * @param permission The permission entity to be added to the template.
     * @return Either an error indicating why the operation failed
     * or the newly created PermissionTemplatePermissionEntity if the operation succeeds.
     */
    fun addPermissionToTemplate(
        template: PermissionTemplateEntity,
        permission: PermissionEntity
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Adds a permission identified by its ID or value to a specified permission template.
     *
     * @param templateId The unique identifier of the permission template to which the permission will be added.
     * @param permissionIdOrValue The unique identifier or value of the permission to be added to the template.
     * @return Either an error of type AddPermissionToTemplateError if the operation fails,
     *         or the newly created PermissionTemplatePermissionEntity if the operation succeeds.
     */
    fun addPermissionToTemplate(
        templateId: UUID,
        permissionIdOrValue: String
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Adds a permission identified by its ID or value to a specified permission template.
     *
     * @param template The permission template entity to which the permission will be added.
     * @param permissionIdOrValue The unique identifier or value of the permission to be added to the template.
     * @return Either an error of type AddPermissionToTemplateError if the operation fails,
     *         or the newly created PermissionTemplatePermissionEntity if the operation succeeds.
     */
    fun addPermissionToTemplate(
        template: PermissionTemplateEntity,
        permissionIdOrValue: String
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Adds a permission identified by its ID or value to a permission template identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to which the permission will be added.
     * @param permissionIdOrValue The unique identifier or value of the permission to be added to the template.
     * @return Either an error of type AddPermissionToTemplateError if the operation fails,
     *         or the newly created PermissionTemplatePermissionEntity if the operation succeeds.
     */
    fun addPermissionToTemplate(
        templateIdOrName: String,
        permissionIdOrValue: String
    ): Either<PermissionService.AddPermissionToTemplateError, PermissionTemplatePermissionEntity>

    /**
     * Removes a permission from a permission template identified by the given IDs.
     *
     * @param templateId The unique identifier of the permission template from which the permission will be removed.
     * @param permissionId The unique identifier of the permission to be removed.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        templateId: UUID,
        permissionId: UUID
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Removes a permission from a specified permission template.
     *
     * @param template The permission template entity from which the permission will be removed.
     * @param permissionId The unique identifier of the permission to be removed.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        template: PermissionTemplateEntity,
        permissionId: UUID
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Removes a permission from a permission template identified by the given template ID.
     *
     * @param templateId The unique identifier of the permission template from which the permission will be removed.
     * @param permission The permission entity to be removed from the template.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        templateId: UUID,
        permission: PermissionEntity
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Removes a permission from a permission template identified by the given ID or name.
     *
     * @param templateIdOrName The identifier or name of the permission template from which the permission will be removed.
     * @param permission The permission entity to be removed from the template.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        templateIdOrName: String,
        permission: PermissionEntity
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Removes a permission from a permission template identified by the given ID or name.
     *
     * @param templateIdOrName The identifier or name of the permission template from which the permission will be removed.
     * @param permissionId The unique identifier of the permission to be removed.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        templateIdOrName: String,
        permissionId: UUID
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Removes a permission from a specified permission template.
     *
     * @param template The permission template entity from which the permission will be removed.
     * @param permission The permission entity to be removed from the template.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        template: PermissionTemplateEntity,
        permission: PermissionEntity
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Removes a permission identified by its ID or value from a specified permission template.
     *
     * @param templateId The unique identifier of the permission template from which the permission will be removed.
     * @param permissionIdOrValue The unique identifier or value of the permission to be removed.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        templateId: UUID,
        permissionIdOrValue: String
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Removes a permission identified by its ID or value from a specified permission template.
     *
     * @param template The permission template entity from which the permission will be removed.
     * @param permissionIdOrValue The unique identifier or value of the permission to be removed.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        template: PermissionTemplateEntity,
        permissionIdOrValue: String
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Removes a permission identified by its ID or value from a permission template identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template from which the permission will be removed.
     * @param permissionIdOrValue The unique identifier or value of the permission to be removed.
     * @return Either an error of type RemovePermissionFromTemplateError if the operation fails,
     *         or Unit if the removal was successful.
     */
    fun removePermissionFromTemplate(
        templateIdOrName: String,
        permissionIdOrValue: String
    ): Either<PermissionService.RemovePermissionFromTemplateError, Unit>

    /**
     * Clears all permissions associated with the specified permission template.
     *
     * @param templateId The unique identifier of the permission template whose permissions will be cleared.
     * @return Either an error of type ClearPermissionsFromTemplateError if the operation fails,
     *         or Unit if the permissions were cleared successfully.
     */
    fun clearPermissionsFromTemplate(
        templateId: UUID
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit>

    /**
     * Clears all permissions associated with the specified permission template entity.
     *
     * @param template The permission template entity whose permissions will be cleared.
     * @return Either an error of type ClearPermissionsFromTemplateError if the operation fails,
     *         or Unit if the permissions were cleared successfully.
     */
    fun clearPermissionsFromTemplate(
        template: PermissionTemplateEntity
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit>

    /**
     * Clears all permissions associated with the permission template identified by the given ID or name.
     *
     * @param idOrName The identifier or name of the permission template whose permissions will be cleared.
     * @return Either an error of type ClearPermissionsFromTemplateError if the operation fails,
     *         or Unit if the permissions were cleared successfully.
     */
    fun clearPermissionsFromTemplate(
        idOrName: String
    ): Either<PermissionService.ClearPermissionsFromTemplateError, Unit>

    /**
     * Retrieves a list of permission templates, optionally filtered, ordered, and limited.
     *
     * @param where An optional lambda returning a boolean expression used to filter the results.
     * @param orderBy A set of column and sort order pairs used to order the results.
     * @param limit The maximum number of templates to return.
     * @return A SizedIterable containing the matching PermissionTemplateEntity instances.
     */
    fun listTemplates(
        where: (() -> Op<Boolean>)? = null,
        orderBy: Set<Pair<Expression<*>, SortOrder>> = emptySet(),
        limit: Int = 20
    ): SizedIterable<PermissionTemplateEntity>

    /**
     * Links a permission template to a role identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param roleId The unique identifier of the role to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        templateId: UUID,
        roleId: UUID
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Links a permission template to the specified role.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param role The role entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Links a permission template to a role identified by the given role ID.
     *
     * @param template The permission template entity to be linked.
     * @param roleId The unique identifier of the role to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Links a permission template to the specified role.
     *
     * @param template The permission template entity to be linked.
     * @param role The role entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Links a permission template identified by its ID or name to a role identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param roleId The unique identifier of the role to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        templateIdOrName: String,
        roleId: UUID
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Links a permission template identified by its ID or name to the specified role.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param role The role entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        templateIdOrName: String,
        role: RoleEntity
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Links a permission template to a role identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param roleIdOrName The unique identifier or name of the role to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        templateId: UUID,
        roleIdOrName: String
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Links the specified permission template to a role identified by its ID or name.
     *
     * @param template The permission template entity to be linked.
     * @param roleIdOrName The unique identifier or name of the role to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        template: PermissionTemplateEntity,
        roleIdOrName: String
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Links a permission template identified by its ID or name to a role identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param roleIdOrName The unique identifier or name of the role to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or a RolePermissionTemplateEntity representing the created link if the operation succeeds.
     */
    fun linkTemplateToRole(
        templateIdOrName: String,
        roleIdOrName: String
    ): Either<PermissionService.LinkTemplateError, RolePermissionTemplateEntity>

    /**
     * Removes the link between a permission template and a role identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param roleId The unique identifier of the role from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(templateId: UUID, roleId: UUID): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and the specified role.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param role The role entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and a role identified by the given role ID.
     *
     * @param template The permission template entity to be unlinked.
     * @param roleId The unique identifier of the role from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and the specified role.
     *
     * @param template The permission template entity to be unlinked.
     * @param role The role entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and a role identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param roleId The unique identifier of the role from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(
        templateIdOrName: String,
        roleId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and the specified role.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param role The role entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(
        templateIdOrName: String,
        role: RoleEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and a role identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param roleIdOrName The unique identifier or name of the role from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(
        templateId: UUID,
        roleIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between the specified permission template and a role identified by its ID or name.
     *
     * @param template The permission template entity to be unlinked.
     * @param roleIdOrName The unique identifier or name of the role from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(
        template: PermissionTemplateEntity,
        roleIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and a role identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param roleIdOrName The unique identifier or name of the role from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromRole(
        templateIdOrName: String,
        roleIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Applies a permission template to a role identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param roleId The unique identifier of the role to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(templateId: UUID, roleId: UUID): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to the specified role.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param role The role entity to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(templateId: UUID, role: RoleEntity): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to a role identified by the given role ID.
     *
     * @param template The permission template entity to be applied.
     * @param roleId The unique identifier of the role to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to the specified role.
     *
     * @param template The permission template entity to be applied.
     * @param role The role entity to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to a role identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param roleId The unique identifier of the role to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(
        templateIdOrName: String,
        roleId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to the specified role.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param role The role entity to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(
        templateIdOrName: String,
        role: RoleEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to a role identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param roleIdOrName The unique identifier or name of the role to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(
        templateId: UUID,
        roleIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies the specified permission template to a role identified by its ID or name.
     *
     * @param template The permission template entity to be applied.
     * @param roleIdOrName The unique identifier or name of the role to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(
        template: PermissionTemplateEntity,
        roleIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to a role identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param roleIdOrName The unique identifier or name of the role to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToRole(
        templateIdOrName: String,
        roleIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Revokes a permission template from a role identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be revoked.
     * @param roleId The unique identifier of the role from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(templateId: UUID, roleId: UUID): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from the specified role.
     *
     * @param templateId The unique identifier of the permission template to be revoked.
     * @param role The role entity from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(
        templateId: UUID,
        role: RoleEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from a role identified by the given role ID.
     *
     * @param template The permission template entity to be revoked.
     * @param roleId The unique identifier of the role from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(
        template: PermissionTemplateEntity,
        roleId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from the specified role.
     *
     * @param template The permission template entity to be revoked.
     * @param role The role entity from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(
        template: PermissionTemplateEntity,
        role: RoleEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from a role identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param roleId The unique identifier of the role from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(
        templateIdOrName: String,
        roleId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from the specified role.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param role The role entity from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(
        templateIdOrName: String,
        role: RoleEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from a role identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be revoked.
     * @param roleIdOrName The unique identifier or name of the role from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(
        templateId: UUID,
        roleIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes the specified permission template from a role identified by its ID or name.
     *
     * @param template The permission template entity to be revoked.
     * @param roleIdOrName The unique identifier or name of the role from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(
        template: PermissionTemplateEntity,
        roleIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from a role identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param roleIdOrName The unique identifier or name of the role from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromRole(
        templateIdOrName: String,
        roleIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Links a permission template to a player identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param playerId The unique identifier of the player to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(templateId: UUID, playerId: UUID): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template to the specified player.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param player The player entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template to a player identified by the given player ID.
     *
     * @param template The permission template entity to be linked.
     * @param playerId The unique identifier of the player to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template to the specified player.
     *
     * @param template The permission template entity to be linked.
     * @param player The player entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template identified by its ID or name to a player identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param playerId The unique identifier of the player to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(
        templateIdOrName: String,
        playerId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template identified by its ID or name to the specified player.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param player The player entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(
        templateIdOrName: String,
        player: PlayerEntity
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template to a player identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param playerIdOrName The unique identifier or name of the player to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(
        templateId: UUID,
        playerIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links the specified permission template to a player identified by its ID or name.
     *
     * @param template The permission template entity to be linked.
     * @param playerIdOrName The unique identifier or name of the player to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template identified by its ID or name to a player identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param playerIdOrName The unique identifier or name of the player to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToPlayer(
        templateIdOrName: String,
        playerIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and a player identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param playerId The unique identifier of the player from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and the specified player.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param player The player entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and a player identified by the given player ID.
     *
     * @param template The permission template entity to be unlinked.
     * @param playerId The unique identifier of the player from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and the specified player.
     *
     * @param template The permission template entity to be unlinked.
     * @param player The player entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and a player identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param playerId The unique identifier of the player from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        templateIdOrName: String,
        playerId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and the specified player.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param player The player entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        templateIdOrName: String,
        player: PlayerEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and a player identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param playerIdOrName The unique identifier or name of the player from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        templateId: UUID,
        playerIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between the specified permission template and a player identified by its ID or name.
     *
     * @param template The permission template entity to be unlinked.
     * @param playerIdOrName The unique identifier or name of the player from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and a player identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param playerIdOrName The unique identifier or name of the player from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromPlayer(
        templateIdOrName: String,
        playerIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Applies a permission template to a player identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param playerId The unique identifier of the player to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToPlayer(templateId: UUID, playerId: UUID): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to the specified player.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param player The player entity to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to a player identified by the given player ID.
     *
     * @param template The permission template entity to be applied.
     * @param playerId The unique identifier of the player to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a given permission template to the specified player.
     *
     * @param template The permission template to be applied.
     * @param player The player entity to which the template will be applied.
     * @return An `Either` containing an error of type `PermissionService.ApplyTemplateError`
     * if the operation fails, or `Boolean` indicating success.
     */
    fun applyTemplateToPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to a player identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param playerId The unique identifier of the player to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToPlayer(
        templateIdOrName: String,
        playerId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to the specified player.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param player The player entity to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToPlayer(
        templateIdOrName: String,
        player: PlayerEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to a player identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param playerIdOrName The unique identifier or name of the player to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToPlayer(
        templateId: UUID,
        playerIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies the specified permission template to a player identified by its ID or name.
     *
     * @param template The permission template entity to be applied.
     * @param playerIdOrName The unique identifier or name of the player to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToPlayer(
        template: PermissionTemplateEntity,
        playerIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to a player identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param playerIdOrName The unique identifier or name of the player to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToPlayer(
        templateIdOrName: String,
        playerIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Revokes a specific template from a player identified by their unique ID.
     *
     * @param templateId The unique identifier of the template to revoke.
     * @param playerId The unique identifier of the player from whom the template will be revoked.
     * @return Either a PermissionService.RevokeTemplateError indicating the error if the operation fails,
     *         or a Boolean value indicating success if the operation is completed.
     */
    fun revokeTemplateFromPlayer(
        templateId: UUID,
        playerId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a specific permission template from a player.
     *
     * @param templateId The unique identifier of the permission template to revoke.
     * @param player The player entity from which the permission template will be revoked.
     * @return Either a `RevokeTemplateError` indicating the reason for
     */
    fun revokeTemplateFromPlayer(
        templateId: UUID,
        player: PlayerEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from a specific player.
     *
     * @param template The permission template to be revoked.
     * @param playerId The unique identifier of the player from whom the template will be revoked.
     * @return Either an error detailing why the revocation failed or a boolean indicating success.
     */
    fun revokeTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from a specified player.
     *
     * @param template The permission template to be revoked from the player.
     * @param player The player entity from whom the permission template is to be revoked.
     * @return Either an error describing the failure to revoke the template or a boolean indicating success.
     */
    fun revokeTemplateFromPlayer(
        template: PermissionTemplateEntity,
        player: PlayerEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from a player identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param playerId The unique identifier of the player from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromPlayer(
        templateIdOrName: String,
        playerId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from the specified player.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param player The player entity from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromPlayer(
        templateIdOrName: String,
        player: PlayerEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from a player identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be revoked.
     * @param playerIdOrName The unique identifier or name of the player from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromPlayer(
        templateId: UUID,
        playerIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes the specified permission template from a player identified by its ID or name.
     *
     * @param template The permission template entity to be revoked.
     * @param playerIdOrName The unique identifier or name of the player from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromPlayer(
        template: PermissionTemplateEntity,
        playerIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from a player identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param playerIdOrName The unique identifier or name of the player from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromPlayer(
        templateIdOrName: String,
        playerIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Links a permission template to a group identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param groupId The unique identifier of the group to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(templateId: UUID, groupId: UUID): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template to the specified group.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param group The group entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(
        templateId: UUID,
        group: GroupEntity
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template to a player identified by the given group ID.
     *
     * @param template The permission template entity to be linked.
     * @param groupId The unique identifier of the group to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(
        template: PermissionTemplateEntity,
        groupId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template to the specified group.
     *
     * @param template The permission template entity to be linked.
     * @param group The group entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(
        template: PermissionTemplateEntity,
        group: GroupEntity
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template identified by its ID or name to a group identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param groupId The unique identifier of the group to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(
        templateIdOrName: String,
        groupId: UUID
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template identified by its ID or name to the specified group.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param group The group entity to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(
        templateIdOrName: String,
        group: GroupEntity
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template to a group identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be linked.
     * @param groupIdOrName The unique identifier or name of the group to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(
        templateId: UUID,
        groupIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links the specified permission template to a group identified by its ID or name.
     *
     * @param template The permission template entity to be linked.
     * @param groupIdOrName The unique identifier or name of the group to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(
        template: PermissionTemplateEntity,
        groupIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Links a permission template identified by its ID or name to a group identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be linked.
     * @param groupIdOrName The unique identifier or name of the group to which the template will be linked.
     * @return Either an error of type LinkTemplateError if the operation fails,
     *         or Unit if the link was created successfully.
     */
    fun linkTemplateToGroup(
        templateIdOrName: String,
        groupIdOrName: String
    ): Either<PermissionService.LinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and a group identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param groupId The unique identifier of the group from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        templateId: UUID,
        groupId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and the specified group.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param group The group entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        templateId: UUID,
        group: GroupEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and a player identified by the given group ID.
     *
     * @param template The permission template entity to be unlinked.
     * @param groupId The unique identifier of the group from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        template: PermissionTemplateEntity,
        groupId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and the specified group.
     *
     * @param template The permission template entity to be unlinked.
     * @param group The group entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        template: PermissionTemplateEntity,
        group: GroupEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and a group identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param groupId The unique identifier of the group from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        templateIdOrName: String,
        groupId: UUID
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and the specified group.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param group The group entity from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        templateIdOrName: String,
        group: GroupEntity
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template and a group identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be unlinked.
     * @param groupIdOrName The unique identifier or name of the group from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        templateId: UUID,
        groupIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between the specified permission template and a group identified by its ID or name.
     *
     * @param template The permission template entity to be unlinked.
     * @param groupIdOrName The unique identifier or name of the group from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        template: PermissionTemplateEntity,
        groupIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Removes the link between a permission template identified by its ID or name and a group identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be unlinked.
     * @param groupIdOrName The unique identifier or name of the group from which the template will be unlinked.
     * @return Either an error of type UnlinkTemplateError if the operation fails,
     *         or Unit if the unlinking was successful.
     */
    fun unlinkTemplateFromGroup(
        templateIdOrName: String,
        groupIdOrName: String
    ): Either<PermissionService.UnlinkTemplateError, Unit>

    /**
     * Applies a permission template to a group identified by their respective IDs.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param groupId The unique identifier of the group to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToGroup(templateId: UUID, groupId: UUID): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to the specified group.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param group The group entity to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToGroup(
        templateId: UUID,
        group: GroupEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to a player identified by the given group ID.
     *
     * @param template The permission template entity to be applied.
     * @param groupId The unique identifier of the group to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToGroup(
        template: PermissionTemplateEntity,
        groupId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a given permission template to the specified group.
     *
     * @param template The permission template to be applied.
     * @param group The group entity to which the template will be applied.
     * @return An `Either` containing an error of type `PermissionService.ApplyTemplateError`
     * if the operation fails, or `Boolean` indicating success.
     */
    fun applyTemplateToGroup(
        template: PermissionTemplateEntity,
        group: GroupEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to a group identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param groupId The unique identifier of the group to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToGroup(
        templateIdOrName: String,
        groupId: UUID
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to the specified group.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param group The group entity to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToGroup(
        templateIdOrName: String,
        group: GroupEntity
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template to a group identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be applied.
     * @param groupIdOrName The unique identifier or name of the group to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToGroup(
        templateId: UUID,
        groupIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies the specified permission template to a group identified by its ID or name.
     *
     * @param template The permission template entity to be applied.
     * @param groupIdOrName The unique identifier or name of the group to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToGroup(
        template: PermissionTemplateEntity,
        groupIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Applies a permission template identified by its ID or name to a group identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be applied.
     * @param groupIdOrName The unique identifier or name of the group to which the template will be applied.
     * @return Either an error of type ApplyTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was applied successfully.
     */
    fun applyTemplateToGroup(
        templateIdOrName: String,
        groupIdOrName: String
    ): Either<PermissionService.ApplyTemplateError, Boolean>

    /**
     * Revokes a specific template from a group identified by their unique ID.
     *
     * @param templateId The unique identifier of the template to revoke.
     * @param groupId The unique identifier of the group from whom the template will be revoked.
     * @return Either a PermissionService.RevokeTemplateError indicating the error if the operation fails,
     *         or a Boolean value indicating success if the operation is completed.
     */
    fun revokeTemplateFromGroup(
        templateId: UUID,
        groupId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a specific permission template from a group.
     *
     * @param templateId The unique identifier of the permission template to revoke.
     * @param group The group entity from which the permission template will be revoked.
     * @return Either a `RevokeTemplateError` indicating the reason for
     */
    fun revokeTemplateFromGroup(
        templateId: UUID,
        group: GroupEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from a specific group.
     *
     * @param template The permission template to be revoked.
     * @param groupId The unique identifier of the group from whom the template will be revoked.
     * @return Either an error detailing why the revocation failed or a boolean indicating success.
     */
    fun revokeTemplateFromGroup(
        template: PermissionTemplateEntity,
        groupId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from a specified group.
     *
     * @param template The permission template to be revoked from the player.
     * @param group The group entity from whom the permission template is to be revoked.
     * @return Either an error describing the failure to revoke the template or a boolean indicating success.
     */
    fun revokeTemplateFromGroup(
        template: PermissionTemplateEntity,
        group: GroupEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from a group identified by its ID.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param groupId The unique identifier of the group from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromGroup(
        templateIdOrName: String,
        groupId: UUID
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from the specified group.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param group The group entity from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromGroup(
        templateIdOrName: String,
        group: GroupEntity
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template from a group identified by its ID or name.
     *
     * @param templateId The unique identifier of the permission template to be revoked.
     * @param groupIdOrName The unique identifier or name of the group from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromGroup(
        templateId: UUID,
        groupIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes the specified permission template from a group identified by its ID or name.
     *
     * @param template The permission template entity to be revoked.
     * @param groupIdOrName The unique identifier or name of the group from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromGroup(
        template: PermissionTemplateEntity,
        groupIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>

    /**
     * Revokes a permission template identified by its ID or name from a group identified by its ID or name.
     *
     * @param templateIdOrName The unique identifier or name of the permission template to be revoked.
     * @param groupIdOrName The unique identifier or name of the group from which the template will be revoked.
     * @return Either an error of type RevokeTemplateError if the operation fails,
     *         or a Boolean indicating whether the template was revoked successfully.
     */
    fun revokeTemplateFromGroup(
        templateIdOrName: String,
        groupIdOrName: String
    ): Either<PermissionService.RevokeTemplateError, Boolean>
}
