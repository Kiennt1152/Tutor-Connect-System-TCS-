package com.tcs.module.profile.controller;

import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.profile.dto.request.ChildProfileRequest;
import com.tcs.module.profile.dto.request.LinkChildAccountRequest;
import com.tcs.module.profile.dto.request.LinkChildRequest;
import com.tcs.module.profile.dto.request.LinkGuardianRequest;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.request.TutorCertificateRequest;
import com.tcs.module.profile.dto.request.TutorEducationRequest;
import com.tcs.module.profile.dto.request.TutorExperienceRequest;
import com.tcs.module.profile.dto.request.UpdateChildProfileRequest;
import com.tcs.module.profile.dto.request.UpdateProfileRequest;
import com.tcs.module.profile.dto.response.ChildProfileResponse;
import com.tcs.module.profile.dto.response.DependentLinkStatusResponse;
import com.tcs.module.profile.dto.response.GuardianProfileResponse;
import com.tcs.module.profile.dto.response.ProfileResponse;
import com.tcs.module.profile.dto.response.PublicTutorProfileResponse;
import com.tcs.module.profile.dto.response.TutorAvailabilityResponse;
import com.tcs.module.profile.dto.response.TutorCertificateResponse;
import com.tcs.module.profile.dto.response.TutorEducationResponse;
import com.tcs.module.profile.dto.response.TutorExperienceResponse;
import com.tcs.module.profile.service.ProfileService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse getMyProfile() {
        return profileService.getMyProfile();
    }

    /** Công khai: hồ sơ chi tiết của một gia sư (trang /gia-su/:tutorId). */
    @GetMapping("/tutor/{tutorId}")
    public PublicTutorProfileResponse getPublicTutorProfile(@PathVariable Long tutorId) {
        return profileService.getPublicTutorProfile(tutorId);
    }

    @PutMapping("/me")
    public ProfileResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
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

    @GetMapping("/children/{childProfileId}")
    public ChildProfileResponse getChildById(@PathVariable Long childProfileId) {
        return profileService.getChildById(childProfileId);
    }

    @PutMapping("/children/{childProfileId}")
    public ChildProfileResponse updateChild(
            @PathVariable Long childProfileId, @RequestBody UpdateChildProfileRequest request) {
        return profileService.updateChild(childProfileId, request);
    }

    @DeleteMapping("/children/{childProfileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChild(@PathVariable Long childProfileId) {
        profileService.deleteChild(childProfileId);
    }

    @PostMapping("/children/link")
    public ChildProfileResponse linkChild(@RequestBody LinkChildRequest request) {
        return profileService.linkChild(request);
    }

    @PostMapping("/children/link-account")
    public ChildProfileResponse linkChildAccount(@RequestBody LinkChildAccountRequest request) {
        return profileService.linkChildAccount(request);
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

    @GetMapping("/educations")
    public List<TutorEducationResponse> getMyEducations() {
        return profileService.getMyEducations();
    }

    @PostMapping("/educations")
    @ResponseStatus(HttpStatus.CREATED)
    public TutorEducationResponse addEducation(@RequestBody TutorEducationRequest request) {
        return profileService.addEducation(request);
    }

    @DeleteMapping("/educations/{educationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEducation(@PathVariable Long educationId) {
        profileService.deleteEducation(educationId);
    }

    @GetMapping("/certificates")
    public List<TutorCertificateResponse> getMyCertificates() {
        return profileService.getMyCertificates();
    }

    @PostMapping("/certificates")
    @ResponseStatus(HttpStatus.CREATED)
    public TutorCertificateResponse addCertificate(@RequestBody TutorCertificateRequest request) {
        return profileService.addCertificate(request);
    }

    @DeleteMapping("/certificates/{certificateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCertificate(@PathVariable Long certificateId) {
        profileService.deleteCertificate(certificateId);
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
    public VerificationResponse submitVerification(@Valid @RequestBody VerificationRequestDto request) {
        return profileService.submitVerification(request);
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public Map<String, String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = profileService.uploadAvatar(file);
        return Map.of("avatarUrl", url);
    }
}
