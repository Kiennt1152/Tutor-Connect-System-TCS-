# Kịch bản demo Happy Case - BF-10 Platform Administration & Analytics

## 1. Mục tiêu demo

Chứng minh một Platform Admin có thể đi hết luồng quản trị nền tảng:

1. Đăng nhập và xem tình trạng vận hành.
2. Kiểm tra hàng đợi công việc và chọn một tác vụ cần xử lý.
3. Xử lý ticket, thay đổi phân loại/độ ưu tiên, phản hồi và hoàn tất yêu cầu.
4. Quản lý tài nguyên dùng chung của nền tảng.
5. Xem, lọc và xuất báo cáo phân tích.
6. Kiểm tra dashboard, audit log và thông báo người dùng đã được cập nhật.

Thời lượng đề xuất: **12-15 phút**.

## 2. Phạm vi UC

| Phần demo | UC-ID theo BF-10 | Chức năng được thể hiện |
|---|---|---|
| Đăng nhập Admin | UC-01 | Xác thực và điều hướng theo vai trò |
| Dashboard | UC-56 | KPI, GMV, doanh thu phí, cảnh báo và tác vụ ưu tiên |
| Hàng đợi vận hành | UC-11, UC-49, UC-51, UC-66 | Tổng hợp xác minh, báo cáo, ticket, rút tiền, hoàn tiền và tranh chấp |
| Quản lý tài nguyên | UC-07, UC-46, UC-57 | Người dùng, danh mục, cấu hình và thông báo hệ thống |
| Báo cáo và phân tích | UC-41, UC-43 | Lọc theo thời gian, xem KPI và xuất CSV |
| Cập nhật hệ thống | System | Refresh dashboard và ghi audit log |
| Thông báo stakeholder | System | Người tạo ticket nhận thông báo sau phản hồi/đóng ticket |

> Lưu ý: tài liệu BF-09 cũ trong repository gọi màn quản lý support ticket là UC-63. Trong bảng BF-10 đầu bài, tác vụ hỗ trợ nằm ở UC-66. Khi thuyết trình nên dùng mã UC theo tài liệu đặc tả chính thức của nhóm và thống nhất lại mapping này trước khi quay.

## 3. Chuẩn bị trước khi demo

### Tài khoản và cửa sổ

- Cửa sổ A: tài khoản `PLATFORM_ADMIN` đang hoạt động.
- Cửa sổ B hoặc trình duyệt ẩn danh: một tài khoản `CLIENT` đang hoạt động.
- Không dùng cùng một profile trình duyệt cho hai tài khoản để tránh ghi đè access token.

### Dữ liệu tối thiểu

- Có ít nhất một số liệu người dùng, gia sư, lớp học và giao dịch để Analytics không trống.
- Có ít nhất một task đang chờ ở một trong các nhóm xác minh, báo cáo, hoàn tiền hoặc tranh chấp để minh họa hàng đợi tổng hợp.
- Tài khoản Client tạo trước một ticket:
  - Tiêu đề: `[DEMO BF10] Không nhận được xác nhận thanh toán`
  - Nội dung: `Tôi đã thanh toán nhưng chưa thấy trạng thái lớp học được cập nhật. Nhờ bộ phận hỗ trợ kiểm tra.`
  - Trạng thái: `OPEN`
  - Priority ban đầu: `MEDIUM` hoặc `LOW`
- Ghi lại giá trị hiện tại của `PLATFORM_FEE_RATE` nếu sẽ demo cập nhật cấu hình.
- Chuẩn bị một danh mục dùng cho demo, ví dụ `Tin học văn phòng`, hoặc chọn một danh mục có thể chỉnh mô tả mà không ảnh hưởng dữ liệu thật.

### Kiểm tra nhanh

- `/platform` tải được KPI.
- `/platform/tasks` có dữ liệu.
- `/platform/tickets` tìm thấy ticket `[DEMO BF10]`.
- `/platform/analytics` có dữ liệu và nút tải CSV.
- `/platform/audit-logs` tải được danh sách.
- Trình duyệt cho phép tải file xuống.

## 4. Kịch bản chi tiết

### Scene 1 - Đăng nhập Platform Admin (UC-01, 45 giây)

**Thao tác**

1. Mở `/login` ở cửa sổ A.
2. Nhập tài khoản Platform Admin và chọn **Đăng nhập**.
3. Xác nhận hệ thống điều hướng vào khu vực quản trị.

**Lời thoại gợi ý**

> "Tôi đăng nhập bằng tài khoản Platform Admin. Hệ thống xác thực tài khoản, nhận diện vai trò quản trị và chỉ cho phép truy cập các module vận hành tương ứng."

**Kết quả cần thấy**

- Đăng nhập thành công.
- Sidebar quản trị xuất hiện.
- Không có lỗi `401` hoặc `403`.

### Scene 2 - Quan sát dashboard (UC-56, 1 phút 30 giây)

**Thao tác**

1. Mở `/platform`.
2. Chỉ lần lượt các KPI: tổng người dùng, gia sư, lớp học, lớp đang diễn ra, xác minh chờ duyệt, báo cáo đang mở, ticket và tranh chấp.
3. Chỉ khu vực **Cần xử lý**, cảnh báo hệ thống, GMV và doanh thu phí dịch vụ.
4. Ghi nhớ số ticket đang mở để so sánh sau khi xử lý.

**Lời thoại gợi ý**

> "Dashboard lấy dữ liệu vận hành hiện tại từ hệ thống. Admin có thể nhìn thấy quy mô nền tảng, các yêu cầu tồn đọng và số liệu tài chính mà không cần mở từng module. Các thẻ cần xử lý đóng vai trò shortcut tới đúng hàng đợi nghiệp vụ."

**Kết quả cần thấy**

- KPI hiển thị số liệu thay vì trạng thái loading/error.
- Các shortcut điều hướng được tới module tương ứng.

### Scene 3 - Kiểm tra hàng đợi vận hành (UC-11, UC-49, UC-51, UC-66, 1 phút 30 giây)

**Thao tác**

1. Mở **Hàng đợi công việc** tại `/platform/tasks`.
2. Chỉ các nhóm: xác minh hồ sơ, báo cáo vi phạm, hỗ trợ, rút tiền, hoàn tiền và tranh chấp.
3. Chọn thẻ hoặc bộ lọc **Khiếu nại & Hỗ trợ**.
4. Tìm task có tiêu đề chứa `[DEMO BF10]` và chọn **Xử lý ngay**.
5. Hệ thống hiện mở trang ticket nhưng chưa tự mở đúng modal; tìm lại `[DEMO BF10]` trong danh sách rồi chọn **Xem chi tiết**.

**Lời thoại gợi ý**

> "Đây là một hàng đợi thống nhất. Admin có thể xem khối lượng công việc theo loại, độ ưu tiên và trạng thái. Tôi lọc support ticket và đi thẳng từ task tới màn xử lý chuyên biệt."

**Kết quả cần thấy**

- Bộ đếm theo loại task hiển thị đúng.
- Danh sách chỉ còn support ticket sau khi lọc.
- **Xử lý ngay** mở đúng module ticket; Admin tìm được ticket tương ứng trong danh sách.

### Scene 4 - Phân loại và xử lý ticket (UC-66, 3 phút)

**Thao tác**

1. Tại `/platform/tickets`, tìm ticket `[DEMO BF10]` và mở chi tiết.
2. Xác nhận việc mở ticket lần đầu tự gán Admin hiện tại và chuyển `OPEN` thành `IN_PROGRESS`.
3. Trong **Phân loại xử lý**:
   - Category: chọn loại phù hợp, ví dụ `PAYMENT`.
   - Priority: đổi thành `HIGH`.
4. Chỉ hạn SLA mới. Giải thích `HIGH = createdAt + 12 giờ`.
5. Chọn **Lưu phân loại**.
6. Nhập phản hồi:

   `Bộ phận hỗ trợ đã kiểm tra giao dịch. Thanh toán đã được ghi nhận và trạng thái lớp học đã được đồng bộ. Bạn vui lòng tải lại trang để kiểm tra.`

7. Gửi phản hồi và xác nhận ticket chuyển sang `IN_REVIEW`.
8. Chọn hoàn tất với trạng thái `RESOLVED`, ghi chú:

   `Đã xác nhận thanh toán và đồng bộ trạng thái lớp học.`

**Lời thoại gợi ý**

> "Admin có thể override category và priority theo tình huống thực tế. Khi priority đổi sang HIGH, hệ thống tính lại dueAt từ thời điểm ticket được tạo theo SLA 12 giờ, đồng thời cập nhật trạng thái vi phạm SLA. Sau đó Admin phản hồi người dùng và đánh dấu yêu cầu đã được giải quyết."

**Kết quả cần thấy**

- Category, priority, badge và dueAt cập nhật cả trong modal lẫn danh sách.
- Phản hồi Admin xuất hiện trong lịch sử hội thoại.
- Ticket cuối cùng có trạng thái `RESOLVED`.
- Ticket đã hoàn tất chỉ đọc, không còn cho lưu category/priority.

### Scene 5 - Xác nhận stakeholder nhận thông báo (System, 1 phút)

**Thao tác**

1. Chuyển sang cửa sổ B đang đăng nhập tài khoản Client tạo ticket.
2. Làm mới trang hoặc mở biểu tượng chuông thông báo.
3. Mở thông báo phản hồi support ticket.
4. Xác nhận nội dung liên quan đúng ticket vừa xử lý.

**Lời thoại gợi ý**

> "Sau hành động của Admin, hệ thống tạo thông báo cho đúng chủ ticket. Người dùng nhận được kết quả xử lý mà không cần liên hệ ngoài nền tảng."

**Kết quả cần thấy**

- Chuông có thông báo mới.
- Tiêu đề hoặc nội dung chứa mã ticket và phản hồi của Admin.
- Thông báo điều hướng về đúng ngữ cảnh support ticket nếu giao diện cung cấp liên kết.

### Scene 6 - Quản lý tài nguyên nền tảng (UC-07, UC-46, UC-57, 3 phút)

#### 6A. Người dùng

1. Quay lại cửa sổ A, mở `/platform/users`.
2. Tìm theo email Client vừa dùng.
3. Lọc vai trò `CLIENT` và trạng thái `ACTIVE`.
4. Chỉ thao tác kích hoạt, tạm ngưng và khóa; không cần khóa tài khoản đang dùng trong happy case.

> "Admin có thể tìm kiếm, phân loại và quản lý trạng thái tài khoản. Tài khoản Platform Admin được bảo vệ, không hiển thị các thao tác khóa chính nó."

#### 6B. Danh mục

1. Mở `/catalog` từ menu **Danh mục**.
2. Chọn nhóm `SUBJECT`.
3. Tạo danh mục con:
   - Tên: `Tin học văn phòng`
   - Mô tả: `Kỹ năng Word, Excel và PowerPoint`
   - Trạng thái: `ACTIVE`
4. Lưu và xác nhận mục mới xuất hiện trong cây danh mục.

> "Danh mục dùng chung được cập nhật tập trung. Mục ACTIVE mới có thể được các luồng hồ sơ, lớp học và tìm kiếm tái sử dụng."

#### 6C. Cấu hình hệ thống

1. Mở `/platform/parameters`.
2. Tìm `PLATFORM_FEE_RATE`.
3. Chọn **Sửa**, đổi ví dụ từ `0.10` thành `0.11`, cập nhật mô tả và lưu.
4. Không xóa parameter này.

> "Tham số phí được lưu tập trung. Analytics đọc chính parameter này để hiển thị tỷ lệ và doanh thu phí dịch vụ. Sau demo có thể trả lại giá trị ban đầu nếu đây là môi trường dùng chung."

#### 6D. Thông báo hệ thống

1. Mở `/platform/announcements`.
2. Chọn **Tạo thông báo mới**.
3. Nhập:
   - Tiêu đề: `Hoàn tất nâng cấp hệ thống TCS`
   - Nội dung: `Các dịch vụ đã hoạt động bình thường. Cảm ơn bạn đã đồng hành cùng TCS.`
   - Đối tượng: `Tất cả vai trò`
   - Kích hoạt: bật
   - Khoảng hiệu lực: bao gồm thời điểm demo
4. Chọn **Tạo thông báo**.
5. Refresh cửa sổ B để tải lại danh sách announcement, sau đó mở chuông thông báo và chỉ thông báo hệ thống vừa tạo.

> "Thông báo hệ thống là nội dung phát hành có lịch và đối tượng nhận. Nó khác mẫu thông báo: mẫu chỉ là cấu trúc tái sử dụng cho các sự kiện nghiệp vụ."

**Kết quả cần thấy**

- Tìm và lọc được người dùng.
- Danh mục mới xuất hiện trong đúng nhóm.
- Parameter lưu thành công.
- Announcement active xuất hiện ở chuông của đúng đối tượng.

### Scene 7 - Báo cáo và phân tích (UC-41, UC-43, 2 phút)

**Thao tác**

1. Mở `/platform/analytics`.
2. Chọn khoảng **Từ ngày** và **Đến ngày** có dữ liệu.
3. Chỉ các nhóm KPI:
   - Cơ cấu người dùng.
   - Tổng lớp và lớp đang diễn ra.
   - Tỷ lệ hoàn thành hợp đồng, duyệt gia sư và khiếu nại.
   - GMV, phí dịch vụ, tiền nạp/rút và escrow.
   - Tăng trưởng sáu tháng gần nhất.
4. Xác nhận tỷ lệ phí hiển thị theo `PLATFORM_FEE_RATE` vừa cập nhật.
5. Chọn **Tải CSV Doanh thu**.
6. Mở file tải xuống và chỉ nhanh header cùng một dòng dữ liệu.

**Lời thoại gợi ý**

> "Admin có thể giới hạn số liệu theo khoảng thời gian, theo dõi KPI vận hành và tài chính, sau đó xuất CSV để tiếp tục phân tích hoặc lập báo cáo bên ngoài. Phiên bản hiện tại hỗ trợ CSV; chưa nên giới thiệu PDF, Excel native hoặc scheduled export như một chức năng đã hoàn thành."

**Kết quả cần thấy**

- Thay đổi ngày làm dữ liệu được tải lại.
- KPI hiển thị nhất quán.
- File `tcs-analytics-revenue-YYYY-MM-DD.csv` tải thành công.

### Scene 8 - Dashboard và audit log tự cập nhật (System, 1 phút 30 giây)

**Thao tác**

1. Quay lại `/platform`, chọn refresh trình duyệt.
2. So sánh số ticket mở với số đã ghi ở Scene 2; ticket vừa resolve không còn nằm trong nhóm đang mở.
3. Mở `/platform/tasks`, lọc support ticket và xác nhận task vừa xử lý không còn trong hàng đợi.
4. Mở `/platform/audit-logs`.
5. Chọn khoảng thời gian bao gồm lúc demo.
6. Tìm các bản ghi mới nhất và mở **Xem chi tiết** ở old/new value. Các action mới chưa có nhãn Việt hóa trong dropdown nhưng vẫn xuất hiện bằng mã kỹ thuật:
   - `UPDATE_TICKET` trên `SupportTicket`.
   - `RESPOND_TICKET` và `CLOSE_TICKET`.
   - `CREATE_CATEGORY`.
   - `UPDATE_SYSTEM_PARAMETER`.
   - `CREATE_ANNOUNCEMENT`.

**Lời thoại gợi ý**

> "Sau khi nghiệp vụ hoàn tất, dữ liệu dashboard và task queue phản ánh trạng thái mới. Các thao tác ghi dữ liệu đều có audit trail gồm actor, thời gian, entity và giá trị trước/sau để phục vụ truy vết."

**Kết quả cần thấy**

- Ticket đã resolve không còn được tính là ticket mở.
- Task không còn trong hàng đợi pending.
- Audit log có actor Admin, entity đúng và old/new value tương ứng.

## 5. Câu kết demo

> "Happy case BF-10 đã hoàn tất: Platform Admin đăng nhập, theo dõi KPI, lấy công việc từ hàng đợi, xử lý ticket theo SLA, quản lý dữ liệu dùng chung và xuất báo cáo. Hệ thống đồng thời cập nhật dashboard, loại task đã hoàn tất khỏi hàng đợi, ghi audit log và thông báo cho stakeholder liên quan."

## 6. Checklist pass/fail

- [ ] Admin đăng nhập và truy cập được khu vực `/platform`.
- [ ] Dashboard hiển thị KPI, cảnh báo, GMV và công việc ưu tiên.
- [ ] Hàng đợi lọc được theo loại và điều hướng đúng task.
- [ ] Ticket được tự gán khi mở lần đầu.
- [ ] Priority `HIGH` làm dueAt bằng `createdAt + 12 giờ`.
- [ ] Category/priority cập nhật ở modal và danh sách.
- [ ] Client nhận thông báo sau khi Admin phản hồi hoặc resolve.
- [ ] Danh mục mới được lưu ở trạng thái ACTIVE.
- [ ] Parameter cập nhật thành công và được Analytics sử dụng.
- [ ] Announcement active xuất hiện cho đúng nhóm người dùng.
- [ ] Analytics lọc được theo ngày và tải được CSV.
- [ ] Dashboard/task queue phản ánh ticket đã hoàn tất.
- [ ] Audit log ghi được các thao tác quan trọng và old/new value.

## 7. Hoàn nguyên dữ liệu sau demo

1. Đưa `PLATFORM_FEE_RATE` về giá trị đã ghi lại trước demo.
2. Xóa hoặc chuyển `INACTIVE` danh mục `Tin học văn phòng` nếu chỉ tạo để quay demo.
3. Tắt hoặc xóa announcement `Hoàn tất nâng cấp hệ thống TCS`.
4. Giữ ticket `RESOLVED` làm bằng chứng demo, hoặc dùng database demo riêng cho lần quay sau.

Các thao tác hoàn nguyên cũng được ghi audit log; đây là hành vi đúng của hệ thống.

## 8. Những nội dung không nên khẳng định trong happy case hiện tại

- Dashboard chưa có bộ lọc theo campus, subject, Tutor Center hoặc service type.
- Analytics hiện lọc theo khoảng ngày; chưa có scheduled report.
- Export hiện là CSV; chưa có PDF hoặc file Excel native.
- Không sử dụng Maintenance Mode trong happy case này.
- Không nói rằng mọi loại tác vụ đều đã được xử lý trong một lần demo; hàng đợi chứng minh khả năng tổng hợp, còn ticket là tác vụ đại diện được xử lý end-to-end.
