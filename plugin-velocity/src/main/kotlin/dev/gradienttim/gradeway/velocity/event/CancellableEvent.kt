/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.velocity.event

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.ResultedEvent.GenericResult

class CancellableEvent : Event, ResultedEvent<GenericResult> {
    private var result: GenericResult = GenericResult.allowed()

    override fun getResult(): GenericResult = this.result

    override fun setResult(result: GenericResult) {
        this.result = result
    }
}
