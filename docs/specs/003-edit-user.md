---
UseCase ID : UC-003
Title: Edit User
Depends On: [UC-001, UC-002]
Status: COMPLETED
---

## Actors

Admin.

## Preconditions

The Admin is authenticated and the account to edit exists.

## UseCase Details

An Admin edits another account's role and active status to control its permissions and access.

- The Admin can make the account active or inactive in the same edit operation.
- The new role and active status are persisted and used by subsequent authentication and authorization checks.
- A deactivated user cannot log in, create messages, create replies, or vote.
- Deactivating a user invalidates or rejects further use of existing authenticated access.
- Reactivating an account restores its ability to authenticate and participate according to its role.
- A regular User cannot edit accounts.

## Business Rules

- Only an authenticated Admin can edit another account.
- The assigned role must be `ADMIN` or `USER`.
- A deactivated user cannot log in, create messages, create replies, or vote.
- Deactivating a user invalidates or rejects further use of existing authenticated access.
- Messages, replies, and votes previously created by the user remain available.
- A regular User cannot edit accounts.

## Acceptance Criteria

- Only an authenticated Admin can edit another account.
- The assigned role must be `ADMIN` or `USER`.
- The Admin can make the account active or inactive in the same edit operation.
- The new role and active status are persisted and used by subsequent authentication and authorization checks.
- A deactivated user cannot log in, create messages, create replies, or vote.
- Deactivating a user invalidates or rejects further use of existing authenticated access.
- Messages, replies, and votes previously created by the user remain available.
- Reactivating an account restores its ability to authenticate and participate according to its role.
- A regular User cannot edit accounts.

## In Scope

Editing account roles and active status, including access changes and preservation of existing contributions.

## Out of scope

None
