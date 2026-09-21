package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.SupportTicketRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * [UC-65] NGỮ CẢNH TRẠNG THÁI VÉ HỖ TRỢ NGƯỜI DÙNG (TICKET CONTEXT PROVIDER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Cung cấp trạng thái các yêu cầu hỗ trợ (Support Tickets) gần nhất của người dùng phục vụ hỏi đáp tiến độ xử lý.
 * 
 * Chức năng chính:
 *   1. Truy vấn tiến độ vé hỗ trợ: Đọc danh sách các ticket đang mở và phản hồi mới nhất từ nhân viên CSKH.
 *   2. Tạo ngữ cảnh tóm tắt: Đóng gói mã ticket, tiêu đề và trạng thái thành dạng ngắn gọn cho LLM.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Xác định userId của người dùng hiện tại.
 *   - Bước 2: Truy vấn bảng SupportTicket và lấy các yêu cầu hỗ trợ trong vòng 30 ngày.
 *   - Bước 3: Tiêm ngữ cảnh vào prompt giúp AI trả lời chính xác tiến độ giải quyết sự cố.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class AiTicketContextProvider {

    private final SupportTicketRepository ticketRepository;

    public List<AiSourceResponse> getTicketContext(String userRole, Long userId) {
        List<AiSourceResponse> results = new ArrayList<>();
        
        if ("PLATFORM_ADMIN".equals(userRole)) {
            // Get OPEN or IN_PROGRESS tickets
            List<SupportTicket> activeTickets = ticketRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS)
            );
            
            for (SupportTicket t : activeTickets) {
                results.add(AiSourceResponse.builder()
                    .sourceId("TICKET_" + t.getTicketId())
                    .sourceType("TICKET")
                    .title("Ticket #" + t.getTicketId() + " - " + t.getSubject())
                    .snippet("Status: " + t.getStatus() + ", Priority: " + t.getPriority() + "\n" + t.getDescription())
                    .similarity(1.0)
                    .finalScore(1.0)
                    .visibility("ADMIN_ONLY")
                    .build());
            }
        } else if (userId != null) {
            // Get user's tickets
            List<SupportTicket> userTickets = ticketRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
            for (SupportTicket t : userTickets) {
                results.add(AiSourceResponse.builder()
                    .sourceId("TICKET_" + t.getTicketId())
                    .sourceType("TICKET")
                    .title("Ticket của bạn #" + t.getTicketId() + " - " + t.getSubject())
                    .snippet("Trạng thái: " + t.getStatus() + ", Mức độ: " + t.getPriority() + "\n" + t.getDescription())
                    .similarity(1.0)
                    .finalScore(1.0)
                    .visibility("OWNER_PRIVATE")
                    .build());
            }
        }
        
        return results;
    }
}
