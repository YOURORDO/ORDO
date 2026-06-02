package com.example.ordo

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class EtherService : Service() {

    private val CHANNEL_ID = "EtherChannelV2"
    private val NOTIFICATION_ID = 101

    // --- ЧАСТИЧНЫЙ WAKELOCK ДЛЯ ЗАЩИТЫ ОТ ЗАСЫПАНИЯ ПРОЦЕССОРА ---
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    companion object {
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // --- ГАРАНТИЯ ИНИЦИАЛИЗАЦИИ ПРИ ВОСКРЕШЕНИИ СЛУЖБЫ ---
        try {
            IdentityManager.init(this)
            DatabaseManager.init(this)
            NetworkManager.appContext = applicationContext
        } catch (e: Exception) {
            Log.e("ETHER_SERVICE", "Критический сбой инициализации криптосистемы: ${e.message}")
            stopSelf() // Корректно завершаем работу службы, так как доступ к зашифрованной базе сейчас невозможен
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Проверяем настройку
        if (!AppSettings.isBackgroundModeEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // ПЕРЕХВАТ СИГНАЛА СЛУЖБЫ: Если прилетел принудительный пинок от будильника во сне
        if (intent?.action == "FORCE_RECONNECT_CHECK") {
            Log.d("ORDO_SERVICE", "Получен сигнал FORCE_RECONNECT_CHECK. Продуваем сокет в фоне...")
            val bgServer = AppSettings.getBackgroundServer(this)
            val startUrl = if (bgServer.isBlank()) NetworkManager.DEFAULT_SERVER else bgServer

            // Благодаря нашему недавнему фиксу в connect() старый сокет-зомби
            // будет мягко закрыт, откроется новый, и CHECK_PING заберет все пинги!
            NetworkManager.connect(startUrl)

            return START_STICKY
        }

        // 2. ЕСЛИ УЖЕ РАБОТАЕМ (обычный запуск) — НИЧЕГО НЕ ДЕЛАЕМ
        if (isServiceRunning) {
            return START_STICKY
        }

        // 3. Если не работали, значит это ПЕРВЫЙ запуск
        createNotificationChannel() // Создаем канал

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ПОРЯДОК")
            .setContentText("Фоновый режим активен")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Понижаем приоритет, чтобы не дергало
            .build()

        // 4. Запускаем как Foreground
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isServiceRunning = true // Устанавливаем флаг, что мы в строю!

            // --- АКТИВИРУЕМ WAKELOCK ---
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "ORDO::WakeLock").apply {
                acquire()
            }

            // --- ЗАПУСКАЕМ НАШ ВЕБСОКЕТ В ФОНОВОМ СЕРВИСЕ ---
            val bgServer = AppSettings.getBackgroundServer(this)
            val startUrl = if (bgServer.isBlank()) NetworkManager.DEFAULT_SERVER else bgServer
            NetworkManager.connect(startUrl)

            Log.d("ORDO_SERVICE", "WebSocket и WakeLock успешно запущены в фоновом сервисе")
        } catch (e: Exception) {
            Log.e("ORDO_SERVICE", "Ошибка запуска: ${e.message}")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false // Сбрасываем флаг

        // --- ОСВОБОЖДАЕМ WAKELOCK ПРИ ВЫКЛЮЧЕНИИ ---
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null

        Log.d("ORDO_SERVICE", "Сетевой сервис остановлен, WakeLock освобожден")
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Направляем воскрешение службы через BroadcastReceiver (EtherReceiver),
        // так как прямой запуск Foreground-службы в фоне заблокирован.
        val restartIntent = Intent(applicationContext, EtherReceiver::class.java)

        val restartPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            // Используем точный будильник для обхода фоновых ограничений Android 14+ (Target SDK 36)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmService.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    restartPendingIntent
                )
            } else {
                alarmService.setExact(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    restartPendingIntent
                )
            }
            Log.d("ETHER_SERVICE", "Воскрешение службы запланировано через точный BroadcastAlarm")
        } catch (e: Exception) {
            Log.e("ETHER_SERVICE", "Не удалось запланировать воскрешение службы: ${e.message}")
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Ордо Эфир",
                NotificationManager.IMPORTANCE_HIGH // Высокий уровень для трея
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}