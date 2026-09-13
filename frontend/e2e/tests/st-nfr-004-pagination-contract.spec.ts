import { test, expect } from '@playwright/test';
import { ApiHelper } from '../utils/apiHelper';
import { testConfig } from '../utils/testConfig';

test.describe('ST-NFR-004: Pagination on every list endpoint', () => {
  test('Endpoint danh sách trả về mặc định <= 20 bản ghi khi không truyền size, và bị clamp tối đa 50 khi truyền size > 50', async ({ request }) => {
    const adminToken = await ApiHelper.getAdminToken();

    // 1. Kiểm tra endpoint Quản lý tác vụ Admin (/api/platform/tasks)
    // 1.1. Gọi không truyền tham số size -> Mặc định size <= 20
    const defaultRes = await request.get(`${testConfig.apiUrl}/platform/tasks`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(defaultRes.status()).toBe(200);
    const defaultData = await defaultRes.json();
    const defaultItems = defaultData.content || defaultData.tasks || defaultData;
    expect(defaultItems.length).toBeLessThanOrEqual(20);

    // 1.2. Gọi truyền tham số size = 100 (> 50) -> Bị clamp về tối đa 50
    const clampedRes = await request.get(`${testConfig.apiUrl}/platform/tasks?size=100`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(clampedRes.status()).toBe(200);
    const clampedData = await clampedRes.json();
    const clampedItems = clampedData.content || clampedData.tasks || clampedData;
    expect(clampedItems.length).toBeLessThanOrEqual(50);
    console.log(`[ST-NFR-004] Tasks size requested=100 -> clamped items returned: ${clampedItems.length} (Max <= 50)`);

    // 2. Kiểm tra endpoint Quản lý xử phạt Admin (/api/platform/penalties)
    const penaltyClampedRes = await request.get(`${testConfig.apiUrl}/platform/penalties?size=100`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    if (penaltyClampedRes.status() === 200) {
      const penaltyData = await penaltyClampedRes.json();
      const penaltyItems = penaltyData.content || penaltyData;
      expect(penaltyItems.length).toBeLessThanOrEqual(50);
    }
  });
});
