package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.request.UpsertAnnouncementRequest;
import com.tcs.module.platform.dto.response.AnnouncementResponse;
import com.tcs.module.platform.service.AnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public List<AnnouncementResponse> getAnnouncements() {
        return announcementService.getAnnouncements();
    }

    @GetMapping("/{announcementId}")
    public AnnouncementResponse getAnnouncement(@PathVariable Long announcementId) {
        return announcementService.getAnnouncement(announcementId);
    }

    @PostMapping
    public AnnouncementResponse createAnnouncement(@Valid @RequestBody UpsertAnnouncementRequest request) {
        return announcementService.createAnnouncement(request);
    }

    @PatchMapping("/{announcementId}")
    public AnnouncementResponse updateAnnouncement(
            @PathVariable Long announcementId, @Valid @RequestBody UpsertAnnouncementRequest request) {
        return announcementService.updateAnnouncement(announcementId, request);
    }

    @DeleteMapping("/{announcementId}")
    public void deleteAnnouncement(@PathVariable Long announcementId) {
        announcementService.deleteAnnouncement(announcementId);
    }
}
