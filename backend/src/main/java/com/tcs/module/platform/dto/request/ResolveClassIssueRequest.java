package com.tcs.module.platform.dto.request;

import com.tcs.module.platform.enums.ClassIssueResolutionAction;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveClassIssueRequest {

    private ClassIssueResolutionAction action;
    private String notes;
}
