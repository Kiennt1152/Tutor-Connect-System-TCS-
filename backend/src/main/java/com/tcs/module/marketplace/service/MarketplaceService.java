package com.tcs.module.marketplace.service;

import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.response.ApplicantResponse;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import java.util.List;

public interface MarketplaceService {

    List<ClassResponse> listClasses(TutoringClassStatus status);

    ClassResponse getClass(Long classId);

    List<ClassResponse> listMyClasses();

    ClassResponse createClass(CreateClassRequest request);

    ClassResponse updateClass(Long classId, CreateClassRequest request);

    ClassResponse publishClass(Long classId);

    void applyToClass(Long classId, ApplyClassRequest request);

    /** Id các lớp mà gia sư đang đăng nhập đã nộp đơn — để UI không cho ứng tuyển lại. */
    List<Long> listMyAppliedClassIds();

    /** Danh sách gia sư đã ứng tuyển vào lớp (chỉ Client tạo lớp được xem), kèm điểm AI gợi ý. */
    List<ApplicantResponse> listApplicants(Long classId);

    /** Client chọn 1 gia sư: đơn được duyệt, các đơn khác bị từ chối, lớp chuyển MATCHED. */
    void chooseApplicant(Long classId, Long applicationId);

    List<TutorSearchResponse> searchTutors(String keyword, Long subjectId);

    void addFavorite(Long tutorId);

    void removeFavorite(Long tutorId);

    List<TutorSearchResponse> getFavorites();
}
