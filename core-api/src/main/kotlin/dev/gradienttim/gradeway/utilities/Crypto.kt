/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides authenticated encryption and decryption of payloads using AES-GCM.
 *
 * This class encrypts data so its contents are unreadable in transit and authenticates it, so
 * tampering or use of the wrong key is detected. The shared-secret [key] string is hashed to a
 * fixed-length AES key, so any non-empty key string is accepted.
 *
 * @property key The shared secret used to derive the AES key for encryption and decryption.
 */
open class Crypto(val key: String) {
    private val secretKeySpec = SecretKeySpec(
        MessageDigest.getInstance("SHA-256").digest(key.toByteArray()),
        "AES"
    )
    private val secureRandom = SecureRandom()

    /**
     * Encrypts the given payload with a fresh random IV, producing ciphertext that is both
     * confidential and tamper-evident.
     *
     * @param payload The plaintext data to encrypt.
     * @return A byte array containing the IV followed by the AES-GCM ciphertext (which itself
     *         includes the authentication tag).
     */
    fun encrypt(payload: ByteArray): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES).also(secureRandom::nextBytes)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        return iv + cipher.doFinal(payload)
    }

    /**
     * Decrypts and authenticates a payload previously produced by [encrypt].
     *
     * @param encryptedPayload The IV-prefixed ciphertext to decrypt.
     * @return The original plaintext if the payload was encrypted with the same key and hasn't
     *         been tampered with, or null if decryption/authentication fails or the input is
     *         malformed.
     */
    fun decrypt(encryptedPayload: ByteArray): ByteArray? {
        if (encryptedPayload.size < IV_LENGTH_BYTES) {
            return null
        }

        return runCatching {
            val iv = encryptedPayload.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertext = encryptedPayload.copyOfRange(IV_LENGTH_BYTES, encryptedPayload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, GCMParameterSpec(TAG_LENGTH_BITS, iv))

            cipher.doFinal(ciphertext)
        }.getOrNull()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
    }
}
