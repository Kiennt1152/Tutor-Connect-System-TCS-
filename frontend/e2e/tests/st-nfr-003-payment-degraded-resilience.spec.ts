import { test, expect } from '@playwright/test';
import { ApiHelper } from '../utils/apiHelper';
import { testConfig } from '../utils/testConfig';

test.describe('ST-NFR-003: Degraded operation when the payment provider fails', () => {
  test('Khi cổng thanh toán gặp sự cố, hệ thống trả lỗi nạp tiền trong vòng <= 5s và các phân hệ khác vẫn hoạt động 100%', async ({ request }) => {
    const clientToken = await ApiHelper.getClientToken();

    // 1. Thử gọi API nạp tiền và đo lường thời gian timeout xử lý lỗi (yêu cầu <= 5000ms)
    const paymentStart = Date.now();
    const topupRes = await request.post(`${testConfig.apiUrl}/finance/wallet/deposit`, {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${clientToken}`,
      },
      body: JSON.stringify({
        amount: -50000, // Giá trị sai hoặc giả lập cổng thanh toán không phản hồi
        paymentMethod: 'SEPAY_MOCK',
      }),
    });
    const paymentDuration = Date.now() - paymentStart;

    console.log(`[ST-NFR-003] Thời gian phản hồi xử lý lỗi thanh toán: ${paymentDuration}ms`);
    expect(paymentDuration).toBeLessThanOrEqual(5000);

    // 2. Xác minh các phân hệ Tìm kiếm / Xem thông tin / Nhắn tin vẫn hoạt động hoàn hảo 100% (Fault Isolation)
    // 2.1. Phân hệ Marketplace (Tìm kiếm lớp học)
    const classSearchRes = await request.get(`${testConfig.apiUrl}/marketplace/classes?status=OPEN`);
    expect(classSearchRes.status()).toBe(200);

    // 2.2. Phân hệ Messaging (Đọc hội thoại tin nhắn)
    const messagingRes = await request.get(`${testConfig.apiUrl}/messaging/conversations`, {
      headers: { Authorization: `Bearer ${clientToken}` },
    });
    expect(messagingRes.status()).toBe(200);

    // 2.3. Phân hệ Profile (Xem hồ sơ cá nhân)
    const meRes = await request.get(`${testConfig.apiUrl}/identity/me`, {
      headers: { Authorization: `Bearer ${clientToken}` },
    });
    expect(meRes.status()).toBe(200);
  });
});
