package com.tcs.common.service.impl;

import com.tcs.common.dto.response.HomeResponse;
import com.tcs.common.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {



    @Override
    @Transactional(readOnly = true)
    public HomeResponse getHomeData() {
        return HomeResponse.builder()
                .welcomeMessage("Hello Tutor Connect System")
                .build();
    }
}
