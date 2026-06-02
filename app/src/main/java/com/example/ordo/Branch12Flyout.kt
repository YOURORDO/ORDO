package com.example.ordo

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Branch12FlyoutMenu(
    state: Branch12State,
    activeNode: HexNode,
    plusSlotIndex: Int,
    contentColor: Color,
    onOptionSelected: (HexNodeType) -> Unit
) {
    val menuOpen = state.activeFlyoutNodeId != null
    val progress by animateFloatAsState(
        targetValue = if (menuOpen) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.8f),
        label = "flyout"
    )

    if (progress > 0.01f) {
        val startSlot = state.currentPlusSlotIndex
        val startPos = HexGridData.slots[startSlot] ?: Pair(322, 2160)
        val targets = HexGridData.getFlyoutTargets(startSlot)

        val options = listOf(
            Triple(targets[0], L10n.newFolder, HexNodeType.STANDARD),
            Triple(targets[1], L10n.newChat, HexNodeType.CHAT),
            Triple(targets[2], L10n.newChannel, HexNodeType.CHANNEL)
        )

        options.forEach { (targetIdx, label, type) ->
            val targetPos = HexGridData.slots[targetIdx] ?: startPos
            val curX = (startPos.first + (targetPos.first - startPos.first) * progress).toInt()
            val curY = (startPos.second + (targetPos.second - startPos.second) * progress).toInt()

            FlyoutItem(curX, curY, label, progress, contentColor) { onOptionSelected(type) }
        }
    }
}

@Composable
fun FlyoutItem(x: Int, y: Int, label: String, alpha: Float, contentColor: Color, onClick: () -> Unit) {
    val density = LocalDensity.current

    Box {
        RenderHexagon(
            props = RenderProps(x, y, 290, alpha),
            node = HexNode("opt", "", contentColor, 0, isAddButton = true),
            slots = emptyList(), // Передаем пустой список для совместимости (СТАЛО)
            contentColor = contentColor,
            onClick = onClick,
            alphaFactor = alpha,
            scaleFactor = 0.8f + (0.2f * alpha)
        )

        if (alpha > 0.8f) {
            val finalX = with(density) { ScreenMetrics.scaleX(x).toDp() + 50.dp }
            val finalY = with(density) { ScreenMetrics.scaleY(y).toDp() - 10.dp }

            Box(modifier = Modifier.offset(x = finalX, y = finalY)) {
                // --- СЛОЙ 1: БЕЛЫЙ ОБРИС (КОНТУР) ---
                Text(
                    text = label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = TextStyle(
                        color = Color.White.copy(alpha = alpha * 0.9f), // Яркий белый контур
                        drawStyle = Stroke(
                            miter = 10f,
                            width = 3f, // Тонкий обрис
                            join = StrokeJoin.Round
                        )
                    )
                )

                // --- СЛОЙ 2: ТЕМНЫЙ ТЕКСТ (ОСНОВНОЙ) ---
                Text(
                    text = label,
                    // Используем темно-серый или черный цвет для самого текста
                    color = Color(0xFF222222).copy(alpha = alpha),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}