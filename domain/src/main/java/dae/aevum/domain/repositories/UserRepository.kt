package dae.aevum.domain.repositories

import dae.aevum.database.AppDatabase
import dae.aevum.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

interface TokenStore {
    fun getToken(userId: Int): String?

    fun setToken(
        userId: Int,
        token: String,
    )

    fun removeToken(userId: Int)
}

interface UserRepository {
    suspend fun getActiveUser(): UserEntity?

    fun flowActiveUser(): Flow<UserEntity?>

    suspend fun getUserById(userId: Int): UserEntity?

    suspend fun listUsers(): List<UserEntity>

    fun flowUsers(): Flow<List<UserEntity>>

    suspend fun addUser(
        instanceUrl: String,
        user: String,
        jiraAccountId: String,
        token: String,
    ): Int

    suspend fun removeUser(userId: Int)

    suspend fun selectUser(userId: Int)

    suspend fun unselectAllUsers()

    suspend fun updateUserToken(
        userId: Int,
        token: String,
    )
}

@Singleton
class UserRepositoryImpl(
    private val database: AppDatabase,
    private val tokenStore: TokenStore,
) : UserRepository {
    override suspend fun getActiveUser(): UserEntity? =
        database.userDao().getActiveUser()?.let { hydrateAndMigrate(it) }

    override fun flowActiveUser(): Flow<UserEntity?> =
        database.userDao().flowActiveUser().map { user ->
            user?.let { overlayToken(it) }
        }

    override suspend fun getUserById(userId: Int): UserEntity? =
        database.userDao().getUserById(userId)?.let { hydrateAndMigrate(it) }

    override suspend fun listUsers(): List<UserEntity> =
        database.userDao().listUsers().map { hydrateAndMigrate(it) }

    override fun flowUsers(): Flow<List<UserEntity>> =
        database.userDao().flowUsers().map { list -> list.map { overlayToken(it) } }

    override suspend fun addUser(
        instanceUrl: String,
        user: String,
        jiraAccountId: String,
        token: String,
    ): Int {
        val entity =
            UserEntity(
                id = 0,
                instanceUrl = instanceUrl,
                user = user,
                jiraUserAccountId = jiraAccountId,
                token = "",
                active = true,
            )
        val id = database.userDao().addUser(entity).toInt()
        tokenStore.setToken(id, token)
        return id
    }

    override suspend fun removeUser(userId: Int) {
        database.worklogDao().deleteWorklogsForUser(userId)
        database.issueDao().deleteIssuesForUser(userId)
        tokenStore.removeToken(userId)
        database.userDao().removeUser(userId)
    }

    override suspend fun selectUser(userId: Int) {
        database.userDao().selectUser(userId)
    }

    override suspend fun unselectAllUsers() {
        database.userDao().setAllUsersInactive()
    }

    override suspend fun updateUserToken(
        userId: Int,
        token: String,
    ) {
        tokenStore.setToken(userId, token)
        database.userDao().updateUserToken(userId, "")
    }

    private suspend fun hydrateAndMigrate(user: UserEntity): UserEntity {
        val stored = tokenStore.getToken(user.id)
        if (!stored.isNullOrBlank()) {
            if (user.token.isNotBlank()) {
                database.userDao().updateUserToken(user.id, "")
            }
            return user.copy(token = stored)
        }
        if (user.token.isNotBlank()) {
            tokenStore.setToken(user.id, user.token)
            database.userDao().updateUserToken(user.id, "")
            return user.copy(token = user.token)
        }
        return user
    }

    private fun overlayToken(user: UserEntity): UserEntity {
        val stored = tokenStore.getToken(user.id)
        return when {
            !stored.isNullOrBlank() -> user.copy(token = stored)
            else -> user
        }
    }
}
