---
UseCase ID : UC-019
Title: Moderate Reply
Depends On: [UC-001, UC-009, UC-010]
Status: COMPLETED
---

## Actors

Admin.

## Preconditions

The Admin is authenticated; the target reply exists.

## UseCase Details

An Admin reviews discussion replies and soft-deletes an inappropriate reply regardless of who created it.

- Only an authenticated Admin can perform an administrative deletion of another user's reply.
- Moderation uses soft deletion and preserves the reply record and audit-relevant associations.
- The deleted reply displays the standard deletion placeholder to users.
- The parent message's displayed active reply count is updated consistently.
- The operation records sufficient information to identify that an administrative moderation action occurred.

## Business Rules

- Only an authenticated Admin can perform an administrative deletion of another user's reply.
- Moderation uses soft deletion and preserves the reply record and audit-relevant associations.
- The parent message's displayed active reply count is updated consistently.
- The operation records sufficient information to identify that an administrative moderation action occurred.
- Moderating an anonymous reply does not reveal its creator to regular users.

## Acceptance Criteria

- Only an authenticated Admin can perform an administrative deletion of another user's reply.
- Moderation uses soft deletion and preserves the reply record and audit-relevant associations.
- The deleted reply displays the standard deletion placeholder to users.
- The parent message's displayed active reply count is updated consistently.
- The operation records sufficient information to identify that an administrative moderation action occurred.
- Moderating an anonymous reply does not reveal its creator to regular users.

## In Scope

Administrative soft deletion of replies, deletion placeholders, reply-count updates, and moderation audit information.

## Out of scope

Physical removal of the reply record or disclosure of anonymous creators to regular users.
