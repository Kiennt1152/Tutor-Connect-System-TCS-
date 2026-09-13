import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class MarketplacePage extends BasePage {
  readonly searchInput: Locator;
  readonly classCards: Locator;
  readonly tutorCards: Locator;
  readonly filterSelects: Locator;

  constructor(page: Page) {
    super(page);
    this.searchInput = page.locator('input[type="text"], input[type="search"]').first();
    this.classCards = page.locator('.class-card, .tutor-card, .find-class-card, article');
    this.tutorCards = page.locator('.tutor-item, .tutor-card, .tutor-profile-card');
    this.filterSelects = page.locator('select');
  }

  async openFindClass(): Promise<void> {
    await this.navigate('/tim-yeu-cau-giang-day');
    await this.waitForLoad();
  }

  async openFindTutor(): Promise<void> {
    await this.navigate('/tim-gia-su');
    await this.waitForLoad();
  }

  async search(keyword: string): Promise<number> {
    await this.searchInput.waitFor({ state: 'visible', timeout: 8000 });
    const startTime = Date.now();
    await this.searchInput.fill(keyword);
    await this.page.keyboard.press('Enter');
    await this.page.waitForLoadState('networkidle');
    const duration = Date.now() - startTime;
    return duration;
  }

  async getCardsCount(): Promise<number> {
    return await this.classCards.count();
  }
}
