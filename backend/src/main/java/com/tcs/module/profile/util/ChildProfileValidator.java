package com.tcs.module.profile.util;

import com.tcs.module.profile.enums.Gender;
import java.time.LocalDate;
import org.springframework.util.StringUtils;

public final class ChildProfileValidator {

    public static final int FULL_NAME_MIN_LENGTH = 2;
    public static final int FULL_NAME_MAX_LENGTH = 100;
    public static final int SCHOOL_NAME_MAX_LENGTH = 200;
    public static final int NOTES_MAX_LENGTH = 2000;

    private ChildProfileValidator() {}

    public static String requireFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            throw new IllegalArgumentException("Tên con là bắt buộc");
        }
        return normalizeFullName(fullName);
    }

    public static String normalizeFullName(String fullName) {
        String trimmed = fullName.trim().replaceAll("\\s+", " ");
        if (trimmed.length() < FULL_NAME_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Tên con phải có ít nhất " + FULL_NAME_MIN_LENGTH + " ký tự");
        }
        if (trimmed.length() > FULL_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Tên con không được vượt quá " + FULL_NAME_MAX_LENGTH + " ký tự");
        }
        return trimmed;
    }

    public static LocalDate validateChildDateOfBirth(LocalDate dateOfBirth, boolean required) {
        if (dateOfBirth == null) {
            if (required) {
                throw new IllegalArgumentException("Ngày sinh là bắt buộc");
            }
            return null;
        }
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
        }
        if (dateOfBirth.isBefore(LocalDate.now().minusYears(100))) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ");
        }
        if (!AgeUtils.isMinor(dateOfBirth)) {
            throw new IllegalArgumentException(
                    "Ngày sinh phải thuộc độ tuổi vị thành niên (dưới 18 tuổi)");
        }
        return dateOfBirth;
    }

    public static void validateGender(Gender gender) {
        if (gender == null) {
            return;
        }
        if (gender != Gender.MALE && gender != Gender.FEMALE) {
            throw new IllegalArgumentException("Giới tính chỉ được chọn Nam hoặc Nữ");
        }
    }

    public static String normalizeSchoolName(String schoolName) {
        if (schoolName == null) {
            return null;
        }
        String trimmed = schoolName.trim();
        if (trimmed.length() > SCHOOL_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Tên trường không được vượt quá " + SCHOOL_NAME_MAX_LENGTH + " ký tự");
        }
        return trimmed;
    }

    public static String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String trimmed = notes.trim();
        if (trimmed.length() > NOTES_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Ghi chú không được vượt quá " + NOTES_MAX_LENGTH + " ký tự");
        }
        return trimmed;
    }

    public static void validateGradeId(Long gradeId) {
        if (gradeId == null || gradeId == 0) {
            return;
        }
        if (gradeId < 0) {
            throw new IllegalArgumentException("Khối/lớp không hợp lệ");
        }
    }

    public static Long requireChildProfileId(Long childProfileId) {
        if (childProfileId == null || childProfileId <= 0) {
            throw new IllegalArgumentException("Mã hồ sơ con không hợp lệ");
        }
        return childProfileId;
    }
}
