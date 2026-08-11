# BF-09 & BF-10 Demo - Quick Reference Card

## 🎯 30-Minute Video Demo Guide

---

## 📝 Setup (Before Recording)

```bash
# Start services
cd backend && mvn spring-boot:run    # Port 8080
cd frontend && npm run dev           # Port 5173
```

**Logins**: student@test.com / admin@test.com (password: `password`)

---

## 🎬 Demo Flow

### Part 1: BF-09 Customer Support (15 min)

| # | Feature | URL | Duration | Key Points |
|---|---------|-----|----------|------------|
| 1 | Browse FAQ | `/help` | 2 min | Public access, search, categories |
| 2 | AI Chatbot | Any page → `/ai-assistant` | 3 min | RAG, tutor/FAQ cards, sessions |
| 3 | User Tickets | `/messaging` | 3 min | Create BUG_REPORT, auto-priority, reply |
| 4 | Admin Tickets | `/platform/tickets` | 4 min | Queue, auto-assign, SLA, respond, close |
| 5 | Admin FAQ | `/platform/faq` | 3 min | CRUD, publish toggle, audit log |

### Part 2: BF-10 Platform Administration (11 min)

| # | Feature | URL | Duration | Key Points |
|---|---------|-----|----------|------------|
| 6 | Dashboard | `/platform/dashboard` | 2 min | KPIs, alerts, real-time |
| 7 | Platform Fees | `/platform/parameters` | 2 min | Edit PLATFORM_FEE_RATE (0.10→0.12) |
| 8 | Audit Logs | `/platform/audit-logs` | 3 min | Filters, JSON diff, accountability |
| 9 | Financial Reports | `/platform/analytics` | 4 min | 6-month metrics, CSV exports |

---

## 🗣️ Key Talking Points

### BF-09 Highlights
- **RAG AI**: Retrieves real tutors, classes, FAQ from DB
- **Auto-priority**: BUG_REPORT→HIGH, SYSTEM_ERROR→URGENT
- **SLA tracking**: LOW=48h, MEDIUM=24h, HIGH=12h, URGENT=4h
- **Audit everything**: All admin actions logged

### BF-10 Highlights
- **Config-driven fees**: PLATFORM_FEE_RATE parameter controls revenue calc
- **Comprehensive audit**: JSON old/new values with diff viewer
- **Business metrics**: Conversion, dispute, completion rates
- **Export to CSV**: Users, classes, revenue for analysis

---

## 📋 Demo Sequence Checklist

### BF-09: Customer Support
- [ ] Show FAQ public page, search "hoàn tiền"
- [ ] Click floating chatbot, ask about finding tutors
- [ ] Navigate to full AI page, complex query with tutor/FAQ cards
- [ ] Login as student, create ticket (BUG_REPORT: VNPay error)
- [ ] Show auto-priority HIGH, due date 12h
- [ ] Reply to ticket
- [ ] Switch to admin, filter tickets (OPEN, HIGH)
- [ ] Open ticket → auto-assign, IN_PROGRESS
- [ ] Update priority HIGH→URGENT
- [ ] Respond to user
- [ ] Close ticket with resolution note
- [ ] Admin FAQ: create, edit, unpublish, delete

### BF-10: Platform Administration
- [ ] Show dashboard KPIs (users, classes, tickets, verifications)
- [ ] Navigate to system parameters
- [ ] Edit PLATFORM_FEE_RATE: 0.10 → 0.12
- [ ] Show audit logs, filter by UPDATE + SystemParameter
- [ ] Open audit entry, show JSON old/new diff
- [ ] Navigate to analytics
- [ ] Show 6-month summary (revenue, users, classes, rates)
- [ ] Export Users CSV, open file
- [ ] Export Classes CSV, open file
- [ ] Export Revenue CSV, show platform fee calculation

---

## ⚡ Quick Commands

### Check Services
```bash
curl http://localhost:8080/actuator/health  # Backend
curl http://localhost:5173                  # Frontend
```

### Seed Demo Data
```sql
-- Quick verify
SELECT COUNT(*) FROM faq_entry WHERE published = true;  -- Should be ≥5
SELECT COUNT(*) FROM support_ticket WHERE status = 'OPEN';  -- Should be ≥1
SELECT param_value FROM system_parameter WHERE param_key = 'PLATFORM_FEE_RATE';  -- Should exist
```

### Browser DevTools
- F12 → Console (check for errors)
- F12 → Network (check API calls)
- Ctrl+Shift+R (hard refresh if needed)

---

## 🎥 Recording Tips

### Pacing
- Speak **medium pace**, pause 1-2s after each action
- Use phrases: "Các bạn thấy đây...", "Điểm đặc biệt là..."
- Highlight with mouse cursor

### Recovery
- Error? → "Okay, chúng ta retry..."
- Slow? → "Đang load data từ server..."
- Forgot? → "Quay lại một chút..."

### Quality
- Resolution: 1920x1080
- Browser zoom: 100-110%
- Close unnecessary apps
- Disable notifications
- Test microphone first

---

## 🚨 Known Issues & Workarounds

| Issue | Workaround |
|-------|------------|
| AI chatbot fails | Check Groq API key, falls back to Gemini |
| `/messaging/tickets` wrong view | Navigate to `/messaging`, click "Support Tickets" tab |
| CSV export empty | Need seed data for 6-month window |
| SLA not marking breach | Verify `@EnableScheduling` on app class |
| Category management no UI | Demo via Swagger at `/swagger-ui.html` |

---

## 📊 Success Criteria

- [ ] All 9 features demoed successfully
- [ ] No critical errors shown on screen
- [ ] Demo flows naturally without long pauses
- [ ] Total time: 26-30 minutes
- [ ] Clear audio throughout
- [ ] Key features highlighted verbally

---

## 📁 Documentation Files Created

1. **DEMO_GUIDE_BF09_BF10.md** - Full detailed guide with all scenarios
2. **DEMO_CHECKLIST_BF09_BF10.md** - Quick checklist format
3. **DEMO_SCRIPT_BF09_BF10.md** - Word-by-word script with timing
4. **DEMO_TEST_SCENARIOS_BF09_BF10.md** - Test scenarios with SQL
5. **DEMO_QUICK_REFERENCE_BF09_BF10.md** - This card (1-page summary)

---

## 🎬 Final Pre-Recording Checklist

**Environment**
- [ ] Backend running (8080)
- [ ] Frontend running (5173)
- [ ] Both services healthy

**Data**
- [ ] ≥5 published FAQs
- [ ] ≥2 open tickets (1 with SLA breach)
- [ ] ≥3 active tutors
- [ ] PLATFORM_FEE_RATE parameter exists
- [ ] Recent audit logs available

**Accounts**
- [ ] student@test.com logged in (Browser 1)
- [ ] admin@test.com logged in (Browser 2)

**Recording**
- [ ] Screen recorder ready
- [ ] Microphone tested
- [ ] Browser clean (no extra tabs)
- [ ] Notifications disabled
- [ ] Script/notes beside monitor

---

**You're ready! Good luck! 🚀🎥**

---

## 📞 Quick Support

**If stuck during demo:**
1. Check browser console (F12)
2. Check backend logs (terminal)
3. Hard refresh (Ctrl+Shift+R)
4. Skip problematic feature, move to next
5. Can always do a second take! 😊
