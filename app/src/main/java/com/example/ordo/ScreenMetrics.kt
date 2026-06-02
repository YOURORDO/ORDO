package com.example.ordo

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

object ScreenMetrics {
    var scale: Float = 1f
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    fun init(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenW: Float
        val screenH: Float

        // Достаем РЕАЛЬНЫЕ физические пиксели матрицы экрана, без учета системных панелей
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenW = bounds.width().toFloat()
            screenH = bounds.height().toFloat()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            screenW = displayMetrics.widthPixels.toFloat()
            screenH = displayMetrics.heightPixels.toFloat()
        }

        // Твоё золотое сечение (Doogee V20)
        val baseW = 1080f
        val baseH = 2400f

        // Считаем масштаб по минимальной стороне, чтобы ничего не обрезалось
        scale = minOf(screenW / baseW, screenH / baseH)

        // Считаем отступы для идеального центрирования
        offsetX = (screenW - (baseW * scale)) / 2f
        offsetY = (screenH - (baseH * scale)) / 2f
    }

    // Перевод координат X с центрированием
    fun scaleX(x: Int): Int = (x * scale + offsetX).toInt()

    // Перевод координат Y с центрированием
    fun scaleY(y: Int): Int = (y * scale + offsetY).toInt()

    // Масштабирование размеров
    fun scaleSize(size: Float): Float = size * scale

    // Перевод свайпов обратно в размерность Doogee
    fun unscale(value: Float): Float = value / scale
}