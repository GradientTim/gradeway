/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.constants

object TableConstants {
    var TABLE_PREFIX: String = "gradeway_"

    val ROLES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}roles" }
    val ROLE_GROUPS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}role_groups" }
    val ROLE_PARENTS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}role_parents" }
    val ROLE_ATTRIBUTES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}role_attributes" }
    val ROLE_PERMISSIONS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}role_permissions" }
    val ROLE_PERMISSION_TEMPLATES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}role_permission_templates" }
    val PLAYERS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}players" }
    val PLAYER_ROLES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}player_roles" }
    val PLAYER_ATTRIBUTES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}player_attributes" }
    val PLAYER_PERMISSIONS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}player_permissions" }
    val PLAYER_PERMISSION_TEMPLATES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}player_permission_templates" }
    val PERMISSIONS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}permissions" }
    val PERMISSION_TEMPLATES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}permission_templates" }
    val PERMISSION_TEMPLATE_PERMISSIONS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}permission_template_permissions" }
    val GROUPS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}groups" }
    val GROUP_PERMISSIONS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}group_permissions" }
    val GROUP_PERMISSION_TEMPLATES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}group_permission_templates" }

    const val ROLES_TABLE_MAX_NAME_LENGTH: Int = 30
    const val PLAYERS_TABLE_MAX_NAME_LENGTH: Int = 16
    const val PERMISSION_TEMPLATES_TABLE_MAX_NAME_LENGTH: Int = 20
    const val GROUPS_TABLE_MAX_NAME_LENGTH: Int = 20
}
