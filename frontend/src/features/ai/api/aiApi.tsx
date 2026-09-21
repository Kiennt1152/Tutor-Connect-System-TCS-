/**
 * ============================================================================
 * [UC-65] CLIENT API TRỢ LÝ TRÍ TUỆ NHÂN TẠO (AI API CLIENT)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-07-29
 * * 1. Mục đích & Chức năng:
 *    - Cung cấp các hàm gọi HTTP Axios Client tới các endpoint backend /api/ai/*.
 *    - Hỗ trợ gửi tin nhắn trò chuyện (chat), lấy danh sách phiên (getSessions), lấy lịch sử tin nhắn và xóa phiên hội thoại.
 * * 2. Luồng xử lý chính:
 *    - Gửi request JSON kèm JWT token (nếu có) tới API /api/ai/chat.
 *    - Nhận dữ liệu phản hồi đã kiểm duyệt ảo giác và tham chiếu thẻ UI trực tiếp.
 * ============================================================================
 */

import axiosClient from '../../../shared/api/axiosClient';
import type {
  AiMessage,
  AiSession,
  ChatRequestPayload,
} from '../types/aiTypes';

const BASE = '/ai';

export const aiApi = {
  async chat(payload: ChatRequestPayload): Promise<AiMessage> {
    const response = await axiosClient.post<AiMessage>(`${BASE}/chat`, payload);
    return response.data;
  },

  async getSessions(): Promise<AiSession[]> {
    const response = await axiosClient.get<AiSession[]>(`${BASE}/sessions`);
    return response.data;
  },

  async getSessionMessages(sessionId: number): Promise<AiMessage[]> {
    const response = await axiosClient.get<AiMessage[]>(`${BASE}/sessions/${sessionId}/messages`);
    return response.data;
  },

  async deleteSession(sessionId: number): Promise<void> {
    await axiosClient.delete(`${BASE}/sessions/${sessionId}`);
  },
};
