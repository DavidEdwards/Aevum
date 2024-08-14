package dae.aevum.ui.composables.reusable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

fun highlightSpanStyle(colors: HighlightColors) = SpanStyle(
    color = colors.color,
    fontWeight = FontWeight.SemiBold,
    background = colors.background
)

data class HighlightColors(
    val color: Color, // MaterialTheme.colorScheme.onPrimaryContainer
    val background: Color // MaterialTheme.colorScheme.primaryContainer
)