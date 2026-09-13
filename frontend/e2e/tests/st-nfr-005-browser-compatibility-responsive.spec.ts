import { test, expect } from '@playwright/test';
import { NfrMetricsHelper } from '../utils/nfrMetricsHelper';

test.describe('ST-NFR-005: Browser compatibility and responsiveness', () => {
  test('Giao diện hiển thị chuẩn xác ở màn hình di động 375px không bị vỡ layout hay tràn ngang', async ({ page }) => {
    // 1. Giả lập màn hình hẹp chuẩn di động: 375px (iPhone SE / chuẩn mobile tối thiểu)
    await page.setViewportSize({ width: 375, height: 667 });

    // 2. Kiểm tra Trang chủ (Home)
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    const isHomeClean = await NfrMetricsHelper.checkNoHorizontalOverflow(page);
    expect(isHomeClean).toBe(true);

    // 3. Kiểm tra Trang Đăng nhập (Login)
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');
    const isLoginClean = await NfrMetricsHelper.checkNoHorizontalOverflow(page);
    expect(isLoginClean).toBe(true);

    // 4. Kiểm tra Trang Tìm kiếm lớp học (Marketplace)
    await page.goto('/tim-yeu-cau-giang-day');
    await page.waitForLoadState('domcontentloaded');
    const isMarketplaceClean = await NfrMetricsHelper.checkNoHorizontalOverflow(page);
    expect(isMarketplaceClean).toBe(true);

    console.log('[ST-NFR-005] Mobile 375px viewport: Giao diện co giãn chuẩn xác, không bị tràn màn hình ngang.');
  });

  test('Giao diện hiển thị chuẩn xác ở màn hình Desktop (1280px)', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto('/tim-yeu-cau-giang-day');
    await page.waitForLoadState('domcontentloaded');
    const isDesktopClean = await NfrMetricsHelper.checkNoHorizontalOverflow(page);
    expect(isDesktopClean).toBe(true);
  });
});
