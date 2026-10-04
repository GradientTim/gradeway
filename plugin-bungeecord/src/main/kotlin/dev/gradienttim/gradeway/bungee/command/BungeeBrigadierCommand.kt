/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bungee.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.gradienttim.gradeway.bungee.GradewayPlugin
import dev.gradienttim.gradeway.command.context.CommandContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor

class BungeeBrigadierCommand(
    val plugin: GradewayPlugin,
    val dispatcher: CommandDispatcher<CommandSender>,
    val context: CommandContext<CommandSender>,
    val commandAliases: Array<String>? = null,
    builder: LiteralArgumentBuilder<CommandSender>,
) : Command(builder.literal), TabExecutor {
    override fun execute(sender: CommandSender, args: Array<String>) {
        val input = (listOf(name) + args).joinToString(" ")

        try {
            dispatcher.execute(input, sender)
        } catch (exception: CommandSyntaxException) {
            context.sendTranslatedMessage(
                sender,
                Component.translatable(
                    "gradeway.command.syntax.error",
                    Argument.string("error", exception.message ?: exception.javaClass.simpleName)
                )
            )
        }
    }

    override fun getAliases(): Array<out String?>? = commandAliases

    override fun onTabComplete(sender: CommandSender, args: Array<out String>): Iterable<String> {
        val input = (listOf(name) + args).joinToString(" ")

        val parseResult = dispatcher.parse(input, sender)
        val suggestions = dispatcher.getCompletionSuggestions(parseResult).resultNow()

        return suggestions.list.map { it.text }
    }

    init {
        dispatcher.register(builder)
        plugin.proxy.pluginManager.registerCommand(plugin, this)
    }
}
