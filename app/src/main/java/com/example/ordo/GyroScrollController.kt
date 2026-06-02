package com.example.ordo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class GyroScrollController(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Поток значений скролла от гироскопа (от -1 до 1, где 0 = ровно)
    private val _tiltFactor = MutableStateFlow(0f)
    val tiltFactor = _tiltFactor.asStateFlow()

    private var isListening = false
    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0] // ось X: наклон влево (-) / вправо (+)
                // Игнорируем малые наклоны (dead zone)
                val deadZone = 1.8f // чувствительность: чем меньше, тем чувствительнее
                val factor = when {
                    x < -deadZone -> (x + deadZone) / (10f - deadZone) // диапазон от -1 до 0
                    x > deadZone -> (x - deadZone) / (10f - deadZone)   // диапазон от 0 до 1
                    else -> 0f
                }
                // Ограничиваем максимум 1.0 и -1.0
                val clamped = factor.coerceIn(-1f, 1f)
                if (abs(clamped - _tiltFactor.value) > 0.02f) { // избегаем лишних обновлений
                    _tiltFactor.value = clamped
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        if (!isListening && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            isListening = true
        }
    }

    fun stop() {
        if (isListening) {
            sensorManager.unregisterListener(listener)
            isListening = false
            _tiltFactor.value = 0f
        }
    }
}

@Composable
fun rememberGyroScrollController(): GyroScrollController {
    val context = LocalContext.current
    // Получаем жизненный цикл текущего экрана (СТАЛО)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val controller = remember { GyroScrollController(context) }

    DisposableEffect(controller, lifecycleOwner) {
        // Создаем наблюдатель, который включает/выключает датчик строго по состоянию приложения (СТАЛО)
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    controller.start() // Включаем датчик только когда приложение на экране
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    controller.stop() // Выключаем датчик при любом сворачивании
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.stop()
        }
    }
    return controller
}