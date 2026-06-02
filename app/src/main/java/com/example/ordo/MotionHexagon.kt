package com.example.ordo

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MotionHexagon(
    props: RenderProps,
    node: HexNode,
    slots: List<SlotEntity>,
    isSmileMenuVisible: Boolean = false, // Принимаем стейт с дефолтным значением для совместимости (СТАЛО)
    isDragging: Boolean,
    contentColor: Color,
    externalModifier: Modifier = Modifier,
    onClick: (HexNode) -> Unit,
    onLongClick: ((HexNode) -> Unit)? = null
) {
    val physicsSpecInt = if (isDragging) spring<Int>(stiffness = 250f, dampingRatio = 0.9f)
    else spring<Int>(stiffness = 60f, dampingRatio = 0.75f)
    val physicsSpecFloat = if (isDragging) spring<Float>(stiffness = 250f, dampingRatio = 0.9f)
    else spring<Float>(stiffness = 60f, dampingRatio = 0.6f)

    val animX by animateIntAsState(props.x, animationSpec = physicsSpecInt, label = "x")
    val animY by animateIntAsState(props.y, animationSpec = physicsSpecInt, label = "y")
    val animSize by animateIntAsState(props.size, animationSpec = physicsSpecInt, label = "size")
    val animAlpha by animateFloatAsState(props.alpha, animationSpec = physicsSpecFloat, label = "alpha")

    RenderHexagon(
        props = RenderProps(animX, animY, animSize, animAlpha),
        node = node,
        slots = slots,
        isSmileMenuVisible = isSmileMenuVisible, // Передаем стейт в рендерер (СТАЛО)
        contentColor = contentColor,
        onClick = { onClick(node) },
        onLongClick = if (onLongClick != null) { { onLongClick(node) } } else null,
        modifier = externalModifier
    )
}

@Composable
fun MotionLabel(props: RenderProps, node: HexNode, color: Color, entranceAlpha: Float, isDragging: Boolean) {
    val isOffscreen = props.x <= -20 || props.y >= 2400
    val stiffness = if (isDragging) Spring.StiffnessHigh else Spring.StiffnessLow

    val animX by animateIntAsState(
        targetValue = props.x,
        animationSpec = if (isOffscreen) spring(stiffness = Spring.StiffnessVeryLow) else spring(stiffness = stiffness),
        label = "tx"
    )
    val animY by animateIntAsState(
        targetValue = props.y,
        animationSpec = if (isOffscreen) spring(stiffness = Spring.StiffnessVeryLow) else spring(stiffness = stiffness),
        label = "ty"
    )
    val targetAlpha = props.alpha * entranceAlpha
    val animAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (isOffscreen) tween(0) else spring(stiffness = stiffness),
        label = "tAlpha"
    )

    if (animAlpha > 0.05f) {
        val density = LocalDensity.current
        val (xDp, yDp) = with(density) {
            val scaledX = ScreenMetrics.scaleX(animX)
            val scaledY = ScreenMetrics.scaleY(animY)
            scaledX.toDp() to scaledY.toDp()
        }

        // Вычисляем имя и неоновый серый суб-индекс (если соту переименовали)
        val mainText = node.name
        val subText = if (node.name != node.id) "#${node.id}" else null

        Box(modifier = Modifier.offset(x = xDp, y = yDp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // --- ГРУППА 1: ЧЕЛОВЕЧЕСКОЕ ИМЯ СОТЫ (СТАЛО) ---
                Box {
                    // Слой 1: Черный контур
                    Text(
                        text = mainText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, // Применили моноширинный шрифт к контуру (СТАЛО)
                        letterSpacing = -2.sp,
                        style = TextStyle(
                            color = Color.Black.copy(alpha = animAlpha * 0.8f),
                            drawStyle = Stroke(
                                miter = 10f,
                                width = 4f,
                                join = StrokeJoin.Round
                            )
                        )
                    )

                    // Слой 2: Основной белый текст
                    Text(
                        text = mainText,
                        color = Color.White.copy(alpha = animAlpha),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, // Применили моноширинный шрифт к тексту (СТАЛО)
                        letterSpacing = -2.sp
                    )
                }

                // --- ГРУППА 2: СИСТЕМНЫЙ ИНДЕКС-АДРЕС (СТАЛО) ---
                if (subText != null) {
                    Spacer(modifier = Modifier.height(2.dp)) // Небольшой отступ вниз под имя соты
                    Box {
                        // Слой 1: Черный контур для индекса
                        Text(
                            text = subText,
                            fontSize = 11.sp, // Мелкий невзрачный шрифт
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            style = TextStyle(
                                color = Color.Black.copy(alpha = animAlpha * 0.7f),
                                drawStyle = Stroke(
                                    miter = 10f,
                                    width = 3f, // Тоньше обрис для мелкого текста
                                    join = StrokeJoin.Round
                                )
                            )
                        )

                        // Слой 2: Основной тускло-серый текст
                        Text(
                            text = subText,
                            color = Color.LightGray.copy(alpha = animAlpha * 0.5f), // Слегка приглушенный неоновый серый
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RenderHexagon(
    props: RenderProps,
    node: HexNode,
    slots: List<SlotEntity>,
    isSmileMenuVisible: Boolean = false, // Принимаем стейт с дефолтным значением для совместимости (СТАЛО)
    contentColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    scaleFactor: Float = 1f,
    alphaFactor: Float = 1f,
    modifier: Modifier = Modifier
) {
    // --- НАСТРОЙКИ ТЕНИ (ТВОИ КРУТИЛКИ) ---
    val shadowBlur = 5.dp          // 1. Рассеивание (чем больше, тем мягче)
    val shadowOffset = 5.dp       // 2. Расстояние (насколько "высоко" парит сота)
    val shadowOpacity = 0.15f       // 3. Прозрачность (0.0 - нет тени, 1.0 - черная)
    // --------------------------------------

    val finalSize = props.size * scaleFactor
    val finalAlpha = props.alpha * alphaFactor
    val density = LocalDensity.current

    val (xDp, yDp, sizeDp) = with(density) {
        val scaledCenterX = ScreenMetrics.scaleX(props.x)
        val scaledCenterY = ScreenMetrics.scaleY(props.y)
        val scaledSize = ScreenMetrics.scaleSize(finalSize)
        val x = (scaledCenterX - scaledSize / 2).toDp()

        // В реальном времени считываем текущую высоту клавиатуры в DP
        val keyboardPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

        // В реальном времени рассчитываем сдвиг на высоту меню смайликов (250 DP) (СТАЛО)
        val smilePadding = if (isSmileMenuVisible) 250.dp else 0.dp

        // Вычитаем и высоту клавиатуры, и высоту смайлов из координаты соты (СТАЛО)
        val y = (scaledCenterY - scaledSize / 2).toDp() - keyboardPadding - smilePadding

        Triple(x, y, scaledSize.toDp())
    }

    // Ищем соответствующий этому гексагону слот ячейки из базы данных (СТАЛО)
    val correspondingSlot = remember(node.id, slots) { slots.find { it.id == node.id } }

    Box(modifier = Modifier.offset(x = xDp, y = yDp).size(sizeDp)) {

        // --- СЛОЙ ТЕНИ (РИСУЕТСЯ ПОД СОТОЙ) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 2. Управляем расстоянием (сдвигаем тень вниз и вправо)
                .offset(x = shadowOffset / 2, y = shadowOffset)
                // 1. Управляем рассеиванием
                .blur(shadowBlur)
                // 3. Управляем прозрачностью и формой
                .alpha(shadowOpacity * finalAlpha)
                .background(Color.Black, RoundedHexagonShape)
        )

        // --- ОСНОВНАЯ СОТА (ТВОЁ СТЕКЛО) ---
        val glassFill = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
        )
        val thicknessBorder = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.9f), Color.Black.copy(alpha = 0.2f))
        )
        val innerGlow = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
            start = Offset.Zero, end = Offset(sizeDp.value * 2, sizeDp.value * 2)
        )

        val interactionSource = remember { MutableInteractionSource() }
        val visualModifier = Modifier
            .fillMaxSize() // Теперь заполняем родительский Box
            .then(modifier)
            .alpha(finalAlpha)
            .clip(RoundedHexagonShape)
            .border(2.dp, thicknessBorder, RoundedHexagonShape)
            .background(glassFill)
            .background(innerGlow)

        val isClickable = node.isAddButton || node.nodeType != HexNodeType.CHANNEL
        val gestureModifier = if (isClickable) {
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick,
                onLongClick = onLongClick
            )
        } else Modifier

        Box(modifier = visualModifier.then(gestureModifier), contentAlignment = Alignment.Center) {
            // ... ВНУТРЕННОСТИ СОТЫ (Image, Text и т.д. без изменений) ...
            when {
                node.photoId != null -> {
                    Image(
                        painter = painterResource(node.photoId!!),
                        contentDescription = null,
                        contentScale = if (node.id == "1") ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().padding(if (node.id == "1") 28.dp else 0.dp).alpha(0.85f)
                    )
                }
                node.emoji != null -> Text(node.emoji!!, fontSize = 50.sp)
                else -> {
                    if (node.isAddButton) {
                        Text(
                            text = "+",
                            // Сделаем цвет максимально ярким (alpha = 1.0f)
                            color = contentColor.copy(alpha = 0.99f),
                            fontSize = 42.sp, // Чуть увеличим
                            // Меняем ExtraLight на Normal или Medium, чтобы плюс стал заметнее
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(y = (-2).dp) // Легкая коррекция по вертикали для центровки
                        )
                    } else {
                        when (node.nodeType) {
                            HexNodeType.CHAT -> {
                                // === ОТРИСОВКА АВАТАРКИ СОБЕСЕДНИКА ВМЕСТО "💬" (СТАЛО) ===
                                if (correspondingSlot?.partnerAvatarBase64 != null) {
                                    val bitmap = remember(correspondingSlot.partnerAvatarBase64) {
                                        try {
                                            val bytes = android.util.Base64.decode(correspondingSlot.partnerAvatarBase64, android.util.Base64.NO_WRAP)
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                        } catch (e: Exception) { null }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedHexagonShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text("💬", fontSize = 38.sp)
                                    }
                                } else {
                                    Text("💬", fontSize = 38.sp)
                                }
                            }
                            HexNodeType.CHANNEL -> Text("🔒", fontSize = 38.sp)
                            else -> {}
                        }
                    }
                }
            }
        }

        // === КРАСНЫЙ НЕОНОВЫЙ МАЯЧОК ПИНГА (РИСУЕТСЯ ПОВЕРХ ВСЕГО) (СТАЛО) ===
        if (correspondingSlot != null && correspondingSlot.hasActivePing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp) // Пропорциональное смещение в правый верхний угол
                    .size(16.dp)
                    .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                    .border(2.dp, Color(0xFF111111), androidx.compose.foundation.shape.CircleShape)
                    .zIndex(10f)
            )
        }
    }
}