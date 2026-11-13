/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.update

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.gradienttim.gradeway.managers.UpdateManager
import dev.gradienttim.gradeway.utilities.SemVer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * Fetches the latest non-draft, non-pre-release from a GitHub repository's releases.
 *
 * @param repository The repository in `owner/name` form.
 * @param userAgent The User-Agent header GitHub requires on API requests.
 */
class GitHubReleaseSource(
    repository: String,
    private val userAgent: String,
) : ReleaseSource {
    private val uri = URI.create("https://api.github.com/repos/$repository/releases/latest")
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun fetchLatest(): CompletableFuture<Either<Throwable, UpdateManager.Release>> {
        val request = HttpRequest.newBuilder(uri)
            .timeout(TIMEOUT)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", userAgent)
            .GET()
            .build()

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).handle { response, throwable ->
            either {
                if (throwable != null) {
                    raise((throwable as? CompletionException)?.cause ?: throwable)
                }
                ensure(response.statusCode() in SUCCESS_STATUS_CODES) {
                    IllegalStateException("GitHub responded with status ${response.statusCode()}")
                }
                parseRelease(response.body()).bind()
            }
        }
    }

    companion object {
        private val TIMEOUT: Duration = Duration.ofSeconds(10)
        private val SUCCESS_STATUS_CODES = 200..299
        private val JSON = Json { ignoreUnknownKeys = true }

        fun parseRelease(body: String): Either<Throwable, UpdateManager.Release> = Either.catch {
            val release = JSON.parseToJsonElement(body).jsonObject
            UpdateManager.Release(SemVer.parse(release.getValue("tag_name").jsonPrimitive.content))
        }
    }
}
