/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.notification

import dev.gradienttim.gradeway.platform.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Sends JSON notification payloads over HTTP without blocking the calling thread.
 *
 * Requests are sent asynchronously, failures and non-2xx responses are only logged, so a slow or
 * unreachable endpoint never delays the operation that produced the notification.
 *
 * @param logger The logger that delivery failures are reported to.
 * @param name A human-readable name of the notification service, used in log messages.
 */
internal class NotificationHttpClient(
    private val logger: Logger,
    private val name: String,
) {
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .build()

    fun postJson(url: String, body: String, headers: Map<String, String> = emptyMap()) {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .apply { headers.forEach { (key, value) -> header(key, value) } }
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete { response, throwable ->
            when {
                throwable != null -> logger.error("Failed to send $name notification: ${throwable.localizedMessage}")
                response.statusCode() !in SUCCESS_STATUS_CODES -> logger.error(
                    "Failed to send $name notification: Received status ${response.statusCode()} " +
                        "(${response.body().take(MAX_LOGGED_BODY_LENGTH)})",
                )
            }
        }
    }

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(10)
        val SUCCESS_STATUS_CODES = 200..299
        const val MAX_LOGGED_BODY_LENGTH = 200
    }
}
