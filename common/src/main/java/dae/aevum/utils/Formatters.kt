package dae.aevum.utils

import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Formatters {
    val universalDateFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
    }
    val universalDateTimeZoneFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }

    val universalTimeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter
            .ofPattern("kk:mm")
            .withZone(ZoneId.systemDefault())
    }
}
