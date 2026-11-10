---
UseCase ID : UC-014
Title: Vote on Reply
Depends On: [UC-001, UC-009, UC-010]
Status: COMPLETED
---

## Actors

Authenticated active user.

## Preconditions

The voter is authenticated and active; the target reply exists.

## UseCase Details

An authenticated active user upvotes or downvotes another user's reply and may remove or change that vote.

- A user can place an `UPVOTE` or `DOWNVOTE` on an active reply created by another user.
- Selecting the opposite vote changes the existing vote rather than creating a second vote.
- A user can remove their existing vote.
- A user cannot vote on their own reply, including when it was posted anonymously.
- Vote counts and the current user's vote reflect the persisted result immediately.
- A user cannot vote on a deleted reply.

## Business Rules

- A user can place an `UPVOTE` or `DOWNVOTE` on an active reply created by another user.
- A user has at most one active vote per reply.
- A user cannot vote on their own reply, including when it was posted anonymously.
- A user cannot vote on a deleted reply.

## Acceptance Criteria

- A user can place an `UPVOTE` or `DOWNVOTE` on an active reply created by another user.
- A user has at most one active vote per reply.
- Selecting the opposite vote changes the existing vote rather than creating a second vote.
- A user can remove their existing vote.
- A user cannot vote on their own reply, including when it was posted anonymously.
- Vote counts and the current user's vote reflect the persisted result immediately.
- A user cannot vote on a deleted reply.

## In Scope

Adding, changing, or removing a reply vote and displaying the persisted counts and current vote.

## Out of scope

Voting on the user’s own replies or deleted replies.
