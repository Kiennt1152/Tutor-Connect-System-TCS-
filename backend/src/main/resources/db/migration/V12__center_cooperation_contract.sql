-- BF-03: Thỏa thuận hợp tác giữa Trung tâm và Gia sư (không gắn với lớp học).
-- Tái dùng bảng contracts + contract_signatures cho hợp đồng loại này.
-- Cho phép contract KHÔNG gắn assignment lẫn class_student, mà gắn recruitment_application.

-- 1. Bỏ ràng buộc cũ (đúng 1 trong assignment/class_student).
ALTER TABLE contracts DROP CHECK chk_contracts_target;

-- 2. Thêm liên kết tới đơn ứng tuyển tuyển dụng (nguồn của thỏa thuận hợp tác).
ALTER TABLE contracts
    ADD COLUMN recruitment_application_id BIGINT NULL AFTER class_student_id;

ALTER TABLE contracts
    ADD CONSTRAINT fk_contracts_recruitment_app
    FOREIGN KEY (recruitment_application_id)
    REFERENCES recruitment_applications (recruitment_app_id);

-- 3. Ràng buộc mới: hợp đồng phải gắn ĐÚNG 1 trong 3 nguồn
--    (lớp / học sinh trong lớp / đơn tuyển dụng).
ALTER TABLE contracts
    ADD CONSTRAINT chk_contracts_target
    CHECK (
        (assignment_id IS NOT NULL)
      + (class_student_id IS NOT NULL)
      + (recruitment_application_id IS NOT NULL) = 1
    );

-- 4. Ô chữ ký được tạo trước khi ký (signer + signed_at chỉ điền khi bên đó thực sự ký OTP)
--    -> signer_id và signed_at phải cho phép NULL (cho cả hợp đồng lớp lẫn hợp đồng hợp tác).
ALTER TABLE contract_signatures MODIFY signer_id BIGINT NULL;
ALTER TABLE contract_signatures MODIFY signed_at DATETIME NULL;
