---
UseCase ID : UC-004
Title: View and Filter Users
Depends On: [UC-001, UC-002]
Status: COMPLETED
---

## Actors

Admin.

## Preconditions

The Admin is authenticated.

## UseCase Details

An Admin views the deployment's users and filters them by status or role.

- Each row displays name, email, role, status, and created date.
- The list can be filtered by active or inactive status.
- The list can be filtered by `ADMIN` or `USER` role.
- A regular User cannot access the user list.

## Business Rules

- Only an authenticated Admin can view the user list.
- The list can be filtered by active or inactive status.
- The list can be filtered by `ADMIN` or `USER` role.
- A regular User cannot access the user list.

## Acceptance Criteria

- Only an authenticated Admin can view the user list.
- Each row displays name, email, role, status, and created date.
- The list can be filtered by active or inactive status.
- The list can be filtered by `ADMIN` or `USER` role.
- A regular User cannot access the user list.

## In Scope

Viewing users and filtering by role or active status.

## Out of scope

None
