package com.tcs.module.ai.service;

import com.tcs.module.ai.constants.AiConstants;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import org.springframework.stereotype.Service;

@Service
public class AiPermissionFilterService {

    public boolean canAccess(AiKnowledgeChunk chunk, String userRole, Long userId) {
        String visibility = chunk.getVisibility() != null ? chunk.getVisibility() : AiConstants.VISIBILITY_PUBLIC;
        
        if (AiConstants.VISIBILITY_PUBLIC.equals(visibility)) {
            return true;
        }

        if (userRole == null || "GUEST".equals(userRole)) {
            return false;
        }

        if ("PLATFORM_ADMIN".equals(userRole)) {
            return true;
        }

        if (AiConstants.VISIBILITY_OWNER_PRIVATE.equals(visibility)) {
            return userId != null && userId.equals(chunk.getOwnerUserId());
        }

        if (AiConstants.VISIBILITY_ROLE_RESTRICTED.equals(visibility)) {
            String minRole = chunk.getMinRole();
            if (minRole == null) return true;
            if ("TUTOR_CENTER".equals(minRole) && "TUTOR_CENTER".equals(userRole)) return true;
            if ("TUTOR".equals(minRole) && ("TUTOR".equals(userRole) || "TUTOR_CENTER".equals(userRole))) return true;
            if ("CLIENT".equals(minRole) && "CLIENT".equals(userRole)) return true;
            return false;
        }

        if (AiConstants.VISIBILITY_ADMIN_ONLY.equals(visibility)) {
            return false; // Already checked for PLATFORM_ADMIN above
        }

        return false;
    }
}
