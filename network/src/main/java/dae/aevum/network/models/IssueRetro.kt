package dae.aevum.network.models

data class SearchRequestRetro(
    val jql: String,
    val fields: List<String> = listOf("id", "summary")
)

data class SearchRetro(
    val startAt: Int,
    val maxResults: Int,
    val total: Int,
    val issues: List<IssueRetro>
)

data class IssueRetro(
    val id: String,
    val key: String,
    val fields: IssueFieldsRetro
)

data class IssueFieldsRetro(
    val summary: String
)