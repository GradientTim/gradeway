/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class MessagingAuthenticatorTest {
    @Test
    fun `signed payload verifies and returns the original bytes`() {
        val messagingAuthenticator = MessagingAuthenticator("shared-secret")
        val payload = "gradeway-payload".toByteArray()

        val verifiedPayload = messagingAuthenticator.verify(messagingAuthenticator.sign(payload))

        assertContentEquals(payload, verifiedPayload)
    }

    @Test
    fun `tampered payload fails verification`() {
        val messagingAuthenticator = MessagingAuthenticator("shared-secret")
        val signedPayload = messagingAuthenticator.sign("gradeway-payload".toByteArray())
        signedPayload[signedPayload.lastIndex] = signedPayload[signedPayload.lastIndex].inc()

        assertNull(messagingAuthenticator.verify(signedPayload))
    }

    @Test
    fun `payload signed with a different shared secret fails verification`() {
        val signingAuthenticator = MessagingAuthenticator("shared-secret")
        val verifyingAuthenticator = MessagingAuthenticator("different-secret")
        val signedPayload = signingAuthenticator.sign("gradeway-payload".toByteArray())

        assertNull(verifyingAuthenticator.verify(signedPayload))
    }

    @Test
    fun `input shorter than the signature length fails verification without throwing`() {
        val messagingAuthenticator = MessagingAuthenticator("shared-secret")

        assertNull(messagingAuthenticator.verify(ByteArray(4)))
        assertNull(messagingAuthenticator.verify(ByteArray(0)))
    }
}
