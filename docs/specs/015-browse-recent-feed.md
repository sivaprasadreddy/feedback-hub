---
UseCase ID : UC-015
Title: Browse Recent Feed
Depends On: [UC-001, UC-005]
Status: COMPLETED
---

## Actors

Authenticated user.

## Preconditions

The reader is authenticated.

## UseCase Details

An authenticated user browses feedback ordered from newest to oldest.

- Recent is the default feed selection.
- Messages are ordered by creation timestamp descending.
- Each feed item presents the information required by UC-006 without exposing anonymous identities.
- Newly created messages appear according to their creation timestamp.

## Business Rules

- Recent is the default feed selection.
- Messages are ordered by creation timestamp descending.
- Each feed item presents the information required by UC-006 without exposing anonymous identities.

## Acceptance Criteria

- Recent is the default feed selection.
- Messages are ordered by creation timestamp descending.
- Each feed item presents the information required by UC-006 without exposing anonymous identities.
- Newly created messages appear according to their creation timestamp.

## In Scope

Browsing the default Recent feed in creation-time order.

## Out of scope

None
