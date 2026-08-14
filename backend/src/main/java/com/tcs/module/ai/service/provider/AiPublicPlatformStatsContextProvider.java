package com.tcs.module.ai.service.provider;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiPublicPlatformStatsContextProvider {

    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final TutoringClassRepository tutoringClassRepository;

    @Transactional(readOnly = true)
    public List<AiSourceResponse> getPlatformStats() {
        List<AiSourceResponse> results = new ArrayList<>();
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);

        long totalTutors = tutorRepository.count();
        long activeTutors = tutorRepository.countByUserStatus(UserStatus.ACTIVE);
        long verifiedTutors = tutorRepository.countByVerificationStatus(ProfileVerificationStatus.VERIFIED);
                
        long totalCenters = tutorCenterRepository.count();
        long activeCenters = tutorCenterRepository.countByUserStatus(UserStatus.ACTIVE);
        long verifiedCenters = tutorCenterRepository.countByVerificationStatus(ProfileVerificationStatus.VERIFIED);
                
        long totalClasses = tutoringClassRepository.count();
        long openClasses = tutoringClassRepository.countByStatus(TutoringClassStatus.OPEN);
        
        String snippet = String.format(
                "Dữ liệu thống kê quy mô thực tế của nền tảng Tutor Connect System (TCS):\n" +
                "- Tổng số người dùng: %d tài khoản (trong đó %d tài khoản đang hoạt động - ACTIVE)\n" +
                "- Tổng số gia sư: %d gia sư (%d gia sư đang hoạt động, %d gia sư đã được xác minh hồ sơ)\n" +
                "- Tổng số trung tâm gia sư: %d trung tâm (%d trung tâm đang hoạt động, %d trung tâm đã xác minh giấy phép)\n" +
                "- Tổng số lớp học: %d lớp học (%d lớp học đang mở tuyển gia sư)", 
                totalUsers, activeUsers, totalTutors, activeTutors, verifiedTutors, 
                totalCenters, activeCenters, verifiedCenters, totalClasses, openClasses);

        results.add(AiSourceResponse.builder()
                .sourceId("STATS")
                .sourceType("SYSTEM")
                .title("Thống kê quy mô nền tảng TCS")
                .snippet(snippet)
                .finalScore(1.0)
                .visibility("PUBLIC")
                .build());

        return results;
    }
}
