package com.chatnova.ai.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR) -> {
                "Today"
            }
            now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR) == 1 -> {
                "Yesterday"
            }
            else -> {
                dateFormat.format(Date(timestamp))
            }
        }
    }

    fun formatFullDateTime(timestamp: Long): String {
        return fullDateTimeFormat.format(Date(timestamp))
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }
}
