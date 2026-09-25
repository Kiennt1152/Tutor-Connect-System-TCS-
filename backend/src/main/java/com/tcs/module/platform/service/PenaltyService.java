package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.dto.response.PagePenaltyResponse;
import com.tcs.module.platform.dto.response.PenaltyResponse;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;

/**
 * ============================================================================
 * [UC-63] QUẢN LÝ & THI HÀNH CHẾ TÀI XỬ PHẠT (PENALTY SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Định nghĩa các nghiệp vụ ban hành và quản lý các quyết định chế tài kỷ luật người dùng vi phạm.
 *   - Bảo vệ trật tự và kỷ cương vận hành của nền tảng kết nối gia sư Tutor Connect.
 * 
 * Chức năng chính:
 *   1. Danh sách chế tài: Phân trang và tra cứu án phạt theo người dùng, trạng thái và loại hình phạt.
 *   2. Ban hành án phạt: Áp dụng các mức chế tài (Cảnh cáo, Cấm chat, Cấm mở lớp, Khóa tài khoản).
 *   3. Thu hồi án phạt: Hủy bỏ hiệu lực án phạt trước thời hạn khi có giải trình hợp lệ.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận yêu cầu xử phạt từ Quản trị viên (`issuePenalty`).
 *   - Bước 2: Kiểm tra ràng buộc và lưu vết án phạt vào CSDL.
 *   - Bước 3: Đồng bộ trạng thái tài khoản người dùng và gửi thông báo kỷ luật.
 *   - Bước 4: Tiếp nhận yêu cầu thu hồi và phục hồi quyền lợi (`revokePenalty`).
 * ============================================================================
 */
public interface PenaltyService {
    /**
     * [UC-63] Tìm kiếm và phân trang danh sách các án phạt người dùng theo bộ lọc đa chiều.
     * 
     * Luồng xử lý:
     * 1. Áp dụng các điều kiện lọc: userId, trạng thái án phạt (status), loại chế tài (type), và nguồn gốc phát sinh (sourceType).
     * 2. Truy vấn CSDL có phân trang và sắp xếp theo ngày ban hành giảm dần.
     * 3. Ánh xạ sang danh sách PenaltyResponse.
     * 
     * @param userId Định danh người dùng bị phạt (hoặc null nếu xem tất cả)
     * @param status Trạng thái án phạt (ACTIVE, EXPIRED, REVOKED)
     * @param type Phân loại chế tài (WARNING, TEMPORARY_BAN, PERMANENT_BAN, FEATURE_RESTRICTION)
     * @param sourceType Nguồn gốc phát sinh (REPORT, CIRCUMVENTION, DISPUTE, TICKET, DIRECT)
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số bản ghi mỗi trang
     * @return PagePenaltyResponse danh sách án phạt phân trang
     */
    PagePenaltyResponse listPenalties(Long userId, UserPenaltyStatus status, UserPenaltyType type, String sourceType, int page, int size);

    /**
     * [UC-63] Tiện ích tra cứu danh sách án phạt phân trang không kèm lọc theo sourceType.
     * 
     * @param userId Định danh người dùng bị phạt
     * @param status Trạng thái án phạt
     * @param type Phân loại chế tài
     * @param page Số trang
     * @param size Số bản ghi mỗi trang
     * @return PagePenaltyResponse danh sách án phạt phân trang
     */
    default PagePenaltyResponse listPenalties(Long userId, UserPenaltyStatus status, UserPenaltyType type, int page, int size) {
        return listPenalties(userId, status, type, null, page, size);
    }

    /**
     * [UC-63] Ban hành quyết định xử phạt kỷ luật người dùng vi phạm quy chế sàn.
     * 
     * Luồng xử lý:
     * 1. Xác thực quyền Quản trị viên của người thực hiện và kiểm tra tính hợp lệ của người bị phạt (không được tự phạt hoặc phạt Admin khác).
     * 2. Xác thực lý do xử phạt (tối thiểu 20 ký tự) và thời hạn hiệu lực phù hợp theo loại hình phạt.
     * 3. Kiểm tra sự tồn tại của thực thể nguồn (Report, Circumvention, Dispute, Ticket).
     * 4. Khởi tạo bản ghi UserPenalty ở trạng thái ACTIVE.
     * 5. Cập nhật trạng thái người dùng thành BANNED nếu thuộc diện cấm tạm thời hoặc vĩnh viễn.
     * 6. Ghi nhận nhật ký kiểm toán ISSUE_PENALTY và gửi thông báo kỷ luật tới người dùng.
     * 
     * @param request Dữ liệu ban hành án phạt
     * @return PenaltyResponse thông tin án phạt vừa được ban hành
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy người dùng hoặc thực thể nguồn
     * @throws IllegalArgumentException nếu thông tin yêu cầu không hợp lệ
     */
    PenaltyResponse issuePenalty(IssuePenaltyRequest request);

    /**
     * [UC-63] Thu hồi hoặc hủy bỏ quyết định xử phạt đang có hiệu lực.
     * 
     * Luồng xử lý:
     * 1. Tìm bản ghi án phạt theo penaltyId và kiểm tra trạng thái phải là ACTIVE.
     * 2. Quản trị viên không thể tự thu hồi án phạt của chính mình.
     * 3. Cập nhật trạng thái án phạt thành REVOKED kèm lý do giải trình thu hồi.
     * 4. Ghi nhận nhật ký kiểm toán REVOKE_PENALTY.
     * 5. Khôi phục trạng thái tài khoản người dùng về ACTIVE nếu không còn án phạt cấm nào khác.
     * 
     * @param penaltyId Định danh án phạt cần thu hồi
     * @param request Dữ liệu lý do thu hồi án phạt
     * @return PenaltyResponse thông tin án phạt sau khi thu hồi
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy án phạt
     * @throws IllegalStateException nếu án phạt không ở trạng thái ACTIVE
     */
    PenaltyResponse revokePenalty(Long penaltyId, RevokePenaltyRequest request);
}
