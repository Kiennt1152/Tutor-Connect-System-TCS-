package com.tcs.module.messaging.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcs.exception.GlobalExceptionHandler;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.controller.MessagingController;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Tag("report52-it")
@SpringBootTest(
        classes = Report52MessagingNotificationApiDbITTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:tcs_msg_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never"
        })
class Report52MessagingNotificationApiDbITTest {

    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private MessagingController messagingController;

    @jakarta.annotation.Resource
    private NotificationRepository notificationRepository;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @BeforeEach
    void setUpMockMvcAndCleanDatabase() {
        mockMvc = MockMvcBuilders.standaloneSetup(messagingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void IT_MSG_010_MarkNotificationAsReadUpdatesOnlyClickedNotificationThroughApiAndDb() throws Exception {
        User owner = userRepository.saveAndFlush(user("it-msg-010-owner@tcs.test", "0900001010"));
        User otherUser = userRepository.saveAndFlush(user("it-msg-010-other@tcs.test", "0900001011"));
        Notification clicked = notificationRepository.saveAndFlush(
                notification(owner, "Đã nhận tiền giải ngân", "WALLET", 10L));
        Notification stillUnread = notificationRepository.saveAndFlush(
                notification(owner, "Có tin nhắn mới", "CHAT", 20L));
        Notification otherUsersNotification = notificationRepository.saveAndFlush(
                notification(otherUser, "Thông báo của người khác", "WALLET", 30L));

        authenticate(owner, UserRole.TUTOR);

        mockMvc.perform(patch("/api/messaging/notifications/{notificationId}/read", clicked.getNotificationId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã đánh dấu đã đọc"));

        mockMvc.perform(get("/api/messaging/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        Notification clickedAfterRead = notificationRepository.findById(clicked.getNotificationId()).orElseThrow();
        Notification untouchedOwnerNotification =
                notificationRepository.findById(stillUnread.getNotificationId()).orElseThrow();
        Notification untouchedOtherNotification =
                notificationRepository.findById(otherUsersNotification.getNotificationId()).orElseThrow();

        assertTrue(clickedAfterRead.getIsRead());
        assertNotNull(clickedAfterRead.getReadAt());
        assertFalse(untouchedOwnerNotification.getIsRead());
        assertFalse(untouchedOtherNotification.getIsRead());
    }

    private User user(String email, String phone) {
        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash("unused-in-notification-it");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Notification notification(User user, String title, String referenceType, Long referenceId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle(title);
        notification.setContent(title + " - nội dung kiểm thử.");
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
        return notification;
    }

    private void authenticate(User user, UserRole role) {
        UserPrincipal principal = new UserPrincipal(user, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.tcs")
    @EnableJpaRepositories(basePackageClasses = {NotificationRepository.class, UserRepository.class})
    @Import({
            MessagingController.class,
            MessagingServiceImpl.class,
            AuthHelper.class,
            TestBeans.class
    })
    static class TestApplication {
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        SupportTicketRepository supportTicketRepository() {
            return mock(SupportTicketRepository.class);
        }

        @Bean
        ReportRepository reportRepository() {
            return mock(ReportRepository.class);
        }

        @Bean
        TutoringClassRepository tutoringClassRepository() {
            return mock(TutoringClassRepository.class);
        }

        @Bean
        PlatformAdminRepository platformAdminRepository() {
            return mock(PlatformAdminRepository.class);
        }

        @Bean
        TicketMessageRepository ticketMessageRepository() {
            return mock(TicketMessageRepository.class);
        }

        @Bean
        NotificationDispatchService notificationDispatchService() {
            return mock(NotificationDispatchService.class);
        }
    }
}
