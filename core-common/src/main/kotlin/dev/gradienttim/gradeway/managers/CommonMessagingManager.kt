/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import arrow.core.Either
import arrow.core.raise.either
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.driver.adapters.MessagingAdapter
import dev.gradienttim.gradeway.driver.meta.DriverType

class CommonMessagingManager(val gradeway: CommonGradeway) : MessagingManager {
    override fun load(): Either<Throwable, Unit> = either {
        val config = gradeway.configs.config.messaging
        if (!config.enabled) {
            return@either
        }

        val driverId = gradeway.configs.config.messaging.driver
        if (driverId.isBlank()) {
            raise(Throwable("Driver identifier cannot be blank."))
        }

        val messagingDriver = gradeway.drivers.findDriver(driverId, DriverType.MESSAGING)
            ?: raise(Throwable("No database driver found with id '$driverId'"))

        if (messagingDriver !is MessagingAdapter) {
            raise(Throwable("Driver '$driverId' has no MessagingAdapter."))
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
