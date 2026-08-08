package com.tcs.module.profile.service;

import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.profile.dto.request.ChildProfileRequest;
import com.tcs.module.profile.dto.request.LinkChildAccountRequest;
import com.tcs.module.profile.dto.request.LinkChildRequest;
import com.tcs.module.profile.dto.request.LinkGuardianRequest;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.request.TutorExperienceRequest;
import com.tcs.module.profile.dto.request.UpdateChildProfileRequest;
import com.tcs.module.profile.dto.request.UpdateProfileRequest;
import com.tcs.module.profile.dto.response.ChildProfileResponse;
import com.tcs.module.profile.dto.response.DependentLinkStatusResponse;
import com.tcs.module.profile.dto.response.GuardianProfileResponse;
import com.tcs.module.profile.dto.response.ProfileResponse;
import com.tcs.module.profile.dto.response.TutorAvailabilityResponse;
import com.tcs.module.profile.dto.response.TutorExperienceResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    ProfileResponse getMyProfile();

    ProfileResponse updateMyProfile(UpdateProfileRequest request);

    List<ChildProfileResponse> getMyChildren();

    ChildProfileResponse createChild(ChildProfileRequest request);

    ChildProfileResponse getChildById(Long childProfileId);

    ChildProfileResponse updateChild(Long childProfileId, UpdateChildProfileRequest request);

    void deleteChild(Long childProfileId);

    ChildProfileResponse linkChild(LinkChildRequest request);

    ChildProfileResponse linkChildAccount(LinkChildAccountRequest request);

    DependentLinkStatusResponse getDependentLinkStatus();

    GuardianProfileResponse getMyGuardian();

    GuardianProfileResponse linkGuardian(LinkGuardianRequest request);

    List<TutorExperienceResponse> getMyExperiences();

    TutorExperienceResponse addExperience(TutorExperienceRequest request);

    void deleteExperience(Long experienceId);

    List<TutorAvailabilityResponse> getMyAvailability();

    TutorAvailabilityResponse addAvailability(TutorAvailabilityRequest request);

    void deleteAvailability(Long availabilityId);

    VerificationResponse submitVerification(VerificationRequestDto request);

    String uploadAvatar(MultipartFile file);
}
