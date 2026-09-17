package dae.aevum.wear

import android.app.Application
import timber.log.Timber

class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
