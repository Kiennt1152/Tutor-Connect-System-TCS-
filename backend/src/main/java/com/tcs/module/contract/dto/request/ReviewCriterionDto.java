package com.tcs.module.contract.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCriterionDto {

    private String code;
    private String question;
    private Integer score;
}
