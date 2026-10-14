package dae.aevum.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IssueSearchQueryTest {
    @Test
    fun fullKey_usesExactKeyLookup() {
        assertEquals("key = PROJ-5712", IssueSearchQuery.buildJql("PROJ-5712"))
        assertTrue(IssueSearchQuery.isFullIssueKey("AAPP-1"))
    }

    @Test
    fun numericId_searchesInternalIdAndKnownProjectKeys() {
        val jql = IssueSearchQuery.buildJql("5712", listOf("PROJ", "AAPP", "PROJ"))
        assertEquals(
            "id = 5712 OR key in (PROJ-5712,AAPP-5712) OR text ~ \"5712\" OR summary ~ \"5712\" ORDER BY updated DESC",
            jql,
        )
        assertTrue(IssueSearchQuery.isNumericId("5712"))
        assertFalse(IssueSearchQuery.isNumericId("PROJ-5712"))
    }

    @Test
    fun numericId_withoutProjects_stillSearchesInternalIdAndText() {
        val jql = IssueSearchQuery.buildJql("5712")
        assertEquals(
            "id = 5712 OR text ~ \"5712\" OR summary ~ \"5712\" ORDER BY updated DESC",
            jql,
        )
    }

    @Test
    fun textTerm_usesSummaryAndTextSearch() {
        val jql = IssueSearchQuery.buildJql("login bug")
        assertEquals(
            "text ~ \"login bug\" OR summary ~ \"login bug\" ORDER BY updated DESC",
            jql,
        )
    }

    @Test
    fun escapeJqlString_escapesQuotesAndBackslashes() {
        assertEquals("say \\\"hi\\\"", IssueSearchQuery.escapeJqlString("say \"hi\""))
        assertEquals("a\\\\b", IssueSearchQuery.escapeJqlString("a\\b"))
    }

    @Test
    fun projectKeyFromIssueId_extractsPrefix() {
        assertEquals("PROJ", IssueSearchQuery.projectKeyFromIssueId("PROJ-5712"))
        assertEquals(null, IssueSearchQuery.projectKeyFromIssueId("5712"))
    }
}
