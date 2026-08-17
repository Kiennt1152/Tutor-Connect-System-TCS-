package com.tcs.config;

import com.tcs.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        // --- Public ---
                        .requestMatchers(
                                "/error",
                                "/uploads/public/**",
                                "/api/home",
                                "/api/home/announcements",
                                "/api/identity/login",
                                "/api/identity/google",
                                "/api/identity/google/complete",
                                "/api/identity/register",
                                "/api/identity/send-otp",
                                "/api/identity/verify-otp",
                                "/api/identity/password/forgot",
                                "/api/identity/password/forgot/verify-otp",
                                "/api/identity/password/reset",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        // Private file access: authenticated users only (owner + admin check in controller)
                        .requestMatchers("/api/files/private/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/catalog/faq/admin")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/catalog/parameters/**")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/catalog/chatbot/ask")
                        .permitAll()
                        .requestMatchers("/api/ai/**")
                        .permitAll()
                        .requestMatchers("/api/catalog/**")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/classes/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/tutors/**")
                        .permitAll()
                        // Danh sách trung tâm đã xác minh: ai cũng xem được (để phụ huynh chọn).
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/centers")
                        .permitAll()
                        // Recruitment: các GET cần đăng nhập phải đứng TRƯỚC GET công khai bên dưới.
                        .requestMatchers(HttpMethod.GET, "/api/center/recruitment/my-posts")
                        .hasRole(RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.GET, "/api/center/recruitment/*/applications")
                        .hasRole(RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.GET, "/api/center/recruitment/applications/mine")
                        .hasRole(RbacConstants.TUTOR)
                        // Tin đang mở: ai cũng xem được.
                        .requestMatchers(HttpMethod.GET, "/api/center/recruitment/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contract/reviews/reviewable")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers(HttpMethod.GET, "/api/contract/reviews/my-reputation")
                        .hasRole(RbacConstants.TUTOR)
                        // Quản lý danh sách gia sư của trung tâm.
                        .requestMatchers("/api/center/members", "/api/center/members/**")
                        .hasRole(RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.GET, "/api/contract/reviews/**")
                        .permitAll()
                        // Hồ sơ công khai của gia sư (trang /gia-su/:tutorId).
                        .requestMatchers(HttpMethod.GET, "/api/profile/tutor/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/finance/webhooks/**")
                        .permitAll()

                        .requestMatchers("/api/platform/**")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/finance/withdrawals")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/finance/withdrawals/*/accept")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/finance/withdrawals/*/approve",
                                "/api/finance/withdrawals/*/reject",
                                "/api/finance/withdrawals/*/transfer-failed")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/finance/settlements/preview/*")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/finance/settlements/*/apply",
                                "/api/finance/settlements/execute")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/finance/refunds/execute")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/finance/refund-requests")
                        .hasAnyRole(RbacConstants.PLATFORM_ADMIN, RbacConstants.TUTOR_CENTER)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/finance/refund-requests/*/approve",
                                "/api/finance/refund-requests/*/reject")
                        .hasAnyRole(RbacConstants.PLATFORM_ADMIN, RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.POST, "/api/finance/refund-requests")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        .requestMatchers("/api/profile/children/**")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers(
                                "/api/profile/experiences/**",
                                "/api/profile/educations/**",
                                "/api/profile/certificates/**",
                                "/api/profile/availability/**")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/profile/verification/submit")
                        .hasAnyRole(RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)
                        .requestMatchers("/api/profile/**")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        .requestMatchers(HttpMethod.GET, "/api/marketplace/lessons/mine", "/api/marketplace/assignments/mine")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR)
                        .requestMatchers(
                                HttpMethod.GET, "/api/marketplace/lessons/requests")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/marketplace/lessons/*/reschedule",
                                "/api/marketplace/lessons/requests/*/decision",
                                "/api/marketplace/lessons/requests/*/cancel")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR)
                        .requestMatchers(
                                "/api/marketplace/assignments/*/contract",
                                "/api/marketplace/assignments/*/contract-terms",
                                "/api/marketplace/assignments/*/refund-payout",
                                "/api/marketplace/assignments/*/sign",
                                "/api/marketplace/assignments/*/sign/request-otp")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR)
                        .requestMatchers("/api/marketplace/assignments/**", "/api/marketplace/lessons/**")
                        .hasRole(RbacConstants.TUTOR)

                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/register")
                        .hasAnyRole(RbacConstants.TUTOR, RbacConstants.CLIENT)
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/apply")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/termination")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/complete")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/**")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers("/api/marketplace/favorites/**")
                        .hasRole(RbacConstants.CLIENT)
                        // Phụ huynh gửi/quản lý yêu cầu mở lớp tới trung tâm.
                        .requestMatchers("/api/marketplace/centers/*/class-requests")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers("/api/marketplace/class-requests/**")
                        .hasRole(RbacConstants.CLIENT)

                        .requestMatchers(
                                "/api/center/classes/**",
                                "/api/center/tutors",
                                "/api/center/class-requests",
                                "/api/center/class-requests/**",
                                "/api/center/contract-templates",
                                "/api/center/contract-templates/**",
                                "/api/center/schedule",
                                "/api/center/reschedules",
                                "/api/center/reschedules/**",
                                "/api/center/substitutions",
                                "/api/center/substitutions/**",
                                "/api/center/reports",
                                "/api/center/reports/**")
                        .hasRole(RbacConstants.TUTOR_CENTER)

                        .requestMatchers("/api/tutor/**")
                        .hasRole(RbacConstants.TUTOR)

                        .requestMatchers(HttpMethod.POST, "/api/center/recruitment/*/apply")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/center/recruitment/**")
                        .hasRole(RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.PUT, "/api/center/recruitment/**")
                        .hasRole(RbacConstants.TUTOR_CENTER)

                        // --- Issue, dispute & refund ---
                        .requestMatchers(HttpMethod.GET, "/api/disputes", "/api/disputes/**")
                        .hasAnyRole(RbacConstants.PLATFORM_ADMIN, RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.POST, "/api/disputes/*/resolve")
                        .hasAnyRole(RbacConstants.PLATFORM_ADMIN, RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.POST, "/api/disputes/*/evidence")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/disputes/*/appeal")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/disputes", "/api/class-issues")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)
                        .requestMatchers("/api/disputes/**", "/api/class-issues/**")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        // --- Finance ---
                        .requestMatchers("/api/finance/**")
                        .hasAnyRole(RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)

                        .requestMatchers(HttpMethod.POST, "/api/contract/reviews/*/reply")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.PUT, "/api/contract/reviews/*")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers(HttpMethod.POST, "/api/contract/reviews")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)
                        .requestMatchers("/api/contract/**")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        .requestMatchers("/api/messaging/**")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        .requestMatchers("/api/identity/me", "/api/identity/password")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
