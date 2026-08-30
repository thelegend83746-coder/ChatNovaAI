package com.chatnova.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = parseMarkdownBlocks(markdown)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Code -> {
                    CodeBlockView(
                        code = block.code,
                        language = block.language
                    )
                }
                is MarkdownBlock.Header -> {
                    Text(
                        text = buildAnnotatedStringFromInline(block.text),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "• ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = buildAnnotatedStringFromInline(block.text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotatedStringFromInline(block.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Header(val text: String, val level: Int) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class Code(val code: String, val language: String?) : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var inCodeBlock = false
    var codeLanguage: String? = null
    val codeBuilder = StringBuilder()
    val paragraphBuilder = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuilder.isNotEmpty()) {
            val content = paragraphBuilder.toString().trim()
            if (content.isNotEmpty()) {
                result.add(MarkdownBlock.Paragraph(content))
            }
            paragraphBuilder.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // End of code block
                result.add(MarkdownBlock.Code(codeBuilder.toString().trimEnd(), codeLanguage))
                codeBuilder.clear()
                inCodeBlock = false
                codeLanguage = null
            } else {
                // Start of code block
                flushParagraph()
                inCodeBlock = true
                codeLanguage = trimmed.removePrefix("```").trim().ifEmpty { null }
            }
        } else if (inCodeBlock) {
            codeBuilder.append(line).append("\n")
        } else {
            when {
                trimmed.startsWith("### ") -> {
                    flushParagraph()
                    result.add(MarkdownBlock.Header(trimmed.removePrefix("### "), 3))
                }
                trimmed.startsWith("## ") -> {
                    flushParagraph()
                    result.add(MarkdownBlock.Header(trimmed.removePrefix("## "), 2))
                }
                trimmed.startsWith("# ") -> {
                    flushParagraph()
                    result.add(MarkdownBlock.Header(trimmed.removePrefix("# "), 1))
                }
                trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                    flushParagraph()
                    result.add(MarkdownBlock.BulletItem(trimmed.substring(2)))
                }
                trimmed.isEmpty() -> {
                    flushParagraph()
                }
                else -> {
                    if (paragraphBuilder.isNotEmpty()) paragraphBuilder.append(" ")
                    paragraphBuilder.append(line)
                }
            }
        }
    }

    if (inCodeBlock) {
        result.add(MarkdownBlock.Code(codeBuilder.toString(), codeLanguage))
    }
    flushParagraph()

    return if (result.isEmpty()) listOf(MarkdownBlock.Paragraph(text)) else result
}

fun buildAnnotatedStringFromInline(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            } else if (text.startsWith("`", i)) {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x33888888),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            } else if (text.startsWith("*", i)) {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}
