package com.tcs.config;

import com.tcs.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String AUTH_BYPASS_PROPERTY = "tcs.dev.auth-bypass";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean authBypass = Boolean.parseBoolean(environment.getProperty(AUTH_BYPASS_PROPERTY, "false"));

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                                    "/api/home",
                                    "/api/identity/login",
                                    "/api/identity/register",
                                    "/api/identity/password/forgot",
                                    "/api/identity/password/reset",
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/catalog/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/marketplace/classes/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/marketplace/tutors/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/center/recruitment/**")
                            .permitAll();

                    if (authBypass) {
                        auth.requestMatchers("/api/platform/**").permitAll();
                    } else {
                        auth.requestMatchers("/api/platform/**").hasRole("PLATFORM_ADMIN");
                    }

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Yêu cầu đăng nhập\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Không có quyền truy cập\"}");
                        }));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
