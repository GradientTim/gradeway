/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.permission

import net.kyori.adventure.util.TriState
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

interface PermissionEntity {
    val id: EntityID<UUID>
    var value: String
    var type: Type

    val regex: Regex
        get() {
            if (type != Type.REGEX) {
                error("This permission is not a Regex type.")
            }
            return Regex(value)
        }

    fun validatePermission(permission: String): TriState {
        return TriState.byBoolean(when (type) {
            Type.EQUALS -> value == permission
            Type.STARTS_WITH -> value.startsWith(permission)
            Type.ENDS_WITH -> value.endsWith(permission)
            Type.REGEX -> regex.matches(permission)
        })
    }

    fun flush(): Boolean
    fun delete()

    enum class Type { EQUALS, STARTS_WITH, ENDS_WITH, REGEX }
}
