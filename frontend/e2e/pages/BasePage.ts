import { type Page, type Locator } from '@playwright/test';

export class BasePage {
  constructor(protected readonly page: Page) {}

  async goto(path: string) {
    await this.page.goto(path);
    await this.page.waitForLoadState('domcontentloaded');
  }

  async waitForUrl(pattern: string | RegExp) {
    await this.page.waitForURL(pattern);
  }

  getAlertError(): Locator {
    return this.page.locator('.adm-alert--error, .contract-alert--error, .reg-alert--error');
  }

  getSuccessToast(): Locator {
    return this.page.locator('.adm-alert--success, .toast-success');
  }
}
