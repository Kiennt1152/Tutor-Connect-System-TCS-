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

    /**
     * [UC-44] Tạo mới đánh giá và xếp hạng sao uy tín sau khi hoàn thành hợp đồng lớp học.
     * 
     * Luồng xử lý:
     * 1. Xác thực người dùng hiện tại và người được đánh giá.
     * 2. Kiểm tra tính hợp lệ của phân công lớp (ClassAssignment): Người đánh giá phải là người tham gia lớp (Phụ huynh hoặc Gia sư).
     * 3. Ràng buộc nghiệp vụ: Không thể tự đánh giá chính mình và mỗi phân công chỉ được gửi 1 lần đánh giá chính thức.
     * 4. Lưu bản ghi Review vào CSDL và ánh xạ sang ReviewResponse.
     * 
     * @param request Dữ liệu gửi đánh giá bao gồm assignmentId, revieweeId, rating (1-5) và comment
     * @return ReviewResponse thông tin đánh giá chi tiết vừa được tạo
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy phân công hoặc người dùng
     * @throws IllegalArgumentException nếu tự đánh giá, đánh giá trùng lặp hoặc vi phạm thang điểm
     */
    ReviewResponse createReview(CreateReviewRequest request);

    /**
     * [UC-44] Lấy danh sách toàn bộ các đánh giá công khai dành cho một Gia sư.
     * 
     * @param tutorUserId Định danh tài khoản người dùng của Gia sư
     * @return Danh sách ReviewResponse chứa các đánh giá, số sao và nhận xét từ người dùng
     */
    List<ReviewResponse> getReviewsForTutor(Long tutorUserId);
}
