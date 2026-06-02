package com.example.ordo

import androidx.compose.ui.graphics.Color

val GrayLight = Color(0xFF5C5C5C)
val GrayDark = Color(0xFF3E3E3E)
val TextDark = Color(0xFF333333)

object HexThemeConfig {
    const val LABEL_ALPHA: Float = 0.99f
    const val PLUS_SYMBOL_ALPHA: Float = 0.4f

    // --- НАСТРОЙКИ ТАЙМИНГА ИМПУЛЬСА ---
    const val PULSE_SPEED_MS: Int = 1500         // Скорость одного удара
    const val PULSE_PAUSE_SHORT_MS: Int = 500    // Короткая пауза (между "тык" и "тык")
    const val PULSE_PAUSE_LONG_MS: Int = 2500    // Длинная пауза (между парами)

    // --- НАСТРОЙКИ ИМПУЛЬСА (СВЕТЛЯЧКОВ) ---
    val PULSE_COLOR: Color = Color.White
    const val PULSE_STROKE_WIDTH: Float = 5f    // Базовая толщина хвоста
    const val SPARK_HEAD_SCALE: Float = 3f    // Во сколько раз голова больше хвоста (НОВОЕ)
    const val SPARK_TRAIL_LENGTH: Float = 600f  // Длина хвоста светлячка
    const val MESH_MAX_ALPHA: Float = 0.25f      // Общая яркость
    const val WELD_TOLERANCE: Float = 15f
}
