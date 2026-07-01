package com.tcs.module.profile.controller;

import com.tcs.module.profile.dto.request.ChildProfileRequest;
import com.tcs.module.profile.dto.request.LinkChildRequest;
import com.tcs.module.profile.dto.request.LinkGuardianRequest;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.request.TutorExperienceRequest;
import com.tcs.module.profile.dto.request.UpdateProfileRequest;
import com.tcs.module.profile.dto.response.ChildProfileResponse;
import com.tcs.module.profile.dto.response.DependentLinkStatusResponse;
import com.tcs.module.profile.dto.response.GuardianProfileResponse;
import com.tcs.module.profile.dto.response.ProfileResponse;
import com.tcs.module.profile.dto.response.TutorAvailabilityResponse;
import com.tcs.module.profile.dto.response.TutorExperienceResponse;
import com.tcs.module.profile.service.ProfileService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse getMyProfile() {
        return profileService.getMyProfile();
    }

    @PutMapping("/me")
    public ProfileResponse updateMyProfile(@RequestBody UpdateProfileRequest request) {
        return profileService.updateMyProfile(request);
    }

    @GetMapping("/children")
    public List<ChildProfileResponse> getMyChildren() {
        return profileService.getMyChildren();
    }

    @PostMapping("/children")
    @ResponseStatus(HttpStatus.CREATED)
    public ChildProfileResponse createChild(@RequestBody ChildProfileRequest request) {
        return profileService.createChild(request);
    }

    @PostMapping("/children/link")
    public ChildProfileResponse linkChild(@RequestBody LinkChildRequest request) {
        return profileService.linkChild(request);
    }

    @GetMapping("/dependent-status")
    public DependentLinkStatusResponse getDependentLinkStatus() {
        return profileService.getDependentLinkStatus();
    }

    @GetMapping("/guardian")
    public GuardianProfileResponse getMyGuardian() {
        return profileService.getMyGuardian();
    }

    @PostMapping("/guardian/link")
    public GuardianProfileResponse linkGuardian(@RequestBody LinkGuardianRequest request) {
        return profileService.linkGuardian(request);
    }

    @GetMapping("/experiences")
    public List<TutorExperienceResponse> getMyExperiences() {
        return profileService.getMyExperiences();
    }

    @PostMapping("/experiences")
    @ResponseStatus(HttpStatus.CREATED)
    public TutorExperienceResponse addExperience(@RequestBody TutorExperienceRequest request) {
        return profileService.addExperience(request);
    }

    @DeleteMapping("/experiences/{experienceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExperience(@PathVariable Long experienceId) {
        profileService.deleteExperience(experienceId);
    }

    @GetMapping("/availability")
    public List<TutorAvailabilityResponse> getMyAvailability() {
        return profileService.getMyAvailability();
    }

    @PostMapping("/availability")
    @ResponseStatus(HttpStatus.CREATED)
    public TutorAvailabilityResponse addAvailability(@RequestBody TutorAvailabilityRequest request) {
        return profileService.addAvailability(request);
    }

    @DeleteMapping("/availability/{availabilityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvailability(@PathVariable Long availabilityId) {
        profileService.deleteAvailability(availabilityId);
    }

    @PostMapping("/verification/submit")
    public Map<String, String> submitVerification() {
        profileService.submitVerification();
        return Map.of("message", "Đã nộp hồ sơ xác minh");
    }
}
