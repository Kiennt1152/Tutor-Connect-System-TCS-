package com.tcs.module.catalog.service;

import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.dto.response.SystemParameterResponse;
import java.util.List;

public interface SystemParameterService {

    /** Liệt kê tham số hệ thống, có thể lọc theo tiền tố khóa (prefix) hoặc từ khóa. */
    List<SystemParameterResponse> getParameters(String prefix, String keyword);

    SystemParameterResponse getParameter(Long parameterId);

    SystemParameterResponse createParameter(UpsertSystemParameterRequest request);

    SystemParameterResponse updateParameter(Long parameterId, UpsertSystemParameterRequest request);

    void deleteParameter(Long parameterId);
}
