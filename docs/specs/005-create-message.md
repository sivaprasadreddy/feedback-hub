---
UseCase ID : UC-005
Title: Create Message
Depends On: [UC-001]
Status: COMPLETED
---

## Actors

Authenticated active Admin or User.

## Preconditions

The author is authenticated and active.

## UseCase Details

An authenticated active user creates a feedback message either under their identity or anonymously.

- An authenticated active Admin or User can create a message with non-empty content.
- The author must explicitly choose to post as themselves or anonymously.
- An anonymous message is returned to regular users and rendered in the UI with `Anonymous` as its author.
- A newly created message appears in the Recent feed.

## Business Rules

- An authenticated active Admin or User can create a message with non-empty content.
- The author must explicitly choose to post as themselves or anonymously.
- The system always records the authenticated creator internally.
- The creator's identity is not exposed through regular-user APIs or UI when the message is anonymous.

## Acceptance Criteria

- An authenticated active Admin or User can create a message with non-empty content.
- The author must explicitly choose to post as themselves or anonymously.
- The system always records the authenticated creator internally.
- An anonymous message is returned to regular users and rendered in the UI with `Anonymous` as its author.
- The creator's identity is not exposed through regular-user APIs or UI when the message is anonymous.
- A newly created message appears in the Recent feed.

## In Scope

Creating identified or anonymous feedback messages and displaying them in Recent.

## Out of scope

None
