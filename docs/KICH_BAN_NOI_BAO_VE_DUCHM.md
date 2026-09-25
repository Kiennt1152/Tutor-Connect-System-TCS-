# 🎙️ KỊCH BẢN THUYẾT TRÌNH BẢO VỆ ĐỒ ÁN CAPSTONE / SWP391
## DÀNH RIÊNG CHO: HOÀNG MINH ĐỨC (DucHM — HE187354 / `mduc1011-swp`)
* **Dự án**: Tutor Connect System (TCS) — Hệ thống kết nối Gia sư thông minh
* **Khung giờ bảo vệ chính thức**: **10:50 – 12:20, Thứ Bảy ngày 26/09/2026**
* **Kiến trúc thuyết trình**: Chu trình 2 Chiều Khép Kín qua 6 Tầng Kiến Trúc Spring Boot:
  `[Frontend UI]` ⬇️ `[Security Filter]` ⬇️ `[Controller]` ⬇️ `[Interface]` ⬇️ `[ServiceImpl]` ⬇️ `[Repository]` ⬇️ `[Database MySQL]`
  *(Điểm quay đầu CSDL)* ⬆️ `[Entity JPA]` ⬆️ `[Mapper: toResponse DTO sạch]` ⬆️ `[Controller]` ⬆️ `[Frontend Render]`
* **Đồng bộ hóa 100%**: Khớp số thứ tự và tên gọi với các comment banner `// LUỒNG 1` đến `// LUỒNG 13` trong mã nguồn Java và [`docs/CAM_NANG_ON_TAP_BAO_VE_DUCHM.md`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/docs/CAM_NANG_ON_TAP_BAO_VE_DUCHM.md).

---

# 🌟 PHẦN 0: LỜI MỞ ĐẦU ĐỊNH VỊ BẢN THÂN (30 GIÂY MỞ MÀN)
*(Đứng thẳng, tự tin nhìn vào hội đồng, giọng nói dõng dạc, rõ ràng)*

> *"Dạ kính thưa quý thầy cô trong hội đồng chấm bảo vệ đồ án tốt nghiệp, em tên là **Hoàng Minh Đức** (DucHM — HE187354).*
> 
> *Trong dự án Tutor Connect System (TCS), em chịu trách nhiệm nghiên cứu, thiết kế và phát triển toàn bộ phân hệ **Điều hành Sàn, Quản trị Tri thức và Trí tuệ Nhân tạo**.*
> 
> *Khối lượng công việc của em được quy hoạch bài bản thành 3 cụm module chiến lược:*
> 1. *Thứ nhất là **3 Module Trụ Cột**: `platform` (Vận hành & Hỗ trợ sàn), `catalog` (Quản trị tri thức FAQ & Danh mục phân cấp) và `ai` (Trợ lý ảo RAG 12 bước).*
> 2. *Thứ hai là **Cụm Module An Ninh Sàn**: gồm `circumvention` (quét lách sàn Regex), `penalty` (xử phạt & chốt chặn quyền) và `audit` (nhật ký kiểm toán bất biến).*
> 3. *Thứ ba là **Cụm Module Tương Tác & Pháp Lý**: gồm `messaging` (tạo ticket hỗ trợ), `contract` (ký hợp đồng OTP, tính điểm uy tín) và `identity` (bảo mật xác thực & thu hồi phiên JWT).*
> 
> *Tất cả các tính năng của em đều được lập trình tuân thủ nghiêm ngặt **Chu trình 2 Chiều Khép Kín qua 6 Tầng Kiến Trúc Spring Boot**: từ Frontend gửi Request đi xuống CSDL, và từ CSDL dữ liệu đi ngược lên được tầng ServiceImpl bóc tách, chuyển đổi qua Mapper thành DTO sạch rồi mới phản hồi về Client.*
> 
> *Sau đây, em xin phép trình bày chi tiết kịch bản vận hành của từng luồng nghiệp vụ ạ!"*

---

# 🏛️ PHẦN 1: KỊCH BẢN NÓI 13 LUỒNG LÕI ĐỒNG BỘ BANNER CODE JAVA

---

### // LUỒNG 1: TRA CỨU DANH MỤC FAQ CÔNG KHAI
*(Vị trí code: `CatalogController.java:60`, `CatalogServiceImpl.java:200`, `FaqEntryRepository.java:14`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 1: Tra cứu & Tìm kiếm Danh mục FAQ Công khai**:
> 
> ⬇️ **Chiều đi xuống (Request & Truy vấn CSDL):**
> 1. *Tại **Frontend UI** (`HelpPage.tsx`), người dùng nhập từ khóa tìm kiếm và chọn danh mục, React gửi request `GET /api/catalog/faqs?category=...&keyword=...`.*
> 2. *Tại **REST Controller** (`CatalogController.java`), hàm `getPublicFaqs()` tiếp nhận query params và gọi sang Service Interface `catalogService.getFaqEntries(category, keyword)`.*
> 3. *Tại **Service Interface** (`CatalogService.java`), hợp đồng trừu tượng định nghĩa chữ ký hàm.*
> 4. *Tại **Service Impl** (`CatalogServiceImpl.java`), việc đầu tiên là ServiceImpl **gọi ngay xuống Tầng 5 Repository** hàm:  
>    `faqEntryRepository.findByPublishedTrueAndCategoryOrderBySortOrderAscFaqIdAsc(category)`  
>    *(hoặc `findByPublishedTrueOrderBySortOrderAscFaqIdAsc()` nếu không lọc danh mục) để Database lọc lấy các câu hỏi FAQ đã xuất bản (`published = true`).*
> 5. *Tại **Repository**, Spring Data JPA tự động dịch tên hàm thành câu lệnh SQL: `SELECT * FROM faq_entries WHERE is_published = 1 AND category = ? ORDER BY sort_order ASC, faq_id ASC`.*
> 6. *Tại **Database & Entity**, MySQL quét bảng `faq_entries` theo chỉ mục và trả về danh sách Entity thô. **Đây là điểm quay đầu của dữ liệu!***
> 
> ⬆️ **Chiều đi ngược lên (Thuật toán In-Memory Ranking & DTO Mapping):**
> * *Database trả về danh sách các thực thể JPA thô `List<FaqEntry>` cho Repository, Repository giao lại lên cho ServiceImpl.*
> * *Tại ServiceImpl, hệ thống thực thi **Thuật toán tìm kiếm & Xếp hạng 2 tầng (In-Memory Ranking Engine)**:
>   - Service tách từ khóa (`tokenize`), chuẩn hóa Unicode NFD loại bỏ dấu tiếng Việt và chữ `đ`.
>   - Duyệt qua từng FAQ và chấm điểm liên quan (`calculateFaqScore`): khớp trong Tiêu đề nhân hệ số 2, khớp trong Nội dung nhân hệ số 1.
>   - Sắp xếp kết quả đa tầng: điểm liên quan cao nhất lên đầu (`score DESC`), sau đó đến thứ tự ưu tiên (`sortOrder ASC`).
>   - Cuối cùng, ServiceImpl dùng mapper `toFaq` chuyển đổi Entity thành DTO sạch `FaqResponse`.*
> * *ServiceImpl `return` danh sách DTO lên Controller.*
> * *Controller gửi phản hồi HTTP 200 OK (JSON) về cho Frontend, React render các câu hỏi thường gặp dạng Accordion mở rộng ạ."*

---

### // LUỒNG 2: TRỢ LÝ AI HỖ TRỢ THÔNG MINH RAG CHATBOT 12 BƯỚC (UC-65)
*(Vị trí code: `AiController.java:50`, `AiServiceImpl.java:114`, `AiProviderRouter.java:40`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 2: Trợ lý Ảo AI Hỗ trợ Thông minh RAG 12 Bước** do em tự thiết kế:
> 
> ⬇️ **Chiều đi xuống (Request & Truy vấn Tri thức):**
> 1. *Tại **Frontend UI** (`AiFloatingWidget.tsx`), người dùng gửi câu hỏi, React đóng gói JSON gửi request `POST /api/ai/chat`.*
> 2. *Tại **REST Controller** (`AiController.java`), endpoint tiếp nhận và kiểm tra `@Valid` DTO, ủy quyền cho Service Interface `aiService.chat(request)`.*
> 3. *Tại **Service Interface** (`AiService.java`), định nghĩa chữ ký hàm nhận Request DTO và trả về Response DTO.*
> 4. *Tại **Service Impl** (`AiServiceImpl.java`), quy trình 12 bước nghiệp vụ RAG được kích hoạt:
>    - Bước 1 & 2: Gọi `aiChatMessageRepository.save(userMsg)` lưu câu hỏi vào CSDL.
>    - Bước 3 & 4: Gọi `aiKnowledgeChunkRepository` truy vấn vector tri thức bằng thuật toán Cosine Similarity để tìm đoạn tài liệu phù hợp nhất.
>    - **Bước 5.1 (Real-time Business Context Injection)**: ServiceImpl gọi trực tiếp `tutoringClassRepository.count()` và `tutorRepository.count()` để lấy số liệu thực tế thời gian thực từ MySQL (số lớp đang tuyển, số gia sư đã duyệt) và tiêm thẳng vào Prompt.
>    - Bước 6, 7 & 8: Gửi Prompt hoàn chỉnh qua `AiProviderRouter.chat()` tới Google Gemini API.
>    - **Bước 9 (Hallucination Guard)**: chạy bộ lọc đối soát câu trả lời từ AI với dữ liệu CSDL để triệt tiêu hiện tượng bịa đặt số liệu.*
> 5. *Tại **Repository**, `AiChatMessageRepository.save()` thực thi SQL INSERT lưu câu trả lời của Bot.*
> 6. *Tại **Database & Entity**, MySQL lưu câu hỏi và câu trả lời vào bảng `ai_chat_messages`, đọc tri thức từ bảng `ai_knowledge_chunks`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `AiChatMessage saved` cho Repository.*
> * *ServiceImpl đóng gói câu trả lời cùng các nguồn trích dẫn tài liệu tham khảo thành DTO sạch `AiChatResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller gửi phản hồi HTTP 200 OK (JSON) về trình duyệt, React hiển thị bong bóng chat thông minh kèm nguồn dẫn minh bạch ạ."*

---

### // LUỒNG 3: NGƯỜI DÙNG TẠO TICKET HỖ TRỢ & TỰ ĐỘNG TÍNH HẠN SLA (UC-65, UC-66)
*(Vị trí code: `MessagingController.java:76`, `MessagingServiceImpl.java:132`, `SupportTicketRepository.java:19`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 3: Người dùng tạo Ticket hỗ trợ & Thuật toán tính hạn SLA**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`MessagingPanel.tsx`), người dùng chọn danh mục sự cố, nhập tiêu đề, mô tả và đính kèm bằng chứng, gửi request `POST /api/messaging/support-tickets`.*
> 2. *Tại **REST Controller** (`MessagingController.java`), endpoint tiếp nhận và kiểm tra `@Valid` DTO, gọi sang Service Interface `messagingService.createSupportTicket()`.*
> 3. *Tại **Service Interface** (`MessagingService.java`), cung cấp chữ ký hàm hợp đồng trừu tượng.*
> 4. *Tại **Service Impl** (`MessagingServiceImpl.java`), phương thức được bảo vệ bởi `@Transactional` và thực thi:
>    - Lấy danh tính người gọi qua `authHelper.currentUserId()`.
>    - Thuật toán **Ép sàn mức ưu tiên** (`escalatePriority`): nếu danh mục là Lỗi hệ thống (`SYSTEM_ERROR`) hoặc Tranh chấp học phí (`DISPUTE`), dù người dùng chọn mức Thấp, hệ thống vẫn tự động ép sàn nâng lên tối thiểu mức `HIGH`.
>    - Thuật toán **Tính hạn cam kết SLA** (`calculateDueAt`): dựa trên độ ưu tiên, hệ thống cộng thêm từ 4h đến 48h vào thời điểm hiện tại làm mốc `dueAt`.
>    - Gọi `supportTicketRepository.save(ticket)` để tạo ticket và gọi `ticketMessageRepository.save(firstMessage)` để lưu tin nhắn mở đầu.
>    - Gọi sang Service liên module `notificationDispatchService.notifyUserFromTemplate(...)` để phát thông báo In-App thời gian thực tới tất cả Admin sàn.*
> 5. *Tại **Repository**, `SupportTicketRepository.save()` và `TicketMessageRepository.save()` sinh các câu lệnh SQL INSERT.*
> 6. *Tại **Database & Entity**, MySQL ghi 2 dòng bản ghi mới vào bảng `support_tickets` và `ticket_messages`. Đây là điểm quay đầu của dữ liệu.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về đối tượng Entity JPA thô `SupportTicket saved` cho Repository.*
> * *ServiceImpl gọi hàm mapper nội bộ `toResponse(saved)`. Hàm này bóc tách dữ liệu từ Entity thô, che giấu các khóa ngoại nội bộ và đóng gói thành DTO sạch `SupportTicketResponse`.*
> * *ServiceImpl return DTO sạch lên Controller.*
> * *Controller gửi phản hồi HTTP 201 Created (JSON) về trình duyệt để React hiển thị thông báo tạo ticket thành công kèm đồng hồ đếm ngược SLA ạ."*

---

### // LUỒNG 4: ADMIN TIẾP NHẬN, XỬ LÝ & ĐO LƯỜNG RESPONSE SLA (UC-66)
*(Vị trí code: `PlatformController.java:325`, `PlatformServiceImpl.java:1526`, `SupportTicketRepository.java:24`)*

> *"Dạ thưa thầy cô, **LUỒNG 4: Admin tiếp nhận, xử lý & Đo lường Response SLA** bao gồm 4 mắt xích liên hoàn:*
> 
> #### 4A. Lọc & Xem danh sách Ticket phân trang
> * *Frontend `PlatformTicketsPage.tsx` gửi `GET /api/platform/tickets` ➡️ `PlatformController.getTickets()` ➡️ `PlatformServiceImpl.getTickets()` gọi `supportTicketRepository.search(status, category, priority, pageable)` thực thi JPQL ➡️ MySQL quét bảng `support_tickets` ➡️ Chiều về: ServiceImpl dùng `.map(this::toResponse)` chuyển đổi thành `Page<SupportTicketResponse>` trả về Controller hiển thị bảng phân trang.*
> 
> #### 4B. Mở Ticket & Tự động nhận bàn giao (Auto-assign PIC)
> * *Khi Admin click mở xem chi tiết ticket, gửi `GET /api/platform/tickets/{id}` ➡️ `PlatformServiceImpl.getTicketDetail()` gọi `supportTicketRepository.findById(ticketId)`. Nếu ticket chưa có ai phụ trách, hệ thống **tự động gán Admin hiện tại vào `assignedAdmin`** và đổi `status = IN_PROGRESS`, sau đó gọi `supportTicketRepository.save(ticket)` cập nhật vào MySQL ➡️ Chiều về: ServiceImpl gọi `ticketMessageRepository.findByTicket_TicketIdOrderByCreatedAtAsc()` tải thêm tin nhắn, ghép thành DTO `SupportTicketDetailResponse` trả về Controller mở khung hội thoại.*
> 
> #### 4C. Gửi phản hồi & Đo lường First Response SLA
> * *Admin nhập tin nhắn phản hồi gửi `POST /api/platform/tickets/{id}/messages` ➡️ `PlatformServiceImpl.respondToTicket()` tự động đo lường: nếu đây là phản hồi đầu tiên, tính ngay `responseSlaMs = now - createdAt` để ghi nhận KPI trực ban ➡️ Gọi `ticketMessageRepository.save(msg)` lưu tin nhắn, gọi `supportTicketRepository.save(ticket)` cập nhật SLA, và gọi Service liên module `auditLogService.record("RESPOND_TICKET", ...)` ghi vết kiểm toán ➡️ Chiều về: ServiceImpl map sang DTO `TicketMessageResponse` đẩy ngay tin nhắn mới lên màn hình.*
> 
> #### 4D. Đóng / Giải quyết Ticket
> * *Admin bấm Đóng ticket gửi `PATCH /api/platform/tickets/{id}/status` ➡️ `PlatformServiceImpl.closeTicket()` gán `status = CLOSED`, mốc `closedAt = now`, gọi `supportTicketRepository.save(ticket)` và gọi `auditLogService.record("CLOSE_TICKET", ...)` ➡️ Chiều về: ServiceImpl chuyển đổi DTO `SupportTicketResponse` trả về cho React chuyển badge màu xám Đã đóng ạ."*

---

### // LUỒNG 5: GỘP TICKET TRÙNG LẶP & CHUYỂN SANG LUỒNG KHIẾU NẠI (UC-66, BF-08)
*(Vị trí code: `PlatformController.java:389`, `PlatformServiceImpl.java:1634`)*

> *"Dạ thưa thầy cô, **LUỒNG 5** giải quyết 2 bài toán điều phối nghiệp vụ nâng cao:*
> 
> #### 5A. Gộp Ticket trùng lặp (Merge Ticket - BF-08)
> ⬇️ **Chiều đi xuống:**
> 1. *Tại `PlatformTicketsPage.tsx`, Admin chọn ticket phụ và nhập ID ticket chính cần gộp vào, gửi `POST /api/platform/tickets/{id}/merge`.*
> 2. *`PlatformController.mergeTicket()` gọi `PlatformServiceImpl.mergeTicket()`.*
> 3. *ServiceImpl kiểm tra **4 chốt chặn an toàn**: cùng một người dùng tạo, 2 ticket khác nhau, ticket phụ chưa đóng, ticket chính đang mở.*
> 4. *Sau đó Service chuyển toàn bộ tin nhắn từ ticket phụ sang ticket chính qua lời gọi `ticketMessageRepository.saveAll(messages)`, đổi ticket phụ sang `status = CLOSED` và gọi `supportTicketRepository.saveAll(...)`.*
> 5. *Gọi Service liên module `auditLogService.record("MERGE_TICKET", ...)` dưới sự bảo vệ của `@Transactional`.*
> 6. *MySQL cập nhật hàng loạt bảng `ticket_messages`, `support_tickets` và `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity ticket chính đã gộp.*
> * *ServiceImpl đóng gói thành DTO `SupportTicketDetailResponse` trả về Controller để React tự động load lại giao diện ticket chính với đầy đủ lịch sử gộp.*
> 
> #### 5B. Chuyển Ticket sang Luồng Khiếu nại/Tranh chấp Escrow
> ⬇️ **Chiều đi xuống:**
> 1. *Admin bấm Chuyển sang Tranh chấp, gửi `POST /api/platform/tickets/{id}/redirect-dispute`.*
> 2. *`PlatformServiceImpl.redirectTicketToDispute()` gọi `reportRepository.save(report)` khởi tạo một thực thể Báo cáo sự cố `Report` mới gắn với lớp học (`targetType = CLASS`), gọi `supportTicketRepository.save(ticket)` đóng ticket hỗ trợ ban đầu và gọi `auditLogService.record("REDIRECT_TO_DISPUTE", ...)`.*
> 3. *MySQL ghi mới bảng `reports`, cập nhật bảng `support_tickets` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `Report` mới.*
> * *ServiceImpl đóng gói thành DTO trả về Controller để React hiển thị thông báo và cung cấp đường link chuyển hướng sang trang Xử lý sự cố lớp học ạ."*

---

### // LUỒNG 6: QUẢN TRỊ TRI THỨC FAQ - ADMIN CRUD & PHÊ DUYỆT BẢN NHÁP (UC-67)
*(Vị trí code: `CatalogController.java:88`, `CatalogServiceImpl.java:316`, `FaqEntryRepository.java:38`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 6: Quản trị Tri thức FAQ - Admin CRUD & Phê duyệt Bản nháp**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformFaqPage.tsx`), Admin soạn câu hỏi, câu trả lời bằng Markdown, chọn lưu Nháp hoặc Công khai, gửi `POST /api/catalog/admin/faqs`.*
> 2. *Tại **REST Controller** (`CatalogController.java`), gọi sang `catalogService.createFaqEntry(request)`.*
> 3. *Tại **Service Interface** (`CatalogService.java`), định nghĩa hàm CRUD bài viết FAQ.*
> 4. *Tại **Service Impl** (`CatalogServiceImpl.java`), hệ thống gán số thứ tự hiển thị `displayOrder`, thiết lập cờ xuất bản `isPublished`, gọi `faqEntryRepository.save(faq)` và gọi Service liên module `auditLogService.record("CREATE_FAQ", ...)`.*
> 5. *Tại **Repository**, `FaqEntryRepository.save()` sinh câu lệnh SQL INSERT/UPDATE.*
> 6. *Tại **Database & Entity**, MySQL ghi nhận bản ghi mới vào bảng `faq_entries` và bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `FaqEntry saved` cho Repository.*
> * *ServiceImpl gọi mapper `toFaqAdmin` đóng gói thành DTO `FaqEntryAdminResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React bổ sung ngay câu hỏi mới vào bảng quản trị FAQ ạ."*

---

### // LUỒNG 7: QUÉT ĐỊNH KỲ & NÂNG CẤP KHẨN CẤP TICKET QUÁ HẠN SLA (JOB-11)
*(Vị trí code: `TicketSlaScheduler.java:11`, `PlatformServiceImpl.java:1721`, `SupportTicketRepository.java:48`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 7: Quét định kỳ & Nâng cấp khẩn cấp Ticket quá hạn SLA (JOB-11)**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Cron job chạy định kỳ tự động qua `TicketSlaScheduler` (hoặc Admin bấm nút quét trên UI), gửi request `POST /api/platform/tickets/sla/scan`.*
> 2. *Tại **REST Controller** (`PlatformController.java`), gọi `platformService.scanAndEscalateSlaBreaches()`.*
> 3. *Tại **Service Interface** (`PlatformService.java`), định nghĩa hàm quét vi phạm SLA.*
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`), việc đầu tiên là Service gọi xuống Repository:  
>    `supportTicketRepository.findBreachedCandidateTickets(now)`  
>    để tìm tất cả các ticket chưa đóng mà có thời hạn cam kết `dueAt < now`.*
> 5. *Với mỗi ticket quá hạn, Service **gán cờ vi phạm `slaBreached = true` và tự động nâng độ ưu tiên lên `URGENT`**, sau đó gọi `supportTicketRepository.saveAll(breachedList)` và gọi `auditLogService.record("SLA_BREACH_ESCALATED", ...)`.*
> 6. *Tại **Database & Entity**, MySQL cập nhật các cột `sla_breached` và `priority` trong bảng `support_tickets`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về danh sách các Entity ticket vi phạm đã cập nhật.*
> * *ServiceImpl tổng hợp số lượng, đóng gói thành DTO `SlaScanSummaryResponse(scannedCount, escalatedCount)`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị toast thông báo số lượng ticket vi phạm đã được đưa vào diện khẩn cấp ạ."*

---

### // LUỒNG 8: BẢNG ĐIỀU KHIỂN QUẢN TRỊ & HÀNG ĐỢI NHIỆM VỤ TRỰC BAN KHẨN CẤP (UC-56, UC-64)
*(Vị trí code: `PlatformController.java:158`, `PlatformServiceImpl.java:451`, `PlatformTaskQueueServiceImpl.java:65`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 8: Bảng điều khiển Quản trị & Hàng đợi Nhiệm vụ Trực ban Khẩn cấp**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformDashboardPage.tsx`), khi Admin mở trang Dashboard, React gửi `GET /api/platform/dashboard`.*
> 2. *Tại **REST Controller** (`PlatformController.java`), gọi `platformService.getDashboard()`.*
> 3. *Tại **Service Interface** (`PlatformService.java`), định nghĩa hàm lấy dữ liệu KPI.*
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`) kết hợp với `PlatformTaskQueueServiceImpl`, ServiceImpl triệu hồi đồng thời các Repository:
>    - Gọi `userRepository.count()` đếm người dùng.
>    - Gọi `tutoringClassRepository.count()` đếm lớp học đang mở.
>    - Gọi `escrowTransactionRepository.sumTotalEscrow()` tính tổng tiền đang ký quỹ.
>    - Gọi `supportTicketRepository.countUrgent()` để gom các việc khẩn cấp.*
> 5. *Tại **Repository**, các câu lệnh SQL `COUNT()` và `SUM()` được thực thi.*
> 6. *Tại **Database & Entity**, MySQL quét các bảng tương ứng và tính toán chỉ số.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về các con số thống kê thô cho Repository.*
> * *ServiceImpl gom toàn bộ số liệu và danh sách tác vụ khẩn cấp vào DTO `PlatformDashboardResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React render các thẻ Card KPI và bảng hàng đợi trực ban khẩn cấp ạ."*

---

### // LUỒNG 9: QUẢN LÝ CÂY DANH MỤC HỆ THỐNG PHÂN CẤP ĐỆ QUY (UC-57)
*(Vị trí code: `CatalogController.java:138`, `CatalogServiceImpl.java:422`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 9: Quản lý Cây Danh mục Hệ thống Phân cấp Đệ quy O(N)**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformCategoriesPage.tsx`), khi mở trang, gửi `GET /api/catalog/categories/tree`.*
> 2. *Tại **REST Controller** (`CatalogController.java`), gọi `catalogService.getCategoryTree()`.*
> 3. *Tại **Service Interface** (`CatalogService.java`), định nghĩa hàm lấy cây danh mục.*
> 4. *Tại **Service Impl** (`CatalogServiceImpl.java`), để tránh lỗi truy vấn lặp $N+1$, ServiceImpl **chỉ gọi xuống Tầng 5 Repository đúng 1 lần duy nhất**: `subjectCategoryRepository.findAll()` để kéo toàn bộ danh mục phẳng lên RAM.*
> 5. *Tại **Repository**, `SubjectCategoryRepository.findAll()` sinh câu lệnh SELECT đơn giản.*
> 6. *Tại **Database & Entity**, MySQL đọc toàn bộ bảng `subject_categories`.*
> 
> ⬆️ **Chiều đi ngược lên (Thuật toán HashMap & DTO Mapping):**
> * *Database trả về danh sách Entity phẳng `List<SubjectCategory>` cho Repository.*
> * *Tại ServiceImpl, em áp dụng thuật toán **`HashMap` trong bộ nhớ RAM** để ánh xạ và nhóm các nút con vào nút cha theo mã cha `parentId`, đạt độ phức tạp thuật toán tối ưu $O(N)$ mà không gọi database đệ quy.*
> * *ServiceImpl biến đổi cấu trúc phẳng thành cây phân cấp đa tầng DTO `List<CategoryNodeResponse>`.*
> * *ServiceImpl return cây DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị Tree View trực quan cho người dùng ạ."*

---

### // LUỒNG 10: THAM SỐ CẤU HÌNH HỆ THỐNG TOÀN CỤC & TỶ LỆ PHÍ SÀN (UC-58)
*(Vị trí code: `SystemParameterController.java:52`, `SystemParameterServiceImpl.java:51`, `CatalogServiceImpl.java:520`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 10: Tham số Cấu hình Hệ thống Toàn cục & Tỷ lệ Phí sàn**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformSettingsPage.tsx`), Admin thay đổi tỷ lệ phí sàn (ví dụ: 10%), gửi `PUT /api/catalog/admin/parameters/{key}`.*
> 2. *Tại **REST Controller** (`SystemParameterController.java`), gọi `systemParameterService.updateParameter()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm cập nhật tham số.*
> 4. *Tại **Service Impl** (`SystemParameterServiceImpl.java`), kiểm tra định dạng giá trị, gọi `systemParameterRepository.save(param)` và gọi `auditLogService.record("UPDATE_SYSTEM_PARAM", ...)`.*
> 5. *Tại **Repository**, `SystemParameterRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database & Entity**, MySQL cập nhật giá trị trong bảng `system_parameters` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `SystemParameter saved`.*
> * *ServiceImpl đóng gói thành DTO `SystemParameterResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị thông báo đã cập nhật tham số thành công ạ."*

---

### // LUỒNG 11: GIÁM SÁT NHẬT KÝ KIỂM TOÁN & SO VẾT THAY ĐỔI JSON DIFF (UC-61)
*(Vị trí code: `AuditLogController.java:30`, `AuditLogServiceImpl.java:71`, `AuditLogRepository.java:15`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 11: Giám sát Nhật ký Kiểm toán Bất biến & So vết JSON Diff**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformAuditLogsPage.tsx`), Admin xem lịch sử tác động hệ thống, gửi `GET /api/platform/audit-logs`.*
> 2. *Tại **REST Controller** (`AuditLogController.java`), gọi `auditLogService.getAuditLogs()`.*
> 3. *Tại **Service Interface** (`AuditLogService.java`), định nghĩa hàm tra cứu và hàm ghi vết nội bộ `record()`.*
> 4. *Tại **Service Impl** (`AuditLogServiceImpl.java`):
>    - Khi đọc: gọi `auditLogRepository.search(action, entityName, pageable)` để lọc phân trang.
>    - Khi ghi: hàm `record()` tuân thủ nguyên tắc **Bất biến (Append-only)**: gọi `auditLogRepository.save(log)` chỉ thực thi lệnh INSERT, cấm tuyệt đối UPDATE hoặc DELETE.*
> 5. *Tại **Repository**, `AuditLogRepository.search()` để đọc và `save()` để ghi.*
> 6. *Tại **Database & Entity**, MySQL đọc/ghi trực tiếp vào bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về danh sách Entity `Page<AuditLog>`.*
> * *ServiceImpl map sang DTO `AuditLogResponse` chứa dữ liệu so vết JSON Diff (Trước/Sau).*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React render bảng nhật ký kèm modal xem vết thay đổi chi tiết ạ."*

---

### // LUỒNG 12: BÁO CÁO TÀI CHÍNH ĐA CHIỀU & XUẤT DỮ LIỆU CSV AN TOÀN (UC-41, UC-43)
*(Vị trí code: `PlatformAnalyticsController.java:80`, `PlatformAnalyticsServiceImpl.java:374`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 12: Báo cáo Tài chính Đa chiều & Xuất Dữ liệu CSV An toàn**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformAnalyticsPage.tsx`), Admin bấm 'Xuất file CSV', gửi `GET /api/platform/analytics/export-csv`.*
> 2. *Tại **REST Controller** (`PlatformAnalyticsController.java`), gọi `platformAnalyticsService.exportCsv()`.*
> 3. *Tại **Service Interface** (`PlatformAnalyticsService.java`), định nghĩa hàm xuất báo cáo.*
> 4. *Tại **Service Impl** (`PlatformAnalyticsServiceImpl.java`), ServiceImpl gọi xuống Repository: `paymentTransactionRepository.find...` và `escrowTransactionRepository.find...` lấy dữ liệu giới hạn 10k dòng để tránh tràn RAM.*
> 5. *ServiceImpl áp dụng **2 kỹ thuật an toàn thông tin cốt lõi**:
>    - Thứ nhất, ghi 3 byte BOM `\uFEFF` ở đầu file để phần mềm Excel mở tiếng Việt có dấu không bao giờ bị lỗi font.
>    - Thứ hai, chạy hàm `escapeCsv()` làm sạch các ký tự nguy hiểm đứng đầu ô như `=`, `+`, `-`, `@` để **triệt tiêu lỗ hổng tấn công DDE Formula Injection**.*
> 6. *Tại **Database & Entity**, MySQL đọc bảng `payment_transactions` và `escrow_transactions`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về các dòng Entity giao dịch.*
> * *ServiceImpl chuyển đổi các dòng dữ liệu thành luồng byte CSV sạch (`byte[]`).*
> * *ServiceImpl return mảng byte kèm định dạng `text/csv` lên Controller.*
> * *Controller trả về file stream kèm header `Content-Disposition`, trình duyệt tự động tải tệp CSV về máy tính ạ."*

---

### // LUỒNG 13: PHÁT HIỆN HÀNH VI LÁCH NỀN TẢNG (CIRCUMVENTION - UC-59) & BAN HÀNH QUYẾT ĐỊNH XỬ PHẠT (UC-60)
*(Vị trí code: `CircumventionServiceImpl.java:59`, `PenaltyServiceImpl.java:101`, `PenaltyAccessServiceImpl.java:45`)*

> *"Dạ thưa thầy cô, **LUỒNG 13** là vũ khí an ninh bảo vệ doanh thu sàn gồm 2 mắt xích:
> 
> #### 13A. Phát hiện lách sàn bằng Regex thời gian thực (UC-59)
> ⬇️ **Chiều đi xuống:**
> 1. *Khi người dùng chat hoặc gửi nội dung, hàm `CircumventionServiceImpl.inspect()` được kích hoạt ngầm.*
> 2. *Tại `CircumventionServiceImpl.java`, hệ thống chạy **4 bộ lọc Regex thời gian thực**: Số điện thoại (80đ), Email (90đ), URL/Website (70đ), Mạng xã hội Zalo/Telegram (65đ).*
> 3. *Nếu điểm rủi ro tổng hợp $\ge 65$, ServiceImpl gọi `circumventionDetectionRepository.save()` lưu vi phạm vào bảng `circumvention_detections`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `CircumventionDetection saved`.*
> * *ServiceImpl đóng gói thành DTO `CircumventionDetectionResponse` trả về Controller để React hiển thị danh sách trường hợp nghi vấn kèm điểm rủi ro cho Admin thanh tra.*
> 
> #### 13B. Ban hành quyết định xử phạt & Chốt chặn tính năng (UC-60)
> ⬇️ **Chiều đi xuống:**
> 1. *Admin chọn hình thức phạt trên `PlatformPenaltiesPage.tsx`, gửi `POST /api/platform/penalties`.*
> 2. *`PenaltyServiceImpl.createPenalty()` gọi `userPenaltyRepository.save()` lưu án phạt vào bảng `user_penalties` và gọi `auditLogService.record("CREATE_PENALTY", ...)`.*
> 
> ⬆️ **Chiều đi ngược lên & Chốt chặn quyền liên module:**
> * *Ở các module khác (như Chat hoặc Nhận lớp), hàm `PenaltyAccessServiceImpl.requireFeature()` sẽ gọi `userPenaltyRepository.findByUser_UserIdAndStatus()`. Nếu người dùng đang chịu án phạt còn hiệu lực, hệ thống lập tức ném lỗi `ForbiddenException` (403 Forbidden) chặn ngay hành động ạ!"*

---

# 👥 PHẦN 2: KỊCH BẢN NÓI CÁC USE CASE NGHIỆP VỤ & BẢO MẬT BỔ TRỢ (LUỒNG 14 ➡️ 27)

---

### LUỒNG 14: QUẢN LÝ NGƯỜI DÙNG & KHÓA TÀI KHOẢN TỨC THÌ (UC-07)
*(Vị trí code: `PlatformController.java:135`, `PlatformServiceImpl.java:344`, `JwtAuthenticationFilter.java:43`)*

> *"Dạ thưa thầy cô, luồng khóa tài khoản vi phạm và thu hồi phiên đăng nhập:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformUsersPage.tsx`), Admin chọn Khóa tài khoản (`BANNED`) kèm lý do giải trình, gửi `PATCH /api/platform/users/{id}/status`.*
> 2. *Tại **REST Controller** (`PlatformController.java`), gọi `platformService.updateUserStatus()`.*
> 3. *Tại **Service Interface** (`PlatformService.java`), định nghĩa hàm đổi trạng thái user.*
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`), ServiceImpl gọi `findUserOrThrow(userId)`, kiểm tra cấm khóa Root Admin, đổi trạng thái user sang `BANNED`, gọi `userRepository.save(user)` và gọi Service liên module `auditLogService.record("UPDATE_USER_STATUS", ...)`.*
> 5. *Tại **Repository**, `UserRepository.save()` sinh câu lệnh SQL UPDATE.*
> 6. *Tại **Database & Entity**, MySQL cập nhật cột `status = 'BANNED'` trong bảng `users` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên & Lớp bảo mật Filter:**
> * *Database trả về Entity `User saved`.*
> * *ServiceImpl dùng mapper `platformMapper.toUserListItem()` che giấu mã băm mật khẩu, trả về DTO sạch `UserListItemResponse` hiển thị badge đỏ BANNED trên React.*
> * *Tại **Cổng lọc bảo mật** `JwtAuthenticationFilter`: mỗi request tiếp theo gửi lên, filter kiểm tra `userDetails.isEnabled() == false` sẽ lập tức từ chối 401 Unauthorized ngay tại cửa ngõ mà không cần chờ token JWT hết hạn ạ!"*

---

### LUỒNG 15: THẨM ĐỊNH eKYC CCCD / BẰNG CẤP GIA SƯ (UC-54)
*(Vị trí code: `PlatformController.java:199`, `PlatformServiceImpl.java:542`)*

> *"Dạ thưa thầy cô, luồng xét duyệt hồ sơ định danh gia sư:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformVerificationsPage.tsx`), Admin soi ảnh 2 mặt CCCD và bằng cấp, bấm Duyệt/Từ chối, gửi `POST /api/platform/verifications/{id}/review`.*
> 2. *Tại **REST Controller** (`PlatformController.java`), gọi `platformService.reviewVerification()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm duyệt hồ sơ eKYC.*
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`), gọi `verificationRequestRepository.save(request)` lưu lịch sử thẩm định, gọi `tutorRepository.save(tutor)` cập nhật trạng thái gia sư sang `VERIFIED` và gọi `auditLogService.record("REVIEW_VERIFICATION", ...)`.*
> 5. *Tại **Repository**, `VerificationRequestRepository` và `TutorRepository` thực thi SQL UPDATE.*
> 6. *Tại **Database & Entity**, MySQL cập nhật bảng `verification_requests`, cột `verification_status` bảng `tutors` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity yêu cầu xác thực đã cập nhật.*
> * *ServiceImpl đóng gói thành DTO `VerificationReviewResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React cập nhật tích xanh uy tín cho gia sư ạ."*

---

### LUỒNG 16: XỬ LÝ SỰ CỐ LỚP HỌC & ĐÓNG BĂNG ESCROW (UC-30)
*(Vị trí code: `PlatformController.java:271`, `PlatformServiceImpl.java:1726`, `EscrowServiceImpl.java`)*

> *"Dạ thưa thầy cô, quy trình giải quyết sự cố lớp học và can thiệp dòng tiền:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformReportsPage.tsx`), Admin chọn phương án can thiệp sự cố lớp học, gửi `PATCH /api/platform/reports/{id}/resolve`.*
> 2. *Tại **REST Controller** (`PlatformController.java`), gọi `platformService.resolveClassIssueReport()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm xử lý báo cáo sự cố.*
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`), hệ thống thực thi 1 trong 7 hành động can thiệp. Nếu là sự cố nghiêm trọng:
>    - Gọi Service liên module `escrowService.holdForDispute()` để **chuyển tiền học phí của lớp sang trạng thái `ON_HOLD`** trong bảng `escrow_transactions`.
>    - Gọi `disputeRepository.save(dispute)` tạo bản ghi tranh chấp mới.
>    - Gọi `reportRepository.save(report)` đóng báo cáo.
>    - Gọi Service liên module `contractService.recomputeTutorReputation()` để trừ điểm uy tín gia sư.*
> 5. *Tại **Repository**, các repository tương ứng thực thi câu lệnh SQL UPDATE/INSERT.*
> 6. *Tại **Database & Entity**, MySQL cập nhật bảng `reports`, `escrow_transactions`, ghi bảng `disputes` và cập nhật bảng `tutors`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity báo cáo đã giải quyết.*
> * *ServiceImpl đóng gói thành DTO `ResolveReportResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị trạng thái tiền đã được phong tỏa an toàn ạ."*

---

### LUỒNG 17: QUẢN LÝ MẪU HỢP ĐỒNG MASTER
*(Vị trí code: `PlatformController.java:480`, `PlatformServiceImpl.java:2265`)*

> *"Dạ thưa thầy cô, quy trình quản trị mẫu hợp đồng khung:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformContractTemplatesPage.tsx`), Admin soạn nội dung hợp đồng khung, gửi `POST /api/platform/contract-templates`.*
> 2. *Tại **REST Controller** (`PlatformController.java`), gọi `platformService.createContractTemplate()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm tạo mẫu hợp đồng.*
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`), gán phiên bản hiệu lực, gọi `contractTemplateRepository.save(template)` và gọi `auditLogService.record("CREATE_CONTRACT_TEMPLATE", ...)`.*
> 5. *Tại **Repository**, `ContractTemplateRepository.save()` sinh SQL INSERT.*
> 6. *Tại **Database & Entity**, MySQL ghi nhận dòng mới vào bảng `contract_templates` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `ContractTemplate saved`.*
> * *ServiceImpl đóng gói thành DTO `ContractTemplateResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React bổ sung mẫu hợp đồng mới vào danh sách sử dụng ạ."*

---

### LUỒNG 18: CẤU HÌNH TỶ LỆ PHÍ ĐỐI TÁC TRUNG TÂM
*(Vị trí code: `PlatformController.java:420`, `PlatformServiceImpl.java:2549`)*

> *"Dạ thưa thầy cô, luồng thiết lập tỷ lệ hoa hồng riêng cho từng trung tâm gia sư:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformFeeSettingsPage.tsx`), Admin nhập mức phí chiết khấu riêng, gửi `PUT /api/platform/centers/{id}/fee`.*
> 2. *Tại **REST Controller** (`PlatformController.java`), gọi `platformService.updateCenterFeeConfig()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm cấu hình phí trung tâm.*
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`), kiểm tra ngưỡng tỷ lệ hợp lệ, gọi `tutorCenterRepository.save(center)` cập nhật và gọi `auditLogService.record("UPDATE_CENTER_FEE", ...)`.*
> 5. *Tại **Repository**, `TutorCenterRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database & Entity**, MySQL cập nhật cột `custom_fee_rate` trong bảng `tutor_centers` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `TutorCenter saved`.*
> * *ServiceImpl đóng gói thành DTO `TutorCenterFeeResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị tỷ lệ phí mới đã áp dụng ạ."*

---

### LUỒNG 19: BẢN TIN & THÔNG BÁO TOÀN SÀN (UC-59)
*(Vị trí code: `AnnouncementController.java:45`, `AnnouncementServiceImpl.java:45`)*

> *"Dạ thưa thầy cô, luồng phát thông báo bảo trì và chính sách toàn sàn:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`PlatformTasksPage.tsx`), Admin soạn thông báo và chọn nhóm mục tiêu, gửi `POST /api/platform/announcements`.*
> 2. *Tại **REST Controller** (`AnnouncementController.java`), gọi `announcementService.upsertAnnouncement()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm tạo bản tin.*
> 4. *Tại **Service Impl** (`AnnouncementServiceImpl.java`), gọi `announcementRepository.save(announcement)` lưu bản tin, gọi Service liên module `notificationDispatchService.notifyUserFromTemplate(...)` phát thông báo In-App realtime cho đối tượng chỉ định (`ALL`, `TUTOR`, `CLIENT`), và ghi `audit_logs`.*
> 5. *Tại **Repository**, `AnnouncementRepository.save()` thực thi SQL INSERT/UPDATE.*
> 6. *Tại **Database & Entity**, MySQL ghi bảng `announcements`, bảng `notifications` và `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `Announcement saved`.*
> * *ServiceImpl đóng gói thành DTO `AnnouncementResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React hiển thị banner thông báo nổi bật trên trang chủ ạ."*

---

### LUỒNG 20: HỢP ĐỒNG ĐIỆN TỬ KÝ SỐ OTP (UC-44)
*(Vị trí code: `ContractController.java:80`, `ContractServiceImpl.java:476`)*

> *"Dạ thưa thầy cô, quy trình ký kết hợp đồng điện tử qua mã OTP:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`ContractDetailPage.tsx`), người dùng nhập mã OTP 6 số từ Email, gửi `POST /api/contract/{id}/sign-otp`.*
> 2. *Tại **REST Controller** (`ContractController.java`), gọi `contractService.signWithOtp()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm ký số OTP.*
> 4. *Tại **Service Impl** (`ContractServiceImpl.java`):
>    - Gọi `emailOtpRepository` xác thực mã OTP còn hạn và chưa bị tiêu thụ.
>    - Gọi `contractSignatureRepository.save(sig)` ghi nhận chữ ký số của bên ký.
>    - **Nếu cả 2 bên cùng ký hoàn tất**: chuyển trạng thái hợp đồng sang `SIGNED`, gọi `contractRepository.save(contract)`.
>    - Đồng thời kích hoạt sự kiện khóa tiền Escrow của lớp học.*
> 5. *Tại **Repository**, các repository tương ứng thực thi câu lệnh SQL.*
> 6. *Tại **Database & Entity**, MySQL ghi bảng `contract_signatures`, cập nhật cột `status = 'SIGNED'` trong bảng `contracts`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity hợp đồng đã hoàn tất.*
> * *ServiceImpl chuyển đổi thành DTO `ContractDetailResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị con dấu chứng thực điện tử màu xanh trên hợp đồng ạ."*

---

### LUỒNG 21: ĐÁNH GIÁ SAU BUỔI HỌC & ĐIỂM UY TÍN GIA SƯ
*(Vị trí code: `ContractController.java:48`, `ReviewServiceImpl.java:45`, `ContractServiceImpl.java:2580`)*

> *"Dạ thưa thầy cô, luồng đánh giá sao và cập nhật điểm uy tín:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`ContractDetailPage.tsx`), học viên chấm điểm sao (1 - 5 sao) và nhận xét, gửi `POST /api/contract/reviews`.*
> 2. *Tại **REST Controller** (`ContractController.java`), gọi `reviewService.createReview()`.*
> 3. *Tại **Service Interface**, `ReviewService` định nghĩa tạo đánh giá, `ContractService` định nghĩa tính uy tín.*
> 4. *Tại **Service Impl** (`ReviewServiceImpl.java`), gọi `reviewRepository.save(review)` lưu vào bảng `reviews`. Sau đó kích hoạt gọi sang Service liên module `contractService.recomputeTutorReputation(tutorId)` để tính lại trung bình cộng số sao của gia sư trên toàn sàn và gọi `tutorRepository.save(tutor)` cập nhật vào hồ sơ.*
> 5. *Tại **Repository**, `ReviewRepository.save()` và `TutorRepository.save()`.*
> 6. *Tại **Database & Entity**, MySQL ghi bảng `reviews` và cập nhật cột `rating_avg` trong bảng `tutors`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `Review saved`.*
> * *ServiceImpl đóng gói thành DTO `ReviewResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React cập nhật điểm sao mới trên trang hồ sơ công khai của gia sư ạ."*

---

### LUỒNG 22: QUÊN MẬT KHẨU OTP QUA EMAIL (DEF-62)
*(Vị trí code: `IdentityController.java:75`, `IdentityServiceImpl.java:519`)*

> *"Dạ thưa thầy cô, luồng đặt lại mật khẩu an toàn qua OTP Email:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`ResetPasswordPage.tsx`), người dùng nhập mã xác thực OTP từ Email và mật khẩu mới, gửi `POST /api/identity/reset-password`.*
> 2. *Tại **REST Controller** (`IdentityController.java`), gọi `identityService.resetPassword()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm reset password.*
> 4. *Tại **Service Impl** (`IdentityServiceImpl.java`):
>    - Gọi `passwordResetTokenRepository.findByToken(request.getToken())` kiểm tra token còn hạn và chưa dùng.
>    - Dùng thư viện `passwordEncoder.encode(request.getNewPassword())` băm mật khẩu mới bằng BCrypt.
>    - Gọi `userRepository.save(user)` lưu mật khẩu mới.
>    - Đánh dấu token đã dùng: gọi `passwordResetTokenRepository.save(token)`.
>    - Gọi Service liên module `auditLogService.record("RESET_PASSWORD", ...)` ghi nhật ký kiểm toán.*
> 5. *Tại **Repository**, `UserRepository` và `PasswordResetTokenRepository` thực thi SQL UPDATE.*
> 6. *Tại **Database & Entity**, MySQL cập nhật cột `password_hash` bảng `users`, cập nhật bảng `password_reset_tokens` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `User` với mật khẩu mới.*
> * *ServiceImpl đóng gói thông điệp thành công vào DTO `ApiResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React tự động chuyển hướng người dùng về màn hình Đăng nhập ạ."*

---

### LUỒNG 23: THAY ĐỔI MẬT KHẨU NGƯỜI DÙNG (CHANGE PASSWORD)
*(Vị trí code: `IdentityController.java:65`, `IdentityServiceImpl.java:432`)*

> *"Dạ thưa thầy cô, luồng đổi mật khẩu trong trang cá nhân:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`ProfilePage.tsx`), người dùng nhập mật khẩu hiện tại và mật khẩu mới, gửi `POST /api/identity/change-password`.*
> 2. *Tại **REST Controller** (`IdentityController.java`), gọi `identityService.changePassword()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm đổi mật khẩu.*
> 4. *Tại **Service Impl** (`IdentityServiceImpl.java`):
>    - Lấy user hiện tại qua `authHelper.currentUserId()`.
>    - Dùng `passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())` để so khớp mật khẩu cũ.
>    - Mã hóa mật khẩu mới bằng `passwordEncoder.encode()`.
>    - Gọi `userRepository.save(user)` lưu mật khẩu mới.
>    - Gọi `auditLogService.record("CHANGE_PASSWORD", ...)`.*
> 5. *Tại **Repository**, `UserRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database & Entity**, MySQL cập nhật cột `password_hash` bảng `users` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `User` đã cập nhật hash.*
> * *ServiceImpl đóng gói DTO phản hồi thành công.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị toast thông báo đổi mật khẩu thành công ạ."*

---

### LUỒNG 24: CHAT TIN NHẮN TỨC THỜI 1-1 & NHÓM LỚP HỌC (UC-50)
*(Vị trí code: `ChatController.java:58`, `ChatServiceImpl.java:480`)*

> *"Dạ thưa thầy cô, luồng gửi tin nhắn thời gian thực:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`ChatWindow.tsx`), người dùng soạn tin nhắn văn bản, gửi `POST /api/messaging/chats/messages`.*
> 2. *Tại **REST Controller** (`ChatController.java`), gọi `chatService.sendMessage()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm gửi tin nhắn.*
> 4. *Tại **Service Impl** (`ChatServiceImpl.java`), hệ thống kiểm tra 2 tầng an ninh liên module:
>    - Gọi `penaltyAccessService.requireFeature(userId, "CHAT")` (chặn người đang bị phạt chat).
>    - Gọi `circumventionService.inspect(content)` (quét Regex lách sàn thời gian thực).
>    - Sau khi qua 2 chốt chặn an toàn, Service gọi `messageRepository.save(message)` lưu tin nhắn vào CSDL.*
> 5. *Tại **Repository**, `MessageRepository.save()` thực thi SQL INSERT.*
> 6. *Tại **Database & Entity**, MySQL ghi dòng mới vào bảng `messages`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `Message saved`.*
> * *ServiceImpl đóng gói thành DTO `ChatMessageResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React đẩy tin nhắn mới vào khung chat realtime ạ."*

---

### LUỒNG 25: TẢI ẢNH ĐẠI DIỆN KIỂM TRA MAGIC BYTES CHỐNG MÃ ĐỘC (UC-08)
*(Vị trí code: `ProfileController.java:45`, `ProfileServiceImpl.java:809`)*

> *"Dạ thưa thầy cô, luồng tải ảnh đại diện an toàn:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`TutorProfilePage.tsx`), người dùng chọn tệp ảnh tải lên, gửi `POST /api/profile/avatar` dạng `multipart/form-data`.*
> 2. *Tại **REST Controller** (`ProfileController.java`), gọi `profileService.uploadAvatar()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm upload avatar.*
> 4. *Tại **Service Impl** (`ProfileServiceImpl.java`):
>    - Đọc mảng byte đầu tiên của file để kiểm tra **Magic Bytes** thực tế (PNG: `89 50 4E 47`, JPEG: `FF D8 FF`), chặn đứng các tệp mã độc giả mạo đuôi ảnh (`.php`, `.exe`).
>    - Lưu tệp lên storage và gán URL vào `user.setAvatarUrl(url)`.
>    - Gọi `userRepository.save(user)`.*
> 5. *Tại **Repository**, `UserRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database & Entity**, MySQL cập nhật cột `avatar_url` trong bảng `users`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `User` đã có avatar mới.*
> * *ServiceImpl đóng gói thành DTO `AvatarUploadResponse` chứa URL ảnh.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React cập nhật ảnh đại diện mới trên thanh điều hướng ạ."*

---

### LUỒNG 26: ĐĂNG XUẤT & THU HỒI PHIÊN JWT BẰNG TOKEN_VERSION (LOGOUT)
*(Vị trí code: `IdentityController.java:55`, `IdentityServiceImpl.java:390`, `JwtAuthenticationFilter.java:45`)*

> *"Dạ thưa thầy cô, cơ chế thu hồi phiên JWT không cần Redis:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Người dùng bấm Đăng xuất, xóa token ở client và gửi `POST /api/identity/logout`.*
> 2. *Tại **REST Controller** (`IdentityController.java`), gọi `identityService.logout()`.*
> 3. *Tại **Service Interface**, định nghĩa hàm đăng xuất.*
> 4. *Tại **Service Impl** (`IdentityServiceImpl.java`), lấy user qua `authHelper.currentUserId()`, thực hiện thao tác cốt lõi:  
>    **`user.setTokenVersion(user.getTokenVersion() + 1L)`**, tăng giá trị phiên đăng nhập thêm 1 đơn vị, gọi `userRepository.save(user)` và gọi `auditLogService.record("LOGOUT", ...)`.*
> 5. *Tại **Repository**, `UserRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database & Entity**, MySQL cập nhật cột `token_version` trong bảng `users` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên & Cơ chế thu hồi Stateless:**
> * *Controller trả về HTTP 200 OK chuyển hướng người dùng về trang chủ.*
> * *Tại **Cổng lọc bảo mật** `JwtAuthenticationFilter`: mỗi request gửi lên đều so khớp:  
>   `principal.getTokenVersion() != jwtService.extractTokenVersion(claims)`  
>   Khi thấy version trong CSDL đã tăng lên lệch so với token cũ, request lập tức bị từ chối 401 Unauthorized, vô hiệu hóa toàn bộ token cũ ngay lập tức mà không cần tốn chi phí duy trì Redis blacklist ạ!"*

---

### LUỒNG 27: KÝ QUỸ ESCROW, VÍ ĐIỆN TỬ & QUYẾT TOÁN DÒNG TIỀN (UC-40)
*(Vị trí code: `FinanceController.java:50`, `EscrowServiceImpl.java`, `SettlementServiceImpl.java:43`)*

> *"Dạ thưa thầy cô, hạ tầng ký quỹ Escrow và quyết toán dòng tiền:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Khi hợp đồng được ký kết (UC-44) hoặc lớp học hoàn thành các buổi học, hệ thống kích hoạt xử lý tại `EscrowServiceImpl` và `SettlementServiceImpl`.*
> 2. *Khi ký hợp đồng: tiền học phí được khóa vào quỹ Escrow an toàn của sàn với trạng thái `HELD`, gọi `escrowTransactionRepository.save()` ghi nhận vào bảng `escrow_transactions`.*
> 3. *Khi hoàn thành lớp học: `SettlementServiceImpl` tự động trích 10% phí sàn (được cấu hình ở Luồng 10) chuyển về cho sàn TCS, và giải ngân 90% còn lại vào ví khả dụng của Gia sư/Trung tâm, gọi `walletRepository.save()` và chuyển trạng thái tiền ký quỹ sang `RELEASED`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database cập nhật bảng `wallets`, `wallet_transactions`, `escrow_transactions`.*
> * *ServiceImpl đóng gói thành DTO `WalletResponse` và `SettlementResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị số dư ví và lịch sử dòng tiền minh bạch ạ!"*

---

# 🏆 PHẦN 3: LỜI KẾT THÚC THUYẾT TRÌNH ẤN TƯỢNG (15 GIÂY)

> *"Dạ kính thưa quý thầy cô trong hội đồng, trên đây là toàn bộ kiến trúc 6 tầng và chu trình dữ liệu 2 chiều khép kín trong các phân hệ mà em trực tiếp đảm nhiệm và phát triển.*
> 
> *Em xin chân thành cảm ơn quý thầy cô đã chú ý lắng nghe và em rất sẵn sàng nhận các câu hỏi nhận xét, phản biện từ hội đồng ạ!"*
