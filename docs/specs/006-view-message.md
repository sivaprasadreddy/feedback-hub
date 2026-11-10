---
UseCase ID : UC-006
Title: View Message
Depends On: [UC-001, UC-005]
Status: COMPLETED
---

## Actors

Authenticated active user.

## Preconditions

The viewer is authenticated and active; the message exists.

## UseCase Details

An authenticated user views a feedback message and its engagement information.

- The view displays the visible author name or `Anonymous`, content, created time, upvote count, downvote count, reply count, and the current user's vote.
- Deleted messages display a deletion placeholder rather than their original content.
- Only authenticated active users can access message details.

## Business Rules

- The actual creator of an anonymous message is never included in responses available to a regular User.
- Deleted messages display a deletion placeholder rather than their original content.
- Only authenticated active users can access message details.

## Acceptance Criteria

- The view displays the visible author name or `Anonymous`, content, created time, upvote count, downvote count, reply count, and the current user's vote.
- The actual creator of an anonymous message is never included in responses available to a regular User.
- Deleted messages display a deletion placeholder rather than their original content.
- Only authenticated active users can access message details.

## In Scope

Viewing message content, visible authorship, engagement information, and deletion placeholders.

## Out of scope

None
