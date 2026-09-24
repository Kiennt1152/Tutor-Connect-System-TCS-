package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * [UC-65] KIỂM SOÁT & TRIỆT TIÊU ẢO GIÁC AI (AI HALLUCINATION GUARD SERVICE)
 * ============================================================================
 * * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * * Mô tả Use Case:
 *   - Hậu kiểm toàn diện câu trả lời tổng hợp từ mô hình ngôn ngữ lớn (LLM) trước khi trả về người dùng.
 *   - Triệt tiêu hoàn toàn hiện tượng bịa đặt thông tin (Hallucination) về gia sư, lớp học, học phí và số liệu sàn.
 * * Chức năng chính:
 *   1. Kiểm soát thực thể Gia sư: Quét và loại bỏ tên gia sư, số điện thoại tự bịa không tồn tại trong CSDL.
 *   2. Kiểm soát thực thể Lớp học: Đảm bảo mã lớp học, môn học và mức giá học phí khớp 100% dữ liệu thực tế.
 *   3. Kiểm soát số liệu thống kê: Chặn LLM suy diễn số liệu tài chính hoặc số lượng thành viên trái với ngữ cảnh RAG.
 *   4. Bảo vệ dữ liệu tài chính theo vai trò: Ngăn chặn tiết lộ thông tin số dư ví nếu người dùng chưa định danh đúng quyền.
 *   5. Phản hồi dự phòng thông minh (Fallback): Trả lời lịch sự khi không tìm thấy dữ liệu đối chiếu hợp lệ.
 * * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận câu trả lời thô từ LLM (`applyGuards`) cùng danh sách thực thể thực tế lấy từ CSDL.
 *   - Bước 2: Phân luồng kiểm tra theo ý định hội thoại (`subIntent` tìm kiếm gia sư, lớp học hay hỏi đáp chung).
 *   - Bước 3: Thực thi các bộ lọc chuyên biệt (`guardTutorResponse`, `guardClassResponse`) để đối chiếu thực thể.
 *   - Bước 4: Trả về văn bản đã được làm sạch và an toàn cho giao diện người dùng.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiHallucinationGuardService {

    private final AiHallucinationGuard hallucinationGuard;
    private final AiFallbackService fallbackService;

    /**
     * Apply domain and entity level hallucination guards on LLM generated response.
     */
    public String applyGuards(
        String aiResponseText,
        AiDomain domain,
        AiSubIntent subIntent,
        Map<String, String> entities,
        List<TutorReferenceDto> tutors,
        List<ClassReferenceDto> classes,
        List<AiSourceResponse> allSources,
        String rawMessage,
        String userRole,
        Long userId
    ) {
        if (hallucinationGuard == null || aiResponseText == null) {
            return aiResponseText;
        }

        String noDataMsg = (fallbackService != null && fallbackService.getLevel3NoData(subIntent, entities) != null)
                ? fallbackService.getLevel3NoData(subIntent, entities).message()
                : "Hiện tại hệ thống chưa tìm thấy dữ liệu phù hợp với yêu cầu của bạn.";

        if (subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) {
            return hallucinationGuard.guardTutorResponse(aiResponseText, tutors, noDataMsg);
        } else if (subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) {
            return hallucinationGuard.guardClassResponse(aiResponseText, classes, noDataMsg);
        } else if (subIntent == AiSubIntent.PLATFORM_STATS) {
            return hallucinationGuard.guardStatsResponse(aiResponseText, allSources, noDataMsg);
        } else if (domain == AiDomain.FINANCE_WALLET) {
            String roleReqMsg = (fallbackService != null && fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance") != null)
                    ? fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance").message()
                    : "Chức năng yêu cầu quyền truy cập.";
            String financeGuardResult = hallucinationGuard.guardFinanceResponse(rawMessage, userRole, userId, roleReqMsg);
            if (financeGuardResult != null) {
                return financeGuardResult;
            }
        }

        return aiResponseText;
    }
}
