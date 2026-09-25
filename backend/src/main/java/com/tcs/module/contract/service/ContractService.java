package com.tcs.module.contract.service;

import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.OtpSentResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.SignatureStatusResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.entity.Contract;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * [UC-44] GIAO DIỆN DỊCH VỤ HỢP ĐỒNG ĐIỆN TỬ (E-CONTRACT SERVICE INTERFACE)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-06-23
 * 
 * 1. Mục đích & Chức năng:
 *    - Định nghĩa các hợp đồng nghiệp vụ cho việc tạo, ký số, giải ngân và quản lý vòng đời hợp đồng.
 *    - Hỗ trợ cả 2 loại hợp đồng: Thỏa thuận hợp tác Trung tâm - Gia sư (BF-03) và Hợp đồng dạy kèm Phụ huynh - Gia sư (UC-44).
 *    - Tích hợp chữ ký số 2FA qua OTP Email và đánh giá uy tín sau hoàn thành (BF-07).
 * 
 * 2. Các nghiệp vụ chính:
 *    - generateCooperationContract / generatePrivateClassContract: Tự động phát sinh hợp đồng từ mẫu pháp lý.
 *    - sendSigningOtp / signContractWithOtp: Sinh mã OTP bảo mật gửi qua Email và xác thực chữ ký số 2 bên.
 *    - checkSignatureStatus: Kiểm tra trạng thái ký của các bên tham gia (Client, Tutor, Center).
 *    - createReview / replyReview: Quản trị đánh giá phản hồi chất lượng đào tạo và tính điểm uy tín.
 * ============================================================================
 */
public interface ContractService {

    /**
     * [BF-03] Tạo thỏa thuận hợp tác Trung tâm - Gia sư từ đơn ứng tuyển tuyển dụng đã duyệt.
     * 
     * Luồng xử lý:
     * 1. Xác thực đơn ứng tuyển tuyển dụng (RecruitmentApplication) hợp lệ và chưa từng tạo hợp đồng.
     * 2. Ưu tiên lấy nội dung điều khoản tùy chỉnh (editedTerms), nếu không lấy từ mẫu hợp đồng (templateId), hoặc điều khoản mặc định.
     * 3. Điền các placeholder thông tin hai bên (tên gia sư, tên trung tâm, ngày ký) và đóng băng điều khoản.
     * 4. Trung tâm được ký sẵn (presigned), tạo ô ký chờ gia sư ký xác nhận trong vòng 48 giờ.
     * 
     * @param recruitmentApplicationId Định danh đơn ứng tuyển tuyển dụng
     * @param templateId Định danh mẫu hợp đồng trung tâm lựa chọn (tùy chọn)
     * @param editedTerms Nội dung điều khoản trung tâm chỉnh sửa riêng (tùy chọn)
     * @return ContractResponse chi tiết thỏa thuận hợp tác vừa tạo
     */
    ContractResponse generateCooperationContract(
            Long recruitmentApplicationId, Long templateId, String editedTerms);

    /**
     * [BF-03] Gia sư từ chối thỏa thuận hợp tác khi chưa ký.
     * 
     * Luồng xử lý:
     * 1. Xác thực người từ chối chính là gia sư của đơn ứng tuyển.
     * 2. Chuyển trạng thái hợp đồng sang TERMINATED (chấm dứt).
     * 3. Đóng đơn ứng tuyển sang trạng thái WITHDRAWN để trung tâm tiếp tục tuyển dụng.
     * 
     * @param contractId Định danh hợp đồng bị từ chối
     */
    void declineCooperationContract(Long contractId);

    /**
     * [BF-04] Tạo hợp đồng đào tạo dành cho học viên khi ghi danh vào lớp trung tâm.
     * 
     * Luồng xử lý:
     * 1. Kiểm tra bản ghi ghi danh ClassStudent tồn tại.
     * 2. Nếu đã có hợp đồng ghi danh thì trả về hợp đồng hiện tại (idempotent).
     * 3. Ngược lại, khởi tạo hợp đồng từ mẫu của lớp trung tâm, phân bổ ô ký cho học viên/phụ huynh.
     * 
     * @param classStudentId Định danh học viên ghi danh lớp
     * @return ContractResponse thông tin hợp đồng đào tạo
     */
    ContractResponse generateStudentContract(Long classStudentId);

    /**
     * [UC-44] Tra cứu thông tin hợp đồng điện tử theo ID (ủy quyền sang getMyContract).
     * 
     * @param contractId Định danh hợp đồng
     * @return ContractResponse chi tiết hợp đồng
     */
    ContractResponse getContract(Long contractId);

    /**
     * [UC-44] Lấy danh sách chữ ký số và tiến độ ký kết của các bên tham gia hợp đồng.
     * 
     * @param contractId Định danh hợp đồng
     * @return ContractSignatureListResponse danh sách chi tiết các ô ký, người ký và trạng thái
     */
    ContractSignatureListResponse getSignatures(Long contractId);

    /**
     * [UC-44] Khởi tạo và gửi mã xác thực ký số OTP qua email cho người dùng hiện tại.
     * 
     * Luồng xử lý:
     * 1. Xác thực người ký không phải vị thành niên và đã hoàn tất xác minh CCCD.
     * 2. Xác định vai trò của người gọi trong hợp đồng (CLIENT, TUTOR, CENTER).
     * 3. Kiểm tra trạng thái hợp đồng đang chờ ký (PENDING/DRAFT) và ô ký chưa hoàn thành.
     * 4. Phát sinh mã OTP 6 chữ số qua OtpService, gửi qua Email và trả về thông tin email được che.
     * 
     * @param contractId Định danh hợp đồng cần ký
     * @return Map chứa thông điệp, email được che (maskedEmail) và thời gian hết hạn OTP
     */
    Map<String, Object> sendOtp(Long contractId);

    /**
     * [UC-44] Thực hiện ký số hợp đồng điện tử bằng mã xác thực OTP qua Email.
     * 
     * Luồng xử lý:
     * 1. Xác thực người ký không phải vị thành niên và đã xác minh CCCD.
     * 2. Xác thực mã OTP thông qua OtpService (kiểm tra hạn dùng và số lần nhập sai).
     * 3. Đánh dấu ô ký thành SIGNED, lưu bằng chứng xác thực OTP (OTP_VERIFIED).
     * 4. Đồng bộ mốc thời gian ký vào ClassAssignment tương ứng.
     * 5. Nếu tất cả các bên đã ký đủ: chuyển hợp đồng sang SIGNED, kích hoạt Escrow và phát sự kiện hoàn tất.
     * 
     * @param contractId Định danh hợp đồng
     * @param request Dữ liệu chứa mã OTP
     * @return ContractResponse thông tin hợp đồng sau khi ký số
     */
    ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request);

    /**
     * [UC-44] Lưu thông tin tài khoản ngân hàng nhận tiền hoàn (dành riêng cho Phụ huynh sau khi ký).
     * 
     * @param contractId Định danh hợp đồng
     * @param request Thông tin tài khoản ngân hàng nhận hoàn tiền
     * @return ContractResponse thông tin hợp đồng sau khi cập nhật
     */
    ContractResponse saveRefundPayoutInfo(Long contractId, SaveRefundPayoutRequest request);

    /**
     * [UC-44] Tạo hợp đồng điện tử cho phân công lớp học (gọi generateForAssignment và trả về DTO).
     * 
     * @param assignmentId Định danh phân công lớp học
     * @return ContractResponse thông tin hợp đồng vừa tạo
     */
    ContractResponse generateContract(Long assignmentId);

    /**
     * [UC-44] Khởi tạo thực thể hợp đồng điện tử từ phân công lớp học cá nhân.
     * 
     * @param assignmentId Định danh phân công lớp
     * @return Thực thể Contract được lưu trữ ở trạng thái PENDING
     */
    Contract generateForAssignment(Long assignmentId);

    /**
     * [BF-04] Khởi tạo thực thể hợp đồng đào tạo từ bản ghi ghi danh lớp trung tâm.
     * 
     * @param classStudentId Định danh học viên ghi danh
     * @return Thực thể Contract được khởi tạo
     */
    Contract generateForEnrollment(Long classStudentId);

    /**
     * [UC-44] Ký số hợp đồng với người ký và mã OTP được chỉ định rõ ràng.
     * 
     * @param contractId Định danh hợp đồng
     * @param otp Mã OTP xác thực
     * @param signerUserId Định danh người ký
     */
    void sign(Long contractId, String otp, Long signerUserId);

    /**
     * [UC-44] Kiểm tra xem hợp đồng đã được tất cả các bên ký kết đầy đủ hay chưa.
     * 
     * @param contractId Định danh hợp đồng
     * @return true nếu số lượng chữ ký đã hoàn thành >= số lượng chữ ký yêu cầu
     */
    boolean isFullySigned(Long contractId);

    /**
     * [UC-44] Tra cứu thông tin hợp đồng thuộc quyền sở hữu/tham gia của người dùng hiện tại.
     * 
     * @param contractId Định danh hợp đồng
     * @return ContractResponse chi tiết hợp đồng
     * @throws com.tcs.exception.ForbiddenException nếu người dùng không có quyền xem hợp đồng
     */
    ContractResponse getMyContract(Long contractId);

    /**
     * [UC-44] Lấy danh sách toàn bộ các hợp đồng điện tử mà người dùng hiện tại có liên quan (Phụ huynh, Gia sư, Trung tâm).
     * 
     * @return Danh sách ContractResponse
     */
    List<ContractResponse> getMyContracts();

    /**
     * [UC-44] Gửi mã OTP xác thực ký hợp đồng điện tử qua email cho người dùng (trả về DTO chuẩn).
     * 
     * @param contractId ID hợp đồng cần ký
     * @return OtpSentResponse chứa thông tin email được che và thông điệp xác nhận
     */
    OtpSentResponse sendSignOtp(Long contractId);

    /**
     * [UC-44] Ký hợp đồng điện tử thông qua DTO SignContractRequest.
     * 
     * @param contractId ID hợp đồng
     * @param request DTO chứa mã OTP ký
     * @return ContractResponse thông tin hợp đồng sau ký
     */
    ContractResponse signContract(Long contractId, SignContractRequest request);

    /**
     * [UC-44] Tra cứu trạng thái chi tiết chữ ký của tất cả các bên trong hợp đồng.
     * 
     * @param contractId ID hợp đồng
     * @return SignatureStatusResponse chi tiết tiến độ ký và danh sách người đã ký
     */
    SignatureStatusResponse getSignatureStatus(Long contractId);

    /**
     * [BF-07] [UC-44] Phụ huynh gửi đánh giá sao và nhận xét chất lượng dạy học của gia sư.
     * 
     * Luồng xử lý:
     * 1. Xác thực quyền CLIENT của người gọi.
     * 2. Kiểm tra các buổi học đã diễn ra qua danh sách điểm danh; đảm bảo chưa vượt số lượt đánh giá cho phép.
     * 3. Tính điểm trung bình tổng thể từ bộ tiêu chí đánh giá chi tiết (1-5 sao).
     * 4. Lưu đánh giá và tự động tính toán lại điểm uy tín gia sư (recomputeTutorReputation).
     * 5. Phát sự kiện ClientReviewedClassEvent để marketplace kiểm tra điều kiện tất toán lớp.
     * 
     * @param request Dữ liệu đánh giá bao gồm assignmentId, rating, comment, criteria
     * @return ReviewResponse thông tin đánh giá đã lưu
     */
    ReviewResponse createReview(CreateReviewRequest request);

    /**
     * [BF-07] Kiểm tra xem phụ huynh/học viên đã từng đánh giá lớp học này hay chưa.
     * 
     * @param classId ID lớp học
     * @return true nếu đã có ít nhất một đánh giá từ phụ huynh
     */
    boolean hasClientReviewedClass(Long classId);

    /**
     * [BF-07] Gia sư gửi phản hồi giải trình/cảm ơn đối với đánh giá nhận được từ phụ huynh.
     * 
     * @param reviewId ID đánh giá
     * @param request Dữ liệu phản hồi
     * @return ReviewResponse thông tin đánh giá sau khi bổ sung phản hồi
     */
    ReviewResponse replyToReview(Long reviewId, ReplyReviewRequest request);

    /**
     * [BF-07] Phụ huynh cập nhật, chỉnh sửa nội dung đánh giá đã gửi trước đó.
     * 
     * @param reviewId ID đánh giá cần sửa
     * @param request Dữ liệu đánh giá cập nhật
     * @return ReviewResponse thông tin đánh giá sau khi cập nhật
     */
    ReviewResponse updateReview(Long reviewId, CreateReviewRequest request);

    /**
     * [BF-07] Tính toán lại điểm số uy tín của Gia sư dựa trên toàn bộ đánh giá đang hiển thị.
     * 
     * @param tutorUserId ID người dùng của gia sư
     */
    void recomputeReputationByTutorUser(Long tutorUserId);

    /**
     * [BF-07] Lấy hồ sơ đánh giá và điểm số uy tín công khai của Gia sư theo tutorId.
     * 
     * @param tutorId ID gia sư
     * @return TutorReputationResponse chi tiết điểm trung bình, phân bổ sao và danh sách đánh giá
     */
    TutorReputationResponse getTutorReputation(Long tutorId);

    /**
     * [BF-07] Gia sư tra cứu bảng điểm uy tín và thống kê đánh giá của chính mình.
     * 
     * @return TutorReputationResponse chi tiết hồ sơ uy tín gia sư
     */
    TutorReputationResponse getMyTutorReputation();

    /**
     * [BF-07] Phụ huynh lấy danh sách các phân công lớp học đủ điều kiện gửi đánh giá hoặc đã đánh giá.
     * 
     * @return Danh sách ReviewableAssignmentResponse kèm số lượt đánh giá còn lại và trạng thái quá hạn
     */
    List<ReviewableAssignmentResponse> getMyReviewableAssignments();
}
