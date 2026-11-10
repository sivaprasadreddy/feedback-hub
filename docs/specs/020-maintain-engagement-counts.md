---
UseCase ID : UC-020
Title: Maintain Engagement Counts
Depends On: [UC-005, UC-009, UC-012, UC-013, UC-014, UC-016, UC-019]
Status: COMPLETED
---

## Actors

System; users performing vote and reply operations; Admins moderating replies.

## Preconditions

A message or reply operation changes engagement, a view reads engagement counts, or the database migration initializes counters.

## UseCase Details

The system stores and transactionally maintains denormalized engagement counts on messages and replies so that frequently used counts and popularity rankings can be read without on-demand aggregation.

- Creating, removing, or changing a message vote updates the corresponding message counters and vote record atomically.
- Creating, removing, or changing a reply vote updates the corresponding reply counters and vote record atomically.
- Creating an active reply increments its parent message's reply count.
- Soft-deleting an active reply, whether by its author or an Admin, decrements its parent message's reply count exactly once.
- If either the detail-record change or its counter update fails, the complete operation is rolled back.
- Message and reply views read engagement counts directly from their parent records instead of calculating them through aggregate queries.
- Popular messages are ordered using the stored message upvote count, with creation time descending as the tie-breaker.
- The database migration backfills all counters from existing votes and active replies before the application reads the new columns.

## Business Rules

- Each message stores non-null `upvote_count`, `downvote_count`, and `reply_count` values that default to zero and cannot be negative.
- Each reply stores non-null `upvote_count` and `downvote_count` values that default to zero and cannot be negative.
- Creating, removing, or changing a message vote updates the corresponding message counters and vote record atomically.
- Creating, removing, or changing a reply vote updates the corresponding reply counters and vote record atomically.
- Creating an active reply increments its parent message's reply count.
- Soft-deleting an active reply, whether by its author or an Admin, decrements its parent message's reply count exactly once.
- Concurrent votes and reply operations use atomic counter updates and do not lose increments or decrements.
- If either the detail-record change or its counter update fails, the complete operation is rolled back.
- Popular messages are ordered using the stored message upvote count, with creation time descending as the tie-breaker.
- Individual message and reply vote records remain stored to enforce one vote per user and determine the current user's vote.
- The database migration backfills all counters from existing votes and active replies before the application reads the new columns.

## Acceptance Criteria

- Each message stores non-null `upvote_count`, `downvote_count`, and `reply_count` values that default to zero and cannot be negative.
- Each reply stores non-null `upvote_count` and `downvote_count` values that default to zero and cannot be negative.
- Creating, removing, or changing a message vote updates the corresponding message counters and vote record atomically.
- Creating, removing, or changing a reply vote updates the corresponding reply counters and vote record atomically.
- Creating an active reply increments its parent message's reply count.
- Soft-deleting an active reply, whether by its author or an Admin, decrements its parent message's reply count exactly once.
- Concurrent votes and reply operations use atomic counter updates and do not lose increments or decrements.
- If either the detail-record change or its counter update fails, the complete operation is rolled back.
- Message and reply views read engagement counts directly from their parent records instead of calculating them through aggregate queries.
- Popular messages are ordered using the stored message upvote count, with creation time descending as the tie-breaker.
- Individual message and reply vote records remain stored to enforce one vote per user and determine the current user's vote.
- The database migration backfills all counters from existing votes and active replies before the application reads the new columns.
- Automated tests cover vote creation, removal and type changes, reply creation and deletion, migrated data, rollback behavior, and concurrent counter updates.

## In Scope

Persisted engagement counters, atomic updates, concurrency, rollback, migration backfill, and automated coverage of the documented scenarios.

## Out of scope

On-demand aggregation of engagement counts for message and reply views.
