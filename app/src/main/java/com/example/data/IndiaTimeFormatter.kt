package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats WhatsApp-style "Last seen" and chat/moment timestamps in India Standard Time (Asia/Kolkata).
 */
object IndiaTimeFormatter {
    private val istZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun formatLastSeen(date: Date?, nowMillis: Long = System.currentTimeMillis()): String {
        if (date == null) return "Last seen recently"
        val nowCal = Calendar.getInstance(istZone).apply { timeInMillis = nowMillis }
        val targetCal = Calendar.getInstance(istZone).apply { time = date }

        val timeStr = formatTime(date)

        val sameYear = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
        val sameDay = sameYear && nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        if (sameDay) {
            return "Last seen today at $timeStr"
        }

        val yesterdayCal = Calendar.getInstance(istZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = yesterdayCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) {
            return "Last seen yesterday at $timeStr"
        }

        val datePattern = if (sameYear) "d MMM" else "d MMM yyyy"
        val dateStr = SimpleDateFormat(datePattern, Locale.ENGLISH).apply {
            timeZone = istZone
        }.format(date)
        return "Last seen $dateStr at $timeStr"
    }

    fun formatTime(date: Date?): String {
        if (date == null) return ""
        return SimpleDateFormat("h:mm a", Locale.ENGLISH).apply {
            timeZone = istZone
        }.format(date).lowercase(Locale.ENGLISH)
    }

    fun formatDateTime(date: Date?): String {
        if (date == null) return "Just now"
        return SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).apply {
            timeZone = istZone
        }.format(date).replace("AM", "am").replace("PM", "pm")
    }

    fun dayBucketKey(date: Date?): String {
        if (date == null) return "TODAY"
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = istZone
        }.format(date)
    }

    fun formatDayHeader(date: Date?, nowMillis: Long = System.currentTimeMillis()): String {
        if (date == null) return "Today"
        val nowCal = Calendar.getInstance(istZone).apply { timeInMillis = nowMillis }
        val targetCal = Calendar.getInstance(istZone).apply { time = date }

        val sameYear = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
        val sameDay = sameYear && nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return "Today"

        val yesterdayCal = Calendar.getInstance(istZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = yesterdayCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "Yesterday"

        val pattern = if (sameYear) "EEE, d MMM" else "d MMM yyyy"
        return SimpleDateFormat(pattern, Locale.ENGLISH).apply {
            timeZone = istZone
        }.format(date)
    }
}
