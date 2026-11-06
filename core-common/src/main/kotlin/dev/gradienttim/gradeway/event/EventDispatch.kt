/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.event

import arrow.core.raise.Raise
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.messaging.payloads.EventPayload
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

private val postEventSerializer = PolymorphicSerializer(PostEvent::class)

private val eventJson = Json {
    serializersModule = postEventSerializersModule
    classDiscriminator = "@type"
}

/**
 * Fires [event] and raises [cancelled] when a listener canceled it.
 */
internal fun <E> Raise<E>.fireCancellable(
    gradeway: CommonGradeway<*>,
    event: Event<Event.CancelledResult>,
    cancelled: E,
) {
    gradeway.events.fire(event)
    if (event.result == Event.CancelledResult.Cancel) {
        raise(cancelled)
    }
}

/**
 * Relays [postEvents] to the other servers and fires them locally, both on the platform scheduler so
 * the calling operation, including the cache invalidation payloads it publishes, has finished first.
 */
internal fun CommonGradeway<*>.dispatchPostEvents(postEvents: List<PostEvent>) {
    if (postEvents.isEmpty()) {
        return
    }

    schedule(postEvents.size) {
        postEvents.forEach { event ->
            relay(event)
            events.fire(event)
        }
    }
}

/**
 * Fires post events relayed by other servers with a [EventOrigin.Remote] origin.
 */
internal fun CommonGradeway<*>.handleRemoteEvent(payload: MessagingPayload) {
    if (payload !is EventPayload || payload.serverId == messaging.serverId) {
        return
    }

    val event = runCatching { eventJson.decodeFromString(postEventSerializer, payload.event) }
        .getOrElse { throwable ->
            logger.error("Failed to decode a relayed event: ${throwable.localizedMessage}")
            return
        }
    event.origin = EventOrigin.Remote(payload.serverId)

    schedule(1) { events.fire(event) }
}

private fun CommonGradeway<*>.relay(event: PostEvent) {
    runCatching { eventJson.encodeToString(postEventSerializer, event) }
        .onSuccess { encoded -> messaging.publish(EventPayload(messaging.serverId, encoded)) }
        .onFailure { logger.error("Failed to relay ${event::class.simpleName}: ${it.localizedMessage}") }
}

private fun CommonGradeway<*>.schedule(eventCount: Int, task: () -> Unit) {
    try {
        scheduler.runTask(task)
    } catch (throwable: Throwable) {
        logger.error("Failed to schedule $eventCount event(s): ${throwable.localizedMessage}")
    }
}
