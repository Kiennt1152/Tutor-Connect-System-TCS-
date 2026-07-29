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
