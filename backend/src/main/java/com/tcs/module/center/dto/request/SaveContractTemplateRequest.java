package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Trung tâm tạo/sửa mẫu hợp đồng của riêng mình. */
@Getter
@Setter
public class SaveContractTemplateRequest {

    private String name;
    private String content;
    /** Loại hợp đồng: RECRUITMENT (tuyển dụng/hợp tác gia sư) hoặc CLASS (học viên/dạy lớp). */
    private String contractType;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
}
