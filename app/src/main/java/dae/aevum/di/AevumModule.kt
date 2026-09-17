package dae.aevum.di

import android.content.Context
import androidx.room.Room
import dae.aevum.database.AppDatabase
import dae.aevum.database.migrations.Migration_10_11
import dae.aevum.database.migrations.Migration_9_10
import dae.aevum.domain.repositories.AudioRepository
import dae.aevum.domain.repositories.AudioRepositoryImpl
import dae.aevum.domain.repositories.ChainedProofreader
import dae.aevum.domain.repositories.JiraRepository
import dae.aevum.domain.repositories.JiraRepositoryImpl
import dae.aevum.domain.repositories.Proofreader
import dae.aevum.domain.repositories.TokenStore
import dae.aevum.domain.repositories.UserRepository
import dae.aevum.domain.repositories.UserRepositoryImpl
import dae.aevum.domain.repositories.WearActiveTimerSync
import dae.aevum.domain.repositories.WidgetRefresher
import dae.aevum.ml.LlamaProofreader
import dae.aevum.network.AuthInterceptor
import dae.aevum.network.Jira
import dae.aevum.network.JiraService
import dae.aevum.security.SecureTokenStore
import dae.aevum.utils.Urls
import dae.aevum.wear.WearActiveTimerSyncImpl
import dae.aevum.widget.WidgetRefresherImpl
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
        @ApplicationContext applicationContext: Context,
    ): AppDatabase {
        val builder =
            Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "database",
                ).addMigrations(Migration_9_10(), Migration_10_11())
        // Destructive fallback only in debug to avoid wiping pending timers in production.
        if (dae.aevum.BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(true)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(userRepository: UserRepository): AuthInterceptor =
        AuthInterceptor {
            userRepository.getActiveUser()
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor(authInterceptor)
        if (dae.aevum.BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BODY)
                },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): JiraService {
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(Urls.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
        return retrofit.create(JiraService::class.java)
    }

    @Provides
    @Singleton
    fun provideJira(service: JiraService): Jira = Jira(service)

    @Provides
    @Singleton
    fun provideIssueRepository(
        jira: Jira,
        userRepository: UserRepository,
        database: AppDatabase,
        proofreader: Proofreader,
    ): JiraRepository = JiraRepositoryImpl(jira, userRepository, database, proofreader)

    @Provides
    @Singleton
    fun provideProofreader(
//        @ApplicationContext context: Context,
        llama: LlamaProofreader,
    ): Proofreader =
        ChainedProofreader(
            listOf(
//            // First pass on-device for privacy and speed
//            MlKitProofreader(context),
                // Second pass via your Llama server for extra polish
                llama,
            ),
        )

    @Provides
    @Singleton
    fun provideLlamaProofreader(): LlamaProofreader = LlamaProofreader()

    @Provides
    @Singleton
    fun provideAudioRepository(
        @ApplicationContext context: Context,
        jiraRepository: JiraRepository,
    ): AudioRepository = AudioRepositoryImpl(context, jiraRepository)

    @Provides
    @Singleton
    fun provideTokenStore(
        @ApplicationContext context: Context,
    ): TokenStore = SecureTokenStore(context)

    @Provides
    @Singleton
    fun provideUserRepository(
        database: AppDatabase,
        tokenStore: TokenStore,
    ): UserRepository = UserRepositoryImpl(database, tokenStore)

    @Provides
    @Singleton
    fun provideWidgetRefresher(
        @ApplicationContext context: Context,
    ): WidgetRefresher = WidgetRefresherImpl(context)

    @Provides
    @Singleton
    fun provideWearActiveTimerSync(
        @ApplicationContext context: Context,
    ): WearActiveTimerSync = WearActiveTimerSyncImpl(context)
}
