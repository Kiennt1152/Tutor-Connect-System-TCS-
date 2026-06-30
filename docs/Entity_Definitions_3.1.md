# 3.1 Entity Definitions (đối chiếu Code ↔ Spec ↔ 71 UC)

**Nguyên tắc:** Giữ cấu trúc module hiện tại (`identity`, `profile`, `catalog`, `marketplace`, `center`, `contract`, `finance`, `messaging`, `platform`). Chỉ **bổ sung** entity thiếu theo UC, **xóa** entity không còn trong phạm vi. **Không** tạo bảng `Role` — vai trò suy từ profile 1:1 (`Client` / `Tutor` / `TutorCenter` / `PlatformAdmin`).

**PK trong code hiện tại:** `BIGINT AUTO_INCREMENT` (spec báo cáo ghi UUID — giữ BIGINT khi implement, UUID chỉ là logical model).

---

## Tổng số bảng

| Nguồn | Số lượng |
|-------|----------|
| Spec §3.1 (71 UC) | **66 bảng** |
| Code cũ (Lead CRM — **không** trong spec) | −4 bảng đã xóa |
| Entity Java hiện có | **66** (50 cũ − Lead − Escrow + 17 mới) |
| Cần bổ sung entity Java | **0** — đã implement |

---

## A. XÓA — không còn sử dụng

| Bảng (code cũ) | Lý do |
|----------------|-------|
| `leads` | Không có trong spec 3.1 / 71 UC |
| `lead_assignments` | Phụ thuộc Lead |
| `lead_routing_rules` | Phụ thuộc Lead |
| `lead_routing_rule_centers` | Phụ thuộc Lead |

**§3.2:** Xóa mọi quan hệ Lead.

---

## B. Ánh xạ tên Code ↔ Spec (giữ entity, đổi tên logic)

| Code (Java / `@Table`) | Spec §3.1 | Ghi chú |
|------------------------|-----------|---------|
| `TutoringClass` / `tutoring_classes` | **Class** | Giữ class Java, alias báo cáo = Class |
| `CenterTutorMembership` / `center_tutor_memberships` | **CenterTutor** | Cùng nghiệp vụ |
| `PaymentTransaction` / `payment_transactions` | **Payment** | Giữ code, spec gọi Payment |
| `RefundRequest` / `refund_requests` | **Refund** | Giữ code, spec gọi Refund |
| `Escrow` + `EscrowTransaction` | **EscrowTransaction** | Đã merge — bảng `escrows` xóa, `EscrowTransaction` là escrow chính |
| `FaqEntry` / `faq_entries` | **FAQ** | Giữ code |
| `AuditLog` / `audit_logs` | **AuditLog** | PK spec `log_id` = code `audit_id` |

---

## C. BỔ SUNG — có trong spec, chưa có entity Java

| # | Bảng spec | Module đề xuất | UC chính |
|---|-----------|----------------|----------|
| 1 | **Subject** | catalog | UC-31, Class |
| 2 | **Qualification** | profile | UC-08, UC-09 |
| 3 | **ClassAssignment** | marketplace | UC-15, 22, BF-04 |
| 4 | **ApplicationStatusHistory** | marketplace | Audit application |
| 5 | **VerificationHistory** | identity | UC-11 |
| 6 | **PaymentHistory** | finance | UC-40, 58 |
| 7 | **Report** | platform | UC-52 |
| 8 | **Location** | catalog | UC-31, UC-08 |
| 9 | **Grade** | catalog | Class, UC-06 |
| 10 | **Province** | catalog | Location |
| 11 | **ApiKey** | platform | Integration (optional) |
| 12 | **ParentChildLink** | profile | UC-05 |
| 13 | **ChildProfile** | profile | UC-06 |
| 14 | **TutorAvailability** | profile | UC-12, UC-13 |
| 15 | **ClassStudent** | marketplace | UC-19, UC-20 |
| 16 | **ContractTemplate** | contract | UC-45 |
| 17 | **UserPenalty** | platform | UC-59, UC-60 |

---

## D. MỞ RỘNG cột — entity đã có, cần thêm field (giữ bảng)

### User (`users`) — UC-01, UC-07, UC-60
| Column | Type | Mới? |
|--------|------|------|
| phone | VARCHAR(15) NULL | MỚI |
| status | ACTIVE, **SUSPENDED**, BANNED (default ACTIVE) | MỞ RỘNG — bỏ PENDING |
| updated_at | TIMESTAMP | MỚI |
| *(không có role_id)* | — | Role = suy từ profile 1:1 |

### Client — UC-08
| Column | Mới? |
|--------|------|
| date_of_birth, gender | MỚI |
| location_id → Location | MỚI |
| avatar_url (rename từ avatar) | ĐỔI TÊN |
| created_at, updated_at | MỚI |

### Tutor — UC-08, UC-09
| Column | Mới? |
|--------|------|
| date_of_birth, location_id | MỚI |
| verification_status (UNDER_VERIFY, VERIFIED, **REJECTED**) | MỚI |
| avatar, created_at, updated_at | MỚI |

### TutorCenter — UC-08, UC-10
| Column | Mới? |
|--------|------|
| location_id, verification_status, avatar | MỚI |
| created_at, updated_at | MỚI |

### PlatformAdmin
| Column | Mới? |
|--------|------|
| created_at, updated_at | MỚI |

### VerificationRequest — UC-11
| Column | Mới? |
|--------|------|
| admin_notes | MỚI |
| status → DRAFT, SUBMITTED, UNDER_REVIEW, VERIFIED, REJECTED | MỞ RỘNG |
| created_at, updated_at | MỚI |

### VerificationDocument
| Column | Mới? |
|--------|------|
| document_type + LICENSE | MỞ RỘNG |
| file_id → MediaFile (thay file_url trực tiếp) | Khuyến nghị |

### Class / TutoringClass — UC-14, UC-31
| Column | Mới? |
|--------|------|
| subject_id, grade_id, location_id | MỚI / FK |
| lesson_mode, number_of_sessions, tuition_fee, recurring_type | MỚI |
| status + MATCHED, DISPUTED | MỞ RỘNG |
| updated_at | MỚI |
| *(xem xét bỏ max_sessions trùng number_of_sessions)* | |

### TutorApplication — UC-16, UC-17
| Column | Mới? |
|--------|------|
| proposed_rate, reviewed_at | MỚI |
| status + WITHDRAWN | MỞ RỘNG |

### Contract — UC-44
| Column | Mới? |
|--------|------|
| contract_no, assignment_id (1:1), contract_file_url, terms_summary, template_id | MỚI / đổi FK |
| status → DRAFT, SIGNED, ACTIVE, COMPLETED, TERMINATED | Spec |
| updated_at | MỚI |

### Conversation — UC-36
| Column | Mới? |
|--------|------|
| status (ACTIVE, ARCHIVED) | MỚI |
| context_type, context_id, type, last_message_at | Giữ code hiện có |

### Notification — UC-37
| Column | Mới? |
|--------|------|
| type, reference_type, reference_id, is_read | MỚI |
| template_id → NotificationTemplate | FK |

### Wallet — UC-39
| Column | Mới? |
|--------|------|
| balance (rename available_balance) | ĐỔI TÊN |
| status ACTIVE, SUSPENDED | MỞ RỘNG |
| updated_at | MỚI |

### EscrowTransaction (merge từ Escrow)
| Column | Mới? |
|--------|------|
| payment_id, assignment_id, deposited_at, released_at | MỚI |
| status + DISPUTED | MỞ RỘNG |

### Review — UC-53, UC-54, UC-55
| Column | Mới? |
|--------|------|
| assignment_id, class_id, status (VISIBLE/HIDDEN/MODERATED) | MỚI |
| review_type (CLIENT_TO_TUTOR, TUTOR_TO_CLIENT) | Giữ code |

### SupportTicket — UC-66
| Column | Mới? |
|--------|------|
| target_class_id, category (enum), evidence_urls | MỚI |
| status + IN_REVIEW | MỞ RỘNG |
| updated_at | MỚI |

### Category — UC-57
| Column | Mới? |
|--------|------|
| category_name, type, is_active, sort_order | MỚI |
| created_at, updated_at | MỚI |

### FAQ / FaqEntry — UC-67
| Column | Mới? |
|--------|------|
| sort_order, is_published, created_at, updated_at | MỚI |

### AuditLog — UC-61
| Column | Mới? |
|--------|------|
| entity_name (spec) = entity_type (code) | Alias |
| timestamp = created_at | Alias |

### RecommendationLog — UC-62, UC-63, UC-64
| Column | Mới? |
|--------|------|
| recruitment_id (nullable) | MỚI UC-63 |
| generated_at = created_at | Alias |

### RecruitmentPost / RecruitmentApplication — UC-68–71
| Column | Mới? |
|--------|------|
| required_experience, subject_id, location_id, max_positions, published_at, closed_at | Post |
| resume_url, interview_date/notes, reviewed_at, status funnel mới | Application |

### CenterTutor / CenterTutorMembership
| Column | Mới? |
|--------|------|
| recruitment_app_id, status TERMINATED | MỚI |

---

## E. §3.2 Entity Relationships (đã chỉnh)

```text
User 1:1 ── Client          (user_id FK UNIQUE)
User 1:1 ── Tutor           (user_id FK UNIQUE)
User 1:1 ── TutorCenter     (user_id FK UNIQUE)
User 1:1 ── PlatformAdmin   (user_id FK UNIQUE)
User 1:1 ── Wallet          (user_id FK UNIQUE)
-- XÓA: User N:1 Role

Tutor 1:N ── TutorSubject       (tutor_id FK)
Tutor 1:N ── Qualification      (tutor_id FK)
Tutor 1:N ── TutorApplication   (tutor_id FK)
Tutor 1:N ── TutorEducation / TutorExperience / TutorCertificate
Tutor 1:N ── TutorAvailability  (MỚI)
Tutor N:M ── Subject             (via TutorSubject)
Tutor N:M ── TutorCenter         (via CenterTutor / CenterTutorMembership)

Client / TutorCenter ──creates──> Class/TutoringClass (creator_id → User)

Class 1:N ── TutorApplication
Class 1:N ── Lesson
Class 1:N ── ScheduleSlot
Class 1:N ── ClassStudent       (MỚI)
Class N:1 ── Subject, Grade, Location

TutorApplication 1:0..1 ── ClassAssignment  (application_id nullable — UC-15)
ClassAssignment 1:1 ── Contract
ClassAssignment 1:N ── EscrowTransaction
ClassAssignment 1:N ── Review
ClassAssignment 1:N ── PaymentReleaseRequest

Payment 1:1 ── EscrowTransaction
EscrowTransaction 1:N ── Refund
Report 1:1 ── Dispute

User 1:N ── VerificationRequest
VerificationRequest 1:N ── VerificationDocument
VerificationRequest 1:N ── VerificationHistory  (MỚI)

User N:M ── Conversation (ConversationParticipant)
Conversation 1:N ── Message

NotificationTemplate 1:N ── Notification
Notification 1:N ── NotificationQueue

User 1:N ── ParentChildLink (MỚI)
ParentChildLink N:1 ── ChildProfile (MỚI)

Contract N:1 ── ContractTemplate (MỚI)
Contract 1:N ── ContractSignature

User 1:N ── UserPenalty (MỚI)
PlatformAdmin 1:N ── UserPenalty

Location N:1 ── Province
Category N:1 ── Category (self)
ScheduleSlot 1:N ── Lesson (slot_id FK — đồng bộ code)
```

---

## F. Danh sách 66 bảng mục tiêu (theo module)

### identity (6)
User, PasswordResetToken, VerificationRequest, VerificationDocument, **VerificationHistory**, MediaFile

### profile (11)
Client, Tutor, TutorCenter, PlatformAdmin, TutorEducation, TutorExperience, TutorCertificate, **Qualification**, **ParentChildLink**, **ChildProfile**, **TutorAvailability**, ReputationHistory

### catalog (8)
Category, **Subject**, TutorSubject, SystemParameter, FAQ, **Location**, **Grade**, **Province**

### marketplace (9)
Class/TutoringClass, TutorApplication, **ClassAssignment**, **ApplicationStatusHistory**, ScheduleSlot, Lesson, MatchingPreference, FavoriteTutor, **ClassStudent**

### center (3)
RecruitmentPost, RecruitmentApplication, CenterTutor/CenterTutorMembership

### contract (4)
Contract, ContractSignature, **ContractTemplate**, Review

### finance (10)
Wallet, PaymentMethod, Payment/PaymentTransaction, **PaymentHistory**, EscrowTransaction, Refund/RefundRequest, PaymentReleaseRequest, FinancialJournal, WithdrawalRequest, Dispute

### messaging (8)
Conversation, ConversationParticipant, Message, MessageAttachment, NotificationTemplate, Notification, NotificationPreference, NotificationQueue

### platform (6)
AuditLog, RecommendationLog, SupportTicket, **Report**, **UserPenalty**, **ApiKey**

---

## G. Thứ tự implement (trạng thái)

1. ✅ **Xóa Lead CRM** — `V2__drop_lead_crm_tables.sql`, xóa entity/repo
2. ✅ **Thêm 17 entity** — `V3__add_entities_and_merge_escrow.sql`
3. ✅ **Merge Escrow → EscrowTransaction** — cùng V3 + cập nhật V1
4. ✅ **Mở rộng cột** entity hiện có — `V4__extend_entities_for_uc.sql`
5. ✅ **Entity UC-26/28** — `TutorReplacementRequest`, `ClassTerminationRequest`

---

*Phiên bản: 3.1-rev2 · Đồng bộ codebase TCS main · 71 UC List v2*
