package com.example.ordo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThemeToggleButton(isDark: Boolean, color: Color, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedHexagonShape)
            .clip(RoundedHexagonShape)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)

            if (isDark) {
                // Идеальный полумесяц через "вырезание" круга из круга
                val r = size.width / 2.4f

                // Главный круг
                val moonPath = Path().apply {
                    addOval(Rect(cx - r, cy - r, cx + r, cy + r))
                }

                // Круг-резак (смещен вправо и вверх)
                val cutPath = Path().apply {
                    val offset = r * 0.5f
                    val cutX = cx + offset
                    val cutY = cy - offset
                    addOval(Rect(cutX - r, cutY - r, cutX + r, cutY + r))
                }

                // Вычитаем один круг из другого
                val crescent = Path().apply {
                    op(moonPath, cutPath, androidx.compose.ui.graphics.PathOperation.Difference)
                }

                // Рисуем обводку (чтобы месяц был в одном стиле с солнышком)
                drawPath(path = crescent, color = color, style = stroke)

            } else {
                val r = size.width / 4.5f
                drawCircle(color = color, center = Offset(cx, cy), radius = r, style = stroke)
                for (i in 0 until 8) {
                    val angle = i * 45.0
                    val rad = Math.toRadians(angle)
                    val startDist = r + 4.dp.toPx()
                    val endDist = r + 9.dp.toPx()
                    drawLine(color = color, start = Offset(cx + cos(rad).toFloat() * startDist, cy + sin(rad).toFloat() * startDist), end = Offset(cx + cos(rad).toFloat() * endDist, cy + sin(rad).toFloat() * endDist), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}