package com.tcs.module.marketplace.service;

import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.request.TutorApplicationReviewRequest;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.TutorApplicationResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import java.util.List;

public interface MarketplaceService {

    List<ClassResponse> listClasses(TutoringClassStatus status);

    ClassResponse getClass(Long classId);

    ClassResponse createClass(CreateClassRequest request);

    ClassResponse publishClass(Long classId);

    void applyToClass(Long classId, ApplyClassRequest request);

    List<TutorSearchResponse> searchTutors(String keyword, Long subjectId);

    void addFavorite(Long tutorId);

    void removeFavorite(Long tutorId);

    List<TutorSearchResponse> getFavorites();

    // ---- Tutoring Request Management ----

    /** Client (class owner) xem danh sách các application cho lớp của mình. */
    List<TutorApplicationResponse> listApplicationsByClass(Long classId);

    /** Tutor xem các application mình đã gửi. */
    List<TutorApplicationResponse> listMyApplications();

    /** Class owner hoặc tutor applicant xem chi tiết 1 application. */
    TutorApplicationResponse getApplication(Long applicationId);

    /** Tutor rút đơn của mình. Chỉ áp dụng khi status = SUBMITTED. */
    void withdrawApplication(Long applicationId);

    /** Client (class owner) duyệt hoặc từ chối 1 application. */
    TutorApplicationResponse reviewApplication(Long applicationId, TutorApplicationReviewRequest request);
}