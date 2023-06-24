package dae.aevum.ui.composables.reusable

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

fun highlightText(
    text: String,
    query: String = "",
    span: SpanStyle
): AnnotatedString {
    return buildAnnotatedString {
        var start = 0
        while (text.indexOf(query, start, ignoreCase = true) != -1 && query.isNotBlank()) {
            val firstIndex = text.indexOf(query, start, true)
            val end = firstIndex + query.length
            append(text.substring(start, firstIndex))
            withStyle(style = span) {
                append(text.substring(firstIndex, end))
            }
            start = end
        }
        append(text.substring(start, text.length))
        toAnnotatedString()
    }
}