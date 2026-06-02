package com.example.ordo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StripedBackground(
    isStarted: Boolean,
    stripeHeight: Dp,
    toggle: Boolean,
    color1: Color,
    color2: Color,
    restingColor: Color,
    children: List<HexNode>,
    textColor: Color,
    showText: Boolean
) {
    val targetColorEven = if (!isStarted) restingColor else (if (toggle) color1 else color2)
    val targetColorOdd = if (!isStarted) restingColor else (if (toggle) color2 else color1)

    val colorEven by animateColorAsState(targetValue = targetColorEven, animationSpec = tween(300), label = "cEven")
    val colorOdd by animateColorAsState(targetValue = targetColorOdd, animationSpec = tween(300), label = "cOdd")

    val topOffset = 3

    Column(modifier = Modifier.fillMaxSize()) {
        repeat(11) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stripeHeight)
                    .background(if (index % 2 == 0) colorEven else colorOdd),
                contentAlignment = Alignment.CenterStart
            ) {
                val childIndex = index - topOffset
                if (showText && childIndex >= 0 && childIndex < children.size && !children[childIndex].isAddButton) {
                    Text(
                        text = children[childIndex].name,
                        color = textColor.copy(alpha = 0.15f),
                        fontSize = 32.sp,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }
            }
        }
    }
}