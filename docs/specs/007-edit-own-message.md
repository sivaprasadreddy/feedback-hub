---
UseCase ID : UC-007
Title: Edit Own Message
Depends On: [UC-001, UC-005]
Status: COMPLETED
---

## Actors

Authenticated active message author.

## Preconditions

The author is authenticated and active; their message exists and is active.

## UseCase Details

A user edits the content of a message they created while preserving its original posting identity.

- An authenticated active user can edit only their own active message.
- The updated content must not be empty.
- Editing updates the message's updated timestamp.
- Another regular User cannot edit the message.
- A deleted message cannot be edited.

## Business Rules

- An authenticated active user can edit only their own active message.
- The updated content must not be empty.
- Editing cannot change a message from identified to anonymous or from anonymous to identified.
- Another regular User cannot edit the message.
- A deleted message cannot be edited.

## Acceptance Criteria

- An authenticated active user can edit only their own active message.
- The updated content must not be empty.
- Editing updates the message's updated timestamp.
- Editing cannot change a message from identified to anonymous or from anonymous to identified.
- Another regular User cannot edit the message.
- A deleted message cannot be edited.

## In Scope

Editing the content of an active message owned by the user.

## Out of scope

Changing the message’s posting identity.
