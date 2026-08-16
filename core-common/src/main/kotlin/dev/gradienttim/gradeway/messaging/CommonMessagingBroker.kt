/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

import arrow.core.Either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.utilities.Crypto
import dev.gradienttim.gradeway.utilities.Signer

class CommonMessagingBroker(
    val gradeway: CommonGradeway<*>,
    val driverMessagingBroker: MessagingBroker
) : MessagingBroker {
    private var crypto: Crypto? = null
    private var signer: Signer? = null

    override val warnNoEncryption: Boolean = driverMessagingBroker.warnNoEncryption
    override val warnNoSigning: Boolean = driverMessagingBroker.warnNoSigning

    override fun open(): Either<Throwable, Unit> = driverMessagingBroker.open()
    override fun close(): Either<Throwable, Unit> = driverMessagingBroker.close()

    override fun publish(channel: String, payload: ByteArray): Boolean {
        var tempPayload = payload

        crypto?.let { tempPayload = it.encrypt(tempPayload) }
        signer?.let { tempPayload = it.sign(tempPayload) }

        return driverMessagingBroker.publish(channel, tempPayload)
    }

    override fun subscribe(channel: String, handler: (payload: ByteArray) -> Boolean): Boolean {
        val currentSigner = signer
        val currentCrypto = crypto

        if (currentSigner == null && currentCrypto == null) {
            return driverMessagingBroker.subscribe(channel, handler)
        }

        val wrappedHandler: (payload: ByteArray) -> Boolean = wrapped@{ incomingPayload ->
            var tempPayload: ByteArray? = incomingPayload

            if (currentSigner != null) {
                tempPayload = tempPayload?.let { currentSigner.verify(it) }
            }
            if (currentCrypto != null) {
                tempPayload = tempPayload?.let { currentCrypto.decrypt(it) }
            }

            val payload = tempPayload ?: return@wrapped false
            handler(payload)
        }

        return driverMessagingBroker.subscribe(channel, wrappedHandler)
    }

    init {
        val messagingEncryptionEnabled = gradeway.messagingEnvironment.booleanDefault(
            names = arrayOf(MessagingConstants.ENV_ENCRYPTION_ENABLED),
            default = false
        )
        val messagingSigningEnabled = gradeway.messagingEnvironment.booleanDefault(
            names = arrayOf(MessagingConstants.ENV_SIGNING_ENABLED),
            default = false
        )

        if (!messagingEncryptionEnabled && warnNoEncryption) {
            gradeway.logger.warn("Encryption is not enabled for messaging. This may pose a security risk.")
        }

        // Encryption already authenticates its payloads (AES-GCM), so signing being off isn't a
        // gap in authenticity coverage by itself - only warn when neither is enabled.
        if (!messagingSigningEnabled && !messagingEncryptionEnabled && warnNoSigning) {
            gradeway.logger.warn("Signing is not enabled for messaging. This may pose a security risk.")
        }

        if (messagingEncryptionEnabled) {
            val messagingEncryptionKey = gradeway.messagingEnvironment.stringRequired(
                names = arrayOf(MessagingConstants.ENV_ENCRYPTION_KEY)
            )
            crypto = Crypto(messagingEncryptionKey)
        }

        if (messagingSigningEnabled) {
            val messagingSigningKey = gradeway.messagingEnvironment.stringRequired(
                names = arrayOf(MessagingConstants.ENV_SIGNING_KEY)
            )
            signer = Signer(messagingSigningKey)
        }
    }
}
