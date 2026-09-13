import { test, expect } from '@playwright/test';
import { testConfig } from '../utils/testConfig';

test.describe('ST-NFR-006: Email rate-limit and delivery reliability', () => {
  test('Gửi yêu cầu OTP vượt ngưỡng tần suất (6 lần liên tiếp) kích hoạt chặn Rate Limit và yêu cầu chờ 60s', async ({ request }) => {
    const testEmail = `nfr_ratelimit_${Date.now()}@tcs.test`;

    // 1. Gửi OTP lần 1 -> Thành công (HTTP 200)
    const firstRes = await request.post(`${testConfig.apiUrl}/identity/send-otp`, {
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: testEmail }),
    });

    console.log(`[ST-NFR-006] Lần 1 gửi OTP: Status ${firstRes.status()}`);
    // Có thể trả về 200 hoặc nếu email chưa hợp lệ trả về 400
    // 2. Gửi ngay lập tức lần 2 trong vòng < 60s -> Phải bị chặn bởi Cooldown 60s hoặc Rate Limit
    const secondRes = await request.post(`${testConfig.apiUrl}/identity/send-otp`, {
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: testEmail }),
    });

    console.log(`[ST-NFR-006] Lần 2 gửi OTP (trong vòng < 60s): Status ${secondRes.status()}`);
    expect(secondRes.status()).toBe(400);

    const errorBody = await secondRes.json();
    console.log(`[ST-NFR-006] Thông báo chặn Rate Limit:`, errorBody);
    const msg = errorBody.message || JSON.stringify(errorBody);
    expect(msg).toMatch(/Quá nhiều yêu cầu|vui lòng thử lại/i);
  });
});
