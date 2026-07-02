/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.driver.meta.DriverType
import dev.gradienttim.gradeway.throwables.driver.DriverBlankIdentifierThrowable
import dev.gradienttim.gradeway.throwables.driver.DriverNotFoundThrowable
import dev.gradienttim.gradeway.throwables.driver.DriverUnsupportedAdapterThrowable

class CommonMessagingManager(val gradeway: CommonGradeway) : MessagingManager {
    override fun load(): Either<Throwable, Unit> = either {
        val config = gradeway.configs.config.messaging
        if (!config.enabled) {
            return@either
        }

        val driverId = config.driver
        if (driverId.isBlank()) {
            raise(DriverBlankIdentifierThrowable())
        }

        val messagingDriver = gradeway.drivers.findDriver(driverId, DriverType.MESSAGING)
            ?: raise(DriverNotFoundThrowable(id = driverId))

        if (messagingDriver !is MessagingAdapter) {
            raise(DriverUnsupportedAdapterThrowable(id = driverId, adapter = MessagingAdapter::class))
        }

        try {
            gradeway.broker = messagingDriver.createMessagingBroker(gradeway.environment)
            gradeway.broker.open()
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun unload(): Either<Throwable, Unit> = either {
        try {
            gradeway.broker.close()
        } catch (throwable: Throwable) {
            raise(throwable)
        }
    }

    override fun reload(): Either<Throwable, Unit> = either {
        unload()
            .onLeft { raise(it) }
            .onRight {
                load().onLeft { raise(it) }
            }
    }
}
