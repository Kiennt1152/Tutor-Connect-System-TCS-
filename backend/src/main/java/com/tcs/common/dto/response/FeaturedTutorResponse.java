package com.tcs.common.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedTutorResponse {
    private String id;
    private String fullName;
    private String gender;
    private String bio;
    private BigDecimal hourlyRate;
    private BigDecimal ratingAvg;
    private Integer experienceYears;
}
