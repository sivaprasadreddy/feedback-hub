---
UseCase ID : UC-016
Title: Browse Popular Feed
Depends On: [UC-001, UC-005, UC-013]
Status: COMPLETED
---

## Actors

Authenticated user.

## Preconditions

The reader is authenticated.

## UseCase Details

An authenticated user browses feedback ranked by its number of upvotes.

- A user can switch from Recent to Popular.
- Messages are ordered by upvote count descending.
- Messages with equal upvote counts are ordered by creation timestamp descending.
- The ranking uses persisted votes and does not expose anonymous identities.

## Business Rules

- Messages are ordered by upvote count descending.
- Messages with equal upvote counts are ordered by creation timestamp descending.
- Downvotes do not affect the MVP popularity ranking.
- The ranking uses persisted votes and does not expose anonymous identities.

## Acceptance Criteria

- A user can switch from Recent to Popular.
- Messages are ordered by upvote count descending.
- Messages with equal upvote counts are ordered by creation timestamp descending.
- Downvotes do not affect the MVP popularity ranking.
- The ranking uses persisted votes and does not expose anonymous identities.

## In Scope

Switching to Popular and ranking messages by upvote count with creation-time tie-breaking.

## Out of scope

Using downvotes to rank popularity in the MVP.
