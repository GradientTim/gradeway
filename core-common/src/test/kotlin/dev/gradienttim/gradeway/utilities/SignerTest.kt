/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignerTest {
    @Test
    fun `signed payload verifies and returns the original bytes`() {
        val signer = Signer(key = "top-secret")

        val payload = "gradeway-payload".toByteArray()
        val verifiedPayload = signer.verify(signer.sign(payload))

        assertContentEquals(payload, verifiedPayload)
    }

    @Test
    fun `signed payload keeps the original bytes readable`() {
        val signer = Signer(key = "top-secret")

        val payload = "gradeway-payload".toByteArray()
        val signedPayload = signer.sign(payload)

        assertContentEquals(payload, signedPayload.copyOfRange(signedPayload.size - payload.size, signedPayload.size))
    }

    @Test
    fun `tampered payload fails verification`() {
        val signer = Signer(key = "top-secret")

        val signedPayload = signer.sign("gradeway-payload".toByteArray())
        signedPayload[signedPayload.lastIndex] = signedPayload[signedPayload.lastIndex].inc()

        assertNull(signer.verify(signedPayload))
    }

    @Test
    fun `payload signed with a different shared secret fails verification`() {
        val signer1 = Signer(key = "top-secret")
        val signer2 = Signer(key = "bottom-secret")

        val signedPayload = signer1.sign("gradeway-payload".toByteArray())

        assertNull(signer2.verify(signedPayload))
    }

    @Test
    fun `input shorter than the signature length fails verification without throwing`() {
        val signer = Signer(key = "top-secret")

        assertNull(signer.verify(ByteArray(4)))
        assertNull(signer.verify(ByteArray(0)))
    }

    @Test
    fun `detached signature matches the prefix of the signed payload and a known hmac`() {
        val signer = Signer(key = "key")

        val payload = "The quick brown fox jumps over the lazy dog".toByteArray()
        val signature = signer.signature(payload)

        assertContentEquals(signer.sign(payload).copyOfRange(0, signature.size), signature)
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8", signature.toHexString())
    }
}
