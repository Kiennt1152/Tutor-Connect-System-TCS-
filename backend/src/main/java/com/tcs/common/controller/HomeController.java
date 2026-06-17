package com.tcs.common.controller;

import com.tcs.common.dto.response.HomeResponse;
import com.tcs.common.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public HomeResponse getHome() {
        return homeService.getHomeData();
    }
}
