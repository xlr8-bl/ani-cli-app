package com.xlr8.app.data

import java.util.Calendar

/** AniList season helpers derived from the current date. */
object AnimeSeason {

    /** Returns the AniList season constant for [month] (1-based). */
    private fun seasonForMonth(month: Int): String = when (month) {
        12, 1, 2 -> "WINTER"
        3, 4, 5 -> "SPRING"
        6, 7, 8 -> "SUMMER"
        else -> "FALL"
    }

    data class SeasonYear(val season: String, val year: Int)

    fun current(calendar: Calendar = Calendar.getInstance()): SeasonYear {
        val month = calendar.get(Calendar.MONTH) + 1
        var year = calendar.get(Calendar.YEAR)
        // December belongs to the following year's WINTER season on AniList.
        if (month == 12) year += 1
        return SeasonYear(seasonForMonth(month), year)
    }

    fun next(calendar: Calendar = Calendar.getInstance()): SeasonYear {
        val cur = current(calendar)
        val order = listOf("WINTER", "SPRING", "SUMMER", "FALL")
        val idx = order.indexOf(cur.season)
        return if (idx == order.lastIndex) {
            SeasonYear("WINTER", cur.year + 1)
        } else {
            SeasonYear(order[idx + 1], cur.year)
        }
    }
}
