package com.tcs.module.profile.service;

import com.tcs.module.profile.dto.request.ChildProfileRequest;
import com.tcs.module.profile.dto.request.LinkChildRequest;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.request.TutorExperienceRequest;
import com.tcs.module.profile.dto.request.UpdateProfileRequest;
import com.tcs.module.profile.dto.response.ChildProfileResponse;
import com.tcs.module.profile.dto.response.ProfileResponse;
import com.tcs.module.profile.dto.response.TutorAvailabilityResponse;
import com.tcs.module.profile.dto.response.TutorExperienceResponse;
import java.util.List;

public interface ProfileService {

    ProfileResponse getMyProfile();

    ProfileResponse updateMyProfile(UpdateProfileRequest request);

    List<ChildProfileResponse> getMyChildren();

    ChildProfileResponse createChild(ChildProfileRequest request);

    ChildProfileResponse linkChild(LinkChildRequest request);

    List<TutorExperienceResponse> getMyExperiences();

    TutorExperienceResponse addExperience(TutorExperienceRequest request);

    void deleteExperience(Long experienceId);

    List<TutorAvailabilityResponse> getMyAvailability();

    TutorAvailabilityResponse addAvailability(TutorAvailabilityRequest request);

    void deleteAvailability(Long availabilityId);

    void submitVerification();

    String uploadAvatar(org.springframework.web.multipart.MultipartFile file);
}
