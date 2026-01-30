package dae.aevum.domain.repositories

import dae.aevum.database.AppDatabase
import dae.aevum.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

interface UserRepository {
    suspend fun getActiveUser(): UserEntity?

    fun flowActiveUser(): Flow<UserEntity?>

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
}

@Singleton
class UserRepositoryImpl(
    private val database: AppDatabase,
) : UserRepository {
    override suspend fun getActiveUser(): UserEntity? = database.userDao().getActiveUser()

    override fun flowActiveUser(): Flow<UserEntity?> = database.userDao().flowActiveUser()

    override suspend fun listUsers(): List<UserEntity> = database.userDao().listUsers()

    override fun flowUsers(): Flow<List<UserEntity>> = database.userDao().flowUsers()

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
                token = token,
                active = true,
            )
        return database.userDao().addUser(entity).toInt()
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
