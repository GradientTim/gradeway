/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.bukkit.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.gradienttim.gradeway.command.context.CommandContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.translation.Argument
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class BukkitBrigadierCommand(
    val dispatcher: CommandDispatcher<CommandSender>,
    val context: CommandContext<CommandSender>,
    builder: LiteralArgumentBuilder<CommandSender>,
) : CommandExecutor, TabCompleter {
    private val name = builder.literal

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
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

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        val input = (listOf(name) + args).joinToString(" ")

        val parseResult = dispatcher.parse(input, sender)
        val suggestions = dispatcher.getCompletionSuggestions(parseResult).resultNow()

        return suggestions.list.map { it.text }
    }

    init {
        dispatcher.register(builder)
    }
}
