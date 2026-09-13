import { type Page, type Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class PlatformReportsPage extends BasePage {
  readonly disputesSection: Locator;
  readonly reportsSection: Locator;

  constructor(page: Page) {
    super(page);
    this.disputesSection = page.locator('#section-disputes, .pd-console');
    this.reportsSection = page.locator('#section-reports');
  }

  async open() {
    await this.goto('/platform/reports');
    await expect(this.page.locator('h1, .adm-layout__title')).toContainText(/Báo cáo/i);
  }

  async verifyNavigationFromSource(reportId: number | string) {
    // URL phải chứa query parameter id=reportId
    await expect(this.page).toHaveURL(new RegExp(`id=${reportId}`));
    // Màn hình báo cáo & tranh chấp hiển thị
    await expect(this.page.locator('h1, .adm-layout__title')).toContainText(/Báo cáo & tranh chấp/i);
  }
}
