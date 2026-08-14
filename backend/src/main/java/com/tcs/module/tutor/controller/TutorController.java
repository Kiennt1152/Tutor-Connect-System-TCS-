package com.tcs.module.tutor.controller;

import com.tcs.module.center.dto.request.BatchAttendanceRequest;
import com.tcs.module.center.dto.request.MarkAttendanceRequest;
import com.tcs.module.center.dto.request.RescheduleRequestBody;
import com.tcs.module.center.dto.request.SubstituteRequestBody;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.RescheduleResponse;
import com.tcs.module.center.dto.response.SubstitutionResponse;
import com.tcs.module.tutor.service.TutorService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

    @GetMapping("/schedule")
    public List<CenterScheduleClassResponse> getSchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tutorService.getSchedule(date);
    }

    @GetMapping("/classes/{classId}/session")
    public CenterScheduleClassResponse getSession(
            @PathVariable Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tutorService.getClassSession(classId, date);
    }

    @PostMapping("/classes/{classId}/attendance")
    public CenterScheduleClassResponse markAttendance(
            @PathVariable Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody MarkAttendanceRequest request) {
        return tutorService.markAttendance(classId, date, request.getClassStudentId(), request.getStatus());
    }

    @PostMapping("/classes/{classId}/attendance/batch")
    public CenterScheduleClassResponse markAttendanceBatch(
            @PathVariable Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody BatchAttendanceRequest request) {
        return tutorService.markAttendanceBatch(classId, date, request.getRecords());
    }

    @PostMapping("/classes/{classId}/complete")
    public java.util.Map<String, String> confirmClassCompletion(@PathVariable Long classId) {
        return java.util.Map.of("message", tutorService.confirmClassCompletion(classId));
    }

    @PostMapping("/classes/{classId}/reschedule")
    public RescheduleResponse requestReschedule(
            @PathVariable Long classId, @RequestBody RescheduleRequestBody request) {
        return tutorService.requestReschedule(classId, request);
    }

    @GetMapping("/reschedules")
    public List<RescheduleResponse> myReschedules() {
        return tutorService.listMyReschedules();
    }

    @PostMapping("/classes/{classId}/substitute")
    public SubstitutionResponse requestSubstitute(
            @PathVariable Long classId, @RequestBody SubstituteRequestBody request) {
        return tutorService.requestSubstitute(classId, request);
    }

    @GetMapping("/substitutions")
    public List<SubstitutionResponse> mySubstitutions() {
        return tutorService.listMySubstitutions();
    }
}
