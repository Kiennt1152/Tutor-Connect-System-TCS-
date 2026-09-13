import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class PlatformDashboardPage extends BasePage {
  readonly reindexButton: Locator;
  readonly reindexMessageAlert: Locator;
  readonly slaOverdueBadge: Locator;

  constructor(page: Page) {
    super(page);
    this.reindexButton = page.locator('button', { hasText: /Đánh chỉ mục lại \(Reindex All\)|Đang reindex.../i });
    this.reindexMessageAlert = page.locator('.adm-card-stat', { hasText: /Đã đánh chỉ mục|Vui lòng đợi/i });
    this.slaOverdueBadge = page.locator('.adm-sla-tag', { hasText: 'QUÁ HẠN' });
  }

  async open(): Promise<void> {
    await this.navigate('/platform');
    await this.waitForLoad();
  }

  async clickReindex(): Promise<void> {
    await this.reindexButton.waitFor({ state: 'visible', timeout: 8000 });
    await this.reindexButton.click();
  }

  async waitForReindexResult(timeout = 15000): Promise<string> {
    await expect(this.page.locator('text=/Đã đánh chỉ mục|Vui lòng đợi/i').first()).toBeVisible({ timeout });
    const text = await this.page.locator('text=/Đã đánh chỉ mục|Vui lòng đợi/i').first().textContent();
    return text || '';
  }

  async verifySlaBadgeVisible(): Promise<void> {
    await expect(this.slaOverdueBadge.first()).toBeVisible({ timeout: 10000 });
  }
}
