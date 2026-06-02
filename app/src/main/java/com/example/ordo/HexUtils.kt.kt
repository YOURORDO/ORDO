package com.example.ordo

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- ЛОГИКА УПРАВЛЯЕМОГО ФРАКТАЛА ---

/**
 * Создает начальное дерево с заданной структурой:
 * Корень (1) -> 3 ребенка (1.1, 1.2, 1.3) -> у каждого по 3 ребенка (1.1.1 и т.д.)
 */
fun generateTree(depth: Int = 2): HexNode {
    // Цвета для уровней (можно настроить по вкусу)
    val levelColors = listOf(
        Color(0xFF2196F3), // Уровень 0 (Root)
        Color(0xFF4CAF50), // Уровень 1
        Color(0xFFFFC107), // Уровень 2
        Color(0xFFFF5722)  // Уровень 3
    )

    fun createNodeRecursive(id: String, currentLevel: Int, parent: HexNode?): HexNode {
        val nodeColor = levelColors.getOrElse(currentLevel) {
            // Если уровней больше, чем цветов, генерируем случайный
            Color(Random.nextInt(100, 255) / 255f, Random.nextInt(100, 255) / 255f, Random.nextInt(100, 255) / 255f, 1f)
        }

        val node = HexNode(
            id = id,
            name = if (id == "1") "ORDO" else id,
            color = nodeColor,
            level = currentLevel,
            parent = parent
        )

        val childrenList = mutableListOf<HexNode>()

        // Если мы не достигли предела начальной генерации (2 уровень вложенности)
        if (currentLevel < depth) {
            for (i in 1..3) {
                val childId = "$id.$i"
                childrenList.add(createNodeRecursive(childId, currentLevel + 1, node))
            }
        }

        // ВСЕГДА добавляем кнопку "+" в конец списка детей каждого узла
        val addButton = HexNode(
            id = "$id.add",
            name = "+",
            color = Color.Gray,
            level = currentLevel + 1,
            parent = node,
            isAddButton = true
        )
        childrenList.add(addButton)

        node.children = childrenList
        return node
    }

    // Начинаем генерацию с корня "1"
    return createNodeRecursive("1", 0, null)
}

/**
 * Функция для динамического добавления нового узла пользователем.
 * @param parent родительский узел
 * @param type тип создаваемого узла (STANDARD, CHAT, CHANNEL)
 */
fun addNewChildTo(parent: HexNode, type: HexNodeType = HexNodeType.STANDARD, forcedIndex: Int? = null) {
    // Список текущих реальных узлов (без учета кнопки +)
    val realChildren = parent.children.filter { !it.isAddButton }
    // Если передан целевой индекс пропуска — используем его, иначе рассчитываем в конец списка
    val nextIndex = forcedIndex ?: (realChildren.size + 1)
    val newId = "${parent.id}.$nextIndex"

    val newNode = HexNode(
        id = newId,
        name = newId,
        color = Color.White,
        level = parent.level + 1,
        parent = parent,
        nodeType = type
    )

    val addButtonForNewNode = HexNode(
        id = "$newId.add",
        name = "+",
        color = Color.Gray,
        level = newNode.level + 1,
        parent = newNode,
        isAddButton = true
    )
    newNode.children = listOf(addButtonForNewNode)

    parent.children = realChildren + newNode + parent.children.filter { it.isAddButton }

    // АВТОСОХРАНЕНИЕ: Отправляем новорожденную соту прямо в базу данных Room на физический диск!
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val nodeEntity = NodeEntity(
                id = newNode.id,
                name = newNode.name,
                nodeType = newNode.nodeType.name // Сохраняем имя типа: "STANDARD" или "CHAT"
            )
            DatabaseManager.getNodeDao().insertNode(nodeEntity)
        } catch (e: Exception) {
            android.util.Log.e("HEX_UTILS", "Критическая ошибка автосохранения соты: ${e.message}")
        }
    }
}
// Вспомогательный рекурсивный метод для поиска узла соты по его уникальному ID
fun findNodeById(root: HexNode, targetId: String): HexNode? {
    if (root.id == targetId) return root
    for (child in root.children) {
        val found = findNodeById(child, targetId)
        if (found != null) return found
    }
    return null
}

// ГЛАВНЫЙ РЕКОНСТРУКТОР: Воссоздает всё дерево фрактала на основе плоской таблицы из базы данных
fun reconstructTree(
    dbNodes: List<NodeEntity>,
    slots: List<SlotEntity>,
    isCreator: Boolean,
    hasSystemChat: Boolean
): HexNode {
    // Строим неизменяемый корневой узел, считывая имя из L10n
    val root = HexNode(
        id = "1",
        name = L10n.introFinal, // Будет автоматически "ПОРЯДОК" или "ORDO"
        color = Color.White,
        level = 0,
        parent = null
    )

    // 3. Строим три спящие соты первого уровня (СТАЛО)
    val node11 = HexNode("1.1", "1.1", Color.White, 1, root)
    val node12 = HexNode("1.2", "1.2", Color.White, 1, root)
    val node13 = HexNode("1.3", "1.3", Color.White, 1, root)

    // Даем им базовые кнопки "+"
    node11.children = listOf(HexNode("1.1.add", "+", Color.Gray, 2, node11, isAddButton = true))
    node12.children = listOf(HexNode("1.2.add", "+", Color.Gray, 2, node12, isAddButton = true))
    node13.children = listOf(HexNode("1.3.add", "+", Color.Gray, 2, node13, isAddButton = true))

    val rootChildren = mutableListOf(node11, node12, node13)


    if (isCreator || hasSystemChat) {
        val node14 = HexNode(
            id = "1.4",
            name = "Info",
            color = Color.White,
            level = 1,
            parent = root,
            isAddButton = false,
            photoId = R.drawable.f_ordo,
            nodeType = HexNodeType.CHAT
        )

        if (isCreator) {
            node14.children = listOf(HexNode("1.4.add", "+", Color.Gray, 2, node14, isAddButton = true))
        }
        rootChildren.add(node14)
    }

    root.children = rootChildren


    val sortedDbNodes = dbNodes.sortedBy { it.id.length }

    sortedDbNodes.forEach { entity ->

        val parentId = if (entity.id.contains('.')) entity.id.substringBeforeLast('.') else "1"


        val parentNode = findNodeById(root, parentId)

        if (parentNode != null) {
            val level = entity.id.split('.').size - 1


            val displayName = if (HexNodeType.valueOf(entity.nodeType) == HexNodeType.CHAT) {
                val correspondingSlot = slots.find { it.id == entity.id }
                if (correspondingSlot != null && correspondingSlot.partnerDisplayName.isNotBlank()) {
                    correspondingSlot.partnerDisplayName
                } else {
                    entity.name
                }
            } else {
                entity.name
            }

            val newNode = HexNode(
                id = entity.id,
                name = displayName, // Используем вычисленное имя (СТАЛО)
                color = Color.White,
                level = level,
                parent = parentNode,
                nodeType = HexNodeType.valueOf(entity.nodeType)
            )

            // Если это папка (STANDARD), даем ей собственную кнопку "+"
            if (newNode.nodeType == HexNodeType.STANDARD) {
                newNode.children = listOf(
                    HexNode("${newNode.id}.add", "+", Color.Gray, level + 1, newNode, isAddButton = true)
                )
            }

            // Прикрепляем новорожденный узел к родителю, сохраняя кнопку "+" на самом последнем месте!
            val parentRealChildren = parentNode.children.filter { !it.isAddButton }
            val parentAddButton = parentNode.children.find { it.isAddButton } ?: HexNode("${parentNode.id}.add", "+", Color.Gray, parentNode.level + 1, parentNode, isAddButton = true)

            parentNode.children = parentRealChildren + newNode + parentAddButton
        }
    }

    // === НАШ НОВЫЙ АЛГОРИТМ ЗАПОЛНЕНИЯ ПУСТОТ ===
    fillGapsRecursively(root)

    return root
}
/**
 * Рекурсивная функция, которая сканирует всё дерево навигации, находит пропуски в индексах
 * и размещает на их местах интерактивные плюсы.
 */
private fun fillGapsRecursively(node: HexNode) {
    // Рекурсивно обрабатываем детей на всех уровнях вложенности в глубину
    val currentChildren = node.children
    currentChildren.forEach { fillGapsRecursively(it) }

    // ИСКЛЮЧЕНИЕ ДЛЯ КОРНЯ: Мы никогда не генерируем плюсы и не заполняем пустоты для корневого узла "1"
    if (node.id == "1") return

    // Нам интересны только реальные дочерние соты
    val realChildren = node.children.filter { !it.isAddButton }
    if (realChildren.isNotEmpty()) {
        val indices = realChildren.mapNotNull { child ->
            // Извлекаем последний индекс из ID соты (например, из "1.2.3" извлекаем 3)
            child.id.substringAfterLast('.').toIntOrNull()
        }

        if (indices.isNotEmpty()) {
            val maxIndex = indices.maxOrNull() ?: 1
            val newChildrenList = mutableListOf<HexNode>()
            var hasGaps = false

            // Циклом проверяем каждый индекс от 1 до максимального существующего
            for (i in 1..maxIndex) {
                val existingChild = realChildren.find { child ->
                    child.id.substringAfterLast('.').toIntOrNull() == i
                }

                if (existingChild != null) {
                    newChildrenList.add(existingChild)
                } else {
                    // Гнездо пустое! Создаем интерактивный плюс для заполнения этого пропуска
                    val gapPlusNode = HexNode(
                        id = "${node.id}.$i.add",
                        name = "+",
                        color = Color.Gray,
                        level = node.level + 1,
                        parent = node,
                        isAddButton = true
                    )
                    newChildrenList.add(gapPlusNode)
                    hasGaps = true
                }
            }

            // Если пропусков в ряду не было — выводим стандартный плюс в самый конец
            if (!hasGaps) {
                val endPlusNode = HexNode(
                    id = "${node.id}.${maxIndex + 1}.add",
                    name = "+",
                    color = Color.Gray,
                    level = node.level + 1,
                    parent = node,
                    isAddButton = true
                )
                newChildrenList.add(endPlusNode)
            }

            node.children = newChildrenList
        }
    }
}