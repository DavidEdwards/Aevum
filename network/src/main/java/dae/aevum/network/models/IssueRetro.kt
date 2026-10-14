package dae.aevum.network.models

data class SearchRetro(
    val startAt: Int,
    val maxResults: Int,
    val total: Int,
    val issues: List<IssueRetro>,
)

data class IssueRetro(
    val id: String,
    val key: String,
    val fields: IssueFieldsRetro,
)

data class IssueFieldsRetro(
    val summary: String,
)

data class IssuePickerSuggestionsRetro(
    val sections: List<IssuePickerSectionRetro> = emptyList(),
)

data class IssuePickerSectionRetro(
    val id: String? = null,
    val label: String? = null,
    val issues: List<IssuePickerIssueRetro> = emptyList(),
)

data class IssuePickerIssueRetro(
    val id: Long? = null,
    val key: String,
    val summary: String? = null,
    val summaryText: String? = null,
)
