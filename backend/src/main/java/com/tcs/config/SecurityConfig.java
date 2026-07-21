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
                                "/uploads/**",
                                "/api/home",
                                "/api/identity/login",
                                "/api/identity/google",
                                "/api/identity/google/complete",
                                "/api/identity/register",
                                "/api/identity/send-otp",
                                "/api/identity/verify-otp",
                                "/api/identity/password/forgot",
                                "/api/identity/password/reset",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**")
                        .permitAll()
                        .requestMatchers("/api/catalog/**")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/classes/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/tutors/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/center/recruitment/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contract/reviews/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/finance/webhooks/**")
                        .permitAll()

                        // --- Platform admin ---
                        .requestMatchers("/api/platform/**")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/finance/withdrawals")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/finance/withdrawals/*/accept")
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

                        // --- Profile (specific before general) ---
                        .requestMatchers("/api/profile/children/**")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers("/api/profile/experiences/**", "/api/profile/availability/**")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/profile/verification/submit")
                        .hasAnyRole(RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)
                        .requestMatchers("/api/profile/**")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        // --- Marketplace mutations ---
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/apply")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/termination")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/**")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers("/api/marketplace/favorites/**")
                        .hasRole(RbacConstants.CLIENT)

                        // --- Center mutations ---
                        .requestMatchers(HttpMethod.POST, "/api/center/recruitment/*/apply")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/center/recruitment/**")
                        .hasRole(RbacConstants.TUTOR_CENTER)

                        // --- Issue, dispute & refund ---
                        .requestMatchers(HttpMethod.GET, "/api/disputes", "/api/disputes/**")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/disputes/*/resolve")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/disputes/*/appeal")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/disputes", "/api/class-issues")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)
                        .requestMatchers("/api/disputes/**", "/api/class-issues/**")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        // --- Finance ---
                        .requestMatchers("/api/finance/**")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)

                        // --- Contract ---
                        .requestMatchers(HttpMethod.POST, "/api/contract/reviews")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)
                        .requestMatchers("/api/contract/**")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        // --- Messaging ---
                        .requestMatchers("/api/messaging/**")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        // --- Identity (authenticated account) ---
                        .requestMatchers("/api/identity/me", "/api/identity/password")
                        .hasAnyRole(RbacConstants.BUSINESS_ROLES)

                        // --- Default ---
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
