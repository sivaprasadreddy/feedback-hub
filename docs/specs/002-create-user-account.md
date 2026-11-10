---
UseCase ID : UC-002
Title: Create User Account
Depends On: [UC-001]
Status: COMPLETED
---

## Actors

Admin.

## Preconditions

The Admin is authenticated.

## UseCase Details

An Admin creates an employee account using the employee's name, unique email address, and initial role.

- Only an authenticated Admin can create an account.
- On success, the user is available in the user list with a recorded creation date and account status.
- A regular User cannot access this operation.

## Business Rules

- Only an authenticated Admin can create an account.
- Name, email, and role are required.
- The role must be `ADMIN` or `USER`.
- The email address must be valid and unique within the deployment.
- A regular User cannot access this operation.

## Acceptance Criteria

- Only an authenticated Admin can create an account.
- Name, email, and role are required.
- The role must be `ADMIN` or `USER`.
- The email address must be valid and unique within the deployment.
- On success, the user is available in the user list with a recorded creation date and account status.
- A regular User cannot access this operation.

## In Scope

Administrative account creation and availability in the user list.

## Out of scope

None
