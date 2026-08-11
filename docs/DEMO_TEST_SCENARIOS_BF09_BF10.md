# BF-09 & BF-10 Test Scenarios
## Kịch Bản Test Cho Demo Video

---

## 🎯 Objective

Đảm bảo tất cả flows hoạt động trơn tru trước khi quay video. Test từng scenario và fix issues nếu có.

---

## 🧪 Pre-Demo Testing Checklist

### Environment Check
```bash
# Backend health check
curl http://localhost:8080/actuator/health

# Frontend accessibility
curl http://localhost:5173
```

### Database Check
```sql
-- Verify test users exist
SELECT user_id, email, role, status FROM "user" 
WHERE email IN ('student@test.com', 'admin@test.com', 'tutor@test.com');

-- Verify FAQ entries exist
SELECT faq_id, question, category, published FROM faq_entry LIMIT 5;

-- Verify system parameters exist
SELECT param_key, param_value FROM system_parameter 
WHERE param_key = 'PLATFORM_FEE_RATE';
```

### Login Check
- [ ] Can login as student@test.com
- [ ] Can login as admin@test.com
- [ ] Can login as tutor@test.com

---

## 📋 BF-09: Customer Support - Test Scenarios

### TS-09-01: Browse FAQ (Public Access)

**Pre-condition**: Logged out (or incognito mode)

**Steps**:
1. Navigate to `http://localhost:5173/help`
2. Verify page loads without authentication
3. Verify FAQ categories are visible
4. Click on category "PAYMENT"
5. Verify FAQ items expand
6. Type "hoàn tiền" in search box
7. Verify filtered results appear

**Expected Results**:
- ✅ Help page accessible without login
- ✅ FAQ accordion works
- ✅ Search filters in real-time
- ✅ No errors in console

**Test Data Needed**:
```sql
-- Insert sample FAQs if missing
INSERT INTO faq_entry (question, answer, category, sort_order, published) VALUES
('Chính sách hoàn tiền như thế nào?', 'Hoàn 100% nếu hủy trước 24h...', 'PAYMENT', 1, true),
('Phí dịch vụ là bao nhiêu?', 'Phí nền tảng 10% trên mỗi giao dịch...', 'PAYMENT', 2, true),
('Làm sao để đăng ký tài khoản?', 'Click Đăng ký > Điền form...', 'ACCOUNT', 1, true);
```

---

### TS-09-02: AI Chatbot - Floating Widget

**Pre-condition**: Logged in as student or anonymous

**Steps**:
1. Navigate to homepage `/`
2. Verify floating chatbot button visible (bottom-right)
3. Click floating button
4. Verify mini chat window opens
5. Type: "Làm sao để tìm gia sư toán lớp 10?"
6. Wait for AI response (5-10s)
7. Verify response contains:
   - Vietnamese text answer
   - Referenced tutor cards (if available)
8. Click "Xem đầy đủ" button
9. Verify navigate to `/ai-assistant`

**Expected Results**:
- ✅ Floating widget appears on all pages except `/ai-assistant`
- ✅ AI responds within 10 seconds
- ✅ Tutor cards clickable
- ✅ Navigation works

**Known Issues**:
- ⚠️ If Groq API key invalid → Falls back to Gemini
- ⚠️ If both fail → Shows error message "AI tạm thời không khả dụng"

**Backend Logs to Check**:
```
INFO  c.t.m.ai.service.impl.AiServiceImpl : RAG retrieval: 3 tutors, 2 classes, 3 FAQs
INFO  c.t.m.ai.service.impl.AiServiceImpl : LLM call successful (Groq)
```

---

### TS-09-03: AI Chatbot - Full Page with RAG

**Pre-condition**: Logged in as student

**Steps**:
1. Navigate to `/ai-assistant`
2. Verify session sidebar visible (left)
3. Type complex query:
   ```
   Tôi cần gia sư Vật Lý lớp 12 ở Hà Nội, giá dưới 200k/buổi. 
   Ngoài ra cho tôi biết quy trình đăng ký học và chính sách hoàn tiền.
   ```
4. Wait for response
5. Verify response contains:
   - Text answer in Vietnamese
   - Referenced tutor cards (filtered by subject=Physics, grade=12)
   - Referenced FAQ cards (about registration, refund)
6. Click on tutor card → navigate to tutor profile
7. Back to `/ai-assistant`
8. Verify session saved in sidebar
9. Click on previous session → load conversation
10. Click delete icon on session → confirm delete

**Expected Results**:
- ✅ RAG retrieves relevant tutors (top 3)
- ✅ RAG retrieves relevant FAQs (top 3)
- ✅ Cards are clickable and navigate correctly
- ✅ Session history persists
- ✅ Delete session works

**Test Data Needed**:
```sql
-- Verify tutors exist
SELECT user_id, full_name, subjects_taught FROM "user" 
WHERE role = 'TUTOR' AND status = 'ACTIVE' LIMIT 5;

-- Verify tutor subjects
SELECT ts.tutor_id, s.subject_name, ts.hourly_rate 
FROM tutor_subject ts 
JOIN subject s ON ts.subject_id = s.subject_id 
LIMIT 5;
```

---

### TS-09-04: User Create Support Ticket

**Pre-condition**: Logged in as student@test.com

**Steps**:
1. Navigate to `/messaging`
2. Click "Support Tickets" tab
3. Verify ticket list (may be empty)
4. Click "Tạo yêu cầu hỗ trợ"
5. Fill form:
   - Category: `BUG_REPORT`
   - Subject: "Không thể thanh toán qua VNPay"
   - Description: "Tôi chọn thanh toán VNPay nhưng redirect về lỗi 500"
   - Evidence URLs: (leave empty or paste test URL)
6. Submit
7. Verify ticket created with:
   - Status: OPEN
   - Priority: HIGH (auto-escalated from BUG_REPORT)
   - Due date: ~12 hours from now
8. Click on ticket to view detail
9. Verify message thread shows initial message
10. Type reply: "Xin bổ sung: Lỗi xảy ra lúc 14h30"
11. Submit reply
12. Verify reply appears in thread

**Expected Results**:
- ✅ Ticket creation succeeds
- ✅ Priority auto-escalated correctly:
  - `BUG_REPORT` → HIGH
  - `SYSTEM_ERROR` → URGENT
  - `INQUIRY` → MEDIUM
  - `REPORT_USER` → MEDIUM
  - `DISPUTE` → HIGH
- ✅ SLA calculated: LOW=48h, MEDIUM=24h, HIGH=12h, URGENT=4h
- ✅ Reply works

**Backend Check**:
```sql
-- Verify ticket created
SELECT ticket_id, category, priority, status, due_at 
FROM support_ticket 
WHERE subject LIKE '%VNPay%' 
ORDER BY created_at DESC LIMIT 1;

-- Verify initial message
SELECT message_id, content, is_from_admin 
FROM ticket_message 
WHERE ticket_id = ? 
ORDER BY created_at;
```

---

### TS-09-05: User Reopen Closed Ticket

**Pre-condition**: 
- Logged in as student
- Have at least 1 CLOSED or RESOLVED ticket

**Steps**:
1. Navigate to `/messaging` > Support Tickets
2. Find a CLOSED or RESOLVED ticket
3. Click on ticket detail
4. Click "Reopen" button
5. Enter reason: "Vấn đề vẫn còn xảy ra"
6. Submit
7. Verify ticket status changed to OPEN
8. Verify dueAt recalculated

**Expected Results**:
- ✅ Reopen succeeds
- ✅ Status: CLOSED/RESOLVED → OPEN
- ✅ Admin receives notification
- ✅ dueAt updated

---

### TS-09-06: Admin View Ticket Queue

**Pre-condition**: Logged in as admin@test.com

**Steps**:
1. Navigate to `/platform/tickets`
2. Verify ticket list loads
3. Test filters:
   - Status: OPEN
   - Category: BUG_REPORT
   - Priority: HIGH
   - Click "Apply"
4. Verify filtered results
5. Test search: Type "VNPay"
6. Verify search results
7. Check for SLA breach indicators (red badge)

**Expected Results**:
- ✅ Ticket list paginated
- ✅ Filters work correctly
- ✅ Search works
- ✅ SLA breach visible (if any)

---

### TS-09-07: Admin Respond to Ticket

**Pre-condition**: 
- Logged in as admin
- Have OPEN ticket from TS-09-04

**Steps**:
1. Navigate to `/platform/tickets`
2. Click on OPEN ticket (status=OPEN, priority=HIGH)
3. Verify modal opens
4. Verify:
   - Ticket auto-assigned to current admin
   - Status changed: OPEN → IN_PROGRESS
5. Verify ticket detail:
   - User info
   - Category, priority, status badges
   - Created at, due at
   - Message thread
6. Test update priority:
   - Change from HIGH to URGENT
   - Add reason: "Ảnh hưởng nhiều users"
   - Submit
   - Verify priority updated, dueAt recalculated (4h)
7. Test respond:
   - Type message: "Team đã xác định lỗi. Đang fix, dự kiến 2h nữa resolve."
   - Submit
   - Verify message appears in thread
   - Verify responseSlaMs recorded
8. Test close ticket:
   - Select status: RESOLVED
   - Enter note: "Đã fix lỗi VNPay integration. User test lại."
   - Submit
   - Verify ticket closed, resolvedAt timestamp

**Expected Results**:
- ✅ Auto-assignment works
- ✅ Status transition: OPEN → IN_PROGRESS → RESOLVED
- ✅ Priority update with dueAt recalculation
- ✅ Response SLA recorded
- ✅ User notified (check notifications table)

**Backend Check**:
```sql
-- Verify ticket updated
SELECT ticket_id, assigned_admin_id, status, priority, 
       response_sla_ms, resolved_at, sla_breached
FROM support_ticket 
WHERE ticket_id = ?;

-- Verify admin response message
SELECT message_id, content, is_from_admin, created_at 
FROM ticket_message 
WHERE ticket_id = ? 
ORDER BY created_at;

-- Verify audit log
SELECT action, entity_type, entity_id, old_value, new_value 
FROM audit_log 
WHERE entity_type = 'SupportTicket' AND entity_id = ?::text 
ORDER BY created_at DESC;
```

---

### TS-09-08: Admin Manage FAQ

**Pre-condition**: Logged in as admin@test.com

**Steps**:
1. Navigate to `/platform/faq`
2. Verify FAQ list loads
3. Test create FAQ:
   - Click "Tạo FAQ mới"
   - Fill:
     - Question: "Làm sao để rút tiền từ ví TCS?"
     - Answer: "Vào Ví của tôi > Rút tiền. Nhập số tiền và thông tin ngân hàng..."
     - Category: PAYMENT
     - Published: true
     - Sort order: 10
   - Submit
   - Verify FAQ created
4. Test edit FAQ:
   - Click on newly created FAQ
   - Edit answer: Add "Lưu ý: Số tiền tối thiểu 100,000 VND"
   - Update sort order: 5
   - Submit
   - Verify FAQ updated
5. Test unpublish:
   - Toggle published to false
   - Submit
   - Verify FAQ hidden from public Help page (check `/help`)
6. Test delete:
   - Click delete icon
   - Confirm
   - Verify FAQ deleted

**Expected Results**:
- ✅ CRUD operations work
- ✅ Published flag controls public visibility
- ✅ Sort order affects display order
- ✅ All actions audit-logged

**Backend Check**:
```sql
-- Verify FAQ
SELECT faq_id, question, answer, category, published, sort_order 
FROM faq_entry 
WHERE question LIKE '%rút tiền%';

-- Verify audit logs
SELECT action, entity_type, old_value, new_value 
FROM audit_log 
WHERE entity_type = 'FaqEntry' 
ORDER BY created_at DESC LIMIT 5;
```

---

## 📊 BF-10: Platform Administration - Test Scenarios

### TS-10-01: View Admin Dashboard

**Pre-condition**: Logged in as admin@test.com

**Steps**:
1. Navigate to `/platform/dashboard`
2. Verify KPI cards display:
   - Total Users (with role breakdown)
   - Active Classes
   - Pending Verifications
   - Open Tickets
   - Recent Reports
3. Verify numbers are > 0 (need seed data)
4. Check Alerts section:
   - SLA-breached tickets
   - Pending verifications
   - High-priority reports
5. Click on alert link → verify navigation

**Expected Results**:
- ✅ Real-time KPI counts
- ✅ Role breakdown accurate
- ✅ Alert links navigate correctly
- ✅ No errors in console

**Backend Check**:
```sql
-- Verify counts match
SELECT role, COUNT(*) FROM "user" GROUP BY role;
SELECT status, COUNT(*) FROM tutoring_class GROUP BY status;
SELECT COUNT(*) FROM verification_request WHERE status = 'PENDING';
SELECT COUNT(*) FROM support_ticket WHERE status IN ('OPEN', 'IN_PROGRESS');
```

---

### TS-10-02: Manage System Categories

**Pre-condition**: Logged in as admin

**Note**: If no UI page exists, test via Swagger UI at `http://localhost:8080/swagger-ui.html`

**API Tests**:
```bash
# Get category tree
curl -X GET "http://localhost:8080/api/catalog/categories?root=true" \
  -H "Authorization: Bearer {admin_token}"

# Create category
curl -X POST "http://localhost:8080/api/catalog/categories" \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sinh học",
    "type": "SUBJECT",
    "parentId": null,
    "status": "ACTIVE",
    "sortOrder": 5
  }'

# Update category
curl -X PUT "http://localhost:8080/api/catalog/categories/{categoryId}" \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sinh học (Biology)",
    "sortOrder": 3
  }'

# Delete category (should fail if has children)
curl -X DELETE "http://localhost:8080/api/catalog/categories/{categoryId}" \
  -H "Authorization: Bearer {admin_token}"
```

**Expected Results**:
- ✅ Tree structure maintained (parent/child)
- ✅ Cannot delete if has children or in use
- ✅ All actions audit-logged
- ✅ Sort order controls display

---

### TS-10-03: Configure Platform Fees

**Pre-condition**: Logged in as admin@test.com

**Steps**:
1. Navigate to `/platform/parameters`
2. Search for: "PLATFORM_FEE_RATE"
3. Verify current value displayed (e.g., "0.10")
4. Click edit icon
5. Change value to "0.12"
6. Update description: "Platform fee rate (0-1). Current: 12%. Updated Aug 2026."
7. Submit
8. Verify success message
9. Verify parameter updated in list

**Expected Results**:
- ✅ Parameter update succeeds
- ✅ Value validated (0-1 range)
- ✅ Audit log created
- ✅ Analytics will use new rate

**Backend Check**:
```sql
-- Verify parameter updated
SELECT param_key, param_value, description, updated_at 
FROM system_parameter 
WHERE param_key = 'PLATFORM_FEE_RATE';

-- Verify audit log
SELECT action, old_value, new_value 
FROM audit_log 
WHERE entity_type = 'SystemParameter' 
  AND entity_id = (SELECT parameter_id::text FROM system_parameter WHERE param_key = 'PLATFORM_FEE_RATE')
ORDER BY created_at DESC LIMIT 1;
```

---

### TS-10-04: Monitor Audit Logs

**Pre-condition**: 
- Logged in as admin
- Have performed some admin actions (create FAQ, update ticket, etc.)

**Steps**:
1. Navigate to `/platform/audit-logs`
2. Verify audit log list displays:
   - Actor (userId + role)
   - Action (CREATE, UPDATE, DELETE)
   - Entity Type
   - Entity ID
   - Timestamp
   - IP Address
3. Test filters:
   - Actor: Select specific admin
   - Action: UPDATE
   - Entity Type: FAQ
   - Date range: Last 7 days
   - Click "Apply"
4. Verify filtered results
5. Click on audit log entry (e.g., FAQ update)
6. Verify detail modal shows:
   - Old value (JSON)
   - New value (JSON)
   - Diff highlighted (if UI supports)
7. Test another entry: SystemParameter update
   - oldValue: {"paramValue": "0.10"}
   - newValue: {"paramValue": "0.12"}

**Expected Results**:
- ✅ All admin write actions logged
- ✅ Filters work correctly
- ✅ JSON old/new values displayed
- ✅ Diff viewer shows changes clearly
- ✅ Pagination works

**Sample Audit Logs to Verify**:
```sql
SELECT 
  al.audit_id,
  al.actor_id,
  u.full_name as actor_name,
  al.actor_role,
  al.action,
  al.entity_type,
  al.entity_id,
  al.old_value,
  al.new_value,
  al.ip_address,
  al.created_at
FROM audit_log al
LEFT JOIN "user" u ON al.actor_id = u.user_id
ORDER BY al.created_at DESC
LIMIT 20;
```

---

### TS-10-05: View Financial Reports

**Pre-condition**: 
- Logged in as admin
- Database has transaction data (classes, payments)

**Steps**:
1. Navigate to `/platform/analytics`
2. Verify summary section displays:
   - Total Revenue (6-month aggregate)
   - Platform Fee Revenue (calculated from PLATFORM_FEE_RATE)
   - Total Users by month
   - Total Classes by month
   - Verification Conversion Rate
   - Dispute Rate
   - Contract Completion Rate
3. Verify monthly breakdown (table or chart):
   - Month column
   - Revenue column
   - User count column
   - Class count column
4. Test CSV export - Users:
   - Click "Export Users CSV"
   - Wait for download
   - Open CSV file
   - Verify columns: userId, email, fullName, role, status, createdAt, emailVerified
   - Verify data accuracy (spot check 3-5 rows)
5. Test CSV export - Classes:
   - Click "Export Classes CSV"
   - Verify columns: classId, subject, grade, tutorName, fee, status, createdAt
6. Test CSV export - Revenue:
   - Click "Export Revenue CSV"
   - Verify columns: month, totalRevenue, platformFeeRevenue, tutorRevenue, classCount, avgFeePerClass
   - Verify platformFeeRevenue = totalRevenue × PLATFORM_FEE_RATE

**Expected Results**:
- ✅ Summary metrics accurate
- ✅ Platform fee calculated correctly using system parameter
- ✅ CSV files download successfully
- ✅ CSV data matches database
- ✅ 6-month rolling window

**Backend Verification**:
```sql
-- Verify total revenue (sample for current month)
SELECT 
  DATE_TRUNC('month', created_at) as month,
  SUM(total_fee) as total_revenue,
  COUNT(*) as class_count
FROM tutoring_class
WHERE status = 'COMPLETED'
  AND created_at >= NOW() - INTERVAL '6 months'
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month DESC;

-- Verify user count by month
SELECT 
  DATE_TRUNC('month', created_at) as month,
  COUNT(*) as user_count,
  role
FROM "user"
WHERE created_at >= NOW() - INTERVAL '6 months'
GROUP BY DATE_TRUNC('month', created_at), role
ORDER BY month DESC, role;

-- Verify platform fee calculation
SELECT 
  (SELECT param_value::numeric FROM system_parameter WHERE param_key = 'PLATFORM_FEE_RATE') as fee_rate,
  SUM(total_fee) as total_revenue,
  SUM(total_fee) * (SELECT param_value::numeric FROM system_parameter WHERE param_key = 'PLATFORM_FEE_RATE') as platform_fee
FROM tutoring_class
WHERE status = 'COMPLETED';
```

---

## 🔧 Fix Issues Before Demo

### Common Issues & Solutions

#### Issue 1: AI Chatbot Returns Error
**Symptom**: "AI tạm thời không khả dụng"

**Fixes**:
- Check Groq API key in `application.properties`
- Verify API key has quota: https://console.groq.com
- Check Gemini fallback key
- Review backend logs for API errors

#### Issue 2: Tickets Tab Not Showing
**Symptom**: `/messaging/tickets` shows conversation list instead

**Fix**:
- Navigate to `/messaging` manually
- Click "Support Tickets" tab
- Or modify route to render `MessagingPanel` directly

#### Issue 3: CSV Export Empty
**Symptom**: Downloaded CSV has headers only

**Fix**:
- Seed more data into database
- Verify date range (6-month window may be empty for fresh DB)
- Check backend logs for query errors

#### Issue 4: SLA Scheduler Not Running
**Symptom**: No SLA breach indicators even for old tickets

**Fix**:
- Verify `@EnableScheduling` on main application class
- Check scheduler logs: `TicketSlaScheduler.checkSlaBreaches()`
- Manually trigger: Update tickets with `due_at < NOW()` and `sla_breached = false`

---

## 📊 Seed Data Script for Demo

```sql
-- Use this script to ensure good demo data

-- Insert sample FAQ entries (if missing)
INSERT INTO faq_entry (question, answer, category, sort_order, published) VALUES
('Làm sao để tìm gia sư phù hợp?', 'Sử dụng bộ lọc theo môn học, cấp độ, khu vực và giá. Xem profile và reviews.', 'GENERAL', 1, true),
('Chính sách hoàn tiền?', 'Hoàn 100% nếu hủy trước 24h, 50% trước 12h, 0% trong 12h.', 'PAYMENT', 2, true),
('Phí dịch vụ là bao nhiêu?', 'Platform thu 10% phí trên mỗi giao dịch. Tutor nhận 90%.', 'PAYMENT', 3, true),
('Làm sao để trở thành gia sư?', 'Đăng ký tài khoản > Chọn role Tutor > Upload CMND và bằng cấp > Chờ duyệt.', 'ACCOUNT', 4, true),
('Quy trình đăng ký học như thế nào?', 'Tìm gia sư > Xem profile > Đăng ký lớp > Thanh toán deposit > Chờ gia sư xác nhận.', 'GENERAL', 5, true);

-- Insert system parameters (if missing)
INSERT INTO system_parameter (param_key, param_value, description) VALUES
('PLATFORM_FEE_RATE', '0.10', 'Platform fee rate (0-1). Current: 10%'),
('MAX_FILE_SIZE', '10485760', 'Max upload file size in bytes (10MB)'),
('SESSION_TIMEOUT', '3600', 'Session timeout in seconds (1 hour)')
ON CONFLICT (param_key) DO NOTHING;

-- Insert sample support tickets for admin to demo
INSERT INTO support_ticket (user_id, category, subject, description, priority, status, due_at, created_at) VALUES
(
  (SELECT user_id FROM "user" WHERE email = 'student@test.com' LIMIT 1),
  'BUG_REPORT', 
  'Lỗi thanh toán VNPay', 
  'Redirect về error 500 khi chọn VNPay payment',
  'HIGH',
  'OPEN',
  NOW() + INTERVAL '12 hours',
  NOW() - INTERVAL '2 hours'
),
(
  (SELECT user_id FROM "user" WHERE email = 'student@test.com' LIMIT 1),
  'INQUIRY',
  'Hỏi về chính sách hủy lớp',
  'Tôi muốn hủy lớp đã đăng ký. Được hoàn tiền không?',
  'MEDIUM',
  'IN_PROGRESS',
  NOW() + INTERVAL '24 hours',
  NOW() - INTERVAL '1 day'
);

-- Insert messages for tickets
INSERT INTO ticket_message (ticket_id, sender_id, is_from_admin, content, created_at)
SELECT 
  st.ticket_id,
  st.user_id,
  false,
  'Đây là mô tả chi tiết của vấn đề tôi gặp phải.',
  st.created_at
FROM support_ticket st
WHERE st.subject = 'Lỗi thanh toán VNPay';

-- Mark one ticket as past SLA (for demo SLA breach)
UPDATE support_ticket 
SET 
  due_at = NOW() - INTERVAL '2 hours',
  sla_breached = true
WHERE subject = 'Hỏi về chính sách hủy lớp';

-- Verify seed data
SELECT 'FAQ Count' as metric, COUNT(*)::text as value FROM faq_entry WHERE published = true
UNION ALL
SELECT 'System Parameters', COUNT(*)::text FROM system_parameter
UNION ALL
SELECT 'Open Tickets', COUNT(*)::text FROM support_ticket WHERE status = 'OPEN'
UNION ALL
SELECT 'Active Users', COUNT(*)::text FROM "user" WHERE status = 'ACTIVE';
```

---

## ✅ Final Checklist Before Recording

### Data
- [ ] At least 5 published FAQ entries
- [ ] At least 3 system parameters including PLATFORM_FEE_RATE
- [ ] At least 2 open support tickets (1 normal, 1 SLA-breached)
- [ ] At least 3 active tutors with subjects
- [ ] At least 10 users (mix of roles)
- [ ] At least 5 completed classes for revenue calculation

### Accounts
- [ ] student@test.com / password - works
- [ ] admin@test.com / password - works
- [ ] tutor@test.com / password - works

### Features
- [ ] FAQ search works
- [ ] AI chatbot responds (Groq or Gemini)
- [ ] Support ticket creation works
- [ ] Admin ticket queue loads
- [ ] Audit logs display
- [ ] CSV export downloads
- [ ] Dashboard KPIs show > 0

### UI/UX
- [ ] No console errors
- [ ] Pages load within 3 seconds
- [ ] Responsive UI (test at 1920x1080)
- [ ] Toast notifications appear
- [ ] Modal animations smooth

### Recording Setup
- [ ] Clear browser cache
- [ ] Close unnecessary tabs/apps
- [ ] Disable notifications
- [ ] Test microphone audio
- [ ] Screen recording software ready (OBS, Camtasia, etc.)
- [ ] Script and notes beside monitor

---

**Good luck with your demo! 🎥**
