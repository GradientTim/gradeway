/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.models.role.DatabaseRoleEntity
import dev.gradienttim.gradeway.database.models.role.RolesTable
import dev.gradienttim.gradeway.entity.player.PlayerEntity
import dev.gradienttim.gradeway.services.AttributeService
import dev.gradienttim.gradeway.services.PlayerService
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
    companion object : UUIDEntityClass<DatabasePlayerEntity>(PlayersTable)

    internal val playerService: PlayerService by inject()
    internal val attributeService: AttributeService by inject()

    override var name by PlayersTable.name

    override var primaryRoleId by PlayersTable.primaryRoleId

    override val createdAt by PlayersTable.createdAt
    override var updatedAt by PlayersTable.updatedAt

    override val primaryRole by DatabaseRoleEntity optionalReferencedOn PlayersTable.primaryRoleId

    override val roles by DatabasePlayerRoleEntity referrersOn PlayerRolesTable.playerId
    override val attributes by DatabasePlayerAttributeEntity referrersOn PlayerAttributesTable.playerId
    override val permissions by DatabasePlayerPermissionEntity referrersOn PlayerPermissionsTable.playerId
    override val permissionTemplates by DatabasePlayerPermissionTemplateEntity referrersOn
            PlayerPermissionTemplatesTable.playerId

    override fun setName(name: String) = playerService.setName(this, name)

    override fun <TValue : Any> addAttribute(attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(this, attribute)

    override fun <TValue : Any> updateAttribute(key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(this, key, value)

    override fun removeAttribute(key: Key) = attributeService.removePlayerAttribute(this, key)
    override fun clearAttributes() = attributeService.clearPlayerAttributes(this)

    override fun hasAttribute(key: Key) = attributeService.hasPlayerAttribute(this, key)
    override fun getAttribute(key: Key) = attributeService.getPlayerAttribute(this, key)

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }
}
