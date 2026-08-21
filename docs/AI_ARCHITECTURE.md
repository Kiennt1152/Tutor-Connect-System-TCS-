# 🏛️ TCS AI MODULE ARCHITECTURE & PIPELINE INVENTORY (v2.0)

**Tài liệu kiểm kê toàn diện mã nguồn phục vụ Tái cấu trúc Semantic-First (Phase 0)**  
**Branch:** `feature/user` | **Môi trường:** Java 21, Spring Boot 3.x, Maven 3.9  

---

## 1. Bản đồ Pipeline Thực tế từ Source Code

```
                                      [User HTTP Request: /api/ai/chat]
                                                      │
                                                      ▼
                                       ┌─────────────────────────────┐
                                       │    AiController.java        │
                                       │    (AuthHelper: userId)     │
                                       └──────────────┬──────────────┘
                                                      │
                                                      ▼
                                       ┌─────────────────────────────┐
                                       │     AiServiceImpl.java      │
                                       └──────────────┬──────────────┘
                                                      │
                        ┌─────────────────────────────┴─────────────────────────────┐
                        │ STAGE 0: Fast-Path Safety & Crisis Filter                 │
                        │ Class: ContentSafetyFilter.java                           │
                        │ Returns: SafetyCheckResult (CRISIS, BLOCKED, PRIVACY)     │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │ (If Safe)
                                                      ▼
                        ┌───────────────────────────────────────────────────────────┐
                        │ STAGE 1: Intent Classification (3-Tier)                   │
                        │ Class: AiIntentService.java                               │
                        │ ├─ Tier 1: IntentClassifier.java (Fast-path & Keywords)   │
                        │ └─ Tier 2: LlmIntentClassifierService.java (LLM Semantic) │
                        │ Returns: DetailedIntentResult (domain, subIntent, intent) │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │
                        ┌─────────────────────────────┴─────────────────────────────┐
                        │ STAGE 2: Deterministic Level 0 Safety & Conversation      │
                        │ Class: AiFallbackService.checkLevel0Safety()              │
                        │ Returns: Greeting, Goodbye, Thanks, Capability, Profanity │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │ (If Business/Open/General)
                                                      ▼
                        ┌───────────────────────────────────────────────────────────┐
                        │ STAGE 3: Conversational Query Rewriting                   │
                        │ Class: AiQueryRewriteService.java                         │
                        │ Returns: RewriteResult (rewrittenQuery, isFollowUp)       │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │
                        ┌─────────────────────────────┴─────────────────────────────┐
                        │ STAGE 4: Capability Policy & RBAC Auth Check              │
                        │ Class: AiCapabilityRouter.java                            │
                        │ Checks: requireAuth, allowedRoles vs userRole             │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │ (If Authorized)
                                                      ▼
                        ┌───────────────────────────────────────────────────────────┐
                        │ STAGE 5: Context Retrieval (Vector RAG + DB Providers)    │
                        │ ├─ AiRetrievalService.java (Vector Search / H2 / MySQL)   │
                        │ ├─ AiRerankService.java (BM25 + Semantic Cross-Rerank)    │
                        │ └─ Context Providers: Tutors, Classes, PlatformStats, etc.│
                        │ Returns: List<AiSourceResponse>                           │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │
                        ┌─────────────────────────────┴─────────────────────────────┐
                        │ STAGE 6: Score Guard & Grounding Evaluation               │
                        │ ├─ Score Guard: Drops sources with finalScore < 0.60      │
                        │ ├─ AiAnswerEvaluatorService.java                          │
                        │ └─ [Legacy Short-Circuit]: Fallback on empty sources      │
                        └─────────────────────────────┬─────────────────────────────┘
                                                      │
                        ┌─────────────────────────────┴─────────────────────────────┐
                        │ STAGE 7: LLM Answer Generation & Response Assembly        │
                        │ ├─ AiPromptBuilderService.java (System Grounding Prompt)  │
                        │ ├─ AiProviderRouter.java (Groq, Cerebras, DeepSeek, Gemini│
                        │ └─ Hydrate DTO: Referenced Tutors, Classes, FAQs, Badges  │
                        └───────────────────────────────────────────────────────────┘
```

---

## 2. Kiểm kê Chi tiết từng Stage (Source Inventory)

| Stage | File Path / Class | Method | Input Type | Output Type | Caller / Callee |
|---|---|---|---|---|---|
| **0. Entrypoint** | [`AiController.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/controller/AiController.java) | `chat` | `ChatRequest`, `userId` | `AiMessageResponse` | HTTP POST `/api/ai/chat` $\rightarrow$ `AiService.chat` |
| **1. Safety Filter** | [`ContentSafetyFilter.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/ContentSafetyFilter.java) | `checkQuery` | `String query` | `SafetyCheckResult` | `AiServiceImpl` $\rightarrow$ Level 0 deterministic response |
| **2. Intent Router** | [`AiIntentService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/AiIntentService.java) | `classifyAndExtractDetailed` | `String message` | `DetailedIntentResult` | `AiServiceImpl` $\rightarrow$ `IntentClassifier`, `LlmIntentClassifierService` |
| **2a. Keyword Classifier** | [`IntentClassifier.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/IntentClassifier.java) | `classifyDetailed` | `String message` | `ClassificationDetail` | `AiIntentService` |
| **2b. LLM Classifier** | [`LlmIntentClassifierService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/LlmIntentClassifierService.java) | `classifyWithLlm` | `String userMessage` | `ClassificationDetail` | `AiIntentService` $\rightarrow$ `AiProviderRouter.chat` |
| **3. Query Rewrite** | [`AiQueryRewriteService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/AiQueryRewriteService.java) | `rewriteQuery` | `history`, `query`, `intent` | `RewriteResult` | `AiServiceImpl` |
| **4. Capability Policy** | [`AiCapabilityRouter.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/AiCapabilityRouter.java) | `getPolicy` | `AiDomain`, `AiSubIntent` | `CapabilityPolicy` | `AiServiceImpl` (Enforces RBAC) |
| **5. Vector Retrieval** | [`AiRetrievalService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/AiRetrievalService.java) | `retrieve` | `query`, `role`, `userId` | `List<RetrievalResult>` | `AiServiceImpl` $\rightarrow$ `AiPermissionFilterService` |
| **6. Context Reranker** | [`AiRerankService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/AiRerankService.java) | `rerank` | `vectorResults`, `intent`, `query` | `List<AiSourceResponse>` | `AiServiceImpl` |
| **7. Prompt Builder** | [`AiPromptBuilderService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/AiPromptBuilderService.java) | `buildPrompt` | `query`, `intent`, `role`, `sources` | `String` (System + User prompt) | `AiServiceImpl` |
| **8. Multi-Provider Router**| [`AiProviderRouter.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/provider/AiProviderRouter.java) | `chat` | `AiProviderChatRequest` | `AiProviderChatResponse` | `AiServiceImpl`, `LlmIntentClassifierService` |
| **9. Fallback Service** | [`AiFallbackService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/AiFallbackService.java) | `getLevel3EnhancedNoData` etc. | `subIntent`, `entities` | `FallbackResult` | `AiServiceImpl` |

---

## 3. Danh mục Enums Hợp lệ trong Hệ thống

### 3.1 `AiDomain` (16 giá trị)
`CONVERSATION_SAFETY`, `IDENTITY_AUTH`, `PROFILE_GUARDIAN`, `VERIFICATION`, `MARKETPLACE`, `TUTOR_OPS`, `CENTER_OPS`, `FINANCE_WALLET`, `CONTRACT_REVIEW`, `MESSAGING_TICKET`, `TRUST_SAFETY`, `CATALOG_FAQ`, `PLATFORM_ADMIN`, `AI_TUTORING`, `OPEN_DOMAIN`, `OUT_OF_SCOPE`.

### 3.2 `AiIntent` (13 giá trị)
`FAQ_SUPPORT`, `FIND_TUTOR`, `FIND_CLASS`, `CREATE_CLASS`, `PAYMENT_SUPPORT`, `TICKET_SUPPORT`, `TUTOR_VERIFICATION`, `TUTOR_OPTIMIZATION`, `CENTER_MANAGEMENT`, `ADMIN_DASHBOARD`, `AI_TUTORING`, `PLATFORM_STATS`, `OUT_OF_SCOPE`.

### 3.3 `AiSubIntent` (80+ giá trị)
Xem định nghĩa đầy đủ tại [`AiSubIntent.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/enums/AiSubIntent.java).

### 3.4 `KnowledgeSourceType` (7 giá trị)
`FAQ`, `TUTOR`, `CLASS`, `POLICY`, `SYSTEM_DOC`, `TICKET`, `TRANSACTION`.

---

## 4. Kiểm kê Nút thắt Kỹ thuật & Các điểm Early-Return Cần Tái Cấu Trúc

1. **Threshold Bypass ở `AiIntentService.java:61`**:
   - `if (detail.confidence() < 0.85 || detail.domain() == AiDomain.OUT_OF_SCOPE)`
   - `IntentClassifier` keyword match trả về `0.95` $\rightarrow$ Chặn hoàn toàn LLM Semantic Router.
2. **Early-Return No-Data Hardcoded ở `AiServiceImpl.java:311-343`**:
   - `if (allSources.isEmpty() && (subIntent == FIND_TUTOR || ...))`
   - Tự động return chuỗi cố định *"Hiện tại hệ thống TCS chưa tìm thấy gia sư phù hợp..."*, không gửi query thật cho LLM.
3. **Hardcoded Strict Grounding ở `AiPromptBuilderService.java:38-41, 81-85`**:
   - Khi `sources` rỗng, prompt bị ép inject *"Không có dữ liệu phù hợp trong cơ sở dữ liệu"* và luật ép LLM phải trả lời câu từ chối mẫu tìm gia sư.
4. **Provider Config & Secrets**:
   - Khởi tạo trong `AiProviderRouter`: timeout 15000ms, cooldown 60s, order: `groq, cerebras, deepseek, gemini`.
   - API keys nạp từ Spring `@Value("${ai.xxx.api-key:}")` bảo mật qua biến môi trường.

---

## 5. Ranh giới Bảo mật Dữ liệu & Server-Side RBAC

- **ĐƯỢC PHÉP đưa vào LLM Prompt**:
  - `originalUserQuery` và `rewrittenQuery` sau khi đi qua [`AiPromptSanitizer.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/util/AiPromptSanitizer.java) để escape an toàn `<`, `>`, `&`, ```` ``` ````, `` ` ``.
  - Trích đoạn tri thức công khai đã được lọc qua [`AiPermissionFilterService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/AiPermissionFilterService.java) (`FAQ`, `POLICY`, `SYSTEM_DOC`, `PUBLIC TUTOR PROFILES`, `OPEN CLASSES`).
  - Dữ liệu cá nhân của chính người dùng đã xác thực (ví dụ số dư ví, danh sách ticket của chính `userId` đó).
- **TUYỆT ĐỐI CẤM đưa vào LLM Prompt**:
  - Mật khẩu, password hash, mã OTP xác thực.
  - Danh sách tài khoản người dùng toàn hệ thống (Bulk Account Dump).
  - Dữ liệu CCCD thô, token xác thực, session ID của người khác.
  - Mọi thao tác quản trị vượt quyền: Luôn enforce `AiCapabilityRouter` & Spring Security server-side trước khi retrieval.

---

## 6. Tiêu chuẩn Phân loại & Quy định Kỹ thuật

1. **Fast-Path Rule**: Tối đa $\le 4$ từ, chỉ kích hoạt khi khớp chính xác danh sách token chào hỏi, tạm biệt, cảm ơn chuẩn tại [`IntentClassifier.checkFastPath`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/IntentClassifier.java).
2. **24 Allowed Classifier Intents**:
   `FIND_TUTOR`, `FIND_CLASS`, `CREATE_CLASS`, `FAQ_SUPPORT`, `FINANCE`, `CONTRACT`, `TRUST_SAFETY`, `DISPUTE`, `REPORT`, `TICKET`, `IDENTITY`, `VERIFICATION`, `PROFILE`, `TUTOR_OPS`, `CENTER_OPS`, `ADMIN`, `AI_TUTORING`, `CHITCHAT`, `ENTERTAINMENT`, `MATH`, `WEATHER`, `TIME`, `SECURITY_VIOLATION`, `OUT_OF_SCOPE`.
3. **Session Ownership & Tenancy**:
   - `getUserSessions`: Trả về danh sách rỗng nếu `userId == null`.
   - `getSessionMessages` & `deleteSession`: Ném `ForbiddenException` (HTTP 403) nếu phiên thuộc về user khác.
   - `chat`: Tự động tạo phiên mới nếu client truyền `sessionId` thuộc về user khác để chống ô nhiễm phiên.
4. **Lệnh chạy Test Suite từ thư mục `backend/`**:
   - Chạy toàn bộ test Backend: `.\mvnw.cmd test` (Windows) hoặc `./mvnw test` (Linux)
   - Chạy riêng module AI: `.\mvnw.cmd test "-Dtest=com.tcs.module.ai.**.*Test"`
   - *Lưu ý về kiểm thử offline*: Bộ test suite sử dụng Mock Provider để đảm bảo $100\%$ tính xác định (deterministic), chạy độc lập không phụ thuộc mạng, kiểm tra toàn diện schema, boundary, timeout, cohorting và fallback logic.
