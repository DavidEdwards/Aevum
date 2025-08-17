package dae.aevum.widget

import dae.aevum.database.AppDatabase
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.UserRepository
import dae.aevum.domain.repositories.WidgetRefresher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AevumWidgetEntryPoint {
    fun jiraRepository(): JiraRepository
    fun userRepository(): UserRepository
    fun database(): AppDatabase
    fun widgetRefresher(): WidgetRefresher
}



