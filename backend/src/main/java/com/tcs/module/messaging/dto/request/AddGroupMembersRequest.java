package com.tcs.module.messaging.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddGroupMembersRequest {

    private List<Long> memberIds;
}
