# Use Case Documentation - feature/user Branch (BF-09 & BF-10 Focus)

> **Document Version**: 4.1 (PRD Standard Template - Fully Compliant)
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

*(Note: UC-35 Configure Notification Templates, UC-58 Manage Escrow Transactions, and UC-59 Detect Platform Circumvention are currently partially implemented/API-only and are excluded from this UI documentation until fully developed.)*

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
| **Postconditions** | A message is stored and delivered to the recipient in real-time. |

### Main Flow
1. **[U]** Opens the Messaging page from the navigation menu.
2. **[S]** Loads existing conversations and establishes a WebSocket connection.
3. **[U]** Selects a conversation.
4. **[S]** Loads the paginated message history.
5. **[U]** Types a message and clicks Send.
6. **[S]** Stores the message in the database and broadcasts it via WebSocket (`/topic/conversation/{id}`).
7. **[S]** Updates the conversation's `lastMessageAt` and unread counters.

### Alternative Flows
- **AF-01 — WebSocket Disconnected:** System falls back to REST API for sending.

### Business Rules
- **BR-UC36-01**: Messages are text-only, maximum 5000 characters.
- **BR-UC36-02**: Read receipts (`lastReadAt`) are tracked per user per conversation.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Content | Text | Required. Max 5000 characters. |

**On success:** The person sees their sent message appear immediately in the chat thread.

### Allowed Roles
- Client, Tutor, Tutor Center, Platform Admin

### Verification Criteria
- **Given** two authenticated users.
- **When** User A sends a valid message to User B.
- **Then** the message is saved AND User B receives it in real-time via WebSocket.

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
1. **[U]** Navigates to a user profile or active class.
2. **[U]** Clicks "Report User" or "Report Class".
3. **[U]** Selects a report category, enters details, and clicks Submit.
4. **[S]** Validates the payload and creates a `Report` record via `POST /reports`.
5. **[S]** Displays a success message confirming the report was sent to Platform Admins.
6. **[U]** (Admin Flow) Logs in and navigates to Reports view (`GET /reports`).
7. **[U]** (Admin Flow) Reviews the report and clicks Resolve.
8. **[S]** (Admin Flow) Validates and sends `PATCH /reports/{reportId}/resolve` to mark it as resolved.

### Alternative Flows
- **AF-01 — Invalid Payload:** At step 4, if details are empty, validation fails and prompts the user.

### Business Rules
- **BR-UC52-01**: Admins review and manually apply penalties if the report is valid.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Target Type | Text | Required. USER or CLASS. |
| Target ID | Number | Required. Valid ID of the reported entity. |
| Category | Text | Required. E.g., SPAM, HARASSMENT, SCAM. |
| Description | Text | Required. |

**On success:** The person sees a success message confirming the report was lodged.

### Allowed Roles
- Client, Tutor, Tutor Center (Submitter). Platform Admin (Reviewer).

### Verification Criteria
- **Given** an authenticated user.
- **When** the user submits a report with valid details.
- **Then** a report record is created in the DB AND is visible to admins for resolution.

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
1. **[U]** Navigates to Financial Reports.
2. **[S]** Aggregates data from `PlatformAnalyticsController`.
3. **[S]** Displays total platform revenue, deposits, withdrawals, and escrow balances over time.
4. **[U]** Uses date filters to adjust the chart views.

### Alternative Flows
- **AF-01 — No Data:** If no data exists for the selected period, charts display an empty state placeholder.

### Business Rules
- **BR-UC41-01**: Access to financial data is restricted to PLATFORM_ADMIN.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Date Range | Date | Optional. Used for filtering. |

**On success:** The admin sees updated aggregate financial metrics based on the applied filters.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin on the financial report page.
- **When** they apply a date filter.
- **Then** the dashboard data re-aggregates accurately for the selected period.

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
| **Postconditions** | CSV file is generated and downloaded. |

### Main Flow
1. **[U]** Navigates to Financial Reports.
2. **[U]** Selects a date range and clicks "Export CSV".
3. **[S]** Generates the CSV report from the database.
4. **[S]** Streams the file download to the user's browser.

### Alternative Flows
- **AF-01 — Empty Export:** If no transactions occurred, the exported CSV contains only headers.

### Business Rules
- **BR-UC43-01**: Export actions are recorded in the audit trail.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Date Range | Date | Required. Start and end date for the export. |

**On success:** A CSV file is successfully generated and downloaded to the admin's machine.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin on the financial report page.
- **When** they click Export CSV.
- **Then** a CSV file is generated containing all transactions for the selected period.

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
- **AF-01 — Invalid Bounds:** If fee rate > 50%, system rejects with "Fee rate exceeds allowed bounds."

### Business Rules
- **BR-UC46-01**: Platform Fee Rate must be a valid decimal between 0.00 and 0.50.
- **BR-UC46-02**: Changes apply only to future contracts.

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
2. **[S]** Fetches KPIs via `PlatformController.getDashboardStats()`.
3. **[S]** Renders summary cards (Pending Verifications, Open Tickets, New Users) and revenue charts.

### Alternative Flows
- **AF-01 — Service Error:** If backend fails, a fallback generic "Service Unavailable" is displayed instead of charts.

### Business Rules
- **BR-UC56-01**: Dashboard stats are cached for performance and refreshed periodically.

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
- **Then** the system displays the aggregated KPI summaries and charts.

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
2. **[S]** Shows the penalty modal (handled via `PlatformPenaltyController`).
3. **[U]** Selects a penalty type (e.g., Account Suspension, Warning), inputs a reason, and saves.
4. **[S]** Records the penalty in the database.
5. **[S]** Triggers the penalty effects (e.g., locks account) and notifies the user.

### Alternative Flows
- **AF-01 — Self-Penalty Block:** Admin cannot apply a penalty to their own account.

### Business Rules
- **BR-UC60-01**: Penalties are permanently logged and directly impact user login access or platform privileges.

### Request Fields
| Field | Type | Rule |
|-------|------|------|
| Penalty Type | Text | Required. E.g., SUSPENSION, WARNING. |
| Reason | Text | Required. Must provide justification. |

**On success:** The penalty is enforced and the user's status updates immediately.

### Allowed Roles
- Platform Admin

### Verification Criteria
- **Given** an admin reviewing a user account.
- **When** the admin applies a suspension penalty.
- **Then** the user is immediately suspended AND receives a notification detailing the reason.

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
- **Then** the table displays only the logs matching the requested criteria.