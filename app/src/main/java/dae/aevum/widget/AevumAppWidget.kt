package dae.aevum.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.Button
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.EntryPointAccessors
import dae.aevum.MainActivity
import dae.aevum.database.entities.ActiveIssueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AevumAppWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 60.dp),
            DpSize(220.dp, 110.dp),
            DpSize(300.dp, 200.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Timber.d("AevumAppWidget.provideGlance(id=%s) invoked", id)
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            AevumWidgetEntryPoint::class.java
        )
        val userRepo = entryPoint.userRepository()
        val db = entryPoint.database()

        // Load lightweight UI data: active issue if present, else last worked issue
        val activeIssue: ActiveIssueEntity? = try {
            withContext(Dispatchers.IO) {
                val user = userRepo.getActiveUser() ?: return@withContext null
                val activeWork = db.worklogDao().getActiveWorklogForUser(user.id)
                if (activeWork == null) return@withContext null
                val issue = try {
                    db.issueDao().findIssueById(user.id, activeWork.issueId.value)
                } catch (_: Throwable) {
                    null
                }
                if (issue != null) ActiveIssueEntity(issue = issue, worklog = activeWork) else null
            }
        } catch (_: Throwable) {
            null
        }
        if (activeIssue == null) {
            Timber.d("AevumAppWidget.provideGlance(id=%s): no active issue", id)
        } else {
            Timber.d(
                "AevumAppWidget.provideGlance(id=%s): active=%s",
                id,
                activeIssue.issue.id.value
            )
        }

        provideContent {
            // read preferences to create a composition dependency so updates are observed
            // read preferences to create a composition dependency so updates are observed
            val nonceKey = intPreferencesKey("nonce")
            val noncePrefs = currentState<Preferences>()
            val nonce = noncePrefs[nonceKey] ?: 0
            WidgetContent(
                context = context,
                active = activeIssue,
            )
            // use nonce to avoid unused warning and ensure recomposition on state change
            if (nonce < 0) {
                Text(text = "")
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        active: ActiveIssueEntity?,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color(0xFF202124), night = Color(0xFF202124)))
                .padding(8.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textStyle = TextStyle(color = ColorProvider(day = Color.White, night = Color.White))
            if (active != null) {
                Text(text = active.issue.id.value, style = textStyle)
                if (active.issue.title.isNotBlank()) {
                    Text(text = active.issue.title, style = textStyle)
                }
                Row(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        text = "Stop",
                        onClick = actionRunCallback<StopActiveTimerAction>()
                    )
                    Button(
                        text = "Open",
                        onClick = actionStartActivity(Intent(context, MainActivity::class.java))
                    )
                }
            } else {
                Text(text = "No active timer", style = textStyle)
                Row(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        text = "Start last",
                        onClick = actionRunCallback<StartLastIssueAction>()
                    )
                    Button(
                        text = "Open",
                        onClick = actionStartActivity(Intent(context, MainActivity::class.java))
                    )
                }
            }
        }
    }
}

class AevumAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AevumAppWidget()
}


