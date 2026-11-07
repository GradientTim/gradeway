/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.platform

import dev.gradienttim.gradeway.platform.KnownPermissionSource
import net.md_5.bungee.api.ProxyServer

class BungeeCordKnownPermissionSource(private val proxy: ProxyServer) : KnownPermissionSource {
    override fun collect(): Collection<String> =
        proxy.pluginManager.commands.mapNotNull { it.value.permission?.takeIf(String::isNotEmpty) }
}
