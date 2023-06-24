package dae.aevum.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dae.aevum.ui.models.IssueUiModel
import dae.aevum.ui.models.UserUiModel
import dae.aevum.ui.models.WorklogUiModel
import java.time.Instant
import java.time.temporal.ChronoUnit

class SampleIssueUiModelProvider : PreviewParameterProvider<IssueUiModel> {
    override val values = sequenceOf(
        IssueUiModel(IssueId("AAPP-0001"), AnnotatedString("Title 1"), true),
        IssueUiModel(IssueId("AAPP-0002"), AnnotatedString("Title 2"), false)
    )
}

class SampleWorklogUiModelProvider : PreviewParameterProvider<WorklogUiModel> {
    override val values = sequenceOf(
        WorklogUiModel(
            workId = 1L,
            issueId = IssueId(value = "AAPP-0001"),
            author = "Name 1",
            from = Instant.now().minus(2, ChronoUnit.HOURS),
            to = Instant.now().minus(1, ChronoUnit.HOURS),
            comment = "Comment 1",
            pending = false,
            active = true
        ),
        WorklogUiModel(
            workId = 2L,
            issueId = IssueId("AAPP-0001"),
            author = "Name 2",
            from = Instant.now().minus(4, ChronoUnit.HOURS),
            to = Instant.now().minus(3, ChronoUnit.HOURS),
            comment = "Comment 2",
            pending = true,
            active = false
        )
    )
}


class SampleUserUiModelProvider : PreviewParameterProvider<UserUiModel> {
    override val values = sequenceOf(
        UserUiModel(
            id = 1,
            user = "test1@test.com",
            active = false
        ),
        UserUiModel(
            id = 2,
            user = "test2@test.com",
            active = true
        ),
        UserUiModel(
            id = 3,
            user = "test3@test.com",
            active = false
        )
    )
}

