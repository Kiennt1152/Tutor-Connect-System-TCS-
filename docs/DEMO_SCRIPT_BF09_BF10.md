# Script Demo Video - BF-09 & BF-10
## Kịch Bản Chi Tiết Cho Video Recording

---

## 🎬 INTRO (30 giây)

**[Màn hình: Desktop với logo TCS hoặc homepage]**

> "Xin chào các bạn! Hôm nay mình sẽ demo 2 business function quan trọng của hệ thống Tutor Connect System:
> - BF-09: Customer Support - Hệ thống hỗ trợ khách hàng
> - BF-10: Platform Administration - Quản trị nền tảng
> 
> Chúng ta sẽ xem cách người dùng và admin tương tác với các tính năng này."

---

## 📋 PART 1: BF-09 - CUSTOMER SUPPORT (15 phút)

### Scene 1: Browse FAQ - Tra Cứu Câu Hỏi Thường Gặp (2 phút)

**[Navigate to /help]**

> "Đầu tiên là trang Help - nơi người dùng có thể tìm câu trả lời cho các thắc mắc thường gặp.
> 
> Điểm đặc biệt: Trang này public, không cần đăng nhập.
> 
> Các FAQ được chia theo category..."

**[Click vào 1-2 categories để mở accordion]**

> "Ví dụ category 'Thanh toán' có các câu hỏi về payment methods, refund policy...
> 
> Category 'Tài khoản' có hướng dẫn đăng ký, verify..."

**[Type vào search box: "hoàn tiền"]**

> "Người dùng cũng có thể search trực tiếp. Ví dụ mình tìm 'hoàn tiền'...
> 
> Hệ thống filter real-time và highlight các FAQ liên quan."

**[Scroll qua kết quả]**

> "Rất tiện lợi để người dùng tự tra cứu trước khi phải liên hệ support."

---

### Scene 2: AI Chatbot - Trợ Lý AI Thông Minh (3 phút)

**[Navigate to homepage hoặc browse page]**

> "Bây giờ chúng ta thấy một tính năng đặc biệt: AI Chatbot.
> 
> Floating button này xuất hiện ở mọi trang..."

**[Click floating chatbot button]**

> "Click vào sẽ mở mini chat window."

**[Type: "Làm sao để tìm gia sư toán lớp 10?"]**

> "Mình hỏi: 'Làm sao để tìm gia sư toán lớp 10?'
> 
> AI sẽ trả lời bằng tiếng Việt..."

**[Wait for response, show tutor cards]**

> "Và quan trọng là AI không chỉ trả lời text, mà còn gợi ý các gia sư phù hợp.
> 
> Đây là công nghệ RAG - Retrieval Augmented Generation.
> AI pull data thật từ database: tutors, classes, FAQ entries."

**[Click "Xem đầy đủ" to navigate to /ai-assistant]**

> "Click 'Xem đầy đủ' để sang trang AI Assistant với nhiều tính năng hơn."

**[Show AI Assistant page layout]**

> "Bên trái là lịch sử các session chat,
> Bên phải là chat area chính."

**[Type complex query]**

> "Bây giờ mình hỏi câu phức tạp hơn:
> 
> 'Tôi cần gia sư Vật Lý lớp 12 ở Hà Nội, giá dưới 200k/buổi. Ngoài ra cho tôi biết quy trình đăng ký học và chính sách hoàn tiền.'"

**[Wait for response]**

> AI phân tích ý định (intent) và trả lời theo luồng RAG rất chuẩn xác:
> - Danh sách gia sư Vật Lý phù hợp với filter
> - Quy trình đăng ký (referenced FAQ)
> - Chính sách hoàn tiền (referenced FAQ)
> 
> Đặc biệt, hệ thống hiển thị thanh Metadata Bar bên dưới mỗi câu trả lời.
> Ở đây ta thấy AI chấm mức độ tin cậy (Confidence Score) là 90%, thuộc nhóm HIGH, dựa trên 5 nguồn tham chiếu, và chế độ trả lời là RAG.
> 
> Tất cả được trình bày dưới dạng cards clickable."

**[Click vào tutor card]**

> "Click vào tutor card sẽ navigate tới trang profile của gia sư."

**[Type vào ô chat: "Thời tiết hôm nay thế nào?" hoặc "1 + 1 bằng mấy?"]**

> "Bây giờ thử một câu hỏi ngoài lề hoặc phổ thông.
> Hệ thống sẽ nhận diện Intent là 'Ngoài phạm vi' (OUT_OF_SCOPE), sau đó chuyển sang Fallback mode (chỉ dùng kiến thức nội tại của LLM, không ép nhồi nhét RAG sai ngữ cảnh).
> Và nếu điểm tin cậy thấp, hệ thống sẽ tự động hiện Banner cảnh báo người dùng."

---

**[Back to AI page, show session history]**

> "Quay lại AI Assistant, các bạn thấy bên trái có lịch sử conversation.
> 
> Người dùng có thể quay lại session cũ..."

**[Click vào previous session]**

> "Hoặc tiếp tục chat trong session hiện tại."

**[Demo delete session]**

> "Và có thể xóa session khi không cần nữa."

---

### Scene 3: User Support Tickets - Tạo Yêu Cầu Hỗ Trợ (3 phút)

**[Login as student@test.com]**

> "Bây giờ mình login với tài khoản student để demo tính năng support tickets."

**[Navigate to /messaging]**

> "Vào Messaging, click tab 'Support Tickets'."

**[Show ticket list if available]**

> "Đây là danh sách các tickets của user.
> 
> Mỗi ticket có:
> - Status badge: OPEN, IN_PROGRESS, RESOLVED, CLOSED
> - Priority indicator với màu sắc: LOW (xanh), MEDIUM (vàng), HIGH (cam), URGENT (đỏ)
> - Category label
> - Ngày tạo và due date (SLA)"

**[Click "Tạo yêu cầu hỗ trợ"]**

> "User có thể tạo ticket mới. Click 'Tạo yêu cầu hỗ trợ'."

**[Fill form]**

> "Điền form:
> - Category: Mình chọn 'BUG_REPORT' - báo lỗi hệ thống
> - Subject: 'Không thể thanh toán qua VNPay'
> - Description: 'Tôi chọn thanh toán VNPay nhưng redirect về lỗi 500. Xảy ra lúc 14h30 hôm nay.'
> - Evidence URLs: có thể attach link ảnh chụp màn hình"

**[Submit]**

> "Submit... Ticket đã được tạo!"

**[Show created ticket]**

> "Các bạn thấy:
> - Priority tự động được set là HIGH vì category là BUG_REPORT
> - Due date: 12 tiếng kể từ bây giờ (SLA cho HIGH priority)
> - Status: OPEN
> 
> Đây là logic business rule: Category nhất định sẽ escalate priority tự động."

**[Click vào ticket detail]**

> "Click vào ticket để xem chi tiết.
> 
> Có message thread, user có thể reply bổ sung thông tin..."

**[Type reply message]**

> "Ví dụ: 'Xin bổ sung: Lỗi xảy ra với số tiền 500,000 VND. Đã thử 3 lần.'"

**[Submit reply]**

> "Reply thành công. Admin sẽ nhận được notification."

---

### Scene 4: Admin Manage Tickets - Xử Lý Support Tickets (4 phút)

**[Login as admin@test.com in separate browser window]**

> "Bây giờ chuyển sang admin view để xem cách admin xử lý tickets."

**[Navigate to /platform/tickets]**

> "Admin vào Platform > Support Tickets."

**[Show ticket list]**

> "Đây là queue của tất cả tickets trong hệ thống.
> 
> Admin có thể filter theo:
> - Status
> - Category  
> - Priority
> - Keyword search"

**[Apply filter: Status=OPEN, Priority=HIGH]**

> "Mình filter Status=OPEN và Priority=HIGH để ưu tiên xử lý tickets khẩn cấp."

**[Show SLA indicators]**

> "Các bạn thấy có badge màu đỏ 'SLA Breached' - đây là tickets đã quá hạn.
> 
> Hệ thống có scheduler chạy mỗi 10 phút để đánh dấu SLA breach."

**[Click vào ticket detail]**

> "Click vào ticket của user vừa tạo...
> 
> Khi admin mở ticket lần đầu:
> - Ticket tự động assign cho admin này
> - Status chuyển từ OPEN sang IN_PROGRESS
> 
> Đây là auto-assignment mechanism."

**[Show ticket info panel]**

> "Ticket detail hiển thị:
> - User info
> - Category, Priority, Status
> - Created at, Due at
> - SLA breached flag
> - Message thread đầy đủ"

**[Update priority]**

> "Admin có thể update priority nếu cần.
> 
> Ví dụ upgrade từ HIGH lên URGENT vì ảnh hưởng nhiều users..."

**[Change priority to URGENT, add reason]**

> "Nhập reason: 'Nhiều users báo cùng lỗi. Critical.'
> 
> Submit... Priority updated, due date được tính lại (URGENT = 4 tiếng)."

**[Type admin response]**

> "Bây giờ admin respond:
> 
> 'Xin chào, team đã xác định được lỗi VNPay integration. Chúng tôi đang fix, dự kiến 2 giờ nữa sẽ hoàn tất. Bạn vui lòng thử lại sau 14h00. Xin lỗi vì sự bất tiện.'"

**[Submit response]**

> "Submit... Response đã gửi.
> 
> Hệ thống ghi nhận:
> - Response SLA time: thời gian từ OPEN đến first admin reply
> - User nhận in-app notification
> - Ticket vẫn giữ status IN_PROGRESS"

**[Close ticket]**

> "Sau khi fix xong, admin close ticket.
> 
> Select status: RESOLVED
> 
> Nhập resolution note: 'Đã fix lỗi VNPay API timeout. Đã test thành công. User có thể thanh toán bình thường.'"

**[Submit close]**

> "Ticket đã đóng, timestamp resolvedAt được ghi nhận.
> 
> User có thể reopen ticket nếu vấn đề vẫn còn."

---

### Scene 5: Admin Manage FAQ - Quản Lý Kiến Thức (3 phút)

**[Navigate to /platform/faq]**

> "Tiếp theo là FAQ Management - quản lý kiến thức cơ sở.
> 
> Admin vào Platform > FAQ Management."

**[Show FAQ list]**

> "Danh sách FAQ với:
> - Published status (công khai hay không)
> - Category
> - Sort order (thứ tự hiển thị)
> - Search box"

**[Click "Tạo FAQ mới"]**

> "Admin tạo FAQ mới."

**[Fill form]**

> "Điền:
> - Question: 'Làm sao để rút tiền từ ví TCS?'
> - Answer: 'Vào Ví của tôi > Rút tiền. Nhập số tiền và thông tin ngân hàng. Xử lý trong 1-3 ngày làm việc. Phí rút: 0 VND.'
> - Category: PAYMENT
> - Published: true
> - Sort order: 5"

**[Submit]**

> "Submit... FAQ created! Hệ thống ghi audit log."

**[Show new FAQ in list]**

> "FAQ mới xuất hiện trong danh sách và ngay lập tức có trên public Help page."

**[Edit FAQ]**

> "Admin có thể edit. Ví dụ bổ sung thêm info...
> 
> 'Lưu ý: Số tiền rút tối thiểu 100,000 VND.'"

**[Update]**

> "Update... Audit log ghi nhận change."

**[Toggle published]**

> "Admin cũng có thể unpublish FAQ tạm thời - ví dụ khi thông tin cũ, cần cập nhật.
> 
> Toggle published = false... FAQ bị ẩn khỏi public view nhưng vẫn trong admin list."

**[Delete FAQ]**

> "Và có thể xóa FAQ nếu không còn cần thiết.
> 
> Confirm delete... FAQ removed, audit log ghi nhận."

---

## 📊 PART 2: BF-10 - PLATFORM ADMINISTRATION (11 phút)

### Scene 6: Admin Dashboard (Activity Timeline) (3 phút)

**[Navigate to /platform/dashboard]**

> "Chuyển sang BF-10: Quản trị nền tảng.
> Đầu tiên là Dashboard tổng quan, hiển thị các KPI quan trọng nhất.
> Ta có thể thấy Active Users, Active Tutors, v.v..."

**[Scroll down to Activity Timeline]**

> "Điểm nổi bật ở bản nâng cấp này là Bảng Hoạt động (Activity Timeline).
> Ở đây chúng ta có bộ lọc ngày (từ ngày, đến ngày) và mức độ hiển thị (Ngày/Tuần/Tháng).
> Khi thay đổi bộ lọc, bảng dữ liệu bên dưới sẽ tính toán realtime sự tăng trưởng: có bao nhiêu Gia sư mới, Trung tâm mới, Lớp mới và Doanh thu tương ứng."

**[Test thay đổi Date Filter trên Dashboard]**

> "Dữ liệu được truy vấn ngay lập tức, rất tiện lợi cho Admin theo dõi sức khỏe nền tảng."

---

> "Dashboard hiển thị các KPI quan trọng:
> 
> - Total Users: X users (breakdown: students, tutors, parents)
> - Active Classes: Y classes đang hoạt động
> - Pending Verifications: Z gia sư chờ duyệt
> - Open Tickets: N tickets chưa xử lý
> 
> Tất cả là real-time data từ database."

**[Show alerts section]**

> "Phía dưới là Alerts - cảnh báo ưu tiên:
> 
> - SLA-breached tickets: Tickets quá hạn cần xử lý gấp
> - High-priority reports: Báo cáo vi phạm nghiêm trọng
> - Pending verifications: Gia sư chờ verify quá 3 ngày
> 
> Click vào alert sẽ navigate trực tiếp đến task tương ứng."

**[Scroll dashboard]**

> "Dashboard giúp admin nắm bắt tình trạng hệ thống một cách nhanh chóng."

---

### Scene 7: Configure Platform Fees - Cấu Hình Phí Nền Tảng (2 phút)

**[Navigate to /platform/parameters]**

> "Một tính năng quan trọng: Cấu hình System Parameters - các thông số hệ thống.
> 
> Admin vào Platform > Cấu hình hệ thống."

**[Show parameters list]**

> "Đây là key-value config store.
> 
> Các parameters như:
> - PLATFORM_FEE_RATE: Tỷ lệ phí nền tảng
> - MAX_FILE_SIZE: Giới hạn upload file
> - SESSION_TIMEOUT: Timeout session
> - v.v."

**[Search: "PLATFORM_FEE_RATE"]**

> "Mình search 'PLATFORM_FEE_RATE'..."

**[Show current value]**

> "Giá trị hiện tại: 0.10 - tức 10%.
> 
> Đây là tỷ lệ phí mà platform thu từ mỗi giao dịch."

**[Click edit]**

> "Admin có quyền thay đổi. Click edit..."

**[Change value to 0.12]**

> "Ví dụ tăng lên 12% do adjust business model.
> 
> Value: 0.12
> Description: 'Platform fee rate (0-1). Current: 12%. Updated Aug 2026.'"

**[Submit]**

> "Submit... Parameter updated, audit log ghi nhận.
> 
> Từ giờ, mọi financial report sẽ dùng rate mới để tính platform fee revenue."

---

### Scene 8: Monitor Audit Logs - Giám Sát Nhật Ký Hệ Thống (3 phút)

**[Navigate to /platform/audit-logs]**

> "Một tính năng bắt buộc cho compliance: Audit Logs.
> 
> Admin vào Platform > Audit Logs."

**[Show audit log list]**

> "Mọi hành động admin write đều được ghi lại:
> 
> - Actor: User nào thực hiện (userId + role)
> - Action: CREATE, UPDATE, DELETE
> - Entity Type: User, FAQ, Ticket, Category, SystemParameter...
> - Entity ID
> - Timestamp
> - IP Address, User Agent"

**[Show filters]**

> "Admin có thể filter để audit:
> 
> - By actor: Kiểm tra hành động của 1 admin cụ thể
> - By action: Chỉ xem UPDATE hoặc DELETE
> - By entity type: Audit FAQ, hoặc SystemParameter
> - By date range: Last 7 days, last month..."

**[Apply filter: Action=UPDATE, Entity=SystemParameter]**

> "Mình filter Action=UPDATE, Entity=SystemParameter để xem ai đã sửa config..."

**[Show results]**

> "Kết quả: Admin X đã update PLATFORM_FEE_RATE lúc 12:05 PM."

**[Click vào log entry]**

> "Click vào entry để xem detail..."

**[Show old/new JSON values]**

> "Audit log lưu đầy đủ:
> - Old value: {\"paramValue\": \"0.10\"}
> - New value: {\"paramValue\": \"0.12\"}
> 
> Với diff viewer, admin dễ dàng thấy field nào thay đổi."

**[Show another example: FAQ update]**

> "Ví dụ khác: Audit log của FAQ update.
> 
> Old value: {\"answer\": \"Vào Ví của tôi...\"}
> New value: {\"answer\": \"Vào Ví của tôi... Lưu ý: Số tiền tối thiểu...\"}"

**[Highlight importance]**

> "Audit trail này quan trọng cho:
> - Security: Phát hiện hành động đáng ngờ
> - Compliance: Đáp ứng yêu cầu kiểm toán
> - Troubleshooting: Debug khi có vấn đề
> - Accountability: Trách nhiệm rõ ràng"

---

### Scene 9: Financial Reports & Analytics (4 phút)

**[Navigate to /platform/analytics]**

> "Cuối cùng là phần quan trọng nhất của Admin: Báo cáo tài chính.
> Hệ thống cung cấp các metrics tổng quan về User Growth, Classes, Revenue.
> 
> Về luồng tài chính, dữ liệu giờ đây được tách biệt minh bạch thành 2 dòng: IN (Nạp tiền, Phí Sàn, Cọc) và OUT (Rút tiền, Hoàn tiền).
> Bảng Transaction Breakdown bên dưới thống kê số lượng và tổng tiền của từng loại giao dịch, kèm phân loại IN/OUT bằng màu sắc rõ ràng."

**[Click Export CSV 'Cashflow' & 'Transaction Breakdown']**

> "Hệ thống hỗ trợ xuất dữ liệu ra file CSV tương ứng với bộ lọc thời gian đang chọn.
> Mình sẽ tải thử Cashflow và Transaction Breakdown."

**[Mở CSV files]**

> "Như bạn thấy, file tải về khớp chuẩn format, UTF-8, có thể dùng ngay cho Excel."

---

> "Phần Summary hiển thị metrics 6 tháng gần nhất:
> 
> - Total Revenue: Tổng doanh thu từ tất cả classes
> - Platform Fee Revenue: Phí nền tảng thu được (= Total Revenue × PLATFORM_FEE_RATE)
> - Total Users by month: User growth trend
> - Total Classes by month: Class volume trend
> 
> Và các chỉ số business intelligence:
> - Verification Conversion Rate: % tutors hoàn tất verification sau khi apply
> - Dispute Rate: % contracts có tranh chấp
> - Contract Completion Rate: % contracts reach COMPLETED status"

**[Show monthly breakdown]**

> "Monthly breakdown cho thấy trend rõ ràng:
> 
> - Tháng 3/2026: Revenue 50M, 120 users, 30 classes
> - Tháng 4/2026: Revenue 65M, 150 users, 38 classes
> - Tháng 5/2026: Revenue 72M, 180 users, 42 classes
> - ...
> 
> Growth tốt và ổn định."

**[Demo CSV export - Users]**

> "Admin có thể export data ra CSV để phân tích sâu hơn.
> 
> Click 'Export Users CSV'..."

**[Download and open CSV]**

> "File users_20260808.csv đã download.
> 
> Mở file ra, có columns:
> - userId, email, fullName, role, status, createdAt, emailVerified, phoneVerified
> 
> Admin có thể dùng Excel/Python để phân tích thêm."

**[Export Classes CSV]**

> "Tương tự, export Classes CSV..."

**[Show Classes CSV]**

> "File classes_20260808.csv:
> - classId, subject, grade, tutorName, fee, status, studentCount, createdAt, completedAt
> 
> Useful để phân tích:
> - Subject nào phổ biến nhất
> - Grade level nào có demand cao
> - Tutor nào active nhất"

**[Export Revenue CSV]**

> "Và quan trọng nhất: Revenue CSV..."

**[Show Revenue CSV]**

> "File revenue_20260808.csv:
> - month, totalRevenue, platformFeeRevenue, tutorRevenue, classCount, averageFeePerClass
> 
> Đây là data để làm financial reports, investor presentations, business planning."

**[Explain metrics]**

> "Giải thích một số metrics:
> 
> - Verification Conversion Rate: Nếu 100 tutors apply mà chỉ 70 hoàn tất verify thì rate = 70%
>   → Cần improve verification UX
> 
> - Dispute Rate: Nếu 100 contracts mà có 5 disputes thì rate = 5%
>   → Cần cải thiện matching, contract terms
> 
> - Contract Completion Rate: Nếu 100 contracts mà 85 reach COMPLETED thì rate = 85%
>   → Còn 15% CANCELLED hoặc FAILED, cần investigate why"

---

## 🎬 OUTRO (1 phút)

**[Quay lại desktop hoặc homepage]**

> "Vậy là chúng ta đã demo xong:
> 
> **BF-09: Customer Support**
> - Browse FAQ: Public knowledge base
> - AI Chatbot: RAG-powered assistant với rich references
> - Support Tickets: User request support, admin respond với SLA tracking
> - FAQ Management: Admin CRUD với publish control
> 
> **BF-10: Platform Administration**
> - Dashboard: Real-time KPIs và alerts
> - System Parameters: Config platform fees và settings
> - Audit Logs: Comprehensive audit trail với JSON diff
> - Financial Reports: 6-month analytics và CSV export
> 
> Tất cả tính năng đều:
> ✅ Có audit logging
> ✅ RBAC security (role-based access)
> ✅ Real-time updates
> ✅ Responsive UI
> ✅ Error handling
> 
> Cảm ơn các bạn đã xem! Nếu có câu hỏi về implementation hoặc architecture, hãy để lại comment."

**[Fade out]**

---

## 📝 Notes for Recording

### Pace
- Speak **medium pace** (không quá nhanh)
- Pause 1-2 giây sau mỗi action để viewer catch up
- Highlight key terms: "Đây là điểm quan trọng..."

### Pointing
- Use mouse cursor to highlight UI elements
- Circle important areas (if using screen annotation tool)
- Zoom in nếu cần (Ctrl + Mouse Wheel)

### Tone
- Professional but friendly
- Explain technical terms: "RAG - Retrieval Augmented Generation nghĩa là..."
- Avoid jargon overload

### Recovery
- Nếu có error: "Okay, chúng ta retry..."
- Nếu slow loading: "Đang load data từ server..."
- Nếu forget step: "Quay lại một chút để mình show..."

---

## ⏱️ Timing Breakdown

| Section | Duration | Cumulative |
|---------|----------|------------|
| Intro | 0:30 | 0:30 |
| BF-09: Browse FAQ | 2:00 | 2:30 |
| BF-09: AI Chatbot | 3:00 | 5:30 |
| BF-09: User Tickets | 3:00 | 8:30 |
| BF-09: Admin Tickets | 4:00 | 12:30 |
| BF-09: Admin FAQ | 3:00 | 15:30 |
| BF-10: Dashboard | 2:00 | 17:30 |
| BF-10: Platform Fees | 2:00 | 19:30 |
| BF-10: Audit Logs | 3:00 | 22:30 |
| BF-10: Financial Reports | 4:00 | 26:30 |
| Outro | 1:00 | 27:30 |
| **Buffer** | 2:30 | **30:00** |

Total: **~30 minutes**

---

Good luck with your recording! 🎥✨
