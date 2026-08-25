package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service to handle access control and authorization checks for personal finance queries.
 */
@Service
@RequiredArgsConstructor
public class AiFinanceGuardService {

    private final AiFallbackService fallbackService;

    /**
     * Check if user has permission to query personal finance data.
     * @return Error message if unauthorized, null if allowed
     */
    public String checkFinanceAccess(AiDomain domain, String rawMessage, String userRole, Long userId) {
        if (domain != AiDomain.FINANCE_WALLET || rawMessage == null) {
            return null;
        }

        String lowerQuery = rawMessage.toLowerCase();
        boolean isPersonalQuery = lowerQuery.contains("của tôi") || 
                                  lowerQuery.contains("lương của tôi") || 
                                  lowerQuery.contains("thu nhập của tôi") || 
                                  lowerQuery.contains("ví của tôi") || 
                                  lowerQuery.contains("tiền của tôi");

        if (isPersonalQuery && (userId == null || 
            (!"TUTOR".equals(userRole) && !"TUTOR_CENTER".equals(userRole)))) {
            if (fallbackService != null && fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance") != null) {
                return fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance").message();
            }
            return "Chức năng xem thông tin tài chính cá nhân yêu cầu tài khoản Gia sư hoặc Trung tâm gia sư.";
        }

        return null;
    }
}
