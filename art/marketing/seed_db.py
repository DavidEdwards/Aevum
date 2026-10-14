#!/usr/bin/env python3
"""Builds a Room database (schema v11) with mocked marketing data.

Usage: seed_db.py <output.db> <now_epoch_seconds>
"""

import random
import sqlite3
import sys
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

ZONE = ZoneInfo("Europe/Berlin")
IDENTITY_HASH = "63c003542f9082f7c879d736a6b2ca74"
USER_ID = 1
ACCOUNT_ID = "712020:5f1c2a9e-8b7d-4e61-a3c0-9d2f4b6e8a11"
AUTHOR = "John Smith"

ISSUES = [
    ("MEET-12", "Sprint ceremonies, stand-ups & refinement", True),
    ("APP-482", "Offline sync for the checkout cart", True),
    ("APP-517", "Redesign the onboarding flow", True),
    ("WEB-231", "Dashboard loads slowly for large accounts", True),
    ("SUP-307", "Investigate payment webhook retries", False),
    ("APP-530", "Crash when rotating during video upload", False),
    ("OPS-88", "Migrate CI pipelines to ARM runners", False),
    ("WEB-244", "Dark mode for the settings pages", False),
    ("APP-498", "Deep links from push notifications", False),
    ("WEB-219", "Accessibility fixes from WCAG 2.2 audit", False),
    ("APP-541", "Biometric login on Android and iOS", False),
    ("OPS-91", "Rotate expiring TLS certificates", False),
    ("WEB-252", "Export reports as CSV", False),
    ("APP-503", "Reduce app download size by 20%", False),
]

SUMMARIES = {
    "MEET-12": [
        "Daily stand-up",
        "Sprint planning for sprint 42",
        "Backlog refinement with product",
        "Sprint review and retro",
    ],
    "APP-482": [
        "Implemented conflict resolution for offline cart edits",
        "Added Room cache for cart items and sync queue",
        "Wrote integration tests for reconnect scenarios",
        "Code review fixes on sync worker",
    ],
    "APP-517": [
        "Built new welcome carousel in Compose",
        "Paired with design on permission screens",
        "Hooked up analytics events for onboarding funnel",
    ],
    "WEB-231": [
        "Profiled dashboard queries, added index on account_id",
        "Paginated widget API responses",
        "Load-tested dashboard with 50k records",
    ],
    "SUP-307": [
        "Traced duplicate webhook deliveries to missing idempotency key",
        "Call with payment provider support",
    ],
    "APP-530": [
        "Reproduced crash on Pixel 8, fixed lifecycle leak",
        "Added regression test for configuration change",
    ],
    "OPS-88": [
        "Ported build images to arm64",
        "Benchmarked ARM runners vs x86, 34% faster",
    ],
    "WEB-244": [
        "Added dark theme tokens to settings components",
        "Fixed contrast issues in form inputs",
    ],
    "APP-498": [
        "Routing for notification deep links",
        "QA pass with notification payload variants",
    ],
    "WEB-219": [
        "Keyboard navigation for modal dialogs",
        "Added aria labels to chart legends",
    ],
    "APP-541": [
        "Spike on BiometricPrompt fallback handling",
        "Implemented secure key storage for biometric unlock",
    ],
    "OPS-91": ["Renewed and deployed certificates to staging and prod"],
    "WEB-252": ["CSV export endpoint with streaming response"],
    "APP-503": [
        "Enabled resource shrinking, removed unused fonts",
        "Converted large PNG assets to WebP",
    ],
}

WORK_ISSUES = [i[0] for i in ISSUES if i[0] != "MEET-12"]

# Logged hours for specific days relative to today (days ago -> hours) to give the
# calendar some variety: a short day, an overtime day and a weekend catch-up.
SPECIAL_DAYS = {8: 6.2, 13: 10.5, 4: 1.75, 20: 7.0}


def ms(dt: datetime) -> int:
    return int(dt.timestamp() * 1000)


def main() -> None:
    out_path = sys.argv[1]
    now = datetime.fromtimestamp(int(sys.argv[2]), ZONE)
    today = now.date()
    rng = random.Random(42)

    db = sqlite3.connect(out_path)
    c = db.cursor()
    c.executescript(
        f"""
        CREATE TABLE IF NOT EXISTS `issues` (`id` TEXT NOT NULL, `userId` INTEGER NOT NULL, `title` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `sort` INTEGER NOT NULL, PRIMARY KEY(`id`, `userId`));
        CREATE TABLE IF NOT EXISTS `worklogs` (`workId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `jiraWorklogId` INTEGER, `issueId` TEXT NOT NULL, `userId` INTEGER NOT NULL, `jiraUserAccountId` TEXT NOT NULL, `from` INTEGER NOT NULL, `to` INTEGER NOT NULL, `author` TEXT NOT NULL, `summary` TEXT NOT NULL, `pending` INTEGER NOT NULL, `active` INTEGER NOT NULL);
        CREATE UNIQUE INDEX IF NOT EXISTS `index_worklogs_userId_jiraWorklogId` ON `worklogs` (`userId`, `jiraWorklogId`);
        CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `instanceUrl` TEXT NOT NULL, `user` TEXT NOT NULL, `jiraUserAccountId` TEXT NOT NULL, `token` TEXT NOT NULL, `active` INTEGER NOT NULL);
        CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT);
        INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '{IDENTITY_HASH}');
        PRAGMA user_version = 11;
        """
    )

    c.execute(
        "INSERT INTO users VALUES (?,?,?,?,?,?)",
        (USER_ID, "https://example.atlassian.net", "john.smith@example.com", ACCOUNT_ID, "mock-token", 1),
    )
    c.execute(
        "INSERT INTO users VALUES (?,?,?,?,?,?)",
        (2, "https://example-studio.atlassian.net", "john@example.org", "712020:0b8e", "mock-token-2", 0),
    )
    for sort, (key, title, pinned) in enumerate(ISSUES):
        c.execute("INSERT INTO issues VALUES (?,?,?,?,?)", (key, USER_ID, title, int(pinned), sort))

    jira_id = 10500

    def add(issue, start, end, summary, pending=False, active=False):
        nonlocal jira_id
        jid = None
        if not pending:
            jira_id += 1
            jid = jira_id
        c.execute(
            "INSERT INTO worklogs (jiraWorklogId, issueId, userId, jiraUserAccountId, `from`, `to`, author, summary, pending, active) "
            "VALUES (?,?,?,?,?,?,?,?,?,?)",
            (jid, issue, USER_ID, ACCOUNT_ID, ms(start), ms(end),
             "Pending" if pending else AUTHOR, summary, int(pending), int(active)),
        )

    def fill_day(day, hours):
        """Splits [hours] into stand-up plus focus blocks starting in the morning."""
        t = datetime(day.year, day.month, day.day, 8, rng.choice([30, 40, 45, 50]), tzinfo=ZONE)
        remaining = round(hours * 4) / 4
        if day.weekday() < 5 and remaining >= 1:
            add("MEET-12", t, t + timedelta(minutes=15), "Daily stand-up")
            t += timedelta(minutes=15)
            remaining -= 0.25
        lunch_done = False
        focus = rng.sample(WORK_ISSUES, 3)
        while remaining > 0.01:
            block = min(remaining, rng.choice([1.0, 1.5, 1.75, 2.0, 2.5]))
            issue = rng.choice(focus)
            end = t + timedelta(hours=block)
            add(issue, t, end, rng.choice(SUMMARIES[issue]))
            remaining -= block
            t = end + timedelta(minutes=rng.choice([0, 5, 10]))
            if not lunch_done and t.hour >= 12:
                t += timedelta(minutes=45)
                lunch_done = True

    yesterday = today - timedelta(days=1)

    def y(h, m):
        return datetime(yesterday.year, yesterday.month, yesterday.day, h, m, tzinfo=ZONE)

    for days_ago in range(60, 0, -1):
        day = today - timedelta(days=days_ago)
        if day == yesterday and day.weekday() < 5:
            add("MEET-12", y(8, 45), y(9, 0), "Daily stand-up")
            add("APP-482", y(9, 0), y(11, 30), "Added Room cache for cart items and sync queue")
            add("SUP-307", y(11, 30), y(12, 15), "Call with payment provider support")
            add("APP-517", y(13, 0), y(15, 0), "Built new welcome carousel in Compose")
            add("WEB-231", y(15, 0), y(16, 30), "Paginated widget API responses")
            add("MEET-12", y(16, 30), y(17, 0), "Backlog refinement with product")
            add("APP-530", y(17, 0), y(17, 45), "Reproduced crash on Pixel 8, fixed lifecycle leak")
        elif days_ago in SPECIAL_DAYS:
            fill_day(day, SPECIAL_DAYS[days_ago])
        elif day.weekday() < 5:
            fill_day(day, rng.choice([8.0, 8.0, 8.0, 8.25, 8.5, 9.0]))

    def at(h, m):
        return datetime(today.year, today.month, today.day, h, m, tzinfo=ZONE)

    # Today: everything still pending upload, plus a running timer.
    add("SUP-307", at(8, 40), at(9, 30), "Traced duplicate webhook deliveries to missing idempotency key", pending=True)
    add("MEET-12", at(9, 30), at(9, 45), "Daily stand-up", pending=True)
    add("APP-482", at(9, 45), at(12, 10), "Implemented conflict resolution for offline cart edits", pending=True)
    add("WEB-231", at(12, 55), at(13, 40), "Profiled dashboard queries, added index on account_id", pending=True)
    active_start = now - timedelta(minutes=47, seconds=12)
    add("APP-517", active_start, active_start, "", pending=True, active=True)

    db.commit()
    db.close()


if __name__ == "__main__":
    main()
