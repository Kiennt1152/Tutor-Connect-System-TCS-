package com.tcs.module.ai.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassReferenceDto {
    private Long classId;
    private String title;
    private String subjectName;
    private String gradeLevelName;
    private BigDecimal tuitionFee;
    private String location;
    private String status;

    public Long getClassId() { return classId; }
    public String getTitle() { return title; }
    public String getSubjectName() { return subjectName; }
    public String getGradeLevelName() { return gradeLevelName; }
    public BigDecimal getTuitionFee() { return tuitionFee; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
}
