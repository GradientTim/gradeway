/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

import arrow.core.Either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.constants.MessagingConstants
import dev.gradienttim.gradeway.utilities.Crypto

class CommonMessagingBroker(
    val gradeway: CommonGradeway<*>,
    val driverMessagingBroker: MessagingBroker
) : MessagingBroker {
    private var crypto: Crypto? = null

    override val warnNoEncryption: Boolean = driverMessagingBroker.warnNoEncryption

    override fun open(): Either<Throwable, Unit> = driverMessagingBroker.open()
    override fun close(): Either<Throwable, Unit> = driverMessagingBroker.close()

    override fun publish(channel: String, payload: ByteArray): Boolean {
        var tempPayload = payload

        crypto?.let { tempPayload = it.sign(payload) }

        return driverMessagingBroker.publish(channel, tempPayload)
    }

    override fun subscribe(channel: String, handler: (payload: ByteArray) -> Boolean): Boolean {
        var tempHandler = handler
        if (crypto != null) {
            tempHandler = handler@{ signedPayload ->
                val payload = crypto?.verify(signedPayload)
                if (payload != null) {
                    handler(payload)
                    return@handler true
                }
                return@handler false
            }
        }
        return driverMessagingBroker.subscribe(channel, tempHandler)
    }

    init {
        val messagingEncryptionEnabled = gradeway.messagingEnvironment.booleanDefault(
            names = arrayOf(MessagingConstants.ENV_ENCRYPTION_ENABLED),
            default = false
        )

        if (!messagingEncryptionEnabled && warnNoEncryption) {
            gradeway.logger.warn("Encryption is not enabled for messaging. This may pose a security risk.")
        }

        if (messagingEncryptionEnabled) {
            val messagingEncryptionKey = gradeway.messagingEnvironment.stringRequired(
                names = arrayOf(MessagingConstants.ENV_ENCRYPTION_KEY)
            )
            crypto = Crypto(messagingEncryptionKey, ALGORITHM, SIGNATURE_LENGTH_BYTES)
        }
    }

    companion object {
        const val ALGORITHM = "HmacSHA256"
        const val SIGNATURE_LENGTH_BYTES = 32
    }
}
