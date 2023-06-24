package dae.aevum

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@HiltAndroidApp
class App : Application() {
    companion object {
        const val BASE_URL = "https://baseurl/rest/api/3/"

        val universalDateFormatter: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.systemDefault())
        }
        val universalDateTimeZoneFormatter: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        }

        val universalTimeFormatter: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("kk:mm")
                .withZone(ZoneId.systemDefault())
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}