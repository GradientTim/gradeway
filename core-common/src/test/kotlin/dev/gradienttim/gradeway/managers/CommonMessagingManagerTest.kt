/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.messaging.payloads.MessagingAction
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import dev.gradienttim.gradeway.messaging.payloads.RoleChangedPayload
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonMessagingManagerTest {
    private val gradeway: CommonGradeway = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    @Test
    fun `publish dispatches to local listeners synchronously without a connected broker`() {
        val payload = RoleChangedPayload(UUID.randomUUID().toString(), MessagingAction.UPDATED)
        val received = mutableListOf<MessagingPayload>()

        gradeway.messaging.subscribe { received.add(it) }
        val publishResult = gradeway.messaging.publish(payload)

        // No broker is connected by default (messaging.enabled = false), so the network sent
        // reports false, but the local listener must still have fired synchronously.
        assertEquals(false, publishResult)
        assertEquals(listOf<MessagingPayload>(payload), received)
    }
}
