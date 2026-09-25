# 🎙️ KỊCH BẢN THUYẾT TRÌNH CHI TIẾT BẢO VỆ ĐỒ ÁN CAPSTONE / SWP391
## DÀNH RIÊNG CHO: HOÀNG MINH ĐỨC (DucHM — HE187354 / `mduc1011-swp`)
* **Dự án**: Tutor Connect System (TCS) — Hệ thống kết nối Gia sư thông minh
* **Khung giờ bảo vệ chính thức**: **10:50 – 12:20, Thứ Bảy ngày 26/09/2026**
* **Kiến trúc trình bày**: Chu trình 2 Chiều Khép Kín qua 6 Tầng Kiến Trúc Spring Boot:
  `[Frontend UI]` ⬇️ `[Controller]` ⬇️ `[Interface]` ⬇️ `[ServiceImpl]` ⬇️ `[Repository]` ⬇️ `[Database MySQL]`
  *(Điểm quay đầu CSDL)* ⬆️ `[Entity JPA]` ⬆️ `[Mapper: toResponse DTO sạch]` ⬆️ `[Controller]` ⬆️ `[Frontend Render]`

---

# 🌟 PHẦN 0: LỜI MỞ ĐẦU ĐỊNH VỊ BẢN THÂN (30 GIÂY MỞ MÀN)
*(Đứng thẳng, nét mặt tự tin, nhìn vào thầy cô chủ tịch và các thành viên hội đồng, nói dõng dạc)*

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

# 🏛️ PHẦN 1: CỤM PHÂN HỆ HỖ TRỢ KHÁCH HÀNG & ĐIỀU HÀNH SÀN (PLATFORM & MESSAGING)

---

### LUỒNG 1: NGƯỜI DÙNG TẠO TICKET HỖ TRỢ & TỰ ĐỘNG TÍNH HẠN SLA (UC-65, UC-66)
*(Thời lượng nói: 2 phút — Luồng trọng điểm)*

> *"Dạ thưa thầy cô, em xin phép trình bày **Luồng Người dùng tạo Ticket hỗ trợ & Thuật toán tính hạn cam kết SLA**:*
> 
> ⬇️ **Chiều đi xuống (Request):**
> 1. *Tại **Frontend UI** (`MessagingPanel.tsx`), người dùng chọn danh mục sự cố, nhập tiêu đề, mô tả và đính kèm bằng chứng. Khi bấm 'Gửi yêu cầu', React đóng gói JSON gửi request `POST /api/messaging/support-tickets`.*
> 2. *Tại **REST Controller** (`MessagingController.java`), endpoint tiếp nhận và dùng `@Valid` kiểm tra dữ liệu đầu vào. Khi hợp lệ, Controller ủy quyền cho Service Interface `messagingService.createSupportTicket()`.*
> 3. *Tại **Service Interface** (`MessagingService.java`), hợp đồng trừu tượng định nghĩa phương thức nhận Request DTO và trả về Response DTO.*
> 4. *Tại **Service Impl** (`MessagingServiceImpl.java`), phương thức được bảo vệ bởi `@Transactional` và thực thi 2 thuật toán then chốt:
>    - Thứ nhất, thuật toán **Ép sàn mức ưu tiên** (`escalatePriority`): nếu người dùng báo cáo lỗi hệ thống (`SYSTEM_ERROR`) hoặc tranh chấp tiền (`DISPUTE`), dù họ chọn mức Thấp, hệ thống vẫn tự động ép sàn nâng lên tối thiểu mức `HIGH`.
>    - Thứ hai, thuật toán **Tính hạn cam kết SLA** (`calculateDueAt`): dựa trên độ ưu tiên, hệ thống cộng thêm từ 4h đến 48h vào thời điểm hiện tại làm mốc `dueAt`.
>    - Sau đó, Service khởi tạo tin nhắn đầu tiên và gọi Repository.*
> 5. *Tại **Repository Interface**, `SupportTicketRepository.save()` và `TicketMessageRepository.save()` sinh các câu lệnh SQL INSERT.*
> 6. *Tại **Database & Entity**, MySQL ghi nhận 2 bản ghi mới vào bảng `support_tickets` và `ticket_messages`. Đây chính là điểm quay đầu của dữ liệu.*
> 
> ⬆️ **Chiều đi ngược lên (Response & DTO Mapping):**
> * *Database trả về đối tượng Entity JPA thô `SupportTicket saved` cho Repository, Repository giao lại cho ServiceImpl.*
> * *Tại ServiceImpl, em gọi hàm mapper nội bộ `toResponse(saved)`. Hàm này bóc tách dữ liệu từ Entity thô, che giấu các thông tin nhạy cảm của CSDL và đóng gói thành DTO sạch `SupportTicketResponse`.*
> * *ServiceImpl `return` đối tượng DTO sạch này lên Controller.*
> * *Controller gửi phản hồi HTTP 201 Created (JSON) về trình duyệt để React hiển thị thông báo tạo ticket thành công kèm đồng hồ đếm ngược SLA ạ."*

---

### LUỒNG 2: ADMIN LỌC & XEM DANH SÁCH TICKET HỖ TRỢ (LUỒNG 4A)
> *"Dạ thưa thầy cô, luồng xem và lọc danh sách Ticket của Admin hoạt động như sau:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformTicketsPage.tsx`), Admin chọn bộ lọc trạng thái, độ ưu tiên hoặc danh mục sự cố, React gửi `GET /api/platform/tickets` kèm query params.*
> 2. *Tại **Controller** (`PlatformController.java`), phương thức `getTickets()` tiếp nhận tham số phân trang `Pageable` và gọi sang Interface `platformService.getTickets()`.*
> 3. *Tại **Interface** (`PlatformService.java`), khai báo chữ ký hàm lấy danh sách phân trang.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), hệ thống kiểm tra quyền Admin qua `authHelper` và chuyển tiếp điều kiện lọc xuống Repository.*
> 5. *Tại **Repository**, `SupportTicketRepository.search()` thực thi câu lệnh JPQL `@Query` đa điều kiện tối ưu theo chỉ mục.*
> 6. *Tại **Database**, MySQL quét bảng `support_tickets` và trả về các dòng dữ liệu thỏa mãn.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về tập hợp các Entity thô dạng `Page<SupportTicket>` cho Repository.*
> * *Repository chuyển lên ServiceImpl. Tại đây, ServiceImpl dùng cú pháp `.map(this::toResponse)` chuyển đổi từng Entity thành DTO `SupportTicketResponse`.*
> * *ServiceImpl return `Page<SupportTicketResponse>` lên Controller.*
> * *Controller phản hồi HTTP 200 OK để React render bảng danh sách phân trang mượt mà ạ."*

---

### LUỒNG 3: ADMIN MỞ TICKET & TỰ ĐỘNG BÀN GIAO (AUTO-ASSIGN - LUỒNG 4B)
> *"Dạ thưa thầy cô, đây là cơ chế tự động bàn giao trách nhiệm xử lý ticket:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformTicketsPage.tsx`), Admin click chọn mở 1 ticket để xem, gửi `GET /api/platform/tickets/{id}`.*
> 2. *Tại **Controller** (`PlatformController.java`), hàm `getTicketDetail()` tiếp nhận `ticketId` và gọi sang `platformService.getTicketDetail()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm lấy chi tiết ticket.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), hệ thống kiểm tra nếu ticket đang ở trạng thái `OPEN` và chưa có ai phụ trách, ServiceImpl sẽ **tự động gán Admin hiện tại vào `assignedAdmin`** và đổi `status = IN_PROGRESS` để tránh tình trạng nhiều Admin cùng tranh nhau xử lý một ticket.*
> 5. *Tại **Repository**, `SupportTicketRepository.save()` thực thi câu lệnh SQL UPDATE.*
> 6. *Tại **Database**, MySQL cập nhật trực tiếp cột `assigned_admin_id` và `status` trong bảng `support_tickets`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `SupportTicket` đã cập nhật cho Repository.*
> * *ServiceImpl tải thêm danh sách tin nhắn từ `TicketMessageRepository`, ghép lại và gọi mapper `toDetailResponse()` tạo thành DTO đầy đủ `SupportTicketDetailResponse`.*
> * *ServiceImpl return DTO chi tiết lên Controller.*
> * *Controller trả về HTTP 200 OK để React mở khung hội thoại trao đổi chi tiết của ticket ạ."*

---

### LUỒNG 4: ADMIN PHẢN HỒI TICKET & ĐO LƯỜNG FIRST RESPONSE SLA (LUỒNG 4C)
> *"Dạ thưa thầy cô, luồng gửi phản hồi và đo lường SLA của Admin diễn ra như sau:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformTicketsPage.tsx`), Admin nhập nội dung trao đổi hoặc hướng giải quyết và bấm gửi, gọi `POST /api/platform/tickets/{id}/messages`.*
> 2. *Tại **Controller** (`PlatformController.java`), nhận DTO phản hồi, kiểm tra `@Valid` rồi chuyển cho `platformService.respondToTicket()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm phản hồi ticket.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), hệ thống thực hiện nghiệp vụ đo lường: nếu đây là phản hồi đầu tiên của Admin, hệ thống tự động tính **thời gian phản hồi thực tế**: `responseSlaMs = now - createdAt` để ghi nhận KPI trực ban. Đồng thời ghi vết kiểm toán qua `auditLogService`.*
> 5. *Tại **Repository**, `TicketMessageRepository.save()` lưu tin nhắn, `SupportTicketRepository.save()` cập nhật mốc SLA.*
> 6. *Tại **Database**, MySQL ghi dòng mới vào bảng `ticket_messages`, cập nhật bảng `support_tickets` và ghi nhận vào bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `TicketMessage saved` cho Repository.*
> * *ServiceImpl gọi mapper `toTicketMessageResponse()` đóng gói thành DTO sạch `TicketMessageResponse`.*
> * *ServiceImpl return DTO tin nhắn lên Controller.*
> * *Controller trả về HTTP 201 Created để React lập tức đẩy tin nhắn mới vào khung chat thời gian thực ạ."*

---

### LUỒNG 5: ĐÓNG / GIẢI QUYẾT TICKET HỖ TRỢ (LUỒNG 4D)
> *"Dạ thưa thầy cô, luồng đóng ticket kết thúc hỗ trợ:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformTicketsPage.tsx`), Admin bấm nút 'Giải quyết' hoặc 'Đóng ticket', gửi `PATCH /api/platform/tickets/{id}/status`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi sang `platformService.closeTicket()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm đóng ticket.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), cập nhật trạng thái `status = CLOSED`, gán mốc thời gian hoàn tất `closedAt = now`, ghi nhật ký kiểm toán.*
> 5. *Tại **Repository**, `SupportTicketRepository.save()` thực thi câu lệnh SQL UPDATE.*
> 6. *Tại **Database**, MySQL cập nhật các cột `status`, `closed_at` trong bảng `support_tickets` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `SupportTicket` đã đóng cho Repository.*
> * *ServiceImpl dùng mapper `toResponse()` đóng gói thành DTO `SupportTicketResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React chuyển badge trạng thái ticket sang màu xám Đã đóng ạ."*

---

### LUỒNG 6: GỘP TICKET TRÙNG LẶP (MERGE TICKET - LUỒNG 5A / BF-08)
*(Thời lượng nói: 1.5 phút — Tính năng độc đáo)*

> *"Dạ thưa thầy cô, đây là tính năng gộp nhiều ticket trùng nội dung của cùng một người dùng:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformTicketsPage.tsx`), Admin chọn một ticket phụ và nhập ID của ticket chính cần gộp vào, gửi `POST /api/platform/tickets/{id}/merge`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi `platformService.mergeTicket()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm gộp ticket.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), em thiết kế **4 chốt chặn an toàn**:
>    - Chốt 1: Hai ticket không được trùng ID.
>    - Chốt 2: Cả 2 ticket phải thuộc về cùng một `userId`.
>    - Chốt 3: Ticket phụ phải chưa bị đóng.
>    - Chốt 4: Ticket chính phải đang mở.
>    - Sau khi thỏa mãn, Service chuyển toàn bộ tin nhắn từ ticket phụ sang ticket chính, đóng ticket phụ với lý do gộp, ghi vết vào `audit_logs` dưới sự bảo vệ của `@Transactional`.*
> 5. *Tại **Repository**, `TicketMessageRepository.saveAll()` đổi khóa ngoại, `SupportTicketRepository.saveAll()` cập nhật trạng thái.*
> 6. *Tại **Database**, MySQL cập nhật đồng loạt bảng `ticket_messages`, `support_tickets` và `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity ticket chính sau khi đã tích hợp thêm tin nhắn.*
> * *ServiceImpl đóng gói thành DTO `SupportTicketDetailResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React tự động đóng modal và load lại giao diện ticket chính với đầy đủ lịch sử gộp ạ."*

---

### LUỒNG 7: CHUYỂN TICKET SANG KHIẾU NẠI TRANH CHẤP ESCROW (LUỒNG 5B)
> *"Dạ thưa thầy cô, khi ticket hỗ trợ thông thường biến thành tranh chấp quyền lợi học phí:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformTicketsPage.tsx`), Admin bấm 'Chuyển sang Tranh chấp', gửi `POST /api/platform/tickets/{id}/redirect-dispute`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi `platformService.redirectTicketToDispute()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm chuyển hướng.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), hệ thống khởi tạo một thực thể Báo cáo sự cố `Report` mới gắn với lớp học (`targetType = CLASS`), đóng ticket hỗ trợ ban đầu và ghi vết `audit_logs`.*
> 5. *Tại **Repository**, `ReportRepository.save()` tạo báo cáo mới, `SupportTicketRepository.save()` đóng ticket cũ.*
> 6. *Tại **Database**, MySQL ghi mới bảng `reports`, cập nhật bảng `support_tickets` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `Report` vừa tạo.*
> * *ServiceImpl đóng gói thành DTO phản hồi chứa ID vụ việc mới.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị thông báo chuyển đổi thành công và cung cấp đường dẫn chuyển sang trang giải quyết tranh chấp ạ."*

---

### LUỒNG 8: QUÉT ĐỊNH KỲ & NÂNG CẤP KHẨN CẤP TICKET QUÁ HẠN SLA (LUỒNG 7 / JOB-11)
> *"Dạ thưa thầy cô, đây là cơ chế tự động bảo vệ cam kết SLA với khách hàng:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Hệ thống định kỳ (hoặc Admin bấm quét trên UI) kích hoạt request `POST /api/platform/tickets/sla/scan`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi `platformService.scanAndEscalateSlaBreaches()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm quét quá hạn.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), hệ thống tìm tất cả các ticket chưa đóng mà có thời hạn cam kết `dueAt < now`. Với mỗi ticket quá hạn, Service **gắn cờ vi phạm `slaBreached = true` và tự động nâng độ ưu tiên lên `URGENT`**, ghi nhận vào `audit_logs`.*
> 5. *Tại **Repository**, `SupportTicketRepository.findBreachedCandidateTickets()` quét dữ liệu và `saveAll()` lưu hàng loạt.*
> 6. *Tại **Database**, MySQL cập nhật các cột `sla_breached` và `priority` trong bảng `support_tickets`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về danh sách các Entity ticket đã được nâng cấp.*
> * *ServiceImpl tổng hợp số lượng, đóng gói thành DTO `SlaScanSummaryResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị toast thông báo số lượng ticket vi phạm đã được đưa vào diện khẩn cấp ạ."*

---

### LUỒNG 9: ADMIN DASHBOARD KPI & HÀNG ĐỢI NHIỆM VỤ TRỰC BAN KHẨN CẤP (LUỒNG 8 / UC-56, UC-64)
> *"Dạ thưa thầy cô, màn hình trung tâm chỉ huy của Admin vận hành như sau:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformDashboardPage.tsx`), khi Admin tải trang, React gửi `GET /api/platform/dashboard`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi `platformService.getDashboard()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm lấy dữ liệu KPI.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`) kết hợp với `PlatformTaskQueueServiceImpl`, hệ thống triệu hồi đồng thời các Repository để đếm và tính tổng: số người dùng, số lớp học đang mở, tổng tiền đang ký quỹ Escrow và gom các việc khẩn cấp.*
> 5. *Tại **Repository**, các interface `UserRepository`, `TutoringClassRepository`, `EscrowTransactionRepository`, `SupportTicketRepository` thực thi các câu lệnh `COUNT()` và `SUM()`.*
> 6. *Tại **Database**, MySQL quét các bảng tương ứng và tính toán chỉ số.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về các con số thống kê thô cho Repository.*
> * *ServiceImpl gom toàn bộ số liệu và danh sách tác vụ khẩn cấp vào DTO `PlatformDashboardResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React vẽ các thẻ Card KPI và bảng hàng đợi trực ban khẩn cấp ạ."*

---

### LUỒNG 10: QUẢN LÝ NGƯỜI DÙNG & KHÓA TÀI KHOẢN TỨC THÌ (UC-07)
*(Thời lượng nói: 1.5 phút — Nhấn mạnh bảo mật)*

> *"Dạ thưa thầy cô, đây là quy trình khóa tài khoản vi phạm và thu hồi phiên đăng nhập:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformUsersPage.tsx`), Admin bấm nút Khóa tài khoản (`BANNED`) kèm nhập lý do giải trình, gửi `PATCH /api/platform/users/{id}/status`.*
> 2. *Tại **Controller** (`PlatformController.java`), kiểm tra `@Valid` rồi gọi `platformService.updateUserStatus()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm đổi trạng thái user.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), hệ thống kiểm tra an toàn: **tuyệt đối cấm khóa tài khoản Root Admin**. Sau đó đổi trạng thái user sang `BANNED`, lưu lý do và ghi vết kiểm toán vào `audit_logs`.*
> 5. *Tại **Repository**, `UserRepository.save()` thực thi câu lệnh SQL UPDATE.*
> 6. *Tại **Database**, MySQL cập nhật cột `status = 'BANNED'` trong bảng `users` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `User saved`.*
> * *ServiceImpl gọi `platformMapper.toUserListItem()` che giấu mã băm mật khẩu, đóng gói thành DTO sạch `UserListItemResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React cập nhật badge trạng thái màu đỏ 'BANNED'.*
> * *Đặc biệt: Ở các request tiếp theo của người dùng bị khóa, bộ lọc `JwtAuthenticationFilter` kiểm tra thấy `userDetails.isEnabled() == false` sẽ lập tức từ chối 401 Unauthorized mà không cần chờ token JWT hết hạn ạ!"*

---

### LUỒNG 11: THẨM ĐỊNH eKYC CCCD / BẰNG CẤP GIA SƯ (UC-54)
> *"Dạ thưa thầy cô, luồng xét duyệt hồ sơ định danh gia sư:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformVerificationsPage.tsx`), Admin soi ảnh 2 mặt CCCD và bằng cấp của gia sư, bấm 'Duyệt' hoặc 'Từ chối', gửi `POST /api/platform/verifications/{id}/review`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi `platformService.reviewVerification()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm thẩm định eKYC.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), lưu lịch sử thẩm định; nếu được duyệt thì cập nhật hồ sơ gia sư sang trạng thái `VERIFIED`, nếu từ chối thì lưu lý do giải trình, ghi nhật ký kiểm toán.*
> 5. *Tại **Repository**, `VerificationRequestRepository.save()` và `TutorRepository.save()`.*
> 6. *Tại **Database**, MySQL cập nhật bảng `verification_requests`, cột `verification_status` bảng `tutors` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity yêu cầu xác thực đã cập nhật.*
> * *ServiceImpl đóng gói thành DTO `VerificationReviewResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React cập nhật trạng thái tích xanh uy tín cho gia sư ạ."*

---

### LUỒNG 12: XỬ LÝ SỰ CỐ LỚP HỌC & ĐÓNG BĂNG ESCROW (UC-30)
> *"Dạ thưa thầy cô, quy trình giải quyết sự cố vi phạm xảy ra trong lớp học:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformReportsPage.tsx`), Admin chọn phương án can thiệp sự cố lớp học, gửi `PATCH /api/platform/reports/{id}/resolve`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi `platformService.resolveClassIssueReport()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm xử lý báo cáo sự cố.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), hệ thống thực thi 1 trong 7 hành động can thiệp. Nếu là sự cố nghiêm trọng, hệ thống tự động gọi `escrowService.holdForDispute()` để **chuyển tiền học phí đang ký quỹ sang trạng thái `ON_HOLD`**, mở phiên tranh chấp `Dispute`, đóng báo cáo và trừ điểm uy tín gia sư.*
> 5. *Tại **Repository**, `ReportRepository.save()`, `DisputeRepository.save()`, `EscrowTransactionRepository.save()`.*
> 6. *Tại **Database**, MySQL cập nhật bảng `reports`, bảng `escrow_transactions`, ghi bảng `disputes` và cập nhật bảng `tutors`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity báo cáo đã giải quyết.*
> * *ServiceImpl đóng gói thành DTO `ResolveReportResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị thông báo đã phong tỏa dòng tiền an toàn ạ."*

---

### LUỒNG 13: QUẢN LÝ MẪU HỢP ĐỒNG MASTER
> *"Dạ thưa thầy cô, luồng quản trị điều khoản hợp đồng mẫu:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformContractTemplatesPage.tsx`), Admin soạn nội dung hợp đồng khung, gửi `POST /api/platform/contract-templates`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi `platformService.createContractTemplate()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm tạo mẫu hợp đồng.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), hệ thống gán phiên bản hiệu lực, ghi nhận vào `audit_logs`.*
> 5. *Tại **Repository**, `ContractTemplateRepository.save()` thực thi SQL INSERT.*
> 6. *Tại **Database**, MySQL ghi nhận dòng mới vào bảng `contract_templates` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `ContractTemplate saved`.*
> * *ServiceImpl đóng gói thành DTO `ContractTemplateResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React bổ sung mẫu hợp đồng mới vào danh sách sử dụng ạ."*

---

### LUỒNG 14: CẤU HÌNH TỶ LỆ PHÍ ĐỐI TÁC TRUNG TÂM
> *"Dạ thưa thầy cô, luồng thiết lập tỷ lệ hoa hồng riêng cho từng trung tâm:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformFeeSettingsPage.tsx`), Admin nhập mức phí chiết khấu riêng, gửi `PUT /api/platform/centers/{id}/fee`.*
> 2. *Tại **Controller** (`PlatformController.java`), gọi `platformService.updateCenterFeeConfig()`.*
> 3. *Tại **Interface** (`PlatformService.java`), định nghĩa hàm cấu hình phí.*
> 4. *Tại **ServiceImpl** (`PlatformServiceImpl.java`), kiểm tra tỷ lệ phần trăm hợp lệ, cập nhật và ghi vết `audit_logs`.*
> 5. *Tại **Repository**, `TutorCenterRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database**, MySQL cập nhật cột `custom_fee_rate` trong bảng `tutor_centers` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `TutorCenter saved`.*
> * *ServiceImpl đóng gói thành DTO `TutorCenterFeeResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị tỷ lệ phí mới đã áp dụng ạ."*

---

### LUỒNG 15: BẢN TIN & THÔNG BÁO TOÀN SÀN (UC-59)
> *"Dạ thưa thầy cô, luồng phát thông báo bảo trì và chính sách toàn sàn:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformTasksPage.tsx`), Admin soạn thông báo và chọn đối tượng mục tiêu, gửi `POST /api/platform/announcements`.*
> 2. *Tại **Controller** (`AnnouncementController.java`), gọi `announcementService.upsertAnnouncement()`.*
> 3. *Tại **Interface** (`AnnouncementService.java`), định nghĩa hàm tạo bản tin.*
> 4. *Tại **ServiceImpl** (`AnnouncementServiceImpl.java`), lưu bản tin, gọi `notificationDispatchService` phát thông báo In-App thời gian thực cho người dùng mục tiêu (`ALL`, `TUTOR`, `CLIENT`), ghi audit log.*
> 5. *Tại **Repository**, `AnnouncementRepository.save()` thực thi SQL.*
> 6. *Tại **Database**, MySQL ghi bảng `announcements`, bảng `notifications` và `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `Announcement saved`.*
> * *ServiceImpl đóng gói thành DTO `AnnouncementResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React hiển thị banner thông báo nổi bật trên trang chủ ạ."*

---

# 📚 PHẦN 2: CỤM PHÂN HỆ TRI THỨC & DANH MỤC HỆ THỐNG (CATALOG)

---

### LUỒNG 16: TRA CỨU DANH MỤC FAQ CÔNG KHAI (LUỒNG 1)
> *"Dạ thưa thầy cô, luồng tra cứu câu hỏi thường gặp của khách vãng lai:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`HelpPage.tsx`), người dùng gõ từ khóa tìm kiếm FAQ, gửi `GET /api/catalog/faqs`.*
> 2. *Tại **Controller** (`CatalogController.java`), hàm `getPublicFaqs()` gọi sang `catalogService.getPublicFaqEntries()`.*
> 3. *Tại **Interface** (`CatalogService.java`), định nghĩa hàm tra cứu FAQ.*
> 4. *Tại **ServiceImpl** (`CatalogServiceImpl.java`), hệ thống chỉ lọc các bài viết có cờ `published = true` và sắp xếp theo `displayOrder`.*
> 5. *Tại **Repository**, `FaqEntryRepository.findByPublishedTrueOrderByDisplayOrderAsc()` sinh câu lệnh SQL.*
> 6. *Tại **Database**, MySQL đọc dữ liệu từ bảng `faq_entries`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về danh sách Entity `List<FaqEntry>`.*
> * *ServiceImpl map từng phần tử sang DTO `FaqEntryResponse`.*
> * *ServiceImpl return danh sách DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị các câu hỏi dạng Accordion mở rộng ạ."*

---

### LUỒNG 17: QUẢN TRỊ FAQ - ADMIN CRUD & PHÊ DUYỆT BẢN NHÁP (LUỒNG 6 / UC-67)
> *"Dạ thưa thầy cô, quy trình quản trị nội dung tri thức FAQ của Admin:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformFaqPage.tsx`), Admin soạn câu hỏi, câu trả lời bằng Markdown, chọn lưu Nháp hoặc Công khai, gửi `POST /api/catalog/admin/faqs`.*
> 2. *Tại **Controller** (`CatalogController.java`), gọi `catalogService.createFaqEntry()`.*
> 3. *Tại **Interface** (`CatalogService.java`), định nghĩa hàm CRUD FAQ.*
> 4. *Tại **ServiceImpl** (`CatalogServiceImpl.java`), gán số thứ tự hiển thị, cờ `isPublished`, ghi vết vào `audit_logs`.*
> 5. *Tại **Repository**, `FaqEntryRepository.save()` thực thi SQL INSERT.*
> 6. *Tại **Database**, MySQL ghi bảng `faq_entries` và bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `FaqEntry saved`.*
> * *ServiceImpl đóng gói thành DTO `FaqEntryAdminResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React bổ sung ngay câu hỏi mới vào bảng quản trị ạ."*

---

### LUỒNG 18: QUẢN LÝ CÂY DANH MỤC HỆ THỐNG PHÂN CẤP ĐỆ QUY O(N) (LUỒNG 9 / UC-57)
*(Thời lượng nói: 1.5 phút — Nhấn mạnh thuật toán)*

> *"Dạ thưa thầy cô, em xin phép trình bày thuật toán dựng cây danh mục môn học $O(N)$:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformCategoriesPage.tsx`), khi mở trang, gửi `GET /api/catalog/categories/tree`.*
> 2. *Tại **Controller** (`CatalogController.java`), gọi `catalogService.getCategoryTree()`.*
> 3. *Tại **Interface** (`CatalogService.java`), định nghĩa hàm lấy cây danh mục.*
> 4. *Tại **ServiceImpl** (`CatalogServiceImpl.java`), để giải quyết triệt để vấn đề truy vấn lặp $N+1$, hệ thống **chỉ gọi CSDL đúng 1 lần duy nhất** để lấy toàn bộ danh mục phẳng. Sau đó, em dùng cấu trúc dữ liệu **`HashMap` trong bộ nhớ RAM** để gom các nút con vào nút cha theo mã cha `parentId`, đạt độ phức tạp thuật toán tối ưu $O(N)$.*
> 5. *Tại **Repository**, `SubjectCategoryRepository.findAll()` sinh câu lệnh SELECT đơn.*
> 6. *Tại **Database**, MySQL đọc toàn bộ bảng `subject_categories`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về danh sách Entity phẳng `List<SubjectCategory>`.*
> * *ServiceImpl biến đổi cấu trúc phẳng thành cây phân cấp đa tầng DTO `List<CategoryNodeResponse>`.*
> * *ServiceImpl return cây DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị Tree View trực quan cho người dùng ạ."*

---

### LUỒNG 19: THAM SỐ CẤU HÌNH TOÀN CỤC & TỶ LỆ PHÍ SÀN (LUỒNG 10 / UC-58)
> *"Dạ thưa thầy cô, luồng quản lý tham số vận hành toàn sàn:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformSettingsPage.tsx`), Admin thay đổi tỷ lệ phí sàn (10%), gửi `PUT /api/catalog/admin/parameters/{key}`.*
> 2. *Tại **Controller** (`CatalogController.java`), gọi `catalogService.updateParameter()`.*
> 3. *Tại **Interface** (`CatalogService.java`), định nghĩa hàm cập nhật tham số.*
> 4. *Tại **ServiceImpl** (`CatalogServiceImpl.java`), kiểm tra định dạng giá trị, ghi nhật ký kiểm toán vào `audit_logs`.*
> 5. *Tại **Repository**, `SystemParameterRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database**, MySQL cập nhật giá trị trong bảng `system_parameters` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `SystemParameter saved`.*
> * *ServiceImpl đóng gói thành DTO `SystemParameterResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị thông báo đã cập nhật tham số thành công ạ."*

---

# 🤖 PHẦN 3: CỤM PHÂN HỆ TRỢ LÝ THÔNG MINH AI (AI RAG)

---

### LUỒNG 20: TRỢ LÝ ẢO AI UNIVERSAL RAG 12 BƯỚC (LUỒNG 2 / UC-65)
*(Thời lượng nói: 2 phút — Điểm sáng công nghệ)*

> *"Dạ thưa thầy cô, em xin phép trình bày **Trợ lý Ảo AI Universal RAG 12 Bước** do em tự tay thiết kế:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend UI** (`AiFloatingWidget.tsx`), người dùng gửi câu hỏi, React gọi `POST /api/ai/chat`.*
> 2. *Tại **Controller** (`AiController.java`), kiểm tra `@Valid` rồi chuyển sang `aiService.chat()`.*
> 3. *Tại **Interface** (`AiService.java`), định nghĩa phương thức chat.*
> 4. *Tại **ServiceImpl** (`AiServiceImpl.java`), quy trình 12 bước được vận hành:
>    - Lưu câu hỏi vào CSDL.
>    - Truy vấn vector tri thức từ `AiKnowledgeChunkRepository` bằng thuật toán Cosine Similarity.
>    - **Đặc biệt ở Bước 5.1 (Real-time Business Context Injection)**: ServiceImpl truy vấn MySQL lấy số liệu thực tế thời gian thực (số lớp đang tuyển, số gia sư đã duyệt) và tiêm trực tiếp vào Prompt.
>    - Gửi Prompt hoàn chỉnh qua `AiProviderRouter` tới Google Gemini API.
>    - **Tại Bước 9 (Hallucination Guard)**: chạy bộ lọc đối soát câu trả lời từ AI với dữ liệu CSDL để triệt tiêu hiện tượng bịa đặt số liệu.*
> 5. *Tại **Repository**, `AiChatMessageRepository.save()` và `AiKnowledgeChunkRepository`.*
> 6. *Tại **Database**, MySQL ghi câu hỏi và câu trả lời vào bảng `ai_chat_messages`, đọc bảng `ai_knowledge_chunks`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `AiChatMessage saved`.*
> * *ServiceImpl đóng gói câu trả lời cùng danh sách tài liệu trích dẫn thành DTO sạch `AiChatResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị bong bóng chat thông minh kèm nguồn trích dẫn ạ."*

---

# 🛡️ PHẦN 4: CỤM AN NINH, XỬ PHẠT & KIỂM TOÁN (SECURITY & ANALYTICS)

---

### LUỒNG 21: GIÁM SÁT NHẬT KÝ KIỂM TOÁN BẤT BIẾN (LUỒNG 11 / UC-61)
> *"Dạ thưa thầy cô, hạ tầng kiểm toán bất biến của hệ thống hoạt động như sau:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformAuditLogsPage.tsx`), Admin xem lịch sử tác động hệ thống, gửi `GET /api/platform/audit-logs`.*
> 2. *Tại **Controller** (`AuditLogController.java`), gọi `auditLogService.getAuditLogs()`.*
> 3. *Tại **Interface** (`AuditLogService.java`), định nghĩa hàm tra cứu và hàm ghi vết nội bộ `record()`.*
> 4. *Tại **ServiceImpl** (`AuditLogServiceImpl.java`):
>    - Khi đọc: chuyển tiếp bộ lọc phân trang xuống Repo.
>    - Khi ghi: hàm `record()` tuân thủ nguyên tắc **Bất biến (Append-only)**: chỉ thực thi lệnh INSERT, cấm tuyệt đối UPDATE hoặc DELETE.*
> 5. *Tại **Repository**, `AuditLogRepository.search()` để đọc và `save()` để ghi.*
> 6. *Tại **Database**, MySQL đọc/ghi trực tiếp vào bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về danh sách Entity `Page<AuditLog>`.*
> * *ServiceImpl map sang DTO `AuditLogResponse` chứa dữ liệu so vết JSON Diff (Trước/Sau).*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React render bảng nhật ký kèm modal xem vết thay đổi chi tiết ạ."*

---

### LUỒNG 22: BÁO CÁO TÀI CHÍNH & XUẤT CSV AN TOÀN (LUỒNG 12 / UC-41, UC-43)
*(Thời lượng nói: 1.5 phút — Nhấn mạnh an toàn thông tin)*

> *"Dạ thưa thầy cô, quy trình xuất báo cáo tài chính an toàn:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformAnalyticsPage.tsx`), Admin bấm 'Xuất file CSV', gửi `GET /api/platform/analytics/export-csv`.*
> 2. *Tại **Controller** (`PlatformAnalyticsController.java`), gọi `platformAnalyticsService.exportCsv()`.*
> 3. *Tại **Interface** (`PlatformAnalyticsService.java`), định nghĩa hàm xuất báo cáo.*
> 4. *Tại **ServiceImpl** (`PlatformAnalyticsServiceImpl.java`), hệ thống áp dụng **2 kỹ thuật bảo mật cốt lõi**:
>    - Thứ nhất, ghi 3 byte BOM `\uFEFF` ở đầu file để phần mềm Excel mở tiếng Việt có dấu không bao giờ bị lỗi font.
>    - Thứ hai, chạy hàm `escapeCsv()` làm sạch các ký tự nguy hiểm đứng đầu ô như `=`, `+`, `-`, `@` để **triệt tiêu lỗ hổng tấn công DDE Formula Injection**.*
> 5. *Tại **Repository**, `PaymentTransactionRepository` và `EscrowTransactionRepository` giới hạn tối đa 10k dòng để tránh tràn bộ nhớ.*
> 6. *Tại **Database**, MySQL đọc bảng `payment_transactions` và `escrow_transactions`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về các dòng Entity giao dịch.*
> * *ServiceImpl chuyển đổi các dòng dữ liệu thành luồng byte CSV sạch (`byte[]`).*
> * *ServiceImpl return mảng byte kèm định dạng `text/csv` lên Controller.*
> * *Controller trả về file stream kèm header `Content-Disposition`, trình duyệt tự động tải tệp CSV về máy tính ạ."*

---

### LUỒNG 23: PHÁT HIỆN HÀNH VI LÁCH SÀN BẰNG REGEX (LUỒNG 13A / UC-59)
> *"Dạ thưa thầy cô, đây là hệ thống tự động phát hiện gian lận trốn phí nền tảng:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Khi người dùng chat hoặc gửi nội dung, hàm `CircumventionServiceImpl.inspect()` được kích hoạt ngầm.*
> 2. *Tại **ServiceImpl** (`CircumventionServiceImpl.java`), hệ thống chạy **4 bộ lọc Regex thời gian thực**:
>    - Bộ lọc Số điện thoại (Trọng số 80 điểm)
>    - Bộ lọc Email (Trọng số 90 điểm)
>    - Bộ lọc URL/Website (Trọng số 70 điểm)
>    - Bộ lọc Mạng xã hội Zalo/Telegram (Trọng số 65 điểm)
>    - Nếu điểm rủi ro tổng hợp $\ge 65$, hệ thống tự động gắn cờ và tạo bản ghi vi phạm.*
> 3. *Tại **Repository**, `CircumventionDetectionRepository.save()` sinh lệnh SQL.*
> 4. *Tại **Database**, MySQL ghi thông tin vi phạm vào bảng `circumvention_detections`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `CircumventionDetection saved`.*
> * *Khi Admin mở `PlatformCircumventionPage.tsx`, ServiceImpl đóng gói thành DTO `CircumventionDetectionResponse`.*
> * *Controller trả về HTTP 200 OK để React hiển thị danh sách các trường hợp nghi vấn kèm mức điểm rủi ro cho Admin thanh tra ạ."*

---

### LUỒNG 24: BAN HÀNH QUYẾT ĐỊNH XỬ PHẠT & CHẶN TÍNH NĂNG (LUỒNG 13B / UC-60)
> *"Dạ thưa thầy cô, quy trình xử phạt và chốt chặn tính năng:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`PlatformPenaltiesPage.tsx`), Admin chọn người dùng vi phạm và hình thức phạt (Cảnh cáo, Khóa chat, Khóa nhận lớp), gửi `POST /api/platform/penalties`.*
> 2. *Tại **Controller** (`PlatformPenaltyController.java`), gọi `penaltyService.createPenalty()`.*
> 3. *Tại **Interface**, `PenaltyService` định nghĩa tạo phạt, `PenaltyAccessService` định nghĩa chốt chặn quyền.*
> 4. *Tại **ServiceImpl** (`PenaltyServiceImpl.java`), lưu quyết định xử phạt, ghi vết kiểm toán vào `audit_logs`.*
> 5. *Tại **Repository**, `UserPenaltyRepository.save()` lưu quyết định.*
> 6. *Tại **Database**, MySQL ghi bảng `user_penalties` và `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên & Chốt chặn liên module:**
> * *Database trả về Entity `UserPenalty saved`.*
> * *ServiceImpl đóng gói thành DTO `UserPenaltyResponse` trả về cho Admin.*
> * *Đặc biệt: Ở các module khác (như Chat hoặc Nhận lớp), hàm `PenaltyAccessServiceImpl.requireFeature()` sẽ truy vấn bảng `user_penalties`. Nếu người dùng đang chịu án phạt còn hiệu lực, hệ thống lập tức ném lỗi `ForbiddenException` (403 Forbidden) chặn ngay hành động ạ!"*

---

# 👥 PHẦN 5: CỤM NGƯỜI DÙNG, HỢP ĐỒNG & BẢO MẬT ĐỊNH DANH (IDENTITY & CORE)

---

### LUỒNG 25: HỢP ĐỒNG ĐIỆN TỬ KÝ SỐ OTP (UC-44)
*(Thời lượng nói: 1.5 phút — Nghiệp vụ pháp lý)*

> *"Dạ thưa thầy cô, quy trình ký kết hợp đồng điện tử qua mã OTP:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`ContractDetailPage.tsx`), Phụ huynh hoặc Gia sư đọc hợp đồng, bấm nhận mã OTP qua Email và nhập mã 6 số, gửi `POST /api/contract/{id}/sign-otp`.*
> 2. *Tại **Controller** (`ContractController.java`), gọi `contractService.signWithOtp()`.*
> 3. *Tại **Interface** (`ContractService.java`), định nghĩa hàm ký số OTP.*
> 4. *Tại **ServiceImpl** (`ContractServiceImpl.java`), hệ thống xác thực mã OTP trong bảng `email_otps`. Sau đó ghi nhận chữ ký số của bên ký. **Nếu cả 2 bên cùng ký hoàn tất**, hệ thống chuyển trạng thái hợp đồng sang `SIGNED` và kích hoạt tự động khóa tiền Escrow của lớp.*
> 5. *Tại **Repository**, `ContractSignatureRepository.save()`, `ContractRepository.save()`.*
> 6. *Tại **Database**, MySQL ghi bảng `contract_signatures`, cập nhật cột `status = 'SIGNED'` trong bảng `contracts`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity hợp đồng đã hoàn tất ký kết.*
> * *ServiceImpl chuyển đổi thành DTO `ContractDetailResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị con dấu chứng thực điện tử màu xanh trên hợp đồng ạ."*

---

### LUỒNG 26: ĐÁNH GIÁ SAU BUỔI HỌC & ĐIỂM UY TÍN GIA SƯ
> *"Dạ thưa thầy cô, luồng đánh giá sao và cập nhật điểm uy tín:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`ContractDetailPage.tsx`), học viên chấm điểm sao (1 - 5 sao) và nhận xét, gửi `POST /api/contract/reviews`.*
> 2. *Tại **Controller** (`ContractController.java`), gọi `reviewService.createReview()`.*
> 3. *Tại **Interface**, `ReviewService` định nghĩa tạo đánh giá, `ContractService` định nghĩa tính uy tín.*
> 4. *Tại **ServiceImpl** (`ReviewServiceImpl.java`), lưu đánh giá vào bảng `reviews`. Sau đó kích hoạt hàm `ContractServiceImpl.recomputeTutorReputation()` tính lại trung bình cộng số sao của gia sư trên toàn sàn và cập nhật vào hồ sơ.*
> 5. *Tại **Repository**, `ReviewRepository.save()`, `TutorRepository.save()`.*
> 6. *Tại **Database**, MySQL ghi bảng `reviews` và cập nhật cột `rating_avg` trong bảng `tutors`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `Review saved`.*
> * *ServiceImpl đóng gói thành DTO `ReviewResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React cập nhật điểm sao mới trên trang hồ sơ công khai của gia sư ạ."*

---

### LUỒNG 27: QUÊN MẬT KHẨU OTP QUA EMAIL (DEF-62)
> *"Dạ thưa thầy cô, luồng đặt lại mật khẩu an toàn qua OTP Email:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`ResetPasswordPage.tsx`), người dùng nhập mã xác thực OTP từ Email và mật khẩu mới, gửi `POST /api/identity/reset-password`.*
> 2. *Tại **Controller** (`IdentityController.java`), gọi `identityService.resetPassword()`.*
> 3. *Tại **Interface** (`IdentityService.java`), định nghĩa hàm reset password.*
> 4. *Tại **ServiceImpl** (`IdentityServiceImpl.java`), kiểm tra token OTP chưa bị tiêu thụ và còn hạn, mã hóa mật khẩu mới bằng BCrypt, đánh dấu token đã sử dụng, ghi vết kiểm toán vào `audit_logs`.*
> 5. *Tại **Repository**, `UserRepository.save()`, `PasswordResetTokenRepository.save()`.*
> 6. *Tại **Database**, MySQL cập nhật cột `password_hash` bảng `users`, cập nhật bảng `password_reset_tokens` và ghi `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `User` với mật khẩu mới.*
> * *ServiceImpl đóng gói thông điệp thành công vào DTO `ApiResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React tự động chuyển hướng người dùng về màn hình Đăng nhập ạ."*

---

### LUỒNG 28: ĐỔI MẬT KHẨU NGƯỜI DÙNG (CHANGE PASSWORD)
> *"Dạ thưa thầy cô, luồng đổi mật khẩu trong trang cá nhân:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`ProfilePage.tsx`), người dùng nhập mật khẩu hiện tại và mật khẩu mới, gửi `POST /api/identity/change-password`.*
> 2. *Tại **Controller** (`IdentityController.java`), gọi `identityService.changePassword()`.*
> 3. *Tại **Interface** (`IdentityService.java`), định nghĩa hàm đổi mật khẩu.*
> 4. *Tại **ServiceImpl** (`IdentityServiceImpl.java`), dùng `passwordEncoder.matches()` kiểm tra mật khẩu cũ, mã hóa mật khẩu mới bằng BCrypt, ghi audit log.*
> 5. *Tại **Repository**, `UserRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database**, MySQL cập nhật cột `password_hash` bảng `users` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `User` đã cập nhật hash.*
> * *ServiceImpl đóng gói DTO phản hồi thành công.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React hiển thị toast thông báo đổi mật khẩu thành công ạ."*

---

### LUỒNG 29: CHAT TIN NHẮN TỨC THỜI 1-1 & NHÓM LỚP HỌC (UC-50)
> *"Dạ thưa thầy cô, luồng gửi tin nhắn thời gian thực:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`ChatWindow.tsx`), người dùng soạn tin nhắn văn bản, gửi `POST /api/messaging/chats/messages`.*
> 2. *Tại **Controller** (`ChatController.java`), gọi `chatService.sendMessage()`.*
> 3. *Tại **Interface** (`ChatService.java`), định nghĩa hàm gửi tin nhắn.*
> 4. *Tại **ServiceImpl** (`ChatServiceImpl.java`), hệ thống kiểm tra 2 lớp bảo vệ: gọi `penaltyAccessService` chặn người đang bị phạt chat, gọi `circumventionService` quét Regex lách sàn, rồi lưu tin nhắn vào CSDL.*
> 5. *Tại **Repository**, `MessageRepository.save()` thực thi SQL INSERT.*
> 6. *Tại **Database**, MySQL ghi dòng mới vào bảng `messages`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `Message saved`.*
> * *ServiceImpl đóng gói thành DTO `ChatMessageResponse`.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 201 Created, React đẩy tin nhắn mới vào khung chat realtime ạ."*

---

### LUỒNG 30: TẢI ẢNH ĐẠI DIỆN KIỂM TRA MAGIC BYTES CHỐNG MÃ ĐỘC (UC-08)
> *"Dạ thưa thầy cô, luồng tải ảnh đại diện an toàn:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend** (`TutorProfilePage.tsx`), người dùng chọn tệp ảnh tải lên, gửi `POST /api/profile/avatar` dạng `multipart/form-data`.*
> 2. *Tại **Controller** (`ProfileController.java`), gọi `profileService.uploadAvatar()`.*
> 3. *Tại **Interface** (`ProfileService.java`), định nghĩa hàm upload avatar.*
> 4. *Tại **ServiceImpl** (`ProfileServiceImpl.java`), hệ thống đọc mảng byte đầu tiên của file để kiểm tra **Magic Bytes** thực tế (PNG: `89 50 4E 47`, JPEG: `FF D8 FF`), chặn đứng các tệp mã độc giả mạo đuôi ảnh, lưu tệp và cập nhật đường dẫn ảnh vào user.*
> 5. *Tại **Repository**, `UserRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database**, MySQL cập nhật cột `avatar_url` trong bảng `users`.*
> 
> ⬆️ **Chiều đi ngược lên:**
> * *Database trả về Entity `User` đã có avatar mới.*
> * *ServiceImpl đóng gói thành DTO `AvatarUploadResponse` chứa URL ảnh.*
> * *ServiceImpl return DTO lên Controller.*
> * *Controller trả về HTTP 200 OK, React cập nhật ảnh đại diện mới trên thanh điều hướng ạ."*

---

### LUỒNG 31: ĐĂNG XUẤT & THU HỒI PHIÊN JWT BẰNG TOKEN_VERSION (LOGOUT)
*(Thời lượng nói: 1 phút — Câu hỏi bẫy yêu thích của hội đồng)*

> *"Dạ thưa thầy cô, cơ chế thu hồi phiên JWT không cần Redis:
> 
> ⬇️ **Chiều đi xuống:**
> 1. *Tại **Frontend**, người dùng bấm 'Đăng xuất', xóa token ở LocalStorage và gửi `POST /api/identity/logout`.*
> 2. *Tại **Controller** (`IdentityController.java`), gọi `identityService.logout()`.*
> 3. *Tại **Interface** (`IdentityService.java`), định nghĩa hàm đăng xuất.*
> 4. *Tại **ServiceImpl** (`IdentityServiceImpl.java`), hệ thống thực hiện thao tác: **`user.setTokenVersion(user.getTokenVersion() + 1L)`**, tăng giá trị phiên đăng nhập thêm 1 đơn vị và ghi audit log.*
> 5. *Tại **Repository**, `UserRepository.save()` thực thi SQL UPDATE.*
> 6. *Tại **Database**, MySQL cập nhật cột `token_version` trong bảng `users` và ghi bảng `audit_logs`.*
> 
> ⬆️ **Chiều đi ngược lên & Cơ chế thu hồi Stateless:**
> * *Database trả về Entity `User` đã cập nhật phiên bản token.*
> * *ServiceImpl đóng gói DTO phản hồi thành công trả về cho Controller.*
> * *Controller trả về HTTP 200 OK, React chuyển hướng người dùng về trang chủ.*
> * *Về mặt kỹ thuật: Trong `JwtAuthenticationFilter`, mỗi request gửi lên đều so khớp `principal.getTokenVersion() != jwtService.extractTokenVersion(claims)`. Khi thấy version trong CSDL đã tăng lên lệch so với token cũ, request lập tức bị từ chối 401 Unauthorized, vô hiệu hóa toàn bộ token cũ ngay lập tức mà không cần tốn chi phí duy trì Redis blacklist ạ!"*

---

# 🏆 PHẦN 6: LỜI KẾT THÚC THUYẾT TRÌNH ẤN TƯỢNG (15 GIÂY)

> *"Dạ kính thưa quý thầy cô trong hội đồng, trên đây là toàn bộ kiến trúc 6 tầng và chu trình dữ liệu 2 chiều khép kín trong các phân hệ mà em trực tiếp đảm nhiệm và phát triển.*
> 
> *Em xin chân thành cảm ơn quý thầy cô đã chú ý lắng nghe và em rất sẵn sàng nhận các câu hỏi nhận xét, phản biện từ hội đồng ạ!"*
