package com.shekhar.trst01androidtask.data.local.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateConverterTest {

    private val converter = CoordinateConverter()

    @Test
    fun serialization_and_deserialization_preservesOrder() {
        val coordinates = listOf(
            LatLngPoint(12.9716, 77.5946),
            LatLngPoint(13.0827, 80.2707),
            LatLngPoint(11.0168, 76.9558)
        )

        val json = converter.fromCoordinateList(coordinates)
        val deserialized = converter.toCoordinateList(json)

        assertEquals(coordinates.size, deserialized.size)
        assertEquals(coordinates[0], deserialized[0])
        assertEquals(coordinates[1], deserialized[1])
        assertEquals(coordinates[2], deserialized[2])
    }

    @Test
    fun emptyCoordinateList_serializesAndDeserializesCorrectly() {
        val coordinates = emptyList<LatLngPoint>()

        val json = converter.fromCoordinateList(coordinates)
        val deserialized = converter.toCoordinateList(json)

        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun nullCoordinateList_serializesToNull() {
        val json = converter.fromCoordinateList(null)
        assertNull(json)
    }

    @Test
    fun nullOrBlankString_deserializesToEmptyList() {
        assertTrue(converter.toCoordinateList(null).isEmpty())
        assertTrue(converter.toCoordinateList("").isEmpty())
        assertTrue(converter.toCoordinateList("   ").isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidJson_throwsIllegalArgumentException() {
        converter.toCoordinateList("invalid_json_string")
    }
}
