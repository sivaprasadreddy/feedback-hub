---
UseCase ID : UC-013
Title: Vote on Message
Depends On: [UC-001, UC-005, UC-006]
Status: COMPLETED
---

## Actors

Authenticated active user.

## Preconditions

The voter is authenticated and active; the target message exists.

## UseCase Details

An authenticated active user upvotes or downvotes another user's message and may remove or change that vote.

- A user can place an `UPVOTE` or `DOWNVOTE` on an active message created by another user.
- Selecting the opposite vote changes the existing vote rather than creating a second vote.
- A user can remove their existing vote.
- A user cannot vote on their own message, including when it was posted anonymously.
- Upvote and downvote counts, and the current user's vote, reflect the persisted result immediately.
- A user cannot vote on a deleted message.

## Business Rules

- A user can place an `UPVOTE` or `DOWNVOTE` on an active message created by another user.
- A user has at most one active vote per message.
- A user cannot vote on their own message, including when it was posted anonymously.
- A user cannot vote on a deleted message.

## Acceptance Criteria

- A user can place an `UPVOTE` or `DOWNVOTE` on an active message created by another user.
- A user has at most one active vote per message.
- Selecting the opposite vote changes the existing vote rather than creating a second vote.
- A user can remove their existing vote.
- A user cannot vote on their own message, including when it was posted anonymously.
- Upvote and downvote counts, and the current user's vote, reflect the persisted result immediately.
- A user cannot vote on a deleted message.

## In Scope

Adding, changing, or removing a message vote and displaying the persisted counts and current vote.

## Out of scope

Voting on the user’s own messages or deleted messages.
