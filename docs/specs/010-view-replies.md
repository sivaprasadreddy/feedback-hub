---
UseCase ID : UC-010
Title: View Replies
Depends On: [UC-001, UC-006, UC-009]
Status: COMPLETED
---

## Actors

Authenticated user.

## Preconditions

The viewer is authenticated; the parent message exists.

## UseCase Details

An authenticated user views the single-level discussion replies associated with a message.

- Replies are displayed beneath their parent message.
- Each active reply shows the visible author or `Anonymous`, content, timestamps, vote counts, and the current user's vote.
- Deleted replies display a deletion placeholder rather than their original content.

## Business Rules

- The actual creator of an anonymous reply is never exposed through regular-user APIs or UI.
- Deleted replies display a deletion placeholder rather than their original content.
- No reply is presented as nested under another reply.

## Acceptance Criteria

- Replies are displayed beneath their parent message.
- Each active reply shows the visible author or `Anonymous`, content, timestamps, vote counts, and the current user's vote.
- The actual creator of an anonymous reply is never exposed through regular-user APIs or UI.
- Deleted replies display a deletion placeholder rather than their original content.
- No reply is presented as nested under another reply.

## In Scope

Viewing replies beneath a message, including engagement information and deletion placeholders.

## Out of scope

Nested reply presentation.
