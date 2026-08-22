package com.tcs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time WebSocket and STOMP Protocol Configuration.
 * <p>
 * Implements real-time messaging for internal chat (FT-18) and real-time notifications (FT-19).
 * Features:
 * <ul>
 *   <li>SockJS fallback endpoint at <code>/ws</code>.</li>
 *   <li>Simple in-memory message broker routing to <code>/topic</code> (broadcasts) and <code>/queue</code> (user-specific).</li>
 *   <li>STOMP heartbeat monitoring (10s interval) for proactive connection liveness.</li>
 *   <li>Inbound channel authentication interceptor validating JWT tokens on STOMP CONNECT frame.</li>
 * </ul>
 *
 * @see com.tcs.config.WebSocketAuthInterceptor
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler);
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
