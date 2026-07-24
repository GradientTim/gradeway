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
    // PermissibleBase's constructor calls recalculatePermissions(), which invokes the overrides
    // below before this field is assigned - nullable so that transient state is safe to check.
    private val gradeway: GradewayLifecycle<BukkitPlatformConfig>?,
    val player: Player,
) : PermissibleBase(player) {
    override fun isPermissionSet(name: String): Boolean = this.hasPermission(name)
    override fun isPermissionSet(perm: Permission): Boolean = this.isPermissionSet(perm.name)
    override fun hasPermission(perm: Permission): Boolean = this.hasPermission(perm.name)

    override fun hasPermission(inName: String): Boolean {
        val gradeway = gradeway ?: return super.hasPermission(inName)
        return gradeway.permissions.hasEffectivePlayerPermission(player.uniqueId, inName)
    }

    // CraftHumanEntity#isOp()/#setOp() talk directly to the server's real operator list and never
    // delegate to this Permissible, so overriding isOp()/setOp() has no effect on real op status - it
    // only affects callers holding a direct reference to this Permissible. CraftHumanEntity#setOp()
    // does, however, always call recalculatePermissions() right after updating the real list, so this
    // is the one reliable hook to catch every real op change (join, mid-game /op, console, other
    // plugins) and force a real de-op when disableOp is enabled.
    override fun recalculatePermissions() {
        super.recalculatePermissions()
        val gradeway = gradeway ?: return
        if (gradeway.configs.config.platform.disableOp && super.isOp()) {
            super.setOp(false)
        }
    }
}
