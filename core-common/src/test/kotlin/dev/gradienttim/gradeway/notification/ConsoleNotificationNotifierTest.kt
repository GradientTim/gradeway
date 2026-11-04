/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification

import dev.gradienttim.gradeway.audit.Actor
import dev.gradienttim.gradeway.audit.AuditNotification
import dev.gradienttim.gradeway.audit.Target
import dev.gradienttim.gradeway.constants.ActionConstants
import dev.gradienttim.gradeway.notification.drivers.console.ConsoleNotificationNotifier
import dev.gradienttim.gradeway.platform.Logger
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsoleNotificationNotifierTest {
    private class RecordingLogger : Logger {
        val infos = mutableListOf<String>()

        override fun info(message: String) {
            infos += message
        }

        override fun warn(message: String) = Unit

        override fun error(message: String) = Unit

        override fun panic(throwable: Throwable) = Unit
    }

    @Test
    fun `audit notifications are logged as a one-line summary`() {
        val logger = RecordingLogger()
        val playerId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val roleId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val notification = AuditNotification(
            actor = Actor.Player(playerId),
            action = ActionConstants.UPDATE,
            target = Target.role(roleId),
            before = null,
            after = null,
            timestamp = Instant.EPOCH,
        )

        ConsoleNotificationNotifier(logger).notify(notification)

        assertEquals(listOf("[notification] player ($playerId) update role ($roleId)"), logger.infos)
    }

    @Test
    fun `non-audit notifications are logged by type`() {
        val logger = RecordingLogger()
        val notification = object : Notification {
            override val type: String = "custom"
        }

        ConsoleNotificationNotifier(logger).notify(notification)

        assertEquals(listOf("[notification] custom"), logger.infos)
    }
}
