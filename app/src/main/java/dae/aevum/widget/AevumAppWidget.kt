package dae.aevum.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dae.aevum.MainActivity
import dae.aevum.utils.Intents
import timber.log.Timber
import dae.aevum.common.R as CommonR

class AevumAppWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode =
        SizeMode.Responsive(
            setOf(
                COMPACT,
                REGULAR,
                EXPANDED,
            ),
        )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        Timber.d("AevumAppWidget.provideGlance(id=%s)", id)
        // Seed preferences from DB. Subsequent update() calls while a session is alive only
        // reload preferences — they do not re-run this function — so the UI must read
        // currentState, not a value captured here.
        WidgetGlanceState.syncFromDatabase(context, id)
        provideContent {
            val prefs = currentState<Preferences>()
            val state = WidgetGlanceState.read(prefs)
            key(LocalSize.current, prefs) {
                WidgetRoot(context = context, state = state)
            }
        }
    }

    @Composable
    @Suppress("ktlint:standard:function-naming")
    private fun WidgetRoot(
        context: Context,
        state: WidgetUiState,
    ) {
        val size = LocalSize.current
        val layout = layoutFor(size)
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(surfaceColor)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 8.dp, vertical = if (layout == WidgetLayout.COMPACT) 4.dp else 8.dp),
            verticalAlignment =
                if (layout == WidgetLayout.COMPACT) {
                    Alignment.CenterVertically
                } else {
                    Alignment.Top
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is WidgetUiState.Active -> {
                    when (layout) {
                        WidgetLayout.COMPACT -> ActiveCompact(context, state.active)
                        WidgetLayout.REGULAR -> ActiveRegular(context, state.active)
                        WidgetLayout.EXPANDED -> ActiveExpanded(context, state.active)
                    }
                }

                is WidgetUiState.Idle -> {
                    when (layout) {
                        WidgetLayout.COMPACT -> IdleCompact(context, state.suggestion)
                        WidgetLayout.REGULAR -> IdleRegular(context, state.suggestion)
                        WidgetLayout.EXPANDED -> IdleExpanded(context, state.suggestion)
                    }
                }
            }
        }
    }

    // region Active layouts

    /** One horizontal row: issue + elapsed | Stop */
    @Composable
    @Suppress("ktlint:standard:function-naming")
    private fun ActiveCompact(
        context: Context,
        active: WidgetActiveState,
    ) {
        val stopIntent = stopIntent(context, active.worklogId)
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = active.issueId,
                    style = labelStyle(11.sp),
                    maxLines = 1,
                )
                Text(
                    text = WidgetFormatters.formatElapsed(active.startedAtEpochMs),
                    style = elapsedStyle(18.sp),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            Button(
                text = context.getString(CommonR.string.timer_action_stop),
                onClick = actionStartActivity(stopIntent),
                modifier = GlanceModifier.height(36.dp),
                style = buttonTextStyle(),
                maxLines = 1,
            )
        }
    }

    /** Issue + elapsed + title, then Stop | Open */
    @Composable
    @Suppress("ktlint:standard:function-naming")
    private fun ActiveRegular(
        context: Context,
        active: WidgetActiveState,
    ) {
        val openIntent = openAppIntent(context)
        val stopIntent = stopIntent(context, active.worklogId)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = active.issueId,
                style = labelStyle(13.sp),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Text(
                text = WidgetFormatters.formatElapsed(active.startedAtEpochMs),
                style = elapsedStyle(20.sp),
                maxLines = 1,
            )
        }
        if (active.issueTitle.isNotBlank()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = active.issueTitle,
                style = secondaryStyle(11.sp),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                text = context.getString(CommonR.string.timer_action_stop),
                onClick = actionStartActivity(stopIntent),
                modifier = GlanceModifier.defaultWeight().height(36.dp),
                style = buttonTextStyle(),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Button(
                text = context.getString(CommonR.string.widget_open),
                onClick = actionStartActivity(openIntent),
                modifier = GlanceModifier.defaultWeight().height(36.dp),
                style = buttonTextStyle(),
                maxLines = 1,
            )
        }
    }

    /** Full remote: elapsed hero, ±15, Stop | Open */
    @Composable
    @Suppress("ktlint:standard:function-naming")
    private fun ActiveExpanded(
        context: Context,
        active: WidgetActiveState,
    ) {
        val openIntent = openAppIntent(context)
        val stopIntent = stopIntent(context, active.worklogId)
        Text(
            text = active.issueId,
            style = labelStyle(14.sp),
            maxLines = 1,
        )
        Text(
            text = WidgetFormatters.formatElapsed(active.startedAtEpochMs),
            style = elapsedStyle(28.sp),
            maxLines = 1,
        )
        if (active.issueTitle.isNotBlank()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = active.issueTitle,
                style = secondaryStyle(12.sp),
                maxLines = 2,
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                text = context.getString(CommonR.string.widget_action_sub),
                onClick = actionRunCallback<SubtractFifteenAction>(),
                modifier = GlanceModifier.defaultWeight().height(36.dp),
                style = buttonTextStyle(),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Button(
                text = context.getString(CommonR.string.widget_action_add),
                onClick = actionRunCallback<AddFifteenAction>(),
                modifier = GlanceModifier.defaultWeight().height(36.dp),
                style = buttonTextStyle(),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                text = context.getString(CommonR.string.timer_action_stop),
                onClick = actionStartActivity(stopIntent),
                modifier = GlanceModifier.defaultWeight().height(36.dp),
                style = buttonTextStyle(),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Button(
                text = context.getString(CommonR.string.widget_open),
                onClick = actionStartActivity(openIntent),
                modifier = GlanceModifier.defaultWeight().height(36.dp),
                style = buttonTextStyle(),
                maxLines = 1,
            )
        }
    }

    // endregion

    // region Idle layouts

    @Composable
    @Suppress("ktlint:standard:function-naming")
    private fun IdleCompact(
        context: Context,
        idle: WidgetIdleState?,
    ) {
        val openIntent = openAppIntent(context)
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = context.getString(CommonR.string.widget_no_active_timer),
                    style = labelStyle(11.sp),
                    maxLines = 1,
                )
                if (idle != null) {
                    Text(
                        text = idle.issueId,
                        style = secondaryStyle(12.sp),
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            if (idle != null) {
                Button(
                    text = context.getString(CommonR.string.widget_start),
                    onClick = actionRunCallback<StartSuggestedIssueAction>(),
                    modifier = GlanceModifier.height(36.dp),
                    style = buttonTextStyle(),
                    maxLines = 1,
                )
            } else {
                Button(
                    text = context.getString(CommonR.string.widget_open),
                    onClick = actionStartActivity(openIntent),
                    modifier = GlanceModifier.height(36.dp),
                    style = buttonTextStyle(),
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    @Suppress("ktlint:standard:function-naming")
    private fun IdleRegular(
        context: Context,
        idle: WidgetIdleState?,
    ) {
        val openIntent = openAppIntent(context)
        Text(
            text = context.getString(CommonR.string.widget_no_active_timer),
            style = labelStyle(13.sp),
            maxLines = 1,
        )
        if (idle != null) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = idle.issueId,
                style = elapsedStyle(16.sp),
                maxLines = 1,
            )
            if (idle.issueTitle.isNotBlank()) {
                Text(
                    text = idle.issueTitle,
                    style = secondaryStyle(11.sp),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (idle != null) {
                Button(
                    text = context.getString(CommonR.string.widget_start),
                    onClick = actionRunCallback<StartSuggestedIssueAction>(),
                    modifier = GlanceModifier.defaultWeight().height(36.dp),
                    style = buttonTextStyle(),
                    maxLines = 1,
                )
            } else {
                Button(
                    text = context.getString(CommonR.string.widget_no_issue_to_start),
                    onClick = actionStartActivity(openIntent),
                    modifier = GlanceModifier.defaultWeight().height(36.dp),
                    style = buttonTextStyle(),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            Button(
                text = context.getString(CommonR.string.widget_open),
                onClick = actionStartActivity(openIntent),
                modifier = GlanceModifier.defaultWeight().height(36.dp),
                style = buttonTextStyle(),
                maxLines = 1,
            )
        }
    }

    @Composable
    @Suppress("ktlint:standard:function-naming")
    private fun IdleExpanded(
        context: Context,
        idle: WidgetIdleState?,
    ) {
        IdleRegular(context, idle)
    }

    // endregion

    private fun openAppIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    private fun stopIntent(
        context: Context,
        worklogId: Long,
    ): Intent =
        openAppIntent(context).apply {
            putExtra(Intents.EXTRA_OPEN_STOP_WORKLOG_ID, worklogId)
        }

    private fun labelStyle(size: androidx.compose.ui.unit.TextUnit) =
        TextStyle(
            color = onSurfaceColor,
            fontWeight = FontWeight.Bold,
            fontSize = size,
        )

    private fun elapsedStyle(size: androidx.compose.ui.unit.TextUnit) =
        TextStyle(
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            fontSize = size,
            textAlign = TextAlign.Start,
        )

    private fun secondaryStyle(size: androidx.compose.ui.unit.TextUnit) =
        TextStyle(
            color = onSurfaceVariantColor,
            fontSize = size,
        )

    private fun buttonTextStyle() =
        TextStyle(
            color = onPrimaryColor,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )

    companion object {
        /** Short wide strip — single-row content only. */
        val COMPACT = DpSize(180.dp, 72.dp)

        /** Standard home-screen tile. */
        val REGULAR = DpSize(250.dp, 120.dp)

        /** Tall remote with ±15 controls. */
        val EXPANDED = DpSize(300.dp, 200.dp)

        private val surfaceColor =
            ColorProvider(day = Color(0xFFFFFBFE), night = Color(0xFF1C1B1F))
        private val onSurfaceColor =
            ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFFE6E1E5))
        private val onSurfaceVariantColor =
            ColorProvider(day = Color(0xFF49454F), night = Color(0xFFCAC4D0))
        private val primaryColor =
            ColorProvider(day = Color(0xFF6750A4), night = Color(0xFFD0BCFF))
        private val onPrimaryColor =
            ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF381E72))

        fun layoutFor(size: DpSize): WidgetLayout =
            when {
                size.height <= COMPACT.height + 8.dp || size.width < REGULAR.width -> {
                    WidgetLayout.COMPACT
                }

                size.height < EXPANDED.height || size.width < EXPANDED.width -> {
                    WidgetLayout.REGULAR
                }

                else -> {
                    WidgetLayout.EXPANDED
                }
            }
    }
}

enum class WidgetLayout {
    COMPACT,
    REGULAR,
    EXPANDED,
}

class AevumAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AevumAppWidget()
}
