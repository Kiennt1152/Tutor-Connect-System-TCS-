-- UC-36: doi lich mot buoi hoc / them buoi hoc.
-- Ca hai ben (phu huynh va gia su) deu tao duoc yeu cau; ben con lai duyet.
-- Lich chi thuc su thay doi khi yeu cau duoc APPROVED.

CREATE TABLE lesson_reschedule_requests (
    request_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id             BIGINT      NOT NULL,
    -- NULL voi yeu cau THEM BUOI (chua co buoi nao de tro toi).
    lesson_id            BIGINT      NULL,
    request_type         VARCHAR(20) NOT NULL,
    new_date             DATE        NOT NULL,
    new_start_time       TIME        NOT NULL,
    new_end_time         TIME        NOT NULL,
    -- Mon hoc: chi dung cho THEM BUOI; doi lich giu nguyen mon cua buoi cu.
    subject_id           BIGINT      NULL,
    reason               VARCHAR(500) NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by_user_id BIGINT      NOT NULL,
    decided_by_user_id   BIGINT      NULL,
    decided_at           DATETIME    NULL,
    decision_note        VARCHAR(500) NULL,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lrr_class FOREIGN KEY (class_id) REFERENCES tutoring_classes (class_id),
    CONSTRAINT fk_lrr_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (lesson_id),
    CONSTRAINT fk_lrr_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id),
    CONSTRAINT fk_lrr_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_lrr_decided_by FOREIGN KEY (decided_by_user_id) REFERENCES users (user_id)
);

-- Truy van chinh: danh sach yeu cau cua cac lop ma nguoi dang dang nhap tham gia.
CREATE INDEX idx_lrr_class_status ON lesson_reschedule_requests (class_id, status);
CREATE INDEX idx_lrr_lesson ON lesson_reschedule_requests (lesson_id);
