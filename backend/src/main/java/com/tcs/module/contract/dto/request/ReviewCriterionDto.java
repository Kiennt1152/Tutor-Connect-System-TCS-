package com.tcs.module.contract.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCriterionDto {

    private String code;
    private String question;
    private Integer score;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
