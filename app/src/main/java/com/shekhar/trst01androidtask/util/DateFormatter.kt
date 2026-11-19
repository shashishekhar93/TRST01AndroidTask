package com.shekhar.trst01androidtask.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object DateFormatter {

    private val OUTPUT_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, hh:mm a", Locale.ENGLISH)

    /**
     * Formats an ISO-8601 date-time string into "EEE, dd MMM yyyy, hh:mm a" format.
     * Example: "2026-11-10T16:09:00" -> "Tue, 10 Nov 2026, 04:09 PM"
     *
     * Timezone Policy:
     * - Timestamps without timezone/offset (e.g., "2026-11-10T16:09:00") are parsed as LocalDateTime
     *   and formatted directly without shifting.
     * - Timestamps with timezone/offset (e.g., "2026-11-10T16:09:00Z" or "2026-11-10T16:09:00+05:30")
     *   are converted to the specified target [zoneId] (default: systemDefault()) before formatting.
     */
    fun formatDate(rawDate: String?, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (rawDate.isNullOrBlank()) return "N/A"
        val trimmed = rawDate.trim()

        return try {
            // Check if string explicitly contains offset/timezone info
            val hasExplicitOffset = trimmed.contains("Z", ignoreCase = true) ||
                    (trimmed.contains("T") && (
                        trimmed.indexOf('+', startIndex = trimmed.indexOf('T')) != -1 ||
                        trimmed.indexOf('-', startIndex = trimmed.indexOf('T')) != -1
                    ))

            if (hasExplicitOffset) {
                val zdt = try {
                    ZonedDateTime.parse(trimmed)
                } catch (_: DateTimeParseException) {
                    OffsetDateTime.parse(trimmed).toZonedDateTime()
                }
                val converted = zdt.withZoneSameInstant(zoneId)
                return OUTPUT_FORMATTER.format(converted)
            }

            // Parse ISO LocalDateTime without offset (e.g., 2026-11-10T16:09:00 or 2026-11-10T16:09:00.123)
            try {
                val ldt = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                return OUTPUT_FORMATTER.format(ldt)
            } catch (_: DateTimeParseException) {
            }

            // Parse ISO LocalDate without time (e.g., 2026-11-10)
            try {
                val ld = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE)
                return OUTPUT_FORMATTER.format(ld.atStartOfDay())
            } catch (_: DateTimeParseException) {
            }

            "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }
}
