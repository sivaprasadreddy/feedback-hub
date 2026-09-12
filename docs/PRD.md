# SpeakUp PRD

## 1. Product Overview

A multi-tenant feedback management platform where organizations provide their employees with a private space to:

* Express feedback, opinions, suggestions, concerns, or ideas.
* Participate in discussions through replies.
* Post or reply using their identity or anonymously.
* Upvote/downvote messages and replies.
* Discover popular and recent discussions.
* Allow organization administrators to manage users.

The system is designed around the principle:

> **Every organization has its own private feedback community.**

Users belonging to one organization must never be able to access another organization's data.

---

# 2. Goals

## 2.1 Primary Goals

1. Provide each organization with a private feedback/discussion space.
2. Allow employees to freely express opinions.
3. Support anonymous participation without exposing the employee's identity to other users.
4. Enable discussions through replies.
5. Allow employees to vote on messages and replies.
6. Make useful/popular feedback discoverable.
7. Provide organization administrators with basic user management.
8. Provide SuperAdmins with tenant and organization management.
9. Enforce strict tenant isolation.

## 2.2 Secondary Goals

* Encourage constructive employee discussions.
* Provide organizations with visibility into employee sentiment and recurring topics.
* Provide a foundation for future moderation, notifications, analytics, and integrations.

---

# 3. Non-Goals for MVP

The first version should **not** attempt to become a full employee engagement platform.

The following can be deferred:

* Surveys and questionnaires.
* Performance reviews.
* 1:1 management.
* HR workflows.
* Slack/Teams integration.
* Email digests.
* Advanced sentiment analysis.
* AI-generated summaries.
* Complex moderation workflows.
* Gamification.
* Badges/reputation systems.
* Multiple organizational hierarchies.
* Cross-tenant users.

---

# 4. Actors and Roles

There are three primary roles.

| Actor      | Scope         | Responsibilities                       |
| ---------- | ------------- | -------------------------------------- |
| SuperAdmin | Entire system | Manage organizations and their users   |
| Admin      | Organization  | Manage users within their organization |
| User       | Organization  | Create posts, replies and votes        |

### 4.1 SuperAdmin

The SuperAdmin is a platform-level administrator.

Can:

* Create organizations.
* View organizations.
* Edit organization information.
* Activate/deactivate organizations.
* Create users for an organization.
* Assign `ADMIN` or `USER` roles.
* Reset/deactivate users.
* View organization-level information.

SuperAdmin can access data across tenants because this is a platform administration function.

---

### 4.2 Admin

An Admin belongs to exactly one organization.

Can:

* View organization users.
* Create users.
* Assign `ADMIN` or `USER` roles.
* Activate/deactivate users.
* View feedback within their organization.
* Participate in discussions like any other employee.

An Admin **cannot**:

* Access another organization.
* Create organizations.
* Modify platform-level configuration.
* Change the organization they belong to.

---

### 4.3 User

A regular employee can:

* View messages.
* Create messages.
* Reply to messages.
* Vote on messages.
* Vote on replies.
* Post anonymously.
* Reply anonymously.
* Edit/delete their own content according to the applicable rules.

---

# 5. Multi-Tenant Model

The fundamental hierarchy is:

```text
Platform
│
├── Organization A
│   ├── Admin
│   ├── User
│   ├── User
│   └── Feedback
│       ├── Message
│       └── Replies
│
├── Organization B
│   ├── Admin
│   ├── User
│   └── Feedback
│
└── Organization C
    ├── Admin
    └── Users
```

Every organization represents a **tenant**.

All tenant-owned entities should contain or be resolvable to an `organization_id`.

For example:

```text
organization
user
message
reply
message_vote
reply_vote
```

Tenant isolation is a critical security requirement.

> A user from Organization A must never be able to retrieve, modify, vote on, or reply to content belonging to Organization B.

---

# 6. Core Concepts

## 6.1 Message

A message is the primary piece of feedback posted by an employee.

Examples:

* "We should have more flexible working hours."
* "The cafeteria food has improved significantly."
* "The deployment process is taking too long."
* "I'd like to suggest introducing learning Fridays."

A message contains:

* Author.
* Content.
* Created timestamp.
* Updated timestamp.
* Anonymous/public author indicator.
* Upvote count.
* Downvote count.
* Reply count.
* Status.

---

## 6.2 Reply

A reply belongs to a message.

Example:

```text
Message:
"We should have more flexible working hours."

    ├── Reply:
    "I completely agree."
    │
    ├── Reply:
    "I'd prefer flexible start/end times."
    │
    └── Reply:
        "This would be difficult for customer-facing teams."
```

### MVP recommendation

Keep replies **single-level**.

That means:

```text
Message
 ├── Reply
 ├── Reply
 └── Reply
```

rather than:

```text
Message
 └── Reply
      └── Reply
           └── Reply
```

Nested discussions add significant complexity without being necessary for the initial product.

---

# 7. Anonymous Participation

Users can choose whether to publish as themselves or anonymously.

For example:

### Identified

> **Siva:**
> I think we should have more technical knowledge-sharing sessions.

### Anonymous

> **Anonymous:**
> I think our current meeting culture is too heavy.

Internally, the system still knows the actual user who created the content.

This is important for:

* Abuse prevention.
* Moderation.
* Auditing.
* Security.
* Potential future administrative workflows.

Therefore:

```text
created_by_user_id = 123
is_anonymous = true
```

should be preferable to creating a fake "Anonymous User" account.

### Privacy rule

Regular employees must **never** be able to discover the identity behind an anonymous post through the application API or UI.

The backend must enforce this rather than merely hiding the name in the frontend.

---

# 8. Voting

Users can vote on:

* Messages.
* Replies.

Supported votes:

```text
UPVOTE
DOWNVOTE
```

A user can have at most **one active vote** per item.

For example:

```text
Message #123

User A → UPVOTE
User B → UPVOTE
User C → DOWNVOTE
```

The user can:

1. Upvote.
2. Downvote.
3. Remove their vote.
4. Change an upvote to a downvote.
5. Change a downvote to an upvote.

### Recommended data model

Instead of storing separate `upvotes` and `downvotes` records:

```text
vote
----
id
organization_id
user_id
message_id
reply_id
vote_type
created_at
updated_at
```

with a uniqueness constraint ensuring one vote per user/item.

---

# 9. Popularity

Messages can be sorted by popularity.

For MVP:

```text
Popularity = number of upvotes
```

The product requirement explicitly states popularity based on upvotes, so downvotes do not need to affect the ranking initially.

However, the system should store downvotes because they are useful for future ranking algorithms.

Future possibilities:

```text
score = upvotes - downvotes
```

or:

```text
score = weighted_votes + recency
```

---

# 10. Message Feed

The primary user experience is the feedback feed.

Users should be able to switch between:

### Recent

Newest messages first.

```text
ORDER BY created_at DESC
```

### Popular

Messages with the highest number of upvotes first.

```text
ORDER BY upvote_count DESC
```

A secondary sort by creation date should be used when scores are equal.

For example:

```text
ORDER BY upvote_count DESC,
         created_at DESC
```

---

# 11. Functional Requirements

## FR-01 — Authentication

Users must authenticate before accessing their organization's feedback system.

The system must:

* Authenticate users.
* Identify their organization.
* Identify their role.
* Establish an authenticated session/token.
* Reject inactive users.

MVP authentication can support:

* Email + password.

Future:

* Google OAuth.
* Microsoft Entra ID.
* GitHub.
* SAML/SSO.
* OIDC.

---

# 12. Organization Management

## FR-02 — Create Organization

SuperAdmin can create an organization.

Required fields:

* Organization name.
* Organization identifier/slug.
* Status.

Example:

```text
Acme Corporation
acme
ACTIVE
```

The slug should be unique.

---

## FR-03 — Organization Status

Organizations can have:

```text
ACTIVE
INACTIVE
```

Inactive organizations cannot be accessed by normal users.

SuperAdmins can reactivate them.

---

## FR-04 — View Organizations

SuperAdmin can view:

* Organization name.
* Identifier.
* Status.
* User count.
* Message count.
* Created date.

---

# 13. User Management

## FR-05 — Create User

SuperAdmin and Admin can create users.

Required information:

* Name.
* Email.
* Role.
* Organization.

Example:

```text
Name: John Doe
Email: john@acme.com
Role: USER
Organization: Acme
```

### Important constraint

An Admin can only create users in their own organization.

---

## FR-06 — Assign Roles

Supported roles:

```text
ADMIN
USER
```

SuperAdmin can assign either role.

Admin can assign roles according to the organization's user-management policy.

For MVP, I recommend allowing an Admin to create both `ADMIN` and `USER` accounts, as specified in your requirement.

---

## FR-07 — Deactivate User

An Admin or SuperAdmin can deactivate a user.

A deactivated user:

* Cannot log in.
* Cannot create messages.
* Cannot reply.
* Cannot vote.

Existing content remains available.

The content should not automatically disappear when a user is deactivated.

---

## FR-08 — User List

Admin can view users belonging to their organization.

Columns:

```text
Name
Email
Role
Status
Created Date
```

Support basic filtering:

* Active/inactive.
* Role.

---

# 14. Message Management

## FR-09 — Create Message

Authenticated users can create a message.

Required:

```text
Content
Posting identity
```

Posting identity:

```text
POST_AS_SELF
POST_ANONYMOUSLY
```

Example UI:

```text
What's on your mind?

[ Write your feedback here... ]

Post as:
○ Siva Prasad
○ Anonymous

             [Post]
```

---

## FR-10 — View Message

A message displays:

```text
Author / Anonymous
Message content
Created time
Upvotes
Downvotes
Reply count
Current user's vote
```

Example:

```text
Anonymous
2 hours ago

We should have more technical learning sessions.

▲ 42    ▼ 3    💬 12
```

---

## FR-11 — Edit Message

Recommended MVP rule:

A user can edit their own message.

However, editing should **not allow changing the original author identity**.

For example:

```text
Created as Anonymous
```

should remain:

```text
Anonymous
```

even after editing.

An audit trail can be added later.

---

## FR-12 — Delete Message

A user can delete their own message.

Recommended approach:

**Soft delete.**

Instead of physically removing it:

```text
status = DELETED
```

The UI can display:

> This message has been deleted.

This preserves discussion integrity and makes moderation/auditing easier.

---

# 15. Reply Management

## FR-13 — Create Reply

Authenticated users can reply to a message.

A reply can also be:

```text
As themselves
Anonymous
```

---

## FR-14 — View Replies

Replies appear underneath the message.

Example:

```text
Message

"We should improve our deployment process."

  ├─ Siva
  │  I agree. Deployments are taking too long.
  │
  ├─ Anonymous
  │  The approval process is the biggest problem.
  │
  └─ John
     We should automate more of the testing.
```

---

## FR-15 — Edit Reply

A user can edit their own reply.

Anonymous identity remains anonymous.

---

## FR-16 — Delete Reply

Users can delete their own replies.

Soft deletion is recommended.

---

# 16. Voting Requirements

## FR-17 — Vote on Message

Users can:

* Upvote.
* Downvote.
* Remove vote.
* Change vote.

The UI should immediately reflect the new vote count.

---

## FR-18 — Vote on Reply

Same behavior as messages.

---

## FR-19 — Prevent Self-Voting?

For MVP, I recommend **allowing users to vote on their own content only if the product has a strong reason to do so; otherwise, prohibit self-voting**.

My recommendation:

> **Do not allow users to vote on their own messages or replies.**

This avoids trivial manipulation of popularity.

---

# 17. Feed Requirements

## FR-20 — Recent Feed

Default feed:

```text
Recent
```

Sorted by:

```text
created_at DESC
```

---

## FR-21 — Popular Feed

Users can select:

```text
Popular
```

Sorted by:

```text
upvote_count DESC
created_at DESC
```

---

## FR-22 — Pagination

The feed must support pagination.

Recommended:

* Cursor-based pagination for production.
* Page/offset pagination is acceptable for the MVP.

Example:

```text
GET /api/messages?sort=recent&cursor=...
```

---

# 18. Search

### MVP

Search is optional and can be deferred.

### Future

Users should be able to search messages by:

* Text.
* Author.
* Date.
* Popularity.
* Topic/category.

For PostgreSQL, full-text search can eventually be added without introducing a separate search engine.

---

# 19. Categories / Topics

I would **not make categories mandatory for MVP**.

The initial product can simply allow free-form messages.

Future:

```text
Category
--------
Work Culture
Engineering
Management
Benefits
Office
HR
Ideas
Other
```

This will make analytics and filtering more useful.

---

# 20. Moderation

This is an important area to consider even if it isn't fully implemented in MVP.

Because anonymous posting is supported, the system needs a basic abuse strategy.

### MVP

Admins should be able to:

* Delete inappropriate messages.
* Delete inappropriate replies.
* View reported content.

### Future

Add:

```text
Report Message
Report Reply
```

with reasons such as:

* Harassment.
* Spam.
* Offensive content.
* Confidential information.
* Other.

---

# 21. Notifications

### MVP

No notifications required.

### Future

Users can receive notifications when:

* Someone replies to their message.
* Someone replies to their discussion.
* Someone mentions them.
* Their message receives significant engagement.
* An Admin responds.

Possible channels:

* In-app.
* Email.
* Slack.
* Microsoft Teams.

---

# 22. Administration Dashboard

## SuperAdmin Dashboard

Example:

```text
Organizations       27
Active Organizations 24
Users              1,284
Messages           18,932
```

Recent organizations:

| Organization | Users | Messages | Status   |
| ------------ | ----: | -------: | -------- |
| Acme         |   125 |    1,240 | Active   |
| Globex       |    83 |      742 | Active   |
| Example Corp |    42 |      193 | Inactive |

---

## Organization Admin Dashboard

Example:

```text
Users                 125
Active Users          119
Messages              1,240
Messages This Month    87
```

Potential future metrics:

* Most popular feedback.
* Most active discussions.
* Number of anonymous posts.
* Engagement rate.
* Categories.
* Trends.

---

# 23. Main User Journeys

## Journey 1 — Employee Creates Feedback

```text
Login
  ↓
Open Feedback Feed
  ↓
Click "Create Feedback"
  ↓
Enter message
  ↓
Choose "Post as myself"
  ↓
Submit
  ↓
Message appears in Recent feed
```

---

## Journey 2 — Anonymous Feedback

```text
Login
  ↓
Create Feedback
  ↓
Enter message
  ↓
Choose "Anonymous"
  ↓
Submit
  ↓
Other users see:
"Anonymous"
```

Internally:

```text
message.created_by = authenticated user
message.is_anonymous = true
```

---

## Journey 3 — Reply Anonymously

```text
Open Message
  ↓
Write Reply
  ↓
Select "Anonymous"
  ↓
Submit
  ↓
Reply appears as Anonymous
```

---

## Journey 4 — Vote

```text
View Message
  ↓
Click Upvote
  ↓
Vote recorded
  ↓
Upvote count increases
```

Clicking the opposite vote changes the existing vote.

---

## Journey 5 — Admin Creates User

```text
Admin Login
  ↓
Administration
  ↓
Users
  ↓
Create User
  ↓
Enter name/email/role
  ↓
Create
  ↓
User receives account/invitation
```

---

## Journey 6 — SuperAdmin Creates Tenant

```text
SuperAdmin Login
  ↓
Organizations
  ↓
Create Organization
  ↓
Enter organization information
  ↓
Create Organization
  ↓
Create initial Admin
  ↓
Admin can manage organization
```

---

# 24. Suggested UI Structure

## Employee Application

```text
┌──────────────────────────────────────────────┐
│ FeedbackHub                     Siva ▼       │
├──────────────────────────────────────────────┤
│                                              │
│  What's on your mind?          [+ Feedback] │
│                                              │
│  [ Recent ] [ Popular ]                     │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │ Anonymous                              │  │
│  │ 2 hours ago                            │  │
│  │                                        │  │
│  │ We should improve our deployment       │  │
│  │ process.                               │  │
│  │                                        │  │
│  │ ▲ 42   ▼ 3   💬 12                     │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │ John Doe                               │  │
│  │ Yesterday                              │  │
│  │                                        │  │
│  │ Can we introduce learning Fridays?     │  │
│  │                                        │  │
│  │ ▲ 31   ▼ 1   💬 8                      │  │
│  └────────────────────────────────────────┘  │
│                                              │
└──────────────────────────────────────────────┘
```

---

# 25. Proposed Domain Model

A reasonable MVP domain model:

```text
Organization
    │
    ├── User
    │
    └── Message
          │
          ├── Reply
          │
          ├── MessageVote
          │
          └── ReplyVote
```

Potential entities:

```text
Organization
User
Message
Reply
MessageVote
ReplyVote
```

Potential enums:

```text
OrganizationStatus
    ACTIVE
    INACTIVE

UserRole
    ADMIN
    USER

UserStatus
    ACTIVE
    INACTIVE

VoteType
    UPVOTE
    DOWNVOTE

ContentStatus
    ACTIVE
    DELETED
```

---

# 26. Suggested Database Model

### organization

```text
organization
------------
id
name
slug
status
created_at
updated_at
```

### user

```text
user
----
id
organization_id
name
email
password_hash
role
status
created_at
updated_at
```

### message

```text
message
-------
id
organization_id
created_by
content
is_anonymous
status
created_at
updated_at
```

### reply

```text
reply
-----
id
organization_id
message_id
created_by
content
is_anonymous
status
created_at
updated_at
```

### message_vote

```text
message_vote
------------
id
organization_id
message_id
user_id
vote_type
created_at
updated_at
```

Unique constraint:

```text
(user_id, message_id)
```

### reply_vote

```text
reply_vote
----------
id
organization_id
reply_id
user_id
vote_type
created_at
updated_at
```

Unique constraint:

```text
(user_id, reply_id)
```

---

# 27. Important Database Constraints

The database should enforce important invariants wherever practical.

### User email

```text
UNIQUE(organization_id, email)
```

This allows the same email to theoretically belong to different tenants while preventing duplicates within an organization.

### Message votes

```text
UNIQUE(user_id, message_id)
```

### Reply votes

```text
UNIQUE(user_id, reply_id)
```

### Organization slug

```text
UNIQUE(slug)
```

---

# 28. API Design

A REST API could look like:

## Authentication

```http
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

## Messages

```http
GET    /api/messages
POST   /api/messages
GET    /api/messages/{id}
PUT    /api/messages/{id}
DELETE /api/messages/{id}
```

Query:

```http
GET /api/messages?sort=recent
GET /api/messages?sort=popular
```

## Replies

```http
GET    /api/messages/{id}/replies
POST   /api/messages/{id}/replies
PUT    /api/replies/{id}
DELETE /api/replies/{id}
```

## Votes

```http
POST   /api/messages/{id}/vote
DELETE /api/messages/{id}/vote

POST   /api/replies/{id}/vote
DELETE /api/replies/{id}/vote
```

Request:

```json
{
  "type": "UPVOTE"
}
```

## Organization administration

```http
GET  /api/admin/users
POST /api/admin/users

PUT  /api/admin/users/{id}
POST /api/admin/users/{id}/activate
POST /api/admin/users/{id}/deactivate
```

## SuperAdmin

```http
GET  /api/superadmin/organizations
POST /api/superadmin/organizations

GET  /api/superadmin/organizations/{id}
PUT  /api/superadmin/organizations/{id}

POST /api/superadmin/organizations/{id}/users
```

---

# 29. Security Requirements

Multi-tenancy makes security particularly important.

### SR-01 — Tenant Isolation

Every request involving tenant data must establish:

```text
currentUser
currentOrganization
currentRole
```

The backend must use the authenticated user's organization rather than accepting arbitrary organization IDs from the client.

Bad:

```http
GET /api/messages?organizationId=123
```

with the backend trusting that value.

Better:

```text
organizationId = authentication.principal.organizationId
```

---

### SR-02 — Authorization

Examples:

```text
USER
 ├── Read own organization's messages
 ├── Create message
 ├── Reply
 └── Vote

ADMIN
 ├── Everything USER can do
 └── Manage organization users

SUPERADMIN
 └── Platform-wide administration
```

---

### SR-03 — Anonymous Privacy

The API must not expose:

```text
created_by
created_by_email
```

for anonymous content to ordinary users.

The frontend should not receive the information and merely hide it.

---

### SR-04 — Authorization on Object Access

Every message/reply operation must validate tenant ownership.

For example:

```text
User A
Organization A

PUT /messages/999
```

If message `999` belongs to Organization B:

```text
403 Forbidden
```

or preferably a response that does not disclose whether the resource exists, depending on the security model.

---

# 30. Auditability

The system should maintain basic audit information.

For content:

```text
created_at
created_by
updated_at
updated_by
```

For administrative operations, a future audit log can record:

```text
actor
organization
action
target
timestamp
metadata
```

Examples:

```text
ADMIN_CREATED_USER
USER_DEACTIVATED
MESSAGE_DELETED
ORGANIZATION_CREATED
```

A full audit-log subsystem can be deferred from MVP.

---

# 31. Non-Functional Requirements

## Performance

The system should support:

* Paginated feeds.
* Efficient popular/recent queries.
* Indexed tenant queries.
* Efficient vote counting.

Recommended indexes:

```text
message(organization_id, created_at)
message(organization_id, status)
message(organization_id, upvote_count)

reply(message_id, created_at)

user(organization_id, email)
```

If vote counts are stored directly on `message`/`reply`, updates must be transactionally consistent with vote changes.

---

## Availability

For a SaaS MVP:

* Automated database backups.
* Health checks.
* Application monitoring.
* Error logging.
* Graceful failure handling.

---

## Security

Must include:

* Password hashing.
* HTTPS.
* Authentication.
* Authorization.
* Tenant isolation.
* Input validation.
* Rate limiting.
* Protection against XSS.
* Protection against CSRF where applicable.
* Secure session/token management.

---

# 32. MVP Scope

I would keep the first release to these capabilities:

### Platform

* [x] SuperAdmin authentication
* [x] Create organization
* [x] Manage organization status
* [x] Create organization users

### Organization

* [x] User authentication
* [x] Admin user management
* [x] User activation/deactivation

### Feedback

* [x] Create message
* [x] Anonymous message
* [x] Edit message
* [x] Delete message
* [x] View message
* [x] Recent feed
* [x] Popular feed

### Discussion

* [x] Create reply
* [x] Anonymous reply
* [x] Edit reply
* [x] Delete reply

### Voting

* [x] Upvote message
* [x] Downvote message
* [x] Upvote reply
* [x] Downvote reply
* [x] Change/remove vote

### Security

* [x] Strict tenant isolation
* [x] RBAC
* [x] Anonymous identity protection

---

# 33. Phase 2

After the MVP proves the core workflow, add:

### Moderation

* Report message.
* Report reply.
* Admin moderation queue.
* Block/suspend users.
* Moderation history.

### Notifications

* Reply notifications.
* Mention notifications.
* Email notifications.
* Notification preferences.

### Discovery

* Search.
* Categories.
* Tags.
* Filters.
* Trending feedback.

### Analytics

* Number of messages.
* Engagement.
* Votes.
* Anonymous vs identified participation.
* Most discussed topics.
* Trending topics.

---

# 34. Phase 3 — SaaS Capabilities

If this becomes a commercial SaaS product, add:

* Subscription plans.
* Billing.
* Usage limits.
* Organization-level settings.
* Custom branding.
* Custom domain.
* SSO/SAML.
* SCIM provisioning.
* Microsoft Entra integration.
* Google Workspace integration.
* Slack/Teams integration.
* Data export.
* Organization data retention policies.
* Compliance controls.

---

# 35. Future AI Capabilities

This application could eventually become considerably more interesting with AI.

For example:

### AI Discussion Summaries

For a highly active discussion:

> **Summary:**
> Employees generally support flexible working hours. The main concerns are customer-support coverage and meeting schedules.

### Topic Clustering

Automatically group feedback:

```text
Engineering
  ├── CI/CD
  ├── Developer Experience
  └── Technical Debt

Workplace
  ├── Office
  ├── Remote Work
  └── Benefits

Culture
  ├── Meetings
  ├── Management
  └── Career Growth
```

### Sentiment Trends

```text
August
Positive   ███████████
Neutral    ███████
Negative   ████

September
Positive   █████████
Neutral    ██████
Negative   ██████
```

### Duplicate Feedback Detection

If somebody posts:

> "Our CI pipeline is too slow."

and another employee posts:

> "Builds are taking forever."

The system could suggest:

> "There is already a discussion about slow CI builds."

### AI-powered Admin Digest

Weekly:

> **This week's feedback**
>
> * CI/CD performance was the most discussed topic.
> * 37 new messages were posted.
> * 12 discussions received more than 10 upvotes.
> * Employees raised concerns about deployment times and meeting overload.

These should be treated as **future enhancements**, not MVP requirements.

---

# 36. Key Product Decisions

I recommend locking down these decisions early:

| Decision           | Recommendation                           |
| ------------------ | ---------------------------------------- |
| Tenant             | Organization                             |
| User belongs to    | Exactly one organization                 |
| Roles              | SUPERADMIN, ADMIN, USER                  |
| Anonymous identity | `is_anonymous` + real `created_by`       |
| Replies            | Single-level                             |
| Voting             | One active vote/user/item                |
| Popularity         | Upvote count                             |
| Deleted content    | Soft delete                              |
| Feed               | Recent + Popular                         |
| Search             | Post-MVP                                 |
| Categories         | Post-MVP                                 |
| Notifications      | Post-MVP                                 |
| Moderation         | Basic deletion MVP, full reporting later |
| Tenant isolation   | Mandatory backend enforcement            |

## 37. Recommended MVP Architecture

Given the size and nature of this application, I would **not over-engineer the architecture**.

A good implementation would be a **modular monolith**:

```text
feedback-management
│
├── organization
│   ├── Organization
│   ├── OrganizationService
│   └── OrganizationRepository
│
├── identity
│   ├── User
│   ├── Role
│   ├── Authentication
│   └── UserService
│
├── feedback
│   ├── Message
│   ├── Reply
│   ├── MessageService
│   └── ReplyService
│
├── voting
│   ├── MessageVote
│   ├── ReplyVote
│   └── VotingService
│
└── administration
    ├── Admin APIs
    └── SuperAdmin APIs
```
