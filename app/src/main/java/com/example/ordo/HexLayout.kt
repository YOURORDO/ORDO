package com.example.ordo

import androidx.compose.ui.geometry.Offset

fun calculateTargetProps(
    node: HexNode,
    activeNode: HexNode,
    rootNode: HexNode,
    isStarted: Boolean,
    currentScroll: Float,
    dragOffset: Offset,
    isDragging: Boolean
): RenderProps {
    if (!isStarted) {
        return if (node.id == rootNode.id) HexSlots.StartButton
        else HexSlots.StartButton.copy(size = 0, alpha = 0f)
    }

    val surfaceTension = 0.18f
    val effectiveDrag = if (isDragging) dragOffset else Offset.Zero

    if (node.id == rootNode.id && activeNode.id != rootNode.id) {
        val ambientX = (effectiveDrag.x * surfaceTension).toInt()
        val ambientY = (effectiveDrag.y * surfaceTension).toInt()
        return HexSlots.ParentLeft.copy(x = HexSlots.ParentLeft.x + ambientX, y = HexSlots.ParentLeft.y + ambientY)
    }
    if (node == activeNode) {
        val targetScale = if (isDragging) 1.15f else 1.0f
        val newSize = (HexSlots.Active.size * targetScale).toInt()
        return HexSlots.Active.copy(
            x = HexSlots.Active.x + dragOffset.x.toInt(),
            y = HexSlots.Active.y + dragOffset.y.toInt(),
            size = newSize
        )
    }
    if (activeNode.children.contains(node)) {
        val index = activeNode.children.indexOf(node)
        val base = HexPath.interpolate(index.toFloat() - currentScroll)
        val ambientX = (effectiveDrag.x * surfaceTension).toInt()
        val ambientY = (effectiveDrag.y * surfaceTension).toInt()
        return base.copy(x = base.x + ambientX, y = base.y + ambientY)
    }
    if (node.parent == activeNode.parent && node.parent != null) {
        val siblings = node.parent.children.filter { it != activeNode }
        val index = siblings.indexOf(node)
        val base = if (index < 2) HexSlots.getSiblingProps(index) else RenderProps(320, 2442, 0, 0f)
        val ambientX = (effectiveDrag.x * surfaceTension).toInt()
        val ambientY = (effectiveDrag.y * surfaceTension).toInt()
        return base.copy(x = base.x + ambientX, y = base.y + ambientY)
    }
    if (node == activeNode.parent) return HexSlots.ParentLeft
    return RenderProps(320, 2160, 0, 0f)
}

fun calculateTextProps(node: HexNode, activeNode: HexNode, currentScroll: Float): RenderProps {
    if (activeNode.children.contains(node)) {
        val index = activeNode.children.indexOf(node)
        return HexPath.interpolateText(index.toFloat() - currentScroll)
    }
    return RenderProps(0, 0, 0, 0f)
}