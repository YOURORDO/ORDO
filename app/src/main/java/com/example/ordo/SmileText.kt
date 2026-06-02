package com.example.ordo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun SmileText(
    text: String,
    color: Color = Color.White,
    fontSize: TextUnit = 16.sp
) {
    val finalAnnotated = remember(text, fontSize) {
        // 1. Применяем Markdown
        val styledText = buildAnnotatedString {
            val pattern = "\\*\\*(.*?)\\*\\*|\\*(.*?)\\*|__(.*?)__|\\[big\\](.*?)\\[/big\\]".toRegex()
            var lastIndex = 0

            try {
                pattern.findAll(text).forEach { match ->
                    append(text.substring(lastIndex, match.range.first))
                    val style = when {
                        match.groupValues[1].isNotEmpty() -> SpanStyle(fontWeight = FontWeight.Bold)
                        match.groupValues[2].isNotEmpty() -> SpanStyle(fontStyle = FontStyle.Italic)
                        match.groupValues[3].isNotEmpty() -> SpanStyle(textDecoration = TextDecoration.Underline)
                        match.groupValues[4].isNotEmpty() -> SpanStyle(fontSize = fontSize * 1.5f)
                        else -> null
                    }
                    val content = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
                    if (content != null && style != null) {
                        withStyle(style) { append(content) }
                    } else if (content != null) {
                        append(content)
                    }
                    lastIndex = match.range.last + 1
                }
                if (lastIndex < text.length) append(text.substring(lastIndex))
            } catch (e: Exception) {
                append(text)
            }
        }

        // 2. Накладываем смайлы поверх СТИЛИЗОВАННОГО текста
        buildAnnotatedString {
            var lastIndex = 0
            SmileManager.smilePattern.findAll(styledText.text).forEach { match ->
                append(styledText.subSequence(lastIndex, match.range.first))
                appendInlineContent(match.value, match.value)
                lastIndex = match.range.last + 1
            }
            append(styledText.subSequence(lastIndex, styledText.length))
        }
    }

    // 3. Рисуем
    Text(
        text = finalAnnotated,
        inlineContent = SmileManager.smiles.mapValues { entry ->
            val ratio = SmileManager.getAspectRatio(entry.key)
            InlineTextContent(
                Placeholder(fontSize * 1.6f * ratio, fontSize * 1.6f, PlaceholderVerticalAlign.Center)
            ) {
                AsyncImage(
                    model = "file:///android_asset/smiles/${entry.value}",
                    contentDescription = null,
                    imageLoader = SmileManager.imageLoader,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        color = color,
        fontSize = fontSize,
        style = LocalTextStyle.current
    )
}