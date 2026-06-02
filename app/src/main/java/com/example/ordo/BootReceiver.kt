package com.example.ordo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Если в настройках приложения разрешен фоновый режим
            if (AppSettings.isBackgroundModeEnabled(context)) {
                val serviceIntent = Intent(context, EtherService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BOOT_RECEIVER", "Ошибка автозапуска службы: ${e.message}")
                }
            }
        }
    }
}