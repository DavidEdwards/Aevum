package dae.aevum.domain.repositories

import android.util.Base64
import dae.aevum.App
import dae.aevum.database.AppDatabase
import dae.aevum.database.entities.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Singleton

interface UserRepository {
    suspend fun getActiveUser(): UserEntity?
    fun flowActiveUser(): Flow<UserEntity?>
    suspend fun listUsers(): List<UserEntity>
    fun flowUsers(): Flow<List<UserEntity>>
    suspend fun addUser(
        instanceUrl: String,
        user: String,
        token: String
    )

    suspend fun testUser(
        instanceUrl: String,
        user: String,
        token: String
    ): Boolean

    suspend fun removeUser(userId: Int)
    suspend fun selectUser(userId: Int)
    suspend fun unselectAllUsers()
}

@Singleton
class UserRepositoryImpl(
    private val database: AppDatabase
) : UserRepository {
    override suspend fun getActiveUser(): UserEntity? {
        return database.userDao().getActiveUser()
    }

    override fun flowActiveUser(): Flow<UserEntity?> {
        return database.userDao().flowActiveUser()
    }

    override suspend fun listUsers(): List<UserEntity> {
        return database.userDao().listUsers()
    }

    override fun flowUsers(): Flow<List<UserEntity>> {
        return database.userDao().flowUsers()
    }

    override suspend fun testUser(
        instanceUrl: String,
        user: String,
        token: String
    ): Boolean {
        val client = OkHttpClient.Builder()
            .build()

        val authorizationToken = Base64.encodeToString(
            "${user}:${token}".toByteArray(),
            Base64.NO_WRAP
        )

        val url =
            "${App.BASE_URL}search?maxResults=1&jql=worklogAuthor%3DcurrentUser()".replace(
                "https://baseurl",
                instanceUrl
            )

        val call = client.newCall(
            Request.Builder()
                .url(url)
                .addHeader(
                    "Authorization",
                    "Basic $authorizationToken"
                )
                .build()
        )

        return try {
            val response = withContext(Dispatchers.IO) {
                call.execute()
            }

            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun addUser(
        instanceUrl: String,
        user: String,
        token: String
    ) {
        val entity = UserEntity(
            id = 0,
            instanceUrl = instanceUrl,
            user = user,
            token = token,
            active = true
        )
        database.userDao().addUser(entity)
    }

    override suspend fun removeUser(userId: Int) {
        database.userDao().removeUser(userId)
    }

    override suspend fun selectUser(userId: Int) {
        database.userDao().selectUser(userId)
    }

    override suspend fun unselectAllUsers() {
        database.userDao().setAllUsersInactive()
    }
}