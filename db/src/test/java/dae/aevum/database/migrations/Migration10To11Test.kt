package dae.aevum.database.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dae.aevum.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [31])
class Migration10To11Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate10To11_addsJiraWorklogIdAndBackfillsSyncedRows() {
        helper.createDatabase(TEST_DB, 10).apply {
            execSQL(
                "INSERT INTO users (id, instanceUrl, user, jiraUserAccountId, token, active) " +
                    "VALUES (1, 'https://example.atlassian.net', 'a@b.c', 'acc', 'tok', 1)",
            )
            execSQL(
                "INSERT INTO worklogs (workId, issueId, userId, jiraUserAccountId, `from`, `to`, author, summary, pending, active) " +
                    "VALUES (42, 'A-1', 1, 'acc', 0, 1000, 'Me', 'done', 0, 0)",
            )
            execSQL(
                "INSERT INTO worklogs (workId, issueId, userId, jiraUserAccountId, `from`, `to`, author, summary, pending, active) " +
                    "VALUES (7, 'A-2', 1, 'acc', 0, 1000, 'Me', 'pending', 1, 0)",
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 11, true, Migration_10_11()).use { db ->
            db.query("SELECT jiraWorklogId FROM worklogs WHERE workId = 42").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(42L, cursor.getLong(0))
            }
            db.query("SELECT jiraWorklogId FROM worklogs WHERE workId = 7").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
