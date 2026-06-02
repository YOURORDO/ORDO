package com.example.ordo

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

// 1. Оптимизированная структура ребра сетки с предварительным расчетом геометрии
data class WeldedEdge(
    val start: Offset,
    val end: Offset,
    val d1: Float,
    val d2: Float,
    val minD: Float = min(d1, d2),
    val maxD: Float = max(d1, d2),
    val lStart: Offset = if (d1 <= d2) start else end,
    val lEnd: Offset = if (d1 <= d2) end else start
)

// 2. Вспомогательная функция отрисовки волны (находится на уровне файла, вне HexMeshOverlay)
private fun DrawScope.drawWave(
    currentR: Float,
    weldedMesh: List<WeldedEdge>,
    trail: Float,
    baseWidth: Float,
    headScale: Float,
    sparkColor: Color,
    maxWaveDist: Float
) {
    if (currentR <= 0f || currentR >= maxWaveDist + trail) return

    weldedMesh.forEach { edge ->
        // Быстрая проверка пересечения по предрассчитанным полям
        if (currentR > edge.minD && (currentR - trail) < edge.maxD) {
            val sStart = max(edge.minD, currentR - trail)
            val sEnd = min(edge.maxD, currentR)

            val tStart = if (edge.maxD > edge.minD) (sStart - edge.minD) / (edge.maxD - edge.minD) else 0f
            val tEnd = if (edge.maxD > edge.minD) (sEnd - edge.minD) / (edge.maxD - edge.minD) else 1f

            val dStart = Offset(
                edge.lStart.x + (edge.lEnd.x - edge.lStart.x) * tStart,
                edge.lStart.y + (edge.lEnd.y - edge.lStart.y) * tStart
            )
            val dEnd = Offset(
                edge.lStart.x + (edge.lEnd.x - edge.lStart.x) * tEnd,
                edge.lStart.y + (edge.lEnd.y - edge.lStart.y) * tEnd
            )

            val brightness = ((sEnd - (currentR - trail)) / trail).coerceIn(0f, 1f)

            // Рисуем хвост светлячка (используем встроенный аппаратный параметр alpha)
            drawLine(
                color = sparkColor,
                start = dStart,
                end = dEnd,
                strokeWidth = baseWidth * brightness,
                cap = StrokeCap.Round,
                alpha = brightness * HexThemeConfig.MESH_MAX_ALPHA
            )

            // Рисуем голову светлячка
            if (sEnd == currentR || sEnd == edge.maxD) {
                drawCircle(
                    color = sparkColor,
                    radius = (baseWidth * headScale * brightness) / 2f,
                    center = dEnd,
                    alpha = brightness * HexThemeConfig.MESH_MAX_ALPHA * 1.2f
                )
            }
        }
    }
}

// 3. Основная Composable-функция сетки
@Composable
fun HexMeshOverlay(isVisible: Boolean, activeNode: HexNode) {
    if (!isVisible) return

    // Общая длительность одного цикла (удар + длинная пауза)
    val totalTimeMs = (HexThemeConfig.PULSE_SPEED_MS + HexThemeConfig.PULSE_PAUSE_LONG_MS).toLong()

    // --- СИСТЕМНЫЙ ПУЛЬС (Синхронизация с кадрами экрана) ---
    val cycleProgress by produceState(initialValue = 0f) {
        while (true) {
            withFrameMillis { // Обновляем значение каждый раз, когда экран готов рисовать кадр
                val currentTime = System.currentTimeMillis()
                value = (currentTime % totalTimeMs).toFloat()
            }
        }
    }

    // --- ГЕНЕРАЦИЯ СЕТКИ (Кешируем, чтобы не считать каждый кадр) ---
    val weldedMesh = remember(activeNode.id) {
        val radius = 200f
        val visualRadius = 170f
        val allVertices = mutableListOf<Offset>()
        val rawEdges = mutableSetOf<Pair<Int, Int>>()
        val weldTolerance = HexThemeConfig.WELD_TOLERANCE * 2.5f

        fun areClose(p1: Offset, p2: Offset) = (p1 - p2).getDistance() < weldTolerance

        // Собираем вершины по координатам слотов из HexGridData
        HexGridData.slots.values.forEach { (rawX, rawY) ->
            val cx = ScreenMetrics.scaleX(rawX).toFloat()
            val cy = ScreenMetrics.scaleY(rawY).toFloat()
            val scaledR = ScreenMetrics.scaleSize(radius)
            val currentIndices = mutableListOf<Int>()

            for (i in 0..5) {
                val rad = Math.toRadians(30.0 + i * 60.0).toFloat()
                val pt = Offset(cx + scaledR * cos(rad), cy + scaledR * sin(rad))
                val existIdx = allVertices.indexOfFirst { areClose(it, pt) }
                if (existIdx != -1) currentIndices.add(existIdx) else {
                    allVertices.add(pt)
                    currentIndices.add(allVertices.size - 1)
                }
            }
            for (i in 0..5) {
                val idx1 = currentIndices[i]
                val idx2 = currentIndices[(i + 1) % 6]
                rawEdges.add(if (idx1 < idx2) idx1 to idx2 else idx2 to idx1)
            }
        }

        // Центр взрыва — текущая активная сота
        val pulseCenter = Offset(
            ScreenMetrics.scaleX(320).toFloat(),
            ScreenMetrics.scaleY(2160).toFloat()
        )

        val scaledVisualR = ScreenMetrics.scaleSize(visualRadius)

        rawEdges.map { (i1, i2) ->
            val p1 = allVertices[i1]
            val p2 = allVertices[i2]
            WeldedEdge(
                p1,
                p2,
                max(0f, (p1 - pulseCenter).getDistance() - scaledVisualR),
                max(0f, (p2 - pulseCenter).getDistance() - scaledVisualR)
            )
        }
    }

    // --- ОТРИСОВКА ---
    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxWaveDist = 2500f
        val trail = HexThemeConfig.SPARK_TRAIL_LENGTH
        val baseWidth = HexThemeConfig.PULSE_STROKE_WIDTH
        val headScale = HexThemeConfig.SPARK_HEAD_SCALE
        val sparkColor = HexThemeConfig.PULSE_COLOR

        // Вычисляем радиус текущих двух волн (двойной удар)
        val r1 = (cycleProgress / HexThemeConfig.PULSE_SPEED_MS.toFloat()) * maxWaveDist
        val r2 = ((cycleProgress - HexThemeConfig.PULSE_PAUSE_SHORT_MS.toFloat()) / HexThemeConfig.PULSE_SPEED_MS.toFloat()) * maxWaveDist

        // Запускаем отрисовку двух волн без создания динамических списков на лету
        drawWave(r1, weldedMesh, trail, baseWidth, headScale, sparkColor, maxWaveDist)
        drawWave(r2, weldedMesh, trail, baseWidth, headScale, sparkColor, maxWaveDist)
    }
}