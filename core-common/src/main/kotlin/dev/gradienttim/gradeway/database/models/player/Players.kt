/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.audit.Target
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.entity.permission.PermissionTemplateEntity
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.entity.track.TrackEntity
import dev.gradienttim.gradeway.services.AttributeService
import dev.gradienttim.gradeway.services.PermissionService
import dev.gradienttim.gradeway.services.PlayerService
import dev.gradienttim.gradeway.services.TrackService
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

    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

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

    override val target by lazy { Target.player(id.value) }

    internal val trackService: TrackService by inject()
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

    override fun setName(actor: Actor, name: String) = playerService.setName(actor, this, name)
    override fun setWeight(actor: Actor, weight: Int) = playerService.setWeight(actor, this, weight)

    override fun <TValue : Any> addAttribute(actor: Actor, attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(actor, this, attribute)

    override fun <TValue : Any> updateAttribute(actor: Actor, key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(actor, this, key, value)

    override fun removeAttribute(actor: Actor, key: Key) = attributeService.removePlayerAttribute(actor, this, key)
    override fun clearAttributes(actor: Actor) = attributeService.clearPlayerAttributes(actor, this)

    override fun hasAttribute(key: Key) = attributeService.hasPlayerAttribute(this, key)
    override fun getAttribute(key: Key) = attributeService.getPlayerAttribute(this, key)

    override fun setPermission(actor: Actor, permission: String, enabled: Boolean) =
        permissionService.setPlayerPermission(actor, this, permission, enabled)

    override fun setPermissions(actor: Actor, permissions: Map<String, Boolean>) =
        permissionService.setPlayerPermissions(actor, this, permissions)

    override fun unsetPermission(
        actor: Actor,
        permission: String,
    ) = permissionService.unsetPlayerPermission(actor, this, permission)
    override fun unsetPermissions(actor: Actor, permissions: Collection<String>) =
        permissionService.unsetPlayerPermissions(actor, this, permissions)

    override fun clearPermissions(actor: Actor) = permissionService.clearPlayerPermissions(actor, this)

    override fun hasPermission(permission: String) = permissionService.hasPlayerPermission(this, permission)
    override fun hasAnyPermissions(permissions: Collection<String>) =
        permissionService.hasPlayerAnyPermissions(this, permissions)

    override fun hasAllPermissions(permissions: Collection<String>) =
        permissionService.hasPlayerAllPermissions(this, permissions)

    override fun linkTemplate(actor: Actor, id: UUID) = permissionService.linkTemplateToPlayer(actor, id, this)
    override fun linkTemplate(actor: Actor, entity: PermissionTemplateEntity) =
        permissionService.linkTemplateToPlayer(actor, entity, this)

    override fun unlinkTemplate(actor: Actor, id: UUID) = permissionService.unlinkTemplateFromPlayer(actor, id, this)
    override fun unlinkTemplate(actor: Actor, entity: PermissionTemplateEntity) =
        permissionService.unlinkTemplateFromPlayer(actor, entity, this)

    override fun applyTemplate(actor: Actor, id: UUID) = permissionService.applyTemplateToPlayer(actor, id, this)
    override fun applyTemplate(actor: Actor, entity: PermissionTemplateEntity) =
        permissionService.applyTemplateToPlayer(actor, entity, this)

    override fun revokeTemplate(actor: Actor, id: UUID) = permissionService.revokeTemplateFromPlayer(actor, id, this)
    override fun revokeTemplate(actor: Actor, entity: PermissionTemplateEntity) =
        permissionService.revokeTemplateFromPlayer(actor, entity, this)

    override fun promote(actor: Actor, trackId: UUID) = trackService.promotePlayer(actor, this, trackId)
    override fun promote(actor: Actor, track: TrackEntity) = trackService.promotePlayer(actor, this, track)

    override fun demote(actor: Actor, trackId: UUID) = trackService.demotePlayer(actor, this, trackId)
    override fun demote(actor: Actor, track: TrackEntity) = trackService.demotePlayer(actor, this, track)

    override fun findCurrentStage(trackId: UUID) = trackService.findPlayersCurrentStage(this, trackId)
    override fun findCurrentStage(track: TrackEntity) = trackService.findPlayersCurrentStage(this, track)

    override fun findNextStage(trackId: UUID) = trackService.findPlayersNextStage(this, trackId)
    override fun findNextStage(track: TrackEntity) = trackService.findPlayersNextStage(this, track)

    override fun findPreviousStage(trackId: UUID) = trackService.findPlayersPreviousStage(this, trackId)
    override fun findPreviousStage(track: TrackEntity) = trackService.findPlayersPreviousStage(this, track)

    override fun isOnTrack(trackId: UUID) = trackService.isPlayerOnTrack(this, trackId)
    override fun isOnTrack(track: TrackEntity) = trackService.isPlayerOnTrack(this, track)

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }

    override fun snapshot(): JsonObject = buildJsonObject {
        put("id", id.value.toString())
        put("name", name)
        put("weight", weight)
        put("primaryRoleId", primaryRoleId?.value?.toString())
        put("createdAt", createdAt.toEpochMilli())
        put("updatedAt", updatedAt.toEpochMilli())
    }
}
