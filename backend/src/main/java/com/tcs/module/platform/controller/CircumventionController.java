package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.request.ReviewCircumventionRequest;
import com.tcs.module.platform.dto.response.CircumventionEventResponse;
import com.tcs.module.platform.dto.response.CircumventionConversationResponse;
import com.tcs.module.platform.dto.response.PageCircumventionEventResponse;
import com.tcs.module.platform.service.CircumventionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * PHÂN HỆ GIÁM SÁT & PHÁT HIỆN HÀNH VI LÁCH SÀN GIAO DỊCH (CIRCUMVENTION MONITOR)
 * ============================================================================
 * 
 * Mã Use Case: [UC-59] Phát hiện & Ngăn chặn hành vi lách sàn giao dịch
 * Tác giả: mduc1011-swp (Đức)
 * 
 * Nghiệp vụ cốt lõi:
 *   - Lách sàn (Platform Circumvention / Disintermediation) là hành vi người dùng (Gia sư, Phụ huynh, Trung tâm)
 *     chia sẻ số điện thoại, tài khoản ngân hàng, link Zalo/Facebook nhằm giao dịch ngoài sàn để trốn phí
 *     hoặc bỏ qua cơ chế bảo vệ Escrow.
 *   - Hệ thống tự động phân tích tin nhắn realtime (Regex & AI NLP Matching):
 *     * Phát hiện số điện thoại ẩn (dạng chữ hoặc dấu cách: "khong chin mot...", "0 9 1 ...").
 *     * Phát hiện số tài khoản ngân hàng, STK, tên ngân hàng (Vietcombank, MB, Techcombank...).
 *     * Phát hiện từ khóa thỏa thuận chuyển khoản riêng ("ck ngoài", "giao dịch riêng", "bớt phí").
 *   - Tự động sinh bản ghi Cảnh báo sự kiện (Circumvention Event) vào hàng đợi để Quản trị viên thẩm định.
 */
@RestController
@RequestMapping("/api/platform/circumvention-events")
@RequiredArgsConstructor
public class CircumventionController {

    private final CircumventionService service;

    /**
     * [UC-59]: Danh sách các sự kiện nghi vấn lách sàn cần Admin thẩm định.
     * 
     * Bộ lọc:
     *   - {@code status}: PENDING (Chờ xử lý), CONFIRMED (Xác nhận vi phạm), FALSE_POSITIVE (Báo động giả), RESOLVED (Đã giải quyết).
     *   - Phân trang dữ liệu theo thứ tự thời gian phát hiện mới nhất.
     * 
     * @param status Trạng thái xử lý sự kiện
     * @param page Số trang (mặc định 0)
     * @param size Số lượng bản ghi mỗi trang (mặc định 20)
     * @return {@link PageCircumventionEventResponse} Danh sách sự kiện lách sàn kèm thông tin người gửi/nhận
     */
    @GetMapping
    public PageCircumventionEventResponse list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    /**
     * [UC-59]: Admin thẩm định và ra quyết định xử lý sự kiện lách sàn.
     * 
     * Các quyết định (Review Action):
     *   - CONFIRMED: Xác nhận người dùng cố tình lách sàn -> Tự động chuyển tiếp sang Phân hệ Chế tài [UC-60]
     *     để ban hành án phạt (Cảnh cáo, Trừ uy tín, Tạm khóa chat hoặc Khóa tài khoản).
     *   - FALSE_POSITIVE: Xác định báo động giả (nhầm lẫn số liệu hoặc ngữ cảnh hợp lệ).
     *   - DISMISSED: Bỏ qua cảnh báo với ghi chú thẩm định của Admin.
     * 
     * @param eventId ID của sự kiện lách sàn
     * @param request Quyết định của Admin và ghi chú giải trình
     * @return {@link CircumventionEventResponse} Bản ghi sự kiện đã cập nhật kết quả thẩm định
     */
    @PatchMapping("/{eventId}")
    public CircumventionEventResponse review(
            @PathVariable Long eventId,
            @Valid @RequestBody ReviewCircumventionRequest request) {
        return service.review(eventId, request);
    }

    /**
     * [UC-59]: Trích xuất đoạn bằng chứng hội thoại ngữ cảnh chứa tin nhắn vi phạm.
     * 
     * Tính năng an toàn & pháp lý:
     *   - Cung cấp cho Admin xem trọn vẹn ngữ cảnh trước và sau tin nhắn vi phạm trong phòng chat.
     *   - Làm căn cứ pháp lý vững chắc trước khi áp dụng chế tài xử phạt [UC-60].
     * 
     * @param eventId ID của sự kiện lách sàn
     * @return {@link CircumventionConversationResponse} Toàn bộ tin nhắn liên quan trong cuộc trò chuyện
     */
    @GetMapping("/{eventId}/conversation")
    public CircumventionConversationResponse getConversationEvidence(@PathVariable Long eventId) {
        return service.getConversationEvidence(eventId);
    }
}
