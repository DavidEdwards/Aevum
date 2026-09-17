package dae.aevum.wear.tile

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import dae.aevum.utils.ActiveTimerSnapshot
import dae.aevum.wear.R
import dae.aevum.wear.StopTimerActivity
import dae.aevum.wear.data.ActiveTimerStore
import java.util.concurrent.TimeUnit

class ActiveTimerTileService : Material3TileService() {
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val snapshot = ActiveTimerStore(this@ActiveTimerTileService).refreshFromDataLayer()
        val layout =
            if (snapshot == null) {
                idleLayout()
            } else {
                activeLayout(snapshot)
            }
        return Tile
            .Builder()
            .setTileTimeline(Timeline.fromLayoutElement(layout))
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
            .build()
    }

    private fun MaterialScope.idleLayout() =
        primaryLayout(
            titleSlot = { text(getString(R.string.active_timer_tile_label).layoutString) },
            mainSlot = {
                text(
                    text = getString(R.string.no_active_timer).layoutString,
                    typography = Typography.BODY_LARGE,
                )
            },
            bottomSlot = {
                text(
                    text = getString(R.string.open_on_phone).layoutString,
                    typography = Typography.BODY_SMALL,
                )
            },
        )

    private fun MaterialScope.activeLayout(snapshot: ActiveTimerSnapshot) =
        primaryLayout(
            titleSlot = { text(snapshot.issueId.layoutString) },
            mainSlot = {
                text(
                    text = formatElapsed(snapshot.startedAtEpochMs).layoutString,
                    typography = Typography.DISPLAY_MEDIUM,
                )
            },
            bottomSlot = {
                textEdgeButton(
                    onClick =
                        clickable(
                            id = "stop_timer",
                            action =
                                ActionBuilders.launchAction(
                                    ComponentName(
                                        this@ActiveTimerTileService,
                                        StopTimerActivity::class.java,
                                    ),
                                    mapOf(
                                        StopTimerActivity.EXTRA_WORKLOG_ID to
                                            ActionBuilders.longExtra(snapshot.worklogId),
                                        StopTimerActivity.EXTRA_ISSUE_ID to
                                            ActionBuilders.stringExtra(snapshot.issueId),
                                    ),
                                ),
                        ),
                ) {
                    text(getString(R.string.stop).layoutString)
                }
            },
        )

    private fun formatElapsed(startedAtEpochMs: Long): String {
        val elapsedMs = (System.currentTimeMillis() - startedAtEpochMs).coerceAtLeast(0L)
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "%d:%02d".format(hours, minutes)
        } else {
            "%dm".format(minutes)
        }
    }

    companion object {
        private val FRESHNESS_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1)
    }
}
