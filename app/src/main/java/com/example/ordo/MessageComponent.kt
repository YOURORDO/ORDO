package com.example.ordo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val myBubbleColor = Color(0xFF98b7fe).copy(alpha = 0.2f)
val peerBubbleColor = Color(0xFF7186b8).copy(alpha = 0.2f)
val glassEdgeColor = Color.White.copy(alpha = 0.22f)
val glassThickness = 1.dp
val cutSize = 8.dp
val shadowElevation = 2.dp
val shadowColor = Color.Black.copy(alpha = 0.9f)
val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageList(
    messages: List<MessageEntity>,
    state: LazyListState,
    aesKey: String,
    channelId: String,
    myEtherOn: Boolean,
    isPartnerOnline: Boolean,
    onEditStart: (MessageEntity) -> Unit,
    editingMsgId: String?,
    editingText: androidx.compose.ui.text.input.TextFieldValue,
    onEditingTextChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onEditCancel: () -> Unit,
    onEditSave: (String, String) -> Unit,
    contextMenuMsgId: String?,                      // НОВОЕ: Глобальный ID открытого меню
    onContextMenuChange: (String?) -> Unit          // НОВОЕ: Функция для смены меню
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().graphicsLayer(clip = false),
        contentPadding = PaddingValues(top = 140.dp, bottom = 90.dp, start = 10.dp, end = 10.dp)
    ) {
        // Ключ снова стабилен на 100%! Вся анимация сползания старых сообщений заработает идеально плавно
        items(items = messages, key = { it.msgId }) { msg ->
            // Теперь состояние привязано к конкретному ID сообщения и не перепутается при скролле
            val isNew = remember(msg.msgId) { (System.currentTimeMillis() - msg.timestamp) < 2000 }

            val offsetY = remember(msg.msgId) { androidx.compose.animation.core.Animatable(if (isNew) -3000f else 0f) }

            // Запускаем физику падения
            LaunchedEffect(msg.msgId) {
                if (isNew) {
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.9f, // мягкая пружинка в конце
                            stiffness = 100f
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .animateItem() // Раздвигает старые сообщения, освобождая слот
                    .graphicsLayer {
                        translationY = offsetY.value // А само облако физически летит в этот слот сверху
                    }
            ) {
                MessageItem(msg, aesKey, myEtherOn, isPartnerOnline, onEditStart, editingMsgId, editingText, onEditingTextChange, onEditCancel, onEditSave, contextMenuMsgId, onContextMenuChange)
            }
        }
    }
}

@Composable
fun FormatToolbar(
    currentText: String,
    selection: TextRange,
    onTagClick: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        val tags = listOf("B" to "**", "I" to "*", "U" to "__", "Big" to "[big]...[/big]")
        tags.forEach { (label, tag) ->
            val isEnabled = StyleRules.canApply(currentText, selection, tag)
            TextButton(
                onClick = { onTagClick(tag) },
                enabled = isEnabled,
                modifier = Modifier.padding(2.dp).height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(label, color = if (isEnabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MessageItem(
    msg: MessageEntity, aesKey: String, myEtherOn: Boolean, isPartnerOnline: Boolean,
    onEditStart: (MessageEntity) -> Unit, editingMsgId: String?,
    editingText: androidx.compose.ui.text.input.TextFieldValue,
    onEditingTextChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onEditCancel: () -> Unit, onEditSave: (String, String) -> Unit,
    contextMenuMsgId: String?, onContextMenuChange: (String?) -> Unit
) {
    val isMine = msg.isMine
    val showMenu = (contextMenuMsgId == msg.msgId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Row теперь чистый и не перехватывает клики с пустого места
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        MessageBubble(
            msg = msg,
            isMine = isMine,
            showMenu = showMenu,
            onDismissMenu = { onContextMenuChange(null) },
            onEditStart = onEditStart,
            editingMsgId = editingMsgId,
            editingText = editingText,
            onEditingTextChange = onEditingTextChange,
            onEditCancel = onEditCancel,
            onEditSave = onEditSave,
            aesKey = aesKey,
            myEtherOn = myEtherOn,
            isPartnerOnline = isPartnerOnline,
            onLongClick = { onContextMenuChange(msg.msgId) } // Передаем событие долгого тапа внутрь баббла
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    msg: MessageEntity, isMine: Boolean, showMenu: Boolean, onDismissMenu: () -> Unit,
    onEditStart: (MessageEntity) -> Unit, editingMsgId: String?,
    editingText: androidx.compose.ui.text.input.TextFieldValue,
    onEditingTextChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onEditCancel: () -> Unit, onEditSave: (String, String) -> Unit,
    aesKey: String, myEtherOn: Boolean, isPartnerOnline: Boolean,
    onLongClick: () -> Unit // Добавили новый коллбэк для обработки долгого клика
) {
    val bubbleShape = if (isMine) CutCornerShape(topStart = cutSize, bottomStart = cutSize, bottomEnd = cutSize, topEnd = 0.dp)
    else CutCornerShape(topEnd = cutSize, bottomEnd = cutSize, bottomStart = cutSize, topStart = 0.dp)

    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .shadow(elevation = shadowElevation, shape = bubbleShape)
            .border(glassThickness, glassEdgeColor, bubbleShape)
            .background(color = if (isMine) myBubbleColor else peerBubbleColor, shape = bubbleShape)
            // Долгое нажатие теперь работает исключительно по площади самого облака сообщения!
            .combinedClickable(
                onClick = onDismissMenu, // Обычный клик закрывает меню
                onLongClick = onLongClick
            )
            .padding(12.dp)
    ) {
        // === 1. ВЕРНУЛИ УДАЛЕННЫЙ ТЕКСТ СООБЩЕНИЯ ===
        if (editingMsgId == msg.msgId) {
            SmileText(text = editingText.text, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = editingText, onValueChange = onEditingTextChange,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp)).padding(8.dp)
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                TextButton(onClick = onEditCancel) { Text("ОТМЕНА", color = Color.Gray, fontSize = 12.sp) }
                TextButton(onClick = { onEditSave(msg.msgId, editingText.text) }) { Text("СОХРАНИТЬ", color = Color.Green, fontSize = 12.sp) }
            }
        } else {
            SmileText(text = msg.text, fontSize = 16.sp)
        }

        // === 2. ВЕРНУЛИ УДАЛЕННЫЙ СТАТУС И ВРЕМЯ ===
        Row(
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Если статус содержит счетчик бродкаста (например, "DELIVERED: 15"),
            // красиво выводим его как "✓ 15"
            val displayStatus = if (msg.status.startsWith("DELIVERED:")) {
                "✓ " + msg.status.substringAfter(":")
            } else {
                " [${msg.status}] "
            }

            Text(text = displayStatus, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp)

            // ДОБАВЛЯЕМ ГОРИЗОНТАЛЬНЫЙ ОТСТУП ДЛЯ ЭСТЕТИКИ И ЧИТАЕМОСТИ
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = timeFormat.format(java.util.Date(msg.timestamp)),
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when(msg.status) {
                    "EDITING" -> "⋯"
                    "READY" -> "⚡"
                    "PENDING" -> "💤"
                    "SENDING" -> "↑"
                    "DELIVERED" -> "✓"
                    "FAILED" -> "!"
                    else -> ""
                },
                color = when(msg.status) {
                    "READY" -> Color.Yellow
                    "PENDING" -> Color.Cyan
                    "FAILED" -> Color.Red
                    else -> Color.Gray
                },
                fontSize = 10.sp
            )
        }

        // === 3. НАША УМНАЯ ПАНЕЛЬ С ИКОНКАМИ ===
        if (showMenu) {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            val context = androidx.compose.ui.platform.LocalContext.current

            // ОПРЕДЕЛЯЕМ СТАТУС: Черновик это или уже отправленное/входящее сообщение
            val isDraft = isMine && (msg.status == "EDITING" || msg.status == "READY" || msg.status == "PENDING")

            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {

                // Тонкая линия-разделитель (Стекло)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

                    // ПОКАЗЫВАЕМ ЭТИ КНОПКИ ТОЛЬКО ДЛЯ ЧЕРНОВИКОВ
                    if (isDraft) {
                        // 1. В ЭФИР (Клавиша)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        DatabaseManager.getMsgDao().updateStatus(msg.msgId, "READY")
                                    }
                                    onDismissMenu()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                                contentDescription = "В эфир", tint = Color.Cyan, modifier = Modifier.size(18.dp)
                            )
                        }

                        // 2. ПРАВИТЬ (Клавиша)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onEditStart(msg)
                                    onDismissMenu()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                                contentDescription = "Править", tint = Color.Yellow, modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // А ЭТИ КНОПКИ ПОКАЗЫВАЕМ ВСЕГДА И ДЛЯ ВСЕХ СООБЩЕНИЙ
                    // 3. КОПИРОВАТЬ (Клавиша)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msg.text))
                                android.widget.Toast.makeText(context, "Скопировано", android.widget.Toast.LENGTH_SHORT).show()
                                onDismissMenu()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.ContentCopy,
                            contentDescription = "Копировать", tint = Color.White, modifier = Modifier.size(18.dp)
                        )
                    }

                    // 4. УДАЛИТЬ (Клавиша)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                CoroutineScope(Dispatchers.IO).launch {
                                    DatabaseManager.getMsgDao().delete(msg)
                                }
                                onDismissMenu()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                            contentDescription = "Удалить", tint = Color.Red, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

object StyleRules {
    private val conflicts = mapOf(
        "**" to listOf("__"),
        "*" to listOf("__"),
        "__" to listOf("**", "*"),
        "[big]...[/big]" to listOf()
    )

    fun canApply(text: String, selection: TextRange, newTag: String): Boolean {
        // 1. ЗАЩИТА: Если выделение пустое или индексы кривые — просто разрешаем (или запрещаем)
        if (selection.collapsed || selection.start < 0 || selection.end > text.length || selection.start >= selection.end) {
            return false
        }

        // 2. БЕЗОПАСНЫЙ ПОЛУЧЕНИЕ ТЕКСТА
        val selectedText = try {
            text.substring(selection.start, selection.end)
        } catch (e: Exception) {
            return false
        }

        val activeTags = listOf("**", "*", "__", "[big]...[/big]").filter { selectedText.contains(it.split("...")[0]) }

        if (activeTags.contains(newTag)) return true
        if (activeTags.size >= 2) return false

        val forbidden = conflicts[newTag] ?: emptyList()
        return activeTags.none { it in forbidden }
    }
}