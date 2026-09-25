package com.tcs.module.contract.controller;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.GenerateContractRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.contract.service.ReviewService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ====================================================================================================
 * [UC-20 / UC-21 / UC-44] PHÂN HỆ HỢP ĐỒNG ĐIỆN TỬ & KÝ SỐ OTP (CONTRACT CONTROLLER)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Khởi tạo bản thảo hợp đồng dạy kèm giữa Phụ huynh/Học viên và Gia sư hoặc Trung tâm.
 * 2. Quản lý quy trình ký kết số 2 lớp bảo mật thông qua mã xác thực OTP qua Email.
 * 3. Kích hoạt bảo chứng Escrow khi ký xong và tiếp nhận đánh giá chất lượng dạy học sau hoàn thành.
 * * @author Nguyễn Tiến Anh (tienanh6677)
 * @author Hoàng Minh Đức (mduc1011-swp)
 * @author Vũ Quốc Khánh (khanhvqhe176783)
 */
@RestController
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ReviewService reviewService;

    /**
     * [UC-29]: Tạo đánh giá và chấm điểm sao mới cho gia sư sau khi hoàn tất khóa học.
     * 
     * @param request Dữ liệu đánh giá gồm assignmentId, điểm rating, tiêu chí chuyên môn và nhận xét {@link CreateReviewRequest}
     * @return {@link ReviewResponse} Bản ghi đánh giá vừa tạo thành công
     */
    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@RequestBody CreateReviewRequest request) {
        return contractService.createReview(request);
    }

    /**
     * [UC-29]: Lấy danh sách toàn bộ các đánh giá nhận xét công khai của một gia sư.
     * 
     * @param tutorUserId ID tài khoản người dùng của gia sư
     * @return Danh sách các đánh giá {@link ReviewResponse} kèm nhận xét và điểm số
     */
    @GetMapping("/reviews/tutor/{tutorUserId}")
    public List<ReviewResponse> getReviewsForTutor(@PathVariable Long tutorUserId) {
        return reviewService.getReviewsForTutor(tutorUserId);
    }

    /**
     * [UC-21]: Khởi tạo bản hợp đồng dạy học điện tử (DRAFT) từ yêu cầu ghép lớp.
     * 
     * Luồng xử lý:
     *   1. Kiểm tra tính hợp lệ của assignmentId (lớp cá nhân) hoặc classStudentId (lớp trung tâm).
     *   2. Sinh số hợp đồng duy nhất theo quy chuẩn HĐ-YYYYMMDD-XXXX.
     *   3. Tính toán điều khoản học phí, lịch học, chính sách Escrow bảo chứng.
     *   4. Lưu trạng thái DRAFT và trả về DTO hiển thị.
     * 
     * @param request Yêu cầu khởi tạo hợp đồng chứa assignmentId hoặc classStudentId {@link GenerateContractRequest}
     * @return {@link ContractResponse} Chi tiết bản hợp đồng vừa khởi tạo
     * @throws IllegalArgumentException nếu thiếu cả assignmentId và classStudentId
     */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateContract(@RequestBody GenerateContractRequest request) {
        if (request.getAssignmentId() != null) {
            return contractService.generateContract(request.getAssignmentId());
        }
        if (request.getClassStudentId() != null) {
            return contractService.getMyContract(
                    contractService.generateForEnrollment(request.getClassStudentId()).getContractId());
        }
        throw new IllegalArgumentException("assignmentId hoặc classStudentId là bắt buộc");
    }

    /**
     * [UC-21]: Khởi tạo hợp đồng dạy kèm cá nhân trực tiếp theo mã phân công lớp.
     * 
     * @param assignmentId ID phân công gia sư nhận lớp
     * @return {@link ContractResponse} Chi tiết bản hợp đồng dạy kèm vừa tạo
     */
    @PostMapping("/generate/assignment/{assignmentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateForAssignment(@PathVariable Long assignmentId) {
        return contractService.getMyContract(
                contractService.generateForAssignment(assignmentId).getContractId());
    }

    /**
     * [UC-21]: Khởi tạo hợp đồng đào tạo dành cho học viên đăng ký lớp của trung tâm gia sư.
     * 
     * @param classStudentId ID bản ghi đăng ký lớp của học viên tại trung tâm
     * @return {@link ContractResponse} Chi tiết bản hợp đồng đào tạo trung tâm
     */
    @PostMapping("/generate/enrollment/{classStudentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse generateForEnrollment(@PathVariable Long classStudentId) {
        return contractService.getMyContract(
                contractService.generateForEnrollment(classStudentId).getContractId());
    }

    /**
     * [UC-21]: Lấy danh sách tất cả các hợp đồng liên quan đến tài khoản người dùng hiện tại.
     * 
     * @return Danh sách hợp đồng {@link ContractResponse} của người dùng (vai trò Client hoặc Tutor)
     */
    @GetMapping
    public List<ContractResponse> getMyContracts() {
        return contractService.getMyContracts();
    }

    /**
     * [UC-21]: Đường dẫn bí danh (/api/contract/my) lấy danh sách hợp đồng cá nhân.
     * 
     * @return Danh sách hợp đồng {@link ContractResponse} của người dùng hiện tại
     */
    @GetMapping("/my")
    public List<ContractResponse> getMyContractsAlias() {
        return contractService.getMyContracts();
    }

    /**
     * [UC-21]: Tra cứu thông tin chi tiết một bản hợp đồng theo ID.
     * 
     * @param contractId Mã định danh hợp đồng
     * @return {@link ContractResponse} Thông tin chi tiết các điều khoản, trạng thái và lịch thanh toán
     */
    @GetMapping("/{contractId}")
    public ContractResponse getContract(@PathVariable Long contractId) {
        return contractService.getMyContract(contractId);
    }

    /**
     * [UC-22]: Lấy lịch sử và thông tin chữ ký số điện tử của các bên tham gia hợp đồng.
     * 
     * @param contractId Mã định danh hợp đồng
     * @return {@link ContractSignatureListResponse} Danh sách chữ ký kèm mốc thời gian và trạng thái xác nhận
     */
    @GetMapping("/{contractId}/signatures")
    public ContractSignatureListResponse getSignatures(@PathVariable Long contractId) {
        return contractService.getSignatures(contractId);
    }

    /**
     * [UC-22]: Phát sinh và gửi mã xác thực OTP dùng một lần qua email để ký số hợp đồng.
     * 
     * Luồng xử lý:
     *   1. Xác thực người dùng hiện tại là một trong các bên tham gia ký kết.
     *   2. Sinh mã OTP ngẫu nhiên 6 chữ số có hiệu lực trong 5 phút.
     *   3. Gửi email thông báo chứa mã OTP kèm số hợp đồng.
     * 
     * @param contractId Mã định danh hợp đồng cần ký
     * @return Map chứa thông báo gửi mã OTP thành công
     */
    @PostMapping("/{contractId}/send-otp")
    public Map<String, Object> sendSignOtp(@PathVariable Long contractId) {
        return contractService.sendOtp(contractId);
    }

    /**
     * [UC-22]: Thực hiện ký số điện tử hợp đồng bằng mã xác thực OTP.
     * 
     * Luồng xử lý:
     *   1. Xác thực mã OTP người dùng nhập vào.
     *   2. Ghi nhận chữ ký số của bên ký (Client hoặc Tutor).
     *   3. Nếu cả hai bên đã ký đủ, hợp đồng chuyển sang trạng thái ACTIVE và kích hoạt Escrow.
     * 
     * @param contractId Mã định danh hợp đồng
     * @param request Dữ liệu ký gồm mã OTP và thông tin người ký {@link SignContractRequest}
     * @return {@link ContractResponse} Hợp đồng sau khi cập nhật chữ ký số
     */
    @PostMapping("/{contractId}/sign")
    public ContractResponse signContract(
            @PathVariable Long contractId,
            @RequestBody SignContractRequest request) {
        return contractService.signContract(contractId, request);
    }

    /**
     * [UC-21]: Gia sư từ chối thỏa thuận hợp tác giảng dạy với trung tâm gia sư.
     * 
     * @param contractId Mã định danh hợp đồng hợp tác
     * @return Map chứa thông báo xác nhận đã từ chối
     */
    @PostMapping("/{contractId}/decline")
    public Map<String, String> declineCooperation(@PathVariable Long contractId) {
        contractService.declineCooperationContract(contractId);
        return Map.of("message", "Đã từ chối thỏa thuận hợp tác");
    }

    /**
     * [UC-23]: Cập nhật thông tin tài khoản ngân hàng thụ hưởng nhận tiền bồi hoàn hợp đồng.
     * 
     * @param contractId Mã định danh hợp đồng cần bồi hoàn
     * @param request Thông tin tài khoản ngân hàng thụ hưởng {@link SaveRefundPayoutRequest}
     * @return {@link ContractResponse} Hợp đồng sau khi lưu thông tin giải ngân
     */
    @PostMapping("/{contractId}/refund-payout")
    public ContractResponse saveRefundPayoutInfo(
            @PathVariable Long contractId,
            @RequestBody SaveRefundPayoutRequest request) {
        return contractService.saveRefundPayoutInfo(contractId, request);
    }

    /**
     * [UC-29]: Gia sư gửi phản hồi giải trình hoặc cảm ơn đối với nhận xét đánh giá của học viên.
     * 
     * @param reviewId ID của đánh giá nhận xét
     * @param request Nội dung phản hồi của gia sư {@link ReplyReviewRequest}
     * @return {@link ReviewResponse} Đánh giá sau khi cập nhật phản hồi của gia sư
     */
    @PostMapping("/reviews/{reviewId}/reply")
    public ReviewResponse replyToReview(
            @PathVariable Long reviewId, @RequestBody ReplyReviewRequest request) {
        return contractService.replyToReview(reviewId, request);
    }

    /**
     * [UC-29]: Chỉnh sửa nội dung đánh giá và điểm số đã gửi trong thời hạn cho phép.
     * 
     * @param reviewId ID đánh giá cần chỉnh sửa
     * @param request Dữ liệu đánh giá cập nhật {@link CreateReviewRequest}
     * @return {@link ReviewResponse} Đánh giá sau khi chỉnh sửa
     */
    @PutMapping("/reviews/{reviewId}")
    public ReviewResponse updateReview(
            @PathVariable Long reviewId, @RequestBody CreateReviewRequest request) {
        return contractService.updateReview(reviewId, request);
    }

    /**
     * [UC-30]: Tra cứu chỉ số uy tín (Reputation Score) và thống kê đánh giá của một gia sư cụ thể.
     * 
     * @param tutorId ID gia sư
     * @return {@link TutorReputationResponse} Điểm uy tín, tỷ lệ hoàn thành lớp và phân bổ điểm sao
     */
    @GetMapping("/reviews/reputation/{tutorId}")
    public TutorReputationResponse getTutorReputation(@PathVariable Long tutorId) {
        return contractService.getTutorReputation(tutorId);
    }

    /**
     * [UC-30]: Gia sư tự tra cứu chỉ số uy tín và xếp hạng danh tiếng của chính mình.
     * 
     * @return {@link TutorReputationResponse} Điểm uy tín cá nhân và phản hồi chi tiết từ học viên
     */
    @GetMapping("/reviews/my-reputation")
    public TutorReputationResponse getMyTutorReputation() {
        return contractService.getMyTutorReputation();
    }

    /**
     * [UC-29]: Lấy danh sách các lớp học đã kết thúc mà học viên đủ điều kiện viết đánh giá.
     * 
     * @return Danh sách các phân công lớp có thể đánh giá {@link ReviewableAssignmentResponse}
     */
    @GetMapping("/reviews/reviewable")
    public List<ReviewableAssignmentResponse> getMyReviewableAssignments() {
        return contractService.getMyReviewableAssignments();
    }
}
