/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Provides functionality for signing and verifying payloads using cryptographic algorithms.
 *
 * This class allows you to securely sign data and later verify it to ensure its integrity
 * and authenticity. It uses a symmetric key and a specified cryptographic algorithm for
 * signing and verification.
 *
 * @property key The secret key used for generating and verifying signatures.
 * @property algorithm The cryptographic algorithm used for signing, such as HmacSHA256.
 * @property signatureLengthBytes The length of the cryptographic signature in bytes.
 */
open class Crypto(
    val key: String,
    val algorithm: String,
    val signatureLengthBytes: Int
) {
    private val secretKeySpec = SecretKeySpec(key.toByteArray(), algorithm)
    private val mac = Mac.getInstance(algorithm).apply { init(secretKeySpec) }

    /**
     * Signs the given payload by generating a cryptographic signature and appending it
     * to the original payload.
     *
     * The method uses the specified cryptographic algorithm and secret key to compute
     * the signature, ensuring the integrity and authenticity of the data.
     *
     * @param payload The data to be signed as a byte array. This should contain the payload
     *                that needs to be secured.
     * @return A byte array representing the signed payload, which includes the cryptographic
     *         signature followed by the original payload.
     */
    fun sign(payload: ByteArray): ByteArray {
        val payloadSignature = mac.doFinal(payload)
        return payloadSignature + payload
    }

    /**
     * Verifies the integrity and authenticity of the provided signed payload.
     *
     * This method checks if the signed payload contains a valid cryptographic signature
     * generated using the specified algorithm and secret key. If the signature is valid,
     * the method extracts and returns the original payload. Otherwise, it returns null.
     *
     * @param signedPayload The signed payload as a byte array. This should include a cryptographic
     *                      signature followed by the original payload.
     * @return The original payload as a byte array if the signature is valid, or null if the signature
     *         is invalid or the signed payload is malformed.
     */
    fun verify(signedPayload: ByteArray): ByteArray? {
        if (signedPayload.size < signatureLengthBytes) {
            return null
        }

        val receivedSignature = signedPayload.copyOfRange(0, signatureLengthBytes)
        val payload = signedPayload.copyOfRange(signatureLengthBytes, signedPayload.size)
        val expectedSignature = mac.doFinal(payload)

        if (!MessageDigest.isEqual(receivedSignature, expectedSignature)) {
            return null
        }

        return payload
    }
}
