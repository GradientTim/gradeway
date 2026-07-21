/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.messaging

import dev.gradienttim.gradeway.messaging.payloads.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class MessagingPayloadSerializationTest {
    @Test
    fun `role permission changed payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = RolePermissionChangedPayload(UUID.randomUUID().toString(), "gradeway.test")

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `role parent changed payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = RoleParentChangedPayload(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            MessagingAction.CREATED,
        )

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `player attribute changed payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = PlayerAttributeChangedPayload(
            UUID.randomUUID().toString(),
            "gradeway:test-key",
            MessagingAction.DELETED,
        )

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `role permissions cleared payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = RolePermissionsClearedPayload(UUID.randomUUID().toString())

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `group permissions cleared payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = GroupPermissionsClearedPayload(UUID.randomUUID().toString())

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `player permissions cleared payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = PlayerPermissionsClearedPayload(UUID.randomUUID().toString())

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `role attributes cleared payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = RoleAttributesClearedPayload(UUID.randomUUID().toString())

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `player attributes cleared payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = PlayerAttributesClearedPayload(UUID.randomUUID().toString())

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `permission template permissions cleared payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = PermissionTemplatePermissionsClearedPayload(UUID.randomUUID().toString())

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `cache flush payload round trips through ProtoBuf`() {
        val payload: MessagingPayload = CacheFlushPayload

        val decoded = ProtoBuf.decodeFromByteArray<MessagingPayload>(ProtoBuf.encodeToByteArray(payload))

        assertEquals(payload, decoded)
    }

    @Test
    fun `network payload wrapping round trips through ProtoBuf and preserves the inner payload`() {
        val payload: MessagingPayload = RoleChangedPayload(UUID.randomUUID().toString(), MessagingAction.UPDATED)
        val encodedPayload = ProtoBuf.encodeToByteArray(payload)
        val networkPayload = NetworkPayload(UUID.randomUUID().toString(), encodedPayload)

        val decodedNetworkPayload =
            ProtoBuf.decodeFromByteArray<NetworkPayload>(ProtoBuf.encodeToByteArray(networkPayload))
        val decodedPayload = ProtoBuf.decodeFromByteArray<MessagingPayload>(decodedNetworkPayload.payload)

        assertEquals(networkPayload.serverId, decodedNetworkPayload.serverId)
        assertEquals(payload, decodedPayload)
    }
}
