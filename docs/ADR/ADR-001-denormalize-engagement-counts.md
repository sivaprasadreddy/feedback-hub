# ADR-001: Store Engagement Counts on Messages and Replies

* **Status:** Accepted
* **Date:** 2026-09-14

## Context

Message votes, reply votes, and replies are stored in separate tables. Displaying a message currently requires aggregating those tables to calculate its upvote, downvote, and active reply counts. Reply vote counts are calculated in the same way.

These on-demand aggregations add query and mapping complexity, require additional database work for common read paths, and make popularity sorting more expensive. The counts are shown frequently, while the underlying votes and replies change less often.

The individual vote records must remain available to enforce one vote per user, identify a user's current vote, and support changing or removing a vote.

## Decision

Store denormalized engagement counters directly on their parent records:

| Table      | New columns                                     |
|------------|-------------------------------------------------|
| `messages` | `upvote_count`, `downvote_count`, `reply_count` |
| `replies`  | `upvote_count`, `downvote_count`                |

All counter columns will be non-null, default to zero, and have database constraints preventing negative values.

The `message_votes` and `reply_votes` tables remain the source of truth for who voted and which vote they selected. The `replies` table remains the source of truth for the replies belonging to a message. The new columns are transactionally maintained projections of that data, optimized for reads.

### Counter updates

Each command will update the detail record and its counter in the same database transaction:

* Adding an upvote increments `upvote_count`; adding a downvote increments `downvote_count`.
* Removing a vote decrements the counter for its existing type.
* Changing an upvote to a downvote decrements `upvote_count` and increments `downvote_count`; the reverse transition performs the opposite updates.
* Creating an active reply increments the parent message's `reply_count`.
* Changing an active reply to deleted decrements `reply_count`. Any future transition that restores a deleted reply must increment it again.

Counter changes will use atomic database updates, such as `counter = counter + :delta`, and verify that exactly one parent row was updated. This avoids lost updates when multiple users vote or reply concurrently. A command must fail and roll back completely if either its detail-row change or counter update fails.

Reads will obtain aggregate counts from `messages` and `replies` rather than calculating them with joins and `GROUP BY`. Popular-message ordering will use `messages.upvote_count` and can be supported with an index if query performance requires one.

## Migration

The database migration will:

1. Add the five counter columns with a default value of zero.
2. Backfill message vote counts from `message_votes`, reply vote counts from `reply_votes`, and active reply counts from `replies`.
3. Mark the columns as non-null and add non-negative check constraints.
4. Add any index required by the popular-message query.

The backfill must run before application code starts reading the new columns. After deployment, a reconciliation query or maintenance task can periodically compare stored counters with their source tables to detect inconsistencies caused by manual data changes or defects.

## Consequences

### Positive

* Message and reply views no longer require aggregate count queries.
* Feed queries and popularity sorting become simpler and more efficient.
* Counts are available as part of the parent entity and its normal query.
* Read performance remains predictable as vote and reply tables grow.

### Negative

* Vote and reply write paths become more complex because counters must remain synchronized.
* Each relevant write updates an additional row or additional columns.
* Incorrect application logic or out-of-band database changes can cause counter drift.
* Tests must cover every create, remove, change, delete, and concurrent-update transition.

## Alternatives Considered

### Continue calculating counts on demand

This keeps a single normalized representation but retains the existing query complexity and repeated aggregation cost on high-frequency read paths.

### Cache calculated counts

A cache reduces some database reads but introduces invalidation and consistency concerns while still requiring a durable calculation path.

### Use database triggers

Triggers can maintain counters independently of the application, including for out-of-band writes. They also hide business behavior in the database, complicate testing and migrations, and split ownership of the voting workflow. Application-managed transactional updates are preferred for this system.

### Use a materialized view

A materialized view is suitable for eventually consistent analytics but requires refresh management and does not provide immediately updated counts after a user action.
