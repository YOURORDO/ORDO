package com.example.ordo

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class WsEnvelope(
    val command: String,
    val pubKey: String,
    val payload: String,
    val signature: String,
    var appContext: android.content.Context? = null
)

object NetworkManager {
    // --- БАЗОВАЯ ЧАСТОТА (НАШ ГЛАВНЫЙ СЕРВЕР) ---
    // Убираем const и пишем безопасную заглушку по умолчанию
    var DEFAULT_SERVER = "wss://your-private-relay-server.com/connect"

    // Наша бронебойная труба для сообщений
    val messageFlow = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 100)

    // Присвоение контекста автоматически обновит дефолтный адрес из запеченных ресурсов
    var appContext: android.content.Context? = null
        set(value) {
            field = value
            if (value != null) {
                try {
                    // Считываем сгенерированный во время сборки безопасный адрес
                    val resId = value.resources.getIdentifier("default_server_url", "string", value.packageName)
                    if (resId != 0) {
                        DEFAULT_SERVER = value.getString(resId)
                    }
                } catch (e: Exception) {
                    // Резервный сценарий в случае ошибки
                }
            }
        }
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS) // Авто-пинг каждые 30 сек. Детектирует скрытые обрывы мобильной сети
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    private val currentConnectionId = java.util.concurrent.atomic.AtomicInteger(0)

    private val gson = Gson()

    // --- БЫСТРАЯ ПАМЯТЬ ОТ ДУБЛЕЙ БРОДКАСТА (ПОТОКОБЕЗОПАСНАЯ) ---
    private val handledBroadcasts = object : java.util.LinkedHashMap<String, Boolean>(101, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
            return size > 100
        }
    }
    private val safeBroadcasts = java.util.Collections.synchronizedMap(handledBroadcasts)

    // Переменные для бессмертного реконнекта — теперь строго @Volatile для мгновенной синхронизации между ядрами процессора
    @Volatile
    private var isReconnecting = false

    @Volatile
    var currentUrl: String = ""

    @Volatile
    private var reconnectDelay = 2000L

    // --- НОВЫЕ РЕАКТИВНЫЕ ПОТОКИ ВЗАМЕН СТАРЫХ КОЛЛБЭКОВ ---
    private val _statusFlow = MutableStateFlow("Подключение...")
    val statusFlow = _statusFlow.asStateFlow()

    private val _serverMessageFlow = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val serverMessageFlow = _serverMessageFlow.asSharedFlow()

    private var timeOffset: Long = 0L

    fun connect(url: String) {
        // 1. Предотвращаем утечку сокетов: жестко закрываем предыдущий сокет перед созданием нового
        try {
            webSocket?.cancel()
        } catch (ignored: Exception) {}
        webSocket = null

        currentUrl = url

        // 2. Безопасно инкрементируем счетчик сессии
        val connectionId = currentConnectionId.incrementAndGet()

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (connectionId != currentConnectionId.get()) return

                isReconnecting = false
                reconnectDelay = 2000L
                _statusFlow.value = "В СЕТИ"
                _serverMessageFlow.tryEmit("[SYSTEM_RECONNECTED]")

                messageFlow.tryEmit("[SYSTEM_RECONNECTED]")

                // Отправляем базовый пинг-опрос
                sendCommand("CHECK_PING", emptyMap())

                // РЕГИСТРИРУЕМ ЛИЧНОСТЬ ГЛОБАЛЬНО НА СЕРВЕРЕ (БЕЗ ЗАНЯТИЯ КОМНАТ)
                // Отправляем RECONNECT с пустым ID канала. Сервер добавит нас в onlineSession,
                // но комната в RAM останется пустой (Active Chat Rooms: 0).
                // Если пользователь находится внутри чата, интерфейс сам переподключит его к нужной комнате.
                Log.d("NETWORK", "Фоновая регистрация глобального присутствия личности (PubKey)")
                sendCommand("RECONNECT", mapOf("channelId" to ""))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (connectionId != currentConnectionId.get()) return

                // Ловим системное время от сервера для синхронизации
                if (text.startsWith("[SYSTEM_TIME]|")) {
                    try {
                        val serverTime = text.substringAfter("|").toLong()
                        timeOffset = serverTime - System.currentTimeMillis()
                        Log.d("NETWORK", "Синхронизация времени: Смещение $timeOffset мс")
                    } catch (e: Exception) {}
                    return
                }

                // ПРИНИМАЕМ ДАННЫЕ ПРОФИЛЯ ОТ ПАРТНЕРА
                if (text.startsWith("PROFILE_DATA|")) {
                    _serverMessageFlow.tryEmit(text)
                    return
                }

                // --- ЛОВИМ СИСТЕМНЫЕ НОВОСТИ (БРОДКАСТ) ---
                val checkText = text.removePrefix("PING_NOTIFICATION|")
                if (checkText.startsWith("[BROADCAST]|")) {
                    val rawPayload = checkText.substringAfter("[BROADCAST]|")
                    val mid: String
                    val cleanText: String
                    val firstPipeIndex = rawPayload.indexOf('|')

                    if (firstPipeIndex != -1) {
                        mid = rawPayload.substring(0, firstPipeIndex)
                        cleanText = rawPayload.substring(firstPipeIndex + 1)
                    } else {
                        mid = "sys-${rawPayload.hashCode()}"
                        cleanText = rawPayload
                    }


                    if (safeBroadcasts.containsKey(mid)) return
                    safeBroadcasts.put(mid, true)

                    appContext?.let { ctx ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = DatabaseManager.getDao()
                            val msgDao = DatabaseManager.getMsgDao()

                            if (msgDao.checkExists(mid) == 0) {
                                val existingSlot = dao.getSlotById("1.4")
                                if (existingSlot == null) {
                                    dao.insertSlot(SlotEntity(
                                        id = "1.4",
                                        status = "ACTIVE",
                                        partnerDisplayName = "Инфо",
                                        partnerId = "ALL"
                                    ))
                                }

                                msgDao.insert(MessageEntity(
                                    msgId = mid, slotId = "1.4", text = cleanText,
                                    status = "DELIVERED", timestamp = System.currentTimeMillis(),
                                    isMine = IdentityManager.isSystemInfo()
                                ))
                            }
                        }
                    }
                    _serverMessageFlow.tryEmit(text)
                    return
                }


                if (checkText.startsWith("[SYSTEM_PING]|")) {
                    val parts = checkText.split("|")
                    val sender = parts.getOrNull(1)?.trim() ?: "Unknown"
                    val intention = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() } ?: "now"
                    val channelId = parts.getOrNull(3)?.trim() ?: ""

                    appContext?.let { ctx ->
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                if (channelId.isNotEmpty()) {
                                    var partnerName = ""
                                    var isMuted = false
                                    var slotId = ""
                                    var loadedFromDb = false

                                    // 1. Пробуем безопасно прочесть данные из шифрованной базы
                                    try {
                                        val slot = DatabaseManager.getDao().getSlotByChannelId(channelId)
                                        if (slot != null) {
                                            DatabaseManager.getDao().setHasActivePing(slot.id, true)
                                            partnerName = slot.partnerDisplayName
                                            isMuted = slot.isMuted
                                            slotId = slot.id
                                            loadedFromDb = true
                                        }
                                    } catch (dbError: Exception) {
                                        Log.e("NETWORK", "База заблокирована. Переключаемся на резервный кэш...")
                                    }

                                    // 2. Резервный сценарий: если база закрыта, берем данные из нешифрованного кэша
                                    if (!loadedFromDb) {
                                        val cachePrefs = ctx.getSharedPreferences("unencrypted_slots_cache", Context.MODE_PRIVATE)
                                        partnerName = cachePrefs.getString("name_$channelId", "") ?: ""
                                        isMuted = cachePrefs.getBoolean("muted_$channelId", false)
                                        slotId = cachePrefs.getString("slot_id_$channelId", "") ?: ""
                                    }

                                    // 3. Если слот успешно найден (в базе или в кэше) — отправляем PUSH в шторку
                                    if (slotId.isNotEmpty()) {
                                        if (AppSettings.isBackgroundModeEnabled(ctx) && !isMuted) {
                                            // Задействуем готовые функции перевода из L10n
                                            val pushText = if (intention == "now") {
                                                L10n.notificationText(partnerName, slotId)
                                            } else {
                                                L10n.notificationTextWithTime(partnerName, slotId, intention)
                                            }
                                            com.example.ordo.NotificationHelper.showPingNotification(ctx, "🔴 $pushText")
                                        }
                                    } else {
                                        Log.e("NETWORK", "Слот не найден ни в БД, ни в кэше для channelId: $channelId")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("NETWORK", "Ошибка обработки входящего пинга: ${e.message}")
                            }
                        }
                    }
                    _serverMessageFlow.tryEmit(text)
                    return
                }

                if (text.startsWith("[REQUEST_PROFILE]")) {
                    _serverMessageFlow.tryEmit(text)
                    return
                }

                val cleanText = try {
                    val json = JsonParser.parseString(text).asJsonObject

                    if (json.has("command") && json.get("command").asString == "PROFILE_DATA") {
                        val payload = JsonParser.parseString(json.get("payload").asString).asJsonObject
                        val name = payload.get("name").asString
                        val avatar = payload.get("avatar").asString
                        "PROFILE_DATA|$name|$avatar"
                    } else if (json.has("text")) {
                        if (json.has("msgId")) {
                            val mid = json.get("msgId").asString
                            "MSG|$mid|${json.get("text").asString}"
                        } else {
                            json.get("text").asString
                        }
                    } else text
                } catch (e: Exception) { text }

                messageFlow.tryEmit(cleanText)
                _serverMessageFlow.tryEmit(cleanText)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (connectionId != currentConnectionId.get()) return

                _statusFlow.value = "ПОИСК СЕТИ..."
                CoroutineScope(Dispatchers.IO).launch { DatabaseManager.getMsgDao().resetStuckMessages() }
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (connectionId != currentConnectionId.get()) return

                _statusFlow.value = "ОТКЛЮЧЕНО"
                CoroutineScope(Dispatchers.IO).launch { DatabaseManager.getMsgDao().resetStuckMessages() }
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (isReconnecting) return
        isReconnecting = true
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("NETWORK", "Ожидание реконнекта: ${reconnectDelay / 1000} сек...")
            delay(reconnectDelay)

            reconnectDelay = (reconnectDelay * 2).coerceAtMost(30000L)

            isReconnecting = false
            if (currentUrl.isNotEmpty()) {
                connect(currentUrl)
            }
        }
    }

    fun sendCommand(command: String, payloadMap: Map<String, String>) {
        val ws = webSocket ?: return

        val finalPayloadMap = payloadMap.toMutableMap()
        val syncedTime = System.currentTimeMillis() + timeOffset
        finalPayloadMap["_ts"] = syncedTime.toString()

        val payloadJson = gson.toJson(finalPayloadMap)
        val envelope = WsEnvelope(
            command = command,
            pubKey = IdentityManager.getFullPublicKey(),
            payload = payloadJson,
            signature = IdentityManager.sign(payloadJson)
        )
        ws.send(gson.toJson(envelope))
    }

    fun changeServer(newUrl: String) {
        val targetUrl = if (newUrl.isBlank()) DEFAULT_SERVER else newUrl
        if (currentUrl == targetUrl) return // Мы уже находимся на этой частоте

        android.util.Log.d("NETWORK", "Переключение частоты: $targetUrl")
        currentUrl = targetUrl
        reconnectDelay = 2000L


        webSocket?.cancel()
        webSocket = null


        isReconnecting = false
        connect(currentUrl)
    }


    fun disconnect() {
        currentUrl = ""
        reconnectDelay = 2000L
        isReconnecting = false
        webSocket?.cancel()
        webSocket = null
        _statusFlow.value = "ОТКЛЮЧЕНО"
    }
}
