package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.response.PageTaskItemResponse;
import com.tcs.module.platform.dto.response.TaskQueueSummaryResponse;

public interface PlatformTaskQueueService {
    TaskQueueSummaryResponse getSummary();
    PageTaskItemResponse listTasks(String type, int page, int size);
}
