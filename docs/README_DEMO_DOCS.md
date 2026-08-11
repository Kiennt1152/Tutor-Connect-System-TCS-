# Demo Documentation Index - BF-09 & BF-10

## 📚 Tổng Quan

Bộ tài liệu hướng dẫn demo đầy đủ cho **BF-09: Customer Support** và **BF-10: Platform Administration** trong hệ thống Tutor Connect System.

---

## 📄 Danh Sách Tài Liệu

### 1. [DEMO_GUIDE_BF09_BF10.md](./DEMO_GUIDE_BF09_BF10.md) (19.5 KB)
**Tài liệu chính - Hướng dẫn chi tiết đầy đủ**

Nội dung:
- Chuẩn bị môi trường (Backend, Frontend, Database, Accounts)
- Script demo chi tiết cho từng Use Case:
  - UC-61: Browse FAQ
  - UC-65: AI Chatbot (Floating Widget + Full Page)
  - UC-53: Request Support (User Tickets)
  - UC-63: Manage Support Requests (Admin)
  - UC-67: Manage FAQ Knowledge Base
  - UC-56: Admin Dashboard
  - UC-57: Manage Categories
  - UC-46: Configure Platform Fees
  - UC-61: Monitor Audit Logs
  - UC-47: View Financial Reports
- Demo timeline (30 phút)
- Tips cho video recording
- Known issues & workarounds
- Test data seed scripts

**Khi nào dùng**: Đọc trước khi quay video để hiểu đầy đủ flow và chi tiết

---

### 2. [DEMO_CHECKLIST_BF09_BF10.md](./DEMO_CHECKLIST_BF09_BF10.md) (4.8 KB)
**Checklist nhanh - Dạng checkbox**

Nội dung:
- Pre-recording setup checklist
- Step-by-step checklist cho mỗi feature
- Time estimates per section
- Key talking points
- Troubleshooting quick tips
- Post-production checklist

**Khi nào dùng**: In ra hoặc mở trên màn hình phụ để tick off khi demo

---

### 3. [DEMO_SCRIPT_BF09_BF10.md](./DEMO_SCRIPT_BF09_BF10.md) (18.9 KB)
**Kịch bản từng câu từng chữ**

Nội dung:
- Intro script (30s)
- Script chi tiết cho từng scene với timing chính xác
- Câu thoại mẫu (tiếng Việt)
- Hướng dẫn cách nói, cách highlight UI
- Outro script
- Timing breakdown table
- Recording tips (pace, pointing, tone, recovery)

**Khi nào dùng**: Đọc theo script khi quay video để đảm bảo không bỏ sót điểm quan trọng

---

### 4. [DEMO_TEST_SCENARIOS_BF09_BF10.md](./DEMO_TEST_SCENARIOS_BF09_BF10.md) (23.0 KB)
**Test scenarios đầy đủ với SQL**

Nội dung:
- Pre-demo testing checklist
- Test scenarios chi tiết cho mỗi Use Case:
  - Pre-conditions
  - Steps
  - Expected results
  - Backend verification SQL
- Common issues & fixes
- Seed data scripts
- Final checklist before recording

**Khi nào dùng**: Chạy test trước khi quay video để đảm bảo mọi thứ hoạt động tốt

---

### 5. [DEMO_QUICK_REFERENCE_BF09_BF10.md](./DEMO_QUICK_REFERENCE_BF09_BF10.md) (6.3 KB)
**Reference card 1 trang**

Nội dung:
- Setup commands
- Demo flow table (URLs, duration, key points)
- Checklist nhanh
- Quick commands (curl, SQL)
- Known issues table
- Success criteria

**Khi nào dùng**: Giữ bên cạnh khi quay video để tham khảo nhanh URLs, timing, key points

---

## 🎯 Cách Sử Dụng Bộ Tài Liệu

### Giai Đoạn 1: Chuẩn Bị (Trước 1-2 ngày)
1. Đọc **DEMO_GUIDE_BF09_BF10.md** để hiểu tổng quan
2. Chạy test theo **DEMO_TEST_SCENARIOS_BF09_BF10.md**
3. Seed data nếu thiếu (SQL scripts trong test scenarios)
4. Fix issues nếu có

### Giai Đoạn 2: Luyện Tập (Trước vài giờ)
1. Đọc **DEMO_SCRIPT_BF09_BF10.md** để quen câu thoại
2. Chạy dry-run theo **DEMO_CHECKLIST_BF09_BF10.md**
3. Time mỗi section để đảm bảo fit 30 phút
4. Luyện chuyển cảnh giữa các feature

### Giai Đoạn 3: Quay Video
1. Setup theo checklist trong **DEMO_QUICK_REFERENCE_BF09_BF10.md**
2. Mở **DEMO_SCRIPT_BF09_BF10.md** trên màn hình phụ hoặc in ra
3. Mở **DEMO_QUICK_REFERENCE_BF09_BF10.md** để tham khảo URLs và key points
4. Quay theo script, tick off **DEMO_CHECKLIST_BF09_BF10.md**

### Giai Đoạn 4: Post-Production
1. Review video
2. Trim intro/outro
3. Add timestamps theo timing breakdown trong **DEMO_SCRIPT_BF09_BF10.md**
4. Add captions (optional)

---

## 📊 So Sánh Tài Liệu

| Tài liệu | Kích thước | Độ chi tiết | Dùng khi nào |
|----------|------------|-------------|--------------|
| DEMO_GUIDE | 19.5 KB | ⭐⭐⭐⭐⭐ | Đọc trước, hiểu đầy đủ |
| DEMO_CHECKLIST | 4.8 KB | ⭐⭐⭐ | In ra, tick off khi demo |
| DEMO_SCRIPT | 18.9 KB | ⭐⭐⭐⭐⭐ | Đọc theo khi quay video |
| DEMO_TEST_SCENARIOS | 23.0 KB | ⭐⭐⭐⭐⭐ | Test trước khi quay |
| DEMO_QUICK_REFERENCE | 6.3 KB | ⭐⭐ | Tham khảo nhanh khi quay |

---

## 🎬 Demo Flow Summary

**Total Duration**: ~30 minutes

### Part 1: BF-09 Customer Support (15 min)
1. Browse FAQ (2 min) - `/help`
2. AI Chatbot (3 min) - Widget + `/ai-assistant`
3. User Tickets (3 min) - `/messaging`
4. Admin Tickets (4 min) - `/platform/tickets`
5. Admin FAQ (3 min) - `/platform/faq`

### Part 2: BF-10 Platform Administration (11 min)
6. Dashboard (2 min) - `/platform/dashboard`
7. Platform Fees (2 min) - `/platform/parameters`
8. Audit Logs (3 min) - `/platform/audit-logs`
9. Financial Reports (4 min) - `/platform/analytics`

### Intro + Outro (4 min buffer)

---

## ✅ Quick Start

**Nếu bạn vội, chỉ cần 3 bước:**

1. **Test**: Chạy test theo `DEMO_TEST_SCENARIOS_BF09_BF10.md` → Section "Pre-Demo Testing Checklist"
2. **Setup**: Làm theo `DEMO_QUICK_REFERENCE_BF09_BF10.md` → Section "Setup" và "Final Pre-Recording Checklist"
3. **Record**: Đọc theo `DEMO_SCRIPT_BF09_BF10.md` + tham khảo `DEMO_QUICK_REFERENCE_BF09_BF10.md` bên cạnh

**Nếu bạn có thời gian:**
- Đọc hết `DEMO_GUIDE_BF09_BF10.md` trước để hiểu sâu
- Luyện theo `DEMO_CHECKLIST_BF09_BF10.md` vài lần
- Rồi mới quay video

---

## 🚀 Implementation Status

### BF-09: Customer Support
| Feature | Backend | Frontend | Status |
|---------|---------|----------|--------|
| Browse FAQ | ✅ | ✅ | Ready |
| AI Chatbot (RAG) | ✅ | ✅ | Ready (check API keys) |
| User Support Tickets | ✅ | ⚠️ | Ready (routing issue noted) |
| Admin Ticket Management | ✅ | ✅ | Ready |
| Admin FAQ Management | ✅ | ✅ | Ready |

### BF-10: Platform Administration
| Feature | Backend | Frontend | Status |
|---------|---------|----------|--------|
| Admin Dashboard | ✅ | ✅ | Ready |
| Manage Categories | ✅ | ⚠️ | Ready (may need Swagger) |
| Configure Platform Fees | ✅ | ✅ | Ready |
| Monitor Audit Logs | ✅ | ✅ | Ready |
| Financial Reports | ✅ | ✅ | Ready |

**Legend**: ✅ Fully implemented | ⚠️ Minor issue/workaround needed | ❌ Not implemented

---

## 📞 Support

**Nếu gặp vấn đề trong quá trình demo:**

1. Xem section "Known Issues & Workarounds" trong `DEMO_GUIDE_BF09_BF10.md`
2. Xem section "Troubleshooting" trong `DEMO_TEST_SCENARIOS_BF09_BF10.md`
3. Check browser console (F12) và backend logs
4. Nếu vẫn stuck, skip feature đó và note lại để fix sau

---

## 📝 Notes

- Tất cả tài liệu đều bằng tiếng Việt (trừ code/SQL)
- Screenshots/recordings không có trong docs (cần quay live)
- SQL scripts đều tested với PostgreSQL
- API keys (Groq, Gemini) cần valid để AI chatbot hoạt động

---

**Chúc bạn quay video thành công! 🎥✨**

---

## 📅 Version History

- **v1.0** (2026-08-09): Initial release - Full demo documentation for BF-09 & BF-10
