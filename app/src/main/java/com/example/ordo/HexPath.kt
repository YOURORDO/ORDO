package com.example.ordo

import kotlin.math.floor

object HexPath {
    private val TRACK = listOf(
        Point(-193, 2600),
        Point(150, 1850),
        Point(493, 1850),
        Point(673, 2160),
        Point(836, 2442),
        Point(836, 2800)
    )

    private val TEXT_TRACK = listOf(
        Point(-100, 1500),
        Point(145, 1585),
        Point(530, 1670),
        Point(730, 1930),
        Point(900, 2200),
        Point(900, 2500)
    )

    data class Point(val x: Int, val y: Int)

    fun interpolate(trackPosition: Float): RenderProps {
        // 🔥 МАГИЯ 1: Используем floor() вместо toInt(), чтобы путь не ломался!
        val index = floor(trackPosition).toInt()
        val fraction = trackPosition - index
        val listIndex = (index + 1).coerceIn(0, TRACK.size - 2)
        val pStart = TRACK[listIndex]
        val pEnd = TRACK[listIndex + 1]
        val currentX = lerp(pStart.x, pEnd.x, fraction)
        val currentY = lerp(pStart.y, pEnd.y, fraction)

        val alpha = when {
            trackPosition < -0.8f -> 0f
            trackPosition > 3.5f -> 0f
            trackPosition < 0f -> (1f + (trackPosition * 5f)).coerceIn(0f, 1f)
            trackPosition > 3f -> 1f - (trackPosition - 3f)
            else -> 1f
        }

        return RenderProps(currentX, currentY, 290, alpha.coerceIn(0f, 1f))
    }

    fun interpolateText(trackPosition: Float): RenderProps {
        // 🔥 И здесь тоже ставим floor()
        val index = floor(trackPosition).toInt()
        val fraction = trackPosition - index
        val listIndex = (index + 1).coerceIn(0, TEXT_TRACK.size - 2)

        val pStart = TEXT_TRACK[listIndex]
        val pEnd = TEXT_TRACK[listIndex + 1]

        val currentX = lerp(pStart.x, pEnd.x, fraction)
        val currentY = lerp(pStart.y, pEnd.y, fraction)

        val alpha = when {
            trackPosition < -0.5f -> 0f
            trackPosition < 0f -> 1f + (trackPosition * 2)
            trackPosition > 2.8f -> 0f
            else -> 1f
        }

        return RenderProps(currentX, currentY, 0, alpha.coerceIn(0f, 1f))
    }

    private fun lerp(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).toInt()
    }
}