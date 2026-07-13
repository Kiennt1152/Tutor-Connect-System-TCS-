package com.tcs.module.marketplace.service;

import com.tcs.module.marketplace.dto.TutorRecommendation;
import java.util.List;

/**
 * Seam 0.10 (dung chung M1/M2). Goi y tutor cho lop (UC-62).
 * Pha 1: ban stub tra danh sach rong (PhaseOneStubConfig).
 */
public interface RecommendationService {

    List<TutorRecommendation> recommendTutors(Long classId);
}
