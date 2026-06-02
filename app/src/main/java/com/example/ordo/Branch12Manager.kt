package com.example.ordo

import androidx.compose.runtime.*

class Branch12State {
    var activeFlyoutNodeId by mutableStateOf<String?>(null)
    var currentPlusSlotIndex by mutableStateOf(1)

    fun toggleMenu(nodeId: String, slot: Int) {
        if (activeFlyoutNodeId == nodeId) {
            activeFlyoutNodeId = null
        } else {
            activeFlyoutNodeId = nodeId
            currentPlusSlotIndex = slot
        }
    }

    fun closeMenu() {
        activeFlyoutNodeId = null
    }
}

fun isBranch12Node(node: HexNode): Boolean = node.id.startsWith("1.2")

fun getPlusSlotIndex(plusNode: HexNode): Int {
    val parent = plusNode.parent ?: return 1
    val realChildrenCount = parent.children.count { !it.isAddButton }

    // Сохраняем оригинальную механику: если папка совсем пустая, меню красиво вылетает из центра
    if (realChildrenCount == 0) {
        return 1
    }

    // В остальных случаях вычисляем точный слот на основе индекса плюса в списке детей
    val index = parent.children.indexOf(plusNode)
    return when (index) {
        0 -> 2 // Первый дочерний слот (Slot 2)
        1 -> 3 // Второй дочерний слот (Slot 3)
        2 -> 4 // Третий дочерний слот (Slot 4)
        3 -> 5 // Четвертый дочерний слот (Slot 5)
        else -> 1
    }
}