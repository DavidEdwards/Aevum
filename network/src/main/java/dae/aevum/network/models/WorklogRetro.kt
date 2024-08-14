package dae.aevum.network.models

data class WorklogListRetro(
    val startAt: Int,
    val maxResults: Int,
    val total: Int,
    val worklogs: List<WorklogRetro>
)

data class WorklogRetro(
    val author: AuthorRetro,
    val comment: CommentNodeRetro?,
    val started: String,
    val timeSpentSeconds: Long,
    val id: String,
    val issueId: String
)

data class AuthorRetro(
    val displayName: String,
    val accountId: String
)

data class CommentNodeRetro(
    val version: Int? = null,
    val type: String,
    val text: String? = null,
    val content: List<CommentNodeRetro>? = null
) {
    fun generateUnifiedString(builder: StringBuilder = StringBuilder()): String {
        if (!text.isNullOrBlank()) {
            builder.append(text)
        }
        content?.forEach { commentNode ->
            commentNode.generateUnifiedString(builder)
        }

        return builder.toString()
    }

    companion object {
        fun generateFromString(comment: String): CommentNodeRetro {
            return CommentNodeRetro(
                version = 1,
                type = "doc",
                content = listOf(
                    CommentNodeRetro(
                        type = "paragraph",
                        content = listOf(
                            CommentNodeRetro(
                                type = "text",
                                text = comment
                            )
                        )
                    )
                )
            )
        }
    }
}

data class WorklogAddRetro(
    /*
    "comment": {
        "content": [
          {
            "content": [
              {
                "text": "I did some work here.",
                "type": "text"
              }
            ],
            "type": "paragraph"
          }
        ],
        "type": "doc",
        "version": 1
      },
     */
    val comment: CommentNodeRetro,

    /* "started": "2021-01-17T12:34:00.000+0000", */
    val started: String,

    /* "timeSpentSeconds": 12000, */
    val timeSpentSeconds: Int
)