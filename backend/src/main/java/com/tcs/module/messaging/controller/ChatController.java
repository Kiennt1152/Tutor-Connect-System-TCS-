package com.tcs.module.messaging.controller;

import com.tcs.module.messaging.dto.request.AddGroupMembersRequest;
import com.tcs.module.messaging.dto.request.CreateGroupRequest;
import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.dto.request.StartConversationRequest;
import com.tcs.module.messaging.dto.request.TransferGroupOwnerRequest;
import com.tcs.module.messaging.dto.request.UpdateGroupRequest;
import com.tcs.module.messaging.dto.response.ConversationResponse;
import com.tcs.module.messaging.dto.response.GroupMemberResponse;
import com.tcs.module.messaging.dto.response.MessageResponse;
import com.tcs.module.messaging.dto.response.UserSummaryResponse;
import com.tcs.module.messaging.service.ChatService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * [UC-50] [BF-09] TIN NHẮN TỨC THỜI & HỘI THOẠI TRỰC TUYẾN (CHAT CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-04
 * 
 * Mô tả Use Case:
 *   - Kênh trao đổi thông tin trực tiếp và tức thì giữa Phụ huynh, Học sinh, Gia sư và Trung tâm.
 *   - Đảm bảo an toàn giao dịch nhờ việc lưu vết và liên kết hội thoại với các lớp học và hợp đồng.
 * 
 * Chức năng chính:
 *   1. Quản lý danh sách hội thoại: Lấy danh sách cuộc trò chuyện cá nhân và nhóm người dùng đang tham gia.
 *   2. Khởi tạo hội thoại: Bắt đầu hội thoại 1-1 hoặc hội thoại theo ngữ cảnh nghiệp vụ (Lớp học, Hợp đồng).
 *   3. Quản lý phòng chat nhóm: Tạo nhóm, cập nhật tên nhóm, thêm/xóa thành viên, chuyển quyền sở hữu và rời nhóm.
 *   4. Phân trang tin nhắn và gửi tin: Tải lịch sử tin nhắn theo trang, gửi tin nhắn văn bản mới.
 *   5. Đánh dấu đã đọc: Cập nhật mốc thời gian đọc tin nhắn gần nhất của thành viên.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Người dùng truy vấn danh sách hội thoại (`getMyConversations`).
 *   - Bước 2: Mở hoặc tạo hội thoại mới với người dùng đích (`startOrGetConversation`).
 *   - Bước 3: Lấy danh sách tin nhắn theo phân trang (`getMessages`) và gọi API đánh dấu đã đọc (`markAsRead`).
 *   - Bước 4: Gửi tin nhắn mới (`sendMessage`), hệ thống lưu tin và kích hoạt thông báo cho các thành viên.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * [UC-33]: Lấy danh sách toàn bộ các cuộc trò chuyện (1-1 và nhóm) của người dùng hiện tại.
     * 
     * @return Danh sách cuộc hội thoại {@link ConversationResponse} kèm tin nhắn mới nhất và số tin chưa đọc
     */
    @GetMapping("/conversations")
    public List<ConversationResponse> getMyConversations() {
        return chatService.getMyConversations();
    }

    /**
     * [UC-33]: Mở cuộc trò chuyện trực tiếp 1-1 với một người dùng đích (tự động tạo mới nếu chưa tồn tại).
     * 
     * @param request Dữ liệu yêu cầu chứa targetUserId {@link StartConversationRequest}
     * @return {@link ConversationResponse} Thông tin phòng chat trực tiếp giữa 2 người
     */
    @PostMapping("/conversations")
    public ConversationResponse startOrGetConversation(@RequestBody StartConversationRequest request) {
        return chatService.startOrGetConversation(request.getTargetUserId());
    }

    /**
     * [UC-34]: Tạo một nhóm chat mới với danh sách thành viên ban đầu.
     * 
     * @param request Dữ liệu nhóm gồm tên nhóm và danh sách ID thành viên {@link CreateGroupRequest}
     * @return {@link ConversationResponse} Thông tin nhóm chat vừa khởi tạo
     */
    @PostMapping("/groups")
    public ConversationResponse createGroup(@RequestBody CreateGroupRequest request) {
        return chatService.createGroup(request.getName(), request.getMemberIds());
    }

    /**
     * [UC-34]: Lấy danh sách thành viên hiện tại của một nhóm chat.
     * 
     * @param conversationId ID phòng chat nhóm
     * @return Danh sách thành viên {@link GroupMemberResponse} kèm vai trò trong nhóm (Owner, Member)
     */
    @GetMapping("/groups/{id}/members")
    public List<GroupMemberResponse> getGroupMembers(@PathVariable("id") Long conversationId) {
        return chatService.getGroupMembers(conversationId);
    }

    /**
     * [UC-34]: Đổi tên hiển thị của phòng chat nhóm.
     * 
     * @param conversationId ID phòng chat nhóm
     * @param request Tên mới của nhóm {@link UpdateGroupRequest}
     * @return {@link ConversationResponse} Thông tin nhóm sau khi cập nhật tên
     */
    @PatchMapping("/groups/{id}")
    public ConversationResponse renameGroup(
            @PathVariable("id") Long conversationId, @RequestBody UpdateGroupRequest request) {
        return chatService.renameGroup(conversationId, request.getName());
    }

    /**
     * [UC-34]: Thêm một hoặc nhiều thành viên mới vào phòng chat nhóm.
     * 
     * @param conversationId ID phòng chat nhóm
     * @param request Danh sách ID người dùng cần thêm vào nhóm {@link AddGroupMembersRequest}
     * @return {@link ConversationResponse} Thông tin nhóm sau khi bổ sung thành viên
     */
    @PostMapping("/groups/{id}/members")
    public ConversationResponse addGroupMembers(
            @PathVariable("id") Long conversationId, @RequestBody AddGroupMembersRequest request) {
        return chatService.addGroupMembers(conversationId, request.getMemberIds());
    }

    /**
     * [UC-34]: Xóa một thành viên ra khỏi phòng chat nhóm (chỉ trưởng nhóm mới có quyền thực hiện).
     * 
     * @param conversationId ID phòng chat nhóm
     * @param userId ID thành viên bị mời ra khỏi nhóm
     * @return {@link ResponseEntity} Trạng thái 204 No Content
     */
    @DeleteMapping("/groups/{id}/members/{userId}")
    public ResponseEntity<Void> removeGroupMember(
            @PathVariable("id") Long conversationId, @PathVariable Long userId) {
        chatService.removeGroupMember(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * [UC-34]: Chuyển quyền trưởng nhóm (Group Owner) cho một thành viên khác trong nhóm.
     * 
     * @param conversationId ID phòng chat nhóm
     * @param request Thông tin ID chủ sở hữu mới {@link TransferGroupOwnerRequest}
     * @return {@link ConversationResponse} Thông tin nhóm sau khi chuyển quyền sở hữu
     */
    @PatchMapping("/groups/{id}/owner")
    public ConversationResponse transferGroupOwner(
            @PathVariable("id") Long conversationId, @RequestBody TransferGroupOwnerRequest request) {
        return chatService.transferGroupOwner(conversationId, request.getOwnerUserId());
    }

    /**
     * [UC-34]: Thành viên tự rời khỏi phòng chat nhóm.
     * 
     * @param conversationId ID phòng chat nhóm
     * @return {@link ResponseEntity} Trạng thái 204 No Content
     */
    @DeleteMapping("/groups/{id}/members/me")
    public ResponseEntity<Void> leaveGroup(@PathVariable("id") Long conversationId) {
        chatService.leaveGroup(conversationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * [UC-33]: Mở hoặc tạo hội thoại gắn liền với một ngữ cảnh nghiệp vụ cụ thể (Lớp học hoặc Hợp đồng).
     * 
     * @param contextType Loại ngữ cảnh (CLASS, CONTRACT, RECRUITMENT)
     * @param contextId ID định danh của đối tượng nghiệp vụ tương ứng
     * @return {@link ConversationResponse} Cuộc trò chuyện theo ngữ cảnh
     */
    @GetMapping("/context/{contextType}/{contextId}")
    public ConversationResponse getOrCreateContextConversation(
            @PathVariable("contextType") String contextType,
            @PathVariable("contextId") String contextId) {
        return chatService.getOrCreateContextConversation(contextType, contextId);
    }

    /**
     * [UC-33]: Tải lịch sử tin nhắn của một cuộc hội thoại theo cơ chế phân trang.
     * 
     * @param conversationId ID cuộc trò chuyện
     * @param page Số trang lịch sử tin nhắn (mặc định 0)
     * @param size Số tin nhắn mỗi trang (mặc định 30)
     * @return {@link Page} Danh sách tin nhắn {@link MessageResponse} phân trang
     */
    @GetMapping("/conversations/{id}/messages")
    public Page<MessageResponse> getMessages(
            @PathVariable("id") Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return chatService.getMessages(conversationId, page, size);
    }

    /**
     * [UC-33]: Gửi một tin nhắn văn bản mới vào phòng chat thông qua REST API.
     * 
     * @param conversationId ID cuộc trò chuyện nhận tin
     * @param request Nội dung tin nhắn và tệp đính kèm nếu có {@link SendMessageRequest}
     * @return {@link MessageResponse} Tin nhắn vừa được lưu thành công
     */
    @PostMapping("/conversations/{id}/messages")
    public MessageResponse sendMessageViaRest(@PathVariable("id") Long conversationId, @RequestBody SendMessageRequest request) {
        request.setConversationId(conversationId);
        return chatService.sendMessage(request);
    }

    /**
     * [UC-33]: Đánh dấu đã đọc tất cả tin nhắn trong cuộc trò chuyện đến thời điểm hiện tại.
     * 
     * @param conversationId ID cuộc trò chuyện
     * @return Map chứa thông báo xác nhận thành công
     */
    @PostMapping("/conversations/{id}/read")
    public Map<String, String> markAsRead(@PathVariable("id") Long conversationId) {
        chatService.markAsRead(conversationId);
        return Map.of("message", "Đã đánh dấu đã đọc");
    }

    /**
     * [UC-33]: Tìm kiếm danh sách người dùng trong hệ thống để bắt đầu cuộc trò chuyện mới.
     * 
     * @param keyword Từ khóa tìm kiếm theo tên hoặc email
     * @return Danh sách người dùng rút gọn {@link UserSummaryResponse}
     */
    @GetMapping("/users")
    public List<UserSummaryResponse> listUsers(@RequestParam(required = false) String keyword) {
        return chatService.listUsers(keyword);
    }

    /**
     * STOMP: client gui toi /app/chat.send. ChatService.sendMessage() se broadcast
     * ket qua toi /topic/conversation/{conversationId} sau khi luu DB.
     */
    @MessageMapping("/chat.send")
    public void sendMessageViaStomp(@Payload SendMessageRequest request) {
        chatService.sendMessage(request);
    }
}
