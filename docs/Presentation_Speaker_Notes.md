# TCS Presentation - Slides 18-25
## Speaker Notes - Phiên bản Nói chuyện

---

# SLIDE 18: Tài khoản Người dùng & Tin Tuyển Dụng

Bây giờ chúng ta sẽ đi vào các thực thể còn lại trong hệ thống.

## Tài khoản Người dùng

Tài khoản có 3 trạng thái chính. **Active** (Hoạt động) là trạng thái mặc định khi user (người dùng) đăng ký thành công - họ có thể đăng nhập, thực hiện giao dịch, và sử dụng đầy đủ tính năng.

**Locked** (Bị khóa) xảy ra khi có 5 lần đăng nhập thất bại liên tiếp - đây là cơ chế chống brute force attack (tấn công brute force - thử password liên tục). Tài khoản sẽ tự động mở khóa sau 30 phút, hoặc admin có thể unlock (mở khóa) thủ công.

**Inactive** (Không hoạt động) là trạng thái kết thúc - có thể do user yêu cầu xóa tài khoản theo GDPR/PDPD, do admin disable (vô hiệu hóa) vì vi phạm, hoặc do không sử dụng trong thời gian dài. Một khi đã Inactive, không có đường quay về Active.

## Tin Tuyển Dụng

Tin tuyển dụng có 3 trạng thái. **Draft** (Bản nháp) là giai đoạn soạn thảo nội bộ - chỉ center nhìn thấy, có thể chỉnh sửa thoải mái. Khi nhấn "Publish" (Xuất bản), tin chuyển sang **Active** (Đang tuyển) - hiển thị công khai, nhận đơn ứng tuyển. Khi tuyển đủ hoặc hết hạn, tin chuyển sang **Closed** (Đã đóng) - đây là terminal state (trạng thái kết thúc), không thể mở lại, phải tạo tin mới.

---

# SLIDE 19: Quy tắc Nghiệp vụ - Dữ liệu & Quy trình

## GB-01: Chuyển đổi trạng thái không thể đảo ngược

Đây là nguyên tắc quan trọng: một số trạng thái CHỈ có thể tiến về phía trước, không thể quay lại.

Tại sao cần quy tắc này? Thứ nhất, bảo vệ tính toàn vẹn business process (quy trình nghiệp vụ) - không thể "un-complete" (hoàn tác) một lớp học đã kết thúc. Thứ hai, compliance pháp lý - transaction (giao dịch) đã hoàn tất không thể undo (hoàn tác). Thứ ba, tạo lòng tin - khi cam kết đã được thực hiện thì không có đường quay lại.

Lấy Class Flow (Luồng lớp học) làm ví dụ: một lớp đi từ được tạo, đã ghép gia sư, đang diễn ra, đến hoàn thành. Khi đến trạng thái "hoàn thành", không có API, không có button (nút), không có admin action (hành động quản trị) nào có thể đưa lớp về trạng thái trước đó. Tương tự với Payment (Thanh toán): pending (đang chờ) → processing (đang xử lý) → completed (đã hoàn tất). Không có cách nào chuyển completed về pending.

Về mặt kỹ thuật, chúng ta enforce (thực thi) quy tắc này ở cả database (cơ sở dữ liệu) và application layer (tầng ứng dụng). Service methods không có "revert" hay "undo" - chỉ có các action tiến về phía trước.

## GB-04: Ràng buộc tính duy nhất

Một số trường phải có giá trị DUY NHẤT trên toàn hệ thống. Bao gồm: Email - primary login identifier (định danh đăng nhập chính), không được trùng; Phone - dùng cho SMS verification (xác thực SMS); Tutor/Center license numbers (số giấy phép); Class code - UUID tự động sinh; Transaction ID - UUID tự động sinh.

Email được lưu lowercase (chữ thường) để đảm bảo uniqueness (tính duy nhất) không phân biệt hoa/thường. Phone được normalize (chuẩn hóa) trước khi check - loại bỏ spaces (dấu cách), dashes (dấu gạch ngang).

---

# SLIDE 20: Quy tắc Nghiệp vụ - Bảo mật & Quyền riêng tư

## GB-03: Bảo mật file CV

CV là tài liệu nhạy cảm nhất - chứa toàn bộ lịch sử cá nhân. Tất cả file CV được mã hóa bằng AES-256 khi lưu trữ - đây là tiêu chuẩn được US Government (Chính phủ Mỹ) và banks (ngân hàng) sử dụng. Master key (khóa chính) được lưu trong HSM (Hardware Security Module - Thiết bị bảo mật phần cứng), keys (khóa) được rotate (xoay vòng) định kỳ 90 ngày.

Về phân quyền: Tutor có toàn quyền với CV của mình. Admin có thể xem để audit (kiểm toán) nhưng không được sửa. Employer (nhà tuyển dụng) chỉ xem được CV của người đã ứng tuyển vào job (công việc) của họ, và chỉ được download sau khi ứng viên grant consent (cấp quyền đồng ý). Guest (khách) không có quyền truy cập.

## GB-02 & GB-06: Ẩn thông tin nội bộ

Một số thông tin CHỈ visible (hiển thị) cho internal staff (nhân viên nội bộ): internal notes (ghi chú nội bộ) từ recruiter (nhân viên tuyển dụng), recruiter rating (điểm đánh giá), screening score (điểm sàng lọc), expected salary (mức lương kỳ vọng), và lead source details (chi tiết nguồn lead). Candidate (ứng viên) không bao giờ thấy những thông tin này.

Tại sao? Thứ nhất, đảm bảo quy trình tuyển dụng công bằng - candidate không bị bias (thiên lệch) bởi internal scoring (chấm điểm nội bộ). Thứ hai, bảo vệ competitive intelligence (thông tin cạnh tranh) - salary ranges (mức lương) và notes không nên leak (rò rỉ). Thứ ba, compliance với GDPR Article 22 về automated decision-making (ra quyết định tự động).

---

# SLIDE 21: Yêu cầu Phi chức năng

Chúng ta có 5 nhóm yêu cầu phi chức năng.

**Performance (Hiệu suất):** Page load (tải trang) dưới 2 giây, search (tìm kiếm) dưới 3 giây, booking completion (hoàn tất đặt lớp) dưới 4 giây. Sử dụng CDN (Mạng phân phối nội dung), caching (bộ nhớ đệm), Elasticsearch (công cụ tìm kiếm), và database optimization (tối ưu hóa CSDL) để đạt targets (mục tiêu) này.

**Availability (Khả dụng):** Target 99.5% uptime - khoảng 3.65 ngày downtime (thời gian chết)/năm. RTO (Recovery Time Objective - Mục tiêu thời gian phục hồi) 30 phút, RPO (Recovery Point Objective - Mục tiêu điểm phục hồi) 24 giờ. Backup strategy (chiến lược sao lưu): daily full backup (sao lưu toàn phần hàng ngày), hourly incremental (sao lưu gia tăng hàng giờ), weekly offsite (sao lưu offsite hàng tuần), 90-day retention (lưu giữ 90 ngày).

**Security (Bảo mật):** Password được hash bằng bcrypt với cost factor 12. JWT access token (mã thông báo truy cập) 15 phút, refresh token (mã làm mới) 7 ngày. Sau 5 lần login (đăng nhập) thất bại, tài khoản bị khóa 30 phút.

**Usability (Khả dụng):** Booking (đặt lớp) trong 5 phút cho user mới. Giao diện hỗ trợ tiếng Việt với date format (định dạng ngày) DD/MM/YYYY, currency (tiền tệ) VND.

**Scalability (Mở rộng):** Horizontal scaling (mở rộng ngang) với auto-scaling (tự động mở rộng) khi CPU > 70%. CDN cho static assets (tài sản tĩnh), Redis cho application cache (bộ nhớ đệm ứng dụng). Target handle 100+ concurrent users (người dùng đồng thời).

---

# SLIDE 22: Rủi ro & Biện pháp Phòng ngừa (1/2)

## Rủi ro mức CAO

**R-01: Thay đổi Facebook API** - Facebook có lịch sử deprecated API (API bị loại bỏ) thường xuyên. Với 30-50% leads (khách hàng tiềm năng) đến từ Facebook, đây là rủi ro nghiêm trọng. Mitigation (biện pháp giảm thiểu): monitor (theo dõi) API changelog (nhật ký thay đổi), giữ code modular (mã nguồn modular), maintain (duy trì) CSV import fallback (phương án dự phòng).

**R-02: Vi phạm Quyền riêng tư** - Theo PDPD (Nghị định 13/2023/NĐ-CP) và GDPR. Vi phạm có thể bị phạt đến 5% annual revenue (doanh thu hàng năm) hoặc VND 100-200 triệu. Mitigation: AES-256 encryption (mã hóa), RBAC (Role-Based Access Control - Kiểm soát truy cập theo vai trò), privacy impact assessment (đánh giá tác động quyền riêng tư) định kỳ, data retention policy (chính sách lưu giữ dữ liệu) rõ ràng.

**R-03: Payment Gateway Downtime (Sự cố cổng thanh toán)** - Nếu Stripe/PayOS down (ngừng hoạt động), không có giao dịch nào được xử lý. Mitigation: multi-provider architecture (kiến trúc đa nhà cung cấp) với Stripe primary (chính), PayOS backup (dự phòng), retry mechanism (cơ chế thử lại) với exponential backoff (lùi theo cấp số nhân), user communication (giao tiếp người dùng) khi có issues (sự cố).

## Rủi ro mức TRUNG BÌNH

**R-04: Scope Creep (Trôi phạm vi)** - Yêu cầu mới liên tục xuất hiện. Mitigation: Change Control Board (Ban kiểm soát thay đổi) đánh giá mọi request (yêu cầu), MoSCoW prioritization (ưu tiên theo MoSCoW: Must-Should-Could-Won't).

**R-05: Thành viên Nghỉ việc** - Mất knowledge (kiến thức) quan trọng. Mitigation: documentation culture (văn hóa tài liệu), mandatory code review (đánh giá code bắt buộc), offboarding checklist (danh sách kiểm tra khi nghỉ việc).

**R-06: Database Performance (Hiệu suất CSDL)** - Slow queries (truy vấn chậm), deadlocks (khoá chết). Mitigation: index optimization (tối ưu chỉ mục), query audit (kiểm tra truy vấn), connection pooling (ghép nối kết nối).

---

# SLIDE 23: Rủi ro & Biện pháp Phòng ngừa (2/2)

## Monitoring Stack (Ngăn xếp giám sát)

Chúng ta có monitoring stack toàn diện: Prometheus thu thập metrics (chỉ số), Grafana visualize (trực quan hóa) trong dashboards (bảng điều khiển), PagerDuty/OpsGenie handle (xử lý) alerting (cảnh báo) và escalation (leo thang), ELK Stack aggregate logs (tổng hợp nhật ký).

## Escalation Matrix (Ma trận leo thang)

4 mức ưu tiên: **P1-Critical (Nghiêm trọng)** (15 phút response (phản hồi), 1 giờ resolve (xử lý)) cho system down, payment broken (thanh toán lỗi); **P2-High (Cao)** (1 giờ response, 4 giờ resolve) cho login broken, major feature broken; **P3-Medium (Trung bình)** (4 giờ response, 24 giờ resolve) cho minor issues (vấn đề nhỏ); **P4-Low (Thấp)** (next business day response) cho cosmetic issues (vấn đề thẩm mỹ).

## Incident Response Process (Quy trình ứng phó sự cố)

6 bước: **Detect** (Phát hiện) → **Triage** (Phân loại ưu tiên) (đánh giá impact (tác động), assign priority (gán ưu tiên)) → **Investigate** (Điều tra) (gather info (thu thập thông tin) từ logs, metrics) → **Mitigate** (Giảm thiểu) (quick fix (sửa nhanh): rollback (quay lại phiên bản trước), disable feature (tắt tính năng), scale (mở rộng)) → **Resolve** (Giải quyết) (confirm (xác nhận), update status (cập nhật trạng thái)) → **Post-Mortem** (Sau sự cố) (48h sau: blameless meeting (họp không đổ lỗi), action items (hành động cần làm)).

---

# SLIDE 24: Đóng góp Kỳ vọng

## Tác động Kinh doanh

**Quản lý Lead:** Từ 20-30 phút/lead (thủ công) xuống 2-3 phút/lead (TCS automation (tự động hóa)) - tiết kiệm 20 giờ/tuần.

**Matching Gia sư:** Từ 2-3 tuần xuống 2-3 ngày - nhanh hơn 60%, chất lượng matches (kết quả ghép) tốt hơn nhờ AI.

**Thanh toán:** Với Escrow (ký quỹ), cả center và tutor đều được bảo vệ 100% - không còn fraud (gian lận) hay dispute (tranh chấp) không có recourse (phương án).

**Tuyển dụng:** Từ 3-4 tuần và VND 5-10 triệu/cuộc tuyển xuống 1 tuần với subscription model (mô hình đăng ký) - nhanh hơn 3 lần, rẻ hơn 50%.

**Dispute Resolution (Giải quyết tranh chấp):** Từ 3-5 ngày xuống dưới 24 giờ - nhanh hơn 90%.

## KPI Mục tiêu

- **DAU (Daily Active Users - Người dùng hoạt động hàng ngày):** 1,000+ sau 12 tháng
- **GMV (Gross Merchandise Value - Tổng giá trị hàng hóa):** 500 triệu VND/tháng (~6 packages (gói)/ngày)
- **Escrow Transactions (Giao dịch ký quỹ):** 200+/tháng (~7 tx (giao dịch)/ngày)
- **Lead Conversion (Chuyển đổi lead):** 25% (từ 8% industry average (trung bình ngành))
- **Tutor Response Time (Thời gian phản hồi gia sư):** < 2 giờ (từ ~24 giờ)
- **User Retention (Giữ chân người dùng):** 70% sau 90 ngày

---

# SLIDE 25: Bước tiếp theo & Lộ trình

## Roadmap (Lộ trình)

**2026 Q2 - v1.0 Launch (Ra mắt):** MVP (Minimum Viable Product - Sản phẩm khả thi tối thiểu) với core features (tính năng cốt lõi) - authentication (xác thực), tutor listing (danh sách gia sư), booking (đặt lớp), payment (thanh toán), escrow (ký quỹ), reviews (đánh giá), notifications (thông báo), admin dashboard (bảng điều khiển admin). Launch criteria (tiêu chí ra mắt): 50 tutors, 10 centers, 100 bookings.

**2026 Q3 - v1.1:** Stabilize (Ổn định) - bug fixes (sửa lỗi), performance optimization (tối ưu hiệu suất), database indexing (lập chỉ mục CSDL), caching (bộ nhớ đệm), CDN.

**2026 Q4 - v1.2:** Scale (Mở rộng) - Kubernetes deployment (triển khai Kubernetes), auto-scaling (tự động mở rộng), load testing (kiểm tra tải), database replication (sao chép CSDL), backup automation (tự động sao lưu).

**2027 Q1 - v2.0 (trọng tâm):** AI v2 + Mobile. Features chính: Personality Matching (Ghép theo tính cách MBTI), Learning Style Compatibility (Tương thích phong cách học), Schedule Optimization AI (AI tối ưu lịch), Predictive Success Score (Điểm dự đoán thành công), Natural Language Search (Tìm kiếm ngôn ngữ tự nhiên), Native Mobile Apps (Ứng dụng di động gốc) (iOS Swift, Android Kotlin).

**2027 Q2 - v2.1:** Video lesson integration (Tích hợp học qua video) + Multi-language (Đa ngôn ngữ) (English, sau đó Khmer, Lao, Thai, Myanmar).

**2027 Q3 - v2.2:** Predictive analytics (Phân tích dự đoán) + Advanced matching algorithms (Thuật toán ghép nâng cao).

## Tóm tắt

Từ MVP trong Q2 năm nay đến nền tảng toàn diện với AI và mobile trong 18 tháng. Mỗi version (phiên bản) đều build (xây dựng) trên nền tảng version trước - đảm bảo stability (ổn định) trong khi vẫn mang lại cải tiến có ý nghĩa.

Cảm ơn các bạn đã lắng nghe. Tôi sẵn sàng trả lời câu hỏi.

---

*Document version: 3.2 - Natural Speaking, Condensed, Vietnamese Notes*
*Last updated: June 2026*
