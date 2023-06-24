package dae.aevum.ui.models

import androidx.compose.ui.text.AnnotatedString
import dae.aevum.database.entities.IssueEntity
import dae.aevum.ui.composables.reusable.HighlightColors
import dae.aevum.ui.composables.reusable.highlightSpanStyle
import dae.aevum.ui.composables.reusable.highlightText
import dae.aevum.utils.IssueId

data class IssueUiModel(
    val id: IssueId,
    val title: AnnotatedString,
    val pinned: Boolean
) {
    companion object {
        fun fromEntity(
            entity: IssueEntity,
            highlight: String? = null,
            pinned: Boolean = false,
            highlightColors: HighlightColors? = null,
        ): IssueUiModel {
            val highlightAnnotated = if (highlight != null && highlightColors != null) {
                highlightText(
                    text = entity.title,
                    query = highlight,
                    span = highlightSpanStyle(highlightColors)
                )
            } else {
                AnnotatedString(entity.title)
            }

            return IssueUiModel(
                id = entity.id,
                title = highlightAnnotated,
                pinned = pinned
            )
        }
    }
}