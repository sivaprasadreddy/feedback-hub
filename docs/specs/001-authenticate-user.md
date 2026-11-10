---
UseCase ID : UC-001
Title: Authenticate User
Depends On: []
Status: COMPLETED
---

## Actors

Registered user.

## Preconditions

The user has a registered account and supplies an email address and password.

## UseCase Details

A registered user signs in with an email address and password to establish an authenticated session or token. The system identifies the user's role and account status.

- A user with valid credentials and an active account can authenticate.
- A successful authentication establishes a session or returns a token associated with the user's identity and role.
- Invalid credentials are rejected without revealing whether a particular account exists.
- An inactive user cannot authenticate.
- Unauthenticated requests to protected feedback and administration features are rejected.

## Business Rules

- Invalid credentials are rejected without revealing whether a particular account exists.
- An inactive user cannot authenticate.
- Unauthenticated requests to protected feedback and administration features are rejected.

## Acceptance Criteria

- A user with valid credentials and an active account can authenticate.
- A successful authentication establishes a session or returns a token associated with the user's identity and role.
- Invalid credentials are rejected without revealing whether a particular account exists.
- An inactive user cannot authenticate.
- Unauthenticated requests to protected feedback and administration features are rejected.

## In Scope

Authentication and protection of authenticated features.

## Out of scope

None
