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
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
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
                        .requestMatchers(HttpMethod.GET, "/api/contract/reviews/reviewable")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers(HttpMethod.GET, "/api/contract/reviews/my-reputation")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.GET, "/api/contract/reviews/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/finance/webhooks/**")
                        .permitAll()

                        .requestMatchers("/api/platform/**")
                        .hasRole(RbacConstants.PLATFORM_ADMIN)

                        .requestMatchers("/api/profile/children/**")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers("/api/profile/experiences/**", "/api/profile/availability/**")
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
                                "/api/marketplace/lessons/extra",
                                "/api/marketplace/lessons/requests/*/decision",
                                "/api/marketplace/lessons/requests/*/cancel")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR)
                        .requestMatchers("/api/marketplace/assignments/**", "/api/marketplace/lessons/**")
                        .hasRole(RbacConstants.TUTOR)

                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/register")
                        .hasAnyRole(RbacConstants.TUTOR, RbacConstants.CLIENT)
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/*/apply")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/classes/**")
                        .hasRole(RbacConstants.CLIENT)
                        .requestMatchers("/api/marketplace/favorites/**")
                        .hasRole(RbacConstants.CLIENT)

                        .requestMatchers(
                                "/api/center/classes/**",
                                "/api/center/tutors",
                                "/api/center/schedule",
                                "/api/center/reschedules",
                                "/api/center/reschedules/**",
                                "/api/center/substitutions",
                                "/api/center/substitutions/**")
                        .hasRole(RbacConstants.TUTOR_CENTER)

                        .requestMatchers("/api/tutor/**")
                        .hasRole(RbacConstants.TUTOR)

                        .requestMatchers(HttpMethod.POST, "/api/center/recruitment/*/apply")
                        .hasRole(RbacConstants.TUTOR)
                        .requestMatchers(HttpMethod.POST, "/api/center/recruitment/**")
                        .hasRole(RbacConstants.TUTOR_CENTER)

                        .requestMatchers("/api/finance/**")
                        .hasAnyRole(RbacConstants.CLIENT, RbacConstants.TUTOR, RbacConstants.TUTOR_CENTER)

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
