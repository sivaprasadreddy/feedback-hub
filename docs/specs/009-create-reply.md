---
UseCase ID : UC-009
Title: Create Reply
Depends On: [UC-001, UC-005]
Status: COMPLETED
---

## Actors

Authenticated active Admin or User.

## Preconditions

The author is authenticated and active; the parent message is active.

## UseCase Details

An authenticated active user adds a single-level reply to an active message either under their identity or anonymously.

- An authenticated active Admin or User can add a non-empty reply to an active message.
- The author must choose to reply as themselves or anonymously.
- An anonymous reply exposes only `Anonymous` to regular users through the API and UI.
- Creating a reply updates the message's displayed reply count.

## Business Rules

- An authenticated active Admin or User can add a non-empty reply to an active message.
- The author must choose to reply as themselves or anonymously.
- The system records the authenticated creator internally even for an anonymous reply.
- An anonymous reply exposes only `Anonymous` to regular users through the API and UI.
- Replies belong directly to a message and cannot be nested beneath other replies.

## Acceptance Criteria

- An authenticated active Admin or User can add a non-empty reply to an active message.
- The author must choose to reply as themselves or anonymously.
- The system records the authenticated creator internally even for an anonymous reply.
- An anonymous reply exposes only `Anonymous` to regular users through the API and UI.
- Replies belong directly to a message and cannot be nested beneath other replies.
- Creating a reply updates the message's displayed reply count.

## In Scope

Creating identified or anonymous single-level replies and updating the message reply count.

## Out of scope

Nested replies.
