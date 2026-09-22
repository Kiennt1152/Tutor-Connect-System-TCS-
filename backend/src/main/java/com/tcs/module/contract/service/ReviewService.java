package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import java.util.List;

/**
 * ============================================================================
 * [BF-07] [UC-44] GIAO DIỆN ĐÁNH GIÁ VÀ UY TÍN GIA SƯ (REVIEW SERVICE INTERFACE)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-07-13
 * 
 * 1. Mục đích & Chức năng:
 *    - Định nghĩa các dịch vụ đánh giá chất lượng dạy học sau khi hoàn thành hợp đồng.
 *    - Tra cứu danh sách đánh giá công khai của gia sư phục vụ hồ sơ và xếp hạng.
 * 
 * 2. Luồng xử lý chính:
 *    - createReview: Tiếp nhận điểm đánh giá (Rating) và nhận xét (Comment) từ người dùng.
 *    - getReviewsForTutor: Lấy danh sách đánh giá kèm tính toán điểm trung bình.
 * ============================================================================
 */
public interface ReviewService {

    ReviewResponse createReview(CreateReviewRequest request);

    List<ReviewResponse> getReviewsForTutor(Long tutorUserId);
}
