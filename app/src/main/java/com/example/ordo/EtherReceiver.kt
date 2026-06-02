package com.example.ordo

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class EtherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!AppSettings.isBackgroundModeEnabled(context)) return

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isServiceRunning = activityManager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == EtherService::class.java.name
        }

        val serviceIntent = Intent(context, EtherService::class.java)

        if (isServiceRunning) {
            serviceIntent.action = "FORCE_RECONNECT_CHECK"
        }

        try {
            // Если служба уже работает — мы можем безопасно послать ей команду через старт.
            // Если служба уничтожена системой во сне — запускаем её, используя привилегии
            // точного будильника (setExactAndAllowWhileIdle дает право запуска Foreground-службы).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("ETHER_RECEIVER", "Фоновый запуск службы ограничен системой: ${e.message}")
        }

        rescheduleAlarm(context)
    }

    private fun rescheduleAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EtherReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val intervalMillis = 15 * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + intervalMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("ETHER_RECEIVER", "Нет прав на будильник: ${e.message}")
        }
    }
}