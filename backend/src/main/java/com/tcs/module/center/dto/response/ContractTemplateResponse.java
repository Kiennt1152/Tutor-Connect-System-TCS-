package com.tcs.module.center.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mẫu hợp đồng hiển thị cho trung tâm chọn/quản lý. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateResponse {

    private Long templateId;
    private String name;
    private String content;
    /** RECRUITMENT (tuyển dụng/hợp tác) hoặc CLASS (học viên/dạy lớp). Mặc định CLASS. */
    private String contractType;
    private Boolean defaultTemplate;
    private String status;
    /** true = mẫu hệ thống dùng chung (không sửa được); false = mẫu của trung tâm. */
    private Boolean system;

    public ContractTemplateResponse() {}

    public ContractTemplateResponse(Long templateId, String name, String content, String contractType, Boolean defaultTemplate, String status, Boolean system) {
        this.templateId = templateId;
        this.name = name;
        this.content = content;
        this.contractType = contractType;
        this.defaultTemplate = defaultTemplate;
        this.status = status;
        this.system = system;
    }

    public Long getTemplateId() { return templateId; }
    public String getName() { return name; }
    public String getContent() { return content; }
    public String getContractType() { return contractType; }
    public Boolean getDefaultTemplate() { return defaultTemplate; }
    public String getStatus() { return status; }
    public Boolean getSystem() { return system; }

    public static ContractTemplateResponseBuilder builder() {
        return new ContractTemplateResponseBuilder();
    }

    public static class ContractTemplateResponseBuilder {
        private Long templateId;
        private String name;
        private String content;
        private String contractType;
        private Boolean defaultTemplate;
        private String status;
        private Boolean system;

        public ContractTemplateResponseBuilder templateId(Long templateId) { this.templateId = templateId; return this; }
        public ContractTemplateResponseBuilder name(String name) { this.name = name; return this; }
        public ContractTemplateResponseBuilder content(String content) { this.content = content; return this; }
        public ContractTemplateResponseBuilder contractType(String contractType) { this.contractType = contractType; return this; }
        public ContractTemplateResponseBuilder defaultTemplate(Boolean defaultTemplate) { this.defaultTemplate = defaultTemplate; return this; }
        public ContractTemplateResponseBuilder status(String status) { this.status = status; return this; }
        public ContractTemplateResponseBuilder system(Boolean system) { this.system = system; return this; }

        public ContractTemplateResponse build() {
            return new ContractTemplateResponse(templateId, name, content, contractType, defaultTemplate, status, system);
        }
    }
}
