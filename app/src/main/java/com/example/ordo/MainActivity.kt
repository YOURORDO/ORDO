package com.example.ordo

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.provider.Settings
import android.net.Uri
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ordo.HexGridData.slots
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // --- ВОЗВРАЩАЕМ МАГИЮ МАСШТАБИРОВАНИЯ (СТАЛО) ---
        ScreenMetrics.init(this)

        IdentityManager.init(this)
        DatabaseManager.init(this)
        NetworkManager.appContext = applicationContext

        // Скрываем панели навигации и статус-бар (иммерсивный полноэкранный режим со свайпом)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Проверяем белый список энергосбережения и просим пользователя выдать права
        requestIgnoreBatteryOptimizations(this)

        // --- Теперь SmileManager запускается в фоне ---
        CoroutineScope(Dispatchers.Main).launch {
            SmileManager.init(this@MainActivity)
        }
        NetworkManager.appContext = applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            DatabaseManager.getMsgDao().resetStuckMessages()
        }

        setContent {
            // Читаем список сохраненных папок в реальном времени прямо с диска!
            val slots by DatabaseManager.getDao().getAllSlotsFlow().collectAsState(initial = emptyList())
            val dbNodes by DatabaseManager.getNodeDao().getAllNodesFlow().collectAsState(initial = emptyList())

            val context = LocalContext.current


            val isSystem = IdentityManager.isSystemInfo()


            val hasSystemChat = slots.any { it.id == "1.4" }


            val forceShowSystemChannel = false


            val rootNode = remember(dbNodes, slots) {
                reconstructTree(
                    dbNodes = dbNodes,
                    slots = slots,
                    isCreator = isSystem,
                    hasSystemChat = hasSystemChat || forceShowSystemChannel
                )
            }

            // ВРЕМЕННЫЙ ДИАГНОСТИЧЕСКИЙ ЛОГГЕР: Покажет в терминале, что реально считалось с диска
            LaunchedEffect(dbNodes) {
                Log.d("ORDO_DEBUG", "Загружено папок с диска: ${dbNodes.size}")
                dbNodes.forEach {
                    Log.d("ORDO_DEBUG", "  Считана сота: id=${it.id}, name=${it.name}, type=${it.nodeType}")
                }
            }

            val branch12Manager = remember { Branch12State() }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted -> Log.d("PERM", "Уведомления: $isGranted") }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // --- НОВЫЙ ГЛОБАЛЬНЫЙ НАБЛЮДАТЕЛЬ ЗА СВОРАЧИВАНИЕМ (Для режима без фона) ---
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (!AppSettings.isBackgroundModeEnabled(context)) {
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> {
                                // Если фон выключен — при сворачивании полностью тушим сокет и бережем батарею!
                                NetworkManager.disconnect()
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                // При возврате в приложение — запускаем сеть заново
                                val bgServer = AppSettings.getBackgroundServer(context)
                                val startUrl = if (bgServer.isBlank()) NetworkManager.DEFAULT_SERVER else bgServer
                                NetworkManager.connect(startUrl)
                            }
                            else -> {}
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            var activeSlotId by remember { mutableStateOf<String?>(null) }
            val connectionStatus by NetworkManager.statusFlow.collectAsState() // ЧИТАЕМ СТАТУС НАПРЯМУЮ ИЗ FLOW
            var messageTrigger by remember { mutableStateOf(0L) }
            var lastServerMessage by remember { mutableStateOf("") }

            // Автоматическое создание ячейки в БД при клике на новую соту-чат
            LaunchedEffect(activeSlotId) {
                if (activeSlotId != null && activeSlotId != "1.4") {
                    val exists = slots.any { it.id == activeSlotId }
                    if (!exists) {
                        // Используем встроенный контекст LaunchedEffect и переключаемся на поток базы данных (IO)
                        withContext(Dispatchers.IO) {
                            DatabaseManager.getDao().insertSlot(
                                SlotEntity(id = activeSlotId!!, status = "EMPTY")
                            )
                        }
                    }
                }
            }

            // СЛУШАЕМ СИСТЕМНЫЕ СИГНАЛЫ ИЗ НАШЕГО НОВОГО FLOW ПОТОКА
            LaunchedEffect(Unit) {
                NetworkManager.serverMessageFlow.collect { msg ->
                    lastServerMessage = msg
                    messageTrigger = System.currentTimeMillis()
                    Log.d("NETWORK", "Получен системный сигнал: $msg")
                }
            }

            // УПРАВЛЕНИЕ ЗАПУСКОМ: ЕСЛИ ФОН ВЫКЛЮЧЕН, СТАРТУЕМ КОННЕКТ НАПРЯМУЮ ИЗ АКТИВИТИ
            LaunchedEffect(Unit) {
                if (!AppSettings.isBackgroundModeEnabled(context)) {
                    val bgServer = AppSettings.getBackgroundServer(context)
                    val startUrl = if (bgServer.isBlank()) NetworkManager.DEFAULT_SERVER else bgServer
                    NetworkManager.connect(startUrl)
                }
            }

            Surface(modifier = Modifier.fillMaxSize().imePadding(), color = Color(0xFF0A0A0A)) {
                // Мы ВСЕГДА рисуем HexagonNavigationScreen как главный контейнер приложения
                HexagonNavigationScreen(
                    rootNode = rootNode,
                    branch12Manager = branch12Manager,
                    slots = slots,
                    connectionStatus = connectionStatus,
                    serverMessage = lastServerMessage,
                    messageTrigger = messageTrigger,
                    onChatNodeClick = { clickedNode ->
                        // При клике на соту-чат записываем её ID в активный слот
                        activeSlotId = clickedNode.id
                        lastServerMessage = ""

                        // Снимаем неоновый флажок пинга в БД при входе в ячейку чата (СТАЛО)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                DatabaseManager.getDao().setHasActivePing(clickedNode.id, false)
                            } catch (e: Exception) {
                                Log.e("MAIN_ACTIVITY", "Ошибка сброса пинга: ${e.message}")
                            }
                        }
                    },
                    onBack = {
                        // По клику назад сбрасываем активный чат в null
                        activeSlotId = null
                    }
                )
            }
        }

        // Запуск фоновых механизмов
        schedulePingAlarm(this)

        // Запускаем сервис в трее при старте приложения
        startEtherService()
    }

    private fun startEtherService() {
        if (!AppSettings.isBackgroundModeEnabled(this)) return

        val serviceIntent = Intent(this, EtherService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d("SERVICE", "Сервис трея запущен при старте")
        } catch (e: Exception) {
            Log.e("SERVICE", "Ошибка старта сервиса: ${e.message}")
        }
    }

    private fun schedulePingAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EtherReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Проверка для Android 12+ (API 31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Если нет разрешения, просим пользователя его выдать
                Log.e("ALARM", "Нет прав на точные будильники. Открываем настройки.")
                val permissionIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(permissionIntent)
                return // Выходим, пока не дадут права
            }
        }

        val triggerTime = System.currentTimeMillis() + (3 * 60 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("ALARM", "Будильник успешно установлен!")
        } catch (e: SecurityException) {
            Log.e("ALARM", "Нет прав на точный будильник: ${e.message}")
        } catch (e: Exception) {
            Log.e("ALARM", "Ошибка: ${e.message}")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlotDetailContainer(
    slot: SlotEntity,
    status: String,
    serverMessage: String,
    messageTrigger: Long,
    isSmileMenuVisible: Boolean, // Принимаем состояние (СТАЛО)
    onSmileMenuVisibleChange: (Boolean) -> Unit, // Принимаем коллбэк (СТАЛО)
    onBack: () -> Unit
) {
    if (slot.status == "ACTIVE") {
        ChatInterface(
            slot = slot,
            status = status,
            serverMessage = serverMessage,
            messageTrigger = messageTrigger,
            isSmileMenuVisible = isSmileMenuVisible, // Передаем стейт дальше (СТАЛО)
            onSmileMenuVisibleChange = onSmileMenuVisibleChange, // Передаем коллбэк дальше (СТАЛО)
            onBack = onBack
        )
    } else {
        RitualInterface(slot.id, serverMessage, status, onBack)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RitualInterface(slotId: String, serverMessage: String, status: String, onBack: () -> Unit) {
    // 1. БАЗОВЫЕ ПЕРЕМЕННЫЕ (Перенесены наверх в правильном порядке)
    val myId = IdentityManager.getShortId()
    val scope = rememberCoroutineScope()

    var partnerIdInput by remember { mutableStateOf("") }
    var secretWordInput by remember { mutableStateOf("") }
    var ritualState by remember { mutableStateOf("IDLE") } // IDLE, SEARCHING, MATCHED

    // Стартовым значением делаем переведенный текущий статус подключения (СТАЛО)
    var statusMessage by remember {
        mutableStateOf(
            when (status) {
                "В СЕТИ" -> L10n.statusOnline
                "ПОДКЛЮЧЕНИЕ..." -> L10n.statusConnecting
                "ПОИСК СЕТИ..." -> L10n.statusSearching
                "ОТКЛЮЧЕНО" -> L10n.statusDisconnected
                else -> status
            }
        )
    }

    // Переменная кастомного сервера (теперь лежит вверху и доступна всем функциям)
    var customServerInput by remember { mutableStateOf("") }
    var showForeignNodeWarning by remember { mutableStateOf<Pair<String, String>?>(null) }

    var showQrDialog by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanning by remember { mutableStateOf(false) }


    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) isScanning = true else statusMessage = "Нужен доступ к камере!"
    }

    // ЛАУНЧЕР ДЛЯ ИМПОРТА QR-КОДА ИЗ ГАЛЕРЕИ (СТАЛО)
    val context = LocalContext.current
    val qrImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            statusMessage = L10n.scanAnalysis // СТАЛО (ЛОКАЛИЗАЦИЯ)
            // Запускаем асинхронный хелпер сканирования из галереи
            decodeAndProcessQrFromUri(context, uri) { scannedId, scannedServer ->
                if (scannedId == "EXPIRED") {
                    statusMessage = L10n.qrExpired // СТАЛО (ЛОКАЛИЗАЦИЯ)
                } else if (scannedId.isNotEmpty()) {
                    partnerIdInput = scannedId
                    if (scannedServer.isNotBlank() && scannedServer != NetworkManager.DEFAULT_SERVER) {
                        // Если в QR вшит другой сервер — выводим предупреждение
                        showForeignNodeWarning = Pair(scannedId, scannedServer)
                    } else {
                        // Обычный QR — мгновенно встаем в очередь
                        NetworkManager.changeServer("")
                        NetworkManager.sendCommand("SEARCH", mapOf("partnerId" to "SCAN:$scannedId"))
                    }
                } else {
                    statusMessage = L10n.scanError // СТАЛО (ЛОКАЛИЗАЦИЯ)
                }
            }
        }
    }



    // НОВЫЕ ПЕРЕМЕННЫЕ ДЛЯ ОТСЛЕЖИВАНИЯ КЛАВИАТУРЫ
    val focusManager = LocalFocusManager.current
    var isInputFocused by remember { mutableStateOf(false) }

    // --- ПЕРЕМЕННЫЕ И ФОРМА ДЛЯ ЭКРАНА РИТУАЛА ---
    val density = androidx.compose.ui.platform.LocalDensity.current

    val ritualCutY = 12.dp        // Вертикальная глубина скоса для всех 4 углов (крути под гексагон!)
    val ritualCutX = 20.dp        // Горизонтальная ширина скоса для всех 4 углов (крути под гексагон!)

    val rCutX = with(density) { ritualCutX.toPx() }
    val rCutY = with(density) { ritualCutY.toPx() }

    // Контур капсулы с идеально симметричными скосами на всех 4 углах
    val ritualShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
        val w = size.width
        val h = size.height

        moveTo(rCutX, 0f)               // 1. Верхний левый угол (после скоса)
        lineTo(w - rCutX, 0f)           // 2. Верхний правый угол (до скоса)
        lineTo(w, rCutY)                // 3. Верхний правый срез
        lineTo(w, h - rCutY)            // 5. Правая вертикальная грань
        lineTo(w - rCutX, h)            // 6. Правый нижний срез
        lineTo(rCutX, h)                // 7. Горизонтальная нижняя грань
        lineTo(0f, h - rCutY)           // 8. Левый нижний срез
        lineTo(0f, rCutY)               // 9. Левая вертикальная грань
        close()                         // 10. Замыкаем контур на левый верхний срез
    }

    // --- ПЕРЕМЕННЫЕ ДЛЯ СВАЙПА СОТЫ И АНИМАЦИЙ ---
    var rowWidthPx by remember { mutableFloatStateOf(0f) }      // Полная ширина поля ввода в пикселях
    var dragX by remember { mutableFloatStateOf(0f) }           // Текущая координата соты в пикселях

    val hexSizePx = with(density) { 49.dp.toPx() }             // С учетом твоих 49.dp
    val paddingPx = with(density) { 3.dp.toPx() }              // С учетом твоего отступа 3.dp

    // Вычисляем максимальный диапазон движения соты внутри поля
    val maxDragRange = (rowWidthPx - hexSizePx - paddingPx * 2f).coerceAtLeast(1f)

    // Прогресс сдвига соты (от 0.0 до 1.0)
    val dragProgress = (dragX / maxDragRange).coerceIn(0f, 1f)

    // --- ПЕРЕМЕННЫЕ ДЛЯ СВАЙПА СОТЫ СЕКРЕТНОГО СЛОВА (ЭТАП 2) ---
    var rowWidthPxSecret by remember { mutableFloatStateOf(0f) }
    var dragXSecret by remember { mutableFloatStateOf(0f) }
    val hexSizePxSecret = with(density) { 49.dp.toPx() }
    val paddingPxSecret = with(density) { 3.dp.toPx() }
    val maxDragRangeSecret = (rowWidthPxSecret - hexSizePxSecret - paddingPxSecret * 2f).coerceAtLeast(1f)
    val dragProgressSecret = (dragXSecret / maxDragRangeSecret).coerceIn(0f, 1f)
    var isInputFocusedSecret by remember { mutableStateOf(false) }

    // Создаем бесконечный таймер для фоновых пульсаций и вращений
    val infiniteTransition = rememberInfiniteTransition(label = "RitualEffects")

    // --- ПРУЖИННЫЙ ДОВОДЧИК СОТЫ-СЛАЙДЕРА СЕКРЕТНОГО СЛОВА ---
    val snapHexagonSecret: () -> Unit = {
        scope.launch {
            val target = if (dragProgressSecret > 0.85f) maxDragRangeSecret else 0f

            Animatable(dragXSecret).animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            ) {
                dragXSecret = this.value
            }

            // РЕАКЦИЯ НА ФИНАЛЬНУЮ ТОЧКУ (Активация моста вместо поиска):
            if (target == maxDragRangeSecret) {
                if (secretWordInput.isNotBlank()) {
                    statusMessage = "Активация моста..."
                    val myFullKey = IdentityManager.getFullPublicKey()
                    val cid = IdentityManager.createChannelId(myFullKey, partnerIdInput, secretWordInput)
                    NetworkManager.sendCommand("ACTIVATE", mapOf("partnerId" to partnerIdInput, "channelId" to cid))
                }
            }
        }
    }

    // 1. Анимация плавного мерцания для стрелочек ">>> введи id >>>"
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowAlpha"
    )

    // 2. Анимация непрерывного вращения буквы "П" при ожидании сети (SEARCHING)
    val waitingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waitingRotation"
    )

    // --- ПРУЖИННЫЙ ДОВОДЧИК СОТЫ-СЛАЙДЕРА ---
    val snapHexagon: () -> Unit = {
        scope.launch {
            // Если дотащили дальше 85% пути — магнитим вправо (maxDragRange), иначе — возвращаем влево (0f)
            val target = if (dragProgress > 0.85f) maxDragRange else 0f

            // Плавная анимация покадровой доводки/отскока
            Animatable(dragX).animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            ) {
                dragX = this.value
            }

            // РЕАКЦИЯ НА ФИНАЛЬНУЮ ТОЧКУ:
            if (target == maxDragRange) {
                // Если сота вщёлкнулась в правый край — запускаем поиск!
                if (ritualState == "IDLE") {
                    NetworkManager.changeServer(customServerInput)
                    NetworkManager.sendCommand("SEARCH", mapOf("partnerId" to partnerIdInput))
                }
            } else {
                // Если сота вернулась в левый край — отменяем поиск (если он был активен)
                if (ritualState == "SEARCHING") {
                    // Переводим локальный статус в IDLE
                    ritualState = "IDLE"
                    statusMessage = L10n.cancelSearch // СТАЛО (ЛОКАЛИЗАЦИЯ)
                    NetworkManager.sendCommand("CANCEL_SEARCH", emptyMap())
                }
            }
        }
    }

    // --- СИНХРОНИЗАЦИЯ СОСТОЯНИЯ СЕТИ И ПОЗИЦИИ СОТЫ (СТАЛО) ---
    LaunchedEffect(ritualState) {
        // Если поиск активен — плавно притягиваем соту «П» в крайнее правое положение (СТАЛО)
        if (ritualState == "SEARCHING" && dragX < maxDragRange) {
            Animatable(dragX).animateTo(maxDragRange, spring(dampingRatio = Spring.DampingRatioLowBouncy)) {
                dragX = this.value
            }
        }

        if (ritualState == "IDLE" && dragX > 0f) {
            Animatable(dragX).animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) {
                dragX = this.value
            }
        }
        // Возвращаем соту секрета в ноль, если соединение оборвалось
        if (ritualState != "MATCHED" && dragXSecret > 0f) {
            Animatable(dragXSecret).animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) {
                dragXSecret = this.value
            }
        }
    }

    // --- ПЕРЕХВАТЧИК 1: Если открыта клавиатура на любом из полей — просто скрываем её ---
    androidx.activity.compose.BackHandler(enabled = isInputFocused || isInputFocusedSecret) {
        focusManager.clearFocus()
    }

    // --- ПЕРЕХВАТЧИК 2: Если клавиатура закрыта на обоих полях — выходим ---
    androidx.activity.compose.BackHandler(enabled = !isInputFocused && !isInputFocusedSecret) {
        NetworkManager.changeServer(AppSettings.getBackgroundServer(context))
        CoroutineScope(Dispatchers.IO).launch {
            val currentSlot = DatabaseManager.getDao().getSlotById(slotId)
            if (currentSlot?.status == "EMPTY") {
                DatabaseManager.getDao().deleteSlotById(slotId)
            }
        }
        onBack()
    }

    // Синхронизируем изменение системного статуса сети и авто-восстанавливаем поиск при дисконнектах (СТАЛО)
    LaunchedEffect(status, ritualState) {
        if (ritualState == "IDLE") {
            // Переводим статус на лету (СТАЛО)
            statusMessage = when (status) {
                "В СЕТИ" -> L10n.statusOnline
                "ПОДКЛЮЧЕНИЕ..." -> L10n.statusConnecting
                "ПОИСК СЕТИ..." -> L10n.statusSearching
                "ОТКЛЮЧЕНО" -> L10n.statusDisconnected
                else -> status
            }
        }

        // Если сеть вернулась в статус "В СЕТИ", а сота «П» все еще крутится в режиме поиска —
        // автоматически бесшумно перерегистрируем наш поиск на сервере! (СТАЛО)
        if (status == "В СЕТИ" && ritualState == "SEARCHING") {
            if (partnerIdInput.isNotBlank()) {
                NetworkManager.sendCommand("SEARCH", mapOf("partnerId" to partnerIdInput))
            } else {
                NetworkManager.sendCommand("SEARCH", mapOf("partnerId" to "QR_OPEN"))
            }
        }
    }

    // --- СЛУШАЕМ ОТВЕТЫ СЕРВЕРА (ЖЕЛЕЗОБЕТОННАЯ ЛОГИКА V1 - НИЧЕГО НЕ ИЗМЕНЕНО) ---
    LaunchedEffect(serverMessage) {
        when {
            serverMessage == "SEARCH_ACCEPTED" -> {
                ritualState = "SEARCHING"
                statusMessage = L10n.airWaiting
            }
            serverMessage == "SEARCH_CANCELLED" -> {
                ritualState = "IDLE"
                statusMessage = L10n.cancelSearch
            }
            serverMessage.startsWith("MATCH") -> {
                if (serverMessage.contains("|")) {
                    val parts = serverMessage.split("|")
                    // ВАЖНО: Подменяем ID партнера на его ПОЛНЫЙ ключ!
                    partnerIdInput = parts[1]
                }
                ritualState = "MATCHED"
                statusMessage = L10n.contactEstablished
                showQrDialog = false
            }
            serverMessage == "ERROR_KEY" -> statusMessage = L10n.matchError
            serverMessage.startsWith("CONNECTED|") -> {
                val cid = serverMessage.substringAfter("|")
                val myFullKey = IdentityManager.getFullPublicKey()

                // Запускаем сопрограмму в рамках текущего LaunchedEffect
                scope.launch {
                    // 1. Уходим на вычислительный поток для тяжелой математики (PBKDF2)
                    val aes = withContext(Dispatchers.Default) {
                        EncryptionManager.generateKey(secretWordInput, myFullKey, partnerIdInput)
                    }

                    // 2. Уходим на поток ввода-вывода для безопасной записи в базу данных
                    withContext(Dispatchers.IO) {
                        DatabaseManager.getDao().updateSlot(
                            SlotEntity(
                                id = slotId,
                                partnerId = partnerIdInput,
                                channelId = cid,
                                status = "ACTIVE",
                                aesKey = aes,
                                serverUrl = customServerInput
                            )
                        )
                    }
                }
            }
        }
    }

    // --- ОСНОВНОЙ КОНТЕЙНЕР (С ПЕРЕХВАТОМ КЛИКОВ МИМО) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus() // Тап в пустоту = прячем клавиатуру
                })
            }
    ) {
        Image(painter = painterResource(id = R.drawable.fon_chat_night), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            // Заголовок (СТАЛО — локализовали ОТМЕНА и РИТУАЛ)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    NetworkManager.changeServer("")
                    CoroutineScope(Dispatchers.IO).launch {
                        val currentSlot = DatabaseManager.getDao().getSlotById(slotId)
                        if (currentSlot?.status == "EMPTY") {
                            DatabaseManager.getDao().deleteSlotById(slotId)
                        }
                    }
                    onBack()
                }) {
                    Text("< ${L10n.cancel}", color = Color.Gray)
                }

                Text("${L10n.ritualTitle}$slotId", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // --- ЭТАП 1: ПОИСК (НОВЫЙ КРАСИВЫЙ ДИЗАЙН) ---
            if (ritualState != "MATCHED") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // --- СЕНСОРНЫЙ КЛАСТЕР (СОТЫ) ---
                    val hexSize = 120.dp
                    val glassFill = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                    val borderFill = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.2f)))

                    var isLaunched by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { isLaunched = true }

                    val showCluster = isLaunched && !isInputFocused

                    val clusterProgress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (showCluster) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 250f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp * clusterProgress.coerceAtLeast(0f))
                            .graphicsLayer {
                                scaleX = clusterProgress
                                scaleY = clusterProgress
                                alpha = clusterProgress.coerceIn(0f, 1f)
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.65f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // 1. Верхняя сота (QR)
                            Box(
                                modifier = Modifier
                                    .size(hexSize)
                                    .clip(RoundedHexagonShape)
                                    .background(glassFill)
                                    .border(3.5.dp, borderFill, RoundedHexagonShape)
                                    .clickable {
                                        qrBitmap = generateQrBitmap(IdentityManager.createQrData(customServerInput))
                                        showQrDialog = true
                                        NetworkManager.sendCommand("SEARCH", mapOf("partnerId" to "QR_OPEN"))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_qr_hex), // Замени на свой ресурс, если другой
                                    contentDescription = "QR",
                                    modifier = Modifier.size(80.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.3f))
                                )
                            }

                            // 2. Нижний ряд сот (Камера и Галерея)
                            Row(
                                modifier = Modifier.offset(y = (-16).dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Левая сота (КАМЕРА)
                                Box(
                                    modifier = Modifier
                                        .size(hexSize)
                                        .clip(RoundedHexagonShape)
                                        .background(glassFill)
                                        .border(3.5.dp, borderFill, RoundedHexagonShape)
                                        .clickable {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_cam_hex), // Замени на свой ресурс
                                        contentDescription = "Камера",
                                        modifier = Modifier.size(80.dp),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.3f))
                                    )
                                }

                                // Правая сота (ГАЛЕРЕЯ) (СТАЛО)
                                Box(
                                    modifier = Modifier
                                        .size(hexSize)
                                        .clip(RoundedHexagonShape)
                                        .background(glassFill)
                                        .border(3.5.dp, borderFill, RoundedHexagonShape)
                                        .clickable {
                                            qrImportLauncher.launch("image/*") // Запускаем выбор картинки из Галереи (СТАЛО)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_gal_hex), // Замени на свой ресурс
                                        contentDescription = "Галерея",
                                        modifier = Modifier.size(80.dp),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp * clusterProgress.coerceAtLeast(0f)))

                    // === МАГИЯ ОТСКОКА ===
                    val bounceOffset = 150.dp * clusterProgress.coerceAtMost(0f)

                    // === 1. ПАНЕЛЬ НАСТРОЙКИ ЧАСТОТЫ (СЕРВЕРА) ===
                    Text(L10n.nodeConnection, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(start = 16.dp).offset(y = bounceOffset))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = bounceOffset)
                            .height(56.dp)
                            .shadow(
                                elevation = shadowElevation,
                                shape = ritualShape,
                                clip = false,
                                ambientColor = shadowColor,
                                spotColor = shadowColor
                            )
                            .border(
                                width = glassThickness,
                                color = if (customServerInput.isNotBlank()) Color.Yellow.copy(alpha = 0.5f) else glassEdgeColor,
                                shape = ritualShape
                            )
                            .background(color = myBubbleColor, shape = ritualShape)
                            .padding(start = 24.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = customServerInput, onValueChange = { customServerInput = it.trim() },
                            modifier = Modifier.weight(1f).onFocusChanged { isInputFocused = it.isFocused },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Yellow),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    NetworkManager.changeServer(customServerInput) // ПЕРЕКЛЮЧАЕМ СЕТЬ!
                                    statusMessage = if (customServerInput.isNotBlank()) "Частота: $customServerInput" else "Частота: ORDO MAIN"
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (customServerInput.isEmpty()) Text("ORDO MAIN", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                                innerTextField()
                            }
                        )
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            NetworkManager.changeServer(customServerInput) // ПЕРЕКЛЮЧАЕМ СЕТЬ!
                            statusMessage = if (customServerInput.isNotBlank()) "Частота: $customServerInput" else "Частота: ORDO MAIN"
                        }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Settings, contentDescription = null, tint = if (customServerInput.isNotBlank()) Color.Yellow else Color.DarkGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp).offset(y = bounceOffset))

                    // === 2. ПАНЕЛЬ ВВОДА ID (ИНТЕРАКТИВНЫЙ СЛАЙДЕР-СОТА) ===
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = bounceOffset)
                            .height(56.dp)
                            .onSizeChanged { size ->
                                rowWidthPx = size.width.toFloat() // Замеряем ширину поля на лету
                            }
                            .shadow(
                                elevation = shadowElevation,
                                shape = ritualShape,
                                clip = false,
                                ambientColor = shadowColor,
                                spotColor = shadowColor
                            )
                            .border(glassThickness, glassEdgeColor, ritualShape)
                            .background(color = myBubbleColor, shape = ritualShape),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // СЛОЙ 1 (ДНО): Центрированная область ввода и красивый маскируемый текст
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp), // Оставляем коридоры безопасности по краям под соту
                            contentAlignment = Alignment.Center
                        ) {
                            // ФИЗИЧЕСКАЯ МОДЕЛЬ: Каждая буква превращается в "*" строго в тот момент,
// когда левый край соты физически пересекает её координату на экране!
                            val textToDisplay by remember(partnerIdInput) {
                                androidx.compose.runtime.derivedStateOf {
                                    val textLength = partnerIdInput.length
                                    if (textLength == 0) ""
                                    else {
                                        // 1. Переводим физические границы текста в пиксели
                                        val textStartPx = with(density) { 48.dp.toPx() }
                                        val textWidthPx = rowWidthPx - with(density) { 96.dp.toPx() } // Отступы 48.dp с двух сторон

                                        // 2. Текущее положение левого края движущейся соты
                                        val hexLeftEdgePx = dragX + paddingPx+ with(density) { 30.dp.toPx() } // paddingPx — это уже рассчитанные в коде 8.dp в пикселях

                                        // Строим строку символ за символом
                                        val builder = StringBuilder()
                                        for (i in 0 until textLength) {
                                            // Прикидываем центр i-го символа на экране
                                            val charXPx = textStartPx + (i + 0.5f) * (textWidthPx / textLength)

                                            // Если символ остался позади левого края соты — маскируем его
                                            if (charXPx < hexLeftEdgePx) {
                                                builder.append("*")
                                            } else {
                                                builder.append(partnerIdInput[i])
                                            }
                                        }
                                        builder.toString()
                                    }
                                }
                            }

                            BasicTextField(
                                value = partnerIdInput,
                                onValueChange = { partnerIdInput = it.trim() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        isInputFocused = focusState.isFocused
                                    },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center // Центрируем ID
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Cyan),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (partnerIdInput.isEmpty()) {
                                            // Пульсирующий плейсхолдер намекает на ввод
                                            Text(
                                                text = L10n.enterIdPlaceholder,
                                                color = Color.Gray.copy(alpha = arrowAlpha),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            )
                                        } else {
                                            // ID обрамленный переливающимися стрелочками
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text("〉〉", color = Color.Cyan.copy(alpha = arrowAlpha), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Text(textToDisplay, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                                Text("〉〉", color = Color.Cyan.copy(alpha = arrowAlpha), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        // Прячем системный текст, оставляя только его ввод, чтобы видеть наш кастомный дизайн
                                        Box(modifier = Modifier.graphicsLayer(alpha = 0f)) {
                                            innerTextField()
                                        }
                                    }
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(start = 3.dp) // Начальный отступ соты слева
                                .graphicsLayer {
                                    // Сама сота теперь только едет по оси X и НИКОГДА не крутится!
                                    translationX = dragX
                                }
                                .size(49.dp)
                                .clip(RoundedHexagonShape)
                                .background(Color.DarkGray.copy(alpha = 0.92f), RoundedHexagonShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedHexagonShape)
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        // Разрешаем тащить соту если введен ID ИЛИ если уже идет активный поиск (СТАЛО)
                                        if ((partnerIdInput.isNotBlank() || ritualState == "SEARCHING") && ritualState != "MATCHED") {
                                            dragX = (dragX + delta).coerceIn(0f, maxDragRange)
                                        }
                                    },
                                    onDragStopped = { _ ->
                                        snapHexagon() // Запускаем физику доводки/отскока при отпускании
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Внутренний контейнер для буквы "П"
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    // Логика вращения буквы "П":
                                    // Если идет поиск (SEARCHING) — она непрерывно крутится (waitingRotation).
                                    // В обычном режиме — она крутится пропорционально сдвигу пальца (dragProgress * 360f).
                                    rotationZ = if (ritualState == "SEARCHING") waitingRotation else dragProgress * 360f
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "П",
                                    // Подсвечиваем букву Cyan цветом, как только введен ID и можно свайпать!
                                    color = if (partnerIdInput.isNotBlank()) Color.LightGray else Color.LightGray.copy(alpha = 0.4f),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Если мы перешли на этап ввода секрета, мягко выталкиваем весь этот блок в центр экрана по вертикали
            if (ritualState == "MATCHED") {
                Spacer(modifier = Modifier.weight(1f))
            }

            // --- ЭТАП 2: СЕКРЕТНОЕ СЛОВО (Только после MATCH) ---
            if (ritualState == "MATCHED") {
                Column(
                    modifier = Modifier.fillMaxWidth(), // Полностью убрали серое тело и внутренние отступы!
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "КОНТАКТ УСТАНОВЛЕН!",
                        color = Color.Green,
                        fontWeight = FontWeight.Medium, // Облегчили шрифт заголовка
                        fontFamily = FontFamily.Monospace, // Сделали хакерским
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Новый интерактивный слайдер для секретного слова
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .onSizeChanged { size ->
                                rowWidthPxSecret = size.width.toFloat()
                            }
                            .shadow(
                                elevation = shadowElevation,
                                shape = ritualShape,
                                clip = false,
                                ambientColor = shadowColor,
                                spotColor = shadowColor
                            )
                            .border(glassThickness, glassEdgeColor, ritualShape)
                            .background(color = myBubbleColor, shape = ritualShape),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // СЛОЙ 1 (ДНО): Центрированная область ввода и маскируемый текст секрета
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val textToDisplaySecret by remember(secretWordInput) {
                                androidx.compose.runtime.derivedStateOf {
                                    val textLength = secretWordInput.length
                                    if (textLength == 0) ""
                                    else {
                                        val textStartPx = with(density) { 48.dp.toPx() }
                                        val textWidthPx = rowWidthPxSecret - with(density) { 96.dp.toPx() }
                                        val hexLeftEdgePx = dragXSecret + paddingPxSecret + with(density) { 30.dp.toPx() }

                                        val builder = StringBuilder()
                                        for (i in 0 until textLength) {
                                            val charXPx = textStartPx + (i + 0.5f) * (textWidthPx / textLength)
                                            if (charXPx < hexLeftEdgePx) {
                                                builder.append("*")
                                            } else {
                                                builder.append(secretWordInput[i])
                                            }
                                        }
                                        builder.toString()
                                    }
                                }
                            }

                            BasicTextField(
                                value = secretWordInput,
                                onValueChange = { secretWordInput = it.trim() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        isInputFocusedSecret = focusState.isFocused
                                    },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal, // Облегченный шрифт ввода
                                    fontFamily = FontFamily.Monospace, // Моноширинный киберпанк-шрифт
                                    letterSpacing = 4.sp, // Увеличенный зазор между буквами
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Cyan),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (secretWordInput.isEmpty()) {
                                            Text(
                                                text = "〉〉ваше слово〉〉",
                                                color = Color.Gray.copy(alpha = arrowAlpha),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Normal, // Облегченный подсказчик
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 2.sp
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text("〉〉", color = Color.Cyan.copy(alpha = arrowAlpha), fontSize = 14.sp, fontWeight = FontWeight.Normal)
                                                Text(
                                                    text = textToDisplaySecret,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Normal, // Тонкий маскируемый текст
                                                    fontFamily = FontFamily.Monospace,
                                                    letterSpacing = 4.sp
                                                )
                                                Text("〉〉", color = Color.Cyan.copy(alpha = arrowAlpha), fontSize = 14.sp, fontWeight = FontWeight.Normal)
                                            }
                                        }
                                        Box(modifier = Modifier.graphicsLayer(alpha = 0f)) {
                                            innerTextField()
                                        }
                                    }
                                }
                            )
                        }

                        // СЛОЙ 2 (КРЫШКА): Вторая сота-слайдер
                        Box(
                            modifier = Modifier
                                .padding(start = 3.dp)
                                .graphicsLayer {
                                    translationX = dragXSecret
                                }
                                .size(49.dp)
                                .clip(RoundedHexagonShape)
                                .background(Color.DarkGray.copy(alpha = 0.92f), RoundedHexagonShape)
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedHexagonShape)
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        if (secretWordInput.isNotBlank() && ritualState == "MATCHED") {
                                            dragXSecret = (dragXSecret + delta).coerceIn(0f, maxDragRangeSecret)
                                        }
                                    },
                                    onDragStopped = { _ ->
                                        snapHexagonSecret()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = if (statusMessage == "Активация моста...") waitingRotation else dragProgressSecret * 360f
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = L10n.letterP, // СТАЛО (Буква П или O на английском)
                                    color = if (secretWordInput.isNotBlank()) Color.LightGray else Color.LightGray.copy(alpha = 0.4f),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Пружинный разделитель снизу будет идеально балансировать центральный элемент с верхним
            Spacer(modifier = Modifier.weight(1f))

            // Динамически подсвечиваем статус сети правильными неоновыми оттенками (СТАЛО)
            val statusColor = when (statusMessage) {
                "В СЕТИ" -> Color(0xFF00C853) // Зеленый неоновый
                "ПОДКЛЮЧЕНИЕ...", "ПОИСК СЕТИ..." -> Color(0xFFFFD600) // Желтый
                "ОТКЛЮЧЕНО" -> Color.Red
                else -> Color.Gray
            }

            Text(
                text = statusMessage,
                color = statusColor,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        // --- УМНЫЙ СКАНЕР (ДЕТЕКТОР СТОРОННИХ СЕРВЕРОВ) ---
        if (isScanning) {
            QrScannerView(
                onCodeScanned = { rawData ->
                    val res = IdentityManager.parseQrData(rawData)
                    if (res != null && res.first != "EXPIRED") {
                        val scannedId = res.first
                        val scannedServer = res.second
                        isScanning = false

                        if (scannedServer.isNotBlank() && scannedServer != NetworkManager.DEFAULT_SERVER) {
                            // ОПАСНОСТЬ: ЧУЖОЙ СЕРВЕР! ВЫЗЫВАЕМ ПРЕДУПРЕЖДЕНИЕ!
                            showForeignNodeWarning = Pair(scannedId, scannedServer)
                        } else {
                            // Обычный QR-код, просто подключаемся
                            partnerIdInput = scannedId
                            NetworkManager.changeServer("") // Убеждаемся, что мы на главном
                            NetworkManager.sendCommand("SEARCH", mapOf("partnerId" to "SCAN:$scannedId"))
                        }
                    } else if (res?.first == "EXPIRED") {
                        statusMessage = "QR истёк!"
                        isScanning = false
                    }
                },
                onClose = { isScanning = false }
            )
        }

        // --- ТРЕВОЖНОЕ ОКНО ДАРКНЕТА ---
        if (showForeignNodeWarning != null) {
            val (scannedId, scannedServer) = showForeignNodeWarning!!
            Dialog(onDismissRequest = { showForeignNodeWarning = null }) {
                Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF111111), modifier = Modifier.border(1.5.dp, Color.Red, RoundedCornerShape(24.dp))) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️ СТОРОННИЙ УЗЕЛ", color = Color.Red, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Этот ритуал использует внешний сервер:\n$scannedServer\n\nВы выходите из безопасной зоны. Уведомления ORDO здесь недоступны.", color = Color.White, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { showForeignNodeWarning = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))) {
                                Text("ОТМЕНА", color = Color.White)
                            }
                            Button(onClick = {
                                customServerInput = scannedServer // Прописываем в интерфейс
                                partnerIdInput = scannedId
                                NetworkManager.changeServer(scannedServer) // ПРЫГАЕМ НА НОВЫЙ СЕРВЕР
                                NetworkManager.sendCommand("SEARCH", mapOf("partnerId" to "SCAN:$scannedId")) // ИЩЕМ ДРУГА
                                showForeignNodeWarning = null
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text("ПРИНЯТЬ", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showQrDialog && qrBitmap != null) {
            // Точно рассчитываем 24-часовое время истечения кода (+30 минут) один раз при открытии диалога
            val expiryString = remember(showQrDialog) {
                val expiryTime = System.currentTimeMillis() + (30 * 60 * 1000L)
                val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                formatter.format(java.util.Date(expiryTime))
            }

            // Получаем имя создателя QR для вывода подписи
            val myName = IdentityManager.getMyName(context)
            val senderName = if (myName.isNotBlank()) myName else IdentityManager.getShortId()

            Dialog(onDismissRequest = { showQrDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF111111),
                    modifier = Modifier.padding(16.dp).border(1.dp, Color.DarkGray, RoundedCornerShape(28.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(L10n.qrTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) // СТАЛО (ЛОКАЛИЗАЦИЯ)

                        // Мелкие серые monospace-подписи с именем и точным временем (СТАЛО)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${L10n.invitationFrom}$senderName",
                            color = Color.LightGray.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${L10n.validUntil}$expiryString",
                            color = Color.LightGray.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(Modifier.height(16.dp))
                        Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.size(240.dp).background(Color.White).padding(12.dp))
                        Spacer(Modifier.height(24.dp))

                        // Ряд из трех кнопок управления (СТАЛО — генерируем и экспортируем брендированную карточку)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Генерируем красивую брендированную карточку и сохраняем её в Галерею (СТАЛО)
                                    val decoratedCard = generateDecoratedQrCard(context, IdentityManager.createQrData(customServerInput), senderName, expiryString)
                                    if (decoratedCard != null) {
                                        saveQrToGallery(context, decoratedCard)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(L10n.save, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) // СТАЛО (ЛОКАЛИЗАЦИЯ)
                            }

                            Button(
                                onClick = {
                                    // Генерируем красивую брендированную карточку и делимся ею в Telegram (СТАЛО)
                                    val decoratedCard = generateDecoratedQrCard(context, IdentityManager.createQrData(customServerInput), senderName, expiryString)
                                    if (decoratedCard != null) {
                                        shareQrImage(context, decoratedCard)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(L10n.share, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) // СТАЛО (ЛОКАЛИЗАЦИЯ)
                            }

                            Button(
                                onClick = { showQrDialog = false },
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(L10n.close, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) // СТАЛО (ЛОКАЛИЗАЦИЯ)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatInterface(
    slot: SlotEntity,
    status: String,
    serverMessage: String,
    messageTrigger: Long,
    isSmileMenuVisible: Boolean, // Принимаем стейт смайликов снаружи (СТАЛО)
    onSmileMenuVisibleChange: (Boolean) -> Unit, // Принимаем коллбэк снаружи (СТАЛО)
    onBack: () -> Unit
) {
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    val myId = IdentityManager.getShortId()
    var isPartnerOnline by remember { mutableStateOf(false) }
    var myEtherOn by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var contextMenuMsgId by remember { mutableStateOf<String?>(null) }

    // Старый локальный стейт var isSmileMenuVisible полностью удален! (СТАЛО)

    var editingMsgId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    val focusManager = LocalFocusManager.current
    var currentHeaderState by remember { mutableStateOf(HeaderState.OFFLINE) }
    val scope = rememberCoroutineScope()
    // ПЕРЕНЕСЛИ И ОБЕРНУЛИ В REMEMBER (Теперь доступно везде в этой функции)
    val displayName = remember(slot.id, slot.partnerDisplayName, slot.partnerId) {
        if (slot.id == "1.4") {
            L10n.infoChannel
        } else if (slot.partnerDisplayName.isNotBlank()) {
            slot.partnerDisplayName
        } else {
            if (slot.partnerId.length > 10) slot.partnerId.take(8) + "..." else "ЯЧЕЙКА"
        }
    }

    val flushMessages = {
        scope.launch(Dispatchers.IO) {
            // ПРОВЕРКА: Если хотя бы один офлайн — даже не пытаемся
            if (!myEtherOn || !isPartnerOnline) return@launch

            val readyMessages = DatabaseManager.getMsgDao().getReadyMessages(slot.id)
            readyMessages.forEach { msg ->
                // Переводим в SENDING только если мы точно онлайн
                DatabaseManager.getMsgDao().updateStatus(msg.msgId, "SENDING")

                val encryptedText = EncryptionManager.encrypt(msg.text, slot.aesKey)
                NetworkManager.sendCommand("CHAT_MSG", mapOf("text" to encryptedText, "msgId" to msg.msgId))

                launch {
                    kotlinx.coroutines.delay(8000)
                    DatabaseManager.getMsgDao().rollbackStatus(msg.msgId)
                }

                kotlinx.coroutines.delay(1100)
            }
        }
    }

    // BackHandler
    androidx.activity.compose.BackHandler(enabled = true) {
        if (isSmileMenuVisible) {
            onSmileMenuVisibleChange(false) // Плавное закрытие через коллбэк (СТАЛО)
        } else {
            // --- РАСТВОРЕНИЕ ЯЧЕЙКИ-ПРИЗРАКА ПРИ ВЫХОДЕ ---
            if (slot.id == "1.4" && !IdentityManager.isSystemInfo()) {
                scope.launch(Dispatchers.IO) {
                    DatabaseManager.getDao().deleteSlotById("1.4")
                }
            }
            onBack()
        }
    }
    // ═══ ПЕРЕКЛЮЧАТЕЛЬ ФОНА ═══

    val context = LocalContext.current
    var isDayTheme by remember { mutableStateOf(!ThemeSettings.isDark(context)) }

    // ТЕПЕРЬ bgRes отвечает ТОЛЬКО за день/ночь для основного экрана
    val bgRes = if (isDayTheme) R.drawable.fon_chat_day else R.drawable.fon_chat_night

    // стили
    val myBubbleColor = Color(0xFF98b7fe).copy(alpha = 0.2f)
    val peerBubbleColor = Color(0xFF7186b8).copy(alpha = 0.2f)
    val glassEdgeColor = Color.White.copy(alpha = 0.22f)
    val glassThickness = 1.dp
    val cutSize = 8.dp
    val cornerRadius = 3.dp
    val shadowElevation = 2.dp
    val shadowColor = Color.Black.copy(alpha = 0.9f)

    val bgBrush = remember(bgRes) {
        val bitmap = BitmapFactory.decodeResource(context.resources, bgRes)
        if (bitmap != null) {
            object : ShaderBrush() {
                override fun createShader(size: androidx.compose.ui.geometry.Size): Shader {
                    return BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                }
            }
        } else {
            null
        }
    }

    // Параллакс и списки
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val listStateBridge = androidx.compose.foundation.lazy.rememberLazyListState()
    val listStateShadow = androidx.compose.foundation.lazy.rememberLazyListState()

    val bridgeMessages by DatabaseManager.getMsgDao().getBridgeMessages(slot.id).collectAsState(initial = emptyList())
    val shadowMessages by DatabaseManager.getMsgDao().getShadowMessages(slot.id).collectAsState(initial = emptyList())

    LaunchedEffect(bridgeMessages.size) {
        if (bridgeMessages.isNotEmpty()) listStateBridge.animateScrollToItem(0)
    }
    LaunchedEffect(shadowMessages.size) {
        if (shadowMessages.isNotEmpty()) listStateShadow.animateScrollToItem(0)
    }

    // --- Сетевые эффекты (Умное переключение) ---
    DisposableEffect(Unit) {
        NetworkManager.changeServer(slot.serverUrl)

        val prefs = context.getSharedPreferences("ether_poll_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_channel_id", slot.channelId).apply()

        // СОХРАНЯЕМ НЕШИФРОВАННЫЙ РЕЗЕРВНЫЙ КЭШ ДЛЯ ФОНОВОГО ПИНГА (БЕЗ УЧАСТИЯ KEYSTORE)
        val cachePrefs = context.getSharedPreferences("unencrypted_slots_cache", Context.MODE_PRIVATE)
        cachePrefs.edit().apply {
            putString("name_${slot.channelId}", displayName)
            putBoolean("muted_${slot.channelId}", slot.isMuted)
            putString("slot_id_${slot.channelId}", slot.id)
            apply()
        }

        onDispose {
            NetworkManager.sendCommand("ETHER_STATE", mapOf("state" to "OFF"))
            NetworkManager.changeServer(AppSettings.getBackgroundServer(context))
        }
    }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // МГНОВЕННО гасим эфир на сервере при сворачивании!
                    // Собеседник сразу увидит, что мы вышли, а сервер перейдет в режим ожидания пингов.
                    NetworkManager.sendCommand("ETHER_STATE", mapOf("state" to "OFF"))
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Принудительный сброс слайдера в офлайн при возврате (твоя осознанная механика входа)
                    myEtherOn = false
                    currentHeaderState = HeaderState.OFFLINE
                    NetworkManager.sendCommand("ETHER_STATE", mapOf("state" to "OFF"))
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            // 1. Гасим трансляцию Эфира
            NetworkManager.sendCommand("ETHER_STATE", mapOf("state" to "OFF"))
            // 2. ДЕКЛАРИРУЕМ ВЫХОД ИЗ КОМНАТЫ НА СЕРВЕРЕ (команда RECONNECT с пустым ID канала)
            NetworkManager.sendCommand("RECONNECT", mapOf("channelId" to ""))
            // 3. Возвращаем сервер по умолчанию, если необходимо
            NetworkManager.changeServer(AppSettings.getBackgroundServer(context))
        }
    }


    LaunchedEffect(isPartnerOnline, myEtherOn) {
        if (myEtherOn && isPartnerOnline) {
            Log.d("CHAT", "Оба в сети! Автоматическая отправка READY-сообщений.")
            flushMessages()
        }
    }

    LaunchedEffect(Unit) {
        // --- ФИКС ГОНКИ ПОТОКОВ ---
        // Даем сборщику событий долю секунды на запуск, чтобы не пропустить моментальный ответ от сервера
        launch {
            kotlinx.coroutines.delay(150)
            NetworkManager.sendCommand("RECONNECT", mapOf("channelId" to slot.channelId))
            NetworkManager.sendCommand("ETHER_STATE", mapOf("state" to "OFF"))

            // --- СИНХРОНИЗИРУЕМ РЕЖИМ ТИШИНЫ С СЕРВЕРОМ ---
            NetworkManager.sendCommand("MUTE_STATE", mapOf("channelId" to slot.id, "state" to if (slot.isMuted) "ON" else "OFF"))

            val myName = IdentityManager.getMyName(context)
            val myAvatar = IdentityManager.getMyAvatar(context) ?: ""
            NetworkManager.sendCommand("PROFILE_DATA", mapOf("name" to myName, "avatar" to myAvatar))
        }

        // Сам сборщик (теперь он точно готов слушать)
        NetworkManager.messageFlow.collect { incomingMsg ->
            when {
                // ЛОВИМ СИГНАЛ СЧЕТЧИКА: Обновляем статус сообщения в БД на "DELIVERED: [число]"
                incomingMsg.startsWith("BROADCAST_COUNT|") -> {
                    val parts = incomingMsg.split("|")
                    val mid = parts[1]
                    val count = parts[2]
                    CoroutineScope(Dispatchers.IO).launch {
                        DatabaseManager.getMsgDao().updateStatus(mid, "DELIVERED: $count")
                    }
                }

                incomingMsg.startsWith("PROFILE_DATA|") -> {
                    val parts = incomingMsg.split("|")
                    val name = parts[1]
                    val avatar = if (parts.size > 2) parts[2] else null
                    CoroutineScope(Dispatchers.IO).launch {
                        val currentSlot = DatabaseManager.getDao().getSlotById(slot.id)
                        if (currentSlot != null) {
                            DatabaseManager.getDao().updateSlot(currentSlot.copy(
                                partnerDisplayName = name,
                                partnerAvatarBase64 = avatar
                            ))
                        }
                    }
                }
                incomingMsg == "[SYSTEM_RECONNECTED]" -> {
                    NetworkManager.sendCommand("RECONNECT", mapOf("channelId" to slot.channelId))
                    NetworkManager.sendCommand("ETHER_STATE", mapOf("state" to if(myEtherOn) "ON" else "OFF"))

                    // --- СИНХРОНИЗИРУЕМ СТАТУС ТИШИНЫ ПРИ ПЕРЕПОДКЛЮЧЕНИИ ---
                    NetworkManager.sendCommand("MUTE_STATE", mapOf("channelId" to slot.id, "state" to if (slot.isMuted) "ON" else "OFF"))
                    val myName = IdentityManager.getMyName(context)
                    val myAvatar = IdentityManager.getMyAvatar(context) ?: ""
                    NetworkManager.sendCommand("PROFILE_DATA", mapOf("name" to myName, "avatar" to myAvatar))
                }
                // --- НОВЫЙ СИГНАЛ: СЕРВЕР ПРОСИТ НАШ ПРОФИЛЬ ДЛЯ СВЕЖЕВОШЕДШЕГО ПАРТНЕРА ---
                incomingMsg == "[REQUEST_PROFILE]" -> {
                    val myName = IdentityManager.getMyName(context)
                    val myAvatar = IdentityManager.getMyAvatar(context) ?: ""
                    NetworkManager.sendCommand("PROFILE_DATA", mapOf("name" to myName, "avatar" to myAvatar))
                }
                incomingMsg == "[SIGNAL_ONLINE]" -> {
                    isPartnerOnline = true

                    CoroutineScope(Dispatchers.IO).launch {
                        DatabaseManager.getMsgDao().updateStatusByStatus(slot.id, "PENDING", "READY")
                        flushMessages()
                    }
                    val myName = IdentityManager.getMyName(context)
                    val myAvatar = IdentityManager.getMyAvatar(context) ?: ""
                    NetworkManager.sendCommand("PROFILE_DATA", mapOf("name" to myName, "avatar" to myAvatar))
                }
                incomingMsg == "[SIGNAL_OFFLINE]" -> {
                    isPartnerOnline = false
                    CoroutineScope(Dispatchers.IO).launch {
                        DatabaseManager.getMsgDao().markAsPending(slot.id)
                    }
                }
                incomingMsg.startsWith("ACK|") -> {
                    val mid = incomingMsg.substringAfter("|")
                    CoroutineScope(Dispatchers.IO).launch { DatabaseManager.getMsgDao().updateStatus(mid, "DELIVERED") }
                }
                incomingMsg.startsWith("ACK_PING|") -> {
                    val rawStatus = incomingMsg.substringAfter("|")
                    // Переводим текст, полученный от сервера
                    val localizedStatus = when (rawStatus) {
                        "Оставлено в эфире (партнер спит)" -> L10n.pingLeftInAir
                        "Сигнал доставлен мгновенно" -> L10n.pingDeliveredInstantly
                        else -> rawStatus
                    }
                    android.widget.Toast.makeText(context, localizedStatus, android.widget.Toast.LENGTH_SHORT).show()
                }
                incomingMsg.startsWith("MSG|") -> {
                    val parts = incomingMsg.split("|")
                    val mid = parts[1]
                    val encryptedTxt = parts[2]
                    val decryptedTxt = EncryptionManager.decrypt(encryptedTxt, slot.aesKey)
                    NetworkManager.sendCommand("ACK", mapOf("msgId" to mid))
                    val incoming = MessageEntity(
                        msgId = mid, slotId = slot.id, text = decryptedTxt,
                        status = "DELIVERED", timestamp = System.currentTimeMillis(), isMine = false
                    )
                    CoroutineScope(Dispatchers.IO).launch { DatabaseManager.getMsgDao().insert(incoming) }
                }
            }
        }
    }

    // --- 1. РАСЧЕТ КООРДИНАТ ДЛЯ ПАРАЛЛАКСА ---
    val density = androidx.compose.ui.platform.LocalDensity.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val translationXPx = -(screenWidthPx * dragProgress) // Тот самый X, который потерялся!

    // ═══ ИНТЕРФЕЙС (СЛОЁНЫЙ ПИРОГ) ═══
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(onTap = {
                contextMenuMsgId = null // Тап в любое место экрана сбрасывает меню!
                focusManager.clearFocus() // Прячем клавиатуру
                onSmileMenuVisibleChange(false) // Принудительно закрываем смайлики при тапе в пустоту (СТАЛО)
            })
        }
    ) {

        // --- СЛОЙ 1 (ДНО): ЧЕРНОВИК (ТЕНЬ) ---
        Box(modifier = Modifier.fillMaxSize().graphicsLayer {
            val scale = 0.9f + (0.1f * dragProgress)
            scaleX = scale
            scaleY = scale
            alpha = dragProgress
        }) {
            Image(painter = painterResource(id = R.drawable.draftbg), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                MessageList(
                    messages = shadowMessages,
                    state = listStateShadow,
                    aesKey = slot.aesKey,
                    channelId = slot.channelId,
                    myEtherOn = myEtherOn,
                    isPartnerOnline = isPartnerOnline,
                    editingMsgId = editingMsgId,
                    editingText = editingText,
                    onEditStart = { msg ->
                        editingMsgId = msg.msgId
                        editingText = androidx.compose.ui.text.input.TextFieldValue(text = msg.text, selection = androidx.compose.ui.text.TextRange(msg.text.length))
                    },
                    onEditingTextChange = { editingText = it },
                    onEditCancel = { editingMsgId = null },
                    onEditSave = { mid, txt ->
                        CoroutineScope(Dispatchers.IO).launch { DatabaseManager.getMsgDao().updateText(mid, txt) }
                        editingMsgId = null
                    },
                    contextMenuMsgId = contextMenuMsgId,
                    onContextMenuChange = { contextMenuMsgId = it }
                )
            }
        }

        // --- СЛОЙ 2 (КРЫШКА): ДИАЛОГ (ОНЛАЙН) ---
        Box(modifier = Modifier.fillMaxSize().graphicsLayer {
            translationX = translationXPx
            alpha = 1f - dragProgress
        }) {
            Image(painter = painterResource(id = bgRes), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                colorFilter = if (currentHeaderState == HeaderState.OFFLINE) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                MessageList(
                    messages = bridgeMessages,
                    state = listStateBridge,
                    aesKey = slot.aesKey,
                    channelId = slot.channelId,
                    myEtherOn = myEtherOn,
                    isPartnerOnline = isPartnerOnline,
                    editingMsgId = editingMsgId,
                    editingText = editingText,
                    onEditStart = { msg ->
                        editingMsgId = msg.msgId
                        editingText = androidx.compose.ui.text.input.TextFieldValue(text = msg.text, selection = androidx.compose.ui.text.TextRange(msg.text.length))
                    },
                    onEditingTextChange = { editingText = it },
                    onEditCancel = { editingMsgId = null },
                    onEditSave = { mid, txt ->
                        CoroutineScope(Dispatchers.IO).launch { DatabaseManager.getMsgDao().updateText(mid, txt) }
                        editingMsgId = null
                    },
                    contextMenuMsgId = contextMenuMsgId,
                    onContextMenuChange = { contextMenuMsgId = it }
                )
            }
        }

        // --- СЛОЙ 3: ШАПКА И ПОЛЕ ВВОДА (ВСЕГДА СВЕРХУ) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Переменную displayName отсюда просто стерли, так как она уже объявлена на самом верху!

            val cellNum = if (slot.id == "1.4") L10n.systemLabel else "#${slot.id}"

            KineticHeader(
                partnerName = displayName, // Используется переменная из верхней области видимости
                cellNumber = cellNum,
                partnerAvatarBase64 = slot.partnerAvatarBase64,
                isPartnerOnline = isPartnerOnline,
                state = currentHeaderState,
                onPingClick = { NetworkManager.sendCommand("SEND_PING", emptyMap()) },
                onDragProgress = { progress -> dragProgress = progress },
                onStateChanged = { newState ->
                    currentHeaderState = newState
                    when(newState) {
                        HeaderState.ONLINE -> {
                            myEtherOn = true
                            NetworkManager.sendCommand("ETHER_STATE", mapOf("state" to "ON"))
                            flushMessages()
                        }
                        else -> { // Если OFF или SHADOW
                            myEtherOn = false
                            NetworkManager.sendCommand("ETHER_STATE", mapOf("state" to "OFF"))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 65.dp, end = 10.dp, bottom = 14.dp)
            ) {
                // 1. Панель стилей (появляется только при редактировании)
                if (editingMsgId != null) {
                    FormatToolbar(
                        currentText = editingText.text,
                        selection = editingText.selection,
                        onTagClick = { tag ->
                            editingText = MarkdownHelper.toggleStyle(editingText, tag)
                        }
                    )
                }

                // 2. ЕДИНСТВЕННАЯ строка поля ввода
                if (slot.id == "1.4" && !IdentityManager.isSystemInfo()) {
                    // --- ЗАГЛУШКА ДЛЯ ЮЗЕРОВ (ТОЛЬКО ЧТЕНИЕ) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .background(Color(0xFF1B2433).copy(alpha = 0.9f), HexInputShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.22f), HexInputShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = L10n.systemReadOnlyLabel, // СТАЛО (ЛОКАЛИЗАЦИЯ)
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                } else {

                    val inputCutSize = 10.dp       // Базовый размер скоса для левой части (симметричный, 45°)

                    // Правый нижний асимметричный скос (настраивай эти две цифры независимо!)
                    val rightCutY = 9.dp           // Вертикальная глубина (меньше число -> длиннее плоский бок справа на 1-2dp)
                    val rightCutX = 16.dp          // Горизонтальная глубина (меньше число -> длиннее плоский низ)

                    // Переводим Dp в пиксели для нативной прорисовки линий
                    val cutL = with(density) { inputCutSize.toPx() }
                    val cutR_y = with(density) { rightCutY.toPx() }
                    val cutR_x = with(density) { rightCutX.toPx() }

                    // Строим асимметричный контур поля ввода
                    val inputShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
                        val w = size.width
                        val h = size.height

                        moveTo(cutL, 0f)                // 1. Верхний левый угол (после скоса)
                        lineTo(w, 0f)                   // 2. Верхний правый (острый, без скоса)
                        lineTo(w, h - cutR_y)           // 3. До начала правого нижнего скоса
                        lineTo(w - cutR_x, h)           // 4. Делаем асимметричный крутой срез
                        lineTo(cutL, h)                 // 5. Идем до левого нижнего скоса
                        lineTo(0f, h - cutL)            // 6. Левый нижний срез
                        lineTo(0f, cutL)                // 7. Стенка вверх до левого верхнего среза
                        close()                         // 8. Замыкаем контур
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .shadow(
                                elevation = shadowElevation,
                                shape = inputShape,
                                clip = false,
                                ambientColor = shadowColor,
                                spotColor = shadowColor
                            )
                            .border(glassThickness, glassEdgeColor, inputShape)
                            .background(color = myBubbleColor, shape = inputShape),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопка смайлов
                        IconButton(onClick = {
                            val targetState = !isSmileMenuVisible
                            onSmileMenuVisibleChange(targetState) // Меняем состояние смайлов через коллбэк (СТАЛО)
                            if (targetState) {
                                focusManager.clearFocus() // Скрываем клавиатуру при открытии панели смайлов (СТАЛО)
                            }
                        }) {
                            Icon(
                                imageVector = if (isSmileMenuVisible) Icons.Default.Keyboard else Icons.Default.Face,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (editingMsgId == null) {
                            BasicTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        // Принудительно плавно гасим смайлики, как только выезжает клавиатура (СТАЛО)
                                        if (focusState.isFocused) {
                                            onSmileMenuVisibleChange(false)
                                        }
                                    },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Cyan),
                                decorationBox = { innerTextField ->
                                    if (messageText.text.isEmpty()) {
                                        Text("Сообщение...", color = Color.Gray, fontSize = 15.sp)
                                    }
                                    innerTextField()
                                }
                            )


                            val sendBtnRoundness = 9f    // Округлость углов гексагона (от 0f до 35f)
                            val sendBtnSize = 36.dp       // Внешний размер кнопки
                            val sendIconSize = 16.dp      // Размер стрелочки внутри
                            val sendBorderWidth = 1.dp    // Толщина рамки (1.dp как у пузырьков)
                            val sendBorderAlpha = 0.2f   // Прозрачность рамки (22%)
                            val sendFillAlpha = 0.35f      // Прозрачность заливки (20%)
                            val sendColor = Color(0xFF40B7FF) // Цвет стрелочки и неона

                            // Вызываем общую функцию геометрии, передавая нужный нам срез
                            val customHexShape = getHexShape(sendBtnRoundness)

                            Box(
                                modifier = Modifier
                                    .padding(end = 2.dp) // Небольшой отступ кнопки от правого края поля ввода
                                    .size(sendBtnSize)
                                    .clip(customHexShape) // Обрезаем клик по форме гексагона
                                    .background(sendColor.copy(alpha = sendFillAlpha), customHexShape)
                                    .border(sendBorderWidth, Color.White.copy(alpha = sendBorderAlpha), customHexShape)
                                    .clickable {
                                        if (messageText.text.isNotBlank()) {
                                            val t = messageText.text
                                            messageText = androidx.compose.ui.text.input.TextFieldValue("")
                                            onSmileMenuVisibleChange(false) // Изменили на вызов коллбэка (СТАЛО)
                                            val mid = "${IdentityManager.getShortId()}-${System.currentTimeMillis()}"

                                            if (slot.id == "1.4") {

                                                val m = MessageEntity(
                                                    msgId = mid, slotId = slot.id, text = t,
                                                    status = "DELIVERED",
                                                    timestamp = System.currentTimeMillis(), isMine = true
                                                )
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    DatabaseManager.getMsgDao().insert(m)
                                                    val payloadText = "$mid|$t"
                                                    NetworkManager.sendCommand("infoBroadcast", mapOf("text" to payloadText)) // СТАЛО (профессионально и строчными)
                                                }
                                            } else {
                                                // --- ОБЫЧНАЯ ЛОГИКА ЧАТА ---
                                                var finalTxt = t
                                                if (finalTxt.startsWith("[SIGNAL_")) finalTxt = " " + finalTxt

                                                val shouldSendInstantly = (selectedTab == 0 && myEtherOn && isPartnerOnline)
                                                val targetStatus = if (shouldSendInstantly) "SENDING" else "EDITING"
                                                val m = MessageEntity(
                                                    msgId = mid,
                                                    slotId = slot.id,
                                                    text = finalTxt,
                                                    status = targetStatus,
                                                    timestamp = System.currentTimeMillis(),
                                                    isMine = true
                                                )
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    DatabaseManager.getMsgDao().insert(m)
                                                    if (shouldSendInstantly) {
                                                        val encryptedText = EncryptionManager.encrypt(finalTxt, slot.aesKey)
                                                        NetworkManager.sendCommand("CHAT_MSG", mapOf("text" to encryptedText, "msgId" to mid))
                                                        launch {
                                                            kotlinx.coroutines.delay(8000)
                                                            DatabaseManager.getMsgDao().rollbackStatus(mid)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = sendColor,
                                    modifier = Modifier.size(sendIconSize)
                                )
                            }
                        } else {
                            Text(
                                "Редактирование...",
                                color = Color.Cyan.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (isSmileMenuVisible) {
                SmilePicker(onSmileClick = { code ->
                    SmileManager.recordUsage(context, code)
                    val smileWithSpace = "$code "
                    if (editingMsgId != null) {
                        val currentText = editingText.text
                        val selection = editingText.selection
                        val newText = StringBuilder(currentText)
                            .insert(selection.start, smileWithSpace)
                            .toString()
                        editingText = androidx.compose.ui.text.input.TextFieldValue(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(selection.start + smileWithSpace.length)
                        )
                    } else {
                        val currentText = messageText.text
                        val selection = messageText.selection
                        val newText = StringBuilder(currentText)
                            .insert(selection.start, smileWithSpace)
                            .toString()
                        messageText = androidx.compose.ui.text.input.TextFieldValue(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(selection.start + smileWithSpace.length)
                        )
                    }
                })
            }
        }
    }
}
@Composable
fun SmilePicker(onSmileClick: (String) -> Unit) {
    val context = LocalContext.current
    val sortedSmiles = remember { SmileManager.getSortedSmiles(context) }

    // Используем сетку, которая загружает элементы "лениво" (только видимые)
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 40.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color(0xFF111111))
            .padding(8.dp)
    ) {
        items(sortedSmiles.size) { index ->
            val (code, fileName) = sortedSmiles[index]
            coil.compose.AsyncImage(
                model = "file:///android_asset/smiles/$fileName",
                contentDescription = null,
                imageLoader = SmileManager.imageLoader,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onSmileClick(code) }
            )
        }
    }
}
object NotificationHelper {
    fun showPingNotification(context: android.content.Context, fullMessage: String) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "PingChannel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "Входящие вызовы",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о вызове в Эфир"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(context, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setContentTitle(L10n.notificationTitle) // Используем переведенный заголовок из словаря
            .setContentText(fullMessage)
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(android.app.Notification.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
fun generateQrBitmap(content: String): Bitmap? {
    val size = 512
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun QrScannerView(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current // Это для запуска сервиса
    val lifecycleOwner =
        androidx.compose.ui.platform.LocalLifecycleOwner.current // А это для слежки за сворачиванием
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Очищаем камеру из памяти при закрытии сканера (чтобы она не висела в фоне и не жрала батарею)
    DisposableEffect(cameraProviderFuture) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                // Игнорируем ошибку отвязки
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val scanner = BarcodeScanning.getClient()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        // Важный фикс для красного .image
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        barcode.rawValue?.let { onCodeScanned(it) }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CAMERA", "Ошибка запуска: ${e.message}")
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Рамка сканера
        Box(
            modifier = Modifier.size(250.dp).align(Alignment.Center)
                .border(2.dp, Color.Cyan, RoundedCornerShape(12.dp))
        )

        // Кнопка закрытия
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding()
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
@Composable
fun EditProfileDialog(slots: List<SlotEntity>, onDismiss: () -> Unit, onSave: (String, String?) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(IdentityManager.getMyName(context)) }
    var avatarBase64 by remember { mutableStateOf(IdentityManager.getMyAvatar(context)) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { avatarBase64 = uriToBase64(context, it) }
    }

    // =======================================================
    // --- НОВЫЕ ЛАУНЧЕРЫ ДЛЯ БЭКАПА БАЗЫ ДАННЫХ (ЧАТОВ) ---
    // =======================================================
    val exportDbLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    DatabaseManager.closeDatabase() // Закрываем базу, чтобы все данные слились в один файл (WAL -> DB)
                    val dbFile = context.getDatabasePath("poryadok_vault.db")
                    context.contentResolver.openOutputStream(it)?.use { outStream ->
                        dbFile.inputStream().use { inStream -> inStream.copyTo(outStream) }
                    }
                    DatabaseManager.init(context) // Снова запускаем базу для работы
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Архив чатов сохранен!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Ошибка сохранения!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importDbLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
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

                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Архив загружен! Приложение перезапустится...", android.widget.Toast.LENGTH_LONG).show()
                        kotlinx.coroutines.delay(2000)
                        kotlin.system.exitProcess(0) // Жесткий, но 100% надежный способ перезагрузить приложение с новой базой
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Ошибка загрузки!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // =======================================================
    // --- ИНТЕРФЕЙС ДИАЛОГОВОГО ОКНА ---
    // =======================================================
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF111111), modifier = Modifier.border(1.dp, Color.DarkGray, RoundedCornerShape(24.dp))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(L10n.currentNodeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFF222222), RoundedHexagonShape)
                        .clip(RoundedHexagonShape)
                        .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedHexagonShape)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBase64 != null) {
                        val bitmap = remember(avatarBase64) {
                            val bytes = android.util.Base64.decode(avatarBase64, android.util.Base64.NO_WRAP)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                        }
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedHexagonShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(L10n.yourName, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(Modifier.height(20.dp))
                var isBackgroundEnabled by remember { mutableStateOf(AppSettings.isBackgroundModeEnabled(context)) }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(L10n.backgroundMode, color = Color.Gray, fontSize = 14.sp)
                    androidx.compose.material3.Switch(
                        checked = isBackgroundEnabled,
                        onCheckedChange = { isChecked ->
                            isBackgroundEnabled = isChecked
                            AppSettings.setBackgroundMode(context, isChecked) // СОХРАНЯЕМ НАСТРОЙКУ

                            val intent = Intent(context, EtherService::class.java)
                            if (isChecked) {
                                // Запускаем фоновую службу
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                // Гасим фоновую службу
                                context.stopService(intent)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // --- НАСТРОЙКА ФОНОВОЙ ЧАСТОТЫ (АНТЕННА) ---
                val customServers = remember(slots) { slots.map { it.serverUrl }.filter { it.isNotBlank() }.distinct() }
                var selectedBgServer by remember { mutableStateOf(AppSettings.getBackgroundServer(context)) }
                var expanded by remember { mutableStateOf(false) }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(L10n.antennaParking, color = Color.Gray, fontSize = 10.sp) // СТАЛО (ЛОКАЛИЗАЦИЯ)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222222), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                            .clickable { expanded = true }
                            .padding(12.dp)
                    ) {
                        Text(if (selectedBgServer.isBlank()) "ORDO MAIN" else selectedBgServer, color = Color.Yellow, fontSize = 14.sp)
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF111111))
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("ORDO MAIN", color = Color.White) },
                                onClick = {
                                    selectedBgServer = ""
                                    AppSettings.setBackgroundServer(context, "")
                                    NetworkManager.changeServer("")
                                    expanded = false
                                }
                            )
                            customServers.forEach { url ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(url, color = Color.White) },
                                    onClick = {
                                        selectedBgServer = url
                                        AppSettings.setBackgroundServer(context, url)
                                        NetworkManager.changeServer(url)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // --- КНОПКИ: ПАСПОРТ (ДНК) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val key = IdentityManager.exportIdentity(context)
                            if (key != null) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("ORDO_KEY", key)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "ДНК-Ключ скопирован!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(L10n.exportDna, color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold) // СТАЛО (ЛОКАЛИЗАЦИЯ)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val pasteData = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

                            if (pasteData.contains("|")) {
                                val success = IdentityManager.importIdentity(context, pasteData)
                                if (success) {
                                    android.widget.Toast.makeText(context, "Личность восстановлена! Перезапустите приложение.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Ошибка ключа!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Буфер обмена пуст или ключ неверный", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(L10n.importDna, color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // --- НОВЫЕ КНОПКИ: БАЗА ДАННЫХ (ЧАТЫ) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { exportDbLauncher.launch("ORDO_Backup_${System.currentTimeMillis()}.ordo") },
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(L10n.saveChats, color = Color(0xFF00C853), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { importDbLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(L10n.loadChats, color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))


                if (!IdentityManager.isSystemInfo()) {
                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = DatabaseManager.getDao()
                                if (dao.getSlotById("1.4") == null) {
                                    dao.insertSlot(SlotEntity(
                                        id = "1.4",
                                        status = "ACTIVE",
                                        partnerDisplayName = "Инфо",
                                        partnerId = "ALL"
                                    ))
                                }
                            }
                            onDismiss() // Закрываем настройки
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2200)), // Темно-желтый киберпанк
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(L10n.revealInfo, color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onSave(name, avatarBase64) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(L10n.saveAndExit, color = Color.Black, fontWeight = FontWeight.Bold) // СТАЛО (ЛОКАЛИЗАЦИЯ)
                }
            }
        }
    }
}
fun uriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream) ?: return null // Если картинка не прочиталась - выходим без краша
        val outputStream = java.io.ByteArrayOutputStream()

        // 1. Вычисляем размер для квадрата (по короткой стороне)
        val size = if (original.width > original.height) original.height else original.width
        val left = (original.width - size) / 2
        val top = (original.height - size) / 2

        // 2. Вырезаем центр (Crop)
        val cropped = Bitmap.createBitmap(original, left, top, size, size)

        // 3. Сжимаем до 200x200
        val scaled = Bitmap.createScaledBitmap(cropped, 200, 200, true)
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
    } catch (e: Exception) { null }
}
val HexInputShape = GenericShape { size, _ ->
    val cutSize = 35f // Размер скоса (косой линии)
    val sideHeight = size.height * 0.35f // Размер вертикальной "стенки" по центру (35% от высоты)
    val verticalStart = (size.height - sideHeight) / 2f
    val verticalEnd = verticalStart + sideHeight

    // 1. Начинаем сверху слева (после левого верхнего скоса)
    moveTo(cutSize, 0f)

    // 2. ВЕРХНЯЯ ЛИНИЯ: Идет до самого конца вправо (ОБРАЗУЕТ ПРЯМОЙ УГОЛ)
    lineTo(size.width, 0f)

    // 3. ПРАВАЯ СТЕНКА: Опускается строго вниз до начала нижнего правого скоса
    lineTo(size.width, verticalEnd)

    // 4. Угол снизу справа и нижняя линия
    lineTo(size.width - cutSize, size.height)
    lineTo(cutSize, size.height)

    // 5. Угол снизу слева и вертикальная стенка слева
    lineTo(0f, verticalEnd)
    lineTo(0f, verticalStart)

    // 6. close() автоматически соединит левую стенку с началом, дорисовав левый верхний скос
    close()
}
fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("BATTERY", "Не удалось открыть запрос оптимизации батареи: ${e.message}")
            }
        }
    }
}
// === СИСТЕМНЫЕ ХЕЛПЕРЫ ДЛЯ ЭКСПОРТА QR-КОДА (СТАЛО) ===

/**
 * Безопасно сохраняет сгенерированный QR-код в публичную системную Галерею Pictures/Ordo.
 * Работает без запроса разрешений на Android 10+ (API 29+).
 */
fun saveQrToGallery(context: android.content.Context, bitmap: android.graphics.Bitmap) {
    try {
        val filename = "ORDO_Pairing_${System.currentTimeMillis()}.png"
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Ordo")
            }
        }
        val resolver = context.contentResolver
        val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            resolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            android.widget.Toast.makeText(context, "QR-код сохранен в Галерею!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Ошибка сохранения!", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Сбой: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Сохраняет картинку во временный кэш и запускает системный Intent отправки "Поделиться"
 * через FileProvider.
 */
fun shareQrImage(context: android.content.Context, bitmap: android.graphics.Bitmap) {
    try {
        val cachePath = java.io.File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = java.io.File(cachePath, "ordo_pairing_qr.png")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        val contentUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        if (contentUri != null) {
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Отправить QR-код"))
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Сбой отправки: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Извлекает статический QR-код из выбранной в Галерее картинки через Google ML Kit.
 * Работает асинхронно в фоновом потоке, возвращая результат в переданный коллбэк.
 */
fun decodeAndProcessQrFromUri(
    context: android.content.Context,
    uri: android.net.Uri,
    onResult: (String, String) -> Unit
) {
    try {
        val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
        val image = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                var found = false
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (rawValue != null) {
                        val res = IdentityManager.parseQrData(rawValue)
                        if (res != null) {
                            found = true
                            onResult(res.first, res.second) // Возвращаем (ID/Ключ, Адрес сервера)
                            break
                        }
                    }
                }
                if (!found) {
                    onResult("", "") // Картинку прочитали, но QR-код Порядка не найден
                }
            }
            .addOnFailureListener {
                onResult("", "")
            }
    } catch (e: Exception) {
        onResult("", "")
    }
}
/**
 * Генерирует стильную, брендированную темно-серую карточку-приглашение (512 x 640),
 * встраивает в нее QR-код и прорисовывает системным шрифтом Monospace подписи
 * об имени создателя и времени действия.
 */
fun generateDecoratedQrCard(
    context: android.content.Context,
    content: String,
    senderName: String,
    expiryString: String
): android.graphics.Bitmap? {
    try {
        // 1. Генерируем базовый QR-код
        val qrSize = 448
        val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(content, com.google.zxing.BarcodeFormat.QR_CODE, qrSize, qrSize)
        val qrBitmap = android.graphics.Bitmap.createBitmap(qrSize, qrSize, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until qrSize) {
            for (y in 0 until qrSize) {
                qrBitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }

        // 2. Создаем виртуальный холст карточки-приглашения (512 x 640)
        val cardW = 512
        val cardH = 640
        val cardBitmap = android.graphics.Bitmap.createBitmap(cardW, cardH, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(cardBitmap)

        // Заливаем карточку глубоким темно-серым фоном
        canvas.drawColor(android.graphics.Color.parseColor("#111111"))

        // 3. Рисуем QR-код по центру с небольшим отступом сверху (32 пикселя)
        val qrLeft = (cardW - qrSize) / 2f
        val qrTop = 32f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        // 4. Настраиваем кисть для рисования левосторонних monospace-подписей
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#80FFFFFF") // 50% белый (серый оттенок)
            textSize = 16f
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT // СДВИГАЕМ ТЕКСТ ВЛЕВО
        }

        // Вычисляем координаты строк под QR-кодом
        val textLeftX = qrLeft + 12f // Небольшой эстетичный отступ от левого края QR
        val firstLineY = qrTop + qrSize + 56f
        val secondLineY = firstLineY + 36f

        canvas.drawText("Приглашение от: $senderName", textLeftX, firstLineY, paint)
        canvas.drawText("Действует до: $expiryString", textLeftX, secondLineY, paint)

        // 5. РИСУЕМ ЛОГОТИП (БУКВА В ГЕКСАГОНЕ) ЧИСТЫМ КОДОМ
        val cx = 432f
        val cy = (firstLineY + secondLineY) / 2f
        val radius = 32f // Внешний радиус гексагона

        // Отрисовка заливки гексагона (чуть светлее общего фона карточки)
        val hexPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1c1c1c")
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        val hexPath = android.graphics.Path()
        for (i in 0..5) {
            val angleRad = Math.toRadians(30.0 + i * 60.0)
            val x = (cx + radius * Math.cos(angleRad)).toFloat()
            val y = (cy + radius * Math.sin(angleRad)).toFloat()
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()
        canvas.drawPath(hexPath, hexPaint)


        hexPaint.apply {
            color = android.graphics.Color.parseColor("#CFD8DC") // Цвет контура (Silver)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f // Толщина обводки
        }
        canvas.drawPath(hexPath, hexPaint)


        val letterPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#CFD8DC") // Цвет буквы (Silver)
            textSize = 42f
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val textY = cy - (letterPaint.descent() + letterPaint.ascent()) / 2f


        canvas.drawText(L10n.letterP, cx, textY, letterPaint)

        return cardBitmap
    } catch (e: Exception) {
        return null
    }
}
