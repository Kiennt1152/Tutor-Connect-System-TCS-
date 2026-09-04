package com.tcs.module.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.GlobalExceptionHandler;
import com.tcs.module.identity.controller.IdentityController;
import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.request.ResetPasswordRequest;
import com.tcs.module.identity.entity.EmailOtp;
import com.tcs.module.identity.entity.EmailVerificationToken;
import com.tcs.module.identity.entity.PasswordResetToken;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.EmailOtpRepository;
import com.tcs.module.identity.repository.EmailVerificationTokenRepository;
import com.tcs.module.identity.repository.PasswordResetTokenRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.EmailService;
import com.tcs.module.identity.service.OtpService;
import com.tcs.module.platform.dto.response.PageAuditLogResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.GoogleTokenVerifier;
import com.tcs.security.JwtService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Tag("report52-it")
@SpringBootTest(
        classes = Report52IdentityPasswordResetApiDbITTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:tcs_auth_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "app.jwt.secret=it-auth-020-test-secret-it-auth-020-test-secret",
                "app.jwt.expiration-ms=3600000"
        })
class Report52IdentityPasswordResetApiDbITTest {

    private static final String EMAIL = "it-auth-020@tcs.test";
    private static final String OLD_PASSWORD = "OldPassword123";
    private static final String NEW_PASSWORD = "NewPassword123";
    private static final String RESET_TOKEN = "it-auth-020-reset-token";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private IdentityController identityController;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @jakarta.annotation.Resource
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @jakarta.annotation.Resource
    private PasswordEncoder passwordEncoder;

    @jakarta.annotation.Resource
    private JdbcTemplate jdbcTemplate;

    @jakarta.annotation.Resource
    private JwtService jwtService;

    @jakarta.annotation.Resource
    private PlatformMapper platformMapper;

    @jakarta.annotation.Resource
    private PlatformAdminRepository platformAdminRepository;

    @jakarta.annotation.Resource
    private ClientRepository clientRepository;

    @jakarta.annotation.Resource
    private TutorRepository tutorRepository;

    @jakarta.annotation.Resource
    private TutorCenterRepository tutorCenterRepository;

    @BeforeEach
    void setUpMockMvcAndCleanDatabase() {
        mockMvc = MockMvcBuilders.standaloneSetup(identityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        jdbcTemplate.update("DELETE FROM audit_logs");
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void IT_AUTH_001_LoginActiveUserIssuesJwtAndUpdatesLastLoginThroughApiAndDb() throws Exception {
        String email = "it-auth-001@tcs.test";
        User user = new User();
        user.setEmail(email);
        user.setPhone("0900000001");
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.saveAndFlush(user);

        when(platformAdminRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(tutorRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(tutorCenterRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(clientRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        when(platformMapper.toUserListItem(any(User.class), any()))
                .thenReturn(UserListItemResponse.builder().displayName("Client API DB").build());
        when(jwtService.generateToken(anyLong(), eq(email), eq(UserRole.CLIENT), eq(0L)))
                .thenReturn("jwt-it-auth-001");

        LoginRequest request = new LoginRequest();
        request.setEmail(email.toUpperCase());
        request.setPassword("Password123");

        mockMvc.perform(post("/api/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Report52-IT-AUTH-001")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-it-auth-001"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.displayName").value("Client API DB"));

        User reloadedUser = userRepository.findByEmail(email).orElseThrow();
        assertNotNull(reloadedUser.getLastLogin());
        Integer auditRows = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM audit_logs
                WHERE actor_id = ?
                  AND action = 'LOGIN'
                  AND entity_type = 'User'
                  AND entity_id = ?
                """,
                Integer.class,
                reloadedUser.getUserId(),
                reloadedUser.getUserId());
        assertNotNull(auditRows);
        assertTrue(auditRows > 0);
    }

    @Test
    void IT_AUTH_020_ResetPasswordConsumesTokenAndStoresNewHashThroughApiAndDb() throws Exception {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPhone("0900000020");
        user.setPasswordHash(passwordEncoder.encode(OLD_PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.saveAndFlush(user);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(RESET_TOKEN);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.saveAndFlush(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(RESET_TOKEN);
        request.setNewPassword(NEW_PASSWORD);

        mockMvc.perform(post("/api/identity/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Report52-IT-AUTH-020")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"));

        User reloadedUser = userRepository.findByEmail(EMAIL).orElseThrow();
        PasswordResetToken consumedToken = passwordResetTokenRepository.findByToken(RESET_TOKEN).orElseThrow();

        assertTrue(passwordEncoder.matches(NEW_PASSWORD, reloadedUser.getPasswordHash()));
        assertFalse(passwordEncoder.matches(OLD_PASSWORD, reloadedUser.getPasswordHash()));
        assertNotNull(consumedToken.getUsedAt());
        Integer auditRows = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM audit_logs
                WHERE actor_id = ?
                  AND action = 'RESET_PASSWORD'
                  AND entity_type = 'User'
                  AND entity_id = ?
                """,
                Integer.class,
                reloadedUser.getUserId(),
                reloadedUser.getUserId());
        assertNotNull(auditRows);
        assertTrue(auditRows > 0);

        request.setNewPassword("AnotherPassword123");
        mockMvc.perform(post("/api/identity/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token đã hết hạn hoặc đã sử dụng"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.tcs")
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @Import({
            IdentityController.class,
            IdentityServiceImpl.class,
            TestBeans.class
    })
    static class TestApplication {
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        OtpService otpService(EmailOtpRepository emailOtpRepository) {
            return new OtpService(emailOtpRepository);
        }

        @Bean
        EmailService emailService() {
            return mock(EmailService.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        AuthHelper authHelper() {
            return mock(AuthHelper.class);
        }

        @Bean
        GoogleTokenVerifier googleTokenVerifier() {
            return mock(GoogleTokenVerifier.class);
        }

        @Bean
        PlatformMapper platformMapper() {
            return mock(PlatformMapper.class);
        }

        @Bean
        PlatformAdminRepository platformAdminRepository() {
            return mock(PlatformAdminRepository.class);
        }

        @Bean
        ClientRepository clientRepository() {
            return mock(ClientRepository.class);
        }

        @Bean
        TutorRepository tutorRepository() {
            return mock(TutorRepository.class);
        }

        @Bean
        TutorCenterRepository tutorCenterRepository() {
            return mock(TutorCenterRepository.class);
        }

        @Bean
        AuditLogService auditLogService(JdbcTemplate jdbcTemplate) {
            return new AuditLogService() {
                @Override
                @Transactional
                public void record(String action, String entityType, Long entityId, Object oldValue, Object newValue) {
                    record(null, action, entityType, entityId, oldValue, newValue);
                }

                @Override
                @Transactional
                public void record(
                        Long actorUserId,
                        String action,
                        String entityType,
                        Long entityId,
                        Object oldValue,
                        Object newValue) {
                    jdbcTemplate.update(
                            """
                            INSERT INTO audit_logs
                                (actor_id, action, entity_type, entity_id, old_value, new_value, created_at)
                            VALUES (?, ?, ?, ?, NULL, NULL, CURRENT_TIMESTAMP)
                            """,
                            actorUserId,
                            action,
                            entityType,
                            entityId);
                }

                @Override
                public PageAuditLogResponse search(
                        Long actorId,
                        String actorRole,
                        String action,
                        String entityType,
                        String keyword,
                        LocalDateTime from,
                        LocalDateTime to,
                        int page,
                        int size) {
                    throw new UnsupportedOperationException("Audit search is outside IT-AUTH-020.");
                }
            };
        }
    }
}
