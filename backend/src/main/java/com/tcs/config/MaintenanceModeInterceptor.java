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

/**
 * ============================================================================
 * INTERCEPTOR BẢO TRÌ HỆ THỐNG TOÀN NỀN TẢNG (MAINTENANCE MODE INTERCEPTOR)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mục đích:
 *   - Kiểm soát và chặn các yêu cầu ghi dữ liệu (POST, PUT, PATCH, DELETE) khi hệ thống
 *     được kích hoạt cờ bảo trì trong bảng SystemParameter (MAINTENANCE_MODE = "true").
 *   - Cho phép các yêu cầu đọc (GET, OPTIONS, HEAD), các API xác thực/quản trị, hoặc
 *     yêu cầu từ người dùng có quyền PLATFORM_ADMIN tiếp tục hoạt động để xử trị bảo trì.
 *   - Trả về mã lỗi HTTP 503 Service Unavailable chuẩn RESTful cùng thông điệp tiếng Việt thân thiện.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceModeInterceptor implements HandlerInterceptor {

    /** Khóa tham số hệ thống xác định trạng thái bảo trì trong CSDL */
    private static final String PARAM_KEY = "MAINTENANCE_MODE";

    /** Danh sách tiền tố URL được miễn trừ kiểm tra bảo trì (Bypass list) */
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/api/auth/",       // Endpoint đăng nhập/đăng ký/xác thực tài khoản
            "/api/platform/",   // Endpoint quản trị viên nền tảng (Admin dashboard/control)
            "/api/catalog/",    // Endpoint danh mục hệ thống công khai
            "/api/public/",     // Endpoint dữ liệu công khai (FAQ, tin tức)
            "/uploads/",        // Tài nguyên tĩnh và hình ảnh tải lên
            "/swagger-ui",      // Tài liệu OpenAPI / Swagger UI
            "/v3/api-docs"      // Metadata tài liệu API
    );

    private final SystemParameterRepository systemParameterRepository;
    private final AuthHelper authHelper;

    /**
     * Tiền xử lý yêu cầu HTTP trước khi chuyển tới Controller.
     * 
     * @param request  đối tượng HttpServletRequest của client
     * @param response đối tượng HttpServletResponse phản hồi client
     * @param handler  bộ xử lý đích (Controller handler)
     * @return {@code true} nếu cho phép yêu cầu tiếp tục; {@code false} nếu chặn lại
     * @throws Exception khi xảy ra lỗi trong quá trình ghi dữ liệu phản hồi
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();

        // Bước 1: Bỏ qua kiểm tra đối với các HTTP Method chỉ đọc dữ liệu hoặc tiền kiểm tra CORS
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }

        // Bước 2: Kiểm tra các URI thuộc danh sách ngoại lệ được phép truy cập
        String uri = request.getRequestURI();
        for (String prefix : ALLOWED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }

        // Bước 3: Đọc cờ trạng thái bảo trì hệ thống từ CSDL thông qua SystemParameterRepository
        boolean isMaintenance = systemParameterRepository.findByParamKey(PARAM_KEY)
                .map(p -> "true".equalsIgnoreCase(p.getParamValue()))
                .orElse(false);

        // Nếu không trong chế độ bảo trì -> Cho phép yêu cầu đi tiếp
        if (!isMaintenance) {
            return true;
        }

        // Bước 4: Cho phép tài khoản Quản trị viên (PLATFORM_ADMIN) vượt qua rào cản bảo trì để cấu hình/khắc phục sự cố
        if (authHelper.hasRole("PLATFORM_ADMIN")) {
            return true;
        }

        // Bước 5: Chặn yêu cầu và trả về mã lỗi HTTP 503 (Service Unavailable) dạng JSON
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
