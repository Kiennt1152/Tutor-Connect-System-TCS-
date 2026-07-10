# TCS Slides 18-25 - Figma-ready Content

## Design System
- Primary Color: #1B2D5C (Navy Blue)
- Secondary: #2D4A8A (Medium Blue)
- Accent: #4A90D9 (Light Blue)
- Background: #FFFFFF
- Text: #333333
- Muted Text: #666666
- Card Background: #F5F7FA
- Success: #28A745
- Warning: #FFC107
- Danger: #DC3545

## Slide Dimensions
- Size: 1920x1080 (16:9)
- Margin: 60px
- Title: 48pt Bold Uppercase
- Subtitle: 20pt
- Body: 16pt

---

## SLIDE 18: ENTITY STATE SPECIFICATION

### Layout Structure
```
┌──────────────────────────────────────────────────────────────────┐
│ [TCS Logo]                                    • • •             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ENTITY STATE SPECIFICATION                                       │
│  Status transition definitions for core entities                   │
│                                                                  │
│  ┌─────────────────────────┐    ┌─────────────────────────────┐ │
│  │ 👤 USER ACCOUNT STATUS │    │      USER STATUS FLOW        │ │
│  │                         │    │                             │ │
│  │ ● Active                │    │  ┌───────┐    ┌───────┐    │ │
│  │   - Normal operation   │    │  │ACTIVE │───→│LOCKED │    │ │
│  │   - Can login, transact │    │  └──┬────┘    └──┬────┘    │ │
│  │                         │    │     │            │          │ │
│  │ ● Locked               │    │     │ 5 fails     │ Admin    │ │
│  │   - Temp blocked       │    │     │             │ disable  │ │
│  │   - 5 failed attempts  │    │     │            ▼          │ │
│  │                         │    │     │     ┌───────────┐       │ │
│  │ ● Inactive             │    │     │     │ INACTIVE  │       │ │
│  │   - Disabled/expired   │    │     │     └───────────┘       │ │
│  └─────────────────────────┘    │                             │ │
│                                  │    JOB POSTING FLOW         │ │
│  ┌─────────────────────────┐    │                             │ │
│  │ 💼 JOB POSTING STATUS   │    │  ┌───────┐    ┌───────┐    │ │
│  │                         │    │  │ DRAFT │───→│ACTIVE │    │ │
│  │ ● Draft                │    │  └───────┘    └──┬────┘    │ │
│  │   - Work in progress   │    │                │          │ │
│  │   - Not public         │    │    publish      │ Filled/  │ │
│  │                         │    │                │ Expired  │ │
│  │ ● Active               │    │                ▼          │ │
│  │   - Live recruitment   │    │           ┌────────┐      │ │
│  │   - Visible to tutors  │    │           │ CLOSED │      │ │
│  │                         │    │           └────────┘      │ │
│  │ ● Closed              │    │           (Cannot revert) │ │
│  │   - Position filled   │    └─────────────────────────────┘ │
│  │   - Cannot reopen      │                                     │
│  └─────────────────────────┘                                     │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ████████████████████████████████████████████████████████████████ │
└──────────────────────────────────────────────────────────────────┘
```

### Visual Elements
- **Header Bar**: Navy gradient strip, 4px height at top
- **Logo**: TCS text logo, top-left
- **Progress Dots**: 3 dots (◐ ◑ ◒), top-right
- **Title**: Navy, 48pt, uppercase, bold, left-aligned
- **Subtitle**: Gray, 20pt, below title
- **Two-column layout**: Left = status list, Right = state flow diagrams
- **State Pills**: Rounded rectangles (borderRadius: 16px), navy fill, white text
- **Arrows**: Simple lines with arrowheads, #2D4A8A
- **Description text**: 14pt, gray, below each pill
- **Footer**: Navy gradient bar, 80px height

---

## SLIDE 19: BUSINESS RULES 1 - DATA & WORKFLOW

### Layout Structure
```
┌──────────────────────────────────────────────────────────────────┐
│ [TCS Logo]                                    • • •             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  BUSINESS RULES 1                                                │
│  Data & Workflow Rules                                           │
│                                                                  │
│  ┌───────────────────────────┐   ┌────────────────────────────┐  │
│  │ [GB-01]                  │   │ [GB-04]                    │  │
│  │                           │   │                            │  │
│  │ Irreversible State       │   │ Data Uniqueness            │  │
│  │ Transition               │   │ Constraint                  │  │
│  │                           │   │                            │  │
│  │ Some status changes can  │   │ Unique fields ensure data   │  │
│  │ only proceed forward,    │   │ integrity across system    │  │
│  │ never backward           │   │                            │  │
│  │                           │   │ ┌──────────────────────┐   │  │
│  │ Class Flow (Example):    │   │ │ ✓ Email (UNIQUE)     │   │  │
│  │                           │   │ │ ✓ Phone (UNIQUE)     │   │  │
│  │ ┌───────┐ ┌────────┐    │   │ │ ✓ License Number     │   │  │
│  │ │CREATED│→│MATCHED │    │   │ │ ✓ Class Code (auto)  │   │  │
│  │ └───────┘ └───┬────┘    │   │ │ ✓ Transaction ID    │   │  │
│  │               ↓          │   │ └──────────────────────┘   │  │
│  │         ┌──────────┐    │   │                            │  │
│  │         │IN_PROGRESS│   │   │ Duplicate handling:         │  │
│  │         └──┬───────┘    │   │ "Email already registered"  │  │
│  │            ↓            │   │ "Phone already exists"      │  │
│  │       ┌───────────┐    │   │                            │  │
│  │       │ COMPLETED │    │   │                            │  │
│  │       └───────────┘    │   │                            │  │
│  │           ✗            │   │                            │  │
│  │    Cannot go back      │   │                            │  │
│  └───────────────────────────┘   └────────────────────────────┘  │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ████████████████████████████████████████████████████████████████ │
└──────────────────────────────────────────────────────────────────┘
```

### Visual Elements
- **Cards**: White background, subtle shadow, 24px padding, 12px borderRadius
- **Badge Labels**: Navy pill badge (GB-01, GB-04) at top-left of card
- **Card Titles**: 24pt navy bold
- **State Pills**: Navy, 14pt white text, connected by arrows
- **X Mark**: Red ✗ symbol for "cannot reverse"
- **Checkmarks**: Success green (#28A745) for unique field list
- **List Items**: Icon + text format

---

## SLIDE 20: BUSINESS RULES 2 - SECURITY & PRIVACY

### Layout Structure
```
┌──────────────────────────────────────────────────────────────────┐
│ [TCS Logo]                                    • • •             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  BUSINESS RULES 2                                                │
│  Security & Privacy Rules                                         │
│                                                                  │
│  ┌───────────────────────────┐   ┌────────────────────────────┐  │
│  │ [GB-03]                  │   │ [GB-02] [GB-06]           │  │
│  │                           │   │                            │  │
│  │ CV File Security          │   │ Hide Internal Notes        │  │
│  │                           │   │                            │  │
│  │ 🔒 AES-256 Encryption    │   │ Internal notes and ratings │  │
│  │    for all uploaded CVs  │   │ are hidden from candidates │  │
│  │                           │   │                            │  │
│  │ Access Control:          │   │ ┌────────────────────────┐ │  │
│  │ ┌────────────────────┐   │   │ │ INTERNAL ONLY          │ │  │
│  │ │ 🔒 Tutor (owner)   │   │   │ │ ────────────────────── │ │  │
│  │ │    Read, Update,   │   │   │ │ • Internal Notes       │ │  │
│  │ │    Delete own CV   │   │   │ │ • Recruiter Rating     │ │  │
│  │ ├────────────────────┤   │   │ │ • Screening Score      │ │  │
│  │ │ 🔒 Admin           │   │   │ │ • Salary (internal)    │ │  │
│  │ │    Read all (audit)│   │   │ │ • Lead Source Details  │ │  │
│  │ ├────────────────────┤   │   │ └────────────────────────┘ │  │
│  │ │ 🔒 Employer        │   │   │                            │  │
│  │ │    Read applicants │   │   │ ┌────────────────────────┐ │  │
│  │ ├────────────────────┤   │   │ │ VISIBLE TO CANDIDATE  │ │  │
│  │ │ 🔒 Guest           │   │   │ │ ────────────────────── │ │  │
│  │ │    NO ACCESS       │   │   │ │ • Name, Email          │ │  │
│  │ └────────────────────┘   │   │ │ • Skills, Experience   │ │  │
│  │                           │   │ │ • Education            │ │  │
│  │ ⚠️ Download requires      │   │ │ • Public Profile       │ │  │
│  │    candidate consent      │   │ └────────────────────────┘ │  │
│  └───────────────────────────┘   └────────────────────────────┘  │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ████████████████████████████████████████████████████████████████ │
└──────────────────────────────────────────────────────────────────┘
```

### Visual Elements
- **Lock Icons**: Unicode 🔒 or custom icon, navy color
- **Two-tier lists**: Internal (restricted) vs Public (visible)
- **Visual divider**: Dashed line between internal/public sections
- **Card shadows**: subtle drop shadow (0 2px 8px rgba(0,0,0,0.1))

---

## SLIDE 21: NON-FUNCTIONAL REQUIREMENTS

### Layout Structure
```
┌──────────────────────────────────────────────────────────────────┐
│ [TCS Logo]                                    • • •             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  NON-FUNCTIONAL REQUIREMENTS                                      │
│  Performance • Scalability • Security • Usability               │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────┐ │
│  │ ⚡       │ │ 🖥️       │ │ 🔒       │ │ 👤       │ │ 📊    │ │
│  │PERFORM-  │ │AVAILA-   │ │SECURITY  │ │USABILITY │ │SCALA- │ │
│  │ANCE      │ │BILITY    │ │          │ │          │ │BILITY │ │
│  ├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤ ├───────┤ │
│  │ Page <2s │ │ Uptime   │ │ bcrypt/   │ │ Booking  │ │ Auto- │ │
│  │ Search<3s│ │ ≥99.5%   │ │ Argon2   │ │ <5 min   │ │ scale │ │
│  │ Book <4s │ │ Recovery  │ │          │ │          │ │       │ │
│  │          │ │ ≤30 min  │ │ JWT 15m  │ │ VN UI    │ │ Cache │ │
│  │          │ │ Backup   │ │ Lockout  │ │ 4 brows- │ │ <200ms│ │
│  │          │ │ Daily    │ │ 5 fails  │ │ ers      │ │       │ │
│  │          │ │          │ │          │ │          │ │ 100   │ │
│  │          │ │ RPO 24h  │ │ MIME     │ │ Error    │ │ users │ │
│  │          │ │          │ │ whitelist│ │ guidance │ │       │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └───────┘ │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ████████████████████████████████████████████████████████████████ │
└──────────────────────────────────────────────────────────────────┘
```

### Visual Elements
- **5 equal cards**: Grid layout (5 columns or 3+2)
- **Card header**: Icon + Title, navy background
- **Card body**: White background, bullet list
- **Icons**: Speed icon, Server icon, Shield icon, User icon, Chart icon
- **Value emphasis**: Bold for key numbers (99.5%, <2s)

---

## SLIDE 22: RISKS & MITIGATIONS (1/2)

### Layout Structure
```
┌──────────────────────────────────────────────────────────────────┐
│ [TCS Logo]                                    • • •             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  RISKS & MITIGATIONS (1/2)                                       │
│  Project risk assessment and response plan                        │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ RISK MATRIX                                                │ │
│  ├────────┬────────────────────┬───────┬───────────────────────┤ │
│  │   ID  │      Risk          │ Level │      Mitigation       │ │
│  ├────────┼────────────────────┼───────┼───────────────────────┤ │
│  │ R-01  │ Facebook API       │ 🔴 H  │ Manual import backup, │ │
│  │       │ changes            │       │ webhook redundancy    │ │
│  ├────────┼────────────────────┼───────┼───────────────────────┤ │
│  │ R-02  │ Data privacy       │ 🔴 H  │ GDPR/PDP compliance,  │ │
│  │       │ violation          │       │ encrypt all PII       │ │
│  ├────────┼────────────────────┼───────┼───────────────────────┤ │
│  │ R-03  │ Payment gateway    │ 🔴 H  │ Multi-provider,       │ │
│  │       │ downtime           │       │ retry mechanism       │ │
│  ├────────┼────────────────────┼───────┼───────────────────────┤ │
│  │ R-04  │ Scope creep        │ 🟡 M  │ Change Control Board,  │ │
│  │       │                   │       │ MoSCoW prioritization │ │
│  ├────────┼────────────────────┼───────┼───────────────────────┤ │
│  │ R-05  │ Team member        │ 🟡 M  │ Documentation,        │ │
│  │       │ leave              │       │ code review           │ │
│  └────────┴────────────────────┴───────┴───────────────────────┘ │
│                                                                  │
│  High Impact Risks Detail:                                       │
│  ┌──────────────────────┐ ┌──────────────────────┐              │
│  │ R-01: Facebook API   │ │ R-03: Payment Down  │              │
│  │ • Monitor API changelog│ │ • Backup: Stripe+PayOS│           │
│  │ • Quick adapt sprint  │ │ • Queue retry        │              │
│  │ • Manual CSV fallback  │ │ • Clear messaging    │              │
│  └──────────────────────┘ └──────────────────────┘              │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ████████████████████████████████████████████████████████████████ │
└──────────────────────────────────────────────────────────────────┘
```

### Visual Elements
- **Table**: Full-width, alternating row colors (white/#F5F7FA)
- **Level badges**: 🔴 High (red), 🟡 Medium (yellow), 🟢 Low (green)
- **Detail cards**: Two cards below main table
- **Table header**: Navy background, white text

---

## SLIDE 23: RISKS & MITIGATIONS (2/2)

### Layout Structure
```
┌──────────────────────────────────────────────────────────────────┐
│ [TCS Logo]                                    • • •             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  RISKS & MITIGATIONS (2/2)                                       │
│  Continued risk details and monitoring                            │
│                                                                  │
│  ┌───────────────────────────┐   ┌────────────────────────────┐  │
│  │ R-06: Database Perf      │   │ MONITORING & RESPONSE      │  │
│  │ ─────────────────────────│   │ ────────────────────────────│  │
│  │ Threat: Slow queries     │   │                            │  │
│  │ Impact: UX degradation   │   │ ┌──────────────────────┐   │  │
│  │ Mitigate:               │   │ │ Trigger      Response│   │  │
│  │   • Index optimization  │   │ │ ────────────────────│   │  │
│  │   • Query audit         │   │ │ API error  PagerDuty│   │  │
│  │   • Connection pooling  │   │ │ >5%       →  Rollback│   │  │
│  └───────────────────────────┘   │ ├──────────────────────┤   │  │
│                                   │ │ Response  Grafana   │   │  │
│  ┌───────────────────────────┐   │ │ >5s       →  Scale up│  │  │
│  │ R-07: Third-party Fail   │   │ ├──────────────────────┤   │  │
│  │ ─────────────────────────│   │ │ Payment   Opsgenie   │   │  │
│  │ Threat: Service outage   │   │ │ fail >2% →  Failover │   │  │
│  │ Mitigate:               │   │ ├──────────────────────┤   │  │
│  │   • Circuit breaker     │   │ │ Security  Immediate  │   │  │
│  │   • Fallback UI         │   │ │ event    →  Alert    │   │  │
│  │   • SLA monitoring      │   │ └──────────────────────┘   │  │
│  └───────────────────────────┘   │                            │  │
│                                   │ Incident Response Team:   │  │
│  ┌───────────────────────────┐   │ • 24/7 on-call rotation │  │
│  │ R-08: Security Breach    │   │ • Escalation matrix       │  │
│  │ ─────────────────────────│   │ • Runbooks documented     │  │
│  │ Threat: Unauthorized     │   │                            │  │
│  │ access                   │   │                            │  │
│  │ Mitigate:               │   │                            │  │
│  │   • OWASP compliance    │   │                            │  │
│  │   • Penetration testing │   │                            │  │
│  │   • Audit logs          │   │                            │  │
│  └───────────────────────────┘   └────────────────────────────┘  │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ████████████████████████████████████████████████████████████████ │
└──────────────────────────────────────────────────────────────────┘
```

### Visual Elements
- **Two-column layout**: Left = 3 risk cards stacked, Right = monitoring table
- **Risk cards**: White bg, left border colored by severity level
- **Monitoring table**: Compact, clear trigger→response mapping
- **Response icons**: Different icons for each response type

---

## SLIDE 24: EXPECTED CONTRIBUTIONS

### Layout Structure
```
┌──────────────────────────────────────────────────────────────────┐
│ [TCS Logo]                                    • • •             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  EXPECTED CONTRIBUTIONS                                           │
│  Business & operational impact, user experience improvements       │
│                                                                  │
│  ┌───────────────────┐ ┌───────────────────┐ ┌─────────────────┐ │
│  │ 📊 BUSINESS       │ │ 😊 USER           │ │ 🎯 KPIs         │ │
│  │    IMPACT         │ │    EXPERIENCE     │ │    TARGETS      │ │
│  ├───────────────────┤ ├───────────────────┤ ├─────────────────┤ │
│  │                   │ │                   │ │                 │ │
│  │ Lead mgmt:       │ │ CLIENT:           │ │ 📈 DAU: 1,000+ │ │
│  │ 20h/week saved   │ │ Find tutor 70%   │ │ 📈 GMV: 500M   │ │
│  │                  │ │ faster            │ │    VND/month   │ │
│  │ Tutor matching:  │ │                  │ │                 │ │
│  │ 60% faster       │ │ TUTOR:           │ │ 📈 Escrow: 200+│ │
│  │                  │ │ Fair matching    │ │    tx/month    │ │
│  │ Payment: 100%    │ │ algorithm        │ │                 │ │
│  │ secure escrow    │ │                  │ │ 📈 Conversion: │ │
│  │                  │ │ CENTER:          │ │    25% (from 8%)│ │
│  │ Center recruit:  │ │ Scale 10x with   │ │                 │ │
│  │ 3x faster       │ │ CRM automation   │ │ 📈 Response:   │ │
│  │                  │ │                  │ │    <2h (from 24h)│ │
│  │ Dispute: <24h    │ │ ADMIN:           │ │                 │ │
│  │ (from 3-5 days) │ │ Focus on value-  │ │ 📈 Retention:  │ │
│  │                  │ │ add tasks        │ │    70%          │ │
│  │ Reports: 90%     │ │                  │ │                 │ │
│  │ time saved       │ │                  │ │                 │ │
│  └───────────────────┘ └───────────────────┘ └─────────────────┘ │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ████████████████████████████████████████████████████████████████ │
└──────────────────────────────────────────────────────────────────┘
```

### Visual Elements
- **Three equal cards**: Business Impact, UX, KPIs
- **Card headers**: Icon + Title, colored background
- **Improvement indicators**: Arrow up (📈) for metrics
- **Comparison format**: "Before → After" or "New value"
- **Bold numbers**: Key metrics emphasized

---

## SLIDE 25: NEXT STEPS & ROADMAP

### Layout Structure
```
┌──────────────────────────────────────────────────────────────────┐
│ [TCS Logo]                                    • • •             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  NEXT STEPS                                                      │
│  v2.0 expansion plan and future roadmap                          │
│                                                                  │
│  TIMELINE ROADMAP                                                │
│  ─────────────────────────────────────────────────────────────── │
│                                                                  │
│  2026 Q2    2026 Q3    2026 Q4    2027 Q1    2027 Q2    2027 Q3 │
│  ┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐   │
│  │ v1.0│──→│ v1.1│──→│ v1.2│──→│ v2.0│──→│ v2.1│──→│ v2.2│   │
│  │Launch│  │ Bug │  │Stab │  │ AI  │  │Video│  │Adv  │   │
│  │      │  │Fix  │  │Scale│  │ v2  │  │Multi│  │Feat │   │
│  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘   │
│                                                                  │
│  v2.0 KEY FEATURES                                               │
│  ┌─────────────────────┐ ┌─────────────────────┐ ┌───────────┐ │
│  │ 🤖 AI TUTOR         │ │ 🔔 AUTO-NOTIFY      │ │ 📱 MOBILE │ │
│  │     MATCHING V2     │ │     SYSTEM          │ │     &     │ │
│  ├─────────────────────┤ ├─────────────────────┤ │  VIDEO    │ │
│  │ • Personality match │ │ • Email templates   │ ├───────────┤ │
│  │ • Learning style    │ │ • Push notifications│ │ • iOS app │ │
│  │ • Schedule AI       │ │ • SMS critical      │ │ • Android │ │
│  │ • Success predict   │ │ • Class reminders   │ │ • Video   │ │
│  │ • NL search         │ │ • Payment alerts    │ │   integration│
│  └─────────────────────┘ └─────────────────────┘ └───────────┘ │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ████████████████████████████████████████████████████████████████ │
└──────────────────────────────────────────────────────────────────┘
```

### Visual Elements
- **Horizontal timeline**: Connected nodes with version labels
- **Timeline nodes**: Rounded rectangles, navy fill, white text
- **Timeline connector**: Lines with arrows between nodes
- **Feature cards**: 3 cards below timeline
- **Card icons**: Robot (AI), Bell (notifications), Mobile (app)
- **Feature bullets**: Icon + text format

---

## Common Elements Across All Slides

### Header
- Logo: "TCS" text or logo image, top-left, 32px height
- Progress indicator: 3 dots (◐ ◑ ◒), top-right
- Thin navy strip at very top (4px)

### Footer
- Navy gradient bar, 80px height
- Can include slide number or copyright text

### Typography
- Title font: System font, Bold, 48pt, Navy (#1B2D5C)
- Subtitle: System font, Regular, 20pt, Gray (#666666)
- Card title: System font, Bold, 20pt, Navy
- Body text: System font, Regular, 14-16pt, Dark (#333333)
- Badge text: System font, Bold, 12pt, White

### Spacing
- Slide padding: 60px all sides
- Card padding: 24px
- Element gap: 16-24px
- Card gap: 24px
