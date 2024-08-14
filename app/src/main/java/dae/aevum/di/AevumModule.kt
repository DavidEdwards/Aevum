package dae.aevum.di

import android.content.Context
import androidx.room.Room
import dae.aevum.database.AppDatabase
import dae.aevum.database.migrations.Migration_9_10
import dae.aevum.domain.repositories.AudioRepository
import dae.aevum.domain.repositories.AudioRepositoryImpl
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.JiraRepositoryImpl
import dae.aevum.domain.repositories.UserRepository
import dae.aevum.domain.repositories.UserRepositoryImpl
import dae.aevum.network.AuthInterceptor
import dae.aevum.network.Jira
import dae.aevum.network.JiraService
import dae.aevum.utils.Urls
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AevumModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext applicationContext: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "database"
        )
            .fallbackToDestructiveMigration()
            .addMigrations(Migration_9_10())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        userRepository: UserRepository
    ): AuthInterceptor {
        return AuthInterceptor {
            userRepository.getActiveUser()
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BASIC)
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): JiraService {
        val retrofit = Retrofit.Builder()
            .baseUrl(Urls.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        return retrofit.create(JiraService::class.java)
    }

    @Provides
    @Singleton
    fun provideJira(
        service: JiraService
    ): Jira {
        return Jira(service)
    }

    @Provides
    @Singleton
    fun provideIssueRepository(
        jira: Jira,
        userRepository: UserRepository,
        database: AppDatabase
    ): JiraRepository {
        return JiraRepositoryImpl(jira, userRepository, database)
    }

    @Provides
    @Singleton
    fun provideAudioRepository(
        @ApplicationContext context: Context,
        jiraRepository: JiraRepository
    ): AudioRepository {
        return AudioRepositoryImpl(context, jiraRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        database: AppDatabase
    ): UserRepository {
        return UserRepositoryImpl(database)
    }
}