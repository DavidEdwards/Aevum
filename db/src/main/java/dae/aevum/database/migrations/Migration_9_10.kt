package dae.aevum.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration_9_10() : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE worklogs ADD COLUMN jiraUserAccountId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE users ADD COLUMN jiraUserAccountId TEXT NOT NULL DEFAULT ''")
    }
}