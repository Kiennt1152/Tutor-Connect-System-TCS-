package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Trung tâm tạo/sửa mẫu hợp đồng của riêng mình. */
@Getter
@Setter
public class SaveContractTemplateRequest {

    private String name;
    private String content;
}
