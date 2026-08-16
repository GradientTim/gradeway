/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class CryptoTest {
    @Test
    fun `encrypted payload decrypts back to the original bytes`() {
        val crypto = Crypto(key = "top-secret")

        val payload = "gradeway-payload".toByteArray()
        val decryptedPayload = crypto.decrypt(crypto.encrypt(payload))

        assertContentEquals(payload, decryptedPayload)
    }

    @Test
    fun `encrypted payload does not expose the plaintext`() {
        val crypto = Crypto(key = "top-secret")

        val payload = "gradeway-payload".toByteArray()
        val encryptedPayload = crypto.encrypt(payload)

        assertNotEquals(payload.toList(), encryptedPayload.toList())
        assertNull(String(encryptedPayload).takeIf { it.contains("gradeway-payload") })
    }

    @Test
    fun `encrypting the same payload twice produces different ciphertext`() {
        val crypto = Crypto(key = "top-secret")

        val payload = "gradeway-payload".toByteArray()

        assertNotEquals(crypto.encrypt(payload).toList(), crypto.encrypt(payload).toList())
    }

    @Test
    fun `tampered payload fails decryption`() {
        val crypto = Crypto(key = "top-secret")

        val encryptedPayload = crypto.encrypt("gradeway-payload".toByteArray())
        encryptedPayload[encryptedPayload.lastIndex] = encryptedPayload[encryptedPayload.lastIndex].inc()

        assertNull(crypto.decrypt(encryptedPayload))
    }

    @Test
    fun `payload encrypted with a different shared secret fails decryption`() {
        val crypto1 = Crypto(key = "top-secret")
        val crypto2 = Crypto(key = "bottom-secret")

        val encryptedPayload = crypto1.encrypt("gradeway-payload".toByteArray())

        assertNull(crypto2.decrypt(encryptedPayload))
    }

    @Test
    fun `input shorter than the iv length fails decryption without throwing`() {
        val crypto = Crypto(key = "top-secret")

        assertNull(crypto.decrypt(ByteArray(4)))
        assertNull(crypto.decrypt(ByteArray(0)))
    }
}
