/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.driver.messaging

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.messaging.MessagingBroker
import org.postgresql.PGConnection
import org.postgresql.PGNotification
import org.postgresql.ds.PGSimpleDataSource
import java.sql.Connection
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class PostgresMessagingBroker(val dataSource: PGSimpleDataSource) : MessagingBroker {
    private val session = AtomicReference<ListenerSession?>()

    override val warnNoEncryption: Boolean = false
    override val warnNoSigning: Boolean = false

    override fun open(): Either<Throwable, Unit> = either {
        val connection = dataSource.connection
        val newSession = ListenerSession(connection, connection.unwrap(PGConnection::class.java))

        newSession.thread = thread(isDaemon = true, name = "gradeway-postgres-listener") {
            pollNotifications(newSession)
        }

        session.set(newSession)
    }

    override fun close(): Either<Throwable, Unit> = either {
        val activeSession = session.getAndSet(null) ?: return@either
        activeSession.close()
    }

    override fun publish(channel: String, payload: ByteArray): Boolean {
        return session.get() != null && runCatching {
            dataSource.connection.use { connection ->
                connection.prepareStatement("SELECT pg_notify(?, ?)").use { statement ->
                    statement.setString(1, channel)
                    statement.setString(2, Base64.getEncoder().encodeToString(payload))
                    statement.execute()
                }
            }
        }.isSuccess
    }

    override fun subscribe(channel: String, handler: (payload: ByteArray) -> Boolean): Boolean {
        val current = session.get() ?: return false

        current.handlers[channel] = handler
        current.pendingSubscriptions.add(channel)
        return true
    }

    private fun pollNotifications(session: ListenerSession) {
        while (session.running.get()) {
            try {
                drainPendingSubscriptions(session)

                val notifications = session.pgConnection.getNotifications(POLL_TIMEOUT_MILLIS.toInt())
                notifications?.forEach { dispatch(it, session) }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (_: Throwable) {
                if (!session.running.get()) return

                // connection likely broken, or the server went away - back off instead of hot-looping
                Thread.sleep(POLL_ERROR_BACKOFF_MILLIS)
            }
        }
    }

    private fun drainPendingSubscriptions(session: ListenerSession) {
        var channel = session.pendingSubscriptions.poll()
        while (channel != null) {
            runCatching {
                val quotedChannel = session.pgConnection.escapeIdentifier(channel)
                session.connection.createStatement().use { statement -> statement.execute("LISTEN $quotedChannel") }
            }
            channel = session.pendingSubscriptions.poll()
        }
    }

    private fun dispatch(notification: PGNotification, session: ListenerSession) {
        val handler = session.handlers[notification.name] ?: return
        val payload = runCatching { Base64.getDecoder().decode(notification.parameter) }.getOrNull() ?: return

        runCatching { handler(payload) }
    }

    private class ListenerSession(val connection: Connection, val pgConnection: PGConnection) {
        val running = AtomicBoolean(true)
        val handlers = ConcurrentHashMap<String, (payload: ByteArray) -> Boolean>()
        val pendingSubscriptions = ConcurrentLinkedQueue<String>()
        lateinit var thread: Thread

        fun close() {
            running.set(false)
            thread.interrupt()
            thread.join(POLL_TIMEOUT_MILLIS * 2)

            handlers.clear()
            pendingSubscriptions.clear()
            connection.close()
        }
    }

    private companion object {
        const val POLL_TIMEOUT_MILLIS = 1000L
        const val POLL_ERROR_BACKOFF_MILLIS = 1000L
    }
}
