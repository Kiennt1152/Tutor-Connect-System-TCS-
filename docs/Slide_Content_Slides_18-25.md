# NỘI DUNG TRÌNH BÀY SLIDE 18-25
## Tutor Connect System (TCS) - Final Presentation

---

## SLIDE 18: Đặc Tả Trạng Thái Thực Thể

### 1. Trạng thái User (Tài Khoản Người Dùng)

| Trạng thái | Mô tả | Điều kiện chuyển đổi |
|------------|-------|---------------------|
| **Active** | Tài khoản hoạt động bình thường | → Locked: Khi đăng nhập sai 5 lần trong 10 phút |
| | | → Inactive: Khi admin vô hiệu hóa hoặc user không hoạt động > 6 tháng |
| **Locked** | Tài khoản bị khóa tạm thời | → Active: Khi user reset password hoặc admin unlock |
| | | → Inactive: Khi khóa kéo dài > 30 ngày |
| **Inactive** | Tài khoản không hoạt động | → Active: Khi user đăng nhập lại và xác thực |

### 2. Trạng thái Job Posting (Bài đăng tuyển dụng)

| Trạng thái | Mô tả | Điều kiện chuyển đổi |
|------------|-------|---------------------|
| **Draft** | Bản nháp, chưa công khai | → Active: Khi tutor center publish |
| **Active** | Đang tuyển dụng | → Closed: Khi đủ ứng viên hoặc hết hạn |
| | | → Draft: Khi center tạm dừng tuyển |
| **Closed** | Đã đóng | → (Không chuyển ngược - tuân thủ GB-01) |

### 3. Sơ đồ State Machine đơn giản

```
User Account State Flow:
┌─────────┐    fail 5x/10min    ┌─────────┐    admin disable    ┌───────────┐
│  ACTIVE │──────────────────→│  LOCKED │───────────────────→│ INACTIVE  │
└─────────┘                    └─────────┘                     └───────────┘
     ↑                               │                                │
     └──────────── unlock ───────────┴─────── re-login ─────────────────┘


Job Posting State Flow:
┌─────────┐    publish    ┌─────────┐    filled/expired    ┌─────────┐
│  DRAFT  │────────────→│  ACTIVE │───────────────────→│ CLOSED  │
└─────────┘              └─────────┘                     └─────────┘
     ↑                         │
     └───── unpublish ─────────┘
     (One-way: CLOSED → DRAFT/ACTIVE KHÔNG ĐƯỢC)
```

---

## SLIDE 19: Quy Tắc Nghiệp Vụ 1 - Data & Workflow Rules

### GB-01: Quy tắc chuyển trạng thái một chiều (Irreversible State Transition)

**Mô tả:** Một số trạng thái trong hệ thống chỉ có thể chuyển đổi theo một hướng nhất định, không thể quay ngược lại.

**Áp dụng cho:**

| Thực thể | Trạng thái không thể quay ngược |
|----------|--------------------------------|
| Job Posting | Closed → Active/Draft |
| Escrow Transaction | Released → Deposited |
| Class | Completed → Active |
| Contract | Signed → Pending |
| Lead | Qualified/Unqualified → New |
| Withdrawal Request | Approved → Pending |

**Ví dụ minh họa:**
```
Class Status Flow (KHÔNG THỂ quay ngược):
┌──────────┐   assign tutor   ┌───────────┐   start   ┌────────────┐   complete   ┌───────────┐
│ CREATED  │───────────────→│  MATCHED  │─────────→│ IN_PROGRESS│───────────→│ COMPLETED │
└──────────┘                └───────────┘           └────────────┘             └───────────┘
                                                        │
                                                        │ dispute → CANCELLED (chỉ tiến, không quay về IN_PROGRESS)
                                                        ↓
                                                   ┌───────────┐
                                                   │ CANCELLED │
                                                   └───────────┘
```

### GB-04: Quy tắc tính duy nhất của dữ liệu (Uniqueness Constraint)

**Mô tả:** Một số trường dữ liệu phải đảm bảo tính duy nhất trên toàn hệ thống.

| Trường | Ràng buộc | Xử lý trùng lặp |
|--------|-----------|-----------------|
| Email | UNIQUE, NOT NULL | Reject + thông báo "Email đã được đăng ký" |
| Phone | UNIQUE, NOT NULL | Reject + gợi ý đăng nhập |
| Tutor License Number | UNIQUE (per type) | Reject + thông báo "Giấy phép đã tồn tại" |
| Center License | UNIQUE, NOT NULL | Reject + yêu cầu liên hệ support |
| Class Code | UNIQUE auto-generated | Hệ thống tự tạo, không trùng |
| Transaction ID | UNIQUE (UUID) | Hệ thống tự tạo |

**Implementation:**
```java
// Backend validation example
@UniqueConstraint(columnNames = {"email"})
private String email;

// Service layer check
if (userRepository.existsByEmail(email)) {
    throw new BusinessException("EMAIL_EXISTS");
}
```

---

## SLIDE 20: Quy Tắc Nghiệp Vụ 2 - Security & Privacy Rules

### GB-03: Quy tắc bảo mật file CV (CV File Security)

**Mục tiêu:** Bảo vệ file CV của ứng viên khỏi truy cập trái phép

**Quy định:**
| Yêu cầu | Chi tiết |
|----------|----------|
| Lưu trữ | Mã hóa AES-256 trước khi lưu vào storage |
| Truy cập | Chỉ user sở hữu, admin, và employer được phép xem |
| Download | Chỉ cho phép khi có sự đồng ý của ứng viên |
| Chia sẻ | KHÔNG cho phép share link công khai |
| Xóa | Soft delete, backup trong 90 ngày trước khi xóa vĩnh viễn |

**Implementation:**
```
CV Access Control:
┌─────────────────────────────────────────────────────────┐
│                    CV FILE ACCESS                        │
├──────────────┬───────────────────────────────────────────┤
│   Actor      │           Permission                      │
├──────────────┼───────────────────────────────────────────┤
│ Tutor (owner)│ Read, Update, Delete own CV              │
│ Admin        │ Read all CVs (audit purpose only)        │
│ Employer     │ Read CV of applicants only               │
│ Guest        │ NO ACCESS                                │
└──────────────┴───────────────────────────────────────────┘
```

### GB-02 & GB-06: Ẩn ghi chú nội bộ (Hide Internal Notes from Candidates)

**Mục tiêu:** Bảo vệ thông tin nội bộ của recruiter/center

**Quy định:**
| Trường | Quyền xem | Người không được xem |
|--------|-----------|---------------------|
| Internal Notes | Admin, Recruiter, HR | Ứng viên |
| Recruiter Rating | Admin, Recruiter | Ứng viên |
| Salary Expectation (internal) | Admin, Recruiter | Ứng viên |
| Screening Score | Admin | Ứng viên |
| Lead Source Details | Admin, Sales | Ứng viên |

**Implementation:**
```json
// API Response Filtering Example
{
  "candidate": {
    "id": "123",
    "name": "Nguyen Van A",
    "email": "a@example.com",
    "publicProfile": { ... },
    // "internalNotes" -> NOT INCLUDED
    // "recruiterRating" -> NOT INCLUDED
  }
}

// Only visible in internal dashboard:
{
  "internalView": {
    "internalNotes": "Có kinh nghiệm 3 năm...",
    "recruiterRating": 4.5,
    "screeningScore": 85
  }
}
```

---

## SLIDE 21: Yêu Cầu Phi Chức Năng (Non-Functional Requirements)

### 1. Hiệu Năng (Performance)

| NFR ID | Yêu cầu | Ngưỡng đạt | Công cụ test |
|--------|---------|------------|--------------|
| NFR-P01 | Search tutors | < 3s (95% requests, 100 users) | K6 |
| NFR-P02 | Load profile page | < 2s (95% requests, 100 users) | Lighthouse |
| NFR-P03 | Booking confirmation | < 4s (95% requests, 100 users) | K6/JMeter |
| NFR-S01 | Peak load response | ≤ 3s (95% requests) | K6/JMeter |
| NFR-S03 | Query response | < 0.5s (100 users) | Sysbench |

### 2. Scalability (Khả năng mở rộng)

| Yêu cầu | Chi tiết |
|---------|----------|
| Auto-scaling | Tự động scale theo load thực tế |
| Cache hit | < 200ms cho cached requests |
| Data growth | 100 concurrent users, queries < 0.5s |
| Stateless API | Required cho auto-scaling |

### 3. Availability (Khả dụng)

| NFR ID | Yêu cầu | Ngưỡng |
|--------|---------|--------|
| NFR-A01 | System uptime | ≥ 99.5% (≤ 50 phút downtime/tuần) |
| NFR-A02 | Recovery time | ≤ 30 phút cho 90% incidents |
| NFR-A03 | Data backup | Daily, RPO 24h |
| NFR-A04 | Fault tolerance | ≥ 95/100 users vẫn browse được khi payment fails |

### 4. Security (Bảo mật)

| NFR ID | Yêu cầu | Chi tiết |
|--------|---------|----------|
| NFR-SEC01 | Password lockout | Lock sau 5 lần fail trong 10 phút |
| NFR-SEC02 | Role-based access | Authorization test scenarios |
| NFR-SEC03 | Password storage | bcrypt/Argon2 hash, no plaintext |
| NFR-SEC04 | JWT expiry | Access token 15 phút, refresh token rotatable |
| NFR-SEC05 | File upload | MIME whitelist, max size limit |

### 5. Usability (Khả dụng)

| Yêu cầu | Chi tiết |
|---------|----------|
| New user booking | < 5 phút cho 90% test users |
| Error messages | 97% hiển thị hướng dẫn ngay |
| Browser support | Chrome, Safari, Edge, Firefox |
| Vietnamese UI | 100% labels đúng tiếng Việt, không lỗi encoding |

---

## SLIDE 22: Rủi Ro Dự Án và Cách Giảm Thiểu (Phần 1)

### Risk Matrix

| ID | Rủi ro | Mức độ | Tác động | Cách giảm thiểu |
|----|--------|--------|----------|-----------------|
| R-01 | Facebook API thay đổi | Cao | Lead scraping không hoạt động | Backup plan: Manual import, webhook redundancy |
| R-02 | Data privacy violation | Cao | Legal risk, user trust | GDPR/PDP compliance review, encrypt all PII |
| R-03 | Payment gateway downtime | Cao | Transaction fails | Multiple payment providers, retry mechanism |
| R-04 | Scope creep | Trung bình | Delay timeline | Strict change control board |
| R-05 | Team member leave | Trung bình | Knowledge gap | Documentation, code review, pair programming |
| R-06 | Database performance | Trung bình | Slow queries | Index optimization, query audit |
| R-07 | Third-party service failure | Thấp | Partial functionality loss | Circuit breaker pattern, fallback UI |
| R-08 | Security breach attempt | Thấp | Data leak | OWASP top 10 compliance, penetration testing |

### Chi tiết các rủi ro chính:

**R-01: Facebook API Changes**
```
Threat: Facebook thay đổi API → Lead scraping dừng
Impact: Mất nguồn lead tự động
Mitigation:
  ✓ Fallback: Manual CSV import
  ✓ Webhook redundancy
  ✓ Monitor API changelog
  ✓ Quick adaptation sprint (2-3 days)
```

**R-03: Payment Gateway Downtime**
```
Threat: Payment provider offline → Users can't pay
Impact: Lost revenue, poor UX
Mitigation:
  ✓ Multi-provider: Stripe + PayOS backup
  ✓ Queue transactions for retry
  ✓ Clear user messaging
  ✓ SLA monitoring alerts
```

---

## SLIDE 23: Rủi Ro Dự Án và Cách Giảm Thiểu (Phần 2)

### Tiếp tục Risk Details:

**R-02: Data Privacy Violation**
```
Threat: Vi phạm PDP Law → Phạt, mất trust
Impact: Legal consequences, reputation damage
Mitigation:
  ✓ Encrypt all PII at rest and in transit
  ✓ Role-based data access
  ✓ Audit log for all data access
  ✓ Data retention policy (auto-delete old data)
  ✓ Privacy impact assessment completed
```

**R-04: Scope Creep**
```
Threat: Stakeholder yêu cầu thêm tính năng
Impact: Timeline delay, budget overrun
Mitigation:
  ✓ Change Control Board (CCB)
  ✓ Prioritization matrix (MoSCoW)
  ✓ v2.0 backlog for non-critical items
  ✓ Weekly scope review meetings
```

### Monitoring & Response Plan

| Trigger | Alert | Response |
|---------|-------|----------|
| API error > 5% | PagerDuty to Tech Lead | Rollback if needed |
| Response time > 5s | Grafana alert | Scale up instances |
| Payment fail rate > 2% | Opsgenie alert | Failover to backup |
| Security event | Immediate alert | Incident response team |

---

## SLIDE 24: Đóng Góp Dự Kiến (Expected Contributions)

### 1. Tác Động Vận Hành (Business Impact)

| Lĩnh vực | Trước TCS | Sau TCS | Cải thiện |
|----------|-----------|---------|-----------|
| **Lead management** | Manual Facebook scrape | Auto NLP extraction | Tiết kiệm 20h/tuần |
| **Tutor matching** | Manual screening | AI-powered matching | 60% faster |
| **Payment handling** | Bank transfer, risky | Escrow protected | 100% secure |
| **Center recruitment** | Word-of-mouth | B2B pipeline | 3x faster |
| **Dispute resolution** | 3-5 days | Automated workflow | < 24h |
| **Report generation** | Manual Excel | Real-time dashboard | 90% time saved |

### 2. Tác Động Người Dùng (UX Impact)

| User Type | Pain Point | Solution | Benefit |
|-----------|-----------|----------|---------|
| **Client** | Khó tìm gia sư uy tín | Verified tutor badges, reviews | Tìm gia sư nhanh hơn 70% |
| **Tutor** | Cạnh tranh không lành mạnh | Fair matching algorithm | Cơ hội công bằng |
| **Tutor Center** | Quản lý leads thủ công | CRM + auto-routing | Scale 10x |
| **Platform Admin** | Xử lý dispute phức tạp | Workflow automation | Focus on value-add tasks |

### 3. Metrics dự kiến (KPIs)

```
Expected KPIs sau 6 tháng launch:

📊 User Engagement:
   - DAU (Daily Active Users): 1,000+
   - Class booking rate: 40% improvement
   - User retention: 70% (từ 30%)

💰 Business Metrics:
   - GMV (Gross Merchandise Value): 500M VND/tháng
   - Escrow volume: 200+ transactions/tháng
   - Lead conversion rate: 25% (từ 8%)

⏱️ Operational Efficiency:
   - Avg. tutor response time: < 2h (từ 24h)
   - Dispute resolution: < 24h (từ 3-5 days)
   - Support ticket volume: -40%
```

---

## SLIDE 25: Bước Tiếp Theo Sau Khi Hoàn Thành (Next Steps)

### 1. Kế hoạch mở rộng v2.0

| Phase | Tính năng | Priority | Timeline |
|-------|-----------|----------|----------|
| **v2.1** | AI Tutor Matching v2 | High | Q1 2027 |
| **v2.2** | Auto-notification system | High | Q1 2027 |
| **v2.3** | Mobile app (iOS/Android) | Medium | Q2 2027 |
| **v2.4** | Video lesson integration | Medium | Q2 2027 |
| **v2.5** | Multi-language support | Low | Q3 2027 |

### 2. Chi tiết v2.1 - AI Tutor Matching v2

**Current:** Weight-based matching (subject, price, rating)
**Enhanced:** 
```
AI Matching v2 Features:
├─ Personality matching (MBTI-based)
├─ Learning style compatibility
├─ Schedule optimization AI
├─ Predictive success score
└─ Natural language search
```

### 3. Chi tiết v2.2 - Auto-Notification System

```
Notification Engine v2:
┌─────────────────────────────────────────┐
│           AUTOMATION RULES              │
├─────────────────────────────────────────┤
│ 📧 Email Notifications:                 │
│   - Class reminder (24h, 1h before)    │
│   - Payment due reminder               │
│   - New tutor recommendation            │
│   - Review request (after class)        │
├─────────────────────────────────────────┤
│ 📱 Push Notifications:                  │
│   - Real-time class updates             │
│   - Application status changes          │
│   - Message received                    │
├─────────────────────────────────────────┤
│ 💬 SMS (for critical):                  │
│   - Payment failed                      │
│   - Account locked                     │
│   - Emergency cancellation              │
└─────────────────────────────────────────┘
```

### 4. Technical Roadmap

```
2026                          2027
Q2        Q3        Q4        Q1        Q2        Q3
│         │         │         │         │         │
├─ v1.0 ──┤         │         │         │         │
│ Launch  │         │         │         │         │
│         ├─ v1.1 ──┤         │         │         │
│         │ Bug fix │         │         │         │
│         │ Perf    │         │         │         │
│         │         ├─ v1.2 ──┤         │         │
│         │         │ Stability│         │         │
│         │         │ Scale   │         │         │
│         │         │         ├─ v2.0 ──┤         │
│         │         │         │ AI v2   │         │
│         │         │         │ Mobile  │         │
│         │         │         │         ├─ v2.1 ──┤
│         │         │         │         │ Video  │
│         │         │         │         │ Multi-lang
└─────────┴─────────┴─────────┴─────────┴─────────┴────────
```

### 5. Hỗ trợ và Maintenance

| Hoạt động | Tần suất | Người phụ trách |
|-----------|----------|-----------------|
| Security patches | As needed | DevOps |
| Bug fixes | Weekly sprint | Development team |
| Performance optimization | Monthly | Tech Lead |
| Feature updates | Quarterly | Product + Dev |
| Security audit | Bi-annual | External auditor |

---

## TỔNG KẾT

| Slide | Chủ đề chính |
|-------|-------------|
| 18 | Entity State Specification - 3 trạng thái User, 3 trạng thái Job Posting |
| 19 | Business Rules GB-01, GB-04 - One-way transitions, Uniqueness |
| 20 | Business Rules GB-02, GB-03, GB-06 - Security & Privacy |
| 21 | NFRs - Performance, Scalability, Security, Usability |
| 22-23 | Risks & Mitigations - 8 rủi ro chính, response plan |
| 24 | Expected Contributions - Business & UX impact, KPIs |
| 25 | Next Steps - v2.0 roadmap, AI & automation features |

---

*Lưu ý: Nội dung dựa trên các báo cáo đã có (VS, Project Plan, PRD, UCS, Project Tracking)*
*File created: $(Get-Date -Format "yyyy-MM-dd")*
