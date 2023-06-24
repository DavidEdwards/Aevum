package dae.aevum.network.models

import androidx.annotation.Keep

@Keep
data class SearchRequestRetro(
    val jql: String,
    val fields: List<String> = listOf("id", "summary")
)

@Keep
data class SearchRetro(
    val startAt: Int,
    val maxResults: Int,
    val total: Int,
    val issues: List<IssueRetro>
)

@Keep
data class IssueRetro(
    val id: String,
    val key: String,
    val fields: IssueFieldsRetro
)

@Keep
data class IssueFieldsRetro(
    val summary: String
)