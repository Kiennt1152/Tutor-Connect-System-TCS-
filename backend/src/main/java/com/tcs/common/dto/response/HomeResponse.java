package com.tcs.common.dto.response;

import java.util.List;
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
public class HomeResponse {
    private long totalTutors;
    private long totalSubjects;
    private long totalClasses;
    private List<SubjectResponse> subjects;
    private List<FeaturedTutorResponse> featuredTutors;
}
