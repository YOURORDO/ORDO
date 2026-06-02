package com.example.ordo

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object MarkdownHelper {

    fun toggleStyle(current: TextFieldValue, tag: String): TextFieldValue {
        val selection = current.selection
        if (selection.collapsed) return current

        val text = current.text
        val selectedText = text.substring(selection.start, selection.end)

        // Определяем, какой стиль мы сейчас хотим применить
        // Если это [big], то open/close разные, если B/I/U — одинаковые
        val open = if (tag.contains("...")) tag.split("...")[0] else tag
        val close = if (tag.contains("...")) tag.split("...")[1] else tag

        val newText: String
        val newSelection: TextRange

        // ПРОВЕРКА: Если текст УЖЕ обернут в этот тег — СНИМАЕМ СТИЛЬ
        if (selectedText.startsWith(open) && selectedText.endsWith(close)) {
            val content = selectedText.substring(open.length, selectedText.length - close.length)
            newText = text.substring(0, selection.start) + content + text.substring(selection.end)
            newSelection = TextRange(selection.start, selection.start + content.length)
        }
        // ИНАЧЕ — НАКИДЫВАЕМ СТИЛЬ
        else {
            newText = text.substring(0, selection.start) + open + selectedText + close + text.substring(selection.end)
            newSelection = TextRange(
                selection.start + open.length,
                selection.start + open.length + selectedText.length
            )
        }

        return TextFieldValue(text = newText, selection = newSelection)
    }
}