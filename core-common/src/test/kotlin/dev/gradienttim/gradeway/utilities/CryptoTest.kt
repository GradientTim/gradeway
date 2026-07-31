/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import dev.gradienttim.gradeway.messaging.CommonMessagingBroker
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class CryptoTest {
    @Test
    fun `signed payload verifies and returns the original bytes`() {
        val crypto = Crypto(
            key = "top-secret",
            algorithm = CommonMessagingBroker.ALGORITHM,
            signatureLengthBytes = CommonMessagingBroker.SIGNATURE_LENGTH_BYTES
        )

        val payload = "gradeway-payload".toByteArray()
        val verifiedPayload = crypto.verify(crypto.sign(payload))

        assertContentEquals(payload, verifiedPayload)
    }

    @Test
    fun `tampered payload fails verification`() {
        val crypto = Crypto(
            key = "top-secret",
            algorithm = CommonMessagingBroker.ALGORITHM,
            signatureLengthBytes = CommonMessagingBroker.SIGNATURE_LENGTH_BYTES
        )

        val signedPayload = crypto.sign("gradeway-payload".toByteArray())
        signedPayload[signedPayload.lastIndex] = signedPayload[signedPayload.lastIndex].inc()

        assertNull(crypto.verify(signedPayload))
    }

    @Test
    fun `payload signed with a different shared secret fails verification`() {
        val crypto1 = Crypto(
            key = "top-secret",
            algorithm = CommonMessagingBroker.ALGORITHM,
            signatureLengthBytes = CommonMessagingBroker.SIGNATURE_LENGTH_BYTES
        )

        val crypto2 = Crypto(
            key = "bottom-secret",
            algorithm = CommonMessagingBroker.ALGORITHM,
            signatureLengthBytes = CommonMessagingBroker.SIGNATURE_LENGTH_BYTES
        )

        val signedPayload = crypto1.sign("gradeway-payload".toByteArray())

        assertNull(crypto2.verify(signedPayload))
    }

    @Test
    fun `input shorter than the signature length fails verification without throwing`() {
        val crypto = Crypto(
            key = "top-secret",
            algorithm = CommonMessagingBroker.ALGORITHM,
            signatureLengthBytes = CommonMessagingBroker.SIGNATURE_LENGTH_BYTES
        )

        assertNull(crypto.verify(ByteArray(4)))
        assertNull(crypto.verify(ByteArray(0)))
    }
}
