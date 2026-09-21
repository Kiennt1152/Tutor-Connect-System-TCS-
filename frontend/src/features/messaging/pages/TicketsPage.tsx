/**
 * ============================================================================
 * [BF-09] QUẢN LÝ & GỬI YÊU CẦU HỖ TRỢ NGƯỜI DÙNG (USER SUPPORT TICKETS PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Cho phép người dùng tạo phiếu yêu cầu hỗ trợ (Support Ticket) khi gặp sự cố vận hành hoặc khiếu nại.
 *   - Theo dõi tiến độ tiếp nhận và phản hồi từ nhân sự chăm sóc khách hàng của nền tảng.
 * 
 * Chức năng chính:
 *   1. Danh sách ticket của tôi: Theo dõi các yêu cầu đã gửi kèm trạng thái (Đang chờ, Đang xử lý, Đã giải quyết).
 *   2. Tạo ticket mới: Điền tiêu đề, chọn danh mục vấn đề, mô tả chi tiết và đính kèm bằng chứng tệp tin.
 *   3. Xem tiến trình xử lý: Đọc nội dung phản hồi giải quyết của Quản trị viên sàn.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Người dùng truy cập trang, hệ thống tải danh sách các ticket đã gửi của tài khoản.
 *   - Bước 2: Nhấn "Tạo yêu cầu mới", điền biểu mẫu và tải lên ảnh chứng minh sự cố nếu có.
 *   - Bước 3: Gửi ticket lên hệ thống, nhận mã số tra cứu và trạng thái tiếp nhận.
 *   - Bước 4: Nhận thông báo khi Quản trị viên phản hồi và đóng phiếu hỗ trợ.
 * ============================================================================
 */

import { MessagingPanel } from '../components/MessagingPanel';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';

export default function TicketsPage() {
  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="msg-page" style={{ minHeight: 'calc(100vh - 70px)' }}>
        <div className="msg-page__body">
          <MessagingPanel />
        </div>
      </div>
    </div>
  );
}
