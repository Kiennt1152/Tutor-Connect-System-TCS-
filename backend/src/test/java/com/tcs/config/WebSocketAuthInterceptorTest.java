package com.tcs.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.CustomUserDetailsService;
import com.tcs.security.JwtService;
import com.tcs.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private MessageChannel messageChannel;
    @InjectMocks private WebSocketAuthInterceptor interceptor;

    @Test
    void subscribe_AllowsParticipant() {
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(12L, 5L))
                .thenReturn(true);

        assertDoesNotThrow(() -> interceptor.preSend(subscribeMessage(12L, 5L), messageChannel));
    }

    @Test
    void subscribe_RejectsNonParticipant() {
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(12L, 5L))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(subscribeMessage(12L, 5L), messageChannel));
    }

    private Message<byte[]> subscribeMessage(Long conversationId, Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("member@example.com");
        UserPrincipal principal = new UserPrincipal(user, UserRole.CLIENT);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversation/" + conversationId);
        accessor.setUser(authentication);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
