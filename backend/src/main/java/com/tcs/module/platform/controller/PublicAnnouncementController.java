package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.response.AnnouncementResponse;
import com.tcs.module.platform.service.AnnouncementService;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home/announcements")
@RequiredArgsConstructor
public class PublicAnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public List<AnnouncementResponse> getVisibleAnnouncements() {
        return announcementService.getVisibleAnnouncements(currentRoleOrNull());
    }

    private UserRole currentRoleOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getRole();
        }
        return null;
    }
}
