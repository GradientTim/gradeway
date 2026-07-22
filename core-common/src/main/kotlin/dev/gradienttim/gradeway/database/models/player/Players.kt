/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.services.AttributeService
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.services.PlayerService
import dev.gradienttim.gradeway.utilities.serialize.JsonSerializable
import kotlinx.serialization.json.*
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.ReferenceOption
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

object PlayersTable : UUIDTable(name = TableConstants.PLAYERS_TABLE_NAME) {
    val name = varchar("name", TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH).uniqueIndex()
    val weight = integer("weight").default(-1)

    val primaryRoleId = optReference(
        name = "primary_role_id",
        refColumn = RolesTable.id,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    init {
        uniqueIndex(id)
        uniqueIndex(id, name)
    }
}

class DatabasePlayerEntity(id: EntityID<UUID>) : UUIDEntity(id), PlayerEntity, KoinComponent {
    companion object : UUIDEntityClass<DatabasePlayerEntity>(PlayersTable), JsonSerializable<DatabasePlayerEntity> {
        override fun serialize(data: DatabasePlayerEntity): JsonObject = buildJsonObject {
            put("id", data.id.value.toString())
            put("name", data.name)
            put("weight", data.weight)
            put("primaryRoleId", data.primaryRoleId?.value?.toString())
            put("createdAt", data.createdAt.toEpochMilli())
            put("updatedAt", data.updatedAt.toEpochMilli())
        }

        override fun deserialize(json: JsonObject): DatabasePlayerEntity {
            val id = UUID.fromString(json.getValue("id").jsonPrimitive.content)

            return new(id) {
                name = json.getValue("name").jsonPrimitive.content
                weight = json.getValue("weight").jsonPrimitive.int

                json.getValue("primaryRoleId").jsonPrimitive.contentOrNull?.let { rawPrimaryRoleId ->
                    primaryRoleId = EntityID(
                        id = UUID.fromString(rawPrimaryRoleId),
                        table = RolesTable
                    )
                }

                createdAt = Instant.ofEpochMilli(json.getValue("createdAt").jsonPrimitive.long)
                updatedAt = Instant.ofEpochMilli(json.getValue("updatedAt").jsonPrimitive.long)
            }
        }
    }

    internal val playerService: PlayerService by inject()
    internal val attributeService: AttributeService by inject()
    internal val permissionService: PermissionService by inject()

    override var name by PlayersTable.name
    override var weight by PlayersTable.weight

    override var primaryRoleId by PlayersTable.primaryRoleId

    override var createdAt by PlayersTable.createdAt
    override var updatedAt by PlayersTable.updatedAt

    override val primaryRole by DatabaseRoleEntity optionalReferencedOn PlayersTable.primaryRoleId

    override val roles by DatabasePlayerRoleEntity referrersOn PlayerRolesTable.playerId
    override val attributes by DatabasePlayerAttributeEntity referrersOn PlayerAttributesTable.playerId
    override val permissions by DatabasePlayerPermissionEntity referrersOn PlayerPermissionsTable.playerId
    override val permissionTemplates by DatabasePlayerPermissionTemplateEntity referrersOn
            PlayerPermissionTemplatesTable.playerId

    override fun setName(name: String) = playerService.setName(this, name)
    override fun setWeight(weight: Int) = playerService.setWeight(this, weight)

    override fun <TValue : Any> addAttribute(attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(this, attribute)

    override fun <TValue : Any> updateAttribute(key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(this, key, value)

    override fun removeAttribute(key: Key) = attributeService.removePlayerAttribute(this, key)
    override fun clearAttributes() = attributeService.clearPlayerAttributes(this)

    override fun hasAttribute(key: Key) = attributeService.hasPlayerAttribute(this, key)
    override fun getAttribute(key: Key) = attributeService.getPlayerAttribute(this, key)

    override fun setPermission(permission: String, enabled: Boolean) =
        permissionService.setPlayerPermission(this, permission, enabled)

    override fun setPermissions(permissions: Map<String, Boolean>) =
        permissionService.setPlayerPermissions(this, permissions)

    override fun unsetPermission(permission: String) = permissionService.unsetPlayerPermission(this, permission)
    override fun unsetPermissions(permissions: Collection<String>) =
        permissionService.unsetPlayerPermissions(this, permissions)

    override fun clearPermissions() = permissionService.clearPlayerPermissions(this)

    override fun hasPermission(permission: String) = permissionService.hasPlayerPermission(this, permission)
    override fun hasAnyPermissions(permissions: Collection<String>) =
        permissionService.hasPlayerAnyPermissions(this, permissions)

    override fun hasAllPermissions(permissions: Collection<String>) =
        permissionService.hasPlayerAllPermissions(this, permissions)

    override fun linkTemplate(id: UUID) = permissionService.linkTemplateToPlayer(id, this)
    override fun linkTemplate(entity: PermissionTemplateEntity) =
        permissionService.linkTemplateToPlayer(entity, this)

    override fun unlinkTemplate(id: UUID) = permissionService.unlinkTemplateFromPlayer(id, this)
    override fun unlinkTemplate(entity: PermissionTemplateEntity) =
        permissionService.unlinkTemplateFromPlayer(entity, this)

    override fun applyTemplate(id: UUID) = permissionService.applyTemplateToPlayer(id, this)
    override fun applyTemplate(entity: PermissionTemplateEntity) =
        permissionService.applyTemplateToPlayer(entity, this)

    override fun revokeTemplate(id: UUID) = permissionService.revokeTemplateFromPlayer(id, this)
    override fun revokeTemplate(entity: PermissionTemplateEntity) =
        permissionService.revokeTemplateFromPlayer(entity, this)

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }
}
