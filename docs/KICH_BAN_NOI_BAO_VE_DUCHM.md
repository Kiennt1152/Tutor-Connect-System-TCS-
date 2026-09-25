# 🎙️ KỊCH BẢN THUYẾT TRÌNH BẢO VỆ ĐỒ ÁN CAPSTONE / SWP391
## DÀNH RIÊNG CHO: HOÀNG MINH ĐỨC (DucHM — HE187354 / `mduc1011-swp`)
* **Dự án**: Tutor Connect System (TCS) — Hệ thống kết nối Gia sư thông minh
* **Khung giờ bảo vệ chính thức**: **10:50 – 12:20, Thứ Bảy ngày 26/09/2026**
* **Kiến trúc thuyết trình**: Chu trình 2 Chiều Khép Kín qua 6 Tầng Kiến Trúc Spring Boot:
  `[Frontend UI]` ⬇️ `[Controller]` ⬇️ `[Interface]` ⬇️ `[ServiceImpl]` ⬇️ `[Repository]` ⬇️ `[Database MySQL]`
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

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 1: Tra cứu Danh mục FAQ Công khai**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`HelpPage.tsx`), khách vãng lai hoặc người dùng nhập từ khóa tìm kiếm câu hỏi thường gặp, React gửi `GET /api/catalog/faqs?category=...&search=...`.*
> 2. *Tại **REST Controller** (`CatalogController.java`), hàm `getPublicFaqs()` tiếp nhận query params và gọi sang Service Interface `catalogService.getPublicFaqEntries(category, search)`.*
> 3. *Tại **Service Interface** (`CatalogService.java`), hợp đồng trừu tượng định nghĩa phương thức tra cứu FAQ.*
> 4. *Tại **Service Impl** (`CatalogServiceImpl.java`), hệ thống thực hiện thuật toán tìm kiếm: chỉ lấy các bản ghi có cờ `isPublished = true`, chuẩn hóa tiếng Việt không dấu (NFD), tách token và sắp xếp theo số thứ tự hiển thị `displayOrder`.*
> 5. *Tại **Repository**, `FaqEntryRepository.findByPublishedTrueOrderByDisplayOrderAsc()` sinh câu lệnh SQL SELECT.*
> 6. *Tại **Database & Entity**, MySQL quét bảng `faq_entries` theo chỉ mục. Đây là điểm quay đầu của dữ liệu.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về danh sách các thực thể JPA thô `List<FaqEntry>` cho Repository.*
> * *Repository chuyển lên cho ServiceImpl. Tại đây, ServiceImpl dùng cú pháp stream map chuyển đổi từng Entity thô thành DTO sạch `FaqEntryResponse`.*
> * *ServiceImpl return danh sách DTO lên Controller.*
> * *Controller gửi phản hồi HTTP 200 OK (JSON) về cho Frontend, React render các câu hỏi thường gặp dạng Accordion mở rộng ạ."*

---

### // LUỒNG 2: TRỢ LÝ AI HỖ TRỢ THÔNG MINH RAG CHATBOT 12 BƯỚC (UC-65)
*(Vị trí code: `AiController.java:50`, `AiServiceImpl.java:114`, `AiProviderRouter.java:40`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 2: Trợ lý Ảo AI Hỗ trợ Thông minh RAG 12 Bước** do em tự thiết kế:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`AiFloatingWidget.tsx`), người dùng gửi câu hỏi, React đóng gói JSON gửi request `POST /api/ai/chat`.*
> 2. *Tại **REST Controller** (`AiController.java`), endpoint tiếp nhận và kiểm tra `@Valid` DTO, ủy quyền cho Service Interface `aiService.chat(request)`.*
> 3. *Tại **Service Interface** (`AiService.java`), định nghĩa chữ ký hàm nhận Request DTO và trả về Response DTO.*
> 4. *Tại **Service Impl** (`AiServiceImpl.java`), quy trình 12 bước nghiệp vụ RAG được kích hoạt:
>    - Bước 1 & 2: Lưu câu hỏi người dùng vào bảng `ai_chat_messages` thông qua `AiChatMessageRepository`.
>    - Bước 3 & 4: Truy vấn vector tri thức từ `AiKnowledgeChunkRepository` bằng thuật toán Cosine Similarity để tìm đoạn tài liệu phù hợp nhất.
>    - **Bước 5.1 (Real-time Business Context Injection)**: ServiceImpl gọi trực tiếp `tutoringClassRepository.count()` để lấy số liệu thực tế thời gian thực từ MySQL (số lớp đang tuyển, số gia sư đã duyệt) và tiêm thẳng vào Prompt.
>    - Bước 6, 7 & 8: Gửi Prompt hoàn chỉnh qua `AiProviderRouter` tới Google Gemini API.
>    - **Bước 9 (Hallucination Guard)**: chạy bộ lọc đối soát câu trả lời từ AI với dữ liệu CSDL để triệt tiêu hiện tượng bịa đặt số liệu.*
> 5. *Tại **Repository**, `AiChatMessageRepository.save()` thực thi SQL INSERT câu trả lời của Bot.*
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
> 4. *Tại **Service Impl** (`MessagingServiceImpl.java`), phương thức được bảo vệ bởi `@Transactional` và thực thi 2 thuật toán nghiệp vụ:
>    - Thuật toán **Ép sàn mức ưu tiên** (`escalatePriority`): nếu danh mục là Lỗi hệ thống (`SYSTEM_ERROR`) hoặc Tranh chấp học phí (`DISPUTE`), dù người dùng chọn mức Thấp, hệ thống vẫn tự động ép sàn nâng lên tối thiểu mức `HIGH`.
>    - Thuật toán **Tính hạn cam kết SLA** (`calculateDueAt`): dựa trên độ ưu tiên, hệ thống cộng thêm từ 4h đến 48h vào thời điểm hiện tại làm mốc `dueAt`.
>    - Sau đó, Service khởi tạo tin nhắn đầu tiên, bắn thông báo cho Admin và gọi Repository.*
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
> * *Frontend `PlatformTicketsPage.tsx` gửi `GET /api/platform/tickets` ➡️ `PlatformController.getTickets()` ➡️ `PlatformServiceImpl.getTickets()` gọi `SupportTicketRepository.search()` thực thi JPQL ➡️ MySQL quét bảng `support_tickets` ➡️ Chiều về: ServiceImpl map sang `Page<SupportTicketResponse>` trả về Controller hiển thị bảng phân trang.*
> 
> #### 4B. Mở Ticket & Tự động nhận bàn giao (Auto-assign PIC)
> * *Khi Admin click mở xem chi tiết ticket, gửi `GET /api/platform/tickets/{id}` ➡️ `PlatformServiceImpl.getTicketDetail()` kiểm tra nếu ticket chưa có ai phụ trách thì **tự động gán Admin hiện tại vào `assignedAdmin`** và đổi `status = IN_PROGRESS` lưu xuống MySQL ➡️ Chiều về: ServiceImpl tải thêm tin nhắn, ghép thành DTO `SupportTicketDetailResponse` trả về Controller mở khung hội thoại.*
> 
> #### 4C. Gửi phản hồi & Đo lường First Response SLA
> * *Admin nhập tin nhắn phản hồi gửi `POST /api/platform/tickets/{id}/messages` ➡️ `PlatformServiceImpl.respondToTicket()` tự động đo lường: nếu đây là phản hồi đầu tiên, tính ngay `responseSlaMs = now - createdAt` để ghi nhận KPI trực ban ➡️ `TicketMessageRepository.save()` lưu tin nhắn, `SupportTicketRepository.save()` cập nhật SLA, `auditLogService.record()` ghi vết kiểm toán ➡️ Chiều về: ServiceImpl map sang DTO `TicketMessageResponse` đẩy ngay tin nhắn mới lên màn hình.*
> 
> #### 4D. Đóng / Giải quyết Ticket
> * *Admin bấm Đóng ticket gửi `PATCH /api/platform/tickets/{id}/status` ➡️ `PlatformServiceImpl.closeTicket()` gán `status = CLOSED`, mốc `closedAt = now`, lưu bảng `support_tickets` và ghi `audit_logs` ➡️ Chiều về: ServiceImpl chuyển đổi DTO trả về cho React chuyển badge màu xám Đã đóng ạ."*

---

### // LUỒNG 5: GỘP TICKET TRÙNG LẶP & CHUYỂN SANG LUỒNG KHIẾU NẠI (UC-66, BF-08)
*(Vị trí code: `PlatformController.java:389`, `PlatformServiceImpl.java:1634`)*

> *"Dạ thưa thầy cô, **LUỒNG 5** giải quyết 2 bài toán điều phối nghiệp vụ nâng cao:*
> 
> #### 5A. Gộp Ticket trùng lặp (Merge Ticket - BF-08)
> ⬇️ **Chiều đi xuống:**
> 1. *Tại `PlatformTicketsPage.tsx`, Admin chọn ticket phụ và nhập ID ticket chính cần gộp vào, gửi `POST /api/platform/tickets/{id}/merge`.*
> 2. *`PlatformController.mergeTicket()` gọi `PlatformServiceImpl.mergeTicket()`.*
> 3. *ServiceImpl kiểm tra **4 chốt chặn an toàn**: cùng một người dùng tạo, 2 ticket khác nhau, ticket phụ chưa đóng, ticket chính đang mở. Sau đó chuyển toàn bộ tin nhắn từ ticket phụ sang ticket chính qua `ticketMessageRepository.saveAll()`, đổi ticket phụ sang `status = CLOSED` và ghi `audit_logs` dưới sự bảo vệ của `@Transactional`.*
> 4. *MySQL cập nhật hàng loạt bảng `ticket_messages`, `support_tickets` và `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity ticket chính đã gộp.*
> * *ServiceImpl đóng gói thành DTO `SupportTicketDetailResponse` trả về Controller để React tự động load lại giao diện ticket chính với đầy đủ lịch sử gộp.*
> 
> #### 5B. Chuyển Ticket sang Luồng Khiếu nại/Tranh chấp Escrow
> ⬇️ **Chiều đi xuống:**
> 1. *Admin bấm Chuyển sang Tranh chấp, gửi `POST /api/platform/tickets/{id}/redirect-dispute`.*
> 2. *`PlatformServiceImpl.redirectTicketToDispute()` khởi tạo một thực thể Báo cáo sự cố `Report` mới gắn với lớp học (`targetType = CLASS`), đóng ticket hỗ trợ ban đầu và ghi vết `audit_logs`.*
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
> 4. *Tại **Service Impl** (`CatalogServiceImpl.java`), hệ thống gán số thứ tự hiển thị `displayOrder`, thiết lập cờ xuất bản `isPublished`, và ghi nhật ký kiểm toán vào `audit_logs`.*
> 5. *Tại **Repository**, `FaqEntryRepository.save()` sinh câu lệnh SQL INSERT/UPDATE.*
> 6. *Tại **Database & Entity**, MySQL ghi nhận bản ghi mới vào bảng `faq_entries` và bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về Entity `FaqEntry saved` cho Repository.*
> * *ServiceImpl gọi mapper đóng gói thành DTO `FaqEntryAdminResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React bổ sung ngay câu hỏi mới vào bảng quản trị FAQ ạ."*

---

### // LUỒNG 7: QUÉT ĐỊNH KỲ & NÂNG CẤP KHẨN CẤP TICKET QUÁ HẠN SLA (JOB-11)
*(Vị trí code: `TicketSlaScheduler.java:11`, `PlatformServiceImpl.java:1721`, `SupportTicketRepository.java:48`)*

> *"Dạ thưa thầy cô, em xin phép trình bày **LUỒNG 7: Quét định kỳ & Nâng cấp khẩn cấp Ticket quá hạn SLA (JOB-11)**:
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Cron job chạy định kỳ tự động (hoặc Admin bấm nút quét trên UI), gửi request `POST /api/platform/tickets/sla/scan`.*
> 2. *Tại **REST Controller** (`PlatformController.java`), gọi `platformService.scanAndEscalateSlaBreaches()`.*
> 3. *Tại **Service Interface** (`PlatformService.java`), định nghĩa hàm quét vi phạm SLA.*
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`), hệ thống tìm tất cả các ticket chưa đóng mà có thời hạn cam kết `dueAt < now`. Với mỗi ticket quá hạn, Service **gán cờ vi phạm `slaBreached = true` và tự động nâng độ ưu tiên lên `URGENT`**, đồng thời ghi vết kiểm toán vào `audit_logs`.*
> 5. *Tại **Repository**, `SupportTicketRepository.findBreachedCandidateTickets()` quét dữ liệu và `saveAll()` lưu hàng loạt.*
> 6. *Tại **Database & Entity**, MySQL cập nhật các cột `sla_breached` và `priority` trong bảng `support_tickets`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về danh sách các Entity ticket vi phạm đã cập nhật.*
> * *ServiceImpl tổng hợp số lượng, đóng gói thành DTO `SlaScanSummaryResponse`.*
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
> 4. *Tại **Service Impl** (`PlatformServiceImpl.java`) kết hợp với `PlatformTaskQueueServiceImpl`, hệ thống triệu hồi đồng thời các Repository để đếm và tính tổng: số người dùng, số lớp học đang mở, tổng tiền đang ký quỹ Escrow và gom các việc khẩn cấp.*
> 5. *Tại **Repository**, các interface `UserRepository`, `TutoringClassRepository`, `EscrowTransactionRepository`, `SupportTicketRepository` thực thi các câu lệnh SQL `COUNT()` và `SUM()`.*
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
> 4. *Tại **Service Impl** (`CatalogServiceImpl.java`), để giải quyết triệt để vấn đề truy vấn lặp $N+1$, hệ thống **chỉ gọi CSDL đúng 1 lần duy nhất** để lấy toàn bộ danh mục phẳng. Sau đó, em dùng cấu trúc dữ liệu **`HashMap` trong bộ nhớ RAM** để gom các nút con vào nút cha theo mã cha `parentId`, đạt độ phức tạp thuật toán tối ưu $O(N)$.*
> 5. *Tại **Repository**, `SubjectCategoryRepository.findAll()` sinh câu lệnh SELECT đơn.*
> 6. *Tại **Database & Entity**, MySQL đọc toàn bộ bảng `subject_categories`.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về danh sách Entity phẳng `List<SubjectCategory>`.*
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
> 4. *Tại **Service Impl** (`SystemParameterServiceImpl.java`), kiểm tra định dạng giá trị, ghi nhật ký kiểm toán vào `audit_logs`.*
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
>    - Khi đọc: chuyển tiếp bộ lọc phân trang xuống Repo.
>    - Khi ghi: hàm `record()` tuân thủ nguyên tắc **Bất biến (Append-only)**: chỉ thực thi lệnh INSERT, cấm tuyệt đối UPDATE hoặc DELETE.*
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
> 4. *Tại **Service Impl** (`PlatformAnalyticsServiceImpl.java`), hệ thống áp dụng **2 kỹ thuật an toàn thông tin cốt lõi**:
>    - Thứ nhất, ghi 3 byte BOM `\uFEFF` ở đầu file để phần mềm Excel mở tiếng Việt có dấu không bao giờ bị lỗi font.
>    - Thứ hai, chạy hàm `escapeCsv()` làm sạch các ký tự nguy hiểm đứng đầu ô như `=`, `+`, `-`, `@` để **triệt tiêu lỗ hổng tấn công DDE Formula Injection**.*
> 5. *Tại **Repository**, `PaymentTransactionRepository` và `EscrowTransactionRepository` giới hạn tối đa 10k dòng để tránh tràn bộ nhớ.*
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
> 2. *Tại `CircumventionServiceImpl.java`, hệ thống chạy **4 bộ lọc Regex thời gian thực**: Số điện thoại (80đ), Email (90đ), URL/Website (70đ), Mạng xã hội Zalo/Telegram (65đ). Nếu điểm rủi ro tổng hợp $\ge 65$, hệ thống tự động gắn cờ vi phạm.*
> 3. *`CircumventionDetectionRepository.save()` lưu vi phạm vào bảng `circumvention_detections`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `CircumventionDetection saved`.*
> * *ServiceImpl đóng gói thành DTO `CircumventionDetectionResponse` trả về Controller để React hiển thị danh sách trường hợp nghi vấn kèm điểm rủi ro cho Admin thanh tra.*
> 
> #### 13B. Ban hành quyết định xử phạt & Chốt chặn tính năng (UC-60)
> ⬇️ **Chiều đi xuống:**
> 1. *Admin chọn hình thức phạt trên `PlatformPenaltiesPage.tsx`, gửi `POST /api/platform/penalties`.*
> 2. *`PenaltyServiceImpl.createPenalty()` lưu án phạt vào bảng `user_penalties` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên & Chốt chặn quyền liên module:**
> * *Ở các module khác (như Chat hoặc Nhận lớp), hàm `PenaltyAccessServiceImpl.requireFeature()` sẽ truy vấn bảng `user_penalties`. Nếu người dùng đang chịu án phạt còn hiệu lực, hệ thống lập tức ném lỗi `ForbiddenException` (403 Forbidden) chặn ngay hành động ạ!"*

---

# 👥 PHẦN 2: KỊCH BẢN NÓI CÁC USE CASE NGHIỆP VỤ & BẢO MẬT BỔ TRỢ (LUỒNG 14 ➡️ 27)

---

### LUỒNG 14: QUẢN LÝ NGƯỜI DÙNG & KHÓA TÀI KHOẢN TỨC THÌ (UC-07)
> *"Dạ thưa thầy cô, luồng khóa tài khoản vi phạm và thu hồi phiên đăng nhập:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại `PlatformUsersPage.tsx`, Admin chọn Khóa tài khoản (`BANNED`) kèm lý do giải trình, gửi `PATCH /api/platform/users/{id}/status`.*
> 2. *`PlatformController` gọi `PlatformServiceImpl.updateUserStatus()`.*
> 3. *ServiceImpl kiểm tra cấm khóa Root Admin, đổi trạng thái user sang `BANNED`, lưu lý do và ghi `audit_logs`.*
> 4. *`UserRepository.save()` cập nhật cột `status = 'BANNED'` trong bảng `users`.*
> 
> ⬆️ **Chiều đi ngược lên & Lớp bảo mật:**
> * *Database trả về Entity `User saved` ➡️ ServiceImpl dùng `platformMapper.toUserListItem()` che giấu mã băm mật khẩu, trả về DTO `UserListItemResponse` hiển thị badge đỏ BANNED.*
> * *Tại `JwtAuthenticationFilter`, mỗi request gửi lên kiểm tra `userDetails.isEnabled() == false` sẽ lập tức từ chối 401 Unauthorized ngay tại cổng lọc ạ!"*

---

### LUỒNG 15: THẨM ĐỊNH eKYC CCCD / BẰNG CẤP GIA SƯ (UC-54)
> *"Dạ thưa thầy cô, luồng xét duyệt hồ sơ định danh gia sư:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại `PlatformVerificationsPage.tsx`, Admin soi ảnh 2 mặt CCCD và bằng cấp, bấm Duyệt/Từ chối, gửi `POST /api/platform/verifications/{id}/review`.*
> 2. *`PlatformServiceImpl.reviewVerification()` lưu lịch sử thẩm định vào `verification_requests`, cập nhật trạng thái gia sư sang `VERIFIED` trong bảng `tutors` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity yêu cầu xác thực đã cập nhật.*
> * *ServiceImpl đóng gói thành DTO `VerificationReviewResponse` trả về Controller để React cập nhật tích xanh uy tín cho gia sư ạ."*

---

### LUỒNG 16: XỬ LÝ SỰ CỐ LỚP HỌC & ĐÓNG BĂNG ESCROW (UC-30)
> *"Dạ thưa thầy cô, quy trình giải quyết sự cố lớp học:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại `PlatformReportsPage.tsx`, Admin chọn phương án can thiệp sự cố lớp học, gửi `PATCH /api/platform/reports/{id}/resolve`.*
> 2. *`PlatformServiceImpl.resolveClassIssueReport()` thực thi 1 trong 7 hành động can thiệp; nếu nghiêm trọng, gọi `escrowService.holdForDispute()` đóng băng tiền học phí của lớp sang `ON_HOLD`, tạo bản ghi `Dispute`, đóng báo cáo trong bảng `reports` và trừ điểm uy tín gia sư.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity báo cáo đã giải quyết.*
> * *ServiceImpl đóng gói thành DTO `ResolveReportResponse` trả về Controller để React hiển thị trạng thái tiền đã được đóng băng an toàn ạ."*

---

### LUỒNG 17: QUẢN LÝ MẪU HỢP ĐỒNG MASTER
> *"Dạ thưa thầy cô, Admin soạn nội dung hợp đồng khung tại `PlatformContractTemplatesPage.tsx`, gửi `POST /api/platform/contract-templates` ➡️ `PlatformServiceImpl.createContractTemplate()` gán phiên bản hiệu lực, lưu vào bảng `contract_templates` và ghi `audit_logs` ➡️ Chiều về đóng gói DTO `ContractTemplateResponse` hiển thị mẫu hợp đồng mới ạ."*

---

### LUỒNG 18: CẤU HÌNH TỶ LỆ PHÍ ĐỐI TÁC TRUNG TÂM
> *"Dạ thưa thầy cô, Admin nhập mức phí chiết khấu riêng tại `PlatformFeeSettingsPage.tsx`, gửi `PUT /api/platform/centers/{id}/fee` ➡️ `PlatformServiceImpl.updateCenterFeeConfig()` kiểm tra ngưỡng tỷ lệ hợp lệ, cập nhật cột `custom_fee_rate` trong bảng `tutor_centers` và ghi `audit_logs` ➡️ Chiều về đóng gói DTO `TutorCenterFeeResponse` xác nhận lưu thành công ạ."*

---

### LUỒNG 19: BẢN TIN & THÔNG BÁO TOÀN SÀN (UC-59)
> *"Dạ thưa thầy cô, Admin soạn thông báo tại `PlatformTasksPage.tsx`, gửi `POST /api/platform/announcements` ➡️ `AnnouncementServiceImpl.upsertAnnouncement()` lưu bảng `announcements`, gọi `notificationDispatchService` gửi thông báo In-App realtime cho nhóm người dùng mục tiêu và ghi `audit_logs` ➡️ Chiều về đóng gói DTO `AnnouncementResponse` hiển thị banner nổi trên trang chủ ạ."*

---

### LUỒNG 20: HỢP ĐỒNG ĐIỆN TỬ KÝ SỐ OTP (UC-44)
> *"Dạ thưa thầy cô, quy trình ký kết hợp đồng điện tử qua mã OTP:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại `ContractDetailPage.tsx`, người dùng nhập mã OTP 6 số từ Email, gửi `POST /api/contract/{id}/sign-otp`.*
> 2. *`ContractServiceImpl.signWithOtp()` xác thực mã OTP trong bảng `email_otps`, lưu chữ ký số vào `contract_signatures`. **Nếu cả 2 bên cùng ký hoàn tất**, chuyển trạng thái hợp đồng sang `SIGNED` trong bảng `contracts` và kích hoạt tự động khóa tiền Escrow của lớp.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity hợp đồng đã hoàn tất.*
> * *ServiceImpl chuyển đổi thành DTO `ContractDetailResponse` trả về Controller để React hiển thị con dấu chứng thực điện tử màu xanh trên hợp đồng ạ."*

---

### LUỒNG 21: ĐÁNH GIÁ SAU BUỔI HỌC & ĐIỂM UY TÍN GIA SƯ
> *"Dạ thưa thầy cô, học viên chấm điểm sao (1 - 5 sao) trên `ContractDetailPage.tsx`, gửi `POST /api/contract/reviews` ➡️ `ReviewServiceImpl.createReview()` lưu vào bảng `reviews` ➡️ kích hoạt hàm `ContractServiceImpl.recomputeTutorReputation()` tính lại trung bình cộng số sao của gia sư trên toàn sàn và cập nhật cột `rating_avg` trong bảng `tutors` ➡️ Chiều về trả DTO `ReviewResponse` cập nhật số sao mới trên hồ sơ gia sư ạ."*

---

### LUỒNG 22: QUÊN MẬT KHẨU OTP QUA EMAIL (DEF-62)
> *"Dạ thưa thầy cô, người dùng nhập OTP từ Email và mật khẩu mới tại `ResetPasswordPage.tsx`, gửi `POST /api/identity/reset-password` ➡️ `IdentityServiceImpl.resetPassword()` kiểm tra token OTP còn hạn, băm mật khẩu mới bằng BCrypt, đánh dấu token đã dùng, cập nhật cột `password_hash` bảng `users` và ghi `audit_logs` ➡️ Chiều về trả DTO thông báo thành công và chuyển hướng về trang Đăng nhập ạ."*

---

### LUỒNG 23: ĐỔI MẬT KHẨU NGƯỜI DÙNG (CHANGE PASSWORD)
> *"Dạ thưa thầy cô, người dùng nhập mật khẩu cũ và mới tại `ProfilePage.tsx`, gửi `POST /api/identity/change-password` ➡️ `IdentityServiceImpl.changePassword()` dùng `passwordEncoder.matches()` kiểm tra mật khẩu cũ, băm mật khẩu mới bằng BCrypt, cập nhật cột `password_hash` bảng `users` và ghi `audit_logs` ➡️ Chiều về trả DTO xác nhận đổi mật khẩu thành công ạ."*

---

### LUỒNG 24: CHAT TIN NHẮN TỨC THỜI 1-1 & NHÓM LỚP HỌC (UC-50)
> *"Dạ thưa thầy cô, người dùng gửi tin nhắn trên `ChatWindow.tsx`, gửi `POST /api/messaging/chats/messages` ➡️ `ChatServiceImpl.sendMessage()` gọi `penaltyAccessService` (chặn nếu bị phạt chat), gọi `circumventionService` (quét lách sàn Regex), lưu tin nhắn vào bảng `messages` ➡️ Chiều về đóng gói DTO `ChatMessageResponse` đẩy tin nhắn mới lên giao diện chat realtime ạ."*

---

### LUỒNG 25: TẢI ẢNH ĐẠI DIỆN KIỂM TRA MAGIC BYTES CHỐNG MÃ ĐỘC (UC-08)
> *"Dạ thưa thầy cô, người dùng tải ảnh tại `TutorProfilePage.tsx`, gửi `POST /api/profile/avatar` ➡️ `ProfileServiceImpl.uploadAvatar()` đọc mảng byte đầu tiên kiểm tra **Magic Bytes** thực tế (PNG: `89 50 4E 47`, JPEG: `FF D8 FF`), chặn tệp giả mạo mã độc, cập nhật URL vào cột `avatar_url` bảng `users` ➡️ Chiều về trả DTO `AvatarUploadResponse` cập nhật ảnh đại diện trên thanh điều hướng ạ."*

---

### LUỒNG 26: ĐĂNG XUẤT & THU HỒI PHIÊN JWT BẰNG TOKEN_VERSION (LOGOUT)
> *"Dạ thưa thầy cô, cơ chế thu hồi phiên JWT không cần Redis:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Người dùng bấm Đăng xuất, xóa token ở client và gửi `POST /api/identity/logout`.*
> 2. *`IdentityServiceImpl.logout()` thực hiện: **`user.setTokenVersion(user.getTokenVersion() + 1L)`**, tăng giá trị phiên đăng nhập thêm 1 đơn vị, cập nhật cột `token_version` bảng `users` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên & Cơ chế thu hồi Stateless:**
> * *Controller trả về HTTP 200 OK chuyển hướng người dùng về trang chủ.*
> * *Trong `JwtAuthenticationFilter`, mỗi request gửi lên đều so khớp `principal.getTokenVersion() != jwtService.extractTokenVersion(claims)`. Khi thấy version trong CSDL đã tăng lên lệch so với token cũ, request lập tức bị từ chối 401 Unauthorized, vô hiệu hóa toàn bộ token cũ ngay lập tức mà không cần tốn chi phí duy trì Redis blacklist ạ!"*

---

### LUỒNG 27: KÝ QUỸ ESCROW, VÍ ĐIỆN TỬ & QUYẾT TOÁN DÒNG TIỀN (UC-40)
> *"Dạ thưa thầy cô, hạ tầng ký quỹ Escrow và quyết toán dòng tiền:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Khi hợp đồng được ký kết hoặc lớp học hoàn thành, hệ thống kích hoạt xử lý tại `EscrowServiceImpl` và `SettlementServiceImpl`.*
> 2. *Khi ký hợp đồng (UC-44): tiền học phí được khóa vào quỹ Escrow an toàn của sàn với trạng thái `HELD` trong bảng `escrow_transactions`.*
> 3. *Khi hoàn thành lớp học: hệ thống tự động trích 10% phí sàn (cấu hình ở Luồng 10) chuyển về cho sàn TCS, và giải ngân 90% còn lại vào ví khả dụng của Gia sư/Trung tâm trong bảng `wallets`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database cập nhật bảng `wallets`, `wallet_transactions`, `escrow_transactions` (`RELEASED`).*
> * *ServiceImpl đóng gói thành DTO `WalletResponse` và `SettlementResponse` trả về Controller hiển thị số dư ví và lịch sử dòng tiền minh bạch ạ!"*

---

# 🏆 PHẦN 3: LỜI KẾT THÚC THUYẾT TRÌNH ẤN TƯỢNG (15 GIÂY)

> *"Dạ kính thưa quý thầy cô trong hội đồng, trên đây là toàn bộ kiến trúc 6 tầng và chu trình dữ liệu 2 chiều khép kín trong các phân hệ mà em trực tiếp đảm nhiệm và phát triển.*
> 
> *Em xin chân thành cảm ơn quý thầy cô đã chú ý lắng nghe và em rất sẵn sàng nhận các câu hỏi nhận xét, phản biện từ hội đồng ạ!"*
