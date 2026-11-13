/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.update

import dev.gradienttim.gradeway.managers.CommonUpdateManager
import dev.gradienttim.gradeway.utilities.SemVer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.translation.Argument

object UpdateMessages {
    const val PERMISSION: String = "gradeway.update.notify"

    val DOWNLOAD_PAGES: List<Pair<String, String>> = listOf(
        "GitHub" to "https://github.com/${CommonUpdateManager.REPOSITORY}/releases/latest",
        "Modrinth" to "https://modrinth.com/plugin/gradeway",
        "Hangar" to "https://hangar.papermc.io/GradientTim/Gradeway",
    )

    fun available(current: SemVer, latest: SemVer, clickable: Boolean): TranslatableComponent {
        val links = if (clickable) {
            Component.join(JoinConfiguration.spaces(), DOWNLOAD_PAGES.map { (name, url) -> button(name, url) })
        } else {
            Component.join(JoinConfiguration.newlines(), DOWNLOAD_PAGES.map { (name, url) -> listEntry(name, url) })
        }
        return Component.translatable(
            if (clickable) "gradeway.update.available" else "gradeway.update.available.plain",
            Argument.string("current", current.toString()),
            Argument.string("latest", latest.toString()),
            Argument.component("links", links),
        )
    }

    private fun button(name: String, url: String): Component =
        Component.text()
            .clickEvent(ClickEvent.openUrl(url))
            .append(Component.translatable("gradeway.update.available.link", linkArguments(name, url)))
            .build()

    private fun listEntry(name: String, url: String): Component =
        Component.translatable("gradeway.update.available.list", linkArguments(name, url))

    private fun linkArguments(name: String, url: String) = listOf(
        Argument.string("name", name),
        Argument.string("url", url),
    )
}
