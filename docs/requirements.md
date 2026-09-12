# SpeakUp Requirements

This document translates the requirements in [PRD](prd.md) into implementable use cases. 
Features explicitly described as optional or future work are excluded. 
Statuses reflect the PRD alone and should be updated as implementation progresses.

## Use Case Summary

| UseCase ID | Use Case Title              | Status          |
|------------|-----------------------------|-----------------|
| UC-001     | Authenticate User           | COMPLETED       |
| UC-002     | Create User Account         | COMPLETED       |
| UC-003     | Edit User                   | COMPLETED       |
| UC-004     | View and Filter Users       | COMPLETED       |
| UC-005     | Create Message              | COMPLETED       |
| UC-006     | View Message                | COMPLETED       |
| UC-007     | Edit Own Message            | COMPLETED       |
| UC-008     | Delete Own Message          | COMPLETED       |
| UC-009     | Create Reply                | COMPLETED       |
| UC-010     | View Replies                | COMPLETED       |
| UC-011     | Edit Own Reply              | COMPLETED       |
| UC-012     | Delete Own Reply            | COMPLETED       |
| UC-013     | Vote on Message             | COMPLETED       |
| UC-014     | Vote on Reply               | COMPLETED       |
| UC-015     | Browse Recent Feed          | COMPLETED       |
| UC-016     | Browse Popular Feed         | COMPLETED       |
| UC-017     | Navigate Feed Pages         | COMPLETED       |
| UC-018     | Moderate Message            | COMPLETED       |
| UC-019     | Moderate Reply              | COMPLETED       |

## Detailed Use Cases

### UC-001 — Authenticate User

- **Status:** COMPLETED
- **Depends On:** None
- **Description:** A registered user signs in with an email address and password to establish an authenticated session or token. The system identifies the user's role and account status.
- **Acceptance Criteria:**
  - A user with valid credentials and an active account can authenticate.
  - A successful authentication establishes a session or returns a token associated with the user's identity and role.
  - Invalid credentials are rejected without revealing whether a particular account exists.
  - An inactive user cannot authenticate.
  - Unauthenticated requests to protected feedback and administration features are rejected.

### UC-002 — Create User Account

- **Status:** COMPLETED
- **Depends On:** UC-001
- **Description:** An Admin creates an employee account using the employee's name, unique email address, and initial role.
- **Acceptance Criteria:**
  - Only an authenticated Admin can create an account.
  - Name, email, and role are required.
  - The role must be `ADMIN` or `USER`.
  - The email address must be valid and unique within the deployment.
  - On success, the user is available in the user list with a recorded creation date and account status.
  - A regular User cannot access this operation.

### UC-003 — Edit User

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-002
- **Description:** An Admin edits another account's role and active status to control its permissions and access.
- **Acceptance Criteria:**
  - Only an authenticated Admin can edit another account.
  - The assigned role must be `ADMIN` or `USER`.
  - The Admin can make the account active or inactive in the same edit operation.
  - The new role and active status are persisted and used by subsequent authentication and authorization checks.
  - A deactivated user cannot log in, create messages, create replies, or vote.
  - Deactivating a user invalidates or rejects further use of existing authenticated access.
  - Messages, replies, and votes previously created by the user remain available.
  - Reactivating an account restores its ability to authenticate and participate according to its role.
  - A regular User cannot edit accounts.

### UC-004 — View and Filter Users

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-002
- **Description:** An Admin views the deployment's users and filters them by status or role.
- **Acceptance Criteria:**
  - Only an authenticated Admin can view the user list.
  - Each row displays name, email, role, status, and created date.
  - The list can be filtered by active or inactive status.
  - The list can be filtered by `ADMIN` or `USER` role.
  - A regular User cannot access the user list.

### UC-005 — Create Message

- **Status:** COMPLETED
- **Depends On:** UC-001
- **Description:** An authenticated active user creates a feedback message either under their identity or anonymously.
- **Acceptance Criteria:**
  - An authenticated active Admin or User can create a message with non-empty content.
  - The author must explicitly choose to post as themselves or anonymously.
  - The system always records the authenticated creator internally.
  - An anonymous message is returned to regular users and rendered in the UI with `Anonymous` as its author.
  - The creator's identity is not exposed through regular-user APIs or UI when the message is anonymous.
  - A newly created message appears in the Recent feed.

### UC-006 — View Message

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-005
- **Description:** An authenticated user views a feedback message and its engagement information.
- **Acceptance Criteria:**
  - The view displays the visible author name or `Anonymous`, content, created time, upvote count, downvote count, reply count, and the current user's vote.
  - The actual creator of an anonymous message is never included in responses available to a regular User.
  - Deleted messages display a deletion placeholder rather than their original content.
  - Only authenticated active users can access message details.

### UC-007 — Edit Own Message

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-005
- **Description:** A user edits the content of a message they created while preserving its original posting identity.
- **Acceptance Criteria:**
  - An authenticated active user can edit only their own active message.
  - The updated content must not be empty.
  - Editing updates the message's updated timestamp.
  - Editing cannot change a message from identified to anonymous or from anonymous to identified.
  - Another regular User cannot edit the message.
  - A deleted message cannot be edited.

### UC-008 — Delete Own Message

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-005
- **Description:** A user soft-deletes a message they created so that discussion integrity and audit information are retained.
- **Acceptance Criteria:**
  - An authenticated active user can delete only their own active message.
  - Deletion changes the message status to `DELETED` instead of physically removing the record.
  - The UI displays `This message has been deleted.` in place of the original content.
  - Replies and audit-relevant associations remain stored.
  - Another regular User cannot delete the message.

### UC-009 — Create Reply

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-005
- **Description:** An authenticated active user adds a single-level reply to an active message either under their identity or anonymously.
- **Acceptance Criteria:**
  - An authenticated active Admin or User can add a non-empty reply to an active message.
  - The author must choose to reply as themselves or anonymously.
  - The system records the authenticated creator internally even for an anonymous reply.
  - An anonymous reply exposes only `Anonymous` to regular users through the API and UI.
  - Replies belong directly to a message and cannot be nested beneath other replies.
  - Creating a reply updates the message's displayed reply count.

### UC-010 — View Replies

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-006, UC-009
- **Description:** An authenticated user views the single-level discussion replies associated with a message.
- **Acceptance Criteria:**
  - Replies are displayed beneath their parent message.
  - Each active reply shows the visible author or `Anonymous`, content, timestamps, vote counts, and the current user's vote.
  - The actual creator of an anonymous reply is never exposed through regular-user APIs or UI.
  - Deleted replies display a deletion placeholder rather than their original content.
  - No reply is presented as nested under another reply.

### UC-011 — Edit Own Reply

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-009
- **Description:** A user edits a reply they created while preserving its original posting identity.
- **Acceptance Criteria:**
  - An authenticated active user can edit only their own active reply.
  - Updated reply content must not be empty.
  - Editing updates the reply's updated timestamp.
  - Editing cannot change a reply from identified to anonymous or from anonymous to identified.
  - Another regular User cannot edit the reply.
  - A deleted reply cannot be edited.

### UC-012 — Delete Own Reply

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-009
- **Description:** A user soft-deletes a reply they created while retaining its record for discussion integrity and auditing.
- **Acceptance Criteria:**
  - An authenticated active user can delete only their own active reply.
  - Deletion changes the reply status to `DELETED` instead of physically removing the record.
  - The original reply content is replaced with a deletion placeholder in the UI.
  - The parent message's displayed active reply count is updated consistently.
  - Another regular User cannot delete the reply.

### UC-013 — Vote on Message

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-005, UC-006
- **Description:** An authenticated active user upvotes or downvotes another user's message and may remove or change that vote.
- **Acceptance Criteria:**
  - A user can place an `UPVOTE` or `DOWNVOTE` on an active message created by another user.
  - A user has at most one active vote per message.
  - Selecting the opposite vote changes the existing vote rather than creating a second vote.
  - A user can remove their existing vote.
  - A user cannot vote on their own message, including when it was posted anonymously.
  - Upvote and downvote counts, and the current user's vote, reflect the persisted result immediately.
  - A user cannot vote on a deleted message.

### UC-014 — Vote on Reply

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-009, UC-010
- **Description:** An authenticated active user upvotes or downvotes another user's reply and may remove or change that vote.
- **Acceptance Criteria:**
  - A user can place an `UPVOTE` or `DOWNVOTE` on an active reply created by another user.
  - A user has at most one active vote per reply.
  - Selecting the opposite vote changes the existing vote rather than creating a second vote.
  - A user can remove their existing vote.
  - A user cannot vote on their own reply, including when it was posted anonymously.
  - Vote counts and the current user's vote reflect the persisted result immediately.
  - A user cannot vote on a deleted reply.

### UC-015 — Browse Recent Feed

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-005
- **Description:** An authenticated user browses feedback ordered from newest to oldest.
- **Acceptance Criteria:**
  - Recent is the default feed selection.
  - Messages are ordered by creation timestamp descending.
  - Each feed item presents the information required by UC-006 without exposing anonymous identities.
  - Newly created messages appear according to their creation timestamp.

### UC-016 — Browse Popular Feed

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-005, UC-013
- **Description:** An authenticated user browses feedback ranked by its number of upvotes.
- **Acceptance Criteria:**
  - A user can switch from Recent to Popular.
  - Messages are ordered by upvote count descending.
  - Messages with equal upvote counts are ordered by creation timestamp descending.
  - Downvotes do not affect the MVP popularity ranking.
  - The ranking uses persisted votes and does not expose anonymous identities.

### UC-017 — Navigate Feed Pages

- **Status:** COMPLETED
- **Depends On:** UC-015, UC-016
- **Description:** An authenticated user navigates a large Recent or Popular feed in bounded pages using cursor-based or page/offset pagination.
- **Acceptance Criteria:**
  - Both Recent and Popular feeds return a bounded number of messages per request.
  - The user can request the next set of results until no more results remain.
  - Ordering remains consistent with the selected feed across pages.
  - A message is not duplicated within a single traversal when the underlying data has not changed.
  - Invalid pagination input is rejected with a clear client error.

### UC-018 — Moderate Message

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-005, UC-006
- **Description:** An Admin reviews feedback and soft-deletes an inappropriate message regardless of who created it.
- **Acceptance Criteria:**
  - Only an authenticated Admin can perform an administrative deletion of another user's message.
  - Moderation uses soft deletion and preserves the message record and discussion associations.
  - The deleted message displays the standard deletion placeholder to users.
  - The operation records sufficient information to identify that an administrative moderation action occurred.
  - Moderating anonymous content does not reveal its creator to regular users.

### UC-019 — Moderate Reply

- **Status:** COMPLETED
- **Depends On:** UC-001, UC-009, UC-010
- **Description:** An Admin reviews discussion replies and soft-deletes an inappropriate reply regardless of who created it.
- **Acceptance Criteria:**
  - Only an authenticated Admin can perform an administrative deletion of another user's reply.
  - Moderation uses soft deletion and preserves the reply record and audit-relevant associations.
  - The deleted reply displays the standard deletion placeholder to users.
  - The parent message's displayed active reply count is updated consistently.
  - The operation records sufficient information to identify that an administrative moderation action occurred.
  - Moderating an anonymous reply does not reveal its creator to regular users.
