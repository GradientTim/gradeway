/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.role

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.role.RoleEntity
import dev.gradienttim.gradeway.services.AttributeService
import dev.gradienttim.gradeway.services.RoleService
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
    val weight = integer("weight").default(0)

    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

class DatabaseRoleEntity(id: EntityID<UUID> ) : UUIDEntity(id), RoleEntity, KoinComponent {
    companion object : UUIDEntityClass<DatabaseRoleEntity>(RolesTable)

    internal val roleService by inject<RoleService>()
    internal val attributeService by inject<AttributeService>()

    override var name by RolesTable.name
    override var weight by RolesTable.weight

    override val createdAt by RolesTable.createdAt
    override var updatedAt by RolesTable.updatedAt

    override val attributes by DatabaseRoleAttributeEntity referrersOn RolesTable.id
    override val permissions by DatabaseRolePermissionEntity referrersOn RolesTable.id
    override val permissionTemplates by DatabaseRolePermissionTemplateEntity referrersOn RolesTable.id

    override fun setName(name: String) = roleService.setName(this, name)
    override fun setWeight(weight: Int) = roleService.setWeight(this, weight)

    override fun flush(batch: EntityBatchUpdate?): Boolean {
        updatedAt = Instant.now()
        return super.flush(batch)
    }

    override fun flush(): Boolean = flush(null)
}
