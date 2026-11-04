/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.Target
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
    val isDefault = bool("is_default").default(false)

    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

class DatabaseRoleEntity(id: EntityID<UUID>) : UUIDEntity(id), RoleEntity, KoinComponent {
    companion object : UUIDEntityClass<DatabaseRoleEntity>(RolesTable), JsonSerializable<DatabaseRoleEntity> {
        override fun serialize(data: DatabaseRoleEntity): JsonObject = buildJsonObject {
            put("id", data.id.value.toString())
            put("name", data.name)
            put("weight", data.weight)
            put("isDefault", data.isDefault)
            put("createdAt", data.createdAt.toEpochMilli())
            put("updatedAt", data.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabaseRoleEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                name = json.getValue("name").jsonPrimitive.content
                weight = json.getValue("weight").jsonPrimitive.int
                isDefault = json["isDefault"]?.jsonPrimitive?.booleanOrNull ?: false
                createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
                updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
            }
        }
    }

    internal val roleService: RoleService by inject()
    internal val groupService: GroupService by inject()
    internal val attributeService: AttributeService by inject()
    internal val permissionService: PermissionService by inject()

    override val target by lazy { Target.role(id.value) }

    override var name by RolesTable.name
    override var weight by RolesTable.weight
    override var isDefault by RolesTable.isDefault

    override var createdAt by RolesTable.createdAt
    override var updatedAt by RolesTable.updatedAt

    override val groups by DatabaseRoleGroupEntity referrersOn RoleGroupsTable.roleId
    override val attributes by DatabaseRoleAttributeEntity referrersOn RoleAttributesTable.roleId
    override val permissions by DatabaseRolePermissionEntity referrersOn RolePermissionsTable.roleId
    override val permissionTemplates by DatabaseRolePermissionTemplateEntity referrersOn
            RolePermissionTemplatesTable.roleId

    override val parents by DatabaseRoleParentEntity referrersOn RoleParentsTable.childId
    override val children by DatabaseRoleParentEntity referrersOn RoleParentsTable.parentId

    override fun setName(actor: Actor, name: String) = roleService.setName(actor, this, name)
    override fun setWeight(actor: Actor, weight: Int) = roleService.setWeight(actor, this, weight)

    override fun addGroup(actor: Actor, id: UUID) = groupService.addRoleToGroup(actor, id, this)
    override fun addGroup(actor: Actor, idOrName: String) = groupService.addRoleToGroup(actor, idOrName, this)
    override fun addGroup(actor: Actor, entity: GroupEntity) = groupService.addRoleToGroup(actor, entity, this)

    override fun removeGroup(actor: Actor, id: UUID) = groupService.removeRoleFromGroup(actor, id, this)
    override fun removeGroup(actor: Actor, idOrName: String) = groupService.removeRoleFromGroup(actor, idOrName, this)
    override fun removeGroup(actor: Actor, entity: GroupEntity) = groupService.removeRoleFromGroup(actor, entity, this)

    override fun <TValue : Any> addAttribute(actor: Actor, attribute: Attribute<TValue>) =
        attributeService.addRoleAttribute(actor, this, attribute)

    override fun <TValue : Any> updateAttribute(actor: Actor, key: Key, value: TValue) =
        attributeService.updateRoleAttribute(actor, this, key, value)

    override fun removeAttribute(actor: Actor, key: Key) = attributeService.removeRoleAttribute(actor, this, key)
    override fun clearAttributes(actor: Actor) = attributeService.clearRoleAttributes(actor, this)

    override fun hasAttribute(key: Key) = attributeService.hasRoleAttribute(this, key)
    override fun getAttribute(key: Key) = attributeService.getRoleAttribute(this, key)

    override fun setPermission(actor: Actor, permission: String, enabled: Boolean) =
        permissionService.setRolePermission(actor, this, permission, enabled)

    override fun setPermissions(actor: Actor, permissions: Map<String, Boolean>) =
        permissionService.setRolePermissions(actor, this, permissions)

    override fun unsetPermission(
        actor: Actor,
        permission: String,
    ) = permissionService.unsetRolePermission(actor, this, permission)

    override fun unsetPermissions(actor: Actor, permissions: Collection<String>) =
        permissionService.unsetRolePermissions(actor, this, permissions)

    override fun clearPermissions(actor: Actor) = permissionService.clearRolePermissions(actor, this)

    override fun hasPermission(permission: String) = permissionService.hasRolePermission(this, permission)
    override fun hasAnyPermissions(permissions: Collection<String>) =
        permissionService.hasRoleAnyPermissions(this, permissions)

    override fun hasAllPermissions(permissions: Collection<String>) =
        permissionService.hasRoleAllPermissions(this, permissions)

    override fun linkTemplate(actor: Actor, id: UUID) = permissionService.linkTemplateToRole(actor, id, this).map { }
    override fun linkTemplate(actor: Actor, entity: PermissionTemplateEntity) =
        permissionService.linkTemplateToRole(actor, entity, this).map { }

    override fun unlinkTemplate(actor: Actor, id: UUID) = permissionService.unlinkTemplateFromRole(actor, id, this)
    override fun unlinkTemplate(actor: Actor, entity: PermissionTemplateEntity) =
        permissionService.unlinkTemplateFromRole(actor, entity, this)

    override fun applyTemplate(actor: Actor, id: UUID) = permissionService.applyTemplateToRole(actor, id, this)
    override fun applyTemplate(actor: Actor, entity: PermissionTemplateEntity) =
        permissionService.applyTemplateToRole(actor, entity, this)

    override fun revokeTemplate(actor: Actor, id: UUID) = permissionService.revokeTemplateFromRole(actor, id, this)
    override fun revokeTemplate(actor: Actor, entity: PermissionTemplateEntity) =
        permissionService.revokeTemplateFromRole(actor, entity, this)

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }

    override fun snapshot(): JsonObject = buildJsonObject {
        put("id", id.value.toString())
        put("name", name)
        put("weight", weight)
        put("isDefault", isDefault)
        put("createdAt", createdAt.toEpochMilli())
        put("updatedAt", updatedAt.toEpochMilli())
    }
}
