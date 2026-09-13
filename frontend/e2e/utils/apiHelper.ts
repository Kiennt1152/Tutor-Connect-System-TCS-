import { testConfig } from './testConfig';

export class ApiHelper {
  private static clientToken: string | null = null;
  private static adminToken: string | null = null;

  static async getClientToken(): Promise<string> {
    if (this.clientToken) return this.clientToken;
    const res = await fetch(`${testConfig.apiUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: testConfig.client.email,
        password: testConfig.client.password,
      }),
    });
    const data = await res.json();
    this.clientToken = data.accessToken || data.token;
    return this.clientToken!;
  }

  static async getAdminToken(): Promise<string> {
    if (this.adminToken) return this.adminToken;
    const res = await fetch(`${testConfig.apiUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: testConfig.admin.email,
        password: testConfig.admin.password,
      }),
    });
    const data = await res.json();
    this.adminToken = data.accessToken || data.token;
    return this.adminToken!;
  }

  /**
   * Tạo nhanh một Báo cáo vi phạm qua REST API.
   */
  static async createReport(params: {
    targetId: number;
    targetType?: 'USER' | 'CLASS' | 'REVIEW';
    category?: 'FRAUD' | 'ABUSE' | 'INAPPROPRIATE' | 'OTHER';
    description?: string;
  }): Promise<number> {
    const token = await this.getClientToken();
    const res = await fetch(`${testConfig.apiUrl}/messaging/reports`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        targetType: params.targetType || 'USER',
        targetId: params.targetId,
        category: params.category || 'FRAUD',
        description: params.description || 'Gia sư có hành vi gian lận và thu tiền ngoài luồng',
      }),
    });
    const data = await res.json();
    return data.reportId || data.id;
  }

  /**
   * Thu hồi án phạt qua REST API để dọn dẹp môi trường.
   */
  static async revokePenalty(penaltyId: number, reason = 'Cleanup automated test'): Promise<void> {
    const token = await this.getAdminToken();
    await fetch(`${testConfig.apiUrl}/platform/penalties/${penaltyId}/revoke`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ reason }),
    });
  }

  static async getTutorToken(): Promise<string> {
    const res = await fetch(`${testConfig.apiUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: testConfig.tutor.email,
        password: testConfig.tutor.password,
      }),
    });
    const data = await res.json();
    return data.accessToken || data.token;
  }

  static async getUnrelatedTutorToken(): Promise<string> {
    const res = await fetch(`${testConfig.apiUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: testConfig.unrelatedTutor.email,
        password: testConfig.unrelatedTutor.password,
      }),
    });
    const data = await res.json();
    return data.accessToken || data.token;
  }

  /**
   * Tạo phiếu hỗ trợ kỹ thuật qua API.
   */
  static async createSupportTicket(params: {
    category: string;
    subject: string;
    description: string;
    priority?: string;
  }): Promise<any> {
    const token = await this.getClientToken();
    const res = await fetch(`${testConfig.apiUrl}/messaging/support-tickets`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        category: params.category,
        subject: params.subject,
        description: params.description,
        priority: params.priority || 'HIGH',
      }),
    });
    return res.json();
  }

  /**
   * Khởi tạo hoặc lấy cuộc trò chuyện giữa 2 người dùng.
   */
  static async startOrGetConversation(token: string, targetUserId: number): Promise<any> {
    const res = await fetch(`${testConfig.apiUrl}/messaging/conversations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ targetUserId }),
    });
    return res.json();
  }

  /**
   * Xóa phiên hội thoại AI để dọn dẹp môi trường.
   */
  static async deleteAiSession(sessionId: number, token?: string): Promise<void> {
    const auth = token || (await this.getClientToken());
    await fetch(`${testConfig.apiUrl}/ai/sessions/${sessionId}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${auth}`,
      },
    });
  }
}
