package com.tcs.module.platform.service;

public interface PenaltyAccessService {
    void requireFeature(Long userId, String featureCode);
}
