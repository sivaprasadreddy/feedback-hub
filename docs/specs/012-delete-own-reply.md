---
UseCase ID : UC-012
Title: Delete Own Reply
Depends On: [UC-001, UC-009]
Status: COMPLETED
---

## Actors

Authenticated active reply author.

## Preconditions

The author is authenticated and active; their reply exists and is active.

## UseCase Details

A user soft-deletes a reply they created while retaining its record for discussion integrity and auditing.

- An authenticated active user can delete only their own active reply.
- Deletion changes the reply status to `DELETED` instead of physically removing the record.
- The original reply content is replaced with a deletion placeholder in the UI.
- The parent message's displayed active reply count is updated consistently.
- Another regular User cannot delete the reply.

## Business Rules

- An authenticated active user can delete only their own active reply.
- Deletion changes the reply status to `DELETED` instead of physically removing the record.
- The parent message's displayed active reply count is updated consistently.
- Another regular User cannot delete the reply.

## Acceptance Criteria

- An authenticated active user can delete only their own active reply.
- Deletion changes the reply status to `DELETED` instead of physically removing the record.
- The original reply content is replaced with a deletion placeholder in the UI.
- The parent message's displayed active reply count is updated consistently.
- Another regular User cannot delete the reply.

## In Scope

Soft deletion of an owned reply, its deletion placeholder, and the parent message reply count.

## Out of scope

Physical removal of the reply record.
