# Quick Demo Checklist - BF-09 & BF-10

## Pre-Recording Setup ✓

### Environment
```bash
# Terminal 1
cd backend && mvn spring-boot:run

# Terminal 2  
cd frontend && npm run dev
```

### Test Accounts
- User: `student@test.com` / `password`
- Admin: `admin@test.com` / `password`

### Browser Windows
- Window 1: User view
- Window 2: Admin view

---

## BF-09: Customer Support Demo Flow

### 1. Browse FAQ (2 min)
- [ ] Go to `/help`
- [ ] Show FAQ accordion by category
- [ ] Demo search: type "thanh toán"
- [ ] Highlight: Public access, no login required

### 2. AI Chatbot (AI Trust & Recommendations) (4 min)
- [ ] Show floating widget on any page
- [ ] Ask: "Làm sao để tìm gia sư toán lớp 10?"
- [ ] Show referenced tutors/classes
- [ ] Highlight: AI trả lời đúng intent tìm gia sư/lớp
- [ ] Navigate to `/ai-assistant` full page
- [ ] Ask complex query about physics tutor + policy
- [ ] Show rich cards (tutors, FAQ)
- [ ] Highlight: Metadata bar (Confidence Score, Level, Answer Mode, Nguồn RAG)
- [ ] Demo câu hỏi ngoài phạm vi hoặc câu phổ thông ("1 + 1 bằng mấy?")
- [ ] Highlight: AI fallback đúng, không ép RAG sai ngữ cảnh
- [ ] Demo câu hỏi thiếu thông tin (hiển thị banner cảnh báo Confidence LOW)
- [ ] Demo session history & delete session

### 3. User Support Tickets (3 min)
- [ ] Login as student
- [ ] Go to `/messaging` > Support Tickets tab
- [ ] Show ticket list with status/priority
- [ ] Create new ticket: BUG_REPORT, VNPay error
- [ ] Show auto-assigned priority (HIGH)
- [ ] View ticket detail
- [ ] Reply to ticket
- [ ] Demo reopen (if closed ticket available)

### 4. Admin Manage Tickets (4 min)
- [ ] Login as admin
- [ ] Go to `/platform/tickets`
- [ ] Filter: Status=OPEN, Priority=HIGH
- [ ] Show SLA breach indicators
- [ ] Open ticket detail (auto-assigns admin)
- [ ] Update priority: HIGH → URGENT
- [ ] Respond to ticket
- [ ] Close ticket with resolution note

### 5. Admin Manage FAQ (3 min)
- [ ] Go to `/platform/faq`
- [ ] Show FAQ list
- [ ] Create new FAQ about withdrawals
- [ ] Edit existing FAQ
- [ ] Toggle published status
- [ ] Delete FAQ

---

## BF-10: Platform Administration Demo Flow

### 6. Admin Dashboard (Activity Timeline) (3 min)
- [ ] Go to `/platform/dashboard`
- [ ] Show KPI cards (users, classes, tickets, verifications, active users)
- [ ] Show alerts and task queue sections
- [ ] Highlight: Bảng Hoạt động (Activity Timeline) với dữ liệu growth realtime
- [ ] Demo bộ lọc thời gian: chọn từ ngày, đến ngày, mức độ hiển thị (Ngày/Tuần)
- [ ] Highlight: Biểu đồ/bảng cập nhật động theo filter

### 7. Platform Fees Config (2 min)
- [ ] Go to `/platform/parameters`
- [ ] Search: "PLATFORM_FEE_RATE"
- [ ] Show current: 0.10 (10%)
- [ ] Edit to 0.12 (12%)
- [ ] Explain impact on revenue calculation

### 8. Audit Logs (3 min)
- [ ] Go to `/platform/audit-logs`
- [ ] Show log list with filters
- [ ] Filter by: Action=UPDATE, Entity=FAQ
- [ ] Open log detail
- [ ] Show JSON old/new values
- [ ] Highlight diff viewer
- [ ] Show another example: SystemParameter change

### 9. Financial Reports & Analytics (4 min)
- [ ] Go to `/platform/analytics`
- [ ] Show summary metrics (6-month):
  - Total revenue
  - Platform fee revenue
  - User growth
  - Conversion rates
- [ ] Highlight: Dòng tiền IN (Nạp/Phí) và OUT (Rút/Hoàn) được phân chia rõ ràng
- [ ] Show: Bảng phân loại giao dịch Transaction Breakdown (hiển thị +/- rõ ràng)
- [ ] Demo Export CSV Users & Classes
- [ ] Demo Export CSV Cashflow & Transaction Breakdown (tính năng xuất file với Date Range)
- [ ] Open CSV files to show exported data khớp với backend

---

## Key Talking Points

### BF-09 Highlights
✅ **AI Trust & RAG**: Intent classification, chấm điểm Confidence, Fallback fallback thông minh  
✅ **Hỗ trợ minh bạch**: Hiển thị metadata bar và cảnh báo cho người dùng  
✅ **Support Tickets**: Category-based priority, SLA tracking  
✅ **Admin Queue**: Auto-assignment, SLA breach monitoring  
✅ **FAQ Management**: Published flag, audit logging

### BF-10 Highlights
✅ **Activity Timeline**: Filter linh hoạt theo thời gian, tính toán realtime biến động  
✅ **Financial Flow**: Phân loại tiền IN/OUT minh bạch, Transaction Breakdown rõ ràng  
✅ **CSV Export**: Xuất dữ liệu cashflow với bộ lọc ngày chính xác  
✅ **System Parameters**: Platform fee configuration  
✅ **Audit Trail**: Full change history với JSON diff

---

## Troubleshooting

### If AI Chatbot Fails
- Check Groq API key in `application.properties`
- Falls back to Gemini automatically
- Check backend logs for API errors

### If Tickets Tab Not Showing
- Navigate to `/messaging` first
- Manually click "Support Tickets" tab
- Known routing issue (prop not wired)

### If Category Management Missing
- Demo via Swagger UI: `http://localhost:8080/swagger-ui.html`
- Or skip this section, focus on Parameters

---

## Time Estimates

| Section | Time |
|---------|------|
| BF-09: Browse FAQ | 2 min |
| BF-09: AI Chatbot | 3 min |
| BF-09: User Tickets | 3 min |
| BF-09: Admin Tickets | 4 min |
| BF-09: Admin FAQ | 3 min |
| **BF-09 Subtotal** | **15 min** |
| BF-10: Dashboard | 2 min |
| BF-10: Platform Fees | 2 min |
| BF-10: Audit Logs | 3 min |
| BF-10: Financial Reports | 4 min |
| **BF-10 Subtotal** | **11 min** |
| **Total** | **~26 min** |

Add 3-4 min for intro/outro/transitions = **30 min total**

---

## Recording Settings

- Resolution: 1920x1080 (Full HD)
- Browser zoom: 100-110%
- Frame rate: 30fps
- Audio: Clear microphone, no background noise
- Screen: Close unnecessary apps/notifications

---

## Post-Production

- [ ] Trim dead air at start/end
- [ ] Add section timestamps in description
- [ ] Add captions (optional)
- [ ] Add chapter markers (YouTube)

---

Good luck! 🎬
