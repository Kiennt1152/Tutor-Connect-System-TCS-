package com.tcs.config;

import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.security.AuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceModeInterceptor implements HandlerInterceptor {

    private static final String PARAM_KEY = "MAINTENANCE_MODE";
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/api/auth/",
            "/api/platform/",
            "/api/catalog/",
            "/api/public/",
            "/uploads/",
            "/swagger-ui",
            "/v3/api-docs"
    );

    private final SystemParameterRepository systemParameterRepository;
    private final AuthHelper authHelper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }

        String uri = request.getRequestURI();
        for (String prefix : ALLOWED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }

        boolean isMaintenance = systemParameterRepository.findByParamKey(PARAM_KEY)
                .map(p -> "true".equalsIgnoreCase(p.getParamValue()))
                .orElse(false);

        if (!isMaintenance) {
            return true;
        }

        if (authHelper.hasRole("PLATFORM_ADMIN")) {
            return true;
        }

        log.warn("Maintenance mode active: blocking {} request to {} from IP {}", method, uri, request.getRemoteAddr());
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {
                    "status": 503,
                    "error": "SERVICE_UNAVAILABLE",
                    "message": "Hệ thống đang trong chế độ bảo trì định kỳ. Các giao dịch tạm thời bị tạm dừng. Vui lòng quay lại sau."
                }
                """);
        return false;
    }
}
