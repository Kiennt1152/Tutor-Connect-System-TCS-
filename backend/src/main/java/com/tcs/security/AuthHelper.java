package com.tcs.security;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.UnauthorizedException;
import com.tcs.module.profile.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    public UserPrincipal requireAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Yêu cầu đăng nhập");
        }
        return principal;
    }

    public UserPrincipal requireRole(UserRole... roles) {
        UserPrincipal principal = requireAuthenticated();
        for (UserRole role : roles) {
            if (principal.getRole() == role) {
                return principal;
            }
        }
        throw new ForbiddenException("Không có quyền truy cập");
    }

    public Long currentUserId() {
        return requireAuthenticated().getUserId();
    }

    /**
     * Id người dùng hiện tại, hoặc null nếu chưa đăng nhập.
     *
     * <p>Dùng cho endpoint công khai muốn cá nhân hóa thêm khi có đăng nhập
     * (ví dụ danh sách tin tuyển dụng ai cũng xem được, nhưng gia sư đã đăng nhập
     * thì đánh dấu tin của trung tâm mình đã thuộc). Khác {@link #currentUserId()}
     * ở chỗ không ném {@code UnauthorizedException} với khách.
     */
    public Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getUserId();
    }

    /**
     * Returns true if the currently authenticated user has the given role name
     * (e.g. "PLATFORM_ADMIN", "TUTOR", "CLIENT", "TUTOR_CENTER").
     */
    public boolean hasRole(String roleName) {
        try {
            UserPrincipal principal = requireAuthenticated();
            return principal.getRole().name().equals(roleName);
        } catch (Exception e) {
            return false;
        }
    }
}
