/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Provides authenticity-only signing and verification of payloads using HMAC.
 *
 * Unlike [Crypto], this does not hide the payload's contents - it only proves the payload was
 * produced by someone holding [key] and hasn't been altered in transit. Use this on its own when
 * confidentiality isn't needed (e.g., payloads that should stay inspectable for debugging) or
 * combine it with [Crypto] for defense-in-depth on top of AES-GCM's own authentication.
 *
 * @property key The shared secret used to generate and verify signatures.
 */
open class Signer(val key: String) {
    private val secretKeySpec = SecretKeySpec(key.toByteArray(), ALGORITHM)
    private val mac = Mac.getInstance(ALGORITHM).apply { init(secretKeySpec) }

    /**
     * Signs the given payload by generating a cryptographic signature and prepending it to the
     * original payload.
     *
     * @param payload The data to be signed as a byte array.
     * @return A byte array containing the signature followed by the original payload.
     */
    fun sign(payload: ByteArray): ByteArray {
        val payloadSignature = mac.doFinal(payload)
        return payloadSignature + payload
    }

    /**
     * Verifies the integrity and authenticity of the provided signed payload.
     *
     * @param signedPayload The signed payload as a byte array, a signature followed by the
     *                       original payload.
     * @return The original payload as a byte array if the signature is valid, or null if the
     *         signature is invalid or the signed payload is malformed.
     */
    fun verify(signedPayload: ByteArray): ByteArray? {
        if (signedPayload.size < SIGNATURE_LENGTH_BYTES) {
            return null
        }

        val receivedSignature = signedPayload.copyOfRange(0, SIGNATURE_LENGTH_BYTES)
        val payload = signedPayload.copyOfRange(SIGNATURE_LENGTH_BYTES, signedPayload.size)
        val expectedSignature = mac.doFinal(payload)

        if (!MessageDigest.isEqual(receivedSignature, expectedSignature)) {
            return null
        }

        return payload
    }

    private companion object {
        const val ALGORITHM = "HmacSHA256"
        const val SIGNATURE_LENGTH_BYTES = 32
    }
}
