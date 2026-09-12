# SpeakUp PRD

# 1. Product Overview

A feedback management application providing its employees with a private space to:

* Express feedback, opinions, suggestions, concerns, or ideas.
* Participate in discussions through replies.
* Post or reply using their identity or anonymously.
* Upvote/downvote messages and replies.
* Discover popular and recent discussions.
* Allow organization administrators to manage users.

---

# 2. Goals

## 2.1 Primary Goals

1. Provide the organization with a private feedback/discussion space.
2. Allow employees to freely express opinions.
3. Support anonymous participation without exposing the employee's identity to other users.
4. Enable discussions through replies.
5. Allow employees to vote on messages and replies.
6. Make useful/popular feedback discoverable.
7. Provide administrators with basic user management.

## 2.2 Secondary Goals

* Encourage constructive employee discussions.
* Provide the organization with visibility into employee sentiment and recurring topics.
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
* Multi-tenancy or hosting multiple organizations in one deployment.

---

# 4. Actors and Roles

There are two primary roles.

| Actor | Scope      | Responsibilities                  |
| ----- | ---------- | --------------------------------- |
| Admin | Deployment | Manage users and moderate content |
| User  | Deployment | Create posts, replies and votes   |

### 4.1 Admin

An Admin manages the application deployment for the organization.

Can:

* View users.
* Create users.
* Assign `ADMIN` or `USER` roles.
* Activate/deactivate users.
* View and moderate feedback.
* Participate in discussions like any other employee.

---

### 4.2 User

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

# 5. Core Concepts

## 5.1 Message

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

## 5.2 Reply

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

# 6. Anonymous Participation

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

# 7. Voting

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
user_id
message_id
reply_id
vote_type
created_at
updated_at
```

with a uniqueness constraint ensuring one vote per user/item.

---

# 8. Popularity

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

# 9. Message Feed

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

# 10. Functional Requirements

## FR-01 — Authentication

Users must authenticate before accessing the feedback system.

The system must:

* Authenticate users.
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

# 11. User Management

## FR-02 — Create User

Admin can create users.

Required information:

* Name.
* Email.
* Role.

Example:

```text
Name: John Doe
Email: john@acme.com
Role: USER
```

---

## FR-03 — Assign Roles

Supported roles:

```text
ADMIN
USER
```

Admin can assign roles according to the organization's user-management policy.

For MVP, I recommend allowing an Admin to create both `ADMIN` and `USER` accounts, as specified in your requirement.

---

## FR-04 — Deactivate User

An Admin can deactivate a user.

A deactivated user:

* Cannot log in.
* Cannot create messages.
* Cannot reply.
* Cannot vote.

Existing content remains available.

The content should not automatically disappear when a user is deactivated.

---

## FR-05 — User List

Admin can view all users in the deployment.

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

# 12. Message Management

## FR-06 — Create Message

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

## FR-07 — View Message

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

## FR-08 — Edit Message

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

## FR-09 — Delete Message

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

# 13. Reply Management

## FR-10 — Create Reply

Authenticated users can reply to a message.

A reply can also be:

```text
As themselves
Anonymous
```

---

## FR-11 — View Replies

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

## FR-12 — Edit Reply

A user can edit their own reply.

Anonymous identity remains anonymous.

---

## FR-13 — Delete Reply

Users can delete their own replies.

Soft deletion is recommended.

---

# 14. Voting Requirements

## FR-14 — Vote on Message

Users can:

* Upvote.
* Downvote.
* Remove vote.
* Change vote.

The UI should immediately reflect the new vote count.

---

## FR-15 — Vote on Reply

Same behavior as messages.

---

## FR-16 — Prevent Self-Voting?

For MVP, I recommend **allowing users to vote on their own content only if the product has a strong reason to do so; otherwise, prohibit self-voting**.

My recommendation:

> **Do not allow users to vote on their own messages or replies.**

This avoids trivial manipulation of popularity.

---

# 15. Feed Requirements

## FR-17 — Recent Feed

Default feed:

```text
Recent
```

Sorted by:

```text
created_at DESC
```

---

## FR-18 — Popular Feed

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

## FR-19 — Pagination

The feed must support pagination.

Recommended:

* Cursor-based pagination for production.
* Page/offset pagination is acceptable for the MVP.

Example:

```text
GET /api/messages?sort=recent&cursor=...
```

---

# 16. Search

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

# 17. Categories / Topics

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

# 18. Moderation

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

# 19. Notifications

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

# 20. Administration Dashboard

## Admin Dashboard

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

# 21. Main User Journeys

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

# 22. Suggested UI Structure

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

# 23. Proposed Domain Model

A reasonable MVP domain model:

```text
User
Message
  ├── Reply
  ├── MessageVote
  └── ReplyVote
```

Potential entities:

```text
User
Message
Reply
MessageVote
ReplyVote
```

Potential enums:

```text
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


# 24. Future AI Capabilities

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
