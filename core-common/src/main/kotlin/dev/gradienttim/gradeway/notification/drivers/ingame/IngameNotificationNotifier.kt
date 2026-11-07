/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification.drivers.ingame

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.AuditFieldChange
import dev.gradienttim.gradeway.audit.AuditNotification
import dev.gradienttim.gradeway.commands.extensions.infoList
import dev.gradienttim.gradeway.extensions.formatUTC
import dev.gradienttim.gradeway.extensions.toIdArgument
import dev.gradienttim.gradeway.notification.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.minimessage.translation.Argument

/**
 * Sends audit notifications as chat messages to every online player who has the [PERMISSION]
 * permission and has turned notifications on (see
 * [dev.gradienttim.gradeway.managers.NotificationManager.isReceiving]).
 *
 * This includes the player who caused the change. Notifications that are not audit notifications carry nothing to show and are ignored.
 *
 * @param TPlayer The platform's online player type.
 */
class IngameNotificationNotifier<TPlayer>(
    private val gradeway: CommonGradeway<*>,
    private val platform: IngameNotificationPlatform<TPlayer>,
) : NotificationNotifier {
    override fun notify(notification: Notification) {
        if (notification !is AuditNotification) return

        // Notifications can be dispatched from any thread, but platform player APIs are not
        // guaranteed to be thread-safe, so delivery runs on the platform scheduler.
        gradeway.scheduler.runTask {
            runCatching { deliver(notification) }.onFailure { throwable ->
                gradeway.logger.error("Failed to deliver in-game notification: ${throwable.localizedMessage}")
            }
        }
    }

    private fun deliver(notification: AuditNotification) {
        val message = createMessage(notification)

        for (player in platform.onlinePlayers()) {
            val shouldReceive = platform.hasPermission(player, PERMISSION) &&
                gradeway.notifications.isReceiving(platform.uniqueId(player))

            if (shouldReceive) {
                platform.sendTranslatedMessage(player, message)
            }
        }
    }

    private fun createMessage(notification: AuditNotification): TranslatableComponent =
        Component.translatable(
            "gradeway.notification.ingame.audit",
            Argument.string("actor", actorName(notification)),
            Argument.string("actor_id", notification.actorLabel),
            Argument.string("action", notification.action),
            Argument.string("target", notification.target.type),
            Argument.string("target_ids", notification.targetLabel),
            Argument.string("time", notification.timestamp.formatUTC()),
            Argument.component("changes", changeList(notification.changes)),
            notification.target.id.toIdArgument(),
        )

    // The full before/after snapshots are too large for a hover, so only the changed fields are
    // shown, limited in count and length.
    private fun changeList(changes: List<AuditFieldChange>): Component {
        val lines = changes.take(MAX_CHANGES).map { change -> changeLine(change) }.toMutableList()
        if (changes.size > MAX_CHANGES) {
            lines += Component.translatable(
                "gradeway.notification.ingame.audit.more",
                Argument.numeric("count", changes.size - MAX_CHANGES),
            )
        }
        return infoList(lines)
    }

    private fun changeLine(change: AuditFieldChange): Component {
        val (fieldName, before, after) = change
        val field = Argument.string("field", fieldName)
        return when {
            before == null -> Component.translatable(
                "gradeway.notification.ingame.audit.added",
                field,
                Argument.string("value", after.orEmpty().truncate()),
            )

            after == null -> Component.translatable(
                "gradeway.notification.ingame.audit.removed",
                field,
                Argument.string("value", before.truncate()),
            )

            else -> Component.translatable(
                "gradeway.notification.ingame.audit.changed",
                field,
                Argument.string("before", before.truncate()),
                Argument.string("after", after.truncate()),
            )
        }
    }

    private fun String.truncate(): String =
        if (length <= MAX_VALUE_LENGTH) this else take(MAX_VALUE_LENGTH - 1) + "\u2026"

    private fun actorName(notification: AuditNotification): String = when (val actor = notification.actor) {
        is Actor.Player -> platform.playerName(actor.uniqueId) ?: actor.uniqueId.toString()
        else -> notification.actorLabel
    }

    companion object {
        private const val MAX_CHANGES = 8
        private const val MAX_VALUE_LENGTH = 48

        const val PERMISSION: String = "gradeway.notify"
    }
}
