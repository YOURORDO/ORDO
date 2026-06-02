package com.example.ordo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import kotlinx.coroutines.delay
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HexagonNavigationScreen(
    rootNode: HexNode,
    branch12Manager: Branch12State,
    slots: List<SlotEntity>,
    connectionStatus: String,
    serverMessage: String,
    messageTrigger: Long,
    onChatNodeClick: (HexNode) -> Unit,
    onBack: () -> Unit
) {
    var activeNode by remember { mutableStateOf(rootNode) }
    var isStarted by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    var isSettingsMode by remember { mutableStateOf(false) }


    val context = androidx.compose.ui.platform.LocalContext.current
    var isSynthesisEnabled by remember { mutableStateOf(AppSettings.isSynthesisEnabled(context)) }

    // --- НАБЛЮДАТЕЛЬ ЗА ФОНОВЫМ РЕЖИМОМ ДЛЯ ВИБРАЦИИ (СТАЛО) ---
    val lifecycleOwnerForHaptic = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwnerForHaptic) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    isAppInForeground = false
                }
                else -> {}
            }
        }
        lifecycleOwnerForHaptic.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwnerForHaptic.lifecycle.removeObserver(observer)
        }
    }


    var nameInput by remember { mutableStateOf(IdentityManager.getMyName(context)) }
    var avatarBase64 by remember { mutableStateOf(IdentityManager.getMyAvatar(context)) }
    var isBackgroundEnabled by remember { mutableStateOf(AppSettings.isBackgroundModeEnabled(context)) }
    var selectedBgServer by remember { mutableStateOf(AppSettings.getBackgroundServer(context)) }
    var expandedServerDropdown by remember { mutableStateOf(false) }

    // Объявляем глобальное состояние смайликов на уровне всего экрана
    var isSmileMenuVisible by remember { mutableStateOf(false) }

    // Объявляем состояние языка приложения и синхронизируем с L10n (СТАЛО)
    var appLanguage by remember { mutableStateOf(AppSettings.getLanguage(context)) }
    L10n.currentLanguage = appLanguage

    // Лаунчер для выбора аватарки из галереи устройства
    val galleryLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { avatarBase64 = uriToBase64(context, it) }
    }

    // Лаунчер для экспорта резервной копии базы данных (чатов)
    val exportDbLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    DatabaseManager.closeDatabase() // Закрываем базу, чтобы все данные слились в один файл (WAL -> DB)
                    val dbFile = context.getDatabasePath("poryadok_vault.db")
                    context.contentResolver.openOutputStream(it)?.use { outStream ->
                        dbFile.inputStream().use { inStream -> inStream.copyTo(outStream) }
                    }
                    DatabaseManager.init(context) // Снова запускаем базу для работы
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Архив чатов сохранен!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Ошибка сохранения!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // Лаунчер для импорта резервной копии базы данных (чатов)
    val importDbLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    DatabaseManager.closeDatabase()
                    val dbFile = context.getDatabasePath("poryadok_vault.db")

                    // Уничтожаем временные файлы (WAL и SHM), чтобы они не сломали нашу новую базу
                    java.io.File(dbFile.path + "-wal").delete()
                    java.io.File(dbFile.path + "-shm").delete()

                    // Записываем выбранный юзером файл поверх нашей базы
                    context.contentResolver.openInputStream(it)?.use { inStream ->
                        dbFile.outputStream().use { outStream -> inStream.copyTo(outStream) }
                    }

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Архив загружен! Приложение перезапустится...",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        kotlinx.coroutines.delay(2000)
                        kotlin.system.exitProcess(0)
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Ошибка загрузки!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    // ===============================================================


    val isDeletable = remember(activeNode) {
        activeNode.id != "1" && activeNode.id != "1.1" && activeNode.id != "1.2" && activeNode.id != "1.3" && activeNode.id != "1.4"
    }

    // Триггер показа диалога предупреждения перед удалением
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // --- ТЕМА ---
    var isDarkTheme by remember { mutableStateOf(ThemeSettings.isDark(context)) }
    val themeContentColor = animateColorAsState(
        targetValue = if (isDarkTheme) Color.White else TextDark,
        animationSpec = tween(500), label = "contentColor"
    ).value

    val stripeColor1 = if (isDarkTheme) GrayLight else Color(0xFFafafaf)
    val stripeColor2 = if (isDarkTheme) GrayDark else Color.White
    val restingColor = if (isDarkTheme) GrayLight else Color.White

    var backgroundToggle by remember { mutableStateOf(false) }
    val scrollOffsets = remember { mutableStateMapOf<String, Float>() }
    val scrollAnimatable = remember { Animatable(0f) }

    var activeNodeDragOffset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isDraggingChild by remember { mutableStateOf(false) }

    val view = LocalView.current
    val focusManager =
        androidx.compose.ui.platform.LocalFocusManager.current // Добавили менеджер фокуса
    var lastHapticIndex by remember { mutableIntStateOf(0) }

    var textEntranceAlpha by remember { mutableStateOf(0f) }

    var bgNumber by remember {
        mutableIntStateOf(DailyBackgroundProvider.getIndexForToday())
    }
    LaunchedEffect(Unit) {
        while (true) {
            val freshIdx = DailyBackgroundProvider.getIndexForToday()
            if (bgNumber != freshIdx) {
                // Опа, время пришло!
                bgNumber = freshIdx
            }

            kotlinx.coroutines.delay(60000)
        }
    }


    LaunchedEffect(rootNode) {
        val freshActiveNode = findNodeById(rootNode, activeNode.id)
        if (freshActiveNode != null) {
            activeNode = freshActiveNode
        }
    }


    var previousActiveNodeId by remember { mutableStateOf(activeNode.id) }


    // Синхронизируем выход из чата и автоудаление рупора при любом способе навигации
    LaunchedEffect(activeNode) {
        // Мгновенно и надежно сбрасываем барабан прокрутки в ноль при любом переходе (СТАЛО)
        scrollAnimatable.snapTo(0f)

        // Гарантированно закрываем смайлики при любом переключении папок/чатов (СТАЛО)
        isSmileMenuVisible = false

        // Если мы только что вышли из рупора 1.4 и мы не Творец — стираем ячейку-призрак из БД


        if (previousActiveNodeId == "1.4" && activeNode.id != "1.4" && !IdentityManager.isSystemInfo()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    DatabaseManager.getDao().deleteSlotById("1.4")
                } catch (e: Exception) {
                    android.util.Log.e(
                        "HEX_NAV",
                        "Ошибка автоудаления инфо при выходе: ${e.message}"
                    )
                }
            }
        }


        previousActiveNodeId = activeNode.id

        if (activeNode.nodeType != HexNodeType.CHAT) {
            onBack()
        }
    }


    LaunchedEffect(activeNode, isStarted) {
        if (isStarted) {
            textEntranceAlpha = 0f
            delay(150)
            val steps = 10
            for (i in 1..steps) {
                textEntranceAlpha = i / steps.toFloat()
                delay(16)
            }
        } else {
            textEntranceAlpha = 0f
        }
    }


    val isChatMode = remember(activeNode) {
        activeNode.nodeType == HexNodeType.CHAT || activeNode.id.startsWith("1.1.2.")
    }

    // --- СИНХРОНИЗАЦИЯ ИМПУЛЬСА И ВИБРАЦИИ (HARD SYNC) ---
    // Добавили переменную isAppInForeground как ключ перезапуска корутины (СТАЛО)
    LaunchedEffect(isStarted, isChatMode, isSynthesisEnabled, isAppInForeground) {
        // Запускаем цикл вибрации только если приложение активно на экране (СТАЛО)
        if (isStarted && !isChatMode && isSynthesisEnabled && isAppInForeground) {
            val totalCycle =
                (HexThemeConfig.PULSE_SPEED_MS + HexThemeConfig.PULSE_PAUSE_LONG_MS).toLong()

            // --- ТВОЯ КРУТИЛКА ( latencyComp ) ---
            // Если вибрация отстает — увеличивай (70, 80, 100).
            // Если вибрация спешит — уменьшай (30, 40, 0).
            val latencyComp = 60L

            while (true) {
                val currentTime = System.currentTimeMillis()
                val msPassed = currentTime % totalCycle


                var waitTime = totalCycle - msPassed - latencyComp
                if (waitTime < 0) waitTime += totalCycle // Защита от отрицательных чисел

                delay(waitTime)


                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                delay(HexThemeConfig.PULSE_PAUSE_SHORT_MS.toLong())


                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)


                delay(100)
            }
        }
    }

    // ---- ГИРОСКОП ----
    val gyroController = rememberGyroScrollController()


    LaunchedEffect(
        gyroController.tiltFactor,
        activeNode,
        isSynthesisEnabled
    ) { // <--- Добавили зависимость
        val speed = 0.45f
        gyroController.tiltFactor.collect { tilt ->

            if (isSynthesisEnabled && !isDraggingChild && abs(tilt) > 0.05f) {
                val maxScroll = (activeNode.children.size - 3).coerceAtLeast(0).toFloat()
                val delta = tilt * speed
                val newValue = (scrollAnimatable.value + delta).coerceIn(0f, maxScroll)
                scrollAnimatable.snapTo(newValue)
                scrollOffsets[activeNode.id] = newValue
            }
        }
    }


    val currentBgColor = if (isChatMode) Color(0xFF0A0A0A) else restingColor

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(currentBgColor)) {
        val screenHeight = maxHeight
        val stripeHeight = screenHeight / 11


        // Фоновый слой (Картинка дня или реальный зашифрованный чат / рупор)
        if (isChatMode) {
            if (activeNode.id == "1.4") {
                // Магия Рупора 1.4: Мокаем системный канал вещания прямо в памяти телефона!
                val systemSlot = SlotEntity(
                    id = "1.4",
                    status = "ACTIVE",
                    partnerDisplayName = "СИСТЕМА ORDO",
                    partnerId = "ALL"
                )
                ChatInterface(
                    slot = systemSlot,
                    status = connectionStatus,
                    serverMessage = serverMessage,
                    messageTrigger = messageTrigger,
                    isSmileMenuVisible = isSmileMenuVisible, // Пробрасываем стейт смайликов (СТАЛО)
                    onSmileMenuVisibleChange = { isSmileMenuVisible = it }, // Передаем коллбэк (СТАЛО)
                    onBack = {
                        isStarted = true
                        isSmileMenuVisible = false // Сбрасываем смайлы при выходе (СТАЛО)
                        activeNode = activeNode.parent ?: rootNode
                    }
                )
            } else {
                // Обычные пользовательские чаты
                val currentSlot = slots.find { it.id == activeNode.id }
                if (currentSlot != null) {
                    SlotDetailContainer(
                        slot = currentSlot,
                        status = connectionStatus,
                        serverMessage = serverMessage,
                        messageTrigger = messageTrigger,
                        isSmileMenuVisible = isSmileMenuVisible, // Пробрасываем стейт смайликов (СТАЛО)
                        onSmileMenuVisibleChange = { isSmileMenuVisible = it }, // Передаем коллбэк (СТАЛО)
                        onBack = {
                            isStarted = true
                            isSmileMenuVisible = false // Сбрасываем смайлы при выходе (СТАЛО)
                            activeNode = activeNode.parent ?: rootNode
                        }
                    )
                } else {
                    // Если ячейка еще создается в БД, временно показываем темный фон
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)))
                }
            }
        } else {
            DailyBackground(
                isStarted = isStarted,
                isDarkTheme = isDarkTheme,
                dragProgress = dragProgress,
                bgNumber = bgNumber,
                onManualCleanup = {}
            )
        }

        // Летающий заголовок (рисуется только когда мы НЕ находимся в режиме чата)
        if (!isChatMode) {
            val baseName = if (activeNode.name.isEmpty()) "ORDO" else activeNode.name
            val titleText = if (isSettingsMode) "$baseName Settings" else baseName
            val shouldBeAtTop = isStarted || (activeNode.id != "1")

            FlyingTitle(
                shouldBeAtTop = shouldBeAtTop,
                text = titleText,
                textColor = themeContentColor,
                stripeHeight = stripeHeight,
                screenHeight = screenHeight,
                onStartClick = {
                    if (!isStarted) {
                        isStarted = true
                        isSettingsMode = false
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        backgroundToggle = !backgroundToggle
                    }
                }
            )
        }

        // Прозрачный слой для сворачивания по пустому тапу (активен только когда соты развернуты)
        if (isStarted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (!isDraggingChild) {
                                    branch12Manager.closeMenu()
                                    isStarted = false
                                    isSettingsMode = false
                                    backgroundToggle = !backgroundToggle
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                            }
                        )
                    }
            )
        }

        // Слой с сотами (поверх прозрачного Box)
        val nodesToRender = remember(activeNode, isStarted, isChatMode, activeNode.children) {
            val list = mutableSetOf<HexNode>()
            list.add(rootNode)
            list.addAll(rootNode.children)
            list.add(activeNode)

            // --- ИЗМЕНЯЕМ ЗДЕСЬ ---
            // Если мы в режиме чата, убираем из детей активного узла кнопку "+"
            val filteredChildren = if (isChatMode) {
                activeNode.children.filter { !it.isAddButton }
            } else {
                activeNode.children
            }
            list.addAll(filteredChildren)
            // ----------------------

            if (isStarted) {
                if (isChatMode) {
                    activeNode.parent?.let { list.add(it) }
                } else {
                    activeNode.parent?.let { list.add(it) }
                    activeNode.parent?.children?.let { list.addAll(it) }
                }
            }

            // ... остальная сортировка списка (list.toList().sortedWith...)
            list.toList().sortedWith(Comparator { a, b ->
                when {
                    a.level != b.level -> a.level.compareTo(b.level)
                    a == activeNode -> 1
                    b == activeNode -> -1
                    else -> a.id.compareTo(b.id)
                }
            })
        }

        nodesToRender.forEach { node ->
            key(node.id) {
                val targetProps = calculateTargetProps(
                    node = node,
                    activeNode = activeNode,
                    rootNode = rootNode,
                    isStarted = isStarted,
                    currentScroll = scrollAnimatable.value,
                    dragOffset = activeNodeDragOffset,
                    isDragging = isDragging
                )

                val textProps = calculateTextProps(
                    node = node,
                    activeNode = activeNode,
                    currentScroll = scrollAnimatable.value
                )

                val isChild = activeNode.children.contains(node)
                val isActive = node == activeNode

                val nodeGestureModifier = Modifier.pointerInput(activeNode.id, node.id) {
                    if (!isChild && !isActive) return@pointerInput

                    val activeCenterX = 320f
                    val activeCenterY = 2160f
                    val touchSlop = 60f

                    var accumulatedDragX = 0f
                    var accumulatedDragY = 0f
                    var gestureModeResolved = false
                    var isSelectionMode = false
                    var triggerAction = false
                    var startNodeX = 0f
                    var startNodeY = 0f
                    var pendingTargetNode: HexNode? = null
                    var hasVibratedForWall = false

                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            if (isChild) isDraggingChild = true
                            accumulatedDragX = 0f
                            accumulatedDragY = 0f
                            if (isActive) activeNodeDragOffset = Offset.Zero
                            gestureModeResolved = false
                            isSelectionMode = false
                            triggerAction = false
                            pendingTargetNode = null
                            hasVibratedForWall = false
                            val currentProps = calculateTargetProps(
                                node = node,
                                activeNode = activeNode,
                                rootNode = rootNode,
                                isStarted = isStarted,
                                currentScroll = scrollAnimatable.value,
                                dragOffset = Offset.Zero,
                                isDragging = true
                            )
                            startNodeX = currentProps.x.toFloat()
                            startNodeY = currentProps.y.toFloat()
                        },
                        onDragEnd = {
                            isDragging = false
                            if (isChild) isDraggingChild = false
                            dragProgress = 0f

                            // ПЕРЕХОД К РОДИТЕЛЮ/БРАТУ (ОТПУСКАНИЕ)
                            if (isActive && pendingTargetNode != null) {
                                if (branch12Manager.activeFlyoutNodeId == null) {
                                    val target =
                                        pendingTargetNode!! // Чтобы не писать !! каждый раз

                                    if (target.isAddButton) {
                                        // Если бросили на ПЛЮС — вызываем меню
                                        val parent = target.parent
                                        if (parent != null) {
                                            if (isBranch12Node(parent)) {
                                                val slotIndex = getPlusSlotIndex(target)
                                                branch12Manager.currentPlusSlotIndex = slotIndex
                                                branch12Manager.toggleMenu(target.id, slotIndex)
                                            } else {
                                                addNewChildTo(parent)
                                            }
                                        }
                                        activeNodeDragOffset =
                                            Offset.Zero // Возвращаем активную соту в центр
                                    } else {

                                        if (target.nodeType == HexNodeType.CHAT) {
                                            // Запускаем анимацию сворачивания сот и открываем реальный чат!
                                            activeNode = target
                                            isStarted = false
                                            isSettingsMode = false
                                            backgroundToggle = !backgroundToggle
                                            onChatNodeClick(target)
                                        } else {
                                            activeNodeDragOffset = Offset.Zero
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            activeNode = target
                                            backgroundToggle = !backgroundToggle
                                        }
                                    }
                                }
                                pendingTargetNode = null
                            }

                            // Всегда сбрасываем офсет в 0 (возврат в центр)
                            activeNodeDragOffset = Offset.Zero

                            if (!triggerAction && isChild) {
                                val targetValue = scrollAnimatable.value.roundToInt().toFloat()
                                val maxScroll =
                                    (activeNode.children.size - 3).coerceAtLeast(0).toFloat()
                                scope.launch {
                                    scrollAnimatable.animateTo(
                                        targetValue = targetValue.coerceIn(0f, maxScroll),
                                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                                    )
                                    scrollOffsets[activeNode.id] = scrollAnimatable.value
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            if (isChild) isDraggingChild = false
                            activeNodeDragOffset = Offset.Zero
                            pendingTargetNode = null
                            dragProgress = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val rawDragX = ScreenMetrics.unscale(dragAmount.x)
                        val rawDragY = ScreenMetrics.unscale(dragAmount.y)

                        accumulatedDragX += rawDragX
                        accumulatedDragY += rawDragY

                        // 1. ОПРЕДЕЛЯЕМ РЕЖИМ (Свайп в центр или Скролл барабана)
                        if (isChild && !isChatMode && !gestureModeResolved && !triggerAction) {
                            val dragDistance = hypot(accumulatedDragX, accumulatedDragY)
                            if (dragDistance > touchSlop) {
                                val vecToCenterX = activeCenterX - startNodeX
                                val vecToCenterY = activeCenterY - startNodeY
                                val distToCenter = hypot(vecToCenterX, vecToCenterY)
                                val dirToCenterX =
                                    if (distToCenter > 0) vecToCenterX / distToCenter else 0f
                                val dirToCenterY =
                                    if (distToCenter > 0) vecToCenterY / distToCenter else 0f
                                val dragDirX = accumulatedDragX / dragDistance
                                val dragDirY = accumulatedDragY / dragDistance
                                val dotProduct =
                                    (dragDirX * dirToCenterX) + (dragDirY * dirToCenterY)

                                isSelectionMode = dotProduct > 0.82f
                                gestureModeResolved = true
                            }
                        }

                        // 2. ЕСЛИ ЭТО ЗАШЛА КНОПКА (isChild)
                        if (isChild && !isChatMode && gestureModeResolved && isSelectionMode && !triggerAction) {
                            val vectorToCenterX = activeCenterX - startNodeX
                            val vectorToCenterY = activeCenterY - startNodeY
                            val axisX = vectorToCenterX / hypot(vectorToCenterX, vectorToCenterY)
                            val axisY = vectorToCenterY / hypot(vectorToCenterX, vectorToCenterY)

                            // ВОТ ЭТА ПЕРЕМЕННАЯ, КОТОРАЯ ТЕРЯЛАСЬ:
                            val currentProgress =
                                (accumulatedDragX * axisX) + (accumulatedDragY * axisY)

                            if (currentProgress > 150f) {
                                if (branch12Manager.activeFlyoutNodeId == null) {
                                    triggerAction = true
                                    isDragging = false
                                    if (isChild) isDraggingChild = false
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                                    // ПРОВЕРКА: Если свайпнули ПЛЮС
                                    if (node.isAddButton) {
                                        val parent = node.parent
                                        if (parent != null) {
                                            if (isBranch12Node(parent)) {
                                                val slotIndex = getPlusSlotIndex(node)
                                                branch12Manager.currentPlusSlotIndex = slotIndex
                                                branch12Manager.toggleMenu(node.id, slotIndex)
                                            } else {
                                                addNewChildTo(parent)
                                            }
                                        }
                                    } else {
                                        // Если свайпнули обычную соту — проверяем, не чат ли это
                                        if (node != activeNode) {
                                            if (node.nodeType == HexNodeType.CHAT) {
                                                // Запускаем анимацию сворачивания сот и открываем реальный чат!
                                                activeNode = node
                                                isStarted = false
                                                isSettingsMode = false
                                                backgroundToggle = !backgroundToggle
                                                onChatNodeClick(node)
                                            } else {
                                                activeNode = node
                                                backgroundToggle = !backgroundToggle
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (isChild && !isChatMode && gestureModeResolved && !isSelectionMode && !triggerAction) {
                            // Логика обычного скролла барабана (оставляем как была)
                            val pixelsPerUnit = 350f
                            val delta = -rawDragX
                            scope.launch {
                                var newValue = scrollAnimatable.value + (delta / pixelsPerUnit)
                                val maxScroll =
                                    (activeNode.children.size - 3).coerceAtLeast(0).toFloat()
                                if (newValue < 0f || newValue > maxScroll) {
                                    newValue = scrollAnimatable.value + (delta / pixelsPerUnit / 4f)
                                }
                                scrollAnimatable.snapTo(newValue)
                                val currentIndex = newValue.roundToInt()
                                if (currentIndex != lastHapticIndex) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    lastHapticIndex = currentIndex
                                    backgroundToggle = !backgroundToggle
                                }
                            }
                        }

                        // 3. ЛОГИКА ДВИЖЕНИЯ АКТИВНОЙ СОТЫ (isActive)
                        if (isActive && !triggerAction) {
                            val currentDist = activeNodeDragOffset.getDistance()
                            val resistance = 1f / (1f + (currentDist / 350f))
                            var nextOffset = activeNodeDragOffset + Offset(
                                rawDragX * resistance * 0.35f,
                                rawDragY * resistance * 0.35f
                            )

                            val totalDragX = nextOffset.x
                            val totalDragY = nextOffset.y
                            val dist = hypot(totalDragX, totalDragY)
                            dragProgress = (dist / 500f).coerceIn(0f, 1f)

                            var foundTarget: HexNode? = null
                            val dragDirX = if (dist > 0) totalDragX / dist else 0f
                            val dragDirY = if (dist > 0) totalDragY / dist else 0f

                            fun checkCorridor(targetX: Float, targetY: Float): Boolean {
                                val toTargetX = targetX - activeCenterX
                                val toTargetY = targetY - activeCenterY
                                val len = hypot(toTargetX, toTargetY)
                                val dot =
                                    dragDirX * (toTargetX / len) + dragDirY * (toTargetY / len)
                                return dot > 0.85f
                            }

                            if (dist > 50f) {
                                // ... тут твои проверки коридоров (checkCorridor) ...
                                if (checkCorridor(-27f, 2160f)) {
                                    if (activeNode.parent != null) foundTarget = activeNode.parent
                                    else if (activeNode == rootNode) foundTarget = rootNode
                                } else if (!isChatMode) {
                                    if (checkCorridor(673f, 2160f)) {
                                        val idx = scrollAnimatable.value.toInt() + 2
                                        if (idx in activeNode.children.indices) foundTarget =
                                            activeNode.children[idx]
                                    } else if (checkCorridor(150f, 1850f)) {
                                        val idx = scrollAnimatable.value.toInt()
                                        if (idx in activeNode.children.indices) foundTarget =
                                            activeNode.children[idx]
                                    } else if (checkCorridor(493f, 1850f)) {
                                        val idx = scrollAnimatable.value.toInt() + 1
                                        if (idx in activeNode.children.indices) foundTarget =
                                            activeNode.children[idx]
                                    }
                                }
                            }

                            if (foundTarget != null) {
                                pendingTargetNode = foundTarget
                                if (dist >= 140f) {
                                    val ratio = 140f / dist
                                    nextOffset = Offset(totalDragX * ratio, totalDragY * ratio)
                                    if (!hasVibratedForWall) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        hasVibratedForWall = true
                                    }
                                } else {
                                    hasVibratedForWall = false
                                }
                            } else {
                                pendingTargetNode = null
                                hasVibratedForWall = false
                            }
                            activeNodeDragOffset = nextOffset
                        }
                    }
                }

                val longClickAction: ((HexNode) -> Unit)? = if (isStarted && node == activeNode) {
                    { _ ->
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        isStarted = false
                        isSettingsMode = true
                        backgroundToggle = !backgroundToggle
                    }
                } else null

                MotionHexagon(
                    props = targetProps,
                    node = node,
                    slots = slots,
                    isSmileMenuVisible = isSmileMenuVisible, // Передаем состояние смайликов в гексагоны (СТАЛО)
                    isDragging = isDragging,
                    contentColor = themeContentColor,
                    externalModifier = nodeGestureModifier,
                    onClick = { clickedNode ->
                        // Закрываем меню, если тыкаем в другую соту
                        if (branch12Manager.activeFlyoutNodeId != null && clickedNode.id != branch12Manager.activeFlyoutNodeId) {
                            branch12Manager.closeMenu()
                            return@MotionHexagon
                        }

                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                        if (clickedNode.isAddButton) {
                            val parent = clickedNode.parent
                            if (parent != null) {
                                if (isBranch12Node(parent)) {
                                    val slotIndex = getPlusSlotIndex(clickedNode)
                                    branch12Manager.currentPlusSlotIndex = slotIndex
                                    branch12Manager.toggleMenu(clickedNode.id, slotIndex)
                                } else {
                                    // Надежно извлекаем целевой индекс пропуска для обычных папок
                                    val parentParts = parent.id.split(".")
                                    val plusParts = clickedNode.id.split(".")
                                    val forcedIndex = if (plusParts.size == parentParts.size + 2) {
                                        plusParts[parentParts.size].toIntOrNull()
                                    } else null

                                    addNewChildTo(parent, forcedIndex = forcedIndex)
                                }
                            }
                        } else {
                            branch12Manager.closeMenu()
                            when (clickedNode.nodeType) {
                                HexNodeType.STANDARD -> {
                                    if (clickedNode.id == "1" && !isStarted) {
                                        isStarted = true
                                        isSettingsMode = false
                                        backgroundToggle = !backgroundToggle
                                    } else if (clickedNode.id != activeNode.id) {
                                        activeNode = clickedNode
                                        isSettingsMode = false
                                        backgroundToggle = !backgroundToggle
                                    }
                                }

                                HexNodeType.CHAT -> {
                                    // Запускаем анимацию сворачивания сот и открываем реальный чат!
                                    activeNode = clickedNode
                                    isStarted = false
                                    isSettingsMode = false
                                    backgroundToggle = !backgroundToggle
                                    onChatNodeClick(clickedNode)
                                }

                                else -> {}
                            }
                        }

                    },
                    onLongClick = longClickAction
                )

                if (isChild && textProps.alpha > 0 && !isChatMode && !node.isAddButton) {
                    MotionLabel(
                        props = textProps,
                        node = node, // Передаем весь объект соты для вывода индекса под именем
                        color = themeContentColor,
                        entranceAlpha = textEntranceAlpha,
                        isDragging = isDragging
                    )
                }
            }
        }
        // --- МЕНЮ ВЫЛЕТА (Branch 1.2) ---
        if (isBranch12Node(activeNode)) {
            Branch12FlyoutMenu(
                state = branch12Manager,
                activeNode = activeNode,
                plusSlotIndex = branch12Manager.currentPlusSlotIndex,
                contentColor = themeContentColor,
                onOptionSelected = { type ->
                    // Безопасно вычисляем целевой индекс пропуска из ID активного плюса
                    val clickedPlusId = branch12Manager.activeFlyoutNodeId
                    val forcedIndex = if (clickedPlusId != null) {
                        val parentParts = activeNode.id.split(".")
                        val plusParts = clickedPlusId.split(".")
                        if (plusParts.size == parentParts.size + 2) {
                            plusParts[parentParts.size].toIntOrNull()
                        } else null
                    } else null

                    when (type) {
                        HexNodeType.CHAT -> {
                            // 1. Создаем узел с типом ЧАТ на месте целевого индекса пропуска
                            addNewChildTo(activeNode, HexNodeType.CHAT, forcedIndex)
                            // 2. Закрываем меню выбора
                            branch12Manager.closeMenu()
                            // 3. Даем обычный виброотклик
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }

                        HexNodeType.STANDARD -> {
                            // Создаем обычный узел на месте целевого индекса пропуска
                            addNewChildTo(activeNode, HexNodeType.STANDARD, forcedIndex)
                            branch12Manager.closeMenu()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }

                        HexNodeType.CHANNEL -> {
                            // Создаем канал на месте целевого индекса пропуска
                            addNewChildTo(activeNode, HexNodeType.CHANNEL, forcedIndex)
                            branch12Manager.closeMenu()
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    }
                }
            )
        }

        // --- ПОЛНОЭКРАННЫЙ ПАНЕЛЬ НАСТРОЕК (ОБЪЕДИНЕННЫЙ ПУЛЬТ) ---
        AnimatedVisibility(
            visible = isSettingsMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Объявляем буфер имени соты с ключом ID
            var renameInput by remember(activeNode.id) { mutableStateOf(activeNode.name) }

            // Размытый полупрозрачный фон на весь экран, перехватывающий клики по лежащим сзади сотам
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F0F).copy(alpha = 0.95f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus() // Прячем клавиатуру при тапе в пустое место
                        })
                    }
                    .padding(top = stripeHeight, start = 24.dp, end = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Горизонтальный ряд: Кнопка Назад (слева) и Выбор языка (справа)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, // Разносит элементы по краям
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопка "< Назад" / "< Back" с безопасным сбросом несохраненных данных
                        Text(
                            text = "< ${L10n.back}",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.clickable {
                                focusManager.clearFocus() // Скрываем клавиатуру, если она открыта

                                // СБРАСЫВАЕМ все изменения в полях ввода на исходные значения из системы
                                nameInput = IdentityManager.getMyName(context)
                                avatarBase64 = IdentityManager.getMyAvatar(context)

                                // Закрываем настройки без сохранения на диск
                                isSettingsMode = false
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            }
                        )

                        // Текстовый переключатель языков ru / en (остался справа)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ru",
                                color = if (appLanguage == "ru") Color.Cyan else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = if (appLanguage == "ru") FontWeight.Bold else FontWeight.Normal,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.clickable {
                                    appLanguage = "ru" // Меняем локально напрямую!
                                    AppSettings.setLanguage(context, "ru")
                                }
                            )
                            Text(
                                text = " / ",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(
                                text = "en",
                                color = if (appLanguage == "en") Color.Cyan else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = if (appLanguage == "en") FontWeight.Bold else FontWeight.Normal,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.clickable {
                                    appLanguage = "en" // Меняем локально напрямую!
                                    AppSettings.setLanguage(context, "en")
                                }
                            )
                        }
                    }

                    // 1. ЗАГОЛОВОК И ОТОБРАЖЕНИЕ ID (ДНК) ПОЛЬЗОВАТЕЛЯ (СТАЛО)
                    Text(
                        text = L10n.mainSettingsTitle, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    val myId = IdentityManager.getShortId()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = L10n.yourId, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            myId,
                            color = Color.Cyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    // 2. АВАТАРКА ПРОФИЛЯ
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFF222222), RoundedHexagonShape)
                            .clip(RoundedHexagonShape)
                            .border(2.dp, themeContentColor.copy(alpha = 0.3f), RoundedHexagonShape)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBase64 != null) {
                            val bitmap = remember(avatarBase64) {
                                try {
                                    val bytes = android.util.Base64.decode(
                                        avatarBase64,
                                        android.util.Base64.NO_WRAP
                                    )
                                    android.graphics.BitmapFactory.decodeByteArray(
                                        bytes,
                                        0,
                                        bytes.size
                                    ).asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedHexagonShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.AddAPhoto,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // 3. ПОЛЕ РЕДАКТИРОВАНИЯ ИМЕНИ ПРОФИЛЯ (СТАЛО)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = {
                            Text(
                                text = L10n.yourName, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                                color = Color.Gray,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 15.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeContentColor,
                            unfocusedBorderColor = themeContentColor.copy(alpha = 0.4f),
                            focusedLabelColor = themeContentColor,
                            unfocusedLabelColor = Color.Gray
                        )
                    )

                    // 4. ПЕРЕИМЕНОВАНИЕ ТЕКУЩЕЙ СОТЫ (СТАЛО)
                    if (isDeletable) {
                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { text ->
                                renameInput = text
                            },
                            label = { Text(text = L10n.currentNodeName, color = Color.Gray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeContentColor,
                                unfocusedBorderColor = themeContentColor.copy(alpha = 0.4f),
                                focusedLabelColor = themeContentColor,
                                unfocusedLabelColor = Color.Gray
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    activeNode.name = renameInput
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            DatabaseManager.getNodeDao().updateNodeName(activeNode.id, renameInput)
                                        } catch (e: Exception) {
                                            Log.e("HEX_NAV", "Ошибка переименования: ${e.message}")
                                        }
                                    }
                                }
                            )
                        )
                    }

                    // 5. КНОПКИ БЫСТРЫХ СИСТЕМНЫХ НАСТРОЕК (ТЕМА, СИНТЕЗ, ОБОИ, КОРЗИНА)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Тема
                        ThemeToggleButton(
                            isDark = isDarkTheme,
                            color = themeContentColor,
                            onToggle = {
                                // Переключаем значение в SharedPreferences и синхронизируем стейт
                                ThemeSettings.toggleTheme(context)
                                isDarkTheme = ThemeSettings.isDark(context)
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            }
                        )

                        // Синтез (гироскоп + вибрация)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(
                                    1.5.dp,
                                    themeContentColor.copy(alpha = if (isSynthesisEnabled) 1f else 0.4f),
                                    RoundedHexagonShape
                                )
                                .clip(RoundedHexagonShape)
                                .clickable {
                                    val newValue = !isSynthesisEnabled
                                    isSynthesisEnabled = newValue
                                    // Записываем новое состояние на диск
                                    AppSettings.setSynthesisEnabled(context, newValue)
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🧬",
                                fontSize = 24.sp,
                                modifier = Modifier.alpha(if (isSynthesisEnabled) 1f else 0.3f)
                            )
                        }

                        // Обои дня
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(
                                    1.5.dp,
                                    themeContentColor.copy(alpha = 0.6f),
                                    RoundedHexagonShape
                                )
                                .clip(RoundedHexagonShape)
                                .clickable {
                                    bgNumber = if (bgNumber < 10) bgNumber + 1 else 1
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🖼️", fontSize = 24.sp)
                        }

                        // Корзина удаления
                        if (isDeletable) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(
                                        1.5.dp,
                                        Color.Red.copy(alpha = 0.6f),
                                        RoundedHexagonShape
                                    )
                                    .clip(RoundedHexagonShape)
                                    .clickable {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                                        showDeleteConfirmation = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🗑️", fontSize = 24.sp)
                            }
                        }
                    }

                    // 6. УПРАВЛЕНИЕ ФОНОВЫМ СЕРВИСОМ
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = L10n.backgroundMode, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        androidx.compose.material3.Switch(
                            checked = isBackgroundEnabled,
                            onCheckedChange = { isChecked ->
                                isBackgroundEnabled = isChecked
                                AppSettings.setBackgroundMode(context, isChecked)

                                val intent =
                                    android.content.Intent(context, EtherService::class.java)
                                if (isChecked) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                } else {
                                    context.stopService(intent)
                                }
                            }
                        )
                    }

                    // ПАРКОВКА АНТЕННЫ (ВЫБОР ФОНОВОГО СЕРВЕРА)
                    val customServers = remember(slots) {
                        slots.map { it.serverUrl }.filter { it.isNotBlank() }.distinct()
                    }
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text(
                            text = L10n.antennaParking, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF222222), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                                .clickable { expandedServerDropdown = true }
                                .padding(12.dp)
                        ) {
                            Text(
                                if (selectedBgServer.isBlank()) "ORDO MAIN" else selectedBgServer,
                                color = Color.Yellow,
                                fontSize = 14.sp
                            )
                            androidx.compose.material3.DropdownMenu(
                                expanded = expandedServerDropdown,
                                onDismissRequest = { expandedServerDropdown = false },
                                modifier = Modifier.background(Color(0xFF111111))
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("ORDO MAIN", color = Color.White) },
                                    onClick = {
                                        selectedBgServer = ""
                                        AppSettings.setBackgroundServer(context, "")
                                        NetworkManager.changeServer("")
                                        expandedServerDropdown = false
                                    }
                                )
                                customServers.forEach { url ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(url, color = Color.White) },
                                        onClick = {
                                            selectedBgServer = url
                                            AppSettings.setBackgroundServer(context, url)
                                            NetworkManager.changeServer(url)
                                            expandedServerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 7. ЭКСПОРТ/ИМПОРТ ДНК-КЛЮЧА ПОЛЬЗОВАТЕЛЯ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val key = IdentityManager.exportIdentity(context)
                                if (key != null) {
                                    val clipboard =
                                        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip =
                                        android.content.ClipData.newPlainText("ORDO_KEY", key)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(
                                        context,
                                        "ДНК-Ключ скопирован!",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = L10n.exportDna,
                                color = Color.Cyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = {
                                val clipboard =
                                    context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val pasteData =
                                    clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

                                if (pasteData.contains("|")) {
                                    val success = IdentityManager.importIdentity(context, pasteData)
                                    if (success) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Личность восстановлена! Перезапустите приложение.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Ошибка ключа!",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Буфер обмена пуст или ключ неверный",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = L10n.importDna,
                                color = Color.Cyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    // 8. БЭКАПЫ ЧАТОВ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportDbLauncher.launch("ORDO_Backup_${System.currentTimeMillis()}.ordo") },
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = L10n.saveChats,
                                color = Color(0xFF00C853),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = { importDbLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = L10n.loadChats,
                                color = Color(0xFFFFD600),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    // 9. ВОСКРЕШЕНИЕ ЖУРНАЛА
                    if (!IdentityManager.isSystemInfo()) {
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = DatabaseManager.getDao()
                                    if (dao.getSlotById("1.4") == null) {
                                        dao.insertSlot(
                                            SlotEntity(
                                                id = "1.4",
                                                status = "ACTIVE",
                                                partnerDisplayName = "Инфо",
                                                partnerId = "ALL"
                                            )
                                        )
                                    }
                                }
                                isSettingsMode = false // Закрываем настройки
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2200)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = L10n.revealInfo, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                                color = Color(0xFFFFD600),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 10. КНОПКА «СОХРАНИТЬ И ВЫЙТИ» (СТАЛО)
                    Button(
                        onClick = {
                            // 1. Сохраняем имя и аватарку профиля в IdentityManager
                            IdentityManager.saveProfile(context, nameInput, avatarBase64)


                            if (isDeletable && renameInput.isNotBlank() && renameInput != activeNode.name) {
                                activeNode.name = renameInput
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        DatabaseManager.getNodeDao().updateNodeName(activeNode.id, renameInput)
                                    } catch (e: Exception) {
                                        Log.e("HEX_NAV", "Ошибка сохранения имени соты: ${e.message}")
                                    }
                                }
                            }

                            isSettingsMode = false
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = L10n.saveAndExit, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }


        HexMeshOverlay(
            isVisible = isStarted && !isChatMode,
            activeNode = activeNode
        )

        // --- СЕТЕВОЙ СТАТУС ВНИЗУ ЭКРАНА В ИНТРО (СТАЛО) ---
        // Добавили проверку !isSettingsMode, чтобы статус скрывался при открытых настройках
        if (!isStarted && !isChatMode && !isSettingsMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Переводим системный статус на лету через словарь L10n (СТАЛО)
                val localizedStatus = when (connectionStatus) {
                    "В СЕТИ" -> L10n.statusOnline
                    "ПОДКЛЮЧЕНИЕ..." -> L10n.statusConnecting
                    "ПОИСК СЕТИ..." -> L10n.statusSearching
                    "ОТКЛЮЧЕНО" -> L10n.statusDisconnected
                    else -> connectionStatus
                }

                // Цвет теперь сверяем по оригинальному значению, а выводим переведенный текст
                val statusColor = when (connectionStatus) {
                    "В СЕТИ" -> Color(0xFF00C853) // Зеленый неоновый
                    "ПОДКЛЮЧЕНИЕ...", "ПОИСК СЕТИ..." -> Color(0xFFFFD600) // Желтый
                    "ОТКЛЮЧЕНО" -> Color.Red
                    else -> Color.Gray
                }
                Text(
                    text = localizedStatus, // Выводим переведенный текст (СТАЛО)
                    color = statusColor.copy(alpha = 0.5f), // Слегка приглушенный неоновый оттенок ожидания
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
    // --- ДИАЛОГ КАТАСТРОФИЧЕСКОГО УДАЛЕНИЯ В СТИЛЕ ORDO (СТАЛО) ---
    if (showDeleteConfirmation) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteConfirmation = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF111111),
                modifier = Modifier.border(1.5.dp, Color.Red, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = L10n.deleteDialogTitle, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                        color = Color.Red,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = L10n.deleteDialogText(activeNode.name), // СТАЛО (ДИНАМИЧЕСКИЙ ПЕРЕВОД)
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text(
                                text = L10n.cancel, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                                color = Color.Gray,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = {
                                showDeleteConfirmation = false
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)


                                val nodeIdToDelete = activeNode.id
                                val nodeToDelete = activeNode

                                // 1. Запускаем рекурсивное удаление соты и всех её детей из баз данных
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        DatabaseManager.getNodeDao().deleteNodeAndChildren(nodeIdToDelete)
                                        DatabaseManager.getDao().deleteSlotAndChildren(nodeIdToDelete)
                                        DatabaseManager.getMsgDao().deleteMessagesAndChildren(nodeIdToDelete)
                                    } catch (e: Exception) {
                                        android.util.Log.e("HEX_NAV", "Ошибка удаления соты: ${e.message}")
                                    }
                                }

                                // 2. Удаляем соту из оперативной памяти (из списка детей её родителя)
                                val parent = nodeToDelete.parent
                                if (parent != null) {
                                    parent.children = parent.children.filter { it != nodeToDelete }
                                }

                                // 3. Мгновенно возвращаем пользователя в родительскую папку на уровень выше
                                activeNode = parent ?: rootNode
                                isSettingsMode = false
                                isStarted = true
                            }
                        ) {
                            Text(
                                text = L10n.delete, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                                color = Color.Red,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } // Конец Dialog
    } // Конец showDeleteConfirmation
} // Конец HexagonNavigationScreen