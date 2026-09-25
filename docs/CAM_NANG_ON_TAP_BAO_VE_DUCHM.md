# CẨM NANG TOÀN DIỆN ÔN TẬP & BẢO VỆ ĐỒ ÁN CAPSTONE / SWP391
## DÀNH RIÊNG CHO: HOÀNG MINH ĐỨC (DucHM — HE187354 / `mduc1011-swp`)
**Dự án**: Tutor Connect System (TCS) — Hệ thống kết nối Gia sư thông minh  
**Khung giờ bảo vệ chính thức**: **10:50 – 12:20, Thứ Bảy ngày 26/09/2026**  
**Kiến trúc chuẩn**: Spring Boot 3-Tier Enterprise Architecture (Mô hình 6 Tầng Chuẩn)  
**Mục tiêu**: Làm chủ 100% luồng code, giải thích tường minh từ UI đến CSDL, tự tin bảo vệ xuất sắc trước hội đồng.

---

# 🏛️ MÔ HÌNH 6 TẦNG KIẾN TRÚC SPRING BOOT (6-TIER ARCHITECTURE)
*(Khi thầy cô yêu cầu giải thích luồng, bạn luôn trình bày theo đúng 6 mắt xích này)*

```
[Tầng 1: Frontend UI]         -> React Component, Form, Modal & Axios API Request
          │
          ▼
[Tầng 2: REST Controller]     -> Nhận HTTP Request, Validate Payload DTO (@Valid)
          │
          ▼
[Tầng 3: Service Interface]   -> Định nghĩa hợp đồng trừu tượng (Abstraction / Dependency Inversion)
          │
          ▼
[Tầng 4: Service Impl]        -> Xử lý nghiệp vụ chính, bảo mật, SLA, transactional (@Transactional)
          │
          ▼
[Tầng 5: Repository Interface]-> Spring Data JPA Interface (JpaRepository) & Custom JPQL Query (@Query)
          │
          ▼
[Tầng 6: Entity & Database]   -> Thực thể JPA ánh xạ Bảng CSDL MySQL & Cột dữ liệu tác động
```

---

# MỤC LỤC
1. [PHẦN 0: BẢN NGUYÊN & GIẢI MÃ BẢN CHẤT 6 TẦNG KIẾN TRÚC ENTERPRISE SPRING BOOT](#phần-0-bản-nguyên--giải-mã-bản-chất-6-tầng-kiến-trúc-enterprise-spring-boot)
2. [PHẦN 1: BẢNG MA TRẬN TRA CỨU NHANH 6 TẦNG (CHEAT SHEET MATRIX)](#phần-1-bảng-ma-trận-tra-cứu-nhanh-6-tầng-cheat-sheet-matrix)
3. [PHẦN 2: GIẢI PHẪU CHI TIẾT TỪNG LUỒNG CODE THỰC CHIẾN 6 TẦNG](#phần-2-giải-phẫu-chi-tiết-từng-luồng-code-thực-chiến-6-tầng)
   - [CHƯƠNG 1: PHÂN HỆ HỖ TRỢ KHÁCH HÀNG (BF-09)](#chương-1-phân-hệ-hỗ-trợ-khách-hàng-bf-09)
   - [CHƯƠNG 2: PHÂN HỆ ĐIỀU HÀNH SÀN & BẢO MẬT (BF-10)](#chương-2-phân-hệ-điều-hành-sàn--bảo-mật-bf-10)
   - [CHƯƠNG 3: TRỢ LÝ ẢO AI UNIVERSAL RAG 12 BƯỚC (UC-65)](#chương-3-trỢ-lý-ảo-ai-universal-rag-12-bước-uc-65)
   - [CHƯƠNG 4: HỢP ĐỒNG ĐIỆN TỬ OTP & HỒ SƠ 3 VAI TRÒ (UC-44, M4, UC-08)](#chương-4-hợp-đồng-điện-tử-otp--hồ-sơ-3-vai-trò-uc-44-m4-uc-08)
4. [PHẦN 3: KỊCH BẢN THUYẾT TRÌNH MẪU "CHỈ CODE 6 BƯỚC"](#phần-3-kịch-bản-thuyết-trình-mẫu-chỉ-code-6-bước)
5. [PHẦN 4: BỘ CÂU HỎI PHẢN BIỆN "BẪY" CỦA HỘI ĐỒNG & CÂU TRẢ LỜI MẪU](#phần-4-bộ-câu-hỏi-phản-biện-bẫy-của-hội-đồng--câu-trả-lời-mẫu)

---

# PHẦN 0: BẢN NGUYÊN & GIẢI MÃ BẢN CHẤT 6 TẦNG KIẾN TRÚC ENTERPRISE SPRING BOOT
*(Phần này giải thích tường minh vì sao phải chia 6 tầng, mỗi tầng làm gì, dữ liệu biến đổi ra sao để bạn trả lời hội đồng với phong thái của một Senior Developer)*

---

### 🏛️ TỔNG QUAN VÒNG ĐỜI DỮ LIỆU 6 TẦNG (END-TO-END DATA LIFECYCLE)

Khi người dùng thực hiện một hành động (ví dụ: bấm nút "Gửi yêu cầu hỗ trợ" trên web), dòng dữ liệu không đi thẳng vào database mà trải qua một chu trình chuyển hóa 6 bước nghiêm ngặt:

1. **Frontend UI**: Thu thập input từ người dùng ➡️ Validate sơ bộ form ➡️ Đóng gói thành **JSON Payload** ➡️ Gửi qua giao thức HTTP (kèm Authorization Bearer Header chứa JWT Token).
2. **REST Controller**: Cổng đón tiếp HTTP ➡️ Bộ lọc `JwtAuthenticationFilter` giải mã Token và xác thực danh tính ➡️ Spring ánh xạ JSON sang Java Object (**Request DTO**) ➡️ Kích hoạt Bean Validation (`@Valid`) ➡️ Ủy quyền xử lý cho Service Interface.
3. **Service Interface**: Định nghĩa **Bản hợp đồng trừu tượng (Contract)** ➡️ Khai báo tên hàm, tham số nhận vào (`Request DTO`) và kết quả trả về (`Response DTO`) ➡️ Cách ly hoàn toàn Controller khỏi chi tiết triển khai bên trong.
4. **Service Implementation**: **"Bộ não" nghiệp vụ** ➡️ Kiểm tra quyền người dùng (`AuthHelper`) ➡️ Áp dụng thuật toán (ép SLA, kiểm tra trùng lặp, tính điểm uy tín) ➡️ Quản lý Transaction (`@Transactional` đảm bảo tính toàn vẹn ACID) ➡️ Chuyển đổi DTO sang **Entity JPA** ➡️ Gọi Repository để lưu hoặc truy vấn.
5. **Repository Interface**: **Cầu nối truy cập dữ liệu** ➡️ Kế thừa `JpaRepository` của Spring Data JPA ➡️ Tự động sinh câu lệnh SQL hoặc thực thi các câu truy vấn JPQL tùy biến (`@Query`) tối ưu chỉ mục ➡️ Chuyển đổi các dòng bản ghi trong MySQL thành đối tượng Entity Java.
6. **Entity & Database Table**: **Tầng lưu trữ bền vững** ➡️ Các class Entity (`@Entity`, `@Table`) phản chiếu cấu trúc bảng MySQL ➡️ Ràng buộc khóa chính (`@Id`), khóa ngoại (`@ManyToOne`, `@JoinColumn`), chỉ mục và ràng buộc toàn vẹn.

---

### 🔍 GIẢI PHẪU CHI TIẾT TỪNG TẦNG KIẾN TRÚC

#### 🌐 TẦNG 1: FRONTEND UI LAYER (TRẢI NGHIỆM & GIAO TIẾP MẠNG)
* **Bản chất**: Ứng dụng Single Page Application (SPA) xây dựng bằng **React 19 & TypeScript**.
* **Trách nhiệm chính**:
  1. Render giao diện người dùng, xử lý trạng thái hiển thị (Loading spinner, Error banner, Success modal).
  2. Validate định dạng người dùng nhập ngay tại Client (ví dụ: email đúng định dạng, ô bắt buộc không để trống) để giảm tải cho máy chủ.
  3. Đính kèm Token JWT vào `Authorization: Bearer <token>` thông qua Axios Interceptor.
  4. Nhận JSON Response từ backend và cập nhật UI thời gian thực (hiển thị thẻ Card, cập nhật bảng phân trang).
* **Các file tiêu biểu trong code của bạn**:
  - `MessagingPanel.tsx`: Form tạo ticket hỗ trợ.
  - `PlatformTicketsPage.tsx`: Màn hình Admin quản lý danh sách Ticket, phản hồi, gộp ticket.
  - `PlatformUsersPage.tsx`: Màn hình Admin quản lý tài khoản và khóa người dùng.
  - `ContractDetailPage.tsx`: Màn hình chi tiết hợp đồng và modal nhập mã OTP ký số.

---

#### 🚪 TẦNG 2: REST CONTROLLER LAYER (CỔNG ĐÓN TIẾP & ĐỊNH TUYẾN)
* **Bản chất**: Lớp giao tiếp trực tiếp với môi trường Web bên ngoài, tiếp nhận và phản hồi các HTTP Request.
* **Trách nhiệm chính**:
  1. **Định tuyến (Routing)**: Sử dụng `@RestController` và `@RequestMapping` để phân phối URL đến đúng phương thức xử lý.
  2. **Ánh xạ dữ liệu (Data Binding)**: Sử dụng `@RequestBody` để tự động parse chuỗi JSON thành đối tượng DTO, `@PathVariable` để lấy ID trên URL, `@RequestParam` để lấy tham số phân trang, bộ lọc.
  3. **Kiểm tra hợp lệ đầu vào (Input Validation)**: Dùng annotation `@Valid` kết hợp `@NotNull`, `@NotBlank`, `@Size` trên DTO. Nếu dữ liệu sai, Spring tự động quăng lỗi 400 Bad Request ngay tại cửa ngõ, không cho đi sâu vào tầng Service gây lãng phí tài nguyên.
  4. **Quy chuẩn mã HTTP**: Trả về đúng HTTP Status Code: `200 OK`, `201 CREATED`, `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 NOT FOUND`.
* **Quy tắc vàng của Controller**: **Tuyệt đối KHÔNG viết logic nghiệp vụ (No Business Logic in Controller)**. Controller chỉ làm nhiệm vụ "nghe lệnh, đón dữ liệu, gọi Service, trả kết quả".
* **Các file tiêu biểu**:
  - `MessagingController.java`: `@PostMapping("/tickets")`
  - `PlatformController.java`: `@GetMapping("/tickets")`, `@PatchMapping("/users/{id}/status")`
  - `ContractController.java`: `@PostMapping("/{id}/sign-otp")`

---

#### 📜 TẦNG 3: SERVICE INTERFACE LAYER (BẢN HỢP ĐỒNG TRỪU TƯỢNG - CONTRACT)
* **Bản chất**: Tập hợp các khai báo hàm trừu tượng (`public interface`), không chứa code thực thi.
* **Tại sao bắt buộc phải có Service Interface mà không dùng thẳng ServiceImpl?**
  1. **Nguyên lý Dependency Inversion (Chữ D trong SOLID)**: Các module cấp cao (Controller) không nên phụ thuộc vào module cấp thấp (ServiceImpl), cả hai phải phụ thuộc vào sự trừu tượng (Interface).
  2. **Giảm thiểu phụ thuộc chặt (Loose Coupling)**: Tầng Controller chỉ biết tên hàm và tham số cần truyền, hoàn toàn không cần biết bên trong ServiceImpl dùng MySQL, Redis hay gọi sang Microservice bên thứ ba.
  3. **Hỗ trợ Kiểm thử Tự động (Unit Testing)**: Khi viết test cho Controller, lập trình viên có thể dễ dàng dùng `@MockBean PlatformService` để giả lập dữ liệu trả về mà không cần khởi động toàn bộ logic phức tạp của ServiceImpl hay kết nối Database thật.
  4. **Tính mở rộng (Polymorphism)**: Sau này nếu muốn đổi logic xử lý (ví dụ: chuyển từ gửi OTP qua Email sang SMS hoặc WhatsApp), ta chỉ cần tạo thêm class `SmsContractServiceImpl` thực thi cùng `ContractService` mà không cần sửa 1 dòng code nào ở `ContractController`.
* **Các file tiêu biểu**:
  - `MessagingService.java`
  - `PlatformService.java`
  - `ContractService.java`

---

#### ⚙️ TẦNG 4: SERVICE IMPLEMENTATION LAYER (TRÁI TIM NGHIỆP VỤ - BUSINESS ENGINE)
* **Bản chất**: Class được đánh dấu `@Service`, nơi chứa 100% logic thông minh, quy tắc nghiệp vụ và các thuật toán của dự án.
* **Trách nhiệm chính**:
  1. **Kiểm tra quyền hạn & Bảo mật nghiệp vụ**: Dùng `authHelper.currentUserId()` hoặc `authHelper.requireRole(PLATFORM_ADMIN)` để đảm bảo người gọi có đủ thẩm quyền thực hiện thao tác.
  2. **Thực thi quy tắc nghiệp vụ (Business Rules)**:
     - Thuật toán ép sàn mức ưu tiên (`escalatePriority`: `DISPUTE` ➡️ `URGENT`).
     - Thuật toán tính hạn cam kết SLA (`dueAt = now + 4h`).
     - 4 chốt chặn an toàn khi Gộp Ticket (kiểm tra cùng `userId`, kiểm tra trạng thái chưa đóng).
     - Kiểm tra tư cách pháp lý khi ký hợp đồng (`assertSignerNotMinor()`, `assertSignerCccdComplete()`).
     - Thuật toán tính lại điểm uy tín gia sư theo đánh giá cuối của từng lớp (`recomputeTutorReputation`).
  3. **Quản lý Giao dịch CSDL (`@Transactional`)**: Đảm bảo nguyên lý ACID. Nếu một chuỗi thao tác gồm 5 bước (ví dụ: trừ tiền ví ➡️ tạo bản ghi giao dịch ➡️ chuyển trạng thái hợp đồng) bị lỗi ở bước thứ 4, `@Transactional` sẽ tự động **Rollback toàn bộ**, không bao giờ để xảy ra tình trạng tiền bị trừ mà hợp đồng chưa kích hoạt!
  4. **Ánh xạ dữ liệu (Data Transformation)**: Chuyển đổi qua lại giữa `Request DTO` ➡️ `Entity JPA` để lưu CSDL, và từ `Entity JPA` ➡️ `Response DTO` để trả về cho Client (thông qua Mapper).
  5. **Tích hợp liên phân hệ & Ghi nhật ký**: Kích hoạt sự kiện Spring Event (ví dụ: `publishContractSigned`), ghi vết kiểm toán bất biến qua `auditLogService.record(...)`, và phát thông báo thời gian thực qua `notificationDispatchService`.
* **Các file tiêu biểu**:
  - `MessagingServiceImpl.java`
  - `PlatformServiceImpl.java`
  - `ContractServiceImpl.java`
  - `AiServiceImpl.java`

---

#### 🔌 TẦNG 5: REPOSITORY INTERFACE LAYER (CẦU NỐI TRUY CẬP DỮ LIỆU - DATA ACCESS)
* **Bản chất**: Interface kế thừa `JpaRepository<T, ID>` của framework Spring Data JPA, là cầu nối trực tiếp giữa mã nguồn Java và hệ quản trị CSDL MySQL.
* **Trách nhiệm chính & 3 Cơ chế truy vấn cốt lõi**:
  1. **Phương thức CRUD có sẵn**: Spring Data JPA tự động sinh mã bytecode cho các thao tác chuẩn: `save()`, `findById()`, `findAll()`, `delete()`, `count()`, lập trình viên không phải viết một dòng lệnh SQL nào.
  2. **Query Derivation (Tự động sinh SQL từ tên hàm)**:
     Ví dụ: `findByUser_UserIdOrderByCreatedAtDesc(Long userId)` ➡️ Spring tự động phân tích tên hàm thành câu lệnh SQL:
     ```sql
     SELECT * FROM support_tickets WHERE user_id = ? ORDER BY created_at DESC;
     ```
  3. **Custom JPQL / Native Query (`@Query`)**: Dành cho các tác vụ nghiệp vụ phức tạp:
     - Tìm kiếm đa tiêu chí kết hợp phân trang:
       ```java
       @Query("SELECT t FROM SupportTicket t WHERE (:status IS NULL OR t.status = :status) ...")
       Page<SupportTicket> search(... Pageable pageable);
       ```
     - Quét các ticket vi phạm SLA:
       ```java
       @Query("SELECT t FROM SupportTicket t WHERE t.status NOT IN (:excludedStatuses) AND t.dueAt < :now AND t.slaBreached = false")
       List<SupportTicket> findBreachedCandidateTickets(...);
       ```
* **Các file tiêu biểu**:
  - `SupportTicketRepository.java`
  - `TicketMessageRepository.java`
  - `UserRepository.java`
  - `ContractRepository.java`

---

#### 🗄️ TẦNG 6: ENTITY & DATABASE PERSISTENCE LAYER (LƯU TRỮ BỀN VỮNG)
* **Bản chất**:
  - Phía Java: Các class Entity được chú thích bằng JPA/Hibernate (`@Entity`, `@Table`).
  - Phía CSDL: Các bảng vật lý trong MySQL Database lưu trữ dữ liệu bền vững trên ổ đĩa cứng.
* **Trách nhiệm chính & Các Annotation cốt lõi**:
  - `@Entity`: Báo cho Spring Boot biết đây là một thực thể CSDL cần được quản lý.
  - `@Table(name = "support_tickets")`: Khai báo tên bảng vật lý tương ứng trong MySQL.
  - `@Id` & `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Định nghĩa khóa chính tự động tăng (Auto-increment Primary Key).
  - `@Enumerated(EnumType.STRING)`: Ép các Enum (như `OPEN`, `IN_PROGRESS`, `URGENT`) lưu xuống database dưới dạng chuỗi văn bản rõ ràng thay vì số nguyên (0, 1, 2), giúp quản trị viên mở MySQL Workbench lên đọc hiểu dữ liệu tức thì.
  - `@ManyToOne` & `@JoinColumn(name = "user_id")`: Thiết lập quan hệ khóa ngoại (Foreign Key) đảm bảo tính toàn vẹn tham chiếu giữa bảng con và bảng cha.
* **Các Entity & Bảng tiêu biểu**:
  - `SupportTicket.java` ➡️ Bảng `support_tickets`
  - `TicketMessage.java` ➡️ Bảng `ticket_messages`
  - `User.java` ➡️ Bảng `users`
  - `Contract.java` ➡️ Bảng `contracts`
  - `AuditLog.java` ➡️ Bảng `audit_logs`

---

### 📊 BẢNG TỔNG KẾT: "NẾU THIẾU TẦNG NÀY THÌ HỆ THỐNG SẼ BỊ GÌ?"
*(Bảng này giúp bạn đối đáp cực kỳ thông minh khi thầy cô hỏi vặn về vai trò của từng tầng)*

| Tầng Kiến Trúc | Nếu KHÔNG có tầng này thì hậu quả là gì? |
| :--- | :--- |
| **Tầng 1: Frontend UI** | Người dùng không có giao diện trực quan, phải dùng Postman hoặc Terminal gõ cURL để gọi API. |
| **Tầng 2: Controller** | Hệ thống không thể tiếp nhận request từ Internet, không kiểm soát được URL, không có chốt chặn validate đầu vào sơ bộ. |
| **Tầng 3: Service Interface** | Vi phạm nguyên lý SOLID (Dependency Inversion), Controller phụ thuộc chặt vào ServiceImpl, cực kỳ khó viết Unit Test Mock dữ liệu và không thể mở rộng đa giải pháp. |
| **Tầng 4: Service Impl** | Hệ thống không có "bộ não" kiểm tra nghiệp vụ, không bảo vệ được dữ liệu qua `@Transactional`, để mặc người dùng gửi dữ liệu sai gây sập database. |
| **Tầng 5: Repository Interface** | Lập trình viên phải tự viết mã JDBC thủ công (`Connection`, `PreparedStatement`, `ResultSet`), code dài gấp 10 lần, dễ dính lỗ hổng SQL Injection và không tận dụng được cơ chế Cache/Phân trang của Hibernate. |
| **Tầng 6: Entity & Database** | Dữ liệu chỉ nằm trên RAM máy chủ, khi tắt server hoặc khởi động lại máy là mất sạch toàn bộ thông tin người dùng và giao dịch. |

---

# PHẦN 1: BẢNG MA TRẬN TRA CỨU NHANH 6 TẦNG (CHEAT SHEET MATRIX)
*(Bảng này mở sẵn trên một góc màn hình lúc bảo vệ. Thầy cô hỏi chức năng nào, liếc 1 giây là biết mở file nào dòng nào!)*

| STT | Tên Chức Năng / Use Case (Khớp Kịch Bản Nói) | Tầng 1: Frontend UI | Tầng 2: Controller & Endpoint | Tầng 3: Service Interface | Tầng 4: Service Implementation & Thuật Toán | Tầng 5: Repository Interface | Tầng 6: Bảng CSDL Tác Động |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | **// LUỒNG 1: Tra cứu FAQ công khai** | `HelpPage.tsx` (L1) | `CatalogController.java` (L62, L72)<br>`GET /api/catalog/faq` | `CatalogService.java`<br>`getFaqEntries()` | `CatalogServiceImpl.java` (L205): `findByPublishedTrue...`, In-memory Ranking Engine Unicode NFD, mapper `toFaq` | `FaqEntryRepository.java`<br>`findByPublishedTrue...` | `faq_entries` |
| **2** | **// LUỒNG 2: Trợ lý AI RAG 12 Bước (UC-65)** | `AiFloatingWidget.tsx`,<br>`AiAssistantPage.tsx` | `AiController.java` (L50)<br>`POST /api/ai/chat` | `AiService.java`<br>`chat()` | `AiServiceImpl.java` (L114): RAG 12 bước, tiêm số liệu CSDL thực tế (Bước 5.1), gọi Gemini qua `AiProviderRouter` (L40), Hallucination Guard (Bước 9) | `AiChatMessageRepository`<br>`AiKnowledgeChunkRepository` | `ai_chat_messages`, `ai_chat_sessions`,<br>`ai_knowledge_chunks` |
| **3** | **// LUỒNG 3: User tạo Ticket & SLA (UC-65, UC-66)** | `MessagingPanel.tsx` (L120) | `MessagingController.java` (L76, L122)<br>`POST /api/messaging/support-tickets` | `MessagingService.java` (L19)<br>`createSupportTicket()` | `MessagingServiceImpl.java` (L153): ép sàn priority (`escalatePriority`), tính hạn SLA (`calculateDueAt`), bắn in-app thông báo Admin, mapper `toResponse` | `SupportTicketRepository.java`<br>`TicketMessageRepository.java` | `support_tickets`, `ticket_messages`,<br>`notifications` |
| **4** | **// LUỒNG 4A: Admin lọc & xem danh sách Ticket** | `PlatformTicketsPage.tsx` (L25) | `PlatformController.java` (L325)<br>`GET /api/platform/tickets` | `PlatformService.java` (L84)<br>`getTickets()` | `PlatformServiceImpl.java` (L1404): gọi `search(...)` phân trang, mapper `toResponse` | `SupportTicketRepository.java` (L37)<br>`search(status, category, ...)` | `support_tickets` |
| **5** | **// LUỒNG 4B: Admin mở Ticket (Auto-Assign PIC)** | `PlatformTicketsPage.tsx` (L63) | `PlatformController.java` (L335)<br>`GET /api/platform/tickets/{ticketId}` | `PlatformService.java` (L89)<br>`getTicketDetail()` | `PlatformServiceImpl.java` (L1441): gán `assignedAdmin = currentAdmin`, đổi `status = IN_PROGRESS`, mapper `toDetailResponse` | `SupportTicketRepository.java`<br>`findById()`, `save()` | `support_tickets`<br>(`assigned_admin_id`, `status='IN_PROGRESS'`) |
| **6** | **// LUỒNG 4C: Admin phản hồi Ticket (First SLA)** | `PlatformTicketsPage.tsx` (L94) | `PlatformController.java` (L362)<br>`POST /api/platform/tickets/{id}/messages` | `PlatformService.java` (L95)<br>`respondToTicket()` | `PlatformServiceImpl.java` (L1546): tính `responseSlaMs = now - createdAt`, gọi `auditLogService.record(...)`, mapper `toTicketMessageResponse` | `TicketMessageRepository.java`<br>`SupportTicketRepository.java` | `ticket_messages`, `support_tickets`<br>(`response_sla_ms`), `audit_logs` |
| **7** | **// LUỒNG 4D: Admin đóng / giải quyết Ticket** | `PlatformTicketsPage.tsx` (L99) | `PlatformController.java` (L374)<br>`PATCH /api/platform/tickets/{id}/status` | `PlatformService.java` (L98)<br>`closeTicket()` | `PlatformServiceImpl.java` (L1603): cập nhật `status = CLOSED`, `closedAt = now`, gọi `auditLogService.record(...)`, mapper `toResponse` | `SupportTicketRepository.java`<br>`save()` | `support_tickets`<br>(`status`, `resolved_at`, `closed_at`), `audit_logs` |
| **8** | **// LUỒNG 5A: Gộp Ticket trùng lặp (BF-08)** | `PlatformTicketsPage.tsx` (L103) | `PlatformController.java` (L389)<br>`POST /api/platform/tickets/{id}/merge` | `PlatformService.java` (L101)<br>`mergeTicket()` | `PlatformServiceImpl.java` (L1653): 4 chốt chặn, chuyển tin nhắn `saveAll()`, đóng ticket phụ `status = CLOSED`, ghi `audit_logs`, mapper `toDetailResponse` | `TicketMessageRepository.java`<br>`SupportTicketRepository.java` | `ticket_messages`, `support_tickets`<br>(`status='CLOSED'`), `audit_logs` |
| **9** | **// LUỒNG 5B: Chuyển Ticket sang Tranh chấp Escrow** | `PlatformTicketsPage.tsx` (L112) | `PlatformController.java` (L404)<br>`POST /api/platform/tickets/{id}/redirect-dispute` | `PlatformService.java` (L107)<br>`redirectTicketToDispute()` | `PlatformServiceImpl.java` (L1841): tạo `Report` (`targetType = CLASS`), đóng ticket cũ, ghi `audit_logs` | `ReportRepository.java`<br>`SupportTicketRepository.java` | `reports` (`target_type='CLASS'`),<br>`support_tickets`, `audit_logs` |
| **10** | **// LUỒNG 6: Quản trị FAQ (Admin CRUD & Duyệt nháp)** | `PlatformFaqPage.tsx` (L1) | `CatalogController.java` (L91, L100-135)<br>`/api/catalog/faq/admin`, `/api/catalog/faq` | `CatalogService.java`<br>`createFaqEntry()`, `update...` | `CatalogServiceImpl.java` (L316): gán `displayOrder`, `isPublished` (Draft/Published), ghi `audit_logs`, mapper `toFaqAdmin` | `FaqEntryRepository.java`<br>`save()`, `delete()` | `faq_entries`, `audit_logs` |
| **11** | **// LUỒNG 7: Quét & nâng cấp quá hạn SLA (JOB-11)** | `useAdminTickets.tsx` (L45) | `PlatformController.java` (L417)<br>`POST /api/platform/tickets/sla/scan` | `PlatformService.java` (L104)<br>`scanAndEscalateSlaBreaches()` | `PlatformServiceImpl.java` (L1738): quét `dueAt < now`, gán `slaBreached = true`, nâng `priority = URGENT`, gọi `saveAll()`, ghi `audit_logs` | `SupportTicketRepository.java` (L48)<br>`findBreachedCandidateTickets()` | `support_tickets`<br>(`priority`, `sla_breached=true`), `audit_logs` |
| **12** | **// LUỒNG 8: Admin Dashboard KPI & Hàng đợi trực ban** | `PlatformDashboardPage.tsx` (L1) | `PlatformController.java` (L158)<br>`GET /api/platform/dashboard` | `PlatformService.java` (L56)<br>`getDashboard()` | `PlatformServiceImpl.java` (L451) + `PlatformTaskQueueServiceImpl.java` (L65): đếm user, đếm lớp, tổng tiền Escrow, gom task khẩn cấp | `UserRepository`, `TutoringClassRepo`,<br>`EscrowTransactionRepo`, `SupportTicketRepo` | `users`, `tutoring_classes`,<br>`escrow_transactions`, `wallets` |
| **13** | **// LUỒNG 9: Cây danh mục đệ quy O(N) (UC-57)** | `PlatformCategoriesPage.tsx` (L1) | `CatalogController.java` (L138)<br>`GET /api/catalog/categories/tree` | `CatalogService.java`<br>`getCategoryTree()` | `CatalogServiceImpl.java` (L422): gọi `findAll()` lấy 1 lần duy nhất, dựng cây bằng `HashMap` RAM $O(N)$ | `SubjectCategoryRepository.java`<br>`findAll()` | `subject_categories` |
| **14** | **// LUỒNG 10: Cấu hình tham số toàn cục & Phí sàn** | `PlatformSettingsPage.tsx` (L1) | `SystemParameterController.java` (L52)<br>`PUT /api/catalog/admin/parameters/{key}` | `SystemParameterService.java`<br>`updateParameter()` | `SystemParameterServiceImpl.java` (L51), `CatalogServiceImpl.java` (L520): validate tỷ lệ (10%), gọi `save()`, ghi `audit_logs` | `SystemParameterRepository.java`<br>`save()` | `system_parameters`, `audit_logs` |
| **15** | **// LUỒNG 11: Giám sát Nhật ký kiểm toán (Audit Logs)** | `PlatformAuditLogsPage.tsx` (L1) | `AuditLogController.java` (L30)<br>`GET /api/platform/audit-logs` | `AuditLogService.java`<br>`getAuditLogs()`, `record()` | `AuditLogServiceImpl.java` (L71): tìm kiếm phân trang; hàm `record()` tuân thủ nguyên tắc Append-only (chỉ INSERT & SELECT, cấm update/delete) | `AuditLogRepository.java`<br>`search()`, `save()` | `audit_logs` (Bất biến) |
| **16** | **// LUỒNG 12: Báo cáo Tài chính & Xuất CSV an toàn** | `PlatformAnalyticsPage.tsx` (L1) | `PlatformAnalyticsController.java` (L80)<br>`GET /api/platform/analytics/export-csv` | `PlatformAnalyticsService.java`<br>`exportCsv()` | `PlatformAnalyticsServiceImpl.java` (L374): lấy tối đa 10k dòng, ghi byte BOM `\uFEFF` chống lỗi font, hàm `escapeCsv()` làm sạch `=, +, -, @` chống DDE Injection | `PaymentTransactionRepository`<br>`EscrowTransactionRepository` | `payment_transactions`, `escrow_transactions` |
| **17** | **// LUỒNG 13A: Phát hiện lách sàn Regex (UC-59)** | `PlatformCircumventionPage.tsx` (L1) | `CircumventionController.java` (L30)<br>`GET /api/platform/circumventions` | `CircumventionService.java`<br>`inspect()`, `review()` | `CircumventionServiceImpl.java` (L59): chạy 4 bộ lọc Regex (SĐT 80, Email 90, URL 70, Social 65), nếu điểm >= 65 tự động gắn cờ vi phạm | `CircumventionDetectionRepository.java`<br>`save()`, `findAll()` | `circumvention_detections`, `messages` |
| **18** | **// LUỒNG 13B: Xử phạt & Chốt chặn tính năng (UC-60)** | `PlatformPenaltiesPage.tsx` (L1) | `PlatformPenaltyController.java` (L45)<br>`POST /api/platform/penalties` | `PenaltyService.java`,<br>`PenaltyAccessService.java` (L25) | `PenaltyServiceImpl.java` (L101) lưu án phạt; `PenaltyAccessServiceImpl.java` (L45) truy vấn CSDL và quăng `ForbiddenException` (403) chặn quyền chat/nhận lớp | `UserPenaltyRepository.java`<br>`save()`, `findByUser_UserIdAndStatus()` | `user_penalties`, `audit_logs` |
| **19** | **LUỒNG 14: Quản lý User & Khóa Token JWT tức thì** | `PlatformUsersPage.tsx` (L1) | `PlatformController.java` (L135)<br>`PATCH /api/platform/users/{id}/status` | `PlatformService.java` (L49)<br>`updateUserStatus()` | `PlatformServiceImpl.java` (L344): cấm ban Root Admin, đổi `status = BANNED`, ghi `audit_logs`, mapper `toUserListItem` (Lọc: `JwtAuthenticationFilter:43` kiểm tra `isEnabled == false` từ chối 401 ngay) | `UserRepository.java`<br>`save()` | `users` (`status='BANNED'`),<br>`audit_logs` |
| **20** | **LUỒNG 15: Thẩm định danh tính eKYC CCCD / Bằng cấp** | `PlatformVerificationsPage.tsx` (L1) | `PlatformController.java` (L199)<br>`POST /api/platform/verifications/{id}/review` | `PlatformService.java` (L62)<br>`reviewVerification()` | `PlatformServiceImpl.java` (L542): lưu lịch sử, cập nhật `verification_status = VERIFIED`, ghi `audit_logs` | `VerificationRequestRepository`<br>`TutorRepository`, `TutorCenterRepo` | `verification_requests`, `verification_histories`,<br>`tutors.verification_status`, `audit_logs` |
| **21** | **LUỒNG 16: Xử lý sự cố lớp học & Tự động khóa Escrow** | `PlatformReportsPage.tsx` (L1) | `PlatformController.java` (L271)<br>`PATCH /api/platform/reports/{id}/resolve` | `PlatformService.java` (L66)<br>`resolveClassIssueReport()` | `PlatformServiceImpl.java` (L1726): 7 hành động can thiệp, gọi `escrowService.holdForDispute()` đóng băng tiền sang `ON_HOLD`, mở `Dispute`, trừ điểm uy tín gia sư | `ReportRepository`, `DisputeRepository`,<br>`EscrowTransactionRepository` | `reports`, `escrow_transactions` (`ON_HOLD`),<br>`disputes`, `tutors` |
| **22** | **LUỒNG 17: Quản lý Mẫu hợp đồng Master** | `PlatformContractTemplatesPage.tsx` (L1) | `PlatformController.java` (L441)<br>`POST /api/platform/contract-templates` | `PlatformService.java` (L112)<br>`createContractTemplate()` | `PlatformServiceImpl.java` (L2265): gán phiên bản hiệu lực, ghi `audit_logs` | `ContractTemplateRepository.java`<br>`save()` | `contract_templates`, `audit_logs` |
| **23** | **LUỒNG 18: Cấu hình Tỷ lệ phí đối tác Trung tâm** | `PlatformFeeSettingsPage.tsx` (L1) | `PlatformController.java` (L510)<br>`PUT /api/platform/fees/centers/{centerId}` | `PlatformService.java` (L127)<br>`updateCenterFeeConfig()` | `PlatformServiceImpl.java` (L2549): kiểm tra ngưỡng phí, cập nhật và ghi `audit_logs` | `TutorCenterRepository.java`<br>`save()` | `tutor_centers` (`custom_fee_rate`),<br>`audit_logs` |
| **24** | **LUỒNG 19: Quản lý Bản tin & Thông báo toàn sàn (UC-59)** | `PlatformTasksPage.tsx` (L1),<br>`HomePage.tsx` | `AnnouncementController.java` (L45)<br>`POST /api/platform/announcements` | `AnnouncementService.java`<br>`upsertAnnouncement()` | `AnnouncementServiceImpl.java` (L45): lưu bản tin, gọi `notificationDispatchService` gửi thông báo In-App realtime, ghi `audit_logs` | `AnnouncementRepository.java`<br>`save()` | `announcements`, `notifications`,<br>`audit_logs` |
| **25** | **LUỒNG 20: Hợp đồng điện tử Ký số OTP qua Email (UC-44)** | `ContractDetailPage.tsx` (L1) | `ContractController.java` (L176, L193)<br>`POST /api/contract/{contractId}/sign` | `ContractService.java`<br>`signWithOtp()` | `ContractServiceImpl.java` (L476): xác thực OTP `emailOtpRepository`, lưu chữ ký `contractSignatureRepository`, đủ 2 bên ký chuyển `contract.status = SIGNED`, kích hoạt khóa tiền Escrow | `EmailOtpRepository`, `ContractRepository`,<br>`ContractSignatureRepository` | `contracts` (`SIGNED`), `contract_signatures`,<br>`email_otps` |
| **26** | **LUỒNG 21: Đánh giá sao sau buổi học & Điểm uy tín** | `ContractDetailPage.tsx` (L150) | `ContractController.java` (L54)<br>`POST /api/contract/reviews` | `ReviewService.java`,<br>`ContractService.java` | `ReviewServiceImpl.java` (L45 lưu review; `ContractServiceImpl.java` (L2580) `recomputeTutorReputation(tutorId)` tính trung bình cộng sao toàn sàn | `ReviewRepository.java`<br>`TutorRepository.java` | `reviews`, `tutors.rating_avg` |
| **27** | **LUỒNG 22: Quên mật khẩu OTP qua Email (DEF-62)** | `ForgotPasswordPage.tsx`,<br>`ResetPasswordPage.tsx` | `IdentityController.java` (L107-123)<br>`POST /api/identity/password/reset` | `IdentityService.java`<br>`resetPassword()` | `IdentityServiceImpl.java` (L519): kiểm tra TTL `passwordResetTokenRepository`, băm BCrypt mật khẩu mới, đánh dấu token đã dùng, ghi `audit_logs` | `PasswordResetTokenRepository.java`<br>`UserRepository.java` | `users.password_hash`,<br>`password_reset_tokens`, `audit_logs` |
| **28** | **LUỒNG 23: Thay đổi mật khẩu người dùng (Change Password)** | `ProfilePage.tsx`,<br>`ChangePasswordModal` | `IdentityController.java` (L101)<br>`PUT /api/identity/password` | `IdentityService.java`<br>`changePassword()` | `IdentityServiceImpl.java` (L432): kiểm tra BCrypt mật khẩu cũ, băm mật khẩu mới, gọi `userRepository.save()`, ghi `audit_logs` | `UserRepository.java`<br>`save()` | `users.password_hash`, `audit_logs` |
| **29** | **LUỒNG 24: Chat tin nhắn tức thời 1-1 & Nhóm lớp (UC-50)** | `MessagingPage.tsx`,<br>`ChatWindow.tsx` | `ChatController.java` (L58)<br>`/api/messaging/chats/messages` | `ChatService.java`<br>`sendMessage()` | `ChatServiceImpl.java` (L480): gọi `penaltyAccessService` (chặn người bị phạt chat), gọi `circumventionService` (quét lách sàn Regex), lưu tin nhắn | `MessageRepository.java`<br>`ConversationRepository.java` | `conversations`, `messages`,<br>`circumvention_detections` |
| **30** | **LUỒNG 25: Tải ảnh đại diện kiểm tra Magic Bytes (UC-08)** | `TutorProfilePage.tsx`,<br>`ClientProfilePage.tsx` | `ProfileController.java` (L236)<br>`POST /api/profile/me/avatar` | `ProfileService.java`<br>`uploadAvatar()` | `ProfileServiceImpl.java` (L809): đọc byte mảng đầu tiên kiểm tra Magic Bytes (`PNG`, `JPEG`), chặn tệp `.php/.exe` giả mạo đuôi ảnh, cập nhật `avatar_url` | `UserRepository.java`<br>`save()` | `users` (`avatar_url`) |
| **31** | **LUỒNG 26: Đăng xuất & Thu hồi phiên JWT bằng token_version** | Navbar bấm Đăng xuất | `IdentityController.java` (L78)<br>`POST /api/identity/logout` | `IdentityService.java`<br>`logout()` | `IdentityServiceImpl.java` (L390): `user.setTokenVersion(v + 1)`, lưu CSDL, ghi `audit_logs` (Filter: `JwtAuthenticationFilter:45` kiểm tra lệch version trả 401 ngay) | `UserRepository.java`<br>`save()` | `users.token_version`, `audit_logs` |
| **32** | **LUỒNG 27: Ký quỹ Escrow, Ví điện tử & Quyết toán (UC-40)** | `PlatformAnalyticsPage.tsx`,<br>`WalletPage.tsx` | `FinanceController.java` (L50)<br>`/api/finance/wallet` | `FinanceService.java`,<br>`SettlementService.java` | `FinanceServiceImpl.java` (L99), `EscrowServiceImpl.java`, `SettlementServiceImpl.java` (L43): tạo quỹ Escrow `HELD` khi ký hợp đồng, trích 10% phí sàn khi xong lớp, giải ngân 90% ví gia sư `RELEASED` | `WalletRepository.java`<br>`EscrowTransactionRepository` | `wallets`, `wallet_transactions`,<br>`escrow_transactions` |

---

# PHẦN 2: GIẢI PHẪU CHI TIẾT TỪNG LUỒNG CODE THỰC CHIẾN 6 TẦNG

---

## CHƯƠNG 1: PHÂN HỆ HỖ TRỢ KHÁCH HÀNG & QUẢN LÝ TICKET (BF-09)

> [!NOTE]
> Tất cả các tiêu đề luồng dưới đây được đồng bộ 100% khớp với comment banner `// LUỒNG ...` trong mã nguồn backend Java của bạn (`MessagingServiceImpl.java`, `PlatformServiceImpl.java`, `SupportTicketRepository.java`).

---

### LUỒNG 3: NGƯỜI DÙNG TẠO TICKET HỖ TRỢ & TỰ ĐỘNG TÍNH HẠN SLA (UC-65, UC-66)
*(Trong mã nguồn: `MessagingServiceImpl.java:132`, `MessagingController.java:76`, `SupportTicketRepository.java:19`)*

* **Tầng 1 (Frontend UI)**:
  - File: `frontend/src/features/messaging/components/MessagingPanel.tsx` (dòng 120-170).
  - Tương tác: Người dùng chọn danh mục sự cố (`category`), nhập tiêu đề (`subject`), mô tả chi tiết (`description`), đính kèm hình ảnh bằng chứng (`evidenceUrls`), sau đó nhấn nút "Gửi yêu cầu hỗ trợ" kích hoạt hàm `handleCreateTicket()`.
  - Mạng: Gọi Axios Client `POST /api/messaging/support-tickets` gửi kèm Bearer Token trong Header.

* **Tầng 2 (REST Controller)**:
  - File: [`MessagingController.java:122`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/controller/MessagingController.java#L122)
  - Endpoint: `@PostMapping("/support-tickets")`, `@ResponseStatus(HttpStatus.CREATED)`
  - Tiếp nhận DTO: `@Valid @RequestBody CreateSupportTicketRequest request`
  - Nhiệm vụ: Đón nhận request, chuyển giao ngay cho `messagingService.createSupportTicket(request)` và trả về `SupportTicketResponse` với HTTP Status `201 CREATED`.

* **Tầng 3 (Service Interface)**:
  - File: [`MessagingService.java:19`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/service/MessagingService.java#L19)
  - Method Contract:
    ```java
    SupportTicketResponse createSupportTicket(CreateSupportTicketRequest request);
    ```
  - Ý nghĩa: Bản hợp đồng trừu tượng định nghĩa khả năng tiếp nhận yêu cầu hỗ trợ, che giấu toàn bộ logic tính toán SLA và ép sàn độ ưu tiên khỏi Controller, cho phép tiêm lỏng (Loose Coupling) và dễ dàng viết Unit Test giả lập (Mock).

* **Tầng 4 (Service Implementation)**:
  - File: [`MessagingServiceImpl.java:153-194`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/service/impl/MessagingServiceImpl.java#L153-L194)
  - Annotation: `@Service`, `@Transactional`
  - Các bước xử lý tuần tự trong code:
    1. **Bước 1 & 2 (Validate & User Context)**: Kiểm tra `request.getCategory() != null` và tiêu đề không được rỗng. Lấy `userId` an toàn từ Security Context:
       ```java
       User user = userRepository.findById(authHelper.currentUserId())
           .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
       ```
    2. **Luồng 3 - Bước 3 (Thuật toán ép nâng mức ưu tiên sàn - Priority Escalation)** (dòng 168-170, 240-248):
       ```java
       SupportTicketPriority priority = escalatePriority(request.getCategory(), request.getPriority());
       ```
       - `DISPUTE` (Tranh chấp tiền/hợp đồng) ➡️ Ép sàn lên **`URGENT`** (Bắt buộc SLA 4h).
       - `SYSTEM_ERROR` / `REPORT_USER` (Lỗi hệ thống/Tố cáo) ➡️ Ép sàn lên tối thiểu **`HIGH`** (SLA 12h).
       - `BUG_REPORT` (Báo lỗi giao diện/chức năng) ➡️ Ép sàn lên tối thiểu **`MEDIUM`** (SLA 24h).
       - `INQUIRY` / Khác ➡️ Giữ ở **`LOW`** (SLA 48h).
       *(Nếu User chọn mức ưu tiên cao hơn sàn thì giữ nguyên lựa chọn của User, nếu chọn thấp hơn sàn thì hệ thống tự động cưỡng chế nâng lên sàn).*
    3. **Luồng 3 - Bước 4 (Thuật toán tính toán hạn chót cam kết SLA)** (dòng 172-175, 250-261):
       ```java
       ticket.setDueAt(LocalDateTime.now().plusHours(calculateSlaHours(priority)));
       ticket.setSlaBreached(false);
       ```
    4. **Luồng 3 - Bước 5 (Lưu thực thể SupportTicket vào CSDL)** (dòng 183-185):
       ```java
       SupportTicket saved = supportTicketRepository.save(ticket);
       ```
    5. **Luồng 3 - Khởi tạo tin nhắn đầu tiên trong chuỗi hội thoại** (dòng 186-188, 263-272):
       Hàm `createTicketConversation(saved, user)` tạo một bản ghi `TicketMessage` với nội dung từ `ticket.getDescription()`, bằng chứng đính kèm, cờ `isFromAdmin = false` và lưu vào bảng `ticket_messages`.
    6. **Luồng 3 - Bước 6 (Phát thông báo In-App thời gian thực tới toàn bộ Admin)** (dòng 189-191, 275-285):
       Hàm `notifyAdminsNewSupportTicket(saved)` lọc tất cả `PlatformAdmin` có `UserStatus.ACTIVE` và gửi thông báo hệ thống In-App qua `notificationDispatchService`.
    7. **Bước 7**: Chuyển đổi thực thể sang DTO `SupportTicketResponse` trả về Client.

  - *Các API tra cứu & tương tác bổ trợ đi kèm trong Luồng 3*:
    - `getMySupportTickets()`: [`MessagingController.java:89`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/controller/MessagingController.java#L89) (`GET /api/messaging/support-tickets`) ➡️ [`MessagingServiceImpl.java:208`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/service/impl/MessagingServiceImpl.java#L208).
    - `getMySupportTicketDetail()`: [`MessagingController.java:105`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/controller/MessagingController.java#L105) (`GET /api/messaging/support-tickets/{ticketId}`) ➡️ [`MessagingServiceImpl.java:230`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/service/impl/MessagingServiceImpl.java#L230) (Chặn xem chéo: `!ticket.getUser().getUserId().equals(authHelper.currentUserId())` ➡️ quăng `ForbiddenException`).
    - `replySupportTicket()`: [`MessagingController.java:141`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/controller/MessagingController.java#L141) (`POST /api/messaging/support-tickets/{ticketId}/messages`) ➡️ Gửi thêm tin nhắn, nếu ticket đang `IN_REVIEW` thì tự chuyển về `OPEN`.
    - `reopenSupportTicket()`: [`MessagingController.java:162`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/controller/MessagingController.java#L162) (`POST /api/messaging/support-tickets/{ticketId}/reopen`) ➡️ Mở lại ticket đã đóng/đã giải quyết, reset hạn SLA mới.

* **Tầng 5 (Repository Interface)**:
  - [`SupportTicketRepository.java:19`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/repository/SupportTicketRepository.java#L19) kế thừa `JpaRepository<SupportTicket, Long>`:
    - Gọi `supportTicketRepository.save(ticket)` (Sinh câu lệnh `INSERT INTO support_tickets ...`).
    - Gọi `supportTicketRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)` (Sinh câu lệnh `SELECT * FROM support_tickets WHERE user_id = ? ORDER BY created_at DESC`).
  - [`TicketMessageRepository.java:9`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/repository/TicketMessageRepository.java#L9) kế thừa `JpaRepository<TicketMessage, Long>`:
    - Gọi `ticketMessageRepository.save(message)` (Sinh câu lệnh `INSERT INTO ticket_messages ...`).

* **Tầng 6 (Entity & Database Table)**:
  - Bảng `support_tickets`: Thực thể `SupportTicket` (`ticket_id`, `user_id`, `category`, `subject`, `description`, `priority`, `status = 'OPEN'`, `due_at`, `sla_breached = false`, `created_at`).
  - Bảng `ticket_messages`: Thực thể `TicketMessage` (`message_id`, `ticket_id`, `sender_id`, `is_from_admin = false`, `content`, `created_at`).
  - Bảng `notifications`: Chèn bản ghi thông báo In-App cho toàn bộ Admin đang Active.

---

### LUỒNG 4: ADMIN TIẾP NHẬN, XỬ LÝ & ĐO LƯỜNG RESPONSE SLA (UC-66)
*(Trong mã nguồn: `PlatformServiceImpl.java:1526`, `PlatformController.java:328`, `SupportTicketRepository.java:24`)*

#### 4.1. Admin Tiếp Nhận & Thuật Toán Tự Động Gán PIC (Auto-Assignment)
* **Tầng 1 (UI)**: Quản trị viên mở trang `PlatformTicketsPage.tsx` (dòng 63), chọn một ticket đang có trạng thái `OPEN` trong danh sách.
* **Tầng 2 (Controller)**: [`PlatformController.java:335`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L335) nhận tại `@GetMapping("/tickets/{ticketId}")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:89`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L89):
  ```java
  SupportTicketDetailResponse getTicketDetail(Long ticketId);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:1441-1452`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1441-L1452):
  ```java
  SupportTicket ticket = findTicketOrThrow(ticketId);
  // Mở ticket lần đầu: tự động gán admin hiện tại và chuyển OPEN -> IN_PROGRESS
  if (ticket.getStatus() == SupportTicketStatus.OPEN) {
      PlatformAdmin admin = currentAdminOrThrow();
      ticket.setAssignedAdmin(admin);
      ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
      ticket = supportTicketRepository.save(ticket);
  }
  return toTicketDetail(ticket);
  ```
  *(Giải pháp này đảm bảo không bao giờ có tình trạng 2 Admin cùng xử lý chồng chéo 1 ticket).*
* **Tầng 5 (Repository Interface)**: `SupportTicketRepository.java`: gọi `findById(ticketId)` và `save(ticket)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `support_tickets` cập nhật `assigned_admin_id = adminId` và `status = 'IN_PROGRESS'`.

#### 4.2. Admin Phản Hồi Ticket & Đo Lường First Response SLA
* **Tầng 1 (UI)**: Admin soạn thảo nội dung giải quyết tại `PlatformTicketsPage.tsx` (dòng 94) và nhấn "Gửi phản hồi".
* **Tầng 2 (Controller)**: [`PlatformController.java:362`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L362) nhận tại `@PostMapping("/tickets/{ticketId}/messages")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:95`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L95):
  ```java
  SupportTicketDetailResponse respondToTicket(Long ticketId, RespondTicketRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:1546-1585`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1546-L1585):
  1. **Luồng 4 - Bước 3**: Nếu ticket chưa có PIC, tự động gán Admin hiện tại.
  2. **Luồng 4 - Bước 4**: Tạo và lưu `TicketMessage` với `isFromAdmin = true`, người gửi là `admin.getUser()`.
  3. **Luồng 4 - Bước 5**: Chuyển trạng thái ticket sang `IN_REVIEW` (chờ User phản hồi/xác nhận).
  4. **Thuật toán đo lường First Response SLA** (dòng 1572-1574):
     ```java
     LocalDateTime now = LocalDateTime.now();
     if (ticket.getResponseSlaMs() == null && ticket.getCreatedAt() != null) {
         ticket.setResponseSlaMs(java.time.Duration.between(ticket.getCreatedAt(), now).toMillis());
     }
     ```
     *(Ghi nhận chính xác số mili-giây từ lúc User tạo ticket tới khoảnh khắc Admin đầu tiên đưa ra phản hồi chính thức).*
  5. Đánh dấu `ticket.setSlaBreached(true)` nếu `now.isAfter(ticket.getDueAt())`.
  6. **Luồng 4 - Bước 6**: Ghi Audit Log `RESPOND_TICKET` và phát thông báo In-App tới User tạo ticket.
* **Tầng 5 (Repository Interface)**:
  - `TicketMessageRepository.java` ➡️ `save(message)`.
  - `SupportTicketRepository.java` ➡️ `save(ticket)`.
  - `AuditLogRepository.java` ➡️ `save(auditLog)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `ticket_messages` (thêm bản ghi mới), bảng `support_tickets` (`response_sla_ms`, `status = 'IN_REVIEW'`), bảng `audit_logs`.

#### 4.3. Admin Đóng / Giải Quyết Ticket
* **Tầng 1 (UI)**: Admin bấm "Giải quyết" (`RESOLVED`) hoặc "Đóng ticket" (`CLOSED`) trên `PlatformTicketsPage.tsx` (dòng 99).
* **Tầng 2 (Controller)**: [`PlatformController.java:374`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L374) nhận tại `@PatchMapping("/tickets/{ticketId}/status")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:98`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L98):
  ```java
  SupportTicketDetailResponse closeTicket(Long ticketId, CloseTicketRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:1603-1631`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1603-L1631):
  - Kiểm tra tính hợp lệ: Chỉ chấp nhận trạng thái `RESOLVED` hoặc `CLOSED`.
  - Ghi nhận thời gian: Nếu `RESOLVED` gán `resolvedAt = now`. Nếu `CLOSED` gán `closedAt = now` (kèm fallback `resolvedAt = now` nếu chưa có).
  - Ghi vết kiểm toán `CLOSE_TICKET` vào bảng `audit_logs`.
  - Phát thông báo kèm lời nhắn hoàn tất xử lý tới người dùng.
* **Tầng 5 (Repository Interface)**: `SupportTicketRepository.java` ➡️ `save(ticket)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `support_tickets` (`status`, `resolved_at`, `closed_at`), bảng `audit_logs`, bảng `notifications`.

---

### LUỒNG 5: GỘP TICKET TRÙNG LẶP & CHUYỂN SANG LUỒNG KHIẾU NẠI (UC-66, BF-08)
*(Trong mã nguồn: `PlatformServiceImpl.java:1634`, `PlatformController.java:389 & 404`)*

#### Luồng 5A: Gộp Ticket Trùng Lặp Của Cùng Một Người Dùng (Merge Ticket)
* **Tầng 1 (UI)**: `PlatformTicketsPage.tsx` (dòng 103), Admin mở modal gộp ticket, nhập `targetTicketId` (ticket chính) và `reason` (lý do gộp).
* **Tầng 2 (Controller)**: [`PlatformController.java:389`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L389) nhận tại `@PostMapping("/tickets/{ticketId}/merge")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:101`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L101):
  ```java
  SupportTicketDetailResponse mergeTicket(Long sourceTicketId, MergeTicketRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:1653-1718`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1653-L1718):
  - **4 Chốt chặn an toàn (Safety Guards)**:
    1. Không tự gộp vào chính nó (`sourceTicketId.equals(targetTicketId)` ➡️ IllegalArgumentException).
    2. Cả 2 ticket phải tồn tại trong CSDL.
    3. **Bắt buộc cùng một chủ sở hữu**: `!sourceTicket.getUser().getUserId().equals(targetTicket.getUser().getUserId())` ➡️ ném ngoại lệ chặn gian lận xem trộm dữ liệu người khác!
    4. Không thể gộp ticket đã đóng (`RESOLVED` hoặc `CLOSED`).
  - Tạo một tin nhắn hệ thống trong ticket đích:
    ```
    [HỆ THỐNG - GỘP TICKET] Đã gộp nội dung từ Ticket #{sourceId} ({sourceSubject}).
    Lý do gộp: {reason}.
    Nội dung ban đầu: {sourceDescription}
    ```
  - Đóng ticket nguồn: `sourceTicket.setStatus(SupportTicketStatus.CLOSED)`, gán `closedAt = LocalDateTime.now()`.
  - Ghi Audit Log `MERGE_TICKET` và gửi thông báo xác nhận cho khách hàng.
* **Tầng 5 (Repository Interface)**:
  - `TicketMessageRepository.java` ➡️ `save(mergeNotice)` vào ticket đích.
  - `SupportTicketRepository.java` ➡️ `save(sourceTicket)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `ticket_messages` (thêm tin nhắn hệ thống), bảng `support_tickets` (ticket nguồn cập nhật `status='CLOSED'`), bảng `audit_logs`.

#### Luồng 5B: Chuyển Ticket Sang Luồng Xử Lý Tranh Chấp & Báo Cáo Sự Cố (BF-08)
* **Tầng 1 (UI)**: `PlatformTicketsPage.tsx` (dòng 112), Admin phát hiện yêu cầu hỗ trợ liên quan đến quỵt tiền học phí, hủy lớp vô cớ hoặc vi phạm hợp đồng, nhấn "Chuyển tranh chấp".
* **Tầng 2 (Controller)**: [`PlatformController.java:404`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L404) nhận tại `@PostMapping("/tickets/{ticketId}/redirect-dispute")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:107`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L107):
  ```java
  SupportTicketDetailResponse redirectTicketToDispute(Long ticketId, RedirectDisputeRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:1841-1896`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1841-L1896):
  1. Kiểm tra ticket phải gắn với một lớp học cụ thể (`targetClass != null`).
  2. Đổi danh mục ticket sang `SupportTicketCategory.DISPUTE`.
  3. Ép mức ưu tiên lên tối thiểu **`HIGH`** (hoặc giữ `URGENT` nếu đã có).
  4. Tạo tin nhắn ghi nhận chuyển luồng trong hội thoại ticket.
  5. **Tự động sinh bản ghi Báo cáo sự cố (`Report`)** kết nối với Phân hệ Tranh chấp Escrow (BF-08 / BF-10):
     ```java
     Report report = new Report();
     report.setReporter(ticket.getUser());
     report.setTargetType(ReportTargetType.CLASS);
     report.setTargetId(tutoringClass.getClassId());
     report.setReason(request.getReason());
     report.setStatus(ReportStatus.PENDING);
     reportRepository.save(report);
     ```
     *(Bản ghi `Report` này lập tức kích hoạt quy trình xem xét tạm khóa tiền ký quỹ Escrow của lớp học để bảo vệ quyền lợi tài chính cho các bên).*
* **Tầng 5 (Repository Interface)**: `ReportRepository.java` ➡️ `save(report)`, `SupportTicketRepository.java` ➡️ `save(ticket)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `reports` (`target_type='CLASS'`, `status='PENDING'`), bảng `support_tickets` (`category='DISPUTE'`), bảng `audit_logs`.

---

### LUỒNG 7: QUÉT ĐỊNH KỲ & NÂNG CẤP KHẨN CẤP TICKET QUÁ HẠN SLA (JOB-11)
*(Trong mã nguồn: `PlatformServiceImpl.java:1721`, `PlatformController.java:417`, `SupportTicketRepository.java:48`)*

* **Tầng 1 (Trigger)**:
  - Tự động: Spring `@Scheduled(cron = "0 */10 * * * *")` kích hoạt định kỳ mỗi 10 phút một lần.
  - Thủ công: Quản trị viên bấm nút "Quét SLA ngay" trên UI `PlatformTicketsPage.tsx` hoặc qua script bảo trì gọi API.
* **Tầng 2 (REST Controller)**:
  - File: [`PlatformController.java:417`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L417)
  - Endpoint: `@PostMapping("/tickets/sla/scan")`
  - Trả về: `Map.of("message", "Quét SLA hoàn tất", "escalatedCount", count)`
* **Tầng 3 (Service Interface)**:
  - File: [`PlatformService.java:104`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L104)
  - Method Contract:
    ```java
    int scanAndEscalateSlaBreaches();
    ```
* **Tầng 4 (Service Implementation)**:
  - File: [`PlatformServiceImpl.java:1738-1822`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1738-L1822)
  - Annotation: `@Transactional`
  - Các bước xử lý trong thuật toán:
    1. Lấy thời điểm hiện tại `LocalDateTime now = LocalDateTime.now()`.
    2. Gọi `supportTicketRepository.findBreachedCandidateTickets(excludedStatuses, now)` với `excludedStatuses = [RESOLVED, CLOSED]`.
    3. Lặp qua từng ticket vi phạm SLA:
       - **Nâng cấp độ ưu tiên lũy tiến từng bậc**:
         $$\text{LOW} \longrightarrow \text{MEDIUM} \longrightarrow \text{HIGH} \longrightarrow \text{URGENT}$$
       - Đánh dấu vi phạm: `ticket.setSlaBreached(true)`.
       - Lưu thực thể cập nhật: `supportTicketRepository.save(ticket)`.
       - Ghi vết kiểm toán tự động: `auditLogService.record("SLA_BREACH_ESCALATION", "SupportTicket", ticketId, ...)`.
       - **Bắn thông báo kép (Dual Notifications)**:
         * *Thông báo Admin*: Phát cảnh báo khẩn cấp tới Quản trị viên phụ trách (PIC) hoặc toàn bộ Admin để can thiệp ngay lập tức.
         * *Thông báo User*: Gửi thông báo xin lỗi và cam kết đang đẩy nhanh tiến độ xử lý tới người dùng đã tạo ticket.
    4. Trả về tổng số lượng ticket đã được nâng cấp trong đợt quét.
* **Tầng 5 (Repository Interface)**:
  - File: [`SupportTicketRepository.java:48-60`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/repository/SupportTicketRepository.java#L48-L60)
  - Query JPQL tối ưu:
    ```java
    @Query("""
            SELECT t FROM SupportTicket t
            WHERE t.status NOT IN (:excludedStatuses)
            AND t.dueAt IS NOT NULL
            AND t.dueAt < :now
            AND (t.slaBreached IS NULL OR t.slaBreached = false)
            """)
    List<SupportTicket> findBreachedCandidateTickets(
            @Param("excludedStatuses") List<SupportTicketStatus> excludedStatuses,
            @Param("now") java.time.LocalDateTime now);
    ```
  - *Ý nghĩa câu lệnh SQL thực tế*: `SELECT * FROM support_tickets WHERE status NOT IN ('RESOLVED', 'CLOSED') AND due_at IS NOT NULL AND due_at < NOW() AND (sla_breached IS NULL OR sla_breached = 0)`.
* **Tầng 6 (CSDL & Entity)**:
  - Bảng `support_tickets`: Cập nhật `priority = newPriority`, `sla_breached = true`.
  - Bảng `audit_logs`: Chèn bản ghi nhật ký hành động tự động của hệ thống `SLA_BREACH_ESCALATION`.
  - Bảng `notifications`: Thêm 2 bản ghi thông báo In-App (1 cho Admin, 1 cho User).

---

## CHƯƠNG 2: PHÂN HỆ ĐIỀU HÀNH SÀN & BẢO MẬT (BF-10)

> [!NOTE]
> Các luồng điều hành sàn dưới đây được đồng bộ 100% với comment banner `// LUỒNG ...` trong mã nguồn backend Java (`PlatformServiceImpl.java`, `PlatformTaskQueueServiceImpl.java`, `CatalogServiceImpl.java`, `AuditLogServiceImpl.java`, `PlatformAnalyticsServiceImpl.java`, `CircumventionServiceImpl.java`, `PenaltyServiceImpl.java`).

---

### LUỒNG 8: BẢNG ĐIỀU KHIỂN QUẢN TRỊ & HÀNG ĐỢI NHIỆM VỤ TRỰC BAN KHẨN CẤP (UC-56, UC-64)
*(Trong mã nguồn: `PlatformServiceImpl.java:451`, `PlatformTaskQueueServiceImpl.java:65`, `PlatformController.java:158`)*

* **Tầng 1 (Frontend UI)**:
  - File: `frontend/src/features/platform/pages/PlatformDashboardPage.tsx`
  - Hiển thị 5 khối trực quan: (1) Cảnh báo rủi ro vận hành (Risk Summary), (2) Dòng tiền & Tỷ lệ phí sàn (Financial Flow), (3) Sức khỏe Gia sư & Trung tâm, (4) Thống kê phân bố lớp học, (5) Hàng đợi trực ban khẩn cấp (Emergency Task Queue).
* **Tầng 2 (REST Controller)**:
  - File: [`PlatformController.java:158`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L158)
  - Endpoint: `@GetMapping("/dashboard")` nhận `from`, `to`, `granularity`.
* **Tầng 3 (Service Interface)**:
  - File: [`PlatformService.java:56`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L56):
    ```java
    DashboardResponse getDashboard(LocalDate from, LocalDate to, String granularity);
    ```
* **Tầng 4 (Service Implementation)**:
  - File: [`PlatformServiceImpl.java:451-510`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L451) phối hợp [`PlatformTaskQueueServiceImpl.java:65-130`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformTaskQueueServiceImpl.java#L65):
    - **Phân vùng 1 & 5 (Task Queue)**: Đếm số lượng ticket quá hạn SLA (`dueAt < now`), báo cáo sự cố chờ duyệt (`ReportStatus.PENDING`), yêu cầu rút tiền đang chờ xử lý, số tiền đang gặp rủi ro trong Escrow.
    - **Phân vùng 2 (Dòng tiền tài chính)**: Tổng doanh thu GMV, phí sàn thu về, số dư ký quỹ Escrow bảo đảm.
    - **Phân vùng 3 (Sức khỏe người dùng)**: Tỷ lệ gia sư đã eKYC CCCD, số lượng trung tâm hoạt động, lớp học đang diễn ra.
* **Tầng 5 (Repository Interface)**: `TutorRepository`, `TutorCenterRepository`, `TutoringClassRepository`, `EscrowTransactionRepository`, `SupportTicketRepository`, `ReportRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `users`, `tutors`, `tutor_centers`, `tutoring_classes`, `escrow_transactions`, `support_tickets`, `reports`.

---

### LUỒNG 9: QUẢN LÝ CÂY DANH MỤC HỆ THỐNG PHÂN CẤP ĐỆ QUY (UC-57)
*(Trong mã nguồn: `CatalogServiceImpl.java:422`, `CatalogController.java`)*

* **Tầng 1 (UI)**: Màn hình quản trị danh mục `PlatformCategoriesPage.tsx`, cho phép xem cây danh mục phân cấp đa tầng (Môn học -> Khối lớp -> Trình độ).
* **Tầng 2 (Controller)**: `CatalogController.java` với các endpoint GET cây danh mục đệ quy, POST tạo danh mục, PUT cập nhật, DELETE xóa.
* **Tầng 3 (Service Interface)**: `CatalogService.java`: `List<CategoryTreeResponse> getCategoryTree()`, `createCategory()`, `updateCategory()`, `deleteCategory()`.
* **Tầng 4 (Service Impl)**: [`CatalogServiceImpl.java:422-510`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/catalog/service/impl/CatalogServiceImpl.java#L422):
  - **Thuật toán dựng cây đệ quy O(N)**: Gom nhóm tất cả category theo `parentId` bằng `LinkedHashMap`, sau đó duyệt đệ quy gắn `children` vào node cha mà không gây N+1 queries.
  - **Chống tạo chu trình đệ quy vô hạn (Anti-circular dependency)**: Khi cập nhật `parentId`, kiểm tra không cho phép chọn chính nó hoặc node con cháu làm node cha.
  - **2 Lớp bảo vệ khi xóa**: Kiểm tra không còn category con (chống node mồ côi) và không còn lớp học nào đang trỏ vào category đó.
* **Tầng 5 (Repository Interface)**: `CategoryRepository.java` (`findAllByOrderByDisplayOrderAsc()`, `findByParentId()`).
* **Tầng 6 (CSDL & Entity)**: Bảng `categories` (`category_id`, `parent_id`, `name`, `code`, `display_order`).

---

### LUỒNG 10: THAM SỐ CẤU HÌNH HỆ THỐNG TOÀN CỤC & TỶ LỆ PHÍ SÀN (UC-58)
*(Trong mã nguồn: `CatalogServiceImpl.java:520+`, `PlatformFeeSettingsPage.tsx`)*

* **Tầng 1 (UI)**: `PlatformFeeSettingsPage.tsx` — Quản trị viên cấu hình phí sàn toàn hệ thống (mặc định 10%), phí đối tác trung tâm (tùy chỉnh 8-12%).
* **Tầng 2 (Controller)**: `CatalogController.java` (`/api/catalog/admin/system-parameters`) & `PlatformController.java:420` (`PUT /api/platform/centers/{id}/fee`).
* **Tầng 3 (Service Interface)**: `CatalogService.java` & `PlatformService.java:127` (`updateCenterFeeConfig`).
* **Tầng 4 (Service Impl)**:
  - Khóa bảo vệ tham số cốt lõi (`MANDATORY_KEYS`): Chặn tuyệt đối không cho phép xóa các khóa bắt buộc của hệ thống như `PLATFORM_FEE_RATE`, `MAX_DISPUTE_HOURS`.
  - **Kiểm tra giới hạn biên số học (Boundary Check)**: Tỷ lệ phí sàn chỉ được nằm trong khoảng hợp lệ `0.00` đến `0.30` (tối đa 30%).
  - Ghi Audit Log toàn bộ sự kiện thay đổi biểu phí.
* **Tầng 5 (Repository Interface)**: `SystemParameterRepository.java`, `TutorCenterRepository.java`.
* **Tầng 6 (CSDL & Entity)**: Bảng `system_parameters`, bảng `tutor_centers` (`custom_fee_rate`), bảng `audit_logs`.

---

### LUỒNG 11: GIÁM SÁT NHẬT KÝ KIỂM TOÁN & SO VẾT THAY ĐỔI JSON DIFF (UC-61)
*(Trong mã nguồn: `AuditLogServiceImpl.java:71`, `AuditLogController.java:30`, `AuditLogRepository.java`)*

* **Tầng 1 (UI)**: [PlatformAuditLogsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformAuditLogsPage.tsx) — Bộ lọc tìm kiếm theo tác nhân, thực thể, hành động, dải ngày; hiển thị modal so sánh Diff JSON đỏ/xanh trực quan.
* **Tầng 2 (Controller)**: `AuditLogController.java:30` nhận tại `@GetMapping("/audit-logs")`.
* **Tầng 3 (Service Interface)**: `AuditLogService.java`:
  ```java
  void record(String action, String entityType, Long entityId, Object oldValue, Object newValue);
  Page<AuditLogResponse> getAuditLogs(...);
  ```
* **Tầng 4 (Service Impl)**: [`AuditLogServiceImpl.java:71-185`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/AuditLogServiceImpl.java#L71):
  - **Luồng 11 - Bước 1**: Tự động trích xuất tác nhân từ `SecurityContextHolder` (`adminUser`).
  - **Luồng 11 - Bước 2**: Trích xuất Client IP (xử lý proxy `X-Forwarded-For`) và Header `User-Agent` từ `RequestContextHolder`.
  - Tuần tự hóa `oldValue` và `newValue` thành chuỗi JSON Diff qua Jackson `ObjectMapper`.
  - **Luồng 11 - Bước 3**: Tra cứu và phân trang đa tiêu chí kết hợp Specification.
  - **Tính bất biến (Append-Only Immutability)**: Bảng `audit_logs` được thiết kế chỉ có hành vi INSERT và SELECT, tuyệt đối không cung cấp hàm UPDATE hay DELETE để bảo đảm toàn vẹn pháp lý khi thanh tra!
* **Tầng 5 (Repository Interface)**: `AuditLogRepository.java` kế thừa `JpaRepository<AuditLog, Long>`.
* **Tầng 6 (CSDL & Entity)**: Bảng `audit_logs` (`log_id`, `admin_id`, `action`, `entity_type`, `entity_id`, `old_value`, `new_value`, `ip_address`, `user_agent`, `created_at`).

---

### LUỒNG 12: BÁO CÁO TÀI CHÍNH ĐA CHIỀU & XUẤT DỮ LIỆU CSV AN TOÀN (UC-41, UC-43)
*(Trong mã nguồn: `PlatformAnalyticsServiceImpl.java:374`, `PlatformAnalyticsController.java:80`)*

* **Tầng 1 (UI)**: [PlatformAnalyticsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformAnalyticsPage.tsx) — Bấm nút "Xuất file CSV" để tải báo cáo doanh thu/giao dịch.
* **Tầng 2 (Controller)**: `PlatformAnalyticsController.java:80` nhận tại `@GetMapping("/analytics/export-csv")`, trả về `ResponseEntity<byte[]>` với Header `Content-Disposition: attachment; filename="report.csv"`.
* **Tầng 3 (Service Interface)**: `PlatformAnalyticsService.java`: `byte[] exportCsv(String type, LocalDate from, LocalDate to);`.
* **Tầng 4 (Service Impl)**: [`PlatformAnalyticsServiceImpl.java:374-450 & 980`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformAnalyticsServiceImpl.java#L374):
  - **Luồng 12 - Bước 1 (Chèn UTF-8 BOM Header)**: Bắt đầu mảng byte bằng `\uFEFF` (`0xEF, 0xBB, 0xBF`) giúp Microsoft Excel tự động nhận diện bảng mã UTF-8 và hiển thị tiếng Việt có dấu chuẩn xác 100%, không bị lỗi font ô vuông.
  - **Luồng 12 - Bước 2 (Chống tràn RAM máy chủ - OOM Protection)**: Giới hạn mặc định tối đa 90 ngày và trần `MAX_EXPORT_ROWS = 10_000` dòng dữ liệu.
  - **Kỹ thuật chống tấn công công thức DDE (Formula / CSV Injection)**: Tại hàm `escapeCsv()` (dòng 983), nếu ký tự đầu tiên của ô là một trong các ký tự điều khiển `=`, `+`, `-`, `@`, hệ thống tự động chèn thêm dấu nháy đơn `'` ở đầu chuỗi để ngăn Excel thực thi lệnh mã độc hại trên máy người dùng.
* **Tầng 5 (Repository Interface)**: `PaymentTransactionRepository`, `EscrowTransactionRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `payment_transactions`, `escrow_transactions`.

---

### LUỒNG 13: PHÁT HIỆN HÀNH VI LÁCH NỀN TẢNG (CIRCUMVENTION - UC-59) & BAN HÀNH QUYẾT ĐỊNH XỬ PHẠT (UC-60)
*(Trong mã nguồn: `CircumventionServiceImpl.java:59`, `PenaltyServiceImpl.java:101`, `PenaltyAccessServiceImpl.java:45`)*

* **Tầng 1 (UI)**:
  - [PlatformCircumventionPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformCircumventionPage.tsx): Xem danh sách tin nhắn bị nghi ngờ lách sàn.
  - [PlatformPenaltiesPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformPenaltiesPage.tsx): Admin lập biên bản xử phạt người dùng vi phạm.
* **Tầng 2 (Controller)**:
  - `CircumventionController.java:30` (`GET /api/platform/circumventions`)
  - `PlatformPenaltyController.java:45` (`POST /api/platform/penalties`)
* **Tầng 3 (Service Interface)**:
  - `CircumventionService.java`: `void inspect(Message message);`
  - `PenaltyService.java`: `PenaltyResponse createPenalty(CreatePenaltyRequest request);`
  - `PenaltyAccessService.java:25`: `void requireFeature(Long userId, String featureCode);`
* **Tầng 4 (Service Impl)**:
  - **Luồng 13 - Bước 1 & 2 (Regex 4 tầng phát hiện lách sàn)** ([`CircumventionServiceImpl.java:59-95`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/CircumventionServiceImpl.java#L59)):
    1. `PHONE` (Điểm rủi ro 80): `(?<!\d)(?:\+?84|0)(?:[ .-]?\d){9,10}(?!\d)`
    2. `EMAIL` (Điểm rủi ro 90): `[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}`
    3. `URL` (Điểm rủi ro 70): `(?i)(?:https?://|www\.)\S+`
    4. `SOCIAL` (Điểm rủi ro 65): `(?i)(?:zalo|telegram|facebook|fb|instagram)\s*[:@-]?\s*[A-Z0-9_.-]{3,}`
    Nếu tổng điểm `risk_score >= 65`, tự động lưu vết vi phạm vào `circumvention_detections`.
  - **Luồng 13 - Bước 4 (Admin ban hành quyết định xử phạt vi phạm)** ([`PenaltyServiceImpl.java:101-180`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PenaltyServiceImpl.java#L101)):
    Tạo bản ghi `UserPenalty` với hình thức cảnh cáo (`WARNING`), cấm tạm thời (`TEMPORARY_BAN` kèm `expiresAt`), hoặc cấm vĩnh viễn (`PERMANENT_BAN`).
  - **Tác vụ nền chạy ngầm định kỳ 5 phút/lần** (dòng 112): Tự động tìm và mở khóa các lệnh phạt đã hết hạn (`expiresAt <= now`).
  - **Cơ chế gác cổng liên module (Cross-Module Gatekeeper)** ([`PenaltyAccessServiceImpl.java:45-53`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PenaltyAccessServiceImpl.java#L45)):
    Tại các điểm nhạy cảm, gọi `penaltyAccessService.requireFeature(userId, code)`. Nếu đang bị phạt, lập tức quăng `ForbiddenException`:
    - `FinanceServiceImpl.java:178`: `requireFeature(userId, "WITHDRAWAL")` (Chặn rút tiền).
    - `MarketplaceServiceImpl.java:369`: `requireFeature(userId, "CLASS_POSTING")` (Chặn đăng lớp).
    - `MarketplaceServiceImpl.java:514`: `requireFeature(tutorId, "CLASS_APPLICATION")` (Chặn nộp hồ sơ gia sư).
    - `ChatServiceImpl.java:394`: `requireFeature(userId, "MESSAGING")` (Chặn gửi tin nhắn).
* **Tầng 5 (Repository Interface)**: `CircumventionEventRepository`, `UserPenaltyRepository`, `MessageRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `circumvention_detections`, bảng `user_penalties`, bảng `audit_logs`.

---

### CÁC NGHIỆP VỤ QUẢN TRỊ TRỌNG YẾU KHÁC TRONG BF-10:

#### 1. Quản Trị User & Vô Hiệu Hóa Token Tức Thì (JWT Invalidation)
* **Tầng 1 (UI)**: [PlatformUsersPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformUsersPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:135`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L135) nhận tại `@PatchMapping("/users/{id}/status")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:49`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L49): `updateUserStatus()`.
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:300-323`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L300):
  - Chống khóa chéo admin: Không cho phép đổi trạng thái tài khoản `PLATFORM_ADMIN`.
  - Cập nhật `user.setStatus(BANNED)`.
  - **Cơ chế chặn tức thì 2 lớp**:
    1. *Lớp 1*: Tại [`UserPrincipal.java:27`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/security/UserPrincipal.java#L27), `enabled = user.getStatus() == UserStatus.ACTIVE`.
    2. *Lớp 2*: Trong [`JwtAuthenticationFilter.java:43`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/security/JwtAuthenticationFilter.java#L43), kiểm tra:
       ```java
       if (!userDetails.isEnabled() || principal.getTokenVersion() != jwtService.extractTokenVersion(claims)) {
           filterChain.doFilter(request, response);
           return;
       }
       ```
       👉 *Request tiếp theo gửi kèm JWT cũ sẽ bị chặn ngay lập tức, trả về 401 Unauthorized!*
* **Tầng 5 (Repository Interface)**: `UserRepository.java` ➡️ `save(user)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `users` (`status = 'BANNED'`), bảng `audit_logs`.

#### 2. Quy Trình Thẩm Định eKYC CCCD / Bằng Cấp (UC-11)
* **Tầng 1 (UI)**: [PlatformVerificationsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformVerificationsPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:199`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L199) nhận tại `@PostMapping("/verifications/{id}/review")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:62`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L62): `reviewVerification()`.
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:542-613`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L542):
  - *Chống sửa đồng thời*: So khớp `expectedUpdatedAt` với `verification.getUpdatedAt()`.
  - *Ép lý do từ chối 10 ký tự*: Nếu `REJECTED`, `adminNotes.length() >= 10`.
  - *Vô hiệu hóa bản ghi cũ*: `deactivatePreviousVerifiedRequests()` chuyển các CCCD cũ của người này sang hết hiệu lực.
  - *Đồng bộ trạng thái*: Cập nhật `tutor.setVerificationStatus(VERIFIED)` hoặc `tutorCenter.setVerificationStatus(VERIFIED)`.
* **Tầng 5 (Repository Interface)**: `VerificationRequestRepository`, `TutorRepository`, `TutorCenterRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `verification_requests`, `verification_histories`, `tutors`, `tutor_centers`.

#### 3. Can Thiệp Sự Cố Lớp & Tự Động Khóa Escrow (UC-49 / BF-10)
* **Tầng 1 (UI)**: [PlatformReportsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformReportsPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:271`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L271) nhận tại `@PatchMapping("/reports/{reportId}/resolve")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:66`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L66): `resolveClassIssue()`.
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:829 & 1726`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1726) (`resolveClassIssueReport`):
  - Áp dụng 7 hành động trong enum `ClassIssueResolutionAction`.
  - **Tự động khóa Escrow**: Nếu chọn `ESCALATE_TO_DISPUTE` hoặc `TERMINATE_CLASS`:
    ```java
    EscrowTransaction escrow = resolveSingleEscrowForClassIssue(report);
    EscrowTransaction heldEscrow = escrowService.holdForDispute(escrow.getEscrowId(), ...);
    Dispute dispute = new Dispute();
    dispute.setReport(report);
    dispute.setEscrowTransaction(heldEscrow);
    dispute.setStatus(DisputeStatus.OPEN);
    disputeRepository.save(dispute);
    ```
* **Tầng 5 (Repository Interface)**: `ReportRepository`, `EscrowTransactionRepository`, `DisputeRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `reports`, `escrow_transactions` (`status='ON_HOLD'`), `disputes` (`status='OPEN'`).

#### 4. Quản Lý Mẫu Hợp Đồng Master (UC-45 - LUỒNG 17)
* **Tầng 1 (UI)**: [PlatformContractTemplatesPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformContractTemplatesPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:441`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L441) nhận tại `@PostMapping("/contract-templates")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:112`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L112): `createContractTemplate()`.
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:2265`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L2265):
  - Thiết lập phiên bản hợp đồng hiệu lực (`template.setEffectiveDate(now)`).
  - Xác thực mẫu hợp đồng khung theo tiêu chuẩn pháp lý TCS.
  - Gọi `contractTemplateRepository.save(template)` và ghi `auditLogService.record("CREATE_CONTRACT_TEMPLATE", ...)`.
* **Tầng 5 (Repository Interface)**: `ContractTemplateRepository.java` ➡️ `save()`.
* **Tầng 6 (CSDL & Entity)**: Bảng `contract_templates`, bảng `audit_logs`.

#### 5. Cấu Hình Tỷ Lệ Phí Đối Tác Trung Tâm (UC-46 - LUỒNG 18)
* **Tầng 1 (UI)**: [PlatformFeeSettingsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformFeeSettingsPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:510`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L510) nhận tại `@PutMapping("/fees/centers/{centerId}")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:127`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L127): `updateCenterFeeConfig()`.
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:2549`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L2549):
  - Kiểm tra ngưỡng tỷ lệ chiết khấu hợp lệ: `customFeeRate` nằm trong khoảng $0.00$ đến $0.30$ ($0\% - 30\%$).
  - Cập nhật tỷ lệ hoa hồng riêng cho đối tác trung tâm: `center.setCustomFeeRate(request.getCustomFeeRate())`.
  - Gọi `tutorCenterRepository.save(center)` và ghi nhật ký kiểm toán `auditLogService.record("UPDATE_CENTER_FEE", ...)`.
* **Tầng 5 (Repository Interface)**: `TutorCenterRepository.java` ➡️ `save()`.
* **Tầng 6 (CSDL & Entity)**: Bảng `tutor_centers` (cột `custom_fee_rate`), bảng `audit_logs`.

---

## CHƯƠNG 3: TRỢ LÝ AI & TRA CỨU TRI THỨC HỖ TRỢ (BF-09)

> [!NOTE]
> Các luồng tra cứu FAQ và Trợ lý AI dưới đây được đồng bộ 100% với comment banner `// LUỒNG ...` trong `CatalogServiceImpl.java:200 & 316` và `AiServiceImpl.java:114`.

---

### LUỒNG 1: TRA CỨU DANH MỤC FAQ CÔNG KHAI
*(Trong mã nguồn: `CatalogServiceImpl.java:200`, `CatalogController.java:62, L72`, `FaqEntryRepository.java`)*

* **Tầng 1 (Frontend UI)**: Trang trung tâm trợ giúp `HelpPage.tsx` — Người dùng gõ từ khóa tìm kiếm hoặc lọc theo danh mục câu hỏi thường gặp.
* **Tầng 2 (REST Controller)**: `CatalogController.java:72` (`GET /api/catalog/faq`).
* **Tầng 3 (Service Interface)**: `CatalogService.java`: `List<FaqResponse> getFaqEntries(String category, String keyword);`.
* **Tầng 4 (Service Implementation)**: [`CatalogServiceImpl.java:79-105 & 200`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/catalog/service/impl/CatalogServiceImpl.java#L200):
  - **Luồng 1 - Bước 5.1 (Chuẩn hóa tiếng Việt)**: Chuyển về chữ thường, thay 'đ' thành 'd', tách dấu thanh Unicode qua `Normalizer.normalize(Form.NFD)` và loại bỏ diacritics.
  - **Luồng 1 - Bước 5.2 (Tokenization)**: Tách chuỗi từ khóa thành tập token đơn, loại bỏ số và stop words vô nghĩa.
  - **Luồng 1 - Bước 5.3 (Thuật toán chấm điểm liên quan)**:
    - Khớp trong Question: Hệ số $\times 2$.
    - Khớp trong Answer: Hệ số $\times 1$.
    - Ngưỡng tối thiểu khớp Question $\ge \min(2, \text{tokens.size()})$.
  - **Luồng 1 - Bước 6**: Ánh xạ `FaqEntry` sang DTO `FaqResponse`.
* **Tầng 5 (Repository Interface)**: `FaqEntryRepository.java` (`findByPublishedTrueOrderByDisplayOrderAsc()`).
* **Tầng 6 (CSDL & Entity)**: Bảng `faq_entries` (`faq_id`, `category`, `question`, `answer`, `published = true`, `display_order`).

---

### LUỒNG 6: QUẢN TRỊ TRI THỨC FAQ - ADMIN CRUD & PHÊ DUYỆT BẢN NHÁP (UC-67)
*(Trong mã nguồn: `CatalogServiceImpl.java:316`, `CatalogController.java:91, L100-135`, `FaqEntryRepository.java`)*

* **Tầng 1 (UI)**: [PlatformFaqPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformFaqPage.tsx) — Quản trị viên quản lý danh sách FAQ, phê duyệt các bản nháp FAQ được AI tự động sinh ra từ cụm Ticket hỗ trợ tương tự nhau.
* **Tầng 2 (Controller)**: `CatalogController.java:91, L100-135` (`/api/catalog/faq/admin`, `/api/catalog/faq`).
* **Tầng 3 (Service Interface)**: `CatalogService.java` (`getAllFaqEntries()`, `createFaqEntry()`, `updateFaqEntry()`, `deleteFaqEntry()`).
* **Tầng 4 (Service Impl)**: [`CatalogServiceImpl.java:316-355`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/catalog/service/impl/CatalogServiceImpl.java#L316):
  - **Luồng 6 - Bước 1**: Admin lấy toàn bộ FAQ (gồm cả bản nháp `published = false`).
  - **Luồng 6 - Bước 3**: Admin tạo bài viết FAQ mới & Ghi vết Audit Log.
  - **Luồng 6 - Bước 4**: Admin chỉnh sửa FAQ hoặc phê duyệt bản nháp do AI sinh ra chuyển `published: true`.
  - **Luồng 6 - Bước 5**: Admin xóa bài viết FAQ & Ghi vết Audit Log `DELETE_FAQ`.
* **Tầng 5 (Repository Interface)**: `FaqEntryRepository.java` (`save()`, `deleteById()`).
* **Tầng 6 (CSDL & Entity)**: Bảng `faq_entries`, bảng `audit_logs`.

---

### LUỒNG 2: TRỢ LÝ AI HỖ TRỢ THÔNG MINH (UNIVERSAL RAG CHATBOT 12 BƯỚC - UC-65)
*(Trong mã nguồn: `AiServiceImpl.java:114`, `AiController.java:50`, `AiProviderRouter.java:40`)*

* **Tầng 1 (UI)**: [AiFloatingWidget.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/ai/components/AiFloatingWidget.tsx), [AiAssistantPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/ai/pages/AiAssistantPage.tsx).
* **Tầng 2 (Controller)**: `AiController.java:50` nhận tại `@PostMapping("/chat")`.
* **Tầng 3 (Service Interface)**: `AiService.java`: `AiMessageResponse chat(ChatRequest request, Long userId);`.
* **Tầng 4 (Service Impl)**: [`AiServiceImpl.java:118-238`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/impl/AiServiceImpl.java#L118-L238):
  - *Bước 0*: Quét an toàn nội dung & Prompt Injection (`contentSafetyFilter`).
  - *Bước 0.4*: Mở rộng từ đồng nghĩa tiếng Việt & câu tiếp nối (`synonymService`).
  - *Bước 0.5*: Kiểm tra **Semantic Cache** trả về ngay **dưới 50ms** (`semanticCacheService`).
  - *Bước 1*: Phân loại ý định 3 tầng (Domain / Sub-Intent / Entities) qua 12 Intent Rules.
  - *Bước 2 & 2.5*: Level 0 Fast-Path (chào hỏi) & Out-of-Scope Gating (chặn câu hỏi ngoài lề).
  - *Bước 3*: Query Rewriting (viết lại câu hỏi kèm lịch sử chat).
  - *Bước 4*: Capability Policy & Role Verification (kiểm tra quyền vai trò người dùng).
  - *Bước 5 & 5.1*: **Real-time Business Context Injection (Dòng 422)**: Truy vấn trực tiếp dữ liệu sống từ MySQL (gia sư rảnh, lớp học đang mở, học phí thực tế).
  - *Bước 5.5*: Contextual Window Enrichment (mở rộng cửa sổ ngữ cảnh chunk tài liệu).
  - *Bước 6 & 7*: Đánh giá căn cứ (Grounding Evaluation) & Đóng gói thẻ card tương tác (Tutor Card, Class Card).
  - *Bước 8*: Gọi bộ định tuyến đa mô hình [`AiProviderRouter.java:40`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/ai/service/provider/AiProviderRouter.java#L40) theo chuỗi ưu tiên:
    $$\text{Groq (Llama 3)} \longrightarrow \text{Cerebras} \longrightarrow \text{DeepSeek} \longrightarrow \text{Gemini 2.0 Flash}$$
    *(Failover tự động khi gặp HTTP 429 Rate Limit hoặc Timeout 15s; Cooldown 60s; trần sinh tối đa 20s)*.
  - *Bước 9*: Hallucination Guard (quét đối soát triệt tiêu ảo giác sau khi sinh).
  - *Bước 10 & 11*: Lưu tin nhắn CSDL và ghi nhớ vào Semantic Cache.
* **Tầng 5 (Repository Interface)**:
  - `AiChatMessageRepository.java` ➡️ `save(message)`.
  - `AiChatSessionRepository.java` ➡️ `save(session)`.
  - `AiKnowledgeChunkRepository.java` ➡️ `findByActiveTrue()`.
* **Tầng 6 (CSDL & Entity)**: Bảng `ai_chat_messages`, `ai_chat_sessions`, `ai_knowledge_chunks`.

---

## CHƯƠNG 4: HỢP ĐỒNG ĐIỆN TỬ OTP & HỒ SƠ 3 VAI TRÒ (UC-44, M4, UC-08)

### 4.1. Hợp Đồng Điện Tử Ký Số OTP 2 Lớp (UC-44)
* **Tầng 1 (UI)**: [ContractDetailPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/contract/pages/ContractDetailPage.tsx).
* **Tầng 2 (Controller)**: [`ContractController.java:176, L193`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/contract/controller/ContractController.java) gửi mã OTP tại `@PostMapping("/{contractId}/send-otp"` và xác thực ký số tại `@PostMapping("/{contractId}/sign")`.
* **Tầng 3 (Service Interface)**: `ContractService.java`: `ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request);`.
* **Tầng 4 (Service Impl)**: [`ContractServiceImpl.java:476-545`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/contract/service/impl/ContractServiceImpl.java#L476-L545):
  1. *Kiểm tra pháp lý*: `assertSignerNotMinor()` (đủ 18 tuổi) và `assertSignerCccdComplete()` (đã có CCCD).
  2. *Xác thực OTP*: Gọi `otpService.verify(...)` (tối đa 5 lần thử, hạn 5 phút).
  3. *Tạo chữ ký số*: Lưu `ContractSignature` với dữ liệu `OTP_VERIFIED:{email}:{signedAt}`.
  4. *Kích hoạt khi đủ 2 bên ký*: Gọi `isFullySigned(contractId)`, nếu đủ 2 bên thì chuyển `contract.setStatus(SIGNED)` và phát sự kiện `publishContractSigned()` **lập tức khóa tiền ký quỹ Escrow**!
* **Tầng 5 (Repository Interface)**: `ContractRepository.java`, `ContractSignatureRepository.java`, `OtpRepository.java`.
* **Tầng 6 (CSDL & Entity)**: Bảng `contracts`, `contract_signatures`, `email_otps`.

---

### 4.2. Đánh Giá Sao & Tính Lại Điểm Uy Tín Sau Buổi Học
* **Tầng 1 (UI)**: [ContractDetailPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/contract/pages/ContractDetailPage.tsx) (dòng 150).
* **Tầng 2 (Controller)**: [`ContractController.java:54`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/contract/controller/ContractController.java#L54) nhận tại `@PostMapping("/reviews")`.
* **Tầng 3 (Service Interface)**: `ReviewService.java`: `ReviewResponse createReview(CreateReviewRequest request);`.
* **Tầng 4 (Service Impl)**:
  - [`ReviewServiceImpl.java:95-126`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/contract/service/impl/ReviewServiceImpl.java#L95-L126): Chặn tự đánh giá, chỉ cho phép người trong cuộc, mỗi phân công lớp chỉ được đánh giá đúng 1 lần.
  - [`ContractServiceImpl.java:2586-2612`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/contract/service/impl/ContractServiceImpl.java#L2586-L2612) (`recomputeTutorReputation`):
    Lấy đánh giá cuối cùng của mỗi lớp (`finalReviewPerClass`), tính trung bình, cập nhật `tutor.setRatingAvg(newScore)`, lưu lịch sử vào `reputation_histories`.
* **Tầng 5 (Repository Interface)**: `ReviewRepository.java`, `TutorRepository.java`, `ReputationHistoryRepository.java`.
* **Tầng 6 (CSDL & Entity)**: Bảng `reviews`, `tutors` (cột `rating_avg`), `reputation_histories`.

---

### 4.3. Quản Lý Hồ Sơ 3 Vai Trò & Onboarding Lần Đầu (UC-08)
* **Tầng 1 (UI)**:
  - [ClientProfilePage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/profile/pages/ClientProfilePage.tsx): Quản lý con cái phụ thuộc (`ChildProfile`).
  - [TutorProfilePage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/profile/pages/TutorProfilePage.tsx): Chứng chỉ (`TutorCertificate`), Lịch bận theo tuần (`TutorBusyTime`).
  - [CenterProfilePage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/profile/pages/CenterProfilePage.tsx): Quản lý danh sách gia sư trực thuộc (`CenterTutorMembership`).
* **Tầng 2 (Controller)**: [`ProfileController.java:236`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/profile/controller/ProfileController.java#L236) nhận tại `@PostMapping(value = "/me/avatar", consumes = "multipart/form-data")` và các endpoint `/api/profile`.
* **Tầng 3 (Service Interface)**: `ProfileService.java`: `uploadAvatar()`, `addCertificate()`, `addBusyTimes()`, `addChildProfile()`.
* **Tầng 4 (Service Impl)**: [`ProfileServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/profile/service/impl/ProfileServiceImpl.java):
  - *Tải avatar (Dòng 809)*: Kiểm tra Magic Bytes mảng byte đầu, dung lượng < 5MB, ghi file `/uploads/avatars/`, lưu `avatar_url`.
  - *Onboarding (Dòng 179)*: Sau khi lưu hồ sơ lần đầu, gán `user.setProfileCompletedAt(now)` để ẩn banner onboarding vĩnh viễn.
  - *Lịch bận (Dòng 679)*: Kiểm tra đối soát chống trùng khung giờ rảnh/bận (`clashes`).
* **Tầng 5 (Repository Interface)**: `ClientRepository`, `TutorRepository`, `TutorCenterRepository`, `TutorCertificateRepository`, `TutorBusyTimeRepository`, `ChildProfileRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `clients`, `tutors`, `tutor_centers`, `tutor_certificates`, `tutor_busy_times`, `child_profiles`.

---

### 4.4. Quên Mật Khẩu OTP Qua Email 2 Giai Đoạn (DEF-62)
* **Tầng 1 (UI)**: [ForgotPasswordPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/identity/pages/ForgotPasswordPage.tsx), [ResetPasswordPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/identity/pages/ResetPasswordPage.tsx).
* **Tầng 2 (Controller)**: [`IdentityController.java:107-123`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/identity/controller/IdentityController.java) tiếp nhận yêu cầu mã tại `@PostMapping("/password/forgot")`, xác thực OTP tại `@PostMapping("/password/forgot/verify-otp")` và đặt lại mật khẩu tại `@PostMapping("/password/reset")`.
* **Tầng 3 (Service Interface)**: `IdentityService.java`: `requestPasswordResetOtp()`, `verifyPasswordResetOtp()`, `resetPassword()`.
* **Tầng 4 (Service Impl)**: [`IdentityServiceImpl.java:470-533`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/identity/service/impl/IdentityServiceImpl.java#L470-L533):
  1. *Giai đoạn 1*: Rate limit theo IP (`acquireIpSlot`), gửi OTP 6 số qua email. Khi verify thành công, sinh chuỗi Token ngẫu nhiên (Opaque Reset Token) có hạn 10 phút.
  2. *Giai đoạn 2*: Người dùng gửi `resetToken` kèm mật khẩu mới. Kiểm tra `token.getUsedAt() == null`, mã hóa BCrypt lưu vào bảng `users`, đánh dấu `token.setUsedAt(now)` (Token dùng 1 lần duy nhất).
* **Tầng 5 (Repository Interface)**: `PasswordResetTokenRepository.java`, `UserRepository.java`.
* **Tầng 6 (CSDL & Entity)**: Bảng `users` (cột `password_hash`), bảng `password_reset_tokens`, bảng `email_otps`.

---

### 4.5. Chat Tin Nhắn Tức Thời 1-1 & Nhóm Lớp Học (UC-50)
* **Tầng 1 (UI)**: [MessagingPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/messaging/pages/MessagingPage.tsx), `ChatWindow.tsx`, `ConversationList.tsx` — Người dùng trò chuyện 1-1 giữa Phụ huynh và Gia sư, hoặc trao đổi trong nhóm chat lớp học.
* **Tầng 2 (Controller)**: [`ChatController.java:58`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/controller/ChatController.java#L58) nhận tại `/api/messaging`:
  - `POST /api/messaging/chats/messages`: Gửi tin nhắn mới.
  - `GET /api/messaging/chats/conversations`: Lấy danh sách hội thoại của người dùng.
  - `GET /api/messaging/chats/conversations/{id}/messages`: Phân trang lịch sử tin nhắn.
  - `POST /api/messaging/chats/groups`: Tạo nhóm chat lớp học.
* **Tầng 3 (Service Interface)**: [`ChatService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/service/ChatService.java):
  ```java
  MessageResponse sendMessage(SendMessageRequest request);
  List<ConversationResponse> getMyConversations();
  Page<MessageResponse> getMessages(Long conversationId, int page, int size);
  ```
* **Tầng 4 (Service Impl)**: [`ChatServiceImpl.java:480`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/service/impl/ChatServiceImpl.java#L480):
  - **Chốt chặn 1 (Gác cổng Xử phạt - L394)**: Gọi `penaltyAccessService.requireFeature(senderId, "MESSAGING")` — nếu tài khoản đang dính án phạt cấm nhắn tin thì lập tức quăng `ForbiddenException`!
  - **Chốt chặn 2 (Quét lách sàn thời gian thực - L490)**: Gọi `circumventionService.inspect(message)` — kiểm tra Regex 4 tầng xem tin nhắn có chứa số điện thoại, link Zalo, email hay không; nếu điểm rủi ro $\ge 65$ tự động lưu vết vi phạm vào bảng `circumvention_detections`.
  - Lưu tin nhắn vào CSDL và kích hoạt thông báo In-App / WebSocket tới các thành viên hội thoại.
* **Tầng 5 (Repository Interface)**: `MessageRepository.java`, `ConversationRepository.java`, `ConversationMemberRepository.java`.
* **Tầng 6 (CSDL & Entity)**: Bảng `conversations`, `messages`, `conversation_members`, `circumvention_detections`.

---

### 4.6. Thay Đổi Mật Khẩu Người Dùng (Change Password)
* **Tầng 1 (UI)**: [ProfilePage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/profile/pages/ProfilePage.tsx) — Modal đổi mật khẩu trong trang quản lý tài khoản.
* **Tầng 2 (Controller)**: [`IdentityController.java:101`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/identity/controller/IdentityController.java#L101) nhận tại `@PutMapping("/password")`.
* **Tầng 3 (Service Interface)**: `IdentityService.java`: `void changePassword(ChangePasswordRequest request);`.
* **Tầng 4 (Service Impl)**: [`IdentityServiceImpl.java:432`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/identity/service/impl/IdentityServiceImpl.java#L432):
  - Lấy tài khoản hiện tại từ `authHelper.currentUserId()`.
  - So khớp mật khẩu cũ qua BCrypt: `passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())`.
  - Validate định dạng mật khẩu mới và kiểm tra: `passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())` ➡️ nếu trùng mật khẩu cũ thì quăng ngoại lệ bắt buộc phải đổi mật khẩu khác!
  - Mã hóa mật khẩu mới: `user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()))`.
  - Lưu CSDL và ghi vết kiểm toán `CHANGE_PASSWORD` vào bảng `audit_logs`.
* **Tầng 5 (Repository Interface)**: `UserRepository.java` ➡️ `save(user)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `users` (cập nhật cột `password_hash`), bảng `audit_logs`.

---

### 4.7. Chuyên Sâu UC-30 & UC-40: Tranh Chấp Sự Cố Lớp & Cơ Chế Ký Quỹ Escrow
* **Nghiệp vụ UC-30 (Báo Cáo Sự Cố Lớp Học & Giải Quyết Tranh Chấp)**:
  - Phía Người dùng: Phụ huynh hoặc Gia sư gửi báo cáo sự cố vi phạm xảy ra trong lớp học (`targetType = CLASS`).
  - Phía Admin: Admin tiếp nhận tại màn hình `/platform/reports`, kích hoạt hàm [`resolveClassIssueReport` (PlatformServiceImpl.java:1726)](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1726) với **7 phương án can thiệp**.
  - **Tự động khóa Escrow**: Nếu chọn `ESCALATE_TO_DISPUTE` hoặc `TERMINATE_CLASS`, hệ thống tự động gọi `escrowService.holdForDispute()`, chuyển tiền ký quỹ của lớp sang trạng thái `ON_HOLD` và khởi tạo thực thể `Dispute` với `status = 'OPEN'`.
  - **Điểm uy tín**: Lớp học dính sự cố sẽ kích hoạt hàm tính lại điểm uy tín gia sư ([`ContractServiceImpl.java:2586`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/contract/service/impl/ContractServiceImpl.java#L2586)), làm sụt giảm điểm sao trung bình của gia sư trên sàn.
* **Nghiệp vụ UC-40 (Quản Trị Ký Quỹ Escrow, Ví Điện Tử & Quyết Toán Dòng Tiền)**:
  - Phía Ví điện tử: Quản lý số dư ví, nạp tiền tự động qua Cổng thanh toán SePay Webhook ([`FinanceController.java:98`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/finance/controller/FinanceController.java#L98)), tạo yêu cầu rút tiền về ngân hàng.
  - **Cơ chế Ký quỹ Escrow (Escrow Holding)**: Khi Phụ huynh và Gia sư ký số hợp đồng qua OTP (UC-44), tiền học phí được tự động khóa lại trong quỹ Escrow an toàn của sàn chứ không chuyển thẳng cho gia sư.
  - **Quyết toán (Settlement - SettlementServiceImpl.java:43)**: Khi lớp học hoàn thành các buổi học và điểm danh đầy đủ, hệ thống tự động trích tỷ lệ **phí sàn** (10% - cấu hình ở UC-58) thu về cho TCS, và giải ngân 90% còn lại vào ví khả dụng của Gia sư/Trung tâm; hoặc hoàn tiền (**Refund**) lại cho Phụ huynh nếu lớp học bị hủy/phán quyết vi phạm.
* **Bảng CSDL tác động**: `wallets`, `wallet_transactions`, `escrow_transactions` (`HELD`, `ON_HOLD`, `RELEASED`, `REFUNDED`), `reports`, `disputes`.

---

### 4.8. Quản Lý Bản Tin & Thông Báo Toàn Sàn (UC-59 - Announcement Controller)
* **Tầng 1 (UI)**: [PlatformTasksPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformTasksPage.tsx), banner thông báo nổi trên trang chủ [HomePage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/home/pages/HomePage.tsx).
* **Tầng 2 (Controller)**: [`AnnouncementController.java:45`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/AnnouncementController.java#L45) nhận tại `/api/platform/announcements`.
* **Tầng 3 (Service Interface)**: [`AnnouncementService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/AnnouncementService.java): `upsertAnnouncement()`, `toggleAnnouncement()`, `deleteAnnouncement()`.
* **Tầng 4 (Service Impl)**: [`AnnouncementServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/AnnouncementServiceImpl.java):
  - Admin tạo hoặc chỉnh sửa thông báo chính sách, lịch bảo trì hoặc sự kiện sàn.
  - Phân loại đối tượng mục tiêu: `ALL`, `TUTOR`, `CLIENT`, `CENTER`.
  - Tự động phát thông báo In-App thời gian thực tới nhóm người dùng mục tiêu và ghi vết vào `audit_logs`.
* **Tầng 5 (Repository Interface)**: `AnnouncementRepository.java`.
* **Tầng 6 (CSDL & Entity)**: Bảng `announcements`, bảng `notifications`, bảng `audit_logs`.

---

### 4.9. Đăng Xuất & Thu Hồi Phiên JWT Bằng `token_version` (LUỒNG 26)
* **Tầng 1 (UI)**: Người dùng nhấn nút "Đăng xuất" trên Navbar, Client xóa JWT Token khỏi LocalStorage / SessionStorage.
* **Tầng 2 (Controller)**: [`IdentityController.java:78`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/identity/controller/IdentityController.java#L78) nhận tại `@PostMapping("/logout")`.
* **Tầng 3 (Service Interface)**: `IdentityService.java`: `void logout();`.
* **Tầng 4 (Service Impl)**: [`IdentityServiceImpl.java:390`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/identity/service/impl/IdentityServiceImpl.java#L390):
  - Lấy người dùng hiện tại từ Security Context qua `authHelper.currentUserId()`.
  - Thực hiện tăng phiên đăng nhập: `user.setTokenVersion(user.getTokenVersion() + 1L)`.
  - Gọi `userRepository.save(user)` lưu CSDL.
  - Ghi nhật ký kiểm toán `auditLogService.record("LOGOUT", "User", userId, ...)`.
* **Tầng 5 (Repository Interface)**: `UserRepository.java` ➡️ `save(user)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `users` (cập nhật cột `token_version`), bảng `audit_logs`.
* **Cơ chế thu hồi phiên Stateless không cần Redis**:
  - Tại [`JwtAuthenticationFilter.java:43-48`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/security/JwtAuthenticationFilter.java#L43):
    ```java
    if (!userDetails.isEnabled() || principal.getTokenVersion() != jwtService.extractTokenVersion(claims)) {
        filterChain.doFilter(request, response);
        return;
    }
    ```
  - Khi người dùng gửi request kèm JWT cũ lên, filter phát hiện `principal.getTokenVersion()` trong DB đã lớn hơn version lưu trong claims của Token ➡️ Request lập tức bị từ chối 401 Unauthorized, vô hiệu hóa toàn bộ token cũ ngay lập tức mà không cần tốn tài nguyên duy trì Redis blacklist!

---

# PHẦN 3: KỊCH BẢN THUYẾT TRÌNH MẪU "CHỈ CODE 6 BƯỚC"

Khi thầy cô yêu cầu: *"Em hãy demo và giải thích luồng hoạt động của tính năng X"*, bạn hãy áp dụng công thức 6 bước chuẩn này:

### Ví dụ Thực Chiến: Trình Bày Tính Năng "Gộp Ticket Trùng Lặp (Merge Ticket)"

> 🎙️ **Kịch bản nói (khoảng 45 giây)**:
> 
> *"Dạ thưa thầy/cô, em xin trình bày chức năng Gộp phiếu khiếu nại trùng lặp theo đúng 6 tầng kiến trúc của hệ thống:*
> 
> * **Tầng 1 (UI)**: Tại màn hình `PlatformTicketsPage.tsx` dòng 103, admin bấm nút 'Gộp ticket', nhập mã ticket đích và lý do gộp, gọi hàm `handleMerge()`.
> * **Tầng 2 (Controller)**: Request gửi qua endpoint `POST /api/platform/tickets/{id}/merge` và được tiếp nhận tại `PlatformController.java` dòng 359.
> * **Tầng 3 (Service Interface)**: Controller ủy quyền xử lý qua interface `PlatformService.java` dòng 101 với phương thức `mergeTicket(sourceTicketId, request)`.
> * **Tầng 4 (Service Impl)**: Logic thực thi tại `PlatformServiceImpl.java` dòng 1360. Tại đây em áp dụng 4 chốt chặn an toàn: kiểm tra 2 ticket không trùng nhau, **bắt buộc phải thuộc cùng một người dùng** để tránh lộ thông tin cá nhân, và cả 2 ticket chưa bị đóng.
> * **Tầng 5 (Repository Interface)**: Hệ thống gọi `TicketMessageRepository.save()` để dời nội dung sang ticket đích, và gọi `SupportTicketRepository.save()` để đóng ticket nguồn.
> * **Tầng 6 (Database)**: CSDL cập nhật ticket nguồn sang trạng thái `CLOSED`, tạo bản ghi tin nhắn mới trong bảng `ticket_messages` của ticket đích, và ghi vết kiểm toán vào bảng `audit_logs`."*

---

# PHẦN 4: BỘ CÂU HỎI PHẢN BIỆN "BẪY" CỦA HỘI ĐỒNG & CÂU TRẢ LỜI MẪU

#### ❓ Câu 1: *"Tại sao trong Controller em lại tiêm Service Interface thay vì tiêm thẳng ServiceImpl?"*
* **Trả lời**: *"Dạ thưa thầy cô, đây là việc tuân thủ nguyên lý **Dependency Inversion Principle (chữ D trong SOLID)** và giảm thiểu phụ thuộc chặt (Loose Coupling): Tầng Controller chỉ phụ thuộc vào hợp đồng trừu tượng (Interface), giúp dễ dàng viết Unit Test giả lập bằng `@MockBean` và cho phép mở rộng các cách thực thi nghiệp vụ khác nhau mà không làm thay đổi code của Controller ạ."*

#### ❓ Câu 2: *"Tại sao khi User gửi ticket có Category là DISPUTE thì hệ thống lại ép Priority lên URGENT mà không cho User tự chọn?"*
* **Trả lời**: *"Dạ, theo phân tích nghiệp vụ, khi xảy ra tranh chấp học phí hoặc sự cố lớp học, quyền lợi tài chính của khách hàng đang bị đe dọa trực tiếp. Nếu để người dùng chọn nhầm độ ưu tiên thấp (như LOW với hạn cam kết SLA là 48h), sàn sẽ bị chậm trễ trong việc can thiệp. Việc ép sàn lên URGENT với SLA 4 giờ giúp sàn xử lý sự cố kịp thời, bảo vệ uy tín nền tảng."*

#### ❓ Câu 3: *"Cơ chế Auto-Assignment khi Admin mở ticket có nhược điểm gì không? Nếu Admin chỉ vô tình bấm vào xem rồi tắt đi mà không làm gì thì ticket có bị 'ngâm' mãi ở trạng thái IN_PROGRESS không?"*
* **Trả lời**: *"Dạ, để giải quyết triệt để vấn đề này, hệ thống đã xây dựng thêm tính năng Quét quá hạn SLA (`scanAndEscalateSlaBreaches`). Dù ticket đã được gán cho Admin nào, nếu sau thời gian quy định (tính theo dueAt) mà ticket vẫn chưa có phản hồi hoặc chưa được giải quyết, hệ thống sẽ tự động kích hoạt cảnh báo trực ban và thông báo khẩn cấp tới toàn bộ ban quản trị để tái phân công."*

#### ❓ Câu 4: *"Tại sao khi xuất báo cáo CSV tài chính em lại phải chèn thêm dấu nháy đơn `'` ở đầu ô nếu ô đó bắt đầu bằng dấu `=`, `+`, `-`?"*
* **Trả lời**: *"Dạ, đây là giải pháp phòng chống lỗ hổng bảo mật **CSV / Formula Injection (DDE)**. Kẻ gian có thể cố tình đặt tên hoặc ghi chú chứa các ký tự này để khi Quản trị viên tải file CSV về mở bằng Microsoft Excel, Excel sẽ tự động biên dịch ô đó thành một công thức lệnh hệ điều hành và thực thi mã độc. Việc chèn dấu nháy đơn buộc Excel hiểu đây là chuỗi văn bản thuần túy và vô hiệu hóa công thức nguy hiểm."*

#### ❓ Câu 5: *"Làm thế nào để phân hệ Trợ lý AI không bịa đặt số liệu tài chính hoặc số lượng lớp học đang mở?"*
* **Trả lời**: *"Dạ, hệ thống áp dụng kỹ thuật RAG kết hợp với 2 mắt xích then chốt: **Bước 5.1 (Real-time Business Context Injection)** và **Bước 9 (Hallucination Guard)** trong `AiServiceImpl.java`. Trước khi gọi LLM, hệ thống truy vấn dữ liệu thực tế thời gian thực từ CSDL MySQL và tiêm thẳng vào ngữ cảnh Prompt. Sau khi nhận kết quả từ LLM, hệ thống chạy bộ lọc đối soát thông tin để đảm bảo câu trả lời phản ánh 100% dữ liệu thực từ CSDL."*

#### ❓ Câu 6: *"Nếu Quản trị viên khóa tài khoản một người dùng bị vi phạm, làm sao để đảm bảo người dùng đó không tiếp tục dùng JWT Token cũ để gọi API?"*
* **Trả lời**: *"Dạ, hệ thống áp dụng kỹ thuật 2 lớp bảo vệ: Tại `PlatformServiceImpl.java`, khi Admin đổi trạng thái sang BANNED, thuộc tính `enabled` của UserPrincipal chuyển thành `false`. Trong `JwtAuthenticationFilter`, mỗi request gửi lên đều kiểm tra `userDetails.isEnabled()`. Nếu bị Banned, request lập tức bị từ chối với mã lỗi 401/403 mà không cần chờ token JWT hết hạn ạ."*

#### ❓ Câu 7: *"UC-30 và UC-40 liên kết với nhau như thế nào trong bài toán bảo vệ dòng tiền học sinh?"*
* **Trả lời**: *"Dạ thưa thầy cô, UC-40 là hạ tầng Ký quỹ Escrow quản lý việc giữ tiền học phí khi hợp đồng được ký số OTP. Còn UC-30 là quy trình xử lý Báo cáo sự cố lớp học. Khi có sự cố nghiêm trọng xảy ra ở UC-30, tại hàm `resolveClassIssueReport`, hệ thống sẽ lập tức can thiệp sang UC-40 để gọi `escrowService.holdForDispute()`, đóng băng số tiền đang ký quỹ không cho giải ngân về ví gia sư, từ đó đảm bảo tiền của học viên luôn an toàn cho tới khi tranh chấp được phán quyết xong ạ."*

#### ❓ Câu 8: *"Làm thế nào để hệ thống ngăn chặn người dùng gửi số điện thoại hoặc link Zalo trong chat tin nhắn?"*
* **Trả lời**: *"Dạ, tại hàm `sendMessage` trong `ChatServiceImpl.java`, trước khi lưu tin nhắn vào CSDL, em đã tích hợp gọi `CircumventionService.inspect(message)`. Hàm này chạy 4 bộ lọc Regex thời gian thực (nhận diện số điện thoại, email, URL và từ khóa mạng xã hội như Zalo, Telegram). Nếu phát hiện điểm rủi ro vượt ngưỡng an toàn (>= 65 điểm), hệ thống lập tức gắn cờ cảnh báo và lưu vào bảng `circumvention_detections` để Admin thanh tra và xử phạt theo UC-60 ạ."*

#### ❓ Câu 9: *"Tại sao hệ thống lại thiết kế trường `token_version` trong bảng `users` và kiểm tra trong `JwtAuthenticationFilter`?"*
* **Trả lời**: *"Dạ thưa thầy cô, JWT về bản chất là stateless (phi trạng thái) nên sau khi cấp phát, token vẫn có hiệu lực cho đến khi hết hạn. Nếu người dùng bấm Đăng xuất (`logout()`), hoặc tài khoản cần thu hồi phiên, nếu không có cơ chế thu hồi thì token cũ vẫn có thể tiếp tục được sử dụng. Bằng cách thiết kế trường `token_version` trong bảng `users`, mỗi khi người dùng Đăng xuất, hệ thống tăng `token_version = token_version + 1`. Tại `JwtAuthenticationFilter`, mỗi request gửi lên đều so khớp `principal.getTokenVersion() != jwtService.extractTokenVersion(claims)`. Khi thấy version lệch nhau, request lập tức bị từ chối 401, vô hiệu hóa toàn bộ token cũ ngay lập tức mà không cần phải duy trì Redis blacklist tốn kém tài nguyên ạ."*

---

*Cẩm nang này đã được cập nhật chuẩn 6 tầng kiến trúc trực tiếp tại [docs/CAM_NANG_ON_TAP_BAO_VE_DUCHM.md](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/docs/CAM_NANG_ON_TAP_BAO_VE_DUCHM.md).*
