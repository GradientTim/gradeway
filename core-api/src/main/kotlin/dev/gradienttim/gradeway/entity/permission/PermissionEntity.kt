/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.entity.permission

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.util.*

/**
 * Represents a permission entity used for validating permission strings based on specific matching types.
 */
interface PermissionEntity {
    /**
     * Unique identifier for an entity of type PermissionEntity.
     * This identifier is represented as an EntityID with a UUID backing value
     * and is used to uniquely distinguish individual permission entities.
     */
    val id: EntityID<UUID>

    /**
     * Represents the value associated with a permission entity.
     *
     * This property determines the main string value used for
     * validating permissions. The interpretation of this value
     * depends on the `type` of the `PermissionEntity`, which
     * might require exact matching, pattern matching, or prefix/suffix
     * validation based on the entity's defined behavior.
     */
    var value: String

    /**
     * Defines the matching strategy used for permission validation.
     *
     * This property specifies the type of comparison to be performed when
     * validating a permission string against the `value` of the permission entity.
     *
     * The available types are:
     * - `EQUALS`: Requires an exact match with the permission string.
     * - `STARTS_WITH`: Matches if the permission string starts with the specified value.
     * - `ENDS_WITH`: Matches if the permission string ends with the specified value.
     * - `REGEX`: Matches based on a regular expression pattern defined in the value.
     */
    var type: Type

    /**
     * A property that retrieves a compiled regular expression from the `value` of this permission entity.
     * This property is only valid when the `type` of the entity is `Type.REGEX`. If accessed while the
     * `type` is not `Type.REGEX`, an error is thrown.
     *
     * @throws IllegalStateException if the entity's `type` is not `Type.REGEX`.
     * @return A `Regex` object compiled from the `value` of this entity.
     */
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

    /**
     * Represents the type of matching mechanism used in permission validation.
     *
     * The available types are:
     * - `EQUALS`: Represents an exact match. The values must be identical.
     * - `STARTS_WITH`: Represents a prefix match. The value must start with the given criteria.
     * - `ENDS_WITH`: Represents a suffix match. The value must end with the given criteria.
     * - `REGEX`: Represents a match using regular expressions. The value must conform to the specified regex pattern.
     */
    enum class Type { EQUALS, STARTS_WITH, ENDS_WITH, REGEX }
}
