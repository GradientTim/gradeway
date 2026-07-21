/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.services

import arrow.core.getOrElse
import dev.gradienttim.gradeway.CommonGradeway
import dev.gradienttim.gradeway.attribute.Attribute
import dev.gradienttim.gradeway.createTestGradeway
import dev.gradienttim.gradeway.disposeTestGradeway
import dev.gradienttim.gradeway.messaging.payloads.MessagingPayload
import dev.gradienttim.gradeway.messaging.payloads.PlayerAttributeChangedPayload
import dev.gradienttim.gradeway.messaging.payloads.PlayerAttributesClearedPayload
import net.kyori.adventure.key.Key
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommonAttributeServiceTest {
    private val gradeway: CommonGradeway = createTestGradeway()

    @AfterTest
    fun tearDown() {
        gradeway.disposeTestGradeway()
    }

    private fun uniquePlayer() =
        gradeway.players.create(UUID.randomUUID(), "player-${UUID.randomUUID().toString().take(8)}")
            .getOrElse { error(it.toString()) }

    @Test
    fun `clearing a player's attributes publishes a single general cleared payload`() {
        val player = gradeway.players.create(UUID.randomUUID(), "player-${UUID.randomUUID().toString().take(8)}")
            .getOrElse { error(it.toString()) }
        gradeway.attributes.addPlayerAttribute(player, Attribute.string(Key.key("gradeway:test-one"), "a"))
            .getOrElse { error(it.toString()) }
        gradeway.attributes.addPlayerAttribute(player, Attribute.string(Key.key("gradeway:test-two"), "b"))
            .getOrElse { error(it.toString()) }

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.attributes.clearPlayerAttributes(player).getOrElse { error(it.toString()) }

        assertEquals(
            listOf<MessagingPayload>(PlayerAttributesClearedPayload(player.id.value.toString())),
            received,
            "expected exactly one general cleared payload instead of one per removed key",
        )
    }

    @Test
    fun `removing a single player attribute still publishes a per-key payload`() {
        val player = gradeway.players.create(UUID.randomUUID(), "player-${UUID.randomUUID().toString().take(8)}")
            .getOrElse { error(it.toString()) }
        val key = Key.key("gradeway:test-single")
        gradeway.attributes.addPlayerAttribute(player, Attribute.string(key, "a")).getOrElse { error(it.toString()) }

        val received = mutableListOf<MessagingPayload>()
        gradeway.messaging.subscribe { received.add(it) }

        gradeway.attributes.removePlayerAttribute(player, key).getOrElse { error(it.toString()) }

        assertEquals(1, received.size)
        val payload = received.single()
        check(payload is PlayerAttributeChangedPayload)
        assertEquals(key.asString(), payload.key)
    }

    @Test
    fun `addPlayerAttribute rejects a duplicate key`() {
        val player = uniquePlayer()
        val key = Key.key("gradeway:test-duplicate")
        gradeway.attributes.addPlayerAttribute(player, Attribute.string(key, "a")).getOrElse { error(it.toString()) }

        val result = gradeway.attributes.addPlayerAttribute(player, Attribute.string(key, "b"))

        assertEquals(AttributeService.AddAttributeError.AttributeAlreadyExists, result.leftOrNull())
    }

    @Test
    fun `updateAttribute round-trips the serialized value`() {
        val player = uniquePlayer()
        val key = Key.key("gradeway:test-update")
        gradeway.attributes.addPlayerAttribute(player, Attribute.string(key, "before"))
            .getOrElse { error(it.toString()) }

        gradeway.attributes.updatePlayerAttribute(player, key, "after").getOrElse { error(it.toString()) }

        assertEquals("after", gradeway.attributes.getPlayerAttribute(player, key)?.value)
    }

    @Test
    fun `hasAttribute and getAttribute reflect the current state`() {
        val player = uniquePlayer()
        val key = Key.key("gradeway:test-has")

        assertFalse(gradeway.attributes.hasPlayerAttribute(player, key))
        assertNull(gradeway.attributes.getPlayerAttribute(player, key))

        gradeway.attributes.addPlayerAttribute(player, Attribute.string(key, "value"))
            .getOrElse { error(it.toString()) }

        assertTrue(gradeway.attributes.hasPlayerAttribute(player, key))
        assertEquals("value", gradeway.attributes.getPlayerAttribute(player, key)?.value)
    }

    @Test
    fun `removeAttribute fails for a key that was never set`() {
        val player = uniquePlayer()

        val result = gradeway.attributes.removePlayerAttribute(player, Key.key("gradeway:test-missing"))

        assertEquals(AttributeService.RemoveAttributeError.AttributeNotExists, result.leftOrNull())
    }
}
