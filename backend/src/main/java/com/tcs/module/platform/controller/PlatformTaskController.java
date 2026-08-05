package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.response.PageTaskItemResponse;
import com.tcs.module.platform.dto.response.TaskQueueSummaryResponse;
import com.tcs.module.platform.service.PlatformTaskQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/tasks")
@RequiredArgsConstructor
public class PlatformTaskController {
    private final PlatformTaskQueueService taskQueueService;

    @GetMapping("/summary")
    public TaskQueueSummaryResponse getSummary() {
        return taskQueueService.getSummary();
    }

    @GetMapping
    public PageTaskItemResponse listTasks(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return taskQueueService.listTasks(type, page, size);
    }
}
