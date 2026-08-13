# Use Case Documentation - feature/user Branch (BF-09 & BF-10 Focus)

> **Document Version**: 5.0 (PRD Standard Template - Fully Compliant)
> **Scope**: BF-09 (Customer Support), BF-10 (Platform Admin), plus core identity UCs (UC-01, 03, 04, 36).

## Table of Contents
### Core Identity & Messaging (Retained)
- [UC-01: Login / Logout](#uc-01-login--logout)
- [UC-03: Reset Password](#uc-03-reset-password)
- [UC-04: Change Password](#uc-04-change-password)
- [UC-36: Chat with Users](#uc-36-chat-with-users)

### Customer Support (BF-09)
- [UC-65: Get Support](#uc-65-get-support)
- [UC-66: Manage Support Requests](#uc-66-manage-support-requests)
- [UC-67: Manage FAQ Knowledge Base](#uc-67-manage-faq-knowledge-base)

### Platform Administration (BF-10)
- [UC-52: Report User or Class](#uc-52-report-user-or-class)
- [UC-07: Manage User Accounts](#uc-07-manage-user-accounts)
- [UC-41: Monitor Financial Report](#uc-41-monitor-financial-report)
- [UC-43: Export Financial Statements](#uc-43-export-financial-statements)
- [UC-46: Configure Platform Fees](#uc-46-configure-platform-fees)
- [UC-56: View Dashboard](#uc-56-view-dashboard)
- [UC-57: Manage System Categories](#uc-57-manage-system-categories)
- [UC-60: Enforce Platform Penalties](#uc-60-enforce-platform-penalties)
- [UC-61: Monitor Audit Logs](#uc-61-monitor-audit-logs)
- [UC-35: Configure Notification Templates](#uc-35-configure-notification-templates)
- [UC-58: Manage Escrow Transactions](#uc-58-manage-escrow-transactions)
- [UC-59: Detect Platform Circumvention](#uc-59-detect-platform-circumvention)

---

## UC-01: Login / Logout
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Guest, Authenticated User |
| **Secondary Actor(s)** | Authentication Service |
| **Module** | Authentication & RBAC |
| **FT-ID Ref** | FT-02 |
| **BF-ID Ref** | BF-01, BF-10 |
| **Depends On** | — |
| **Preconditions** | The visitor has an existing ACTIVE account (for Login). User is signed in (for Logout). |
| **Postconditions** | User receives a JWT access token and session is established (Login). Session is destroyed on client side (Logout). |

### Main Flow
1. **[U]** Opens the Login page.
2. **[U]** Enters email and password, and clicks "Login".
3. **[S]** Validates the credentials against the database hash.
4. **[S]** Checks if the account status is ACTIVE (see BR-UC01-01).
5. **[S]** Generates a JWT access token containing the user's role and permissions.
6. **[S]** Returns the token and redirects the user to the appropriate landing page based on role.
7. **[U]** (For Logout) Clicks the "Logout" button in the navigation menu.
8. **[S]** Clears the JWT from client storage and redirects to the public homepage.

### Alternative Flows
- **AF-01 — Invalid Credentials:** At step 3, if the email or password is incorrect, the system shows an inline message: "Invalid email or password."
- **AF-02 — Inactive Account:** At step 4, if the account is SUSPENDED or PENDING, the system blocks login and displays: "Your account is currently suspended or pending verification."

### Business Rules
- **BR-UC01-01**: Only accounts with an `ACTIVE` status can authenticate.
- **BR-UC01-02**: Passwords are securely hashed using BCrypt and never stored in plain text.
- **BR-UC01-03**: Tokens expire after a predefined duration (e.g., 24 hours).

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Email | Text | Required. Valid email format. |
| Password | Text | Required. |

**On success:** The person sees the authenticated dashboard or homepage.

### Allowed Roles
- Public (Login), All Authenticated Roles (Logout)

### Verification Criteria
- **Given** the login page and valid credentials.
- **When** the visitor submits the form.
- **Then** the user receives a token AND is redirected to the dashboard.

---

## UC-03: Reset Password
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Guest |
| **Secondary Actor(s)** | Email/Notification Service |
| **Module** | Authentication & RBAC |
| **FT-ID Ref** | FT-03 |
| **BF-ID Ref** | BF-01 |
| **Depends On** | — |
| **Preconditions** | The visitor does not remember their password and has an ACTIVE account. |
| **Postconditions** | The user's password is changed in the system. The verified-email reset token is consumed. |

### Main Flow
1. **[U]** Opens the Forgot Password page, enters registered email, and clicks "Send OTP".
2. **[S]** Validates the email and rate limits. Generates a one-time code (OTP), sends it via email, and shows an inline confirmation.
3. **[U]** Enters the received OTP and clicks "Verify OTP".
4. **[S]** Validates the OTP. On success, issues a short-lived reset token and directs to the Reset Password screen.
5. **[U]** Fills in the new password and confirm password fields, and clicks "Reset Password".
6. **[S]** Validates the password match and token. Updates the password in the database (hashed), consumes the token, and records in the audit log.
7. **[S]** Shows a success message and directs to the Login screen.

### Alternative Flows
- **AF-01 — Incorrect OTP:** At step 4, the entered code does not match. An inline message shows: "The verification code is incorrect."
- **AF-02 — Passwords do not match:** At step 6, confirm-password differs from new password. An inline message shows: "Passwords do not match."

### Business Rules
- **BR-UC03-01**: OTP is a 6-digit code, valid for 10 minutes.
- **BR-UC03-02**: Password must be at least 8 characters, letters and numbers. No accented (non-ASCII) characters.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Email | Text | Required. Valid email format. |
| OTP code | Text | Required to verify. 6 digits. |
| New Password | Text | Required. Min 8 chars. No accented characters. |
| Confirm Password | Text | Required. Must match New Password. |

**On success:** The person sees a success message confirming the password was changed.

### Allowed Roles
- Public

### Verification Criteria
- **Given** an ACTIVE account.
- **When** the visitor correctly verifies the OTP and enters a valid new password.
- **Then** the password is changed AND the person can proceed to sign in.

---

## UC-04: Change Password
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Authenticated User |
| **Secondary Actor(s)** | None |
| **Module** | Authentication & RBAC |
| **FT-ID Ref** | FT-03 |
| **BF-ID Ref** | BF-01 |
| **Depends On** | UC-01 |
| **Preconditions** | The user is signed in with an ACTIVE account. |
| **Postconditions** | The user's password is changed in the system. |

### Main Flow
1. **[U]** Opens the Profile settings and navigates to the Change Password section.
2. **[U]** Enters the current password, new password, and confirms new password, then clicks "Update".
3. **[S]** Validates the current password against the stored hash.
4. **[S]** Validates the new password format. Updates the password in the database.
5. **[S]** Records the action in the audit trail and displays a success message.

### Alternative Flows
- **AF-01 — Incorrect Current Password:** At step 3, current password does not match. Shows: "Incorrect current password."

### Business Rules
- **BR-UC04-01**: The provided current password must match the existing hash for the authenticated user.
- **BR-UC04-02**: New password must be at least 8 characters. Must not contain accented characters.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Current Password | Text | Required. Must match existing password. |
| New Password | Text | Required. Min 8 chars. |
| Confirm Password | Text | Required. Must match New Password. |

**On success:** The person sees a success message indicating the password was updated.

### Allowed Roles
- Client, Tutor, Tutor Center, Platform Admin

### Verification Criteria
- **Given** an authenticated user on the profile page.
- **When** the user provides the correct current password and valid new passwords.
- **Then** the password is changed AND an audit log is created.

---

## UC-36: Chat with Users
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Authenticated User |
| **Secondary Actor(s)** | Messaging Service (WebSocket) |
| **Module** | Messaging |
| **FT-ID Ref** | FT-18 |
| **BF-ID Ref** | BF-02, BF-04, BF-05 |
| **Depends On** | UC-01 |
| **Preconditions** | The user has an active session. |
| **Postconditions** | A message is stored and delivered to the recipient(s) in real-time. For group creation, a new conversation with multiple participants is created. |

### Main Flow
1. **[U]** Opens the Messaging page from the navigation menu.
2. **[S]** Loads existing conversations (direct and group) and establishes a WebSocket connection.
3. **[U]** Selects a conversation.
4. **[S]** Loads the paginated message history.
5. **[U]** Types a message and clicks Send.
6. **[S]** Stores the message in the database and broadcasts it via WebSocket (`/topic/conversation/{id}`).
7. **[S]** Updates the conversation's `lastMessageAt` and unread counters.

### Alternative Flows
- **AF-01 — WebSocket Disconnected:** System falls back to REST API for sending.
- **AF-02 — Create Group Chat:** User opens the new-chat modal, switches to group mode, enters a group name (3-80 chars) and selects 2-19 other members, then clicks "Create". System creates a `GROUP` conversation, adds the creator as owner, and notifies all added members.
- **AF-03 — Manage Group:** The group owner opens Group Info and can rename the group, add members (up to a total of 20 participants), remove a member, or transfer ownership to another member. The owner must transfer ownership before leaving the group; non-owner members can leave freely.

### Business Rules
- **BR-UC36-01**: Messages are text-only, maximum 5000 characters.
- **BR-UC36-02**: Read receipts (`lastReadAt`) are tracked per user per conversation.
- **BR-UC36-03**: A `GROUP` conversation has exactly one owner (`Conversation.ownerUserId`) at any time; there is no multi-admin/co-owner role. Only the owner can rename the group, add/remove members, or transfer ownership.
- **BR-UC36-04**: Group size is capped at 20 participants (owner + up to 19 members). Creation requires the owner plus at least 2 other members.
- **BR-UC36-05**: The owner cannot leave the group without first transferring ownership to another member.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Content | Text | Required. Max 5000 characters. |
| Group Name | Text | Required for group creation. 3-80 characters. |
| Member IDs | List | Required for group creation. 2-19 other users. |

**On success:** The person sees their sent message appear immediately in the chat thread, or the new group appears in the conversation list.

### Allowed Roles
- Client, Tutor, Tutor Center, Platform Admin

### Verification Criteria
- **Given** two authenticated users.
- **When** User A sends a valid message to User B.
- **Then** the message is saved AND User B receives it in real-time via WebSocket.
- **Given** an authenticated user creating a group.
- **When** they enter a valid name and select 2 or more members.
- **Then** a `GROUP` conversation is created AND all selected members receive a notification AND the creator is set as owner.

---

## UC-65: Get Support
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Guest, Authenticated User |
| **Secondary Actor(s)** | Generative AI Service (Gemini), Notification Service |
| **Module** | Customer Support |
| **FT-ID Ref** | FT-51 |
| **BF-ID Ref** | BF-09 |
| **Depends On** | — |
| **Preconditions** | FAQ entries exist in the database (for Browse FAQ). Gemini API is available (for Chatbot). |
| **Postconditions** | User receives information (FAQ/Chatbot) OR a support ticket is created in OPEN status. |

### Main Flow
1. **[U]** Navigates to the Help Center page.
2. **[S]** Retrieves all published FAQ entries.
3. **[U]** Types a keyword in the search bar.
4. **[S]** Normalizes Vietnamese text, calculates relevance scores, and displays matching results.
5. **[U]** Clicks "Chat with AI Assistant" and asks a complex question.
6. **[S]** Queries the FAQ database for relevant context (RAG) and calls the Gemini API to generate an answer with FAQ citations.
7. **[U]** If the FAQ/AI does not solve the issue, clicks "Submit a Support Request" (Requires authentication).
8. **[U]** Fills in the ticket form (Category, Subject, Description) and clicks Submit.
9. **[S]** Validates the input. Generates a ticket ID and assigns Priority automatically based on Category.
10. **[S]** Creates the ticket in `support_tickets` table with status `OPEN`.
11. **[S]** Shows a success message and redirects to My Tickets view.

### Alternative Flows
- **AF-01 — AI Service Error:** At step 6, if the AI service fails, the system returns a fallback message: "Chatbot temporarily unavailable."
- **AF-02 — Reopen Resolved Ticket:** User opens a `RESOLVED` ticket and clicks "Reopen". System changes status to `OPEN` and notifies admins.

### Business Rules
- **BR-UC65-01**: Only FAQs marked as `is_published = true` are visible.
- **BR-UC65-02**: AI uses Retrieval-Augmented Generation (RAG) prioritizing internal FAQ data.
- **BR-UC65-03**: Ticket Priority is auto-escalated: DISPUTE/SYSTEM_ERROR -> HIGH/URGENT; BUG_REPORT -> MEDIUM; INQUIRY -> LOW.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Category | Text | Required. One of: DISPUTE, SYSTEM_ERROR, REPORT_USER, BUG_REPORT, INQUIRY. |
| Subject | Text | Required. Max 150 chars. |
| Description | Text | Required. Max 5000 chars. |

**On success:** The person sees the AI answer OR a success message confirming the ticket submission.

### Allowed Roles
- Public (FAQ, Chatbot). Client, Tutor, Tutor Center (Submit Ticket).

### Verification Criteria
- **Given** an authenticated user needing help.
- **When** they submit a ticket with valid fields.
- **Then** a ticket is created in OPEN status AND Priority is auto-assigned AND it appears in the Admin's queue.

---

## UC-66: Manage Support Requests
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | Notification Service |
| **Module** | Customer Support |
| **FT-ID Ref** | FT-23 |
| **BF-ID Ref** | BF-09 |
| **Depends On** | UC-65 |
| **Preconditions** | The admin is authenticated with PLATFORM_ADMIN role. |
| **Postconditions** | Ticket status is updated. Admin reply is recorded. User is notified. |

### Main Flow
1. **[U]** Navigates to the Support Tickets Management page.
2. **[S]** Loads paginated ticket list, sorted by priority and creation date.
3. **[U]** Clicks on a ticket to view details.
4. **[U]** Types a response and clicks "Send Reply".
5. **[S]** Saves the message with `sender=ADMIN`, changes status to `IN_PROGRESS`, and notifies the user.
6. **[U]** Resolves the issue and clicks "Mark as Resolved".
7. **[S]** Changes status to `RESOLVED`.
8. **[U]** Clicks "Close Ticket".
9. **[S]** Changes status to `CLOSED`, sets `closedAt`, and sends a final notification to the user.

### Alternative Flows
- **AF-01 — Update Priority/Category:** Admin clicks Edit, changes priority (e.g., LOW to URGENT), and saves. System updates metadata and audit log.

### Business Rules
- **BR-UC66-01**: Admin responses trigger real-time notifications to the ticket creator.
- **BR-UC66-02**: Final closure (`CLOSED` state) permanently locks the ticket.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Content | Text | Required for replying. Max 5000 chars. |
| Status | Text | Required for updating status. |

**On success:** The admin sees the updated ticket thread and status badges reflect the changes immediately.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin viewing an OPEN ticket.
- **When** the admin sends a reply and clicks Close Ticket.
- **Then** the message is saved AND the status becomes CLOSED AND the ticket creator receives a permanent closure notification.

---

## UC-67: Manage FAQ Knowledge Base
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Customer Support |
| **FT-ID Ref** | FT-23 |
| **BF-ID Ref** | BF-09 |
| **Depends On** | — |
| **Preconditions** | The admin is authenticated with PLATFORM_ADMIN role. |
| **Postconditions** | FAQ records are created, updated, or deleted. |

### Main Flow
1. **[U]** Navigates to the FAQ Management page.
2. **[U]** Clicks "Create FAQ".
3. **[U]** Enters Question, Answer, Category, and Sort Order. Selects "Published" status.
4. **[U]** Clicks Save.
5. **[S]** Validates the fields. Inserts record into `faq_entries` table.
6. **[S]** Displays success message and refreshes list.

### Alternative Flows
- **AF-01 — Edit FAQ:** User clicks Edit on an existing FAQ, modifies content or toggles visibility, and saves.
- **AF-02 — Delete FAQ:** User clicks Delete, confirms in modal. System removes the record.

### Business Rules
- **BR-UC67-01**: Only FAQs marked as Published appear in the Help Center for end users.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Question | Text | Required. Max 255 chars. |
| Answer | Text | Required. Max 5000 chars. |
| Published | Yes/No | Required boolean. |

**On success:** The admin sees the newly created or updated FAQ in the management list.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin on the FAQ management page.
- **When** the admin creates a new FAQ and sets it to Published.
- **Then** the FAQ is saved to the database AND is immediately visible to public guests.

---

## UC-52: Report User or Class
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Authenticated User |
| **Secondary Actor(s)** | Notification Service |
| **Module** | Trust & Safety |
| **FT-ID Ref** | FT-42 |
| **BF-ID Ref** | BF-08, BF-10 |
| **Depends On** | UC-01 |
| **Preconditions** | The user has an active session. |
| **Postconditions** | A report is created. Admin can review and resolve it. |

### Main Flow
1. **[U]** Opens an existing direct (1-1) chat conversation with the user to be reported, and clicks the "Report User" (!) icon in the chat header.
2. **[U]** Selects a report category (FRAUD, ABUSE, SPAM, INAPPROPRIATE, OTHER), enters a description (min. 10 characters), and clicks Submit.
3. **[S]** Validates the payload and creates a `Report` record via `POST /api/messaging/reports`.
4. **[S]** Displays a success message confirming the report was sent to Platform Admins.
5. **[U]** (Class Flow) Navigates to a contract or active class detail page and clicks "Report Class Issue". Fills in issue type, description, and optional evidence, then submits via `POST /api/class-issues`.
6. **[U]** (Admin Flow) Logs in and navigates to Reports view (`GET /api/platform/reports`).
7. **[U]** (Admin Flow) Reviews the report and clicks Resolve.
8. **[S]** (Admin Flow) Validates and sends `PATCH /api/platform/reports/{reportId}` (USER/REVIEW) or `PATCH /api/platform/reports/{reportId}/resolve` (CLASS) to mark it as resolved.

### Alternative Flows
- **AF-01 — Invalid Payload:** At step 3, if description is under 10 characters or required fields are empty, validation fails and prompts the user.
- **AF-02 — Report a Review:** From "My Reputation" (Tutor) or "My Reviews" (Client), the user can report a specific review via the same underlying report endpoint with `targetType = REVIEW`.
- **AF-03 — Rate Limit / Duplicate Block:** If the user has submitted 5+ reports in the last 24 hours, or already has a `PENDING` report against the same target, the submission is rejected with an inline error.
- **AF-04 — Tutor Center Review:** For class reports belonging to their own classes, a Tutor Center can view and resolve them via a dedicated Reports page (`GET/PATCH /api/center/reports/**`).

### Business Rules
- **BR-UC52-01**: Admins review and manually apply penalties if the report is valid.
- **BR-UC52-02**: A user cannot report themselves, and cannot submit more than 5 reports within a rolling 24-hour window.
- **BR-UC52-03**: A new report against the same target is blocked while a prior report from the same reporter for that target is still `PENDING`.
- **BR-UC52-04**: `CLASS` target reports must be resolved through the class-issue resolution flow (`resolveClassIssue`), not the generic report resolution endpoint.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Target Type | Text | Required. USER, CLASS, or REVIEW. |
| Target ID | Number | Required. Valid ID of the reported entity. |
| Category | Text | Required. One of: FRAUD, ABUSE, SPAM, INAPPROPRIATE, OTHER. |
| Description | Text | Required. Min 10 characters. |

**On success:** The person sees a success message confirming the report was lodged.

### Allowed Roles
- Client, Tutor, Tutor Center (Submitter). Platform Admin (Reviewer, all targets). Tutor Center (Reviewer, class reports for their own classes only).

### Verification Criteria
- **Given** an authenticated user viewing an open direct chat.
- **When** the user submits a "Report User" with a valid category and description (≥10 chars).
- **Then** a report record is created in the DB AND is visible to Platform Admins for resolution.

### Known Gap
- There is currently no standalone "Report User" screen with its own user search/target picker; reporting a user is only reachable from within an already-open direct chat conversation (`activeConv.otherParticipant`).

---

## UC-07: Manage User Accounts
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-36 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | User account status is updated. |

### Main Flow
1. **[U]** Navigates to User Management.
2. **[S]** Fetches paginated list of users from `GET /users`.
3. **[U]** Searches for a user by email and clicks "Suspend Account".
4. **[S]** Sends `PATCH /users/{id}/status` to the backend.
5. **[S]** System updates user status to `SUSPENDED` and writes to the audit log.
6. **[S]** Displays a success message.

### Alternative Flows
- **AF-01 — Activate User:** Admin changes a suspended user back to `ACTIVE`.

### Business Rules
- **BR-UC07-01**: Admins cannot suspend themselves or other super admins.
- **BR-UC07-02**: Suspended users cannot log in (enforced in UC-01).

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Status | Text | Required. Must be ACTIVE or SUSPENDED. |

**On success:** The admin sees the updated user status badge in the list.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin viewing the user list.
- **When** they suspend a user.
- **Then** the status is updated in the DB AND that user can no longer log in.

---

## UC-41: Monitor Financial Report
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-39 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | Read-only operation. |

### Main Flow
1. **[U]** Navigates to the Analytics page.
2. **[S]** Calls `GET /api/platform/analytics/summary?from=&to=` (`PlatformAnalyticsController` → `PlatformAnalyticsServiceImpl`).
3. **[S]** Displays user/tutor/class counts, contract completion rate, verification conversion rate, dispute rate, total platform revenue, platform fee revenue, and a breakdown of deposits, withdrawals, escrow held, escrow released, and escrow refunded.
4. **[U]** Uses the date range filter to re-aggregate the same metrics for a specific period.

### Alternative Flows
- **AF-01 — No Data:** If no data exists for the selected period, cards display zero values.
- **AF-02 — Invalid Range:** If `from` is after `to`, the API rejects the request with a validation error.

### Business Rules
- **BR-UC41-01**: Access to financial data is restricted to PLATFORM_ADMIN. There is currently no Tutor Center-scoped financial report endpoint.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Date Range (`from`, `to`) | Date | Optional. When provided, `from` must not be after `to`. |

**On success:** The admin sees updated aggregate financial metrics (including deposit/withdrawal/escrow breakdown) based on the applied filters.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin on the financial report page.
- **When** they apply a date filter.
- **Then** the dashboard data re-aggregates accurately for the selected period, including the deposit/withdrawal/escrow breakdown.

### Known Gap
- Tutor Center users cannot view a financial report scoped to their own center; the analytics endpoint is PLATFORM_ADMIN-only.

---

## UC-43: Export Financial Statements
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-39 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | UC-41 |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | CSV file is generated and downloaded. An `EXPORT_ANALYTICS` audit log entry is recorded. |

### Main Flow
1. **[U]** Navigates to the Analytics page.
2. **[U]** Selects an export type (e.g., users, transactions), an optional date range, and clicks "Export CSV".
3. **[S]** Calls `GET /api/platform/analytics/export?type=&from=&to=&format=csv`.
4. **[S]** Generates the CSV report from the database (`analyticsService.exportCsv`).
5. **[S]** Records an `EXPORT_ANALYTICS` entry in the audit log (type, from, to).
6. **[S]** Streams the file download (`tcs-analytics-{type}-{date}.csv`) to the admin's browser.

### Alternative Flows
- **AF-01 — Empty Export:** If no transactions occurred, the exported CSV contains only headers.
- **AF-02 — Unsupported Format:** If a format other than `csv` is requested, the API rejects with a validation error.

### Business Rules
- **BR-UC43-01**: Every export action is recorded in the audit trail via `auditLogService.record("EXPORT_ANALYTICS", ...)`.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Type | Text | Required. Defaults to `users`. Determines which dataset is exported. |
| Format | Text | Optional. Only `csv` is supported. |
| Date Range | Date | Optional. Start and end date for the export period. |

**On success:** A CSV file is successfully generated and downloaded to the admin's machine, and the export is logged.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin on the financial report page.
- **When** they click Export CSV for a given period.
- **Then** a CSV file is generated for that period AND an `EXPORT_ANALYTICS` audit log entry is created.

---

## UC-46: Configure Platform Fees
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-47 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | System parameters are updated. |

### Main Flow
1. **[U]** Navigates to Platform Settings.
2. **[S]** Loads current values for `PLATFORM_FEE_RATE` and `ESCROW_HOLD_DAYS`.
3. **[U]** Updates the Platform Fee Rate input (e.g., to 10%) and clicks Save.
4. **[S]** Validates constraints. Updates `system_parameters` table and logs the change.
5. **[S]** Shows a success message.

### Alternative Flows
- **AF-01 — Invalid Bounds:** If fee rate is negative or greater than 0.50 (50%), the system rejects the update with "PLATFORM_FEE_RATE phải từ 0.00 đến 0.50."
- **AF-02 — Protected Key:** If the admin attempts to rename or delete a mandatory key (`PLATFORM_FEE_RATE` or `ESCROW_HOLD_DAYS`), the system rejects the operation.

### Business Rules
- **BR-UC46-01**: Platform Fee Rate must be a valid decimal between 0.00 and 0.50, validated server-side on every create/update.
- **BR-UC46-02**: Changes apply only to future contracts.
- **BR-UC46-03**: `PLATFORM_FEE_RATE` and `ESCROW_HOLD_DAYS` are mandatory keys; they cannot be deleted, and their key name cannot be changed.
- **BR-UC46-04**: `ESCROW_HOLD_DAYS` must be an integer between 1 and 365.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Parameter Value | Number | Required. Must be within allowed bounds. |

**On success:** The admin sees a success toast and the new parameter value reflects immediately.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin on Platform Settings.
- **When** they set the fee rate to a valid value and save.
- **Then** the `system_parameters` table is updated AND the audit log records the change.

---

## UC-56: View Dashboard
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-40 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | Read-only overview. |

### Main Flow
1. **[U]** Lands on the main Dashboard after login.
2. **[S]** Fetches KPIs via `PlatformService.getDashboard()`, which combines the task queue summary and the analytics summary.
3. **[S]** Renders summary cards (Pending Verifications, Open Tickets, New Users) and revenue figures, plus contextual alert cards (e.g., open disputes, pending withdrawals).
4. **[S]** Frontend polls `GET /api/platform/dashboard` every 60 seconds (`usePlatformDashboard`) to keep the view current.

### Alternative Flows
- **AF-01 — Service Error:** If backend fails, a fallback generic "Service Unavailable" is displayed instead of charts.

### Business Rules
- **BR-UC56-01**: The dashboard automatically refreshes every 60 seconds via client-side polling; there is currently no server-side cache, so each refresh re-aggregates from the database.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| None | N/A | N/A (Read-only operation) |

**On success:** The admin sees the populated dashboard.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin logs into the system.
- **When** they land on the dashboard.
- **Then** the system displays the aggregated KPI summaries and charts, refreshing automatically every 60 seconds.

### Known Gap
- Dashboard aggregation is not cached (no `@Cacheable`/Redis/Caffeine); `PlatformAnalyticsServiceImpl` performs multiple `findAll()` calls (users, classes, transactions, verification requests) and aggregates in application code rather than via DB-level aggregate queries. This may not scale well as data volume grows.

---

## UC-57: Manage System Categories
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-47 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | System category definitions are updated globally. |

### Main Flow
1. **[U]** Navigates to System Categories via `CatalogController`.
2. **[U]** Clicks "Add Category", enters a name (e.g., "Mathematics"), and clicks Save.
3. **[S]** Validates uniqueness. Creates the record in the `categories` table.
4. **[S]** Updates the UI list.

### Alternative Flows
- **AF-01 — Duplicate Name:** If the category already exists, validation fails with "Category name must be unique."

### Business Rules
- **BR-UC57-01**: Category names must be unique system-wide.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Category Name | Text | Required. Unique. Max 100 chars. |

**On success:** The admin sees the new category, which immediately appears in dropdowns across the platform.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin on the categories page.
- **When** they create a category with a unique name.
- **Then** it is saved to the database AND visible system-wide for class creation.

---

## UC-60: Enforce Platform Penalties
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | Notification Service |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-43, FT-44 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | Penalty record is created and user's trust score/status is impacted. |

### Main Flow
1. **[U]** Navigates to the user's profile and clicks "Apply Penalty".
2. **[S]** Shows the penalty modal (handled via `PlatformPenaltyController` → `PenaltyService`).
3. **[U]** Selects a penalty type (`TEMPORARY_BAN`, `PERMANENT_BAN`, `WARNING`, or `FEATURE_RESTRICTION`), inputs a reason (and, for `FEATURE_RESTRICTION`, the restricted feature codes; for `TEMPORARY_BAN`, an expiry date), and saves.
4. **[S]** Records the penalty in the `user_penalties` table.
5. **[S]** For `TEMPORARY_BAN`/`PERMANENT_BAN`, sets the user's account status to `BANNED`. For `FEATURE_RESTRICTION`, no account-level lock is applied; enforcement happens per-feature at request time (see BR-UC60-03).
6. **[S]** Sends a `PENALTY_ISSUED` notification to the affected user with the penalty type and reason.
7. **[S]** (Background) A scheduled job (`expireOverduePenalties`, every 5 minutes) automatically expires `TEMPORARY_BAN` penalties past their `expiresAt` and restores the user to `ACTIVE` status if no other active ban remains.

### Alternative Flows
- **AF-01 — Self-Penalty Block:** Admin cannot apply a penalty to their own account, nor to another Platform Admin's account.
- **AF-02 — Revoke Penalty:** Admin selects an active penalty and clicks "Revoke" with a reason; system sets status to `REVOKED` and restores `ACTIVE` status if no other active ban exists.
- **AF-03 — Feature Restriction Enforcement:** When a restricted user attempts the restricted action (sending a message, posting a class, applying to a class, or requesting a withdrawal), the system blocks the request with a 403 error until the restriction expires or is revoked.

### Business Rules
- **BR-UC60-01**: Penalties are permanently logged and directly impact user login access or platform privileges.
- **BR-UC60-02**: An admin cannot apply a penalty to their own account or to another Platform Admin's account.
- **BR-UC60-03**: `FEATURE_RESTRICTION` is enforced per feature code (`MESSAGING`, `CLASS_POSTING`, `CLASS_APPLICATION`, `WITHDRAWAL`) via `PenaltyAccessService.requireFeature()`, checked at the point of use rather than as a blanket account lock.
- **BR-UC60-04**: `TEMPORARY_BAN` requires a future `expiresAt`; `PERMANENT_BAN` has no expiry. Expired temporary bans are auto-restored to `ACTIVE` by a scheduled job.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Penalty Type | Text | Required. One of: TEMPORARY_BAN, PERMANENT_BAN, WARNING, FEATURE_RESTRICTION. |
| Reason | Text | Required. Must provide justification. |
| Restriction Details | Text (JSON) | Required for FEATURE_RESTRICTION. Must reference at least one valid feature code. |
| Expires At | Date | Required for TEMPORARY_BAN. Must be a future date. |

**On success:** The penalty is enforced and the user's status/feature access updates immediately, and the user is notified.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin reviewing a user account.
- **When** the admin applies a suspension (ban) penalty.
- **Then** the user is immediately suspended AND receives a notification detailing the reason.
- **Given** an admin applies a `FEATURE_RESTRICTION` on `WITHDRAWAL` to a user.
- **When** that user attempts to request a withdrawal.
- **Then** the request is rejected with a 403 error while the account otherwise remains usable.

---

## UC-61: Monitor Audit Logs
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-41 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | Read-only access. |

### Main Flow
1. **[U]** Navigates to the Audit Logs page (`AuditLogController`).
2. **[S]** Retrieves chronological list of all system actions (e.g., config changes, user bans).
3. **[U]** Uses filters (Date range, Action Type, Actor Email) to investigate a specific event.

### Alternative Flows
- **AF-01 — No Match:** If filters yield no results, "No logs found" is displayed in the table.

### Business Rules
- **BR-UC61-01**: Audit logs are append-only. They cannot be deleted or modified by any user, including admins.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Date Range | Date | Optional. Filter logs by time. |
| Action Type | Text | Optional. Filter by event type (e.g., PENALTY_APPLIED). |

**On success:** The admin sees a filtered view of the system audit logs.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin on the Audit Logs page.
- **When** they apply specific Date Range and Action Type filters.
<<<<<<< Updated upstream
- **Then** the table displays only the logs matching the requested criteria.
=======
- **Then** the table displays only the logs matching the requested criteria.

---

## UC-35: Configure Notification Templates
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | Notification Service |
| **Module** | Platform Administration |
| **FT-ID Ref** | FT-47 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | Notification template records are created, updated, or disabled. Future notifications using that template code render with the updated content. |

### Main Flow
1. **[U]** Navigates to Notification Templates (`PlatformNotificationTemplatesPage`).
2. **[S]** Fetches the list via `GET /api/platform/notification-templates`.
3. **[U]** Clicks "Create Template", enters a unique code, title template, content template (with `{{placeholder}}` syntax), channel, and enabled flag, then clicks Save.
4. **[S]** Validates the payload and creates the record via `POST /api/platform/notification-templates`.
5. **[U]** Clicks "Preview", supplies sample JSON variables, and views the rendered title/content.
6. **[S]** Calls `POST /api/platform/notification-templates/preview` to substitute placeholders and return the rendered text.
7. **[S]** At runtime, whenever a module (e.g., Marketplace, Chat, Payment, Verification, Dispute, Penalty) triggers a notification via `NotificationDispatchService.notifyUserFromTemplate(user, type, code, variables, fallbackTitle, fallbackContent, ...)`, the system looks up the enabled template by code, substitutes the variables, and sends it; if the template is disabled or missing, the fallback title/content is used instead.

### Alternative Flows
- **AF-01 — Edit Template:** Admin edits an existing template's title/content/channel and saves via `PATCH /api/platform/notification-templates/{id}`.
- **AF-02 — Disable Template:** Admin disables a template via `DELETE /api/platform/notification-templates/{id}` (soft-disable, not a hard delete); future dispatches for that code fall back to the caller-provided default text.
- **AF-03 — Missing Placeholder:** If a variable referenced in the template is not supplied at render time, the placeholder is left as-is or substituted with an empty value depending on the renderer's behavior.

### Business Rules
- **BR-UC35-01**: Template `code` must be unique (case-insensitive).
- **BR-UC35-02**: Placeholders use the syntax `{{variableName}}`, matched via a fixed regex pattern.
- **BR-UC35-03**: Disabled or non-existent templates cause the dispatch service to fall back to the caller-supplied default title/content rather than failing the notification.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Code | Text | Required. Unique (case-insensitive). |
| Title Template | Text | Required. |
| Content Template | Text | Required. |
| Channel | Text | Required. |
| Enabled | Yes/No | Required boolean. |

**On success:** The admin sees the new/updated template in the list, and the preview reflects the rendered output for supplied sample variables.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin creates a template with code `PENALTY_ISSUED` and content `"Loại: {{penaltyType}}. Lý do: {{reason}}"`.
- **When** `PenaltyServiceImpl.issuePenalty()` later calls `notifyUserFromTemplate(user, ..., "PENALTY_ISSUED", Map.of("penaltyType", ..., "reason", ...), ...)`.
- **Then** the user receives a notification with the placeholders substituted by the actual penalty type and reason.

---

## UC-58: Manage Escrow Transactions
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | None |
| **Module** | Platform Administration / Finance |
| **FT-ID Ref** | FT-39 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | — |
| **Preconditions** | Admin is logged in. |
| **Postconditions** | Escrow funds are released to the beneficiary and/or refunded to the payer; the transaction status is updated. |

### Main Flow
1. **[U]** Navigates to the Escrow Transactions queue (`AdminEscrowQueue`).
2. **[S]** Fetches a filtered, paginated list via `GET /api/platform/escrows?status=&from=&to=&reference=&payer=&beneficiary=&page=&size=`.
3. **[U]** Filters by status and/or keyword (reference), and clicks "Xử lý" (Process) on a transaction.
4. **[S]** Loads the transaction detail via `GET /api/platform/escrows/{escrowId}` into the settlement form (`PlatformEscrowPage`).
5. **[U]** Chooses a split (release all, refund all, half, custom percentage) and confirms.
6. **[S]** Calls `POST /finance/settlements/execute` (release to beneficiary / partial release) or `POST /finance/refunds/execute` (refund to payer) with the `escrowId` and computed amounts.
7. **[S]** Updates the escrow transaction status and records the settlement/refund.

### Alternative Flows
- **AF-01 — No Results:** If filters yield no matching transactions, the queue shows an empty state.
- **AF-02 — Split Settlement:** Admin can split funds between release-to-beneficiary and refund-to-payer in the same action (e.g., 70/30) rather than an all-or-nothing settlement.

### Business Rules
- **BR-UC58-01**: Only PLATFORM_ADMIN can list, view, settle, or refund escrow transactions.
- **BR-UC58-02**: The sum of released and refunded amounts in a settlement action cannot exceed the escrowed amount.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Status | Text | Optional filter. |
| Date Range (`from`, `to`) | Date | Optional filter. |
| Reference / Payer / Beneficiary | Text | Optional filter. |
| Escrow ID | Number | Required for settle/refund actions. |
| Release / Refund Amount | Number | Required. Must not exceed the escrowed amount. |

**On success:** The admin sees the escrow transaction move to a settled/refunded state, and the list reflects the updated status.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin viewing the escrow queue with an `ESCROWED` filter.
- **When** they select a transaction and execute a full release.
- **Then** the beneficiary's wallet is credited AND the escrow transaction status changes accordingly.

---

## UC-59: Detect Platform Circumvention
**UI-type block**

| Field | Value |
|-------|-------|
| **Primary Actor** | Platform Admin |
| **Secondary Actor(s)** | Messaging Service (automatic scan) |
| **Module** | Trust & Safety |
| **FT-ID Ref** | FT-42 |
| **BF-ID Ref** | BF-10 |
| **Depends On** | UC-36 |
| **Preconditions** | Admin is logged in. A chat message has been sent through the Messaging module. |
| **Postconditions** | Suspicious messages are flagged as a `CircumventionEvent` for admin review, and the review decision is recorded. |

### Main Flow
1. **[S]** Every time a chat message is saved (`ChatServiceImpl`), the system automatically inspects its content via `CircumventionService.inspect(message)`.
2. **[S]** The inspector applies regex-based rules for phone numbers, email addresses, URLs, and social-media handles, assigning a risk score (65-90) per matched rule.
3. **[S]** If a rule matches, a `CircumventionEvent` is created with status `PENDING`, storing the matched rule, evidence snippet, and risk score.
4. **[U]** Navigates to the Circumvention Detection page (`PlatformCircumventionPage`).
5. **[S]** Fetches the review queue via `GET /api/platform/circumvention-events?status=&page=&size=`.
6. **[U]** Opens the linked conversation to review context, then clicks "Confirm" or "Dismiss", optionally entering a note.
7. **[S]** Calls `PATCH /api/platform/circumvention-events/{eventId}` to set status to `CONFIRMED` or `DISMISSED`, recording the reviewer and review note.

### Alternative Flows
- **AF-01 — No Match:** If a message triggers no rule, no event is created.
- **AF-02 — Escalation:** Admin may follow up a `CONFIRMED` event by applying a penalty (see UC-60) to the offending user.

### Business Rules
- **BR-UC59-01**: Detection runs automatically and synchronously as part of message delivery; it is not a manual/on-demand scan.
- **BR-UC59-02**: Events default to `PENDING` status and can only transition to `CONFIRMED` or `DISMISSED` via admin review.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Status Filter | Text | Optional. One of PENDING, CONFIRMED, DISMISSED. |
| Review Note | Text | Optional. Max 500 characters. |

**On success:** The admin sees the circumvention event queue update to reflect the review decision.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** a user sends a chat message containing a phone number.
- **When** the message is saved.
- **Then** a `CircumventionEvent` with status `PENDING` is created and appears in the admin review queue.
- **Given** an admin reviewing a `PENDING` event.
- **When** they mark it `CONFIRMED` with a note.
- **Then** the event status updates AND the reviewer and note are recorded.
>>>>>>> Stashed changes
