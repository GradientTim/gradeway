/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Authenticates the payloads every [MessagingBroker] sends and receives, so that a message
 * dispatched to a subscriber is always known to have come from another broker configured with
 * the same shared secret rather than an untrusted party able to reach the underlying transport.
 * [MessagingBroker] applies this uniformly and unconditionally to every implementation - a
 * shared secret is always required to construct one, and messages are never dispatched to a
 * subscriber unauthenticated, regardless of how trusted the transport is otherwise assumed to be.
 *
 * All brokers speaking the same channel must be constructed with the same [sharedSecret], since
 * a payload signed by one broker is only accepted by another that was given the same secret.
 *
 * @property sharedSecret The secret used to key the HMAC. Must be identical across every server
 * and proxy participating in the same messaging channel.
 */
class MessagingAuthenticator(sharedSecret: String) {
    private val secretKey = SecretKeySpec(sharedSecret.toByteArray(Charsets.UTF_8), ALGORITHM)

    /**
     * Signs [payload] with an HMAC computed over its bytes using the configured shared secret.
     *
     * @param payload The raw payload bytes to sign, as written to the wire.
     * @return The computed signature followed by the original [payload] bytes, ready to be sent
     * over the wire as a single message.
     */
    fun sign(payload: ByteArray): ByteArray {
        val payloadSignature = computeSignature(payload)
        return payloadSignature + payload
    }

    /**
     * Verifies a message previously produced by [sign], recomputing its signature with the
     * configured shared secret and comparing it against the received signature in constant time.
     *
     * @param signedPayload The full message as received from the wire, i.e. a signature followed
     * by the original payload bytes.
     * @return The original payload bytes if [signedPayload] is long enough to contain a signature
     * and that signature is valid, or `null` if it is too short, was signed with a different
     * shared secret, or was tampered with in transit.
     */
    fun verify(signedPayload: ByteArray): ByteArray? {
        if (signedPayload.size < SIGNATURE_LENGTH_BYTES) {
            return null
        }

        val receivedSignature = signedPayload.copyOfRange(0, SIGNATURE_LENGTH_BYTES)
        val payload = signedPayload.copyOfRange(SIGNATURE_LENGTH_BYTES, signedPayload.size)
        val expectedSignature = computeSignature(payload)

        if (!MessageDigest.isEqual(receivedSignature, expectedSignature)) {
            return null
        }
        return payload
    }

    private fun computeSignature(payload: ByteArray): ByteArray =
        Mac.getInstance(ALGORITHM).apply { init(secretKey) }.doFinal(payload)

    companion object {
        private const val ALGORITHM = "HmacSHA256"
        private const val SIGNATURE_LENGTH_BYTES = 32

        /**
         * Name of the environment/config variable every [dev.gradienttim.gradeway.driver.adapters.
         * MessagingAdapter] must read a [sharedSecret] from when constructing the
         * [MessagingAuthenticator] passed to its [MessagingBroker].
         */
        const val SHARED_SECRET_VARIABLE: String = "GRADEWAY_MESSAGING_SECRET"
    }
}
