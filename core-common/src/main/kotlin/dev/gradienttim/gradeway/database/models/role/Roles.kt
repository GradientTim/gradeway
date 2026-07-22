/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.services.AttributeService
import dev.gradienttim.gradeway.services.GroupService
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.services.RoleService
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.*
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.EntityBatchUpdate
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*

object RolesTable : UUIDTable(name = TableConstants.ROLES_TABLE_NAME) {
    val name = varchar("name", TableConstants.ROLES_TABLE_MAX_NAME_LENGTH).uniqueIndex()
    val weight = integer("weight").default(-1)

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

class DatabaseRoleEntity(id: EntityID<UUID>) : UUIDEntity(id), RoleEntity, KoinComponent {
    companion object : UUIDEntityClass<DatabaseRoleEntity>(RolesTable), JsonSerializable<DatabaseRoleEntity> {
        override fun serialize(data: DatabaseRoleEntity): JsonObject = buildJsonObject {
            put("id", data.id.value.toString())
            put("name", data.name)
            put("weight", data.weight)
            put("createdAt", data.createdAt.toEpochMilli())
            put("updatedAt", data.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabaseRoleEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                name = json.getValue("name").jsonPrimitive.content
                weight = json.getValue("weight").jsonPrimitive.int
                createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
                updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
            }
        }
    }

    internal val roleService: RoleService by inject()
    internal val groupService: GroupService by inject()
    internal val attributeService: AttributeService by inject()
    internal val permissionService: PermissionService by inject()

    override var name by RolesTable.name
    override var weight by RolesTable.weight

    override var createdAt by RolesTable.createdAt
    override var updatedAt by RolesTable.updatedAt

    override val groups by DatabaseRoleGroupEntity referrersOn RoleGroupsTable.roleId
    override val attributes by DatabaseRoleAttributeEntity referrersOn RoleAttributesTable.roleId
    override val permissions by DatabaseRolePermissionEntity referrersOn RolePermissionsTable.roleId
    override val permissionTemplates by DatabaseRolePermissionTemplateEntity referrersOn
            RolePermissionTemplatesTable.roleId

    override val parents by DatabaseRoleParentEntity referrersOn RoleParentsTable.childId
    override val children by DatabaseRoleParentEntity referrersOn RoleParentsTable.parentId

    override fun setName(name: String) = roleService.setName(this, name)
    override fun setWeight(weight: Int) = roleService.setWeight(this, weight)

    override fun addGroup(id: UUID) = groupService.addRoleToGroup(id, this)
    override fun addGroup(idOrName: String) = groupService.addRoleToGroup(idOrName, this)
    override fun addGroup(entity: GroupEntity) = groupService.addRoleToGroup(entity, this)

    override fun removeGroup(id: UUID) = groupService.removeRoleFromGroup(id, this)
    override fun removeGroup(idOrName: String) = groupService.removeRoleFromGroup(idOrName, this)
    override fun removeGroup(entity: GroupEntity) = groupService.removeRoleFromGroup(entity, this)

    override fun <TValue : Any> addAttribute(attribute: Attribute<TValue>) =
        attributeService.addRoleAttribute(this, attribute)

    override fun <TValue : Any> updateAttribute(key: Key, value: TValue) =
        attributeService.updateRoleAttribute(this, key, value)

    override fun removeAttribute(key: Key) = attributeService.removeRoleAttribute(this, key)
    override fun clearAttributes() = attributeService.clearRoleAttributes(this)

    override fun hasAttribute(key: Key) = attributeService.hasRoleAttribute(this, key)
    override fun getAttribute(key: Key) = attributeService.getRoleAttribute(this, key)

    override fun setPermission(permission: String, enabled: Boolean) =
        permissionService.setRolePermission(this, permission, enabled)

    override fun setPermissions(permissions: Map<String, Boolean>) =
        permissionService.setRolePermissions(this, permissions)

    override fun unsetPermission(permission: String) = permissionService.unsetRolePermission(this, permission)
    override fun unsetPermissions(permissions: Collection<String>) =
        permissionService.unsetRolePermissions(this, permissions)

    override fun clearPermissions() = permissionService.clearRolePermissions(this)

    override fun hasPermission(permission: String) = permissionService.hasRolePermission(this, permission)
    override fun hasAnyPermissions(permissions: Collection<String>) =
        permissionService.hasRoleAnyPermissions(this, permissions)

    override fun hasAllPermissions(permissions: Collection<String>) =
        permissionService.hasRoleAllPermissions(this, permissions)

    override fun linkTemplate(id: UUID) = permissionService.linkTemplateToRole(id, this).map { }
    override fun linkTemplate(entity: PermissionTemplateEntity) =
        permissionService.linkTemplateToRole(entity, this).map { }

    override fun unlinkTemplate(id: UUID) = permissionService.unlinkTemplateFromRole(id, this)
    override fun unlinkTemplate(entity: PermissionTemplateEntity) =
        permissionService.unlinkTemplateFromRole(entity, this)

    override fun applyTemplate(id: UUID) = permissionService.applyTemplateToRole(id, this)
    override fun applyTemplate(entity: PermissionTemplateEntity) =
        permissionService.applyTemplateToRole(entity, this)

    override fun revokeTemplate(id: UUID) = permissionService.revokeTemplateFromRole(id, this)
    override fun revokeTemplate(entity: PermissionTemplateEntity) =
        permissionService.revokeTemplateFromRole(entity, this)

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }
}
