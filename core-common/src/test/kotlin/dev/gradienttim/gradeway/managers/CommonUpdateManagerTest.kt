/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.left
import arrow.core.right
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.update.GitHubReleaseSource
import dev.gradienttim.gradeway.update.ReleaseSource
import dev.gradienttim.gradeway.utilities.SemVer
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommonUpdateManagerTest {
    private fun sourceOf(version: String) = ReleaseSource {
        CompletableFuture.completedFuture(UpdateManager.Release(SemVer.parse(version)).right())
    }

    private fun withManager(current: String, source: ReleaseSource, block: (CommonUpdateManager) -> Unit) {
        val gradeway = createTestGradeway()
        try {
            block(CommonUpdateManager(gradeway, SemVer.parse(current), source))
        } finally {
            gradeway.disposeTestGradeway()
        }
    }

    @Test
    fun `check reports an update when the latest release is newer`() = withManager("0.4.1", sourceOf("v0.5.0")) {
        it.check().join()
        assertEquals(SemVer(0, 5, 0), it.latestRelease?.version)
        assertTrue(it.isUpdateAvailable)
    }

    @Test
    fun `check reports no update when running the latest or a newer version`() {
        withManager("0.4.1", sourceOf("0.4.1")) {
            it.check().join()
            assertFalse(it.isUpdateAvailable)
        }
        withManager("0.5.0-rc.1", sourceOf("0.4.1")) {
            it.check().join()
            assertFalse(it.isUpdateAvailable)
        }
    }

    @Test
    fun `failed check keeps the latest release unset`() {
        val failing = ReleaseSource { CompletableFuture.completedFuture(IllegalStateException("offline").left()) }
        withManager("0.4.1", failing) {
            assertTrue(it.check().join().isLeft())
            assertNull(it.latestRelease)
            assertFalse(it.isUpdateAvailable)
        }
    }

    @Test
    fun `parseRelease reads the tag from the GitHub response`() {
        val body = """{"tag_name":"v1.2.3","html_url":"https://github.com/x/y/releases/tag/v1.2.3","draft":false}"""
        assertEquals(UpdateManager.Release(SemVer(1, 2, 3)), GitHubReleaseSource.parseRelease(body).getOrNull())
    }

    @Test
    fun `parseRelease fails on malformed responses`() {
        assertTrue(GitHubReleaseSource.parseRelease("""{"message":"Not Found"}""").isLeft())
        assertTrue(GitHubReleaseSource.parseRelease("""{"tag_name":"latest"}""").isLeft())
    }
}
