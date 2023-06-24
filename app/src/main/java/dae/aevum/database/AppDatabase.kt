package dae.aevum.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dae.aevum.database.daos.IssuesDao
import dae.aevum.database.daos.UserDao
import dae.aevum.database.daos.WorklogDao
import dae.aevum.database.entities.IssueEntity
import dae.aevum.database.entities.UserEntity
import dae.aevum.database.entities.WorklogEntity
import javax.inject.Singleton

@Database(
    entities = [
        IssueEntity::class,
        WorklogEntity::class,
        UserEntity::class
    ],
    version = 9
)
@TypeConverters(Converters::class)
@Singleton
abstract class AppDatabase : RoomDatabase() {
    abstract fun issueDao(): IssuesDao
    abstract fun worklogDao(): WorklogDao
    abstract fun userDao(): UserDao
}