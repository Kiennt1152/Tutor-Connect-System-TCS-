package com.tcs.module.contract.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Diem cham cho mot tieu chi danh gia (vd: dung gio, de hieu...). */
@Getter
@Setter
public class ReviewCriterionDto {

    private String code;
    private String question;
    private Integer score;
}
