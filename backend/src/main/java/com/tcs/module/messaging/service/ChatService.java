package com.tcs.module.messaging.service;

import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.dto.response.ConversationResponse;
import com.tcs.module.messaging.dto.response.GroupMemberResponse;
import com.tcs.module.messaging.dto.response.MessageResponse;
import com.tcs.module.messaging.dto.response.UserSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * ============================================================================
 * [BF-09] GIAO TIẾP THỜI GIAN THỰC & QUẢN LÝ HỘI THOẠI (CHAT SERVICE)
 * ============================================================================
 * * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-04
 * * Mô tả Use Case:
 *   - Giao tiếp trực tiếp và theo nhóm giữa Phụ huynh, Học sinh, Gia sư và Trung tâm gia sư.
 *   - Hỗ trợ trò chuyện gắn liền với ngữ cảnh lớp học/hợp đồng nhằm tăng tính minh bạch và an toàn giao dịch.
 * * Chức năng chính:
 *   1. Quản lý danh sách hội thoại cá nhân (1-1) và hội thoại nhóm (Group chat).
 *   2. Thao tác cấu hình nhóm: Tạo nhóm, đổi tên nhóm, thêm/xóa thành viên, nhượng quyền trưởng nhóm, rời nhóm.
 *   3. Lấy và khởi tạo hội thoại theo ngữ cảnh nghiệp vụ (Class, Contract, Application).
 *   4. Phân trang tải lịch sử tin nhắn và gửi tin nhắn mới kèm đính kèm.
 *   5. Đánh dấu đã đọc (Mark as read) và tìm kiếm danh sách người dùng để kết nối trò chuyện.
 * * Luồng xử lý chính:
 *   - Bước 1: Người dùng truy vấn danh sách hội thoại của bản thân (`getMyConversations`).
 *   - Bước 2: Khởi tạo hoặc tìm lại cuộc hội thoại với người dùng đích (`startOrGetConversation`).
 *   - Bước 3: Tải tin nhắn phân trang (`getMessages`) và gửi tin nhắn mới (`sendMessage`).
 *   - Bước 4: Đồng bộ trạng thái đã đọc (`markAsRead`) cập nhật mốc thời gian đọc của thành viên.
 * ============================================================================
 */
public interface ChatService {

    List<ConversationResponse> getMyConversations();

    ConversationResponse startOrGetConversation(Long targetUserId);

    ConversationResponse createGroup(String name, List<Long> memberIds);

    List<GroupMemberResponse> getGroupMembers(Long conversationId);

    ConversationResponse renameGroup(Long conversationId, String name);

    ConversationResponse addGroupMembers(Long conversationId, List<Long> memberIds);

    void removeGroupMember(Long conversationId, Long memberUserId);

    ConversationResponse transferGroupOwner(Long conversationId, Long ownerUserId);

    void leaveGroup(Long conversationId);

    ConversationResponse getOrCreateContextConversation(String contextType, String contextId);

    Page<MessageResponse> getMessages(Long conversationId, int page, int size);

    MessageResponse sendMessage(SendMessageRequest request);

    void markAsRead(Long conversationId);

    List<UserSummaryResponse> listUsers(String keyword);
}
