---
UseCase ID : UC-018
Title: Moderate Message
Depends On: [UC-001, UC-005, UC-006]
Status: COMPLETED
---

## Actors

Admin.

## Preconditions

The Admin is authenticated; the target message exists.

## UseCase Details

An Admin reviews feedback and soft-deletes an inappropriate message regardless of who created it.

- Only an authenticated Admin can perform an administrative deletion of another user's message.
- Moderation uses soft deletion and preserves the message record and discussion associations.
- The deleted message displays the standard deletion placeholder to users.
- The operation records sufficient information to identify that an administrative moderation action occurred.

## Business Rules

- Only an authenticated Admin can perform an administrative deletion of another user's message.
- Moderation uses soft deletion and preserves the message record and discussion associations.
- The operation records sufficient information to identify that an administrative moderation action occurred.
- Moderating anonymous content does not reveal its creator to regular users.

## Acceptance Criteria

- Only an authenticated Admin can perform an administrative deletion of another user's message.
- Moderation uses soft deletion and preserves the message record and discussion associations.
- The deleted message displays the standard deletion placeholder to users.
- The operation records sufficient information to identify that an administrative moderation action occurred.
- Moderating anonymous content does not reveal its creator to regular users.

## In Scope

Administrative soft deletion of messages, deletion placeholders, and moderation audit information.

## Out of scope

Physical removal of the message record or disclosure of anonymous creators to regular users.
