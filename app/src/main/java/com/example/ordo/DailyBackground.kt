package com.example.ordo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DailyBackground(
    isStarted: Boolean,
    isDarkTheme: Boolean,
    dragProgress: Float, // Натяжение от 0 до 1
    bgNumber: Int,       // Номер картинки, который мы меняем кнопкой
    onManualCleanup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 1. Получаем ID картинки напрямую по номеру (fon1, fon2...)
    // remember(bgNumber) заставит картинку обновиться мгновенно при нажатии кнопки
    val imageRes = remember(bgNumber) {
        val resId = context.resources.getIdentifier("fon$bgNumber", "drawable", context.packageName)
        if (resId != 0) resId else R.drawable.fon1 // Если вдруг файла нет, покажет первый
    }

    // 2. Анимация радиуса (0 -> 1200)
    val radiusAnim = remember { Animatable(0f) }

    // 3. Общая прозрачность слоя (шторки)
    val overlayAlpha = remember { Animatable(1f) }

    // Управление анимациями
    LaunchedEffect(isStarted) {
        if (!isStarted) {
            // Режим интро: шторка плотная, дырка растет
            overlayAlpha.snapTo(1f)
            radiusAnim.animateTo(1200f, tween(2500))
        } else {
            // Режим меню: дырка уже большая, шторка исчезает
            radiusAnim.snapTo(1200f)
            overlayAlpha.animateTo(0f, tween(500))
        }
    }

    // Итоговая прозрачность: если меню активно — слушаем палец, если нет — анимацию
    val currentOverlayAlpha = if (isStarted) dragProgress else overlayAlpha.value

    Box(modifier = Modifier
        .fillMaxSize()
        // Тап для ручного скрытия шторки после интро
        .pointerInput(isStarted) {
            detectTapGestures {
                if (!isStarted && radiusAnim.value > 1100f) {
                    scope.launch { overlayAlpha.animateTo(0f, tween(500)) }
                    onManualCleanup()
                }
            }
        }
    ) {
        // --- СЛОЙ 1: КАРТИНКА ---
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterStart
        )

        // --- СЛОЙ 2: ШТОРКА ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Рассчитываем координаты локально.
            // DrawScope наследует интерфейс Density, поэтому with(density) больше не требуется.
            val baseY = size.height * 0.35f
            val correction = 70.dp.toPx()
            val centerPoint = Offset(size.width / 2f, baseY + correction)

            drawRect(
                brush = Brush.radialGradient(
                    0.0f to Color.Transparent,
                    // Цвет тумана зависит от темы
                    1.0f to (if (isDarkTheme) Color(0xFF121212) else Color.White),
                    center = centerPoint,
                    radius = radiusAnim.value.coerceAtLeast(1f)
                ),
                alpha = currentOverlayAlpha
            )
        }
    }
}