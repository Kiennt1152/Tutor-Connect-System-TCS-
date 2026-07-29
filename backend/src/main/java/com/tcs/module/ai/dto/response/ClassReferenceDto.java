package com.tcs.module.ai.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClassReferenceDto {
    Long classId;
    String title;
    String subjectName;
    String gradeLevelName;
    BigDecimal tuitionFee;
    String location;
    String status;
}
