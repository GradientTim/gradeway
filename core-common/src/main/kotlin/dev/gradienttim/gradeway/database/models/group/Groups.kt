/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.group

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RoleGroupsTable
import dev.gradienttim.gradeway.entity.group.GroupEntity
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.*
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

object GroupsTable : UUIDTable(name = TableConstants.GROUPS_TABLE_NAME) {
    val name = varchar("name", TableConstants.GROUPS_TABLE_MAX_NAME_LENGTH)
    val defaultWeight = integer("default_weight").default(-1)

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

class DatabaseGroupEntity(id: EntityID<UUID>) : UUIDEntity(id), GroupEntity, KoinComponent {
    companion object : UUIDEntityClass<DatabaseGroupEntity>(GroupsTable), JsonSerializable<DatabaseGroupEntity> {
        override fun serialize(data: DatabaseGroupEntity): JsonObject = buildJsonObject {
            put("id", data.id.value.toString())
            put("name", data.name)
            put("defaultWeight", data.defaultWeight)
            put("createdAt", data.createdAt.toEpochMilli())
            put("updatedAt", data.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabaseGroupEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                name = json.getValue("name").jsonPrimitive.content
                defaultWeight = json.getValue("defaultWeight").jsonPrimitive.int
                createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
                updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
            }
        }
    }

    internal val permissionService: PermissionService by inject()

    override var name by GroupsTable.name
    override var defaultWeight by GroupsTable.defaultWeight

    override var createdAt by GroupsTable.createdAt
    override var updatedAt by GroupsTable.updatedAt

    override val roles by DatabaseRoleEntity via RoleGroupsTable
    override val permissions by DatabaseGroupPermissionEntity referrersOn GroupPermissionsTable.groupId
    override val permissionTemplates by DatabaseGroupPermissionTemplateEntity referrersOn
            GroupPermissionTemplatesTable.groupId

    override fun setPermission(permission: String, enabled: Boolean) =
        permissionService.setGroupPermission(this, permission, enabled)

    override fun setPermissions(permissions: Map<String, Boolean>) =
        permissionService.setGroupPermissions(this, permissions)

    override fun unsetPermission(permission: String) = permissionService.unsetGroupPermission(this, permission)
    override fun unsetPermissions(permissions: Collection<String>) =
        permissionService.unsetGroupPermissions(this, permissions)

    override fun clearPermissions() = permissionService.clearGroupPermissions(this)

    override fun hasPermission(permission: String) = permissionService.hasGroupPermission(this, permission)
    override fun hasAnyPermissions(permissions: Collection<String>) =
        permissionService.hasGroupAnyPermissions(this, permissions)

    override fun hasAllPermissions(permissions: Collection<String>) =
        permissionService.hasGroupAllPermissions(this, permissions)

    override fun linkTemplate(id: UUID) = permissionService.linkTemplateToGroup(id, this)
    override fun linkTemplate(entity: PermissionTemplateEntity) =
        permissionService.linkTemplateToGroup(entity, this)

    override fun unlinkTemplate(id: UUID) = permissionService.unlinkTemplateFromGroup(id, this)
    override fun unlinkTemplate(entity: PermissionTemplateEntity) =
        permissionService.unlinkTemplateFromGroup(entity, this)

    override fun applyTemplate(id: UUID) = permissionService.applyTemplateToGroup(id, this)
    override fun applyTemplate(entity: PermissionTemplateEntity) =
        permissionService.applyTemplateToGroup(entity, this)

    override fun revokeTemplate(id: UUID) = permissionService.revokeTemplateFromGroup(id, this)
    override fun revokeTemplate(entity: PermissionTemplateEntity) =
        permissionService.revokeTemplateFromGroup(entity, this)

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }
}
