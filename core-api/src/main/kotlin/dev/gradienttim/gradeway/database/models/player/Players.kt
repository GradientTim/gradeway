/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.player

import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.constants.SerializationConstants
import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.database.entities.AttributeEntity
import dev.gradienttim.gradeway.database.entities.PermissionEntity
import dev.gradienttim.gradeway.services.AttributeService
import dev.gradienttim.gradeway.services.PlayerService
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.SetSerializer
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.EntityBatchUpdate
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*

object PlayersTable : UUIDTable(name = TableConstants.PLAYERS_TABLE_NAME) {
    val name = varchar("name", TableConstants.PLAYERS_TABLE_MAX_NAME_LENGTH).uniqueIndex()

    val attributes = json<Set<Attribute<*>>>(
        name = "attributes",
        jsonConfig = SerializationConstants.JSON,
        kSerializer = SetSerializer(PolymorphicSerializer(Attribute::class))
    ).default(emptySet())

    val permissions = json<Map<String, Boolean>>("permissions", SerializationConstants.JSON).default(emptyMap())

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    init {
        uniqueIndex(id)
        uniqueIndex(id, name)
    }
}

class PlayerEntity(id: EntityID<UUID>) : UUIDEntity(id), AttributeEntity, PermissionEntity, KoinComponent {
    companion object : UUIDEntityClass<PlayerEntity>(PlayersTable)

    internal val playerService by inject<PlayerService>()
    internal val attributeService by inject<AttributeService>()

    var name by PlayersTable.name
    override var attributes by PlayersTable.attributes
    override var permissions by PlayersTable.permissions

    val createdAt by PlayersTable.createdAt
    var updatedAt by PlayersTable.updatedAt

    val roles by PlayerRoleEntity referrersOn PlayerRolesTable.playerId

    fun setName(name: String) = playerService.setName(this, name)

    fun <TValue : Any> addAttribute(attribute: Attribute<TValue>) =
        attributeService.addPlayerAttribute(this, attribute)

    fun <TValue : Any> updateAttribute(key: Key, value: TValue) =
        attributeService.updatePlayerAttribute(this, key, value)

    fun removeAttribute(key: Key) = attributeService.removePlayerAttribute(this, key)
    fun hasAttribute(key: Key) = attributeService.hasPlayerAttribute(this, key)
    fun getAttribute(key: Key) = attributeService.getPlayerAttribute(this, key)

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }
}
