package dae.aevum.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName") // Room migration naming convention
class Migration_10_11 : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE worklogs ADD COLUMN jiraWorklogId INTEGER DEFAULT NULL")
        // Previously remote rows used Jira's id as the local PK; preserve that as jiraWorklogId.
        db.execSQL(
            "UPDATE worklogs SET jiraWorklogId = workId WHERE pending = 0 AND active = 0",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_worklogs_userId_jiraWorklogId " +
                "ON worklogs(userId, jiraWorklogId)",
        )
    }
}
