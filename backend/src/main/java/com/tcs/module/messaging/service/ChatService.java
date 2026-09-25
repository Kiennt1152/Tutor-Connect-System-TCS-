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

    /**
     * [BF-09] Lấy danh sách toàn bộ các cuộc hội thoại (1-1 và nhóm) mà người dùng hiện tại tham gia.
     * 
     * @return Danh sách ConversationResponse kèm thông tin tin nhắn mới nhất và số lượng tin nhắn chưa đọc
     */
    List<ConversationResponse> getMyConversations();

    /**
     * [BF-09] Bắt đầu hoặc lấy lại cuộc trò chuyện trực tiếp 1-1 giữa người dùng hiện tại và người dùng đích.
     * 
     * Luồng xử lý:
     * 1. Kiểm tra không cho phép tự chat trực tiếp với chính mình.
     * 2. Kiểm tra chế tài cấm chat qua PenaltyAccessService.requireFeature(userId, "CHAT").
     * 3. Tìm cuộc trò chuyện trực tiếp sẵn có giữa 2 bên.
     * 4. Nếu chưa tồn tại, khởi tạo mới Conversation loại DIRECT kèm 2 thành viên và trả về thông tin.
     * 
     * @param targetUserId Định danh người dùng muốn trò chuyện
     * @return ConversationResponse thông tin cuộc trò chuyện 1-1
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy người dùng đích
     * @throws IllegalArgumentException nếu tự chat với chính mình
     * @throws com.tcs.exception.ForbiddenException nếu người dùng đang chịu chế tài cấm chat
     */
    ConversationResponse startOrGetConversation(Long targetUserId);

    /**
     * [BF-09] Tạo mới một cuộc trò chuyện nhóm (Group chat).
     * 
     * Luồng xử lý:
     * 1. Kiểm tra chế tài cấm chat của người tạo nhóm.
     * 2. Chuẩn hóa danh sách thành viên (bắt buộc tối thiểu 2 người tham gia ngoài trưởng nhóm).
     * 3. Khởi tạo Conversation loại GROUP với người gọi là chủ sở hữu (OWNER).
     * 4. Gắn các thành viên khác vào nhóm với vai trò MEMBER.
     * 
     * @param name Tên nhóm trò chuyện
     * @param memberIds Danh sách định danh người dùng tham gia nhóm
     * @return ConversationResponse thông tin nhóm trò chuyện vừa tạo
     */
    ConversationResponse createGroup(String name, List<Long> memberIds);

    /**
     * [BF-09] Lấy danh sách thành viên chi tiết trong một cuộc trò chuyện nhóm.
     * 
     * @param conversationId Định danh cuộc trò chuyện
     * @return Danh sách GroupMemberResponse kèm vai trò trong nhóm (OWNER, MEMBER)
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy hội thoại
     * @throws com.tcs.exception.ForbiddenException nếu người gọi không phải thành viên nhóm
     */
    List<GroupMemberResponse> getGroupMembers(Long conversationId);

    /**
     * [BF-09] Đổi tên nhóm trò chuyện (chỉ dành cho chủ sở hữu hoặc thành viên có quyền).
     * 
     * @param conversationId Định danh nhóm trò chuyện
     * @param name Tên nhóm mới
     * @return ConversationResponse thông tin nhóm sau khi đổi tên
     */
    ConversationResponse renameGroup(Long conversationId, String name);

    /**
     * [BF-09] Thêm thành viên mới vào cuộc trò chuyện nhóm.
     * 
     * @param conversationId Định danh nhóm trò chuyện
     * @param memberIds Danh sách ID người dùng cần thêm vào nhóm
     * @return ConversationResponse thông tin nhóm sau khi bổ sung thành viên
     */
    ConversationResponse addGroupMembers(Long conversationId, List<Long> memberIds);

    /**
     * [BF-09] Xóa một thành viên ra khỏi nhóm trò chuyện (chỉ OWNER mới có quyền thực hiện).
     * 
     * @param conversationId Định danh nhóm trò chuyện
     * @param memberUserId Định danh thành viên cần xóa
     * @throws com.tcs.exception.ForbiddenException nếu người gọi không phải OWNER
     */
    void removeGroupMember(Long conversationId, Long memberUserId);

    /**
     * [BF-09] Chuyển giao quyền chủ sở hữu (OWNER) nhóm trò chuyện cho thành viên khác.
     * 
     * @param conversationId Định danh nhóm trò chuyện
     * @param ownerUserId Định danh thành viên được chuyển quyền OWNER mới
     * @return ConversationResponse thông tin nhóm sau khi chuyển giao quyền
     * @throws com.tcs.exception.ForbiddenException nếu người gọi không phải OWNER hiện tại
     */
    ConversationResponse transferGroupOwner(Long conversationId, Long ownerUserId);

    /**
     * [BF-09] Tự rời khỏi nhóm trò chuyện hiện tại.
     * 
     * @param conversationId Định danh nhóm trò chuyện
     * @throws IllegalStateException nếu OWNER rời nhóm khi vẫn còn các thành viên khác
     */
    void leaveGroup(Long conversationId);

    /**
     * [BF-09] Lấy hoặc tự động khởi tạo cuộc hội thoại theo ngữ cảnh nghiệp vụ (Lớp học, Hợp đồng, Ứng tuyển).
     * 
     * Luồng xử lý:
     * 1. Xác thực tính hợp lệ của contextType (CLASS, CONTRACT, APPLICATION) và contextId.
     * 2. Tìm cuộc hội thoại đã gắn liền với ngữ cảnh này.
     * 3. Nếu chưa có, tự động xác định các bên liên quan từ thực thể nghiệp vụ (Phụ huynh, Gia sư, Trung tâm) và khởi tạo cuộc hội thoại.
     * 
     * @param contextType Loại ngữ cảnh nghiệp vụ
     * @param contextId Định danh thực thể ngữ cảnh
     * @return ConversationResponse thông tin cuộc trò chuyện ngữ cảnh
     */
    ConversationResponse getOrCreateContextConversation(String contextType, String contextId);

    /**
     * [BF-09] Tải lịch sử tin nhắn trong cuộc trò chuyện có phân trang.
     * 
     * @param conversationId Định danh cuộc trò chuyện
     * @param page Số trang truy vấn
     * @param size Kích thước trang
     * @return Trang MessageResponse chứa nội dung tin nhắn và tệp đính kèm
     */
    Page<MessageResponse> getMessages(Long conversationId, int page, int size);

    /**
     * [BF-09] Gửi tin nhắn mới vào cuộc trò chuyện thời gian thực.
     * 
     * Luồng xử lý:
     * 1. Kiểm tra chế tài cấm chat qua PenaltyAccessService.
     * 2. Xác thực người gửi là thành viên của cuộc trò chuyện.
     * 3. Lưu tin nhắn vào CSDL và kích hoạt CircumventionService.inspect để quét từ khóa lách sàn.
     * 4. Gửi thông báo đẩy và cập nhật mốc thời gian hoạt động của hội thoại.
     * 
     * @param request Dữ liệu gửi tin nhắn bao gồm conversationId, content, attachmentUrl
     * @return MessageResponse thông tin tin nhắn vừa gửi
     */
    MessageResponse sendMessage(SendMessageRequest request);

    /**
     * [BF-09] Đánh dấu toàn bộ tin nhắn trong cuộc trò chuyện là đã đọc đối với người dùng hiện tại.
     * 
     * @param conversationId Định danh cuộc trò chuyện
     */
    void markAsRead(Long conversationId);

    /**
     * [BF-09] Tìm kiếm danh sách người dùng trong hệ thống để bắt đầu cuộc trò chuyện mới.
     * 
     * @param keyword Từ khóa tìm kiếm (email hoặc tên)
     * @return Danh sách UserSummaryResponse phù hợp
     */
    List<UserSummaryResponse> listUsers(String keyword);
}
