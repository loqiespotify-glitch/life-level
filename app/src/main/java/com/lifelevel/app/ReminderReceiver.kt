package com.lifelevel.app

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("lifelevel", Context.MODE_PRIVATE)
        val type = intent.getStringExtra("type") ?: return
        val (title, body) = when(type) {
            "water" -> "Время выпить воды" to "Добавь стакан воды в дневной баланс."
            "fast_start" -> "Окно питания открыто" to "Началось запланированное окно питания."
            else -> "Окно питания закрывается" to "Пора завершить приём пищи и начать период голодания."
        }
        val notification = NotificationCompat.Builder(context, "lifelevel_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(type.hashCode(), notification)

        // Re-arm the next occurrence. Water reminders use the configured interval.
        when(type) {
            "water" -> {
                if (prefs.getBoolean("waterEnabled", true)) {
                    val hours = prefs.getInt("waterEvery", 2).coerceIn(1,8)
                    val pi = PendingIntent.getBroadcast(context, 101, Intent(context, ReminderReceiver::class.java).putExtra("type","water"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+hours*60*60*1000L, pi)
                }
            }
            "fast_start", "fast_end" -> ReminderScheduler.scheduleAll(context)
        }
    }
}
