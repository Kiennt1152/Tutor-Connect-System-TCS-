package com.tcs.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            verify(sessionRepository).deleteById(SESSION_ID);
        }

        @Test
        @DisplayName("UTCID02 (A) - Phiên không tồn tại -> không có gì để xóa, không lỗi")
        void utcid02_sessionNotFound() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            service.deleteSession(SESSION_ID, OWNER_ID);

            verify(sessionRepository).deleteById(SESSION_ID);
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
        @DisplayName("UTCID02 (N) - Phiên chưa có tin nhắn -> trả danh sách rỗng")
        void utcid02_emptySession() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(SESSION_ID, OWNER_ID)));
            when(messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of());

            assertTrue(service.getSessionMessages(SESSION_ID, OWNER_ID).isEmpty());
        }

        /**
         * UTCID03 (A) - DEF-07.
         * Đặc tả: chỉ chủ phiên đọc được lịch sử chat của mình.
         * Thực tế: hàm chỉ truy vấn theo sessionId, tham số userId không được dùng.
         */
        @Test
        @DisplayName("UTCID03 (A) - Người khác đọc phiên không phải của mình -> phải bị từ chối [DEF-07]")
        void utcid03_strangerCannotRead() {
            AiChatSession s = session(SESSION_ID, OWNER_ID);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));
            when(messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(SESSION_ID))
                    .thenReturn(List.of(message(1L, s, "user", "So dien thoai cua toi la 0900000000")));

            assertThrows(RuntimeException.class,
                    () -> service.getSessionMessages(SESSION_ID, STRANGER_ID),
                    "Lịch sử chat của user " + OWNER_ID + " phải được bảo vệ; "
                            + "thực tế user " + STRANGER_ID + " đọc được toàn bộ nội dung");
        }

        /**
         * UTCID04 (A) - DEF-07 (khách chưa đăng nhập).
         */
        @Test
        @DisplayName("UTCID04 (A) - Khách chưa đăng nhập đọc phiên của người dùng -> phải bị từ chối [DEF-07]")
        void utcid04_guestCannotRead() {
            AiChatSession s = session(SESSION_ID, OWNER_ID);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));
            when(messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(SESSION_ID))
                    .thenReturn(List.of(message(1L, s, "user", "Noi dung rieng tu")));

            assertThrows(RuntimeException.class,
                    () -> service.getSessionMessages(SESSION_ID, null),
                    "Khách chưa đăng nhập phải bị chặn khi đọc phiên của người khác");
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
         * UTCID03 (A) - DEF-08.
         * Đặc tả (sheet getUserSessions, UTCID02 bản gốc): userId = null -> trả tối đa 20 phiên gần nhất.
         * Repository đã có sẵn findTop20ByOrderByUpdatedAtDesc() nhưng service không bao giờ gọi;
         * nó vẫn truy vấn theo user_id = null nên luôn ra rỗng.
         */
        @Test
        @DisplayName("UTCID03 (A) - userId = null -> phải trả tối đa 20 phiên gần nhất [DEF-08]")
        void utcid03_guestGetsRecentSessions() {
            when(sessionRepository.findByUserIdOrderByUpdatedAtDesc(null)).thenReturn(List.of());
            when(sessionRepository.findTop20ByOrderByUpdatedAtDesc())
                    .thenReturn(List.of(session(3L, null), session(4L, null)));

            var result = service.getUserSessions(null);

            assertEquals(2, result.size(),
                    "Khách phải nhận 20 phiên gần nhất qua findTop20ByOrderByUpdatedAtDesc(); "
                            + "thực tế service truy vấn user_id = null nên trả về rỗng");
        }
    }
}
