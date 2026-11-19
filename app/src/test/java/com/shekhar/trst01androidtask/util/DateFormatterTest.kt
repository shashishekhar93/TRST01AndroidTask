package com.shekhar.trst01androidtask.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class DateFormatterTest {

    private val utcZone = ZoneId.of("UTC")

    @Test
    fun validIsoLocalDateTime_formatsCorrectlyWithoutShift() {
        val result = DateFormatter.formatDate("2026-11-10T16:09:00")
        assertEquals("Tue, 10 Nov 2026, 04:09 PM", result)
    }

    @Test
    fun amTime_formatsCorrectly() {
        val result = DateFormatter.formatDate("2026-11-10T08:05:00")
        assertEquals("Tue, 10 Nov 2026, 08:05 AM", result)
    }

    @Test
    fun pmTime_formatsCorrectly() {
        val result = DateFormatter.formatDate("2026-11-10T12:00:00")
        assertEquals("Tue, 10 Nov 2026, 12:00 PM", result)
    }

    @Test
    fun midnightTime_formatsCorrectly() {
        val result = DateFormatter.formatDate("2026-11-10T00:00:00")
        assertEquals("Tue, 10 Nov 2026, 12:00 AM", result)
    }

    @Test
    fun fractionalSeconds_formatsCorrectly() {
        val result = DateFormatter.formatDate("2026-11-10T16:09:00.123456")
        assertEquals("Tue, 10 Nov 2026, 04:09 PM", result)
    }

    @Test
    fun explicitUtcTimestamp_convertsToTargetZone() {
        val result = DateFormatter.formatDate("2026-11-10T16:09:00Z", zoneId = utcZone)
        assertEquals("Tue, 10 Nov 2026, 04:09 PM", result)
    }

    @Test
    fun timestampWithTimezoneOffset_convertsToTargetZone() {
        // 2026-11-10T21:39:00+05:30 in UTC is 2026-11-10T16:09:00Z
        val result = DateFormatter.formatDate("2026-11-10T21:39:00+05:30", zoneId = utcZone)
        assertEquals("Tue, 10 Nov 2026, 04:09 PM", result)
    }

    @Test
    fun nullValue_returnsNA() {
        val result = DateFormatter.formatDate(null)
        assertEquals("N/A", result)
    }

    @Test
    fun blankValue_returnsNA() {
        val result = DateFormatter.formatDate("   ")
        assertEquals("N/A", result)
    }

    @Test
    fun invalidDateString_returnsNA() {
        val result = DateFormatter.formatDate("invalid-date-string")
        assertEquals("N/A", result)
    }
}
