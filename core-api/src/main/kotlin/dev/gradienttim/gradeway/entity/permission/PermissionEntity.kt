/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.permission

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

    /**
     * Validates whether the given permission string matches this entity based on its type.
     *
     * @param permission The permission string to validate against this entity.
     * @return True if the permission matches this entity's value according to its type, false otherwise.
     */
    fun validatePermission(permission: String): Boolean {
        return when (type) {
            Type.EQUALS -> value == permission
            Type.STARTS_WITH -> value.startsWith(permission)
            Type.ENDS_WITH -> value.endsWith(permission)
            Type.REGEX -> regex.matches(permission)
        }
    }

    enum class Type { EQUALS, STARTS_WITH, ENDS_WITH, REGEX }
}
