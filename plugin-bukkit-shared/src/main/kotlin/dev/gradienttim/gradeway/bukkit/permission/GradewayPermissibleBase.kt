/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.permission

import dev.gradienttim.gradeway.GradewayLifecycle
import dev.gradienttim.gradeway.bukkit.config.BukkitPlatformConfig
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissibleBase
import org.bukkit.permissions.Permission

class GradewayPermissibleBase(
    private val gradeway: GradewayLifecycle<BukkitPlatformConfig>?,
    val player: Player?,
) : PermissibleBase(player) {
    override fun isPermissionSet(name: String): Boolean = this.hasPermission(name)
    override fun isPermissionSet(perm: Permission): Boolean = this.isPermissionSet(perm.name)
    override fun hasPermission(perm: Permission): Boolean = this.hasPermission(perm.name)

    override fun hasPermission(inName: String): Boolean {
        val playerId = player?.uniqueId ?: return false
        val gradeway = gradeway ?: return super.hasPermission(inName)
        return gradeway.permissions.hasEffectivePlayerPermission(playerId, inName)
    }

    override fun isOp(): Boolean {
        if (gradeway?.configs?.platformEntry?.config?.disableOp == true) {
            if (player?.isOp == true) {
                isOp = false
            }
            return false
        }
        return false
    }
}
