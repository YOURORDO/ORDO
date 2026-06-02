package com.example.ordo

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import androidx.compose.ui.res.painterResource

// 1. СИММЕТРИЧНАЯ ГЕОМЕТРИЯ КАПСУЛЫ (Nodes Style)
// Создаем гибкий генератор гексагонов
fun getHexShape(cornerRadius: Float): androidx.compose.ui.graphics.Shape = GenericShape { size, _ ->
    val radius = size.minDimension / 2f
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val angles = (0..5).map { Math.toRadians(30.0 + it * 60.0) }
    val vertices = angles.map { angle ->
        (centerX + radius * kotlin.math.cos(angle).toFloat()) to (centerY + radius * kotlin.math.sin(angle).toFloat())
    }
    for (i in 0..5) {
        val current = vertices[i]
        val next = vertices[(i + 1) % 6]
        val dx = next.first - current.first
        val dy = next.second - current.second
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val offset = if (dist > 0) minOf(cornerRadius, dist / 2) else 0f
        val ratio = if (dist > 0) offset / dist else 0f
        val x1 = current.first + dx * ratio
        val y1 = current.second + dy * ratio
        val prevIndex = if (i == 0) 5 else i - 1
        val prev = vertices[prevIndex]
        val dxPrev = current.first - prev.first
        val dyPrev = current.second - prev.second
        val distPrev = kotlin.math.sqrt(dxPrev * dxPrev + dyPrev * dyPrev)
        val ratioPrev = if (distPrev > 0) minOf(cornerRadius, distPrev / 2) / distPrev else 0f
        val xStart = current.first - dxPrev * ratioPrev
        val yStart = current.second - dyPrev * ratioPrev
        if (i == 0) this.moveTo(xStart, yStart) else this.lineTo(xStart, yStart)
        this.quadraticBezierTo(current.first, current.second, x1, y1)
    }
    this.close()
}

// Сохраняем старый RoundedHexagonShape для аватарок, чтобы ничего не сломать
val RoundedHexagonShape = getHexShape(12f)

// Обновленная симметричная плашка (Nodes Style)
val KineticHeaderShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height

    // Настройка "среза" углов (как на твоём макете)
    val cutSize = h * 0.38f
    // Высота вертикальной стенки сбоку
    val sideHeight = h * 0.48f
    val r = 16f // Радиус закругления

    val sideStart = (h - sideHeight) / 2f
    val sideEnd = sideStart + sideHeight

    // 1. Верхняя линия (движемся слева направо)
    moveTo(cutSize + r, 0f)
    lineTo(w - cutSize - r, 0f)

    // 2. ПРАВЫЙ ВЕРХНИЙ УГОЛ
    // Плавный переход от горизонтали к диагонали
    quadraticBezierTo(w - cutSize, 0f, w - cutSize + r/2, r/2)
    lineTo(w - r/2, sideStart - r/2)
    // Плавный переход от диагонали к вертикали
    quadraticBezierTo(w, sideStart, w, sideStart + r)

    // 3. ПРАВЫЙ ТОРЕЦ
    lineTo(w, sideEnd - r)

    // 4. ПРАВЫЙ НИЖНИЙ УГОЛ
    quadraticBezierTo(w, sideEnd, w - r/2, sideEnd + r/2)
    lineTo(w - cutSize + r/2, h - r/2)
    quadraticBezierTo(w - cutSize, h, w - cutSize - r, h)

    // 5. НИЖНЯЯ ЛИНИЯ
    lineTo(cutSize + r, h)

    // 6. ЛЕВЫЙ НИЖНИЙ УГОЛ
    quadraticBezierTo(cutSize, h, cutSize - r/2, h - r/2)
    lineTo(r/2, sideEnd + r/2)
    quadraticBezierTo(0f, sideEnd, 0f, sideEnd - r)

    // 7. ЛЕВЫЙ ТОРЕЦ
    lineTo(0f, sideStart + r)

    // 8. ЛЕВЫЙ ВЕРХНИЙ УГОЛ (Исправленный "косяк")
    // Плавный переход от вертикали к диагонали
    quadraticBezierTo(0f, sideStart, r/2, sideStart - r/2)
    lineTo(cutSize - r/2, r/2)
    // Плавный переход от диагонали к горизонтали (замыкаем на начало)
    quadraticBezierTo(cutSize, 0f, cutSize + r, 0f)

    close()
}

enum class HeaderState { ONLINE, OFFLINE, SHADOW }

@Composable
fun KineticHeader(
    partnerName: String,
    cellNumber: String,
    partnerAvatarBase64: String?,
    isPartnerOnline: Boolean,
    state: HeaderState,
    onStateChanged: (HeaderState) -> Unit,
    onPingClick: () -> Unit,
    // НОВАЯ СТРОЧКА: Это наш "провод", по которому мы будем передавать процент сдвига экрана
    onDragProgress: (Float) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 1. ПАРАМЕТРЫ ПРИБОРА
    val capsuleWidthDp = (configuration.screenWidthDp * 0.95f).dp
    val capsuleWidthPx = with(density) { capsuleWidthDp.toPx() }
    val avatarBoxSizePx = with(density) { 72.dp.toPx() }
    val edgePaddingPx = with(density) { 8.dp.toPx() }

    // 2. ТОЧКИ ПРИМАГНИЧИВАНИЯ
    val posOnline = edgePaddingPx
    val posOffline = (capsuleWidthPx / 2f) - (avatarBoxSizePx / 2f)
    val posShadow = capsuleWidthPx - avatarBoxSizePx - edgePaddingPx

    // Координата бегунка (авы) — теперь это суперлегкий Float-стейт
    var offsetX by remember { mutableStateOf(posOffline) }
    var currentState by remember { mutableStateOf(HeaderState.OFFLINE) }

    // --- УЛУЧШЕННЫЙ ДАТЧИК ДВИЖЕНИЯ С "МЕРТВОЙ ЗОНОЙ" ---
    LaunchedEffect(offsetX) { // Следим за изменением Float-координаты напрямую
        // 1. Считаем чистый прогресс (от 0 до 1)
        val rawProgress = if (offsetX > posOffline) {
            (offsetX - posOffline) / (posShadow - posOffline)
        } else {
            0f
        }

        // 2. Настраиваем "порог срабатывания"

        val threshold = 0.15f

        val smoothProgress = if (rawProgress < threshold) {
            0f // Пока не прошли порог, экран стоит на месте
        } else {
            // Как только порог пройден, плавно догоняем движение
            (rawProgress - threshold) / (1f - threshold)
        }

        // Отправляем финальное значение
        onDragProgress(smoothProgress.coerceIn(0f, 1f))
    }
    // --------------------------------------------

    // --- ЦВЕТОВАЯ ПАЛИТРА (ЧИСТЫЕ ЦВЕТА) ---
    val targetGlowColor = when (currentState) {
        // Нежный голубой неон
        HeaderState.ONLINE -> Color(0xFFace5ff)

        HeaderState.SHADOW -> Color(0xFFfff6d0)

        else -> Color.White.copy(alpha = 0.4f)
    }

    val glowColor by animateColorAsState(
        targetValue = targetGlowColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    fun snapToClosest() {
        scope.launch {
            val target = when {
                offsetX < (posOnline + posOffline) / 2 -> posOnline
                offsetX > (posOffline + posShadow) / 2 -> posShadow
                else -> posOffline
            }
            // Запускаем анимацию кадра за кадром и плавно доводим наше легкое Float-значение
            Animatable(offsetX).animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            ) {
                offsetX = this.value // Плавно обновляем интерфейс на каждом шаге анимации
            }
            val newState = when(target) {
                posOnline -> HeaderState.ONLINE
                posShadow -> HeaderState.SHADOW
                else -> HeaderState.OFFLINE
            }
            if (newState != currentState) {
                currentState = newState
                onStateChanged(newState)
            }
        }
    }

    LaunchedEffect(state) {
        if (state == HeaderState.OFFLINE) {
            Animatable(offsetX).animateTo(
                targetValue = posOffline,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            ) {
                offsetX = this.value
            }
            currentState = HeaderState.OFFLINE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(top = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Твой стеклянный градиент
        val glassFillCapsule = androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.04f))
        )

        // --- ЧИСТАЯ ПЛАШКА: ОДИН ЦВЕТ ВНУТРИ ---
        Box(
            modifier = Modifier
                .width(capsuleWidthDp)
                .height(85.dp)
                // 1. Внешнее свечение (ореол вокруг)
                .shadow(
                    elevation = if (currentState != HeaderState.OFFLINE) 40.dp else 0.dp,
                    shape = KineticHeaderShape,
                    spotColor = glowColor,
                    ambientColor = glowColor
                )
                // 2. ЕДИНСТВЕННАЯ ЗАЛИВКА ЦВЕТОМ (сплошной тон)
                // Мы убрали "стекло", теперь внутри только этот цвет
                .background(
                    color = glowColor.copy(alpha = 0.3f),
                    shape = KineticHeaderShape
                )
                // 3. ЕДИНСТВЕННАЯ РАМКА
                .border(
                    width = 1.5.dp,
                    color = glowColor.copy(alpha = 0.7f),
                    shape = KineticHeaderShape
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // Блокируем движение соты-аватарки во сне, если это системный Рупор!
                        if (cellNumber != "ВЕЩАНИЕ") {
                            offsetX = (offsetX + delta).coerceIn(posOnline, posShadow)
                        }
                    },
                    onDragStopped = { _ ->
                        if (cellNumber != "ВЕЩАНИЕ") {
                            snapToClosest()
                        }
                    }
                )
        ) {
            // Убираем .value, так как offsetX теперь обычный Float
            val textAlpha = if (offsetX > posOffline) {
                (1f - (offsetX - posOffline) / (posShadow - posOffline)).coerceIn(0f, 1f)
            } else 1f

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .offset(x = with(density) { offsetX.toDp() }), //offsetX теперь Float, а не Animatable
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onPingClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().offset(x = 2.dp, y = 3.dp).graphicsLayer(alpha = 0.15f).background(Color.Black, RoundedHexagonShape))

                    val glassFillAva = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                    val borderAva = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.2f)))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedHexagonShape)
                            .border(1.5.dp, borderAva, RoundedHexagonShape)
                            .background(glassFillAva),
                        contentAlignment = Alignment.Center
                    ) {
                        // Сверяем значение с локализованной меткой из L10n (поймет и "Системный", и "System")
                        if (cellNumber == L10n.systemLabel) {

                            Image(
                                painter = painterResource(id = R.drawable.f_ordo),
                                contentDescription = null,
                                modifier = Modifier.size(66.dp).clip(RoundedHexagonShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (partnerAvatarBase64 != null) {
                            val bitmap = remember(partnerAvatarBase64) {
                                try {
                                    val bytes = android.util.Base64.decode(partnerAvatarBase64, android.util.Base64.NO_WRAP)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(66.dp).clip(RoundedHexagonShape),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = if (isPartnerOnline) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.graphicsLayer(alpha = textAlpha)) {
                    // 1. Имя (остается без изменений)
                    Text(text = partnerName, color = Color(0xFF00C853), fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)

                    // 2. НОВЫЙ БЛОК: Номер ячейки (курсив, серый цвет, мелкий шрифт)
                    Text(text = cellNumber, color = Color.Gray.copy(alpha = 0.4f), fontSize = 11.sp, fontStyle = FontStyle.Italic)

                    // 3. Статус сети (СТАЛО — ЛОКАЛИЗАЦИЯ)
                    Text(
                        text = if (isPartnerOnline) L10n.userOnline else L10n.userOffline, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                        color = if (isPartnerOnline) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

// ГЕОМЕТРИЯ АВАТАРКИ (Чтобы файл её видел)
val HexAvatarShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w / 2f, 0f)
    lineTo(w, h * 0.21f)
    lineTo(w, h * 0.79f)
    lineTo(w / 2f, h)
    lineTo(0f, h * 0.79f)
    lineTo(0f, h * 0.21f)
    close()
}