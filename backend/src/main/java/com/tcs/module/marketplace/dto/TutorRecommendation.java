package com.tcs.module.marketplace.dto;

/**
 * Seam 0.10: ket qua goi y tutor cho mot lop (UC-62).
 * Pha 1 tra danh sach rong; AI that trien khai o pha 2.
 */
public record TutorRecommendation(
        Long tutorId,
        double score,
        String reason) {
}
