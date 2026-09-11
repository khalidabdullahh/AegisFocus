/*
 * ==============================================================================
 * 📌 প্রজেক্ট: ASR / Aegis-Focus
 * 📄 ফাইল: TimeUtils.kt
 * 🎯 উদ্দেশ্য: সময় ও তারিখ ফরম্যাটিং ইউটিলিটি ক্লাস।
 * 💡 ভূমিকা: ফোকাস সেশনের টাইমার, ঘণ্টার হিসাব ও তারিখকে ব্যবহারকারীর জন্য আকর্ষণীয়ভাবে প্রদর্শন করে।
 * ==============================================================================
 */
package com.aegisfocus.app.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    /**
     * -------------------------------------------------------------------------
     * 📌 ফাংশন: formatRemainingMillis
     * 🎯 উদ্দেশ্য: অবশিষ্ট মিলিসেকেন্ডকে টাইমার ফরম্যাটে (HH:MM:SS অথবা MM:SS) রূপান্তর।
     * 📥 ইনপুট: millis (অবশিষ্ট সময় মিলিসেকেন্ডে)
     * 📤 আউটপুট: "25:00" বা "01:15:30" স্ট্রিং
     * -------------------------------------------------------------------------
     */
    fun formatRemainingMillis(millis: Long): String {
        // ১. সময় শেষ বা নেগেটিভ হলে ডিফল্ট ০ দেখাচ্ছে
        if (millis <= 0) return "00:00"
        
        // ২. মিলিসেকেন্ডকে মোট সেকেন্ড, ঘণ্টা ও মিনিটে ভাগ করা
        val totalSeconds = (millis + 999) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        // ৩. এক ঘণ্টার বেশি হলে ঘণ্টা সহ ফরম্যাট করে, অন্যথায় শুধু মিনিট:সেকেন্ড
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * -------------------------------------------------------------------------
     * 📌 ফাংশন: formatMinutesHuman
     * 🎯 উদ্দেশ্য: মোট মিনিটকে মানুষের পড়ার উপযোগী "২h ৩০m" ফরম্যাটে প্রদর্শন।
     * -------------------------------------------------------------------------
     */
    fun formatMinutesHuman(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun formatMillisHuman(totalMillis: Long): String {
        val totalMinutes = (totalMillis / 60000).toInt()
        return formatMinutesHuman(totalMinutes)
    }

    fun formatTimeOfDay(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    fun getStartOfDay(epochMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfDay(epochMs: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun getTodayDateString(epochMs: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    fun getFormattedDisplayDate(epochMs: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    fun getGreeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String {
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Night Calm"
        }
    }

    /**
     * Bitmask operations for Days of the Week:
     * Bit 0: Sunday (1)
     * Bit 1: Monday (2)
     * Bit 2: Tuesday (4)
     * Bit 3: Wednesday (8)
     * Bit 4: Thursday (16)
     * Bit 5: Friday (32)
     * Bit 6: Saturday (64)
     */
    fun isDaySelected(bitmask: Int, calendarDayOfWeek: Int): Boolean {
        val bitIndex = calendarDayOfWeek - 1 // Calendar.SUNDAY is 1 -> index 0
        return (bitmask and (1 shl bitIndex)) != 0
    }

    fun toggleDay(bitmask: Int, calendarDayOfWeek: Int): Int {
        val bitIndex = calendarDayOfWeek - 1
        return bitmask xor (1 shl bitIndex)
    }

    fun formatDaysOfWeek(bitmask: Int): String {
        if (bitmask == 127) return "Everyday"
        if (bitmask == 62) return "Mon – Fri"
        if (bitmask == 65) return "Weekends"

        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val activeDays = mutableListOf<String>()
        for (i in 0..6) {
            if ((bitmask and (1 shl i)) != 0) {
                activeDays.add(dayNames[i])
            }
        }
        return if (activeDays.isEmpty()) "No days" else activeDays.joinToString(", ")
    }
}
