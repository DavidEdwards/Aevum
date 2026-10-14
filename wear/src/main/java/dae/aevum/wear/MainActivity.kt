package dae.aevum.wear

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dae.aevum.utils.ActiveTimerSnapshot
import dae.aevum.utils.WearRecentIssue
import dae.aevum.wear.data.ActiveTimerStore
import dae.aevum.wear.data.RecentIssuesStore
import dae.aevum.wear.data.StartTimerMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Scrollable list of active-timer details and recent issues. Wear tiles themselves cannot
 * scroll, so this screen is opened from the tile / launcher for readable interaction.
 */
class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var listContainer: LinearLayout
    private var elapsedTicker: Job? = null
    private var startingIssueId: String? = null
    private var latestSnapshot: ActiveTimerSnapshot? = null
    private var latestRecent: List<WearRecentIssue> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listContainer =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val horizontal = dp(20)
                val vertical = dp(40)
                setPadding(horizontal, vertical, horizontal, vertical)
            }

        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                addView(
                    listContainer,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )

        val activeStore = ActiveTimerStore(this)
        val recentStore = RecentIssuesStore(this)

        scope.launch(Dispatchers.IO) {
            activeStore.refreshFromDataLayer()
            recentStore.refreshFromDataLayer()
        }

        scope.launch {
            combine(activeStore.snapshots, recentStore.recentIssues) { snapshot, recent ->
                snapshot to recent
            }.collectLatest { (snapshot, recent) ->
                latestSnapshot = snapshot
                latestRecent = recent
                if (snapshot != null && snapshot.issueId == startingIssueId) {
                    startingIssueId = null
                }
                renderList(snapshot, recent)
                restartElapsedTicker(snapshot)
            }
        }
    }

    private fun restartElapsedTicker(snapshot: ActiveTimerSnapshot?) {
        elapsedTicker?.cancel()
        if (snapshot == null) return
        elapsedTicker =
            scope.launch {
                while (isActive) {
                    delay(1_000L)
                    updateElapsedRow(snapshot.startedAtEpochMs)
                }
            }
    }

    private fun renderList(
        snapshot: ActiveTimerSnapshot?,
        recent: List<WearRecentIssue>,
    ) {
        listContainer.removeAllViews()
        if (snapshot == null) {
            listContainer.addView(
                listRow(
                    label = getString(R.string.active_timer_tile_label),
                    value = getString(R.string.no_active_timer),
                ),
            )
        } else {
            listContainer.addView(
                listRow(
                    label = getString(R.string.issue_label),
                    value = snapshot.issueId,
                ),
            )
            if (snapshot.issueTitle.isNotBlank()) {
                listContainer.addView(
                    listRow(
                        label = getString(R.string.title_label),
                        value = snapshot.issueTitle,
                    ),
                )
            }
            listContainer.addView(
                listRow(
                    label = getString(R.string.elapsed_label),
                    value = formatElapsed(snapshot.startedAtEpochMs),
                    valueTag = ELAPSED_VALUE_TAG,
                ),
            )
            listContainer.addView(stopButton(snapshot))
        }

        listContainer.addView(sectionHeading(getString(R.string.recent_issues_heading)))

        val candidates =
            recent.filter { issue ->
                snapshot == null || issue.issueId != snapshot.issueId
            }
        if (candidates.isEmpty()) {
            listContainer.addView(
                listRow(
                    label = getString(R.string.status_label),
                    value = getString(R.string.no_recent_issues),
                ),
            )
        } else {
            candidates.forEach { issue ->
                listContainer.addView(recentIssueRow(issue))
            }
        }
    }

    private fun updateElapsedRow(startedAtEpochMs: Long) {
        val elapsedView = listContainer.findViewWithTag<TextView>(ELAPSED_VALUE_TAG) ?: return
        elapsedView.text = formatElapsed(startedAtEpochMs)
    }

    private fun sectionHeading(text: String): View =
        TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            params.topMargin = dp(16)
            params.bottomMargin = dp(8)
            layoutParams = params
        }

    private fun listRow(
        label: String,
        value: String,
        valueTag: Any? = null,
    ): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(12)
            val padV = dp(10)
            setPadding(padH, padV, padH, padV)
            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            params.bottomMargin = dp(8)
            layoutParams = params
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.list_row_background)

            addView(
                TextView(this@MainActivity).apply {
                    text = label
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    alpha = 0.7f
                },
            )
            addView(
                TextView(this@MainActivity).apply {
                    text = value
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTypeface(typeface, Typeface.BOLD)
                    tag = valueTag
                },
            )
        }

    private fun recentIssueRow(issue: WearRecentIssue): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(12)
            val padV = dp(10)
            setPadding(padH, padV, padH, padV)
            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            params.bottomMargin = dp(8)
            layoutParams = params
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.list_row_background)
            isClickable = true
            isFocusable = true
            setOnClickListener { startTimerFor(issue.issueId) }

            addView(
                TextView(this@MainActivity).apply {
                    text = issue.issueId
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTypeface(typeface, Typeface.BOLD)
                },
            )
            if (issue.issueTitle.isNotBlank()) {
                addView(
                    TextView(this@MainActivity).apply {
                        text = issue.issueTitle
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        alpha = 0.85f
                        maxLines = 2
                    },
                )
            }
            addView(
                TextView(this@MainActivity).apply {
                    text =
                        if (startingIssueId == issue.issueId) {
                            getString(R.string.starting_timer)
                        } else {
                            getString(R.string.start_timer)
                        }
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    alpha = 0.7f
                    setPadding(0, dp(4), 0, 0)
                },
            )
        }

    private fun startTimerFor(issueId: String) {
        if (startingIssueId != null) return
        startingIssueId = issueId
        renderList(latestSnapshot, latestRecent)
        scope.launch {
            val sent =
                withContext(Dispatchers.IO) {
                    StartTimerMessenger.send(this@MainActivity, issueId)
                }
            if (!sent) {
                startingIssueId = null
                renderList(latestSnapshot, latestRecent)
                Toast
                    .makeText(this@MainActivity, R.string.start_timer_failed, Toast.LENGTH_SHORT)
                    .show()
            } else {
                // Active timer sync should refresh the list; clear spinner after a short wait.
                delay(2_500L)
                if (startingIssueId == issueId) {
                    startingIssueId = null
                    renderList(latestSnapshot, latestRecent)
                }
            }
        }
    }

    private fun stopButton(snapshot: ActiveTimerSnapshot): View =
        Button(this).apply {
            text = getString(R.string.stop)
            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            params.topMargin = dp(8)
            layoutParams = params
            gravity = Gravity.CENTER
            setOnClickListener {
                startActivity(
                    android.content.Intent(this@MainActivity, StopTimerActivity::class.java).apply {
                        putExtra(StopTimerActivity.EXTRA_WORKLOG_ID, snapshot.worklogId)
                        putExtra(StopTimerActivity.EXTRA_ISSUE_ID, snapshot.issueId)
                    },
                )
            }
        }

    private fun dp(value: Int): Int =
        TypedValue
            .applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value.toFloat(),
                resources.displayMetrics,
            ).toInt()

    override fun onDestroy() {
        elapsedTicker?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ELAPSED_VALUE_TAG = "elapsed_value"
    }
}
