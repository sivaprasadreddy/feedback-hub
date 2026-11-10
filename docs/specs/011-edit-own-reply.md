---
UseCase ID : UC-011
Title: Edit Own Reply
Depends On: [UC-001, UC-009]
Status: COMPLETED
---

## Actors

Authenticated active reply author.

## Preconditions

The author is authenticated and active; their reply exists and is active.

## UseCase Details

A user edits a reply they created while preserving its original posting identity.

- An authenticated active user can edit only their own active reply.
- Updated reply content must not be empty.
- Editing updates the reply's updated timestamp.
- Another regular User cannot edit the reply.
- A deleted reply cannot be edited.

## Business Rules

- An authenticated active user can edit only their own active reply.
- Updated reply content must not be empty.
- Editing cannot change a reply from identified to anonymous or from anonymous to identified.
- Another regular User cannot edit the reply.
- A deleted reply cannot be edited.

## Acceptance Criteria

- An authenticated active user can edit only their own active reply.
- Updated reply content must not be empty.
- Editing updates the reply's updated timestamp.
- Editing cannot change a reply from identified to anonymous or from anonymous to identified.
- Another regular User cannot edit the reply.
- A deleted reply cannot be edited.

## In Scope

Editing the content of an active reply owned by the user.

## Out of scope

Changing the reply’s posting identity.
