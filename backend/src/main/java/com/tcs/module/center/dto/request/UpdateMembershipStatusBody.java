package com.tcs.module.center.dto.request;

import com.tcs.module.center.enums.CenterTutorMembershipStatus;
import lombok.Getter;
import lombok.Setter;

/** Trung tâm đổi trạng thái thành viên gia sư (ACTIVE / INACTIVE / TERMINATED). */
@Getter
@Setter
public class UpdateMembershipStatusBody {

    private CenterTutorMembershipStatus status;
}
