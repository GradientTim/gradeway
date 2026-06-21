/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.constants

object TableConstants {
    var TABLE_PREFIX: String = "gradeway_"

    val ROLES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}roles" }
    val ROLE_PARENTS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}role_parents" }
    val PLAYERS_TABLE_NAME: String by lazy { "${TABLE_PREFIX}players" }
    val PLAYER_ROLES_TABLE_NAME: String by lazy { "${TABLE_PREFIX}player_roles" }

    const val ROLES_TABLE_MAX_NAME_LENGTH: Int = 30
    const val PLAYERS_TABLE_MAX_NAME_LENGTH: Int = 16
}
