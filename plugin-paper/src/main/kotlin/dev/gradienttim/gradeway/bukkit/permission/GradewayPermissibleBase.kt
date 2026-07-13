/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.permission

import dev.gradienttim.gradeway.Gradeway
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissibleBase
import org.bukkit.permissions.Permission

class GradewayPermissibleBase(val gradeway: Gradeway, val player: Player) : PermissibleBase(player) {
    override fun isPermissionSet(name: String): Boolean = this.hasPermission(name)
    override fun isPermissionSet(perm: Permission): Boolean = this.isPermissionSet(perm.name)
    override fun hasPermission(perm: Permission): Boolean = this.hasPermission(perm.name)

    override fun hasPermission(inName: String): Boolean {
        return gradeway.permissions.hasEffectivePlayerPermission(player.uniqueId, inName)
    }
}
