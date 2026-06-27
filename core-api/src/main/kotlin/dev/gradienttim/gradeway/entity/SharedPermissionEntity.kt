/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity

import dev.gradienttim.gradeway.entity.permission.PermissionEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

interface SharedPermissionEntity {
    val permissionId: EntityID<UUID>
    var isEnabled: Boolean

    val permission: PermissionEntity

    fun flush(): Boolean
    fun delete()
}
