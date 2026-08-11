package com.tcs.config;

import com.tcs.security.CustomUserDetailsService;
import com.tcs.security.JwtService;
import com.tcs.security.UserPrincipal;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Xac thuc JWT tai buoc STOMP CONNECT (WebSocket khong the dung header Authorization
 * qua JwtAuthenticationFilter thong thuong vi HTTP upgrade chi xay ra 1 lan).
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ConversationParticipantRepository conversationParticipantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    var claims = jwtService.parseClaims(token);
                    String email = claims.get("email", String.class);
                    if (email != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        if (userDetails.isEnabled()
                                && userDetails instanceof UserPrincipal principal
                                && principal.getTokenVersion() == jwtService.extractTokenVersion(claims)) {
                            Authentication authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            accessor.setUser(authentication);
                        }
                    }
                } catch (Exception ignored) {
                    // Token khong hop le -> khong set user, ket noi se bi tu choi o buoc sau
                }
            }
            if (accessor.getUser() == null) {
                throw new AccessDeniedException("WebSocket authentication required");
            }
        }

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/conversation/")) {
                if (!(accessor.getUser() instanceof Authentication authentication)
                        || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                    throw new AccessDeniedException("WebSocket authentication required");
                }
                Long conversationId;
                try {
                    conversationId = Long.valueOf(destination.substring("/topic/conversation/".length()));
                } catch (NumberFormatException exception) {
                    throw new AccessDeniedException("Invalid conversation destination");
                }
                if (!conversationParticipantRepository
                        .existsByConversation_ConversationIdAndUser_UserId(
                                conversationId, principal.getUserId())) {
                    throw new AccessDeniedException("Not a conversation participant");
                }
            }
        }

        return message;
    }
}
