package dae.aevum.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dae.aevum.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUser(entity: UserEntity)

    @Query("SELECT * FROM users WHERE active = 1 LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    @Query("SELECT * FROM users WHERE active = 1 LIMIT 1")
    fun flowActiveUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY active DESC, user ASC")
    fun listUsers(): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY active DESC, user ASC")
    fun flowUsers(): Flow<List<UserEntity>>

    @Delete
    suspend fun removeUser(entity: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun removeUser(userId: Int)

    @Transaction
    suspend fun selectUser(userId: Int) {
        setAllUsersInactive()
        setUserActive(userId)
    }

    @Query("UPDATE users SET active = 0")
    suspend fun setAllUsersInactive()

    @Query("UPDATE users SET active = 1 WHERE id = :userId")
    suspend fun setUserActive(userId: Int)

    @Query("SELECT id FROM users WHERE active = 1 LIMIT 1")
    suspend fun hasActiveUser(): Boolean
}