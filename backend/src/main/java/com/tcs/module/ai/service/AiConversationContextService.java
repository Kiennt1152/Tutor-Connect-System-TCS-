package com.tcs.module.ai.service;

import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * [UC-65] QUẢN LÝ PHIÊN HỘI THOẠI AI (AI CONVERSATION CONTEXT SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Quản lý vòng đời phiên chat của người dùng (tạo mới, lưu vết tin nhắn, đặt tên tiêu đề phiên).
 * 
 * Chức năng chính:
 *   1. Quản lý phiên chat: Tạo mới phiên hội thoại hoặc tìm nạp phiên hiện tại theo userId.
 *   2. Lưu trữ tin nhắn: Lưu vết tin nhắn người dùng và phản hồi của AI vào cơ sở dữ liệu.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận sessionId từ yêu cầu chat hoặc khởi tạo phiên mới nếu chưa có.
 *   - Bước 2: Ghi nhận tin nhắn người dùng vào CSDL.
 *   - Bước 3: Cập nhật thời gian hoạt động cuối cùng của phiên chat.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class AiConversationContextService {

    private final AiChatMessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<AiChatMessage> getHistory(Long sessionId) {
        return messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);
    }
}
