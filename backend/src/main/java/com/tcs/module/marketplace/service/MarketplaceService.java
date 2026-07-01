package com.tcs.module.marketplace.service;

import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.response.ClassResponse;
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
}
