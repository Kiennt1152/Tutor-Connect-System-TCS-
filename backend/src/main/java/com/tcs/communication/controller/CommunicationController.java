package com.tcs.communication.controller;

import com.tcs.communication.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/communication")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService communicationService;
}
