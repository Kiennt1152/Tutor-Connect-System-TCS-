package com.tcs.module.profile.dto.request;

import com.tcs.module.profile.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Họ và tên phải từ 2 đến 50 ký tự")
    private String fullName;

    @Pattern(
            regexp = "^(\\+?84|0)(3|5|7|8|9)[0-9]{8}$",
            message = "Số điện thoại không hợp lệ (10 số, đầu 0 hoặc +84)")
    private String phone;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Size(max = 255, message = "URL ảnh đại diện tối đa 255 ký tự")
    private String avatarUrl;

    @Past(message = "Ngày sinh phải trong quá khứ")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String bio;

    @PositiveOrZero(message = "Số năm kinh nghiệm phải >= 0")
    private Integer experienceYears;

    @PositiveOrZero(message = "Học phí phải >= 0")
    private java.math.BigDecimal hourlyRate;

    @Size(min = 2, max = 50, message = "Tên trung tâm phải từ 2 đến 50 ký tự")
    private String companyName;

    @Size(max = 1000, message = "Mô tả trung tâm tối đa 1000 ký tự")
    private String description;

    @Size(max = 50, message = "Số giấy phép tối đa 50 ký tự")
    private String licenseNo;
}