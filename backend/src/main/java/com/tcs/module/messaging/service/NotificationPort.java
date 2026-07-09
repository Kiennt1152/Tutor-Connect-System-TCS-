package com.tcs.module.messaging.service;

import java.util.Map;

/**
 * Seam 0.7 (dung chung). Mot diem goi thong bao (UC-37) cho ca 4 module,
 * boc quanh MessagingService de tranh moi module tu lam mot kieu.
 */
public interface NotificationPort {

    void notify(Long userId, String templateCode, Map<String, Object> params);
}
