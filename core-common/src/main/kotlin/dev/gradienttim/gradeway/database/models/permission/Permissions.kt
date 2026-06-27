/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.models.permission

import dev.gradienttim.gradeway.constants.TableConstants
import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.*

object PermissionsTable : UUIDTable(name = TableConstants.PERMISSIONS_TABLE_NAME) {
    val value = text("value").uniqueIndex()
    val type = enumeration<PermissionEntity.Type>("type").default(PermissionEntity.Type.EQUALS)
}

class DatabasePermissionEntity(id: EntityID<UUID>) : UUIDEntity(id), PermissionEntity {
    companion object : UUIDEntityClass<DatabasePermissionEntity>(PermissionsTable)

    override var value by PermissionsTable.value
    override var type by PermissionsTable.type

    override fun flush() = flush(null)
}
