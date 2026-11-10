---
UseCase ID : UC-017
Title: Navigate Feed Pages
Depends On: [UC-015, UC-016]
Status: COMPLETED
---

## Actors

Authenticated user.

## Preconditions

The reader is authenticated and is browsing the Recent or Popular feed.

## UseCase Details

An authenticated user navigates a large Recent or Popular feed in bounded pages using cursor-based or page/offset pagination.

- Both Recent and Popular feeds return a bounded number of messages per request.
- The user can request the next set of results until no more results remain.
- Ordering remains consistent with the selected feed across pages.
- A message is not duplicated within a single traversal when the underlying data has not changed.
- Invalid pagination input is rejected with a clear client error.

## Business Rules

- Both Recent and Popular feeds return a bounded number of messages per request.
- Ordering remains consistent with the selected feed across pages.
- A message is not duplicated within a single traversal when the underlying data has not changed.
- Invalid pagination input is rejected with a clear client error.

## Acceptance Criteria

- Both Recent and Popular feeds return a bounded number of messages per request.
- The user can request the next set of results until no more results remain.
- Ordering remains consistent with the selected feed across pages.
- A message is not duplicated within a single traversal when the underlying data has not changed.
- Invalid pagination input is rejected with a clear client error.

## In Scope

Bounded feed pages and traversal until no more results remain.

## Out of scope

None
