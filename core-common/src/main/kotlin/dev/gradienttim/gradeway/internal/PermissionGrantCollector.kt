/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.internal

import dev.gradienttim.gradeway.Gradeway
import dev.gradienttim.gradeway.entity.SharedPermissionEntity
import dev.gradienttim.gradeway.entity.SharedPermissionTemplateEntity
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.reference.PermissionReference
import dev.gradienttim.gradeway.reference.PermissionTemplateReference
import dev.gradienttim.gradeway.services.permission.PermissionInspectionService.GrantSource
import dev.gradienttim.gradeway.services.permission.PermissionInspectionService.PermissionGrant
import java.util.*

internal class PermissionGrantCollector(
    private val permission: String,
    private val gradeway: Gradeway<*>,
) {
    private val grants = mutableListOf<PermissionGrant>()

    fun result(): List<PermissionGrant> = grants
        .distinctBy { grant -> grant.path.map { it.id } to grant.permission.id.value }
        .sortedBy { it.path.size }

    fun collectOwn(path: List<GrantSource>, entity: PermissionReference<out SharedPermissionEntity>) {
        entity.permissions
            .filter { it.isEnabled }
            .map { it.permission }
            .filter { it.validatePermission(permission) }
            .forEach { add(path, it) }
    }

    fun collectTemplates(
        path: List<GrantSource>,
        entity: PermissionTemplateReference<out SharedPermissionTemplateEntity>,
    ) {
        entity.permissionTemplates.forEach { link ->
            val template = link.permissionTemplate
            val templatePath = path + GrantSource.Template(template.id.value, template.name)
            template.permissions
                .map { it.permission }
                .filter { it.validatePermission(permission) }
                .forEach { add(templatePath, it) }
        }
    }

    fun collectGroup(path: List<GrantSource>, group: GroupEntity) {
        val groupPath = path + GrantSource.Group(group.id.value, group.name)
        collectOwn(groupPath, group)
        collectTemplates(groupPath, group)
    }

    fun collectRole(path: List<GrantSource>, role: RoleEntity, ancestors: Set<UUID>) {
        val roleId = role.id.value
        if (roleId in ancestors || grants.size >= MAX_GRANTS) {
            return
        }
        if (!gradeway.permissions.hasEffectiveRolePermission(roleId, permission)) {
            return
        }

        val rolePath = path + GrantSource.Role(roleId, role.name)
        collectOwn(rolePath, role)
        collectTemplates(rolePath, role)
        role.groups.forEach { collectGroup(rolePath, it.group) }
        role.parents.forEach { collectRole(rolePath, it.parent, ancestors + roleId) }
    }

    private fun add(path: List<GrantSource>, permissionEntity: PermissionEntity) {
        if (grants.size < MAX_GRANTS) {
            grants += PermissionGrant(path, permissionEntity)
        }
    }

    companion object {
        /** Upper bound on reported grant paths, keeping pathological hierarchies cheap to explain. */
        const val MAX_GRANTS = 50
    }
}
