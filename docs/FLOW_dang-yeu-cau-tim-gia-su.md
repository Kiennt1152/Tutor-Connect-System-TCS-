# Sơ đồ luồng code — Màn "Đăng yêu cầu tìm gia sư"

**Route:** `/dang-yeu-cau-tim-gia-su`
**Ai dùng:** CLIENT (phụ huynh / học viên)
**Kết quả:** tạo 1 tin tìm gia sư ở trạng thái **DRAFT (nháp)**

---

## 0. Bản đồ tệp

### Frontend

| Tệp | Vai trò |
|---|---|
| `frontend/src/shared/constants/routes.tsx:5` | Hằng đường dẫn `postTutorRequest` |
| `frontend/src/app/App.tsx:83` | Đăng ký `<Route>` |
| `frontend/src/features/home/pages/PostTutorRequestPage.tsx` | Khung trang, chặn quyền, màn thành công |
| `frontend/src/features/home/hooks/useTutorRequestForm.tsx` | Nạp danh mục + hàm gửi form |
| `frontend/src/features/marketplace/components/ClassRequestForm.tsx` | **Toàn bộ ô nhập + 3 tầng kiểm tra** (1212 dòng) |
| `frontend/src/features/center/components/LocationPicker.tsx` | Ô Tỉnh / Phường (gọi API ngoài) |
| `frontend/src/features/marketplace/mappers/marketplaceMapper.tsx` | Tính tiền + `formToPayload` / `classToForm` |
| `frontend/src/features/marketplace/types/marketplaceTypes.tsx` | Kiểu dữ liệu + hằng số |
| `frontend/src/features/marketplace/constants/catalogFallback.tsx` | Danh sách Lớp dự phòng |
| `frontend/src/features/marketplace/api/marketplaceApi.tsx` | Gọi HTTP |
| `frontend/src/shared/api/axiosClient.tsx` | Gắn Bearer token |

### Backend

| Tệp | Vai trò |
|---|---|
| `backend/src/main/java/com/tcs/config/CorsConfig.java` | Cho phép gọi từ `localhost:3000` |
| `backend/src/main/java/com/tcs/security/JwtAuthenticationFilter.java` | Xác thực token |
| `backend/src/main/java/com/tcs/config/SecurityConfig.java` | Phân quyền theo đường dẫn |
| `backend/src/main/java/com/tcs/config/MaintenanceModeInterceptor.java` | Chặn khi bảo trì |
| `backend/src/main/java/com/tcs/module/catalog/controller/CatalogController.java` | 3 API danh mục |
| `backend/src/main/java/com/tcs/module/marketplace/controller/MarketplaceController.java:71` | `POST /classes` |
| `backend/src/main/java/com/tcs/module/marketplace/service/impl/MarketplaceServiceImpl.java:349` | `createClass()` |
| `backend/src/main/java/com/tcs/module/marketplace/dto/request/CreateClassRequest.java` | DTO vào |
| `backend/src/main/java/com/tcs/module/marketplace/dto/response/ClassResponse.java` | DTO ra |
| `backend/src/main/java/com/tcs/module/marketplace/entity/TutoringClass.java` | Entity |
| `backend/src/main/java/com/tcs/exception/GlobalExceptionHandler.java` | Đổi exception → mã HTTP |

### Bảng CSDL

`tutoring_classes` (ghi) · `audit_logs` (ghi) · `subjects` · `grades` · `provinces` · `clients` · `users` · `user_penalties` · `system_parameters` (đọc)

---

## 0.5. Sơ đồ mở code — thứ tự đọc trong IDE

Mở lần lượt theo số. Trong IntelliJ dùng `Ctrl+Shift+N` gõ tên file, rồi
`Ctrl+G` nhảy tới số dòng.

### FRONTEND — `frontend ▸ src`

```
src
│
├── shared
│   ├── constants ▸ routes.tsx ...................... ① dòng 5   khai báo URL
│   └── api       ▸ axiosClient.tsx ................. ⑨ gắn Bearer token
│
├── app ▸ App.tsx ................................... ② dòng 83  <Route>
│
└── features
    │
    ├── home
    │   ├── pages ▸ PostTutorRequestPage.tsx ........ ③ KHUNG TRANG
    │   │                                              dòng 90  handleSubmit
    │   └── hooks ▸ useTutorRequestForm.tsx ......... ④ nạp danh mục + gửi
    │
    ├── marketplace
    │   ├── components ▸ ClassRequestForm.tsx ....... ⑤ ★ TRÁI TIM (1212 dòng)
    │   │                                              160 gradeMatchesSubjects
    │   │                                              241 visibleGrades
    │   │                                              255 handleLocationChange
    │   │                                              268 toggleSubject
    │   │                                              351 setSubjectFee
    │   │                                              363 toggleWeekday
    │   │                                              429 setSlotSession
    │   │                                              487 slotErrors
    │   │                                              516 conflicts
    │   │                                              533 missing
    │   │                                              555 handleSubmit
    │   │
    │   ├── mappers ▸ marketplaceMapper.tsx ......... ⑥ dòng 298 formToPayload
    │   │                                              dòng 230 totalBudget
    │   │                                              dòng  79 classToForm
    │   │
    │   ├── types     ▸ marketplaceTypes.tsx ........ ⑦ kiểu + hằng số
    │   ├── constants ▸ catalogFallback.tsx ......... ⑦ Lớp dự phòng
    │   └── api       ▸ marketplaceApi.tsx .......... ⑧ dòng 70 createClass
    │
    └── center ▸ components ▸ LocationPicker.tsx .... ⑩ API NGOÀI
                                                        provinces.open-api.vn
```

### BACKEND — `backend ▸ src ▸ main ▸ java ▸ com.tcs`

```
com.tcs
│
├── security
│   └── JwtAuthenticationFilter.java ................ ⑪ xác thực token
│                                                        doFilterInternal()
│
├── config
│   ├── CorsConfig.java ............................. cho phép localhost:3000
│   ├── SecurityConfig.java ......................... ⑫ dòng  62  catalog public
│   │                                                   dòng 171  apply → TUTOR
│   │                                                   dòng 177  classes → CLIENT
│   ├── MaintenanceModeInterceptor.java ............. ⑬ chặn khi bảo trì
│   └── RbacConstants.java .......................... tên các vai
│
├── module
│   ├── catalog
│   │   ├── controller ▸ CatalogController.java ..... ⑭ dòng 33/38/43
│   │   └── service ▸ impl ▸ CatalogServiceImpl.java     3 API danh mục
│   │
│   └── marketplace
│       ├── controller ▸ MarketplaceController.java   ⑮ dòng 71 POST /classes
│       │
│       ├── service ▸ impl ▸ MarketplaceServiceImpl.java  ⑯ ★ TRÁI TIM
│       │                                              349 createClass
│       │                                              392 applyRequest
│       │                                              413 resolveTitle
│       │                                              420 autoTitle
│       │                                              449 publishClass
│       │                                             4076 requireUser
│       │                                             4082 requireClient
│       │
│       ├── dto ▸ request  ▸ CreateClassRequest.java  ⑰ 16 trường vào
│       ├── dto ▸ response ▸ ClassResponse.java ..... ⑰ trả ra
│       ├── entity     ▸ TutoringClass.java ......... ⑱ ánh xạ bảng
│       └── repository ▸ TutoringClassRepository.java    truy vấn
│
└── exception
    └── GlobalExceptionHandler.java ................. ⑲ exception → mã HTTP
```

### Cây file BACKEND đầy đủ — 37 file, theo thứ tự request đi qua

```
backend ▸ src ▸ main ▸ java ▸ com.tcs
│
├── config ................................. [1] HẠ TẦNG — mọi request đi qua
│   ├── CorsConfig.java ....................  cho localhost:3000 gọi sang 8080
│   ├── SecurityConfig.java ★ ..............  62  GET /catalog/**  = permitAll
│   │                                         171 POST apply       = TUTOR
│   │                                         177 POST classes/**  = CLIENT
│   ├── RbacConstants.java .................  hằng tên vai
│   ├── MaintenanceModeInterceptor.java ....  chặn POST khi bảo trì → 503
│   └── WebMvcConfig.java ..................  đăng ký interceptor
│
├── security .............................. [2] DANH TÍNH
│   ├── JwtAuthenticationFilter.java ★ .....  đọc Bearer → SecurityContext
│   ├── JwtService.java ....................  giải mã token, tokenVersion
│   ├── CustomUserDetailsService.java ★ ....  29  ⚠ 5 TRUY VẤN
│   │                                              users + platform_admins
│   │                                              + tutors + tutor_centers
│   │                                              + clients  → suy ra vai
│   ├── UserPrincipal.java .................  bọc User thành UserDetails
│   └── AuthHelper.java ....................  currentUserId()
│
├── exception ............................. [3] LỖI
│   ├── GlobalExceptionHandler.java ★ ......  exception → mã HTTP
│   ├── ForbiddenException.java ............  → 403
│   └── ResourceNotFoundException.java .....  → 404
│
└── module
    │
    ├── catalog ........................... [4] 3 API LÚC MỞ TRANG
    │   ├── controller ▸ CatalogController.java ★  33 /subjects
    │   │                                          38 /grades
    │   │                                          43 /provinces
    │   ├── service ▸ CatalogService.java ......  interface
    │   ├── service ▸ impl ▸ CatalogServiceImpl.java ★  113 findAll()
    │   ├── dto ▸ response ▸ CatalogItemResponse.java   {id,name,description}
    │   ├── entity ▸ Subject.java ..............  bảng subjects
    │   ├── entity ▸ Grade.java ................  bảng grades
    │   ├── repository ▸ SubjectRepository.java
    │   ├── repository ▸ GradeRepository.java
    │   ├── repository ▸ ProvinceRepository.java
    │   └── repository ▸ SystemParameterRepository.java  ← interceptor dùng
    │
    ├── marketplace ....................... [5] API TẠO TIN
    │   ├── controller ▸ MarketplaceController.java ★  71 POST /classes
    │   ├── service ▸ MarketplaceService.java ..  interface
    │   ├── service ▸ impl ▸ MarketplaceServiceImpl.java ★★ TRÁI TIM
    │   │                                          349  createClass
    │   │                                          392  applyRequest
    │   │                                          413  resolveTitle
    │   │                                          420  autoTitle
    │   │                                          449  publishClass
    │   │                                         4076  requireUser
    │   │                                         4082  requireClient
    │   │                                         4110  resolveSubject
    │   ├── dto ▸ request  ▸ CreateClassRequest.java   16 trường vào
    │   ├── dto ▸ response ▸ ClassResponse.java ....   trả ra, có expiresAt
    │   ├── entity     ▸ TutoringClass.java ★ ......   bảng tutoring_classes
    │   ├── repository ▸ TutoringClassRepository.java  save() → INSERT
    │   └── enums ▸ TutoringClassStatus.java .......   DRAFT OPEN MATCHED...
    │             ▸ LessonMode.java ................   OFFLINE / ONLINE
    │             ▸ RecurringType.java .............   WEEKLY
    │
    ├── identity .......................... [6] TÀI KHOẢN
    │   ├── entity     ▸ User.java ..............  bảng users + token_version
    │   └── repository ▸ UserRepository.java ...   findByEmail / findById
    │
    ├── profile ........................... [7] HỒ SƠ KHÁCH
    │   ├── entity     ▸ Client.java ............  bảng clients
    │   └── repository ▸ ClientRepository.java ★   findByUser_UserId — chốt ③
    │
    └── platform .......................... [8] PHẠT + NHẬT KÝ
        ├── service ▸ PenaltyAccessService.java .  interface
        ├── service ▸ impl ▸ PenaltyAccessServiceImpl.java ★
        │                                          requireFeature("CLASS_POSTING")
        ├── repository ▸ UserPenaltyRepository.java
        ├── service ▸ AuditLogService.java ......  interface
        ├── service ▸ impl ▸ AuditLogServiceImpl.java ★  60 record()
        ├── repository ▸ AuditLogRepository.java
        └── entity     ▸ AuditLog.java ..........  bảng audit_logs
```

### Số truy vấn từng chặng

```
CorsConfig                    0
JwtAuthenticationFilter       5    ⚠ users + 4 bảng vai
SecurityConfig                0      chỉ so chuỗi URL
MaintenanceModeInterceptor    1      system_parameters
MarketplaceController         0
MarketplaceServiceImpl   @Transactional ─────────────────
   requireUser()              1      users
   requireFeature()           1      user_penalties (lọc tiếp bằng Java)
   requireClient()            1      clients
   kiểm môn học               0
   applyRequest()             2      subjects + grades
   status = DRAFT             0      ép cứng
   save()                            INSERT tutoring_classes
   auditLog()                 0–1    INSERT audit_logs
──────────────────────────────────────────────────────────
TỔNG                       ~12 SELECT  +  2 INSERT
```

### Nếu chỉ có 15 phút — đọc 8 file này

```
①  MarketplaceServiceImpl.java:349     ★★ toàn bộ luật nghiệp vụ
②  SecurityConfig.java:177             ★  ai gọi được API nào
③  JwtAuthenticationFilter.java        ★  danh tính từ đâu ra
④  CustomUserDetailsService.java:29    ★  5 truy vấn + cách suy ra vai
⑤  MarketplaceController.java:71          cửa vào, đúng 1 dòng
⑥  CatalogController.java:33/38/43        3 API danh mục
⑦  CreateClassRequest.java                hình dạng JSON nhận vào
⑧  GlobalExceptionHandler.java            lỗi ra sao
```

### Đường đi của một lần bấm nút

```
FRONTEND                                    BACKEND
────────────────────────────────────────────────────────────────────
ClassRequestForm.tsx:555                    JwtAuthenticationFilter
   handleSubmit()                              doFilterInternal()
        │                                            │
        ▼                                            ▼
marketplaceMapper.tsx:298                   SecurityConfig:177
   formToPayload()                             hasRole(CLIENT)
        │                                            │
        ▼                                            ▼
PostTutorRequestPage.tsx:90                 MaintenanceModeInterceptor
   handleSubmit()                              preHandle()
        │                                            │
        ▼                                            ▼
useTutorRequestForm.tsx                     MarketplaceController:71
   createRequest()                             createClass()
        │                                            │
        ▼                                            ▼
marketplaceApi.tsx:70                       MarketplaceServiceImpl:349
   createClass()                               createClass()
        │                                            │  ├─ requireUser()      :4076
        ▼                                            │  ├─ requireFeature()
axiosClient.tsx                                      │  ├─ requireClient()    :4082
   interceptors.request                              │  ├─ applyRequest()     :392
   + Bearer token                                    │  │     └─ autoTitle()  :420
        │                                            │  ├─ status = DRAFT
        └──────── POST /api/marketplace/classes ─────┤  ├─ save()   → tutoring_classes
                                                     │  └─ audit()  → audit_logs
        ┌──────── 201 Created + ClassResponse ───────┘
        ▼
PostTutorRequestPage
   setCreated() → màn ✓
```

### Hai file cần đọc kỹ nhất

| | File | Vì sao |
|---|---|---|
| ★ | `ClassRequestForm.tsx` | Chứa **toàn bộ** logic form: đồng bộ state, lọc Lớp, né trùng giờ, 3 tầng kiểm tra |
| ★ | `MarketplaceServiceImpl.java` | Chứa **toàn bộ** luật nghiệp vụ: 4 chốt chặn, tự sinh tiêu đề, ép DRAFT |

Các file còn lại đều mỏng — phần lớn chỉ vài chục dòng nối dây.

---

## 1. Toàn cảnh

```
┌──────────────────────── TRÌNH DUYỆT ────────────────────────┐
│  routes.tsx ─ App.tsx ─ PostTutorRequestPage                 │
│        │                                                     │
│        ├── useTutorRequestForm  ──┐                          │
│        └── ClassRequestForm       │                          │
│              └── LocationPicker ──┼── API ngoài              │
│              └── marketplaceMapper│   provinces.open-api.vn  │
└───────────────────────────────────┼──────────────────────────┘
                                    │  axiosClient (+ Bearer)
┌───────────────────────────────────▼──────────────────────────┐
│  CorsConfig → JwtFilter → SecurityConfig → Interceptor       │
│       │                                                      │
│       ├─ CatalogController      (GET, public)                │
│       └─ MarketplaceController  (POST, role CLIENT)          │
│                └── MarketplaceServiceImpl  @Transactional    │
└───────────────────────────────────┬──────────────────────────┘
                                    ▼
                    MySQL: tutoring_classes + audit_logs
```

---

## 2. GIAI ĐOẠN 1 — Mở trang

```
URL /dang-yeu-cau-tim-gia-su
  └─ routes.tsx:5              hằng đường dẫn
  └─ App.tsx:83                <Route> → <PostTutorRequestPage />
       ├─ useAuth()            quyết định hiện banner nào
       ├─ useTutorRequestForm()
       │     └─ useEffect  ──►  3 request SONG SONG
       └─ <ClassRequestForm />
```

| Gọi | Endpoint | Đổ vào |
|---|---|---|
| `listSubjects()` | `GET /api/catalog/subjects` | 12 checkbox Môn học |
| `listGrades()` | `GET /api/catalog/grades` | dropdown Lớp |
| `listProvinces()` | `GET /api/catalog/provinces` | (màn này không dùng) |

Mỗi cái có `.catch(() => setX([]))` → một API chết thì chỉ ô đó trống, trang vẫn mở.

### Backend cho 3 API này

```
Request
  → CorsConfig                 localhost:3000 ≠ localhost:8080, phải cho phép
  → JwtAuthenticationFilter    KHÔNG có header Bearer → cho đi tiếp luôn
  → SecurityConfig:62          GET /api/catalog/** = permitAll
  → MaintenanceModeInterceptor thoát ngay vì là GET
  → CatalogController:33/38/43
  → CatalogServiceImpl → Repository → bảng subjects / grades / provinces
  → 200 OK + JSON
```

> **Ô Tỉnh/Phường KHÔNG đụng backend TCS.** `LocationPicker` gọi thẳng
> `https://provinces.open-api.vn/api/v2`. Mất mạng ngoài là hai ô này trống
> dù backend vẫn chạy bình thường.

---

## 3. GIAI ĐOẠN 2 — Người dùng điền form

Tất cả ở `ClassRequestForm.tsx`. Toàn bộ state gói trong **một object**
`ClassFormValues`, mọi hàm đều `setForm(prev => ...)`.

### 3.1 Checkbox Môn học → `toggleSubject()` (dòng 268)

Bấm 1 ô kéo theo 4 việc:

```
toggleSubject(id)
  ├─ thêm/bớt id trong  subjectIds
  ├─ thêm/xóa ô tiền   subjectFees[id]
  ├─ thêm/xóa buổi học slots (lọc theo subjectId)
  └─ XÓA Lớp đang chọn nếu không còn hợp (xem 3.2)
```

Ô **"Khác"** → `addOtherSubject()` sinh id dạng `other:xxx`, tên môn lưu ở
`subjectOthers`. Trong DB sẽ thấy:

```json
"subjectIds":    ["other:vo"],
"subjectOthers": {"other:vo": "Võ thuật"}
```

### 3.2 Dropdown Lớp lọc theo môn → `visibleGrades` (dòng 241)

`gradeMatchesSubjects()` (dòng 160):

| Chọn môn | "Luyện thi chứng chỉ" | "Luyện thi Đại học" |
|---|---|---|
| Toán | ẩn | hiện |
| Tiếng Anh | hiện | hiện |
| Tin học | ẩn | ẩn |
| Có môn "Khác" | hiện (bỏ lọc) | hiện (bỏ lọc) |

`gradeOptions` trộn thêm `FALLBACK_GRADES` rồi khử trùng theo tên → API catalog
chết vẫn chọn được Lớp 1–12.

### 3.3 Offline / Online

`isOffline = form.lessonMode !== 'ONLINE'`
Chọn Online → cụm Tỉnh/Phường/Địa chỉ biến mất **và** 3 trường đó bị loại khỏi
danh sách bắt buộc.

### 3.4 Tỉnh / Phường → `handleLocationChange()` (dòng 255)

Lưu **tên**, cố tình xóa trắng `provinceId` / `wardId`:

```js
provinceName: v.province,   provinceId: '',
wardName:     v.ward,       wardId:     '',
address:      v.addressDetail
```

→ Đây là lý do `tutorMatching.tsx` phải chuẩn hóa chuỗi khi so địa điểm
("Thành phố Hà Nội" = "TP Hà Nội" = "Hà Nội").

### 3.5 Học phí từng môn → `setSubjectFee()` (dòng 351)

`value.replace(/\D/g, '')` — lọc sạch ký tự không phải số ngay khi gõ.
Kèm `blockNonDigits` chặn paste chữ.

### 3.6 Lịch học — phần phức tạp nhất

Đổi chế độ → `setScheduleMode()` **vứt bỏ buổi không hợp lệ**
(buổi có `day` khi chuyển sang chế độ ngày cụ thể, và ngược lại).

**`toggleWeekday()` (dòng 363)** — bấm "T2" cho môn Toán:

```
1. Mặc định 06:00–08:00, hoặc mượn khung của buổi đã có cùng môn
2. Nếu ĐÈ GIỜ với buổi môn khác cùng thứ:
      → dò khung trống gần nhất trong 06:00–23:30
      → giữ nguyên độ dài buổi
3. Không còn chỗ trống → không thêm gì cả
```

**`setSlotSession()` (dòng 429)** — chọn Sáng/Chiều/Tối:

```
blocked = khung mặc định của buổi đã bị buổi khác chiếm
past    = khung đó đã trôi qua trong ngày hôm nay
blocked || past  →  chỉ đặt tên buổi, để giờ TRỐNG
ngược lại        →  điền sẵn giờ mặc định
```

`sessionFromStart()` suy ngược tên buổi từ giờ bắt đầu:
`< 12:00` Sáng · `< 18:00` Chiều · còn lại Tối

### 3.7 Tính tiền tự động → `marketplaceMapper.tsx`

```
totalBudget (dòng 230)
  = Σ  (học phí/giờ môn i) × (số giờ/chu kỳ môn i) × patternRepeats
```

- `patternRepeats()` (dòng 196)
  - Lịch tuần: `số tháng × 4`, trừ tuần nghỉ nếu chọn "học 2 tuần nghỉ 2 tuần"
  - Chọn ngày cụ thể: trả về `1` (mỗi buổi chỉ diễn ra đúng một lần)
- `hoursPerRepeatForSubject()` — cộng độ dài các buổi của riêng môn đó

Chạy trong `useMemo([form])` → đổi 1 con số là tổng tiền cập nhật ngay.

---

## 4. GIAI ĐOẠN 3 — Bấm "Đăng yêu cầu": 3 tầng kiểm tra

`handleSubmit()` (dòng 555):

```
setTouched(true)                 ← từ đây mới bôi đỏ; trước đó form im lặng
if (missing.length > 0)      return
if (slotErrors.length > 0)   return
if (conflicts.length > 0)    return
onSubmit(formToPayload(form, subjects))
```

| Biến | Dòng | Bắt lỗi gì |
|---|---|---|
| `missing` | 533 | Thiếu Môn / Tên môn khác / Lớp / Tỉnh / Phường / Địa chỉ / Lịch học |
| `slotErrors` | 487 | Buổi thiếu thông tin · ngày quá khứ · giờ hôm nay đã qua · giờ kết thúc ≤ giờ bắt đầu · môn chưa có buổi · **chưa nhập học phí** · **học phí < 50.000đ** (`FEE_PER_HOUR_MIN`) |
| `conflicts` | 516 | Hai buổi cùng thứ/ngày đè giờ nhau — quét đôi một `O(n²)` |

---

## 5. GIAI ĐOẠN 4 — `formToPayload()` (mapper dòng 298): làm phẳng

Form có hàng chục trường, bảng `tutoring_classes` chỉ có vài cột:

| Cột DB | Suy ra từ đâu |
|---|---|
| `title` | Ghép tên môn → *"Cần tìm gia sư môn Toán, Vật lý…"* |
| `description` | Dòng môn + `buildScheduleSummary()` + ghi chú |
| `startDate` / `endDate` | Tuần: hôm nay → hôm nay + `weeksForCycle × 7`<br>Ngày cụ thể: ngày sớm nhất → muộn nhất |
| `tuitionFee` | ⚠️ **Học phí môn ĐẦU TIÊN**, không phải tổng |
| `budget` | `totalBudget()` — tổng cả khóa |
| `numberOfSessions` | `estimatedSessions()` |
| `subjectId` | Môn danh mục đầu tiên (bỏ qua môn `other:`) |
| **`detailsJson`** | **Toàn bộ form nén thành JSON** |

`detailsJson` chứa: `subjectIds`, `subjectOthers`, `subjectFees`, `slots`,
`learningGoal`, `provinceName`, `wardName`, `studyWeeks`…
Nhờ nó mà `classToForm()` (dòng 79) dựng ngược về form được khi mở sửa.

---

## 6. GIAI ĐOẠN 5 — Gửi đi

```
PostTutorRequestPage.handleSubmit (dòng 90)
  └─ chặn nếu user?.role !== 'CLIENT'
  └─ createRequest(payload)
       └─ marketplaceApi.createClass()          [marketplaceApi.tsx:70]
            └─ axiosClient.post('/marketplace/classes', payload)
                 └─ interceptor gắn Authorization: Bearer <token>
```

---

## 7. GIAI ĐOẠN 6 — Backend xử lý `POST /api/marketplace/classes`

### 7.1 `JwtAuthenticationFilter`

```
Đọc header Authorization: Bearer ...
  ├─ không có     → đi tiếp, vô danh (sẽ bị chặn ở bước sau)
  └─ có
       parseClaims() → email → loadUserByUsername() → nạp user từ DB
       kiểm 2 điều:
         • tài khoản còn isEnabled()
         • tokenVersion trong token == users.token_version trong DB
              ↑ đổi mật khẩu thì tăng cột này → mọi token cũ tự chết
       OK → nhét Authentication vào SecurityContextHolder
       catch (Exception ignored)  ← token hỏng thì im lặng đi tiếp
```

### 7.2 `SecurityConfig` — chặn theo vai

```java
.requestMatchers(HttpMethod.POST, "/api/marketplace/classes/**")
.hasRole(RbacConstants.CLIENT)                          // dòng 177
```

⚠️ **Thứ tự khai báo quyết định tất cả** — Spring lấy luật KHỚP ĐẦU TIÊN:

```
dòng 171   POST /classes/*/apply     → hasRole(TUTOR)
dòng 175   POST /classes/*/complete  → hasRole(TUTOR)
dòng 177   POST /classes/**          → hasRole(CLIENT)   ← bắt phần còn lại
```

Đảo ngược thì `/classes/1/apply` rơi vào luật CLIENT, gia sư hết ứng tuyển được.

Cả app chạy `STATELESS` (dòng 33): không session, không cookie, mỗi request tự
mang token. `csrf` tắt vì không dùng cookie thì không có nguy cơ CSRF.

Sai vai → **403**, chết tại đây, chưa vào controller.

### 7.3 `MaintenanceModeInterceptor`

```
GET / OPTIONS / HEAD          → cho qua ngay
/api/catalog/ /api/auth/ ...  → ALLOWED_PREFIXES, cho qua
                                 (để admin còn đăng nhập tắt bảo trì)
còn lại → tra system_parameters['MAINTENANCE_MODE']
            true → 503 cho mọi người trừ admin
```

### 7.4 Controller — `MarketplaceController.java:71`

```java
@PostMapping("/classes")
@ResponseStatus(HttpStatus.CREATED)          // trả 201, không phải 200
public ClassResponse createClass(@RequestBody CreateClassRequest request) {
    return marketplaceService.createClass(request);
}
```

Jackson map JSON → `CreateClassRequest` (16 trường).
DTO này **không có annotation validate nào** — toàn bộ kiểm tra dồn xuống service.

### 7.5 Service — `MarketplaceServiceImpl.createClass()` dòng 349

`@Transactional` → ném exception ở bất kỳ đâu thì **rollback sạch**.

**Bốn chốt chặn, đúng thứ tự:**

| # | Lệnh | Làm gì | Lỗi → HTTP |
|---|---|---|---|
| 1 | `requireUser()` | Đọc user id từ `SecurityContextHolder` → tra bảng `users` | 404 |
| 2 | `requireFeature(userId, "CLASS_POSTING")` | Quét `user_penalties` còn ACTIVE, loại `FEATURE_RESTRICTION`, chưa hết hạn, xem `restriction_details` có chứa `"CLASS_POSTING"` | 403 |
| 3 | `requireClient(userId)` | Tra bảng `clients` xem có hồ sơ khách hàng chưa | 403 |
| 4 | Kiểm môn học | Phải có `subjectId` **hoặc** `detailsJson` | 400 |

> **Vì sao có cả 7.2 lẫn chốt 3 khi cả hai đều kiểm CLIENT?**
> 7.2 kiểm **vai trong token** (nhanh, chặn sớm).
> Chốt 3 kiểm **có dòng trong bảng `clients` thật hay không**.
> Token có thể đúng vai nhưng hồ sơ client chưa được tạo.

> **Chốt 2 là tầng phạt** — admin cấm một tài khoản đăng tin mà không cần khóa
> cả tài khoản.

### 7.6 `applyRequest()` dòng 392 — DTO → Entity

Backend **không tin dữ liệu client gửi lên**, tự tra lại từ DB:

```java
resolveSubject(request.getSubjectId())    // id → entity Subject
resolveGrade(request.getGradeId())        // id → entity Grade
resolveLocation(request.getLocationId())
resolveCategory(request.getCategoryId())
```

Id `null` → trả `null` (cột `nullable`, bỏ qua). Id **có giá trị nhưng không tồn tại**
trong DB → ném `ResourceNotFoundException` → **404**.

**Backend tự sinh 2 trường:**

- `resolveTitle()` (dòng 413) → thiếu title thì `autoTitle()` (dòng 420) đọc
  `detailsJson`, ghép `"Cần tìm gia sư môn " + tên các môn`, cắt còn **150 ký
  tự** + `…` cho khớp `varchar(150)`
- `resolveDescription()` → tương tự

`detailsJson` thì **lưu nguyên xi**, không parse, không kiểm.

Các trường `if (x != null)` — `null` thì giữ mặc định của cột, không ghi đè.

### 7.7 Ép trạng thái DRAFT — chỗ quan trọng nhất

```java
tutoringClass.setStatus(TutoringClassStatus.DRAFT);
```

**Cố ý bỏ qua mọi trạng thái client gửi lên.** Gọi API tay và nhét
`"status":"OPEN"` vào body cũng vô ích.

Hệ quả: `listClasses(OPEN)` không trả về → gia sư ở màn
`/tim-yeu-cau-giang-day` **không thấy**. Muốn công khai phải gọi tiếp:

```
POST /marketplace/classes/{id}/publish  →  publishClass()  [dòng 449]
     status     = OPEN
     expires_at = now + 30 ngày     ← đồng hồ "Còn 29d..." sinh từ đây
```

### 7.8 Ghi DB

```java
TutoringClass saved = tutoringClassRepository.save(tutoringClass);   // INSERT
auditLogService.record(userId, "CREATE_CLASS", "TutoringClass",
                       saved.getClassId(), null, request);           // INSERT
```

`audit_logs` lưu **nguyên vẹn request** để truy vết khi có tranh chấp.
Hai lệnh cùng transaction: `audit_logs` lỗi thì `tutoring_classes` cũng rollback.

### 7.9 Trả về

`toClassResponse(saved)` → `ClassResponse` → JSON → **201 Created**

---

## 8. Xử lý lỗi tập trung

`GlobalExceptionHandler` (`@RestControllerAdvice`) bắt hết → service **không cần
try/catch**, cứ ném exception đúng loại:

| Exception | HTTP | Ném ở đâu trong luồng này |
|---|---|---|
| `IllegalArgumentException` | 400 | Chốt 4 — chưa chọn môn học |
| `UnauthorizedException` | 401 | — |
| `ForbiddenException` | 403 | Chốt 2 (bị phạt) · Chốt 3 (không phải CLIENT) |
| `ResourceNotFoundException` | 404 | Chốt 1 — không tìm thấy user |
| `BusinessException` | 409 | — |

Body luôn cùng hình dạng `{ "message": "..." }` → frontend chỉ cần một hàm
`extractError()` duy nhất đọc `err.response.data.message`.

---

## 9. GIAI ĐOẠN 7 — Sau khi lưu

```
created được set
  → form biến mất
  → hiện màn ✓ "Đã gửi yêu cầu tìm gia sư!"
     kèm đúng câu "đang ở trạng thái nháp"
  → 2 nút: [Quản lý yêu cầu]  [Đăng yêu cầu khác]
```

---

## 10. Tóm tắt một trang

```
① GET /api/catalog/subjects|grades|provinces        (public, lúc mở trang)
     JwtFilter(bỏ qua) → permitAll → Controller → Repository

② Người dùng điền form  (ClassRequestForm)
     toggleSubject       đồng bộ 4 mảng state
     visibleGrades       lọc Lớp theo môn đã chọn
     LocationPicker      API NGOÀI provinces.open-api.vn
     toggleWeekday       tự né khung giờ đã bị chiếm
     totalBudget         tính lại theo mỗi phím gõ

③ Bấm nút → 3 tầng kiểm tra: missing / slotErrors / conflicts
   → formToPayload() làm phẳng, nén detailsJson

④ POST /api/marketplace/classes
     JwtFilter          xác thực token + tokenVersion
     SecurityConfig     hasRole(CLIENT)                    → 403
     Interceptor        chặn nếu MAINTENANCE_MODE          → 503
     Controller         JSON → CreateClassRequest
     Service @Transactional
         ├ requireUser()                                   → 404
         ├ requireFeature("CLASS_POSTING")                 → 403
         ├ requireClient()                                 → 403
         ├ kiểm môn học                                    → 400
         ├ applyRequest()   tra lại entity + tự sinh title
         ├ status = DRAFT   (ép cứng)
         ├ save()           INSERT tutoring_classes
         └ auditLog()       INSERT audit_logs
     201 Created + ClassResponse

⑤ Hiện màn thành công — tin vẫn là NHÁP, gia sư chưa thấy
```

---

## 11. Ba điều dễ hiểu nhầm nhất

1. **Ô Tỉnh/Phường không phải backend mình** — gọi API công cộng bên ngoài
   `provinces.open-api.vn`.
2. **Tin luôn là nháp** — backend ép cứng `DRAFT`, client không can thiệp được.
   Phải bấm "Đăng lớp" ở màn khác mới thành `OPEN`.
3. **Thứ tự luật trong `SecurityConfig` quyết định tất cả** — luật cụ thể phải
   đứng TRÊN luật `/**`.

---
---

---
---

# PHỤ LỤC — Đi qua CODE THẬT, 12 bước

Phần trên mô tả luồng. Phần này dán code thật của từng bước, theo đúng thứ tự
máy chạy.

---

## ① Bấm nút "Tạo lớp"

`ClassRequestForm.tsx:1202`

```tsx
<button type="button" className="mkt-btn mkt-btn--primary" onClick={handleSubmit}>
```

Nút **luôn `type="button"`** chứ không phải `submit` → trang không bao giờ reload.

---

## ② Ba tầng kiểm tra

`ClassRequestForm.tsx:555`

```tsx
function handleSubmit() {
  setTouched(true);
  if (missing.length > 0 || slotErrors.length > 0 || conflicts.length > 0) return;
  onSubmit(formToPayload(form, subjects));
}
```

Ba mảng này **không phải state** — tính lại mỗi lần render.

### slotErrors — `ClassRequestForm.tsx:487`

```tsx
const slotErrorSet = new Set<string>();          // Set để không in trùng thông báo
form.slots.forEach((s) => {
  const nm = subjName(s.subjectId);
  const whenMissing = isWeekly ? !s.day : !s.date;
  if (whenMissing || !s.session || !s.start || !s.end) {
    slotErrorSet.add(`${nm}: có buổi chưa đủ thông tin (...)`);
  } else if (!isWeekly && s.date < today) {
    slotErrorSet.add(`${nm}: ngày học không được ở quá khứ`);
  } else if (!isWeekly && s.date === today && s.start <= nowHm) {
    slotErrorSet.add(`${nm}: giờ học hôm nay đã qua (phải sau ${nowHm})`);
  } else if (s.end <= s.start) {
    slotErrorSet.add(`${nm}: giờ kết thúc phải sau giờ bắt đầu`);
  }
});
```

> **Mẹo trong code**: giờ là chuỗi `"06:00"` nên so sánh thẳng `s.end <= s.start`
> được luôn — không cần đổi ra số phút. Nhờ định dạng `HH:mm` có `padStart(2,'0')`.

### conflicts — `ClassRequestForm.tsx:516`

```tsx
for (let i = 0; i < form.slots.length; i++) {
  for (let j = i + 1; j < form.slots.length; j++) {
    const a = form.slots[i], b = form.slots[j];
    const sameWhen = isWeekly ? !!a.day && a.day === b.day
                              : !!a.date && a.date === b.date;
    if (sameWhen && a.start < b.end && b.start < a.end) {   // ← công thức đè khoảng
      conflicts.push(`Trùng giờ ${when}: ...`);
    }
  }
}
```

> `a.start < b.end && b.start < a.end` là **công thức chuẩn kiểm tra 2 khoảng
> giao nhau**. Nhớ cái này, dùng lại được ở mọi chỗ có lịch.

---

## ③ `formToPayload()` làm phẳng form

`marketplaceMapper.tsx:298` — hình dạng cuối cùng gửi lên server:

```ts
return {
  subjectId:   primarySubjectId ? Number(primarySubjectId) : null,
  gradeId:     form.gradeId ? Number(form.gradeId) : null,
  learningGoal:     resolveLearningGoal(form) || null,
  tutorRequirement: resolveTutorRequirement(form) || null,
  locationId:  null,                    // ← luôn null, địa chỉ gộp vào chuỗi
  address:     fullAddress || null,     // "123 Lê Lợi, Phường X, Thành phố Hà Nội"
  lessonMode:  form.lessonMode,
  numberOfSessions: sessions,
  startDate, endDate,
  tuitionFee:  primaryFee,              // ⚠️ học phí MÔN ĐẦU TIÊN
  budget,                               // tổng cả khóa
  recurringType: 'WEEKLY',              // ⚠️ hardcode
  description: description || undefined,
  detailsJson: JSON.stringify(form),    // ⚠️ NGUYÊN object form
};
```

**Ba chỗ đáng nhớ nhất trong toàn bộ luồng:**

| Dòng | Ý nghĩa |
|---|---|
| `detailsJson: JSON.stringify(form)` | Không lọc gì cả — **cả object form** thành chuỗi. Đây là lý do trong DB có `subjectFees`, `slots`, `studyWeeks`… |
| `tuitionFee: primaryFee` | Chỉ là `Number(form.subjectFees[form.subjectIds[0]])`. Lớp 5 môn thì cột này **không đại diện** cho lớp |
| `locationId: null` | Không dùng bảng `locations`, địa chỉ chỉ là chuỗi ghép |

Còn `title` thì **frontend không gửi** — để backend tự sinh (bước ⑩).

---

## ④ Trang cha chặn quyền

`PostTutorRequestPage.tsx:90`

```tsx
async function handleSubmit(payload: ClassRequestPayload) {
  if (!isClient) {                        // ← chặn ngay ở client
    setError('Vui lòng đăng nhập bằng tài khoản Client...');
    return;
  }
  setSubmitting(true);                    // ← khóa nút, tránh bấm 2 lần
  setError(null);
  try {
    const result = await createRequest(payload);
    setCreated(result);                   // ← có kết quả thì đổi sang màn ✓
  } catch (err) {
    setError(extractError(err));
  } finally {
    setSubmitting(false);                 // ← finally: lỗi hay không cũng mở khóa nút
  }
}
```

---

## ⑤ Axios gắn token

`axiosClient.tsx` — interceptor chạy trước **mọi** request:

```ts
axiosClient.interceptors.request.use((config) => {
  const token = authStorage.getToken();
  if (token && !isAuthEndpoint(requestUrl) && authStorage.isSessionExpired()) {
    redirectToExpiredSession();                        // ← đá về /login
    return Promise.reject(new Error('Phiên đăng nhập đã hết hạn.'));
  }
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

Request thật bay đi:

```http
POST http://localhost:8080/api/marketplace/classes
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{"subjectId":1,"gradeId":9,"tuitionFee":200000,"budget":6400000,
 "detailsJson":"{\"subjectIds\":[\"1\"],\"subjectFees\":{\"1\":\"200000\"},...}"}
```

---

# ▼ TỪ ĐÂY LÀ BACKEND ▼

## ⑥ `JwtAuthenticationFilter` dựng danh tính

```java
String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);     // ← không có token: đi tiếp, vô danh
    return;
}
String token = authHeader.substring(7);
try {
    var claims = jwtService.parseClaims(token);
    String email = claims.get("email", String.class);
    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
    if (!userDetails.isEnabled()
            || principal.getTokenVersion() != jwtService.extractTokenVersion(claims)) {
        filterChain.doFilter(request, response);  // ← token cũ: bỏ qua
        return;
    }
    SecurityContextHolder.getContext().setAuthentication(authentication);
} catch (Exception ignored) { }                   // ← token hỏng: im lặng
```

**Hai điều quan trọng:**

- Filter **không bao giờ trả lỗi**. Nó chỉ "đóng dấu" danh tính rồi đi tiếp.
  Việc chặn là của `SecurityConfig`.
- `tokenVersion` so với cột `users.token_version` → đổi mật khẩu thì tăng cột đó,
  **mọi token cũ chết ngay** dù chưa hết hạn.

---

## ⑦ `SecurityConfig` chặn theo vai

```java
.requestMatchers(HttpMethod.POST, "/api/marketplace/classes/**")
.hasRole(RbacConstants.CLIENT)                    // dòng 177
```

Trước nó phải có các luật hẹp hơn, vì Spring lấy **luật khớp đầu tiên**:

```java
.requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/apply")
.hasRole(RbacConstants.TUTOR)                     // dòng 171 — phải đứng TRÊN
```

Sai vai → **403**, dừng luôn, chưa vào controller.

---

## ⑧ Controller

`MarketplaceController.java:71` — mỏng đúng 1 dòng:

```java
@PostMapping("/classes")
@ResponseStatus(HttpStatus.CREATED)               // ← 201, không phải 200
public ClassResponse createClass(@RequestBody CreateClassRequest request) {
    return marketplaceService.createClass(request);
}
```

Jackson tự map JSON → `CreateClassRequest`.
**Trường nào JSON không có thì để `null`**, không lỗi.

---

## ⑨ `createClass()` — 4 chốt chặn

`MarketplaceServiceImpl.java:349`

```java
@Override
@Transactional                                     // ← lỗi ở đâu cũng rollback sạch
public ClassResponse createClass(CreateClassRequest request) {
    User creator = requireUser();                                        // 1) 404
    penaltyAccessService.requireFeature(creator.getUserId(), "CLASS_POSTING");  // 2) 403
    requireClient(creator.getUserId());                                  // 3) 403
    if (request.getSubjectId() == null && !StringUtils.hasText(request.getDetailsJson())) {
        throw new IllegalArgumentException("Vui lòng chọn môn học");     // 4) 400
    }
    TutoringClass tutoringClass = new TutoringClass();
    tutoringClass.setCreator(creator);
    applyRequest(tutoringClass, request);                                // 5) đổ dữ liệu
    tutoringClass.setBudget(request.getBudget() != null ? request.getBudget() : BigDecimal.ZERO);
    tutoringClass.setStatus(TutoringClassStatus.DRAFT);                  // 6) ÉP CỨNG
    TutoringClass saved = tutoringClassRepository.save(tutoringClass);   // 7) INSERT
    auditLogService.record(creator.getUserId(), "CREATE_CLASS", "TutoringClass",
                           saved.getClassId(), null, request);           // 8) INSERT
    return toClassResponse(saved);
}
```

### Từng chốt làm gì

```java
// 1) Lấy user id từ SecurityContext (do bước ⑥ nhét vào) rồi tra DB
private User requireUser() {
    return userRepository.findById(authHelper.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
}

// 2) Quét bảng user_penalties xem tài khoản có bị cấm đăng tin không
boolean restricted = repository.findByUser_UserIdAndStatus(userId, ACTIVE).stream()
        .filter(item -> item.getPenaltyType() == FEATURE_RESTRICTION)
        .filter(item -> item.getExpiresAt() == null || item.getExpiresAt().isAfter(now()))
        .anyMatch(details -> details.contains("\"CLASS_POSTING\""));

// 3) Đúng vai chưa đủ — phải có dòng thật trong bảng clients
private void requireClient(Long userId) {
    if (clientRepository.findByUser_UserId(userId).isEmpty()) {
        throw new ForbiddenException("Chỉ phụ huynh/khách hàng mới tạo lớp học");
    }
}
```

> **Vì sao bước 7 và 8 an toàn**: cùng nằm trong `@Transactional`. Ghi `audit_logs`
> lỗi thì `tutoring_classes` cũng bị hủy — không bao giờ có tin không có vết.

---

## ⑩ `applyRequest()` đổ DTO vào Entity

`MarketplaceServiceImpl.java:392`

```java
private void applyRequest(TutoringClass tutoringClass, CreateClassRequest request) {
    Subject subject = resolveSubject(request.getSubjectId());   // null → null; id sai → 404
    Grade grade     = resolveGrade(request.getGradeId());
    tutoringClass.setSubject(subject);
    tutoringClass.setGrade(grade);
    tutoringClass.setTitle(resolveTitle(request, subject, grade));       // ← backend tự sinh
    tutoringClass.setDescription(resolveDescription(request, subject, grade));
    tutoringClass.setDetailsJson(request.getDetailsJson());              // ← lưu nguyên xi
    if (request.getLessonMode() != null) tutoringClass.setLessonMode(request.getLessonMode());
    if (request.getStartDate() != null)  tutoringClass.setStartDate(request.getStartDate());
    if (request.getTuitionFee() != null) tutoringClass.setTuitionFee(request.getTuitionFee());
    // ...
}
```

**Mẫu `if (x != null)` lặp lại khắp nơi** — đây là cách hàm này dùng chung được
cho cả `createClass` (entity mới) lẫn `updateClass` (entity cũ): client không gửi
trường nào thì giữ nguyên giá trị cũ, không bị `null` đè lên.

### Tự sinh tiêu đề

```java
private String autoTitle(String detailsJson, Subject subject, Grade grade) {
    List<String> names = subjectNamesFromJson(detailsJson);   // đọc subjectIds + subjectOthers
    if (names.isEmpty() && subject != null) names = List.of(subject.getSubjectName());
    StringBuilder sb = new StringBuilder("Cần tìm gia sư");
    if (!names.isEmpty()) sb.append(" môn ").append(String.join(", ", names));
    String title = sb.toString();
    return title.length() > TITLE_MAX_LENGTH                  // 150 = varchar(150)
            ? title.substring(0, TITLE_MAX_LENGTH - 1) + "…"
            : title;
}
```

Ra `"Cần tìm gia sư môn Toán, Vật lý, Hóa học"`. Cắt 150 ký tự để không nổ
`DataIntegrityViolationException` khi chọn quá nhiều môn.

---

## ⑪ SQL thật chạy xuống MySQL

```sql
INSERT INTO tutoring_classes
  (creator_id, class_type, subject_id, grade_id, title, description, details_json,
   address, lesson_mode, number_of_sessions, tuition_fee, start_date, end_date,
   budget, recurring_type, status, created_at, updated_at, expires_at)
VALUES
  (37, 'PRIVATE', 1, 9, 'Cần tìm gia sư môn Toán', '...', '{"subjectIds":[...]}',
   '123 Lê Lợi, Phường Láng, Thành phố Hà Nội', 'OFFLINE', 8, 200000, '2026-08-24', '2026-09-24',
   6400000, 'WEEKLY', 'DRAFT', NOW(), NOW(), NULL);
                              -- status = nháp,  expires_at = chưa có hạn hiển thị

INSERT INTO audit_logs (user_id, action, entity_type, entity_id, new_value, created_at)
VALUES (37, 'CREATE_CLASS', 'TutoringClass', 328, '{...toàn bộ request...}', NOW());
```

`expires_at = NULL` vì tin còn nháp — chỉ khi bấm "Đăng lớp" mới có:

```java
// publishClass() — MarketplaceServiceImpl.java:449
tutoringClass.setStatus(TutoringClassStatus.OPEN);
tutoringClass.setExpiresAt(LocalDateTime.now().plusDays(CLASS_DISPLAY_DAYS));  // +30 ngày
```

---

## ⑫ Về lại trình duyệt

```
201 Created
{"classId":328,"title":"Cần tìm gia sư môn Toán","status":"DRAFT", ... }
```

`setCreated(result)` → React re-render → form biến mất, hiện màn ✓.

---

## Nếu lỗi thì đi đường nào

Không có `try/catch` trong service. Exception bay lên `GlobalExceptionHandler`:

```java
@ExceptionHandler(ForbiddenException.class)
public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                         .body(Map.of("message", ex.getMessage()));
}
```

Về đến frontend:

```tsx
function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;      // ← đọc đúng field "message"
  }
  if (err instanceof Error) return err.message;
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}
```

Nhờ backend **luôn** trả `{"message": "..."}` mà frontend chỉ cần đúng một hàm
này cho mọi API.

---

## Sơ đồ 12 bước

```
①  onClick            nút type="button", không reload trang
②  handleSubmit       missing / slotErrors / conflicts      ← chặn ở FE
③  formToPayload      JSON.stringify(form) → detailsJson
④  Page.handleSubmit  chặn !isClient, khóa nút
⑤  axios interceptor  gắn Bearer token
    ─────────── HTTP ───────────
⑥  JwtFilter          token → SecurityContext (không tự trả lỗi)
⑦  SecurityConfig     hasRole(CLIENT)                       → 403
⑧  Controller         JSON → CreateClassRequest, đánh dấu 201
⑨  createClass        4 chốt: user / phạt / client / môn    → 404·403·403·400
⑩  applyRequest       tra entity, autoTitle, if(!=null)
⑪  SQL                INSERT tutoring_classes + audit_logs    status=DRAFT
⑫  201 Created        → setCreated() → màn ✓
```
