package com.example.ordo

import androidx.compose.ui.graphics.Color

/**
 * Единый системный локализатор (словарь) приложения PORYADOK.
 * Хранит все тексты интерфейса на двух языках (RU и EN).
 */
object L10n {
    // Текущий выбранный язык приложения ("ru" или "en").
    // Теперь по умолчанию стоит "en", чтобы соответствовать старту приложения.
    var currentLanguage = "en"

    private fun get(ru: String, en: String): String {
        return if (currentLanguage == "en") en else ru
    }

    // --- БЛОК 1: ПОЛНОЭКРАННЫЕ НАСТРОЙКИ ПРОФИЛЯ ---
    val mainSettingsTitle get() = get("НАСТРОЕК ORDO", "ORDO SETTINGS")
    val yourId get() = get("ТВОЙ ID", "YOUR ID")
    val yourName get() = get("ТВОЁ ИМЯ", "YOUR NAME")
    val currentNodeName get() = get("ИМЯ ТЕКУЩЕГО УЗЛА", "CURRENT NODE NAME")
    val backgroundMode get() = get("ФОНОВЫЙ РЕЖИМ", "BACKGROUND MODE")
    val antennaParking get() = get("ПАРКОВКА АНТЕННЫ (ДЛЯ ПИНГОВ):", "ANTENNA PARKING (FOR PINGS):")
    val exportDna get() = get("ЭКСПОРТ ДНК", "EXPORT DNA")
    val importDna get() = get("ВСТАВИТЬ ДНК", "IMPORT DNA")
    val saveChats get() = get("СОХРАНИТЬ ЧАТЫ", "SAVE CHATS")
    val loadChats get() = get("ЗАГРУЗИТЬ ЧАТЫ", "LOAD CHATS")
    val revealInfo get() = get("ПРОЯВИТЬ ИНФО", "REVEAL INFO")
    val saveAndExit get() = get("СОХРАНИТЬ И ВЫЙТИ", "SAVE AND EXIT")

    val back get() = get("Назад", "Back") // Добавили слово "Назад" / "Back"

    // --- БЛОК 2: НАВИГАЦИОННОЕ МЕНЮ ВЫЛЕТА (ПЛЮС) ---
    val newFolder get() = get("Новый узел", "New Folder")
    val newChat get() = get("Новый чат", "New Chat")
    val newChannel get() = get("Новый канал", "New Channel")

    // --- БЛОК 3: ДИАЛОГ КАТАСТРОФИЧЕСКОГО УДАЛЕНИЯ ---
    val deleteDialogTitle get() = get("⚠️ УНИЧТОЖЕНИЕ УЗЛА", "⚠️ DESTROY NODE")
    fun deleteDialogText(name: String) = get(
        "Вы уверены, что хотите безвозвратно стереть папку «$name» и все вложенные в неё чаты и сообщения?\n\nЭто действие невозможно отменить.",
        "Are you sure you want to permanently erase the folder \"$name\" and all nested chats and messages?\n\nThis action cannot be undone."
    )
    val cancel get() = get("ОТМЕНА", "CANCEL")
    val delete get() = get("УДАЛИТЬ", "DELETE")

    // --- БЛОК 4: СИСТЕМНЫЕ СТАТУСЫ СЕТИ И УВЕДОМЛЕНИЯ (ПУШИ) ---
    val statusOnline get() = get("В СЕТИ", "ONLINE")
    val statusConnecting get() = get("ПОДКЛЮЧЕНИЕ...", "CONNECTING...")

    val pingLeftInAir get() = get("Собеседник недоступен. Пинг будет ждать", "The partner is unavailable. Ping will be pending")

    val pingDeliveredInstantly get() = get("Пинг доставлен", "Ping delivered")
    val statusSearching get() = get("ПОИСК СЕТИ...", "SEARCHING FOR NETWORK...")
    val statusDisconnected get() = get("ОТКЛЮЧЕНО", "DISCONNECTED")

    val notificationTitle get() = get("ПОРЯДОК: Вас вызывает", "ORDO: Calling you")
    fun notificationText(name: String, id: String) = if (name.isNotBlank()) {
        get("$name в #$id", "$name in #$id")
    } else {
        get("в #$id", "in #$id")
    }
    fun notificationTextWithTime(name: String, id: String, time: String) = if (name.isNotBlank()) {
        get("$name в #$id в $time", "$name in #$id at $time")
    } else {
        get("в #$id в $time", "in #$id at $time")
    }

    // --- БЛОК 5: ЭКРАН РИТУАЛА РУКОПОЖАТИЯ ---
    val ritualTitle get() = get("РИТУАЛ #", "RITUAL #")
    val airWaiting get() = get("В Эфире. Ждем партнера...", "In the Air. Waiting for partner...")
    val cancelSearch get() = get("Поиск отменен", "Search canceled")
    val contactEstablished get() = get("КОНТАКТ УСТАНОВЛЕН!", "CONTACT ESTABLISHED!")
    val matchError get() = get("ОШИБКА: Коды не совпали!", "ERROR: Keys mismatched!")
    val scanAnalysis get() = get("Анализ изображения...", "Analyzing image...")
    val scanError get() = get("Не удалось распознать QR!", "Failed to decode QR!")
    val qrExpired get() = get("QR истёк!", "QR expired!")
    val nodeConnection get() = get("УЗЕЛ СВЯЗИ (NODE):", "CONNECTION NODE:")
    val enterIdPlaceholder get() = get("〉〉 введи id 〉〉", "〉〉 enter id 〉〉")
    val enterSecretPlaceholder get() = get("〉〉ваше слово〉〉", "〉〉your word〉〉")
    val letterP get() = get("П", "П") // Буква П (Порядок) на русском, O (Ordo) на английском

    // --- БЛОК 6: ШАПКА ЧАТА (KINETIC HEADER) ---
    val infoChannel get() = get("Инфо", "Info")
    val systemLabel get() = get("Системный", "System")
    val systemReadOnlyLabel get() = get("Системный канал (Только чтение)", "System Channel (Read-Only)")
    val userOnline get() = get("в диалоге", "active")
    val userOffline get() = get("отсутствует", "absent")

    // --- БЛОК 7: ДИАЛОГ С QR-КОДОМ ---
    val qrTitle get() = get("ВАШ КЛЮЧ", "YOUR KEY")
    val invitationFrom get() = get("Приглашение от: ", "Invitation from: ")
    val validUntil get() = get("Действует до: ", "Valid until: ")
    val close get() = get("ЗАКРЫТЬ", "CLOSE")
    val save get() = get("СОХРАНИТЬ", "SAVE")
    val share get() = get("ПОДЕЛИТЬСЯ", "SHARE")
    val qrExpiredLabel get() = get("Действует 30 минут", "Valid for 30 minutes")

    val introYour get() = get("ТВОЙ", "YOUR")
    val introFinal get() = get("ПОРЯДОК", "ORDO")
}
