---
UseCase ID : UC-008
Title: Delete Own Message
Depends On: [UC-001, UC-005]
Status: COMPLETED
---

## Actors

Authenticated active message author.

## Preconditions

The author is authenticated and active; their message exists and is active.

## UseCase Details

A user soft-deletes a message they created so that discussion integrity and audit information are retained.

- An authenticated active user can delete only their own active message.
- Deletion changes the message status to `DELETED` instead of physically removing the record.
- The UI displays `This message has been deleted.` in place of the original content.
- Another regular User cannot delete the message.

## Business Rules

- An authenticated active user can delete only their own active message.
- Deletion changes the message status to `DELETED` instead of physically removing the record.
- Replies and audit-relevant associations remain stored.
- Another regular User cannot delete the message.

## Acceptance Criteria

- An authenticated active user can delete only their own active message.
- Deletion changes the message status to `DELETED` instead of physically removing the record.
- The UI displays `This message has been deleted.` in place of the original content.
- Replies and audit-relevant associations remain stored.
- Another regular User cannot delete the message.

## In Scope

Soft deletion of an owned message and display of its deletion placeholder.

## Out of scope

Physical removal of the message record.
