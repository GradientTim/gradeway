/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.database.columns

import net.kyori.adventure.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdventureKeyColumnTypeTest {
    private val columnType = AdventureKeyColumnType()

    @Test
    fun `valueToDB serializes a key to its string form`() {
        val key = Key.key("gradeway", "test-key")
        assertEquals("gradeway:test-key", columnType.valueToDB(key))
    }

    @Test
    fun `valueToDB returns null for a null value`() {
        assertNull(columnType.valueToDB(null))
    }

    @Test
    fun `valueFromDB round-trips a serialized key string`() {
        val key = Key.key("gradeway", "test-key")
        val serialized = columnType.valueToDB(key)

        assertEquals(key, columnType.valueFromDB(serialized!!))
    }

    @Test
    fun `valueFromDB returns null for a non-string input`() {
        assertNull(columnType.valueFromDB(42))
    }
}
