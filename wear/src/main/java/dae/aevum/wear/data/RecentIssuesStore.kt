package dae.aevum.wear.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import dae.aevum.utils.WearActiveTimerContract
import dae.aevum.utils.WearRecentIssue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private val Context.recentIssuesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "recent_issues",
)

class RecentIssuesStore(
    private val context: Context,
) {
    private val idsKey = stringPreferencesKey(WearActiveTimerContract.KEY_RECENT_ISSUE_IDS)
    private val titlesKey = stringPreferencesKey(WearActiveTimerContract.KEY_RECENT_ISSUE_TITLES)

    val recentIssues: Flow<List<WearRecentIssue>> =
        context.recentIssuesDataStore.data.map { prefs ->
            decode(
                idsJoined = prefs[idsKey],
                titlesJoined = prefs[titlesKey],
            )
        }

    suspend fun write(issues: List<WearRecentIssue>) {
        context.recentIssuesDataStore.edit { prefs ->
            prefs[idsKey] = issues.joinToString(SEPARATOR) { it.issueId }
            prefs[titlesKey] = issues.joinToString(SEPARATOR) { encodeTitle(it.issueTitle) }
        }
    }

    suspend fun readCached(): List<WearRecentIssue> = recentIssues.first()

    suspend fun refreshFromDataLayer(): List<WearRecentIssue> {
        return try {
            val uri =
                Uri
                    .Builder()
                    .scheme(PutDataRequest.WEAR_URI_SCHEME)
                    .path(WearActiveTimerContract.RECENT_ISSUES_PATH)
                    .build()
            val buffer = Wearable.getDataClient(context).getDataItems(uri).await()
            try {
                val item =
                    buffer.firstOrNull() ?: return readCached().also {
                        Timber.v("No Data Layer item for recent issues; using cache")
                    }
                val map = DataMapItem.fromDataItem(item).dataMap
                val issues =
                    fromDataMap(
                        ids = map.getStringArray(WearActiveTimerContract.KEY_RECENT_ISSUE_IDS),
                        titles = map.getStringArray(WearActiveTimerContract.KEY_RECENT_ISSUE_TITLES),
                    )
                write(issues)
                issues
            } finally {
                buffer.release()
            }
        } catch (t: Throwable) {
            Timber.w(t, "Failed to read recent issues from Data Layer")
            readCached()
        }
    }

    companion object {
        private const val SEPARATOR = "\u001F"

        fun fromDataMap(
            ids: Array<String>?,
            titles: Array<String>?,
        ): List<WearRecentIssue> {
            if (ids.isNullOrEmpty()) return emptyList()
            val titleList = titles.orEmpty()
            return ids.mapIndexed { index, id ->
                WearRecentIssue(
                    issueId = id,
                    issueTitle = titleList.getOrNull(index).orEmpty(),
                )
            }.filter { it.issueId.isNotBlank() }
                .take(WearActiveTimerContract.RECENT_ISSUES_LIMIT)
        }

        private fun decode(
            idsJoined: String?,
            titlesJoined: String?,
        ): List<WearRecentIssue> {
            if (idsJoined.isNullOrBlank()) return emptyList()
            val ids = idsJoined.split(SEPARATOR)
            val titles = titlesJoined.orEmpty().split(SEPARATOR)
            return ids.mapIndexed { index, id ->
                WearRecentIssue(
                    issueId = id,
                    issueTitle = decodeTitle(titles.getOrNull(index).orEmpty()),
                )
            }.filter { it.issueId.isNotBlank() }
        }

        private fun encodeTitle(title: String): String = title.replace(SEPARATOR, " ")

        private fun decodeTitle(title: String): String = title
    }
}
