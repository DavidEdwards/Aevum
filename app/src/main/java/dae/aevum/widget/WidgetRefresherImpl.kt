package dae.aevum.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import dae.aevum.domain.repositories.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class WidgetRefresherImpl
    @Inject
    constructor(
        @param:ApplicationContext private val appContext: Context,
    ) : WidgetRefresher {
        override suspend fun refreshAll() {
            Timber.d("WidgetRefresherImpl.refreshAll")
            val manager = GlanceAppWidgetManager(appContext)
            val ids = manager.getGlanceIds(AevumAppWidget::class.java)
            for (id in ids) {
                WidgetGlanceState.syncFromDatabase(appContext, id)
                AevumAppWidget().update(appContext, id)
            }
        }
    }
