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

| STT | Tên Chức Năng / Use Case | Tầng 1: Frontend UI | Tầng 2: Controller & Endpoint | Tầng 3: Service Interface | Tầng 4: Service Implementation | Tầng 5: Repository Interface | Tầng 6: Bảng CSDL Tác Động |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | **User tạo Ticket khiếu nại** | `MessagingPanel.tsx` (L120) | `MessagingController.java` (L110)<br>`POST /api/messaging/tickets` | `MessagingService.java` (L19)<br>`createSupportTicket()` | `MessagingServiceImpl.java` (L116)<br>`createSupportTicket()` | `SupportTicketRepository.java`<br>`TicketMessageRepository.java` | `support_tickets`, `ticket_messages` |
| **2** | **Admin lọc & xem danh sách Ticket** | `PlatformTicketsPage.tsx` (L25) | `PlatformController.java` (L318)<br>`GET /api/platform/tickets` | `PlatformService.java` (L84)<br>`getTickets()` | `PlatformServiceImpl.java` (L1176)<br>`getTickets()` | `SupportTicketRepository.java` (L37)<br>`search(status, category, ...)` | `support_tickets` |
| **3** | **Admin mở Ticket (Auto-Assign PIC)** | `PlatformTicketsPage.tsx` (L63) | `PlatformController.java` (L334)<br>`GET /api/platform/tickets/{id}` | `PlatformService.java` (L89)<br>`getTicketDetail()` | `PlatformServiceImpl.java` (L1201)<br>`getTicketDetail()` | `SupportTicketRepository.java`<br>`findById()`, `save()` | `support_tickets`<br>(`assigned_admin_id`, `status='IN_PROGRESS'`) |
| **4** | **Admin phản hồi Ticket (First SLA)** | `PlatformTicketsPage.tsx` (L94) | `PlatformController.java` (L346)<br>`POST /api/platform/tickets/{id}/respond` | `PlatformService.java` (L95)<br>`respondToTicket()` | `PlatformServiceImpl.java` (L1278)<br>`respondToTicket()` | `TicketMessageRepository.java`<br>`SupportTicketRepository.java` | `ticket_messages`, `support_tickets`<br>(`response_sla_ms`), `audit_logs` |
| **5** | **Đóng / Giải quyết Ticket** | `PlatformTicketsPage.tsx` (L99) | `PlatformController.java` (L353)<br>`POST /api/platform/tickets/{id}/close` | `PlatformService.java` (L98)<br>`closeTicket()` | `PlatformServiceImpl.java` (L1322)<br>`closeTicket()` | `SupportTicketRepository.java`<br>`save()` | `support_tickets`<br>(`status`, `resolved_at`, `closed_at`) |
| **6** | **Gộp Ticket trùng lặp (Merge Ticket)** | `PlatformTicketsPage.tsx` (L103) | `PlatformController.java` (L359)<br>`POST /api/platform/tickets/{id}/merge` | `PlatformService.java` (L101)<br>`mergeTicket()` | `PlatformServiceImpl.java` (L1360)<br>`mergeTicket()` | `TicketMessageRepository.java`<br>`SupportTicketRepository.java` | `ticket_messages`, `support_tickets`<br>(`status='CLOSED'`), `audit_logs` |
| **7** | **Quét & nâng cấp quá hạn SLA (Job-11)** | `useAdminTickets.tsx` (L45) | `PlatformController.java` (L365)<br>`POST /api/platform/tickets/scan-sla` | `PlatformService.java` (L104)<br>`scanAndEscalateSlaBreaches()` | `PlatformServiceImpl.java` (L1433)<br>`scanAndEscalateSlaBreaches()` | `SupportTicketRepository.java` (L58)<br>`findBreachedCandidateTickets()` | `support_tickets`<br>(`priority`, `sla_breached=true`) |
| **8** | **Chuyển Ticket sang Tranh chấp Escrow** | `PlatformTicketsPage.tsx` (L112) | `PlatformController.java` (L371)<br>`POST /api/platform/tickets/{id}/redirect-dispute` | `PlatformService.java` (L107)<br>`redirectTicketToDispute()` | `PlatformServiceImpl.java` (L1524)<br>`redirectTicketToDispute()` | `ReportRepository.java`<br>`SupportTicketRepository.java` | `reports` (`target_type='CLASS'`),<br>`support_tickets`, `audit_logs` |
| **9** | **Tra cứu FAQ & Hỗ trợ công khai** | `HelpPage.tsx` (L1) | `CatalogController.java` (L25)<br>`GET /api/catalog/faqs` | `CatalogService.java`<br>`getPublicFaqEntries()` | `CatalogServiceImpl.java` (L215)<br>`getPublicFaqEntries()` | `FaqEntryRepository.java`<br>`findByPublishedTrue...` | `faq_entries` |
| **10** | **Quản trị FAQ (Admin CRUD & Duyệt nháp)** | `PlatformFaqPage.tsx` (L1) | `CatalogController.java` (L80)<br>`/api/catalog/admin/faqs` | `CatalogService.java`<br>`createFaqEntry()`, `update...` | `CatalogServiceImpl.java` (L331)<br>`createFaqEntry()`, `update...` | `FaqEntryRepository.java`<br>`save()`, `delete()` | `faq_entries`, `audit_logs` |
| **11** | **Admin Dashboard KPI & Sức khỏe sàn** | `PlatformDashboardPage.tsx` (L1) | `PlatformController.java` (L158)<br>`GET /api/platform/dashboard` | `PlatformService.java` (L56)<br>`getDashboard()` | `PlatformServiceImpl.java` (L398)<br>`getDashboard()` | `TutorRepository`, `TutoringClassRepository`, `EscrowTransactionRepository` | `users`, `tutoring_classes`,<br>`escrow_transactions`, `wallets` |
| **12** | **Quản lý User & Khóa Token JWT tức thì** | `PlatformUsersPage.tsx` (L1) | `PlatformController.java` (L135)<br>`PATCH /api/platform/users/{id}/status` | `PlatformService.java` (L49)<br>`updateUserStatus()` | `PlatformServiceImpl.java` (L300)<br>`updateUserStatus()` | `UserRepository.java`<br>`save()` | `users` (`status='BANNED'`, `token_version`),<br>(Filter: `JwtAuthenticationFilter:43`) |
| **13** | **Thẩm định danh tính eKYC CCCD / Bằng cấp** | `PlatformVerificationsPage.tsx` (L1) | `PlatformController.java` (L199)<br>`POST /api/platform/verifications/{id}/review` | `PlatformService.java` (L62)<br>`reviewVerification()` | `PlatformServiceImpl.java` (L542)<br>`reviewVerification()` | `VerificationRequestRepository`<br>`TutorRepository`, `TutorCenterRepo` | `verification_requests`, `verification_histories`,<br>`tutors.verification_status` |
| **14** | **Xử lý sự cố lớp học (7 phương án) & Khóa Escrow** | `PlatformReportsPage.tsx` (L1) | `PlatformController.java` (L271)<br>`PATCH /api/platform/reports/{id}/resolve` | `PlatformService.java` (L66)<br>`resolveClassIssue()` | `PlatformServiceImpl.java` (L829 & L1726)<br>`resolveClassIssueReport()` | `ReportRepository`, `DisputeRepository`,<br>`EscrowTransactionRepository` | `reports`, `escrow_transactions` (`ON_HOLD`),<br>`disputes` (`status='OPEN'`) |
| **15** | **Xử phạt người dùng (User Penalties)** | `PlatformPenaltiesPage.tsx` (L1) | `PlatformPenaltyController.java` (L45)<br>`POST /api/platform/penalties` | `PenaltyService.java`<br>`createPenalty()` | `PenaltyServiceImpl.java` (L50)<br>`createPenalty()` | `UserPenaltyRepository.java`<br>`save()` | `user_penalties`, `audit_logs` |
| **16** | **Chặn tính năng khi bị phạt (Cross-module Guard)** | Filter / Service Guard liên module | Gọi nội bộ Service | `PenaltyAccessService.java` (L25)<br>`requireFeature(userId, code)` | `PenaltyAccessServiceImpl.java` (L45)<br>`requireFeature(userId, code)` | `UserPenaltyRepository.java`<br>`findByUser_UserIdAndStatus()` | `user_penalties`<br>(Quăng `ForbiddenException`) |
| **17** | **Phát hiện lách sàn Regex (Circumvention)** | `PlatformCircumventionPage.tsx` (L1) | `CircumventionController.java` (L30)<br>`GET /api/platform/circumventions` | `CircumventionService.java`<br>`inspect()`, `review...` | `CircumventionServiceImpl.java` (L84)<br>`inspect()`, `review...` | `CircumventionEventRepository`<br>`MessageRepository` | `circumvention_detections`, `messages` |
| **18** | **Giám sát Nhật ký Kiểm toán (Audit Logs)** | `PlatformAuditLogsPage.tsx` (L1) | `AuditLogController.java` (L30)<br>`GET /api/platform/audit-logs` | `AuditLogService.java`<br>`record()`, `getAuditLogs()` | `AuditLogServiceImpl.java` (L76)<br>`record()`, `getAuditLogs()` | `AuditLogRepository.java`<br>`save()`, `search()` | `audit_logs` (Bất biến: Chỉ INSERT & SELECT) |
| **19** | **Báo cáo Tài chính & Xuất CSV an toàn** | `PlatformAnalyticsPage.tsx` (L1) | `PlatformAnalyticsController.java` (L80)<br>`GET /api/platform/analytics/export-csv` | `PlatformAnalyticsService.java`<br>`exportCsv()` | `PlatformAnalyticsServiceImpl.java` (L359)<br>`exportCsv()`, `escapeCsv()` | `PaymentTransactionRepository`<br>`EscrowTransactionRepository` | `payment_transactions`, `escrow_transactions`,<br>(Chống DDE Injection & UTF-8 BOM `\uFEFF`) |
| **20** | **Cấu hình biểu phí sàn & Phí đối tác** | `PlatformFeeSettingsPage.tsx` (L1) | `PlatformController.java` (L420)<br>`PUT /api/platform/centers/{id}/fee` | `PlatformService.java` (L127)<br>`updateCenterFeeConfig()` | `PlatformServiceImpl.java` (L2549)<br>`updateCenterFeeConfig()` | `TutorCenterRepository.java`<br>`save()` | `tutor_centers` (`custom_fee_rate`),<br>`audit_logs` |
| **21** | **Quản lý Mẫu hợp đồng Master** | `PlatformContractTemplatesPage.tsx` (L1) | `PlatformController.java` (L480)<br>`POST /api/platform/contract-templates` | `PlatformService.java` (L112)<br>`createContractTemplate()` | `PlatformServiceImpl.java` (L2265)<br>`createContractTemplate()` | `ContractTemplateRepository.java`<br>`save()` | `contract_templates`<br>(Xóa mềm: `status='ARCHIVED'`) |
| **22** | **Hợp đồng điện tử Ký số OTP (UC-44)** | `ContractDetailPage.tsx` (L1) | `ContractController.java` (L80)<br>`POST /api/contract/{id}/sign-otp` | `ContractService.java`<br>`signWithOtp()` | `ContractServiceImpl.java` (L476)<br>`signWithOtp()`, `publishSigned()` | `ContractRepository.java`<br>`ContractSignatureRepository.java` | `contracts` (`SIGNED`), `contract_signatures`,<br>`email_otps` (Kích hoạt khóa Escrow) |
| **23** | **Đánh giá sao sau buổi học & Điểm uy tín** | `ContractDetailPage.tsx` (L150) | `ContractController.java` (L48)<br>`POST /api/contract/reviews` | `ReviewService.java`, `ContractService.java`<br>`recomputeReputation...` | `ReviewServiceImpl.java` (L45)<br>`ContractServiceImpl.java` (L2580) | `ReviewRepository.java`<br>`TutorRepository.java` | `reviews`, `tutors.rating_avg`,<br>`reputation_histories` |
| **24** | **Trợ lý Ảo AI Universal RAG 12 Bước** | `AiFloatingWidget.tsx`, `AiAssistantPage.tsx` | `AiController.java` (L50)<br>`POST /api/ai/chat` | `AiService.java`<br>`chat()` | `AiServiceImpl.java` (L118)<br>`AiProviderRouter.java` (L40) | `AiChatMessageRepository`<br>`AiKnowledgeChunkRepository` | `ai_chat_messages`, `ai_chat_sessions`,<br>`ai_knowledge_chunks` |
| **25** | **Quản lý Hồ sơ 3 vai trò & Avatar Onboarding (UC-08)** | `ClientProfilePage.tsx`, `TutorProfilePage.tsx` | `ProfileController.java` (L45)<br>`/api/profile/avatar`, `/profile/me` | `ProfileService.java`<br>`uploadAvatar()`, `addCertificate()` | `ProfileServiceImpl.java` (L809)<br>`uploadAvatar()`, `addCertificate()` | `ClientRepository`, `TutorRepository`,<br>`TutorCertificateRepository` | `clients`, `tutors`, `tutor_certificates`,<br>`tutor_busy_times`, `child_profiles` |
| **26** | **Quên mật khẩu OTP qua Email (DEF-62)** | `ForgotPasswordPage.tsx`, `ResetPasswordPage.tsx` | `IdentityController.java` (L75)<br>`POST /api/identity/reset-password` | `IdentityService.java`<br>`resetPassword()` | `IdentityServiceImpl.java` (L519)<br>`resetPassword()` | `PasswordResetTokenRepository`<br>`UserRepository.java` | `users.password_hash`,<br>`password_reset_tokens` (Dùng 1 lần) |

---

# PHẦN 2: GIẢI PHẪU CHI TIẾT TỪNG LUỒNG CODE THỰC CHIẾN 6 TẦNG

---

## CHƯƠNG 1: PHÂN HỆ HỖ TRỢ KHÁCH HÀNG (BF-09)

### 1.1. Luồng Người Dùng Tạo Ticket Khiếu Nại & Thuật Toán Ép Sàn Priority
* **Tầng 1 (UI)**: `MessagingPanel.tsx` (dòng 120-170). Người dùng nhập form, bấm "Gửi yêu cầu hỗ trợ", gọi hàm `handleCreateTicket()`.
* **Tầng 2 (Controller)**: [`MessagingController.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/controller/MessagingController.java#L110) nhận tại `@PostMapping("/tickets")`.
* **Tầng 3 (Service Interface)**: [`MessagingService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/service/MessagingService.java#L19) dòng 19:
  ```java
  SupportTicketResponse createSupportTicket(CreateSupportTicketRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`MessagingServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/messaging/service/impl/MessagingServiceImpl.java#L116-L157) dòng 116-157:
  1. *Lấy User*: `User user = userRepository.findById(authHelper.currentUserId()).orElseThrow(...)`.
  2. *Ép sàn độ ưu tiên (`escalatePriority` - dòng 180-187)*:
     - `DISPUTE` ➡️ Ép sàn lên **`URGENT`** (SLA 4h).
     - `SYSTEM_ERROR` / `REPORT_USER` ➡️ Ép lên **`HIGH`** (SLA 12h).
     - `BUG_REPORT` ➡️ Ép lên **`MEDIUM`** (SLA 24h).
     - `INQUIRY` ➡️ Giữ ở **`LOW`** (SLA 48h).
  3. *Tính hạn chót SLA*: `ticket.setDueAt(LocalDateTime.now().plusHours(calculateSlaHours(priority)))`.
  4. *Khởi tạo tin nhắn đầu tiên (`createTicketConversation` - dòng 203)*: Tạo 1 bản ghi `TicketMessage` với `isFromAdmin = false`.
  5. *Bắn thông báo Admin*: Gửi In-App notification tới toàn bộ `PlatformAdmin` đang `ACTIVE`.
* **Tầng 5 (Repository Interface)**:
  - [`SupportTicketRepository.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/repository/SupportTicketRepository.java#L16) kế thừa `JpaRepository<SupportTicket, Long>`: gọi `save(ticket)`.
  - [`TicketMessageRepository.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/repository/TicketMessageRepository.java#L9) kế thừa `JpaRepository<TicketMessage, Long>`: gọi `save(message)`.
* **Tầng 6 (CSDL & Entity)**: Thực thể `SupportTicket` lưu bảng `support_tickets` (`status='OPEN'`), thực thể `TicketMessage` lưu bảng `ticket_messages`.

---

### 1.2. Luồng Admin Tiếp Nhận & Tự Động Gán PIC (Auto-Assignment)
* **Tầng 1 (UI)**: Quản trị viên mở bảng điều khiển `PlatformTicketsPage.tsx` (dòng 63), bấm vào xem một ticket đang `OPEN`.
* **Tầng 2 (Controller)**: [`PlatformController.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L334) nhận tại `@GetMapping("/tickets/{ticketId}")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L89) dòng 89:
  ```java
  SupportTicketDetailResponse getTicketDetail(Long ticketId);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1200-L1212) dòng 1200-1212:
  ```java
  if (ticket.getStatus() == SupportTicketStatus.OPEN) {
      PlatformAdmin admin = currentAdminOrThrow();
      ticket.setAssignedAdmin(admin);
      ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
      ticket = supportTicketRepository.save(ticket);
  }
  ```
* **Tầng 5 (Repository Interface)**: `SupportTicketRepository.java`: gọi `findById(ticketId)` và `save(ticket)`.
* **Tầng 6 (CSDL & Entity)**: Cập nhật bản ghi trong bảng `support_tickets` (`assigned_admin_id = adminId`, `status = 'IN_PROGRESS'`).

---

### 1.3. Luồng Admin Phản Hồi & Đo Lường First Response SLA
* **Tầng 1 (UI)**: Admin nhập câu trả lời tại `PlatformTicketsPage.tsx` (dòng 94) bấm "Gửi phản hồi".
* **Tầng 2 (Controller)**: [`PlatformController.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L346) nhận tại `@PostMapping("/tickets/{ticketId}/respond")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L95) dòng 95:
  ```java
  SupportTicketDetailResponse respondToTicket(Long ticketId, RespondTicketRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1278-L1318):
  1. Tạo `TicketMessage`: `isFromAdmin = true`, `sender = admin.getUser()`.
  2. Đổi trạng thái: `ticket.setStatus(SupportTicketStatus.IN_REVIEW)`.
  3. **Đo First Response SLA (Dòng 1305)**:
     ```java
     if (ticket.getResponseSlaMs() == null && ticket.getCreatedAt() != null) {
         ticket.setResponseSlaMs(Duration.between(ticket.getCreatedAt(), now).toMillis());
     }
     ```
  4. Đánh dấu `ticket.setSlaBreached(true)` nếu `now.isAfter(ticket.getDueAt())`.
  5. Ghi Audit Log: `auditLogService.record("RESPOND_TICKET", "SupportTicket", ticketId, null, request)`.
* **Tầng 5 (Repository Interface)**:
  - `TicketMessageRepository.java` ➡️ `save(message)`.
  - `SupportTicketRepository.java` ➡️ `save(ticket)`.
  - `AuditLogRepository.java` ➡️ `save(auditLog)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `ticket_messages` (thêm dòng mới), bảng `support_tickets` (`response_sla_ms`, `status='IN_REVIEW'`), bảng `audit_logs`.

---

### 1.4. Luồng Đóng / Giải Quyết Ticket
* **Tầng 1 (UI)**: Admin bấm "Giải quyết" hoặc "Đóng" trên `PlatformTicketsPage.tsx` (dòng 99).
* **Tầng 2 (Controller)**: [`PlatformController.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L353) nhận tại `@PostMapping("/tickets/{ticketId}/close")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L98) dòng 98:
  ```java
  SupportTicketDetailResponse closeTicket(Long ticketId, CloseTicketRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1322-L1351):
  - Validate: Chỉ nhận `RESOLVED` hoặc `CLOSED`.
  - Ghi nhận: Nếu `RESOLVED` gán `resolvedAt = now`. Nếu `CLOSED` gán `closedAt = now`.
  - Ghi vết kiểm toán `CLOSE_TICKET` vào bảng `audit_logs`.
* **Tầng 5 (Repository Interface)**: `SupportTicketRepository.java` ➡️ `save(ticket)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `support_tickets` (`status`, `resolved_at`, `closed_at`).

---

### 1.5. 🌟 Thuật Toán Tự Động Quét & Thăng Cấp Quá Hạn SLA (Job-11)
* **Tầng 1 (Trigger)**: Chạy tự động qua Scheduler định kỳ hoặc Admin kích hoạt qua UI.
* **Tầng 2 (Controller)**: [`PlatformController.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L365) nhận tại `@PostMapping("/tickets/scan-sla")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L104) dòng 104:
  ```java
  int scanAndEscalateSlaBreaches();
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1433-L1519):
  1. Gọi `supportTicketRepository.findBreachedCandidateTickets(excludedStatuses, now)`.
  2. Lặp qua các ticket vi phạm, gọi hàm `escalatePriority(oldPriority)`:
     `LOW` ➡️ `MEDIUM` ➡️ `HIGH` ➡️ `URGENT`.
  3. Đánh dấu `ticket.setSlaBreached(true)`, lưu CSDL, ghi log `SLA_BREACH_ESCALATION`.
  4. Bắn thông báo kép: Báo động khẩn cấp tới Admin và gửi lời xin lỗi kèm cập nhật tiến độ tới User.
* **Tầng 5 (Repository Interface)**: [`SupportTicketRepository.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/repository/SupportTicketRepository.java#L51-L60) (dòng 51-60):
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
* **Tầng 6 (CSDL & Entity)**: Bảng `support_tickets` (`priority = newPriority`, `sla_breached = true`), bảng `audit_logs`, bảng `notifications`.

---

### 1.6. 🌟 Thuật Toán Gộp Ticket Trùng Lặp (Merge Ticket)
* **Tầng 1 (UI)**: `PlatformTicketsPage.tsx` (dòng 103), Admin mở modal gộp ticket, nhập `targetTicketId` và `reason`.
* **Tầng 2 (Controller)**: [`PlatformController.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L359) nhận tại `@PostMapping("/tickets/{id}/merge")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L101) dòng 101:
  ```java
  SupportTicketDetailResponse mergeTicket(Long sourceTicketId, MergeTicketRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1360-L1425):
  - **4 Chốt chặn an toàn**: Không tự gộp chính nó, bắt buộc **cùng `userId`** (`sourceTicket.getUser().equals(targetTicket.getUser())`), cả 2 chưa bị `CLOSED`.
  - Tạo 1 `TicketMessage` trong ticket đích ghi rõ: `[HỆ THỐNG - GỘP TICKET] Đã gộp nội dung từ Ticket #{sourceId}... Lý do: {reason}`.
  - Chuyển ticket nguồn thành `CLOSED`, gán `closedAt = now`.
  - Ghi Audit Log `MERGE_TICKET` và bắn thông báo cho khách hàng.
* **Tầng 5 (Repository Interface)**:
  - `TicketMessageRepository.java` ➡️ `save(mergeNotice)` vào ticket đích.
  - `SupportTicketRepository.java` ➡️ `save(sourceTicket)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `ticket_messages` (thêm tin nhắn gộp), bảng `support_tickets` (ticket nguồn đóng `CLOSED`).

---

### 1.7. 🌟 Chuyển Tiếp Sang Tranh Chấp Escrow (Redirect to Dispute)
* **Tầng 1 (UI)**: `PlatformTicketsPage.tsx` (dòng 112), Admin chọn "Chuyển tranh chấp".
* **Tầng 2 (Controller)**: [`PlatformController.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L371) nhận tại `@PostMapping("/tickets/{id}/redirect-dispute")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L107) dòng 107:
  ```java
  SupportTicketDetailResponse redirectTicketToDispute(Long ticketId, RedirectDisputeRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1524-L1580):
  1. Đổi `ticket.setCategory(DISPUTE)`, ép priority lên tối thiểu `HIGH`.
  2. Tạo tin nhắn thông báo trong luồng chat của Ticket.
  3. **Tích hợp liên phân hệ (Dòng 1571-1580)**: Tự động tạo bản ghi Báo cáo sự cố (`Report`):
     ```java
     Report report = new Report();
     report.setReporter(ticket.getUser());
     report.setTargetType(ReportTargetType.CLASS);
     report.setTargetId(tutoringClass.getClassId());
     report.setStatus(ReportStatus.PENDING);
     reportRepository.save(report);
     ```
     Báo cáo này hiển thị ngay trên màn hình `/platform/reports` để Admin can thiệp tạm khóa Escrow!
* **Tầng 5 (Repository Interface)**: `ReportRepository.java` ➡️ `save(report)`, `SupportTicketRepository.java` ➡️ `save(ticket)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `reports` (`target_type='CLASS'`, `status='PENDING'`), bảng `support_tickets`.

---

## CHƯƠNG 2: PHÂN HỆ ĐIỀU HÀNH SÀN & BẢO MẬT (BF-10)

### 2.1. Admin Dashboard KPI (UC-56)
* **Tầng 1 (UI)**: [PlatformDashboardPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformDashboardPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:158`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L158) nhận tại `@GetMapping("/dashboard")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:56`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L56):
  ```java
  DashboardResponse getDashboard(LocalDate from, LocalDate to, String granularity);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:398-505`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L398-L505):
  - `taskQueueService.getSummary()`: Lấy số ticket quá hạn, tranh chấp mở, báo cáo chưa duyệt.
  - `analyticsService.getSummary()`: Dòng tiền nạp/rút, tiền giữ trong Escrow, phí sàn.
  - Tổng hợp số lượng gia sư, trung tâm và lớp học đang hoạt động.
* **Tầng 5 (Repository Interface)**: `TutorRepository`, `TutorCenterRepository`, `TutoringClassRepository`, `EscrowTransactionRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `users`, `tutors`, `tutor_centers`, `tutoring_classes`, `escrow_transactions`, `support_tickets`.

---

### 2.2. Quản Trị User & Vô Hiệu Hóa Token Tức Thì (JWT Invalidation)
* **Tầng 1 (UI)**: [PlatformUsersPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformUsersPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:135`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L135) nhận tại `@PatchMapping("/users/{id}/status")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:49`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L49):
  ```java
  UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:300-323`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L300-L323):
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

---

### 2.3. Quy Trình Thẩm Định eKYC CCCD / Bằng Cấp (UC-11)
* **Tầng 1 (UI)**: [PlatformVerificationsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformVerificationsPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:199`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L199) nhận tại `@PostMapping("/verifications/{id}/review")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:62`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L62):
  ```java
  VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:542-613`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L542-L613):
  - *Chống sửa đồng thời*: So khớp `expectedUpdatedAt` với `verification.getUpdatedAt()`.
  - *Ép lý do từ chối 10 ký tự*: Nếu `REJECTED`, `adminNotes.length() >= 10`.
  - *Vô hiệu hóa bản ghi cũ*: `deactivatePreviousVerifiedRequests()` chuyển các CCCD cũ của người này sang hết hiệu lực.
  - *Đồng bộ trạng thái*: Cập nhật `tutor.setVerificationStatus(VERIFIED)` hoặc `tutorCenter.setVerificationStatus(VERIFIED)`.
* **Tầng 5 (Repository Interface)**: `VerificationRequestRepository`, `TutorRepository`, `TutorCenterRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `verification_requests`, `verification_histories`, `tutors`, `tutor_centers`.

---

### 2.4. Can Thiệp 7 Phương Án Sự Cố Lớp & Tự Động Khóa Escrow (UC-30 / UC-52)
* **Tầng 1 (UI)**: [PlatformReportsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformReportsPage.tsx).
* **Tầng 2 (Controller)**: [`PlatformController.java:271`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/controller/PlatformController.java#L271) nhận tại `@PatchMapping("/reports/{reportId}/resolve")`.
* **Tầng 3 (Service Interface)**: [`PlatformService.java:66`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/PlatformService.java#L66):
  ```java
  ReportResponse resolveClassIssue(Long reportId, ResolveClassIssueRequest request);
  ```
* **Tầng 4 (Service Impl)**: [`PlatformServiceImpl.java:829 & 1726`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformServiceImpl.java#L1726) (`resolveClassIssueReport`):
  - Áp dụng 7 hành động trong enum `ClassIssueResolutionAction`.
  - **Tự động khóa Escrow (Dòng 1738)**: Nếu chọn `ESCALATE_TO_DISPUTE` hoặc `TERMINATE_CLASS`:
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

---

### 2.5. Chế Tài Phạt Người Dùng & Chặn Tính Năng Liên Module (UC-60)
* **Tầng 1 (UI)**: [PlatformPenaltiesPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformPenaltiesPage.tsx).
* **Tầng 2 (Controller)**: `PlatformPenaltyController.java:45` nhận tại `@PostMapping("/penalties")`.
* **Tầng 3 (Service Interface)**: `PenaltyAccessService.java:25`:
  ```java
  void requireFeature(Long userId, String featureCode);
  ```
* **Tầng 4 (Service Impl)**: [`PenaltyAccessServiceImpl.java:45-53`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PenaltyAccessServiceImpl.java#L45-L53):
  Kiểm tra nếu người dùng có bản ghi phạt còn hiệu lực (`expires_at > now`), lập tức quăng `ForbiddenException`!
  **4 Điểm Gác Cổng Thực Tế**:
  - `FinanceServiceImpl.java:178`: `requireFeature(userId, "WITHDRAWAL")` (Chặn rút tiền).
  - `MarketplaceServiceImpl.java:369`: `requireFeature(creator.getUserId(), "CLASS_POSTING")` (Chặn đăng lớp).
  - `MarketplaceServiceImpl.java:514`: `requireFeature(tutorId, "CLASS_APPLICATION")` (Chặn nộp hồ sơ).
  - `ChatServiceImpl.java:394`: `requireFeature(userId, "MESSAGING")` (Chặn gửi tin nhắn).
* **Tầng 5 (Repository Interface)**: `UserPenaltyRepository.java:findByUser_UserIdAndStatus()`.
* **Tầng 6 (CSDL & Entity)**: Bảng `user_penalties`.

---

### 2.6. Phát Hiện Lách Sàn Bằng Regex 4 Tầng (UC-59)
* **Tầng 1 (UI)**: [PlatformCircumventionPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformCircumventionPage.tsx).
* **Tầng 2 (Controller)**: `CircumventionController.java:30` nhận tại `@GetMapping("/circumventions")`.
* **Tầng 3 (Service Interface)**: `CircumventionService.java`: `void inspect(Message message);`.
* **Tầng 4 (Service Impl)**: [`CircumventionServiceImpl.java:65-90`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/CircumventionServiceImpl.java#L65-L90):
  - **4 Bộ lọc Regex kèm điểm rủi ro**:
    1. `PHONE` (Điểm 80): `(?<!\d)(?:\+?84|0)(?:[ .-]?\d){9,10}(?!\d)`
    2. `EMAIL` (Điểm 90): `[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}`
    3. `URL` (Điểm 70): `(?i)(?:https?://|www\.)\S+`
    4. `SOCIAL` (Điểm 65): `(?i)(?:zalo|telegram|facebook|fb|instagram)\s*[:@-]?\s*[A-Z0-9_.-]{3,}`
* **Tầng 5 (Repository Interface)**: `CircumventionEventRepository.java` ➡️ `save(event)`.
* **Tầng 6 (CSDL & Entity)**: Bảng `circumvention_detections` (`risk_score`, `detected_pattern`).

---

### 2.7. Nhật Ký Kiểm Toán Bất Biến (Audit Logs - UC-61)
* **Tầng 1 (UI)**: [PlatformAuditLogsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformAuditLogsPage.tsx).
* **Tầng 2 (Controller)**: `AuditLogController.java:30` nhận tại `@GetMapping("/audit-logs")`.
* **Tầng 3 (Service Interface)**: `AuditLogService.java`: `void record(action, entityType, entityId, oldValue, newValue);`.
* **Tầng 4 (Service Impl)**: [`AuditLogServiceImpl.java:76-115`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/AuditLogServiceImpl.java#L76-L115):
  - Tuần tự hóa JSON Diff (`oldValue` vs `newValue`) qua Jackson `ObjectMapper`.
  - Tự động lấy Client IP và Header `User-Agent` từ Spring `RequestContextHolder`.
  - **Tính bất biến**: Bảng `audit_logs` chỉ có hàm ghi `record` (INSERT) và đọc `getAuditLogs` (SELECT), tuyệt đối không có UPDATE hay DELETE.
* **Tầng 5 (Repository Interface)**: `AuditLogRepository.java`.
* **Tầng 6 (CSDL & Entity)**: Bảng `audit_logs`.

---

### 2.8. Báo Cáo Tài Chính & Kỹ Thuật Xuất CSV An Toàn (UC-41, UC-43)
* **Tầng 1 (UI)**: [PlatformAnalyticsPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/platform/pages/PlatformAnalyticsPage.tsx).
* **Tầng 2 (Controller)**: `PlatformAnalyticsController.java:80` nhận tại `@GetMapping("/analytics/export-csv")`.
* **Tầng 3 (Service Interface)**: `PlatformAnalyticsService.java`: `byte[] exportCsv(String type, LocalDate from, LocalDate to);`.
* **Tầng 4 (Service Impl)**: [`PlatformAnalyticsServiceImpl.java:359 & 979`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/platform/service/impl/PlatformAnalyticsServiceImpl.java#L359):
  1. *Chống DDE Injection (`escapeCsv` - dòng 983)*: Nếu ô bắt đầu bằng `=`, `+`, `-`, `@`, tự động chèn dấu nháy đơn `'` ở đầu chuỗi.
  2. *Chèn UTF-8 BOM Header (`\uFEFF` - dòng 362)*: Giúp Microsoft Excel hiển thị đúng tiếng Việt có dấu.
  3. *Chống tràn RAM (OOM)*: Giới hạn tối đa `MAX_EXPORT_ROWS = 10_000` dòng.
* **Tầng 5 (Repository Interface)**: `PaymentTransactionRepository`, `EscrowTransactionRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `payment_transactions`, `escrow_transactions`.

---

## CHƯƠNG 3: TRỢ LÝ ẢO AI UNIVERSAL RAG 12 BƯỚC (UC-65)

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
* **Tầng 2 (Controller)**: [`ContractController.java:80`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/contract/controller/ContractController.java) nhận tại `@PostMapping("/{id}/sign-otp")`.
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
* **Tầng 2 (Controller)**: `ContractController.java:48` nhận tại `@PostMapping("/reviews")`.
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
* **Tầng 2 (Controller)**: `ProfileController.java:45` nhận tại `/api/profile`.
* **Tầng 3 (Service Interface)**: `ProfileService.java`: `uploadAvatar()`, `addCertificate()`, `addBusyTimes()`, `addChildProfile()`.
* **Tầng 4 (Service Impl)**: [`ProfileServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/profile/service/impl/ProfileServiceImpl.java):
  - *Tải avatar (Dòng 809)*: Kiểm tra MIME type, dung lượng < 5MB, ghi file `/uploads/`, lưu `avatar_url`.
  - *Onboarding (Dòng 179)*: Sau khi lưu hồ sơ lần đầu, gán `user.setProfileCompletedAt(now)` để ẩn banner onboarding vĩnh viễn.
  - *Lịch bận (Dòng 679)*: Kiểm tra đối soát chống trùng khung giờ rảnh/bận (`clashes`).
* **Tầng 5 (Repository Interface)**: `ClientRepository`, `TutorRepository`, `TutorCenterRepository`, `TutorCertificateRepository`, `TutorBusyTimeRepository`, `ChildProfileRepository`.
* **Tầng 6 (CSDL & Entity)**: Bảng `clients`, `tutors`, `tutor_centers`, `tutor_certificates`, `tutor_busy_times`, `child_profiles`.

---

### 4.4. Quên Mật Khẩu OTP Qua Email 2 Giai Đoạn (DEF-62)
* **Tầng 1 (UI)**: [ForgotPasswordPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/identity/pages/ForgotPasswordPage.tsx), [ResetPasswordPage.tsx](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/frontend/src/features/identity/pages/ResetPasswordPage.tsx).
* **Tầng 2 (Controller)**: `IdentityController.java:75` nhận tại `@PostMapping("/reset-password")`.
* **Tầng 3 (Service Interface)**: `IdentityService.java`: `requestPasswordResetOtp()`, `verifyPasswordResetOtp()`, `resetPassword()`.
* **Tầng 4 (Service Impl)**: [`IdentityServiceImpl.java:470-533`](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/backend/src/main/java/com/tcs/module/identity/service/impl/IdentityServiceImpl.java#L470-L533):
  1. *Giai đoạn 1*: Rate limit theo IP (`acquireIpSlot`), gửi OTP 6 số qua email. Khi verify thành công, sinh chuỗi Token ngẫu nhiên (Opaque Reset Token) có hạn 10 phút.
  2. *Giai đoạn 2*: Người dùng gửi `resetToken` kèm mật khẩu mới. Kiểm tra `token.getUsedAt() == null`, mã hóa BCrypt lưu vào bảng `users`, đánh dấu `token.setUsedAt(now)` (Token dùng 1 lần duy nhất).
* **Tầng 5 (Repository Interface)**: `PasswordResetTokenRepository.java`, `UserRepository.java`.
* **Tầng 6 (CSDL & Entity)**: Bảng `users` (cột `password_hash`), bảng `password_reset_tokens`, bảng `email_otps`.

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

---

*Cẩm nang này đã được cập nhật chuẩn 6 tầng kiến trúc trực tiếp tại [docs/CAM_NANG_ON_TAP_BAO_VE_DUCHM.md](file:///c:/Users/Admin/Documents/GitHub/Tutor-Connect-System-TCS-/docs/CAM_NANG_ON_TAP_BAO_VE_DUCHM.md).*
