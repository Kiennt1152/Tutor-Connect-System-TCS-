package com.tcs.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.entity.AiChatSession;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import com.tcs.module.ai.repository.AiChatSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test module AI — cac sheet deleteSession, getSessionMessages, getUserSessions.
 *
 * <p>Ca ba API deu nhan tham so userId. Cac UTCID duoi day viet theo DAC TA
 * (userId dung de gioi han quyen truy cap / chon nguon du lieu).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiServiceImplTest {

    private static final Long OWNER_ID = 10L;
    private static final Long STRANGER_ID = 99L;
    private static final Long SESSION_ID = 500L;

    @Mock private AiChatSessionRepository sessionRepository;
    @Mock private AiChatMessageRepository messageRepository;
    @Mock private com.tcs.module.ai.service.AiReferenceCardService referenceCardService;

    @InjectMocks private AiServiceImpl service;

    private AiChatSession session(Long id, Long userId) {
        AiChatSession s = new AiChatSession();
        s.setSessionId(id);
        s.setUserId(userId);
        s.setTitle("Tim gia su Toan lop 9");
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private AiChatMessage message(Long id, AiChatSession s, String role, String content) {
        AiChatMessage m = new AiChatMessage();
        m.setMessageId(id);
        m.setSession(s);
        m.setRole(role);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    // ===================================================================
    //  Sheet: deleteSession
    // ===================================================================
    @Nested
    @DisplayName("deleteSession")
    class DeleteSession {

        @Test
        @DisplayName("UTCID01 (N) - Chủ phiên xóa phiên của mình -> xóa tin nhắn + phiên")
        void utcid01_ownerDeletesOwnSession() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(SESSION_ID, OWNER_ID)));

            service.deleteSession(SESSION_ID, OWNER_ID);

            verify(messageRepository).deleteBySession_SessionId(SESSION_ID);
            verify(sessionRepository).delete(any(AiChatSession.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - Phiên không tồn tại -> ném ResourceNotFoundException")
        void utcid02_sessionNotFound() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            assertThrows(com.tcs.exception.ResourceNotFoundException.class, () -> service.deleteSession(SESSION_ID, OWNER_ID));
        }

        /**
         * UTCID03 (A) - DEF-06.
         * Đặc tả: userId truyền vào để giới hạn quyền — chỉ chủ phiên được xóa.
         * Thực tế: tham số userId không hề được đọc trong thân hàm.
         */
        @Test
        @DisplayName("UTCID03 (A) - Người khác xóa phiên không phải của mình -> phải bị từ chối [DEF-06]")
        void utcid03_strangerCannotDelete() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(SESSION_ID, OWNER_ID)));

            assertThrows(RuntimeException.class,
                    () -> service.deleteSession(SESSION_ID, STRANGER_ID),
                    "Phiên chat thuộc user " + OWNER_ID + ", user " + STRANGER_ID
                            + " phải bị chặn; thực tế vẫn xóa được");
        }

        /**
         * UTCID04 (A) - DEF-06 (khách chưa đăng nhập).
         * AiController bắt exception khi lấy userId nên khách vãng lai truyền userId = null.
         */
        @Test
        @DisplayName("UTCID04 (A) - Khách chưa đăng nhập (userId = null) xóa phiên -> phải bị từ chối [DEF-06]")
        void utcid04_guestCannotDelete() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(SESSION_ID, OWNER_ID)));

            assertThrows(RuntimeException.class,
                    () -> service.deleteSession(SESSION_ID, null),
                    "Khách chưa đăng nhập phải bị chặn; thực tế vẫn xóa được phiên của người khác");
        }

        @Test
        @DisplayName("UTCID05 (B) - sessionId = null -> trả về ngay, không truy vấn và không xóa gì")
        void utcid05_nullSessionId() {
            service.deleteSession(null, OWNER_ID);

            verify(sessionRepository, never()).findById(org.mockito.ArgumentMatchers.any());
            verify(sessionRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("UTCID06 (B) - phiên có userId = null (phiên khách) -> ai cũng xóa được")
        void utcid06_guestSessionDeletableByAnyone() {
            AiChatSession s = session(SESSION_ID, null);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));

            service.deleteSession(SESSION_ID, STRANGER_ID);

            verify(messageRepository).deleteBySession_SessionId(SESSION_ID);
            verify(sessionRepository).delete(s);
        }
    }

    // ===================================================================
    //  Sheet: getSessionMessages
    // ===================================================================
    @Nested
    @DisplayName("getSessionMessages")
    class GetSessionMessages {

        @Test
        @DisplayName("UTCID01 (N) - Chủ phiên đọc phiên của mình -> trả đủ tin nhắn theo thứ tự")
        void utcid01_ownerReadsOwnSession() {
            AiChatSession s = session(SESSION_ID, OWNER_ID);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));
            when(messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(SESSION_ID))
                    .thenReturn(List.of(
                            message(1L, s, "user", "Tim gia su Toan"),
                            message(2L, s, "assistant", "Ban co the tham khao...")));

            var result = service.getSessionMessages(SESSION_ID, OWNER_ID);

            assertEquals(2, result.size());
            assertEquals("user", result.get(0).getRole());
            assertEquals("assistant", result.get(1).getRole());
        }

        @Test
        @DisplayName("UTCID02 (B) - Phiên chưa có tin nhắn -> trả danh sách rỗng")
        void utcid02_emptySession() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(SESSION_ID, OWNER_ID)));
            when(messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of());

            assertTrue(service.getSessionMessages(SESSION_ID, OWNER_ID).isEmpty());
        }

        /**
         * DEF-07 da duoc sua: ham nay kiem tra quyen so huu truoc khi tra ve tin nhan.
         */
        @Test
        @DisplayName("UTCID03 (A) - Người khác đọc phiên không phải của mình -> ForbiddenException")
        void utcid03_strangerCannotRead() {
            AiChatSession s = session(SESSION_ID, OWNER_ID);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));

            var ex = assertThrows(com.tcs.exception.ForbiddenException.class,
                    () -> service.getSessionMessages(SESSION_ID, STRANGER_ID));
            assertEquals("You do not have permission to view messages in this session", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - sessionId không tồn tại -> ResourceNotFoundException 'Chat session not found'")
        void utcid04_sessionNotFound() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            var ex = assertThrows(com.tcs.exception.ResourceNotFoundException.class,
                    () -> service.getSessionMessages(SESSION_ID, OWNER_ID));
            assertEquals("Chat session not found", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (B) - phiên có userId = null (phiên khách) -> ai cũng đọc được")
        void utcid05_guestSessionReadableByAnyone() {
            AiChatSession s = session(SESSION_ID, null);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));
            when(messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(SESSION_ID))
                    .thenReturn(List.of(message(1L, s, "user", "Cau hoi cong khai")));

            var result = service.getSessionMessages(SESSION_ID, STRANGER_ID);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("UTCID06 (B) - sessionId = null -> trả danh sách rỗng ngay, không truy vấn")
        void utcid06_nullSessionId() {
            assertTrue(service.getSessionMessages(null, OWNER_ID).isEmpty());
            verify(sessionRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        }
    }

    // ===================================================================
    //  Sheet: getUserSessions
    // ===================================================================
    @Nested
    @DisplayName("getUserSessions")
    class GetUserSessions {

        @Test
        @DisplayName("UTCID01 (N) - Có userId -> trả phiên của user, mới nhất trước")
        void utcid01_returnsUserSessions() {
            when(sessionRepository.findByUserIdOrderByUpdatedAtDesc(OWNER_ID))
                    .thenReturn(List.of(session(2L, OWNER_ID), session(1L, OWNER_ID)));

            var result = service.getUserSessions(OWNER_ID);

            assertEquals(2, result.size());
            assertEquals(2L, result.get(0).getSessionId());
        }

        @Test
        @DisplayName("UTCID02 (N) - User chưa có phiên nào -> trả danh sách rỗng")
        void utcid02_noSessions() {
            when(sessionRepository.findByUserIdOrderByUpdatedAtDesc(OWNER_ID)).thenReturn(List.of());

            assertTrue(service.getUserSessions(OWNER_ID).isEmpty());
        }

        /**
         * UTCID03 (A) - khách chưa đăng nhập.
         * Repository có sẵn findTop20ByOrderByUpdatedAtDesc() nhưng service cố ý KHÔNG gọi:
         * 20 phiên gần nhất là hội thoại của những tài khoản khác, trả về sẽ làm lộ nội dung chat.
         * Vì vậy userId = null thoát ngay với danh sách rỗng.
         */
        @Test
        @DisplayName("UTCID03 (A) - userId = null -> trả danh sách rỗng, không lộ hội thoại của người khác")
        void utcid03_guestGetsEmptyList() {
            // Khach chua dang nhap khong duoc doc phien chat cua tai khoan khac,
            // nen service thoat ngay ma khong truy van bang phien.
            var result = service.getUserSessions(null);

            assertTrue(result.isEmpty());
            verify(sessionRepository, never()).findTop20ByOrderByUpdatedAtDesc();
        }
    }
}
