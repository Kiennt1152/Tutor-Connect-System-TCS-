package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.PageTaskItemResponse;
import com.tcs.module.platform.dto.response.TaskQueueSummaryResponse;

public interface PlatformTaskQueueService {
    TaskQueueSummaryResponse getSummary();
    PageTaskItemResponse listTasks(String type, String priority, Boolean slaBreached, int page, int size);

    default PageTaskItemResponse listTasks(String type, int page, int size) {
        return listTasks(type, null, null, page, size);
    }
}
