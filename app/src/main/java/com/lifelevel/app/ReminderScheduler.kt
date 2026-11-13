package com.lifelevel.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler {
    private const val WATER = 101
    private const val FAST_START = 102
    private const val FAST_END = 103

    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences("lifelevel", Context.MODE_PRIVATE)
        cancel(context, WATER); cancel(context, FAST_START); cancel(context, FAST_END)
        if (prefs.getBoolean("waterEnabled", true)) {
            val hours = prefs.getInt("waterEvery", 2).coerceIn(1, 8)
            scheduleRepeating(context, WATER, hours * 60 * 60 * 1000L, "water")
        }
        if (prefs.getBoolean("fastingEnabled", true)) {
            scheduleDaily(context, FAST_START, prefs.getString("eatStart", "16:00") ?: "16:00", "fast_start")
            scheduleDaily(context, FAST_END, prefs.getString("eatEnd", "00:00") ?: "00:00", "fast_end")
        }
    }

    private fun scheduleRepeating(context: Context, id: Int, interval: Long, type: String) {
        val first = System.currentTimeMillis() + interval
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, first, pending(context, id, type))
        // Re-arm after every delivery so it survives process death without exact-alarm access.
    }

    private fun scheduleDaily(context: Context, id: Int, time: String, type: String) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0,23) ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0,59) ?: 0
        var next = LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
        val trigger = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending(context, id, type))
    }

    private fun pending(context: Context, id: Int, type: String) =
        PendingIntent.getBroadcast(context, id, Intent(context, ReminderReceiver::class.java).putExtra("type", type), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun cancel(context: Context, id: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(context, id, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) am.cancel(pi)
    }
}
