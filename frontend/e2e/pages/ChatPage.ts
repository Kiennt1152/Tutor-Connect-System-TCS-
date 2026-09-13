import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class ChatPage extends BasePage {
  readonly messageInput: Locator;
  readonly sendButton: Locator;
  readonly messageBubbles: Locator;
  readonly emptyThreadState: Locator;
  readonly conversationItems: Locator;

  constructor(page: Page) {
    super(page);
    this.messageInput = page.locator('.msg-input-bar__textarea');
    this.sendButton = page.locator('.msg-input-bar__send');
    this.messageBubbles = page.locator('.msg-bubble__content');
    this.emptyThreadState = page.locator('.msg-thread-panel__empty');
    this.conversationItems = page.locator('.msg-conv-item');
  }

  async open(conversationId?: number): Promise<void> {
    const url = conversationId ? `/messages?conv=${conversationId}` : '/messages';
    await this.navigate(url);
    await this.waitForLoad();
  }

  async sendMessage(content: string): Promise<void> {
    await this.messageInput.waitFor({ state: 'visible', timeout: 8000 });
    await this.messageInput.fill(content);
    await this.sendButton.click();
  }

  async verifyMessageReceived(content: string, timeout = 10000): Promise<void> {
    const targetBubble = this.page.locator('.msg-bubble__content', { hasText: content }).last();
    await expect(targetBubble).toBeVisible({ timeout });
  }

  async verifyForbiddenAccess(): Promise<void> {
    // When attempting to access an unauthorized conversation, it either returns 403 or stays empty without loading messages
    await expect(this.messageBubbles).toHaveCount(0);
  }
}
