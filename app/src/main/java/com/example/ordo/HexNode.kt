package com.example.ordo


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class HexNodeType {
    STANDARD, CHAT, CHANNEL

}

class HexNode(
    val id: String,
    var name: String, // Изменяемая переменная (теперь можно переименовывать!)
    val color: Color,
    val level: Int,
    val parent: HexNode? = null,
    val isAddButton: Boolean = false,
    val photoId: Int? = null,
    val emoji: String? = null,
    var nodeType: HexNodeType = HexNodeType.STANDARD // Добавь это поле в конструктор
) {
    var children: List<HexNode> by mutableStateOf(emptyList())
}