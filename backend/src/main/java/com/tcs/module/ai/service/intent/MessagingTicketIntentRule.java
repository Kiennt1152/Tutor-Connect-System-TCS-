package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

/**
 * ============================================================================
 * [UC-65] QUY TẮC Ý ĐỊNH NHẮN TIN & PHẢN ÁNH HỖ TRỢ (MESSAGING TICKET INTENT RULE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Nhận diện yêu cầu chat trực tiếp với gia sư/học viên và yêu cầu mở vé hỗ trợ Ticket khi gặp sự cố kỹ thuật.
 * 
 * Chức năng chính:
 *   1. Nhận diện nhắn tin: Bắt từ khóa chat với gia sư, nhắn tin trao đổi, phòng chat.
 *   2. Nhận diện gửi yêu cầu hỗ trợ: Bắt từ khóa gửi ticket hỗ trợ, liên hệ ban quản trị.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quét từ khóa nhắn tin hoặc khiếu nại kỹ thuật trong câu hỏi.
 *   - Bước 2: Phân loại sang AiDomain.MESSAGING_TICKET.
 *   - Bước 3: Trả về liên kết màn hình chat /messaging hoặc gửi ticket /help.
 * ============================================================================
 */
@Component
public class MessagingTicketIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        if (containsAny(normalized, "nhan tin voi gia su", "chat voi phu huynh", "nhan tin voi phu huynh", "chat voi", "nhan tin voi", "nhan tin rieng", "chat rieng", "nhan tin")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.MESSAGING_OPEN_HELP, AiIntent.FAQ_SUPPORT, 0.9, "/messages");
        }

        if (containsAny(normalized, "sla", "thoi gian phan hoi sla", "quy dinh sla phan hoi")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_SLA, AiIntent.TICKET_SUPPORT, 0.95, "/messaging/tickets");
        }

        if (containsAny(normalized, "kiem tra trang thai ticket", "trang thai ticket")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_STATUS, AiIntent.TICKET_SUPPORT, 0.9, "/messaging/tickets");
        }

        if (containsAny(normalized, "tao ticket", "gui ticket", "dong ticket", "mo lai ticket", "xem thong bao", "yeu cau ho tro", "ticket",
                "tao phieu", "phieu ho tro", "phieu khieu nai", "gui phieu", "tao phieu khieu nai", "tao phieu ho tro",
                "chua thay cong tien", "chua vao vi", "loi nap tien", "su co nap tien", "chuyen khoan chua thay")) {
            return new ClassificationDetail(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_CREATE, AiIntent.TICKET_SUPPORT, 0.95, "/messaging/tickets");
        }

        return null;
    }
}
