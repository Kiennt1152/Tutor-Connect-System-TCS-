# Hướng Dẫn Demo BF-09 & BF-10 - Video Recording Guide

## Mục Lục
- [I. Chuẩn Bị](#i-chuẩn-bị)
- [II. BF-09: Customer Support](#ii-bf-09-customer-support)
- [III. BF-10: Platform Administration](#iii-bf-10-platform-administration)
- [IV. Script Demo Timeline](#iv-script-demo-timeline)

---

## I. Chuẩn Bị

### 1.1 Môi Trường
```bash
# Terminal 1: Backend
cd backend
mvn spring-boot:run

# Terminal 2: Frontend
cd frontend
npm run dev
```

### 1.2 Tài Khoản Test
Cần chuẩn bị:
- **User thường**: student@test.com / password
- **Admin**: admin@test.com / password
- **Tutor đã verify**: tutor@test.com / password

### 1.3 Checklist Trước Demo
- [ ] Backend chạy ở `http://localhost:8080`
- [ ] Frontend chạy ở `http://localhost:5173`
- [ ] Database có seed data (users, subjects, grades, FAQ entries)
- [ ] Test login cả user và admin
- [ ] Clear browser cache/cookies nếu cần

### 1.4 Browser Setup
- Mở 2 browser windows side-by-side:
  - **Window 1**: User view (logged in as student)
  - **Window 2**: Admin view (logged in as admin)

---

## II. BF-09: Customer Support

### UC-61: Browse FAQ (Public)
**Demo Time: 2 phút**

#### Màn Hình: Help Page
**URL**: `http://localhost:5173/help`

#### Script Demo
```
1. Navigate to Help page (có thể access từ footer hoặc /help)
2. Giới thiệu: "Đây là trang trợ giúp công khai, người dùng chưa login cũng access được"
3. Show FAQ accordion:
   - Click vào 1-2 category để mở FAQ
   - Highlight: "FAQ được group theo category, có search box"
4. Demo search:
   - Type keyword vào search box (vd: "thanh toán", "giá", "hoàn tiền")
   - Show kết quả filtered
```

#### Các Điểm Cần Highlight
- ✅ FAQ public, không cần auth
- ✅ Group theo category
- ✅ Search real-time
- ✅ Accordion UI responsive

---

### UC-65: Get Support (AI Chatbot)
**Demo Time: 3 phút**

#### Màn Hình 1: Floating Widget
**Location**: Mọi page (trừ /ai-assistant)

#### Script Demo - Part 1: Widget
```
1. Navigate đến any page (vd: home, browse tutors)
2. Point out floating chatbot button góc dưới bên phải
3. Click floating button → opens mini chat window
4. Type câu hỏi đơn giản:
   "Làm sao để tìm gia sư toán lớp 10?"
5. Show AI response với:
   - Text answer (Vietnamese)
   - Referenced tutors (cards với avatar, name, subjects)
   - Referenced classes (nếu có)
6. Click "Xem đầy đủ" để navigate sang full AI Assistant page
```

#### Màn Hình 2: Full AI Assistant Page
**URL**: `http://localhost:5173/ai-assistant`

#### Script Demo - Part 2: Full Page
```
1. Show layout:
   - Left sidebar: Chat sessions history (top 20 nếu anonymous)
   - Main area: Active chat thread
2. Demo complex query:
   "Tôi cần gia sư Vật Lý lớp 12 ở Hà Nội, giá dưới 200k/buổi. 
    Ngoài ra cho tôi biết quy trình đăng ký học và chính sách hoàn tiền."
3. Show AI response với:
   - Detailed Vietnamese answer
   - Referenced tutors (với filter criteria matched)
   - Referenced FAQ entries (liên quan đến payment, refund policy)
4. Click vào tutor card → navigate to tutor detail
5. Back to AI page, show session history sidebar:
   - Click vào previous session để switch
6. Demo delete session (trash icon)
```

#### Các Điểm Cần Highlight
- ✅ RAG (Retrieval-Augmented Generation): AI pull data từ tutors, classes, FAQ
- ✅ Rich cards: Tutor, Class, FAQ references (clickable)
- ✅ Session history: Multi-turn conversation
- ✅ Floating widget: Always accessible
- ✅ Vietnamese response, LaTeX math support (nếu hỏi về công thức)

---

### UC-53: Request Support (User Tickets)
**Demo Time: 3 phút**

#### Màn Hình: Messaging Panel (Tickets Tab)
**URL**: `http://localhost:5173/messaging` (hoặc `/messaging/tickets`)

#### Script Demo
```
1. Login as student user
2. Navigate to Messaging > Support Tickets tab
3. Show ticket list (nếu có sẵn):
   - Status badges (OPEN, IN_PROGRESS, RESOLVED, CLOSED)
   - Priority indicators (LOW, MEDIUM, HIGH, URGENT với colors)
   - Category labels
4. Demo create new ticket:
   - Click "Tạo yêu cầu hỗ trợ"
   - Fill form:
     * Category: "BUG_REPORT" (hoặc INQUIRY, SYSTEM_ERROR, REPORT_USER, DISPUTE)
     * Subject: "Không thể thanh toán qua VNPay"
     * Description: "Tôi chọn thanh toán VNPay nhưng redirect về lỗi 500"
     * Evidence URLs (optional): paste image link
   - Submit
5. Show ticket created:
   - Auto-assigned priority (BUG_REPORT → HIGH)
   - Due date calculated (SLA: HIGH = 12h)
   - Status: OPEN
6. Click vào ticket để xem detail:
   - Show ticket info panel
   - Show message thread (initial message)
7. Demo reply to ticket:
   - Type message: "Xin bổ sung: lỗi xảy ra lúc 14h30 ngày 8/8"
   - Attach evidence URL (optional)
   - Submit
8. Demo reopen closed ticket (nếu có ticket CLOSED):
   - Click "Reopen" button
   - Enter reason
   - Submit → ticket quay lại OPEN
```

#### Các Điểm Cần Highlight
- ✅ Category-based priority escalation (BUG_REPORT, SYSTEM_ERROR → HIGH/URGENT)
- ✅ SLA enforcement (LOW=48h, MEDIUM=24h, HIGH=12h, URGENT=4h)
- ✅ Message thread (user-admin conversation)
- ✅ Evidence URLs support
- ✅ Reopen capability

---

### UC-63: Manage User Support Requests (Admin)
**Demo Time: 4 phút**

#### Màn Hình: Platform Tickets Page (Admin)
**URL**: `http://localhost:5173/platform/tickets`

#### Script Demo
```
1. Login as admin
2. Navigate to Platform > Support Tickets
3. Show ticket list with filters:
   - Filter by status: OPEN, IN_PROGRESS, RESOLVED, CLOSED
   - Filter by category: INQUIRY, BUG_REPORT, etc.
   - Filter by priority: LOW, MEDIUM, HIGH, URGENT
   - Search by keyword
4. Apply filter: Status=OPEN, Priority=HIGH
5. Show ticket queue:
   - SLA breach indicator (red badge nếu past due)
   - Response time (responseSlaMs)
   - Assigned admin (nếu có)
6. Click vào ticket detail (modal opens):
   - Auto-assigns current admin (OPEN → IN_PROGRESS)
   - Show ticket info:
     * User info
     * Category, Priority, Status
     * Created at, Due at
     * SLA breached flag
   - Show message thread
7. Demo admin actions:
   a. Update priority:
      - Change from HIGH → URGENT
      - Reason: "Ảnh hưởng nhiều users"
      - Submit → priority updated, dueAt recalculated
   b. Respond to ticket:
      - Type message: "Chúng tôi đã xác định lỗi. Team đang fix, dự kiến 2h nữa resolve."
      - Submit → responseSlaMs recorded, user notified
   c. Close ticket:
      - Select status: RESOLVED (hoặc CLOSED)
      - Enter resolution note: "Đã fix lỗi VNPay integration. User test lại nhé."
      - Submit → ticket closed, resolvedAt timestamp
8. Back to list, show ticket status updated
```

#### Các Điểm Cần Highlight
- ✅ Auto-assignment: First admin to open ticket được assign
- ✅ SLA monitoring: Scheduler đánh dấu breach every 10 min
- ✅ Response SLA tracking: Thời gian từ OPEN → first admin reply
- ✅ Priority/category update với audit log
- ✅ Resolution notes
- ✅ User notification (in-app)

---

### UC-67: Manage FAQ Knowledge Base (Admin)
**Demo Time: 3 phút**

#### Màn Hình: Platform FAQ Page (Admin)
**URL**: `http://localhost:5173/platform/faq`

#### Script Demo
```
1. Login as admin
2. Navigate to Platform > FAQ Management
3. Show FAQ list:
   - Published/Unpublished status
   - Category
   - Sort order
   - Search by keyword
4. Demo create new FAQ:
   - Click "Tạo FAQ mới"
   - Fill form:
     * Question: "Làm sao để rút tiền từ ví TCS?"
     * Answer: "Vào Ví của tôi > Rút tiền, nhập số tiền và thông tin ngân hàng..."
     * Category: "PAYMENT" (hoặc GENERAL, ACCOUNT, CLASS)
     * Published: true
     * Sort order: 10
   - Submit → FAQ created, audit log recorded
5. Show new FAQ appeared in list
6. Demo edit FAQ:
   - Click vào FAQ
   - Edit answer (bổ sung info)
   - Update sort order
   - Submit → FAQ updated, audit log recorded
7. Demo unpublish FAQ:
   - Toggle published = false
   - Submit → FAQ hidden from public view
8. Demo delete FAQ:
   - Click delete icon
   - Confirm → FAQ deleted, audit log recorded
```

#### Các Điểm Cần Highlight
- ✅ Published flag: Control visibility on public Help page
- ✅ Category grouping
- ✅ Sort order: Control display order
- ✅ Audit logging: All CRUD actions logged
- ✅ Search: Admin can search all FAQ (including unpublished)

---

## III. BF-10: Platform Administration

### UC-56: View Administrative Dashboard
**Demo Time: 2 phút**

#### Màn Hình: Platform Dashboard
**URL**: `http://localhost:5173/platform/dashboard`

#### Script Demo
```
1. Login as admin
2. Navigate to Platform > Dashboard
3. Show KPI cards (real-time counts):
   - Total users (với breakdown by role: student/tutor/parent)
   - Active classes
   - Pending verifications
   - Open tickets
   - Recent reports
4. Show alerts/notifications section:
   - SLA-breached tickets
   - Pending verification requests
   - High-priority reports
5. Scroll down: Recent activity timeline (optional, nếu có implement)
```

#### Các Điểm Cần Highlight
- ✅ Real-time metrics
- ✅ Role breakdown
- ✅ Alert system
- ✅ Quick navigation links

---

### UC-57: Manage System Categories
**Demo Time: 3 phút**

#### Màn Hình: (Integrated in Catalog or separate Category Management page)
**URL**: `http://localhost:5173/platform/categories` (hoặc `/catalog/categories`)

**Note**: Nếu chưa có dedicated UI page, có thể demo qua API tool (Postman/Swagger) hoặc skip

#### Script Demo (nếu có UI)
```
1. Login as admin
2. Navigate to Categories Management
3. Show category tree:
   - Root categories: SUBJECT, EDUCATION_LEVEL, LOCATION, SYSTEM_CONFIG
   - Expand để show children (vd: SUBJECT > Toán, Lý, Hóa)
4. Demo create category:
   - Click "Add category"
   - Fill: Name="Sinh học", Type=SUBJECT, Parent=null, Status=ACTIVE, sortOrder=5
   - Submit → category created, audit logged
5. Demo edit category:
   - Click vào category
   - Edit name/sort order
   - Submit → updated, audit logged
6. Demo delete attempt on category with children:
   - Try delete parent category
   - Show error: "Cannot delete category with children"
7. Demo delete leaf category:
   - Delete a category without children
   - Confirm → deleted, audit logged
```

#### Các Điểm Cần Highlight (backend logic)
- ✅ Tree structure với parent/child validation
- ✅ Type enum (SUBJECT, EDUCATION_LEVEL, LOCATION, SYSTEM_CONFIG)
- ✅ Cannot delete if has children or in use
- ✅ Audit logging
- ✅ Sort order control

---

### UC-46: Configure Platform Fees
**Demo Time: 2 phút**

#### Màn Hình: System Parameters Page
**URL**: `http://localhost:5173/platform/parameters`

#### Script Demo
```
1. Login as admin
2. Navigate to Platform > Cấu hình hệ thống (System Parameters)
3. Search for parameter: "PLATFORM_FEE_RATE"
4. Show current value: "0.10" (= 10%)
5. Demo edit platform fee:
   - Click edit icon
   - Change value to "0.12" (= 12%)
   - Description: "Platform fee rate (0-1). Current: 12%"
   - Submit → parameter updated, audit logged
6. Show success message
7. Explain: "Platform fee được áp dụng khi escrow được giải ngân.
    Gia sư/trung tâm nhận số tiền sau phí; hệ thống ghi một giao dịch PLATFORM_FEE riêng."
```

#### Các Điểm Cần Highlight
- ✅ System parameters: Key-value config store
- ✅ PLATFORM_FEE_RATE drives actual fee deduction for future escrow settlements
- ✅ Analytics sums recorded PLATFORM_FEE transactions instead of estimating from deposits
- ✅ Audit logging
- ✅ Searchable parameters
- ✅ Other parameters: MAX_FILE_SIZE, SESSION_TIMEOUT, etc.

---

### UC-61: Monitor Audit Logs
**Demo Time: 3 phút**

#### Màn Hình: Audit Logs Page
**URL**: `http://localhost:5173/platform/audit-logs`

#### Script Demo
```
1. Login as admin
2. Navigate to Platform > Audit Logs
3. Show audit log list (paginated):
   - Actor (userId + role)
   - Action (CREATE, UPDATE, DELETE)
   - Entity Type (User, FAQ, Ticket, Category, SystemParameter, etc.)
   - Entity ID
   - Timestamp
   - IP Address, User Agent
4. Demo filters:
   a. Filter by actor: Select admin user
   b. Filter by action: UPDATE
   c. Filter by entity type: FAQ
   d. Date range: Last 7 days
   - Click "Apply" → filtered results
5. Click vào 1 audit log entry để xem detail:
   - Show old value (JSON)
   - Show new value (JSON)
   - Highlight diff (nếu có UI diff viewer):
     * Changed fields highlighted
     * Old value vs New value side-by-side
6. Show another example: SystemParameter update (PLATFORM_FEE_RATE)
   - oldValue: "0.10"
   - newValue: "0.12"
```

#### Các Điểm Cần Highlight
- ✅ Comprehensive audit trail: All admin write actions logged
- ✅ JSON old/new values: Full change history
- ✅ Filters: Actor, role, action, entity type, date range, keyword
- ✅ IP + User Agent tracking
- ✅ Diff viewer (nếu có)
- ✅ Compliance: Security, accountability

---

### UC-41 / UC-43: Monitor & Export Financial Reports
**Demo Time: 4 phút**

#### Màn Hình: Platform Analytics Page
**URL**: `http://localhost:5173/platform/analytics`

#### Script Demo
```
1. Login as admin
2. Navigate to Platform > Analytics (hoặc Financial Reports)
3. Show summary section (6-month metrics):
   - Total Revenue
   - Platform Fee Revenue (sum of actual PLATFORM_FEE transactions)
   - Total Users (by month)
   - Total Classes (by month)
   - Verification Conversion Rate
   - Dispute Rate
   - Contract Completion Rate
4. Show monthly breakdown chart (nếu có chart UI):
   - Line chart: Revenue trend
   - Bar chart: User growth
   - Bar chart: Class volume
5. Demo CSV export:
   a. Export Users:
      - Click "Export Users CSV"
      - Download file: users_YYYYMMDD.csv
      - Open CSV, show columns: userId, email, fullName, role, status, createdAt, emailVerified
   b. Export Classes:
      - Click "Export Classes CSV"
      - Download file: classes_YYYYMMDD.csv
      - Show columns: classId, subject, grade, tutor, fee, status, createdAt
   c. Export Revenue:
      - Click "Export Revenue CSV"
      - Download file: revenue_YYYYMMDD.csv
      - Show columns: month, totalRevenue, platformFeeRevenue, tutorRevenue, classCount
6. Explain metrics:
   - "Verification Conversion Rate: % tutors complete verification sau khi apply"
   - "Dispute Rate: % contracts có dispute"
   - "Contract Completion Rate: % contracts reach COMPLETED status"
```

#### Các Điểm Cần Highlight
- ✅ 6-month rolling metrics
- ✅ Platform fee calculation from PLATFORM_FEE_RATE parameter
- ✅ CSV export: Users, Classes, Revenue
- ✅ Real-time aggregation from DB
- ✅ Business intelligence metrics (conversion, dispute, completion rates)

---

## IV. Script Demo Timeline

### Suggested Flow (Total: ~25-30 phút)

#### Part 1: BF-09 Customer Support (12-15 phút)
1. **Intro** (1 min): Giới thiệu BF-09, use cases
2. **UC-61: Browse FAQ** (2 min): Public Help page
3. **UC-65: AI Chatbot** (3 min): Floating widget + Full page với RAG
4. **UC-53: Request Support** (3 min): User tạo ticket, reply, reopen
5. **UC-63: Admin Manage Tickets** (4 min): Admin queue, assign, respond, close, SLA
6. **UC-67: Admin Manage FAQ** (3 min): CRUD FAQ, publish/unpublish

#### Part 2: BF-10 Platform Administration (13-15 phút)
7. **UC-56: Dashboard** (2 min): KPI cards, alerts
8. **UC-57: Categories** (3 min): Tree structure, CRUD với validation (hoặc skip nếu không có UI)
9. **UC-46: Platform Fees** (2 min): Edit PLATFORM_FEE_RATE parameter
10. **UC-61: Audit Logs** (3 min): Filters, JSON diff viewer
11. **UC-41 / UC-43: Financial Reports** (4 min): Summary metrics, CSV export

#### Closing (1 min)
- Recap: "Vừa rồi chúng ta đã demo đầy đủ BF-09 Customer Support và BF-10 Platform Administration"
- Next steps: Testing, documentation, deployment

---

## V. Tips for Video Recording

### Trước Khi Quay
- [ ] Clear browser history/cache
- [ ] Zoom browser to 100% (hoặc 110% nếu demo trên màn hình lớn)
- [ ] Close unnecessary tabs/apps
- [ ] Disable notifications (Windows Focus Assist / Mac Do Not Disturb)
- [ ] Test microphone
- [ ] Prepare script/notes bên cạnh

### Trong Khi Quay
- Speak slowly and clearly (tiếng Việt)
- Point out UI elements: "Ở đây các bạn thấy..."
- Highlight key features: "Điểm đặc biệt là..."
- Show both success and error cases (vd: validation errors)
- Pause briefly between sections

### Sau Khi Quay
- Trim intro/outro
- Add captions/subtitles (optional)
- Add timestamps in video description

---

## VI. Known Issues / Limitations

### BF-09
- ⚠️ `/messaging/tickets` route hiện tại chưa mở đúng tab Tickets (wiring issue)
  - **Workaround**: Navigate to `/messaging` rồi manually click tab "Support Tickets"
- ⚠️ Groq API key hard-coded trong `application.properties`
  - **Impact**: AI chatbot cần API key valid để hoạt động
  - **Workaround**: Ensure API key còn quota, hoặc fallback sang Gemini

### BF-10
- ⚠️ Category Management có thể chưa có dedicated UI page
  - **Workaround**: Demo via Swagger UI (`http://localhost:8080/swagger-ui.html`)
- ⚠️ Analytics chart visualization có thể chưa implement (chỉ có số liệu)
  - **Workaround**: Focus vào CSV export và summary metrics

---

## VII. Test Data Seed Script

Nếu cần seed thêm data để demo:

```sql
-- Insert sample FAQ entries
INSERT INTO faq_entry (question, answer, category, sort_order, published) VALUES
('Làm sao để tìm gia sư phù hợp?', 'Bạn có thể dùng bộ lọc theo môn học, cấp độ, khu vực...', 'GENERAL', 1, true),
('Chính sách hoàn tiền như thế nào?', 'Hoàn 100% nếu hủy trước 24h, 50% nếu hủy trước 12h...', 'PAYMENT', 2, true),
('Làm sao để trở thành gia sư?', 'Đăng ký tài khoản Tutor, upload CMND và bằng cấp...', 'ACCOUNT', 3, true);

-- Insert sample support tickets
INSERT INTO support_ticket (user_id, category, subject, description, priority, status, due_at) VALUES
(1, 'BUG_REPORT', 'Lỗi thanh toán VNPay', 'Redirect về lỗi 500', 'HIGH', 'OPEN', NOW() + INTERVAL '12 hours'),
(2, 'INQUIRY', 'Hỏi về chính sách hủy lớp', 'Tôi muốn hủy lớp đã đăng ký', 'MEDIUM', 'IN_PROGRESS', NOW() + INTERVAL '24 hours');

-- Insert system parameters
INSERT INTO system_parameter (param_key, param_value, description) VALUES
('PLATFORM_FEE_RATE', '0.10', 'Platform fee rate (0-1). Current: 10%'),
('MAX_FILE_SIZE', '10485760', 'Max upload file size in bytes (10MB)'),
('SESSION_TIMEOUT', '3600', 'Session timeout in seconds (1 hour)');
```

---

## VIII. Contact & Support

Nếu gặp vấn đề khi chuẩn bị demo:
- Check terminal logs (backend/frontend)
- Verify database connections
- Review API responses in Browser DevTools > Network tab

Good luck với video demo! 🎥
