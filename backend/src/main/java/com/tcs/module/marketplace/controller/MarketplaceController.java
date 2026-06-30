package com.tcs.module.marketplace.controller;

import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.service.MarketplaceService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/classes")
    public List<ClassResponse> listClasses(@RequestParam(required = false) TutoringClassStatus status) {
        return marketplaceService.listClasses(status);
    }

    @GetMapping("/classes/{classId}")
    public ClassResponse getClass(@PathVariable Long classId) {
        return marketplaceService.getClass(classId);
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse createClass(@RequestBody CreateClassRequest request) {
        return marketplaceService.createClass(request);
    }

    @PostMapping("/classes/{classId}/publish")
    public ClassResponse publishClass(@PathVariable Long classId) {
        return marketplaceService.publishClass(classId);
    }

    @PostMapping("/classes/{classId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> applyToClass(@PathVariable Long classId, @RequestBody ApplyClassRequest request) {
        marketplaceService.applyToClass(classId, request);
        return Map.of("message", "Đã gửi đơn ứng tuyển");
    }

    @GetMapping("/tutors/search")
    public List<TutorSearchResponse> searchTutors(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Long subjectId) {
        return marketplaceService.searchTutors(keyword, subjectId);
    }

    @PostMapping("/favorites/{tutorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> addFavorite(@PathVariable Long tutorId) {
        marketplaceService.addFavorite(tutorId);
        return Map.of("message", "Đã thêm vào yêu thích");
    }

    @DeleteMapping("/favorites/{tutorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable Long tutorId) {
        marketplaceService.removeFavorite(tutorId);
    }

    @GetMapping("/favorites")
    public List<TutorSearchResponse> getFavorites() {
        return marketplaceService.getFavorites();
    }
}
