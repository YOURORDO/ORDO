package com.example.ordo

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FlyingTitle(
    shouldBeAtTop: Boolean,
    text: String,
    textColor: Color,
    stripeHeight: Dp,
    screenHeight: Dp,
    onStartClick: () -> Unit
) {
    val context = LocalContext.current
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    val soundId = remember {
        val resId = context.resources.getIdentifier("typing", "raw", context.packageName)
        if (resId != 0) soundPool.load(context, resId, 1) else 0
    }
    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    val targetY = if (shouldBeAtTop) (stripeHeight / 2) else (screenHeight * 0.35f)
    val animatedY by animateDpAsState(targetValue = targetY, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "tY")
    val animationProgress = remember { Animatable(0f) }
    var displayText by remember { mutableStateOf(".") }
    val animationDuration = 3000

    LaunchedEffect(Unit) {
        launch { animationProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)) }
        launch {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < animationDuration) {
                val elapsed = System.currentTimeMillis() - startTime

                displayText = when {
                    elapsed < 300 -> "."
                    // Первый раз "ТВОЙ" / "YOUR" на 0.1 сек
                    elapsed in 900..1000 -> L10n.introYour
                    // Второй раз "ТВОЙ" / "YOUR" на 0.1 сек
                    elapsed in 1400..1500 -> L10n.introYour
                    // Третий раз "ТВОЙ" / "YOUR" на 0.1 сек
                    elapsed in 1900..2000 -> L10n.introYour
                    else -> generateGlitchText()
                }

                if (soundId != 0 && elapsed > 300) {
                    val randomRate = Random.nextFloat() * 0.4f + 0.8f
                    soundPool.play(soundId, 0.3f, 0.3f, 0, 0, randomRate)
                }
                delay(50)
            }
            // Финал заставки: "ПОРЯДОК" или "ORDO" в зависимости от языка
            displayText = L10n.introFinal
        }
    }

    Box(modifier = Modifier.fillMaxSize().offset(y = animatedY - 30.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(200.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onStartClick() }
                .graphicsLayer {
                    val scale = animationProgress.value
                    scaleX = scale
                    scaleY = scale
                    alpha = scale.coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.Center
        ) {
            if (!shouldBeAtTop) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val path = Path()
                    val radius = width / 2f
                    val cx = width / 2f
                    val cy = height / 2f
                    val angles = (0..5).map { Math.toRadians(30.0 + it * 60.0) }
                    val vertices = angles.map { angle ->
                        (cx + radius * cos(angle).toFloat()) to (cy + radius * sin(angle).toFloat())
                    }
                    path.moveTo(vertices[0].first, vertices[0].second)
                    for (i in 1..5) path.lineTo(vertices[i].first, vertices[i].second)
                    path.close()
                    drawPath(path = path, color = textColor, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            val currentText = if (shouldBeAtTop) text else displayText

            // Динамически рассчитываем размер шрифта:
            // Если включен английский язык — ставим 50.sp, если русский — ставим 30.sp
            val computedFontSize = if (L10n.currentLanguage == "en") 50.sp else 30.sp

            Text(
                text = currentText,
                color = textColor,
                fontSize = computedFontSize, // Применяем динамический размер шрифта
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }
    }
}