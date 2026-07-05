/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.utilities

import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneOffset

object TimeParser {
    private val regex = Regex("(\\d+)\\s*([YMDhms])")

    /**
     * Parses a duration string and applies it to a given starting Instant,
     * returning the resulting Instant.
     *
     * The input string should follow a specific format where the duration is specified
     * using integer values followed by unit specifiers. Supported units are:
     * - "Y" for years
     * - "M" for months
     * - "D" for days
     * - "h" for hours
     * - "m" for minutes
     * - "s" for seconds
     *
     * @param input A string representing the duration to be parsed. Should be in a format
     *              like "1Y 2M 3D 4h 5m 6s" or similar combinations.
     * @param start The starting Instant to which the parsed duration will be applied.
     * @return The resulting Instant after applying the parsed duration, or null if the input
     *         string does not contain a valid duration format.
     */
    fun parseToInstant(input: String, start: Instant): Instant? {
        val matches = regex.findAll(input).toList()
        if (matches.isEmpty()) {
            return null
        }

        var years = 0
        var months = 0
        var days = 0
        var hours = 0L
        var minutes = 0L
        var seconds = 0L

        matches.forEach { result ->
            val value = result.groupValues[1]
            val unit = result.groupValues[2]

            when (unit) {
                "Y" -> years = value.toInt()
                "M" -> months = value.toInt()
                "D" -> days = value.toInt()
                "h" -> hours = value.toLong()
                "m" -> minutes = value.toLong()
                "s" -> seconds = value.toLong()
            }
        }

        val zonedDateTime = start.atZone(ZoneOffset.UTC)

        val period = Period.of(years, months, days)
        val duration = Duration
            .ofHours(hours)
            .plusMinutes(minutes)
            .plusSeconds(seconds)

        return zonedDateTime
            .plus(period)
            .plus(duration)
            .toInstant()
    }
}
