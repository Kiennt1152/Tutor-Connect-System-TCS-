package com.tcs.tutorprofile.controller;

import com.tcs.tutorprofile.service.TutorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutor-profiles")
@RequiredArgsConstructor
public class TutorProfileController {

    private final TutorProfileService tutorProfileService;
}
