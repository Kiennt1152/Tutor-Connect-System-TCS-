import axiosClient from '../../../shared/api/axiosClient';
import type {
  CreateSupportTicketApiRequest,
  SupportTicketApiResponse,
  SupportTicketDetailApiResponse,
} from '../types/messagingTypes';

export const MESSAGING_API_BASE = '/messaging';

export const messagingApi = {
  http: axiosClient,
  basePath: MESSAGING_API_BASE,



  async getMySupportTickets() {
    const response = await axiosClient.get<SupportTicketApiResponse[]>(
      `${MESSAGING_API_BASE}/support-tickets`,
    );
    return response.data;
  },

  async getMySupportTicketDetail(ticketId: string) {
    const response = await axiosClient.get<SupportTicketDetailApiResponse>(
      `${MESSAGING_API_BASE}/support-tickets/${ticketId}`,
    );
    return response.data;
  },

  async createSupportTicket(payload: CreateSupportTicketApiRequest) {
    const response = await axiosClient.post<SupportTicketApiResponse>(
      `${MESSAGING_API_BASE}/support-tickets`,
      payload,
    );
    return response.data;
  },

  async replySupportTicket(ticketId: string, content: string) {
    const response = await axiosClient.post(
      `${MESSAGING_API_BASE}/support-tickets/${ticketId}/messages`,
      { content },
    );
    return response.data;
  },

  async reopenSupportTicket(ticketId: string, content: string) {
    const response = await axiosClient.post(
      `${MESSAGING_API_BASE}/support-tickets/${ticketId}/reopen`,
      { content },
    );
    return response.data;
  },

  /* ── Direct Chat / Context Conversations ── */

  async getConversations() {
    const response = await axiosClient.get<import('../types/messagingTypes').ConversationResponse[]>(
      `${MESSAGING_API_BASE}/conversations`,
    );
    return response.data;
  },

  async startOrGetConversation(targetUserId: number) {
    const response = await axiosClient.post<import('../types/messagingTypes').ConversationResponse>(
      `${MESSAGING_API_BASE}/conversations`,
      { targetUserId },
    );
    return response.data;
  },

  async getOrCreateContextConversation(contextType: string, contextId: string | number) {
    const response = await axiosClient.get<import('../types/messagingTypes').ConversationResponse>(
      `${MESSAGING_API_BASE}/context/${contextType}/${contextId}`,
    );
    return response.data;
  },

  async getMessages(conversationId: number, page = 0, size = 30) {
    const response = await axiosClient.get<import('../types/messagingTypes').PageResponse<import('../types/messagingTypes').MessageResponse>>(
      `${MESSAGING_API_BASE}/conversations/${conversationId}/messages`,
      { params: { page, size } },
    );
    return response.data;
  },

  async sendMessage(conversationId: number, content: string) {
    const response = await axiosClient.post<import('../types/messagingTypes').MessageResponse>(
      `${MESSAGING_API_BASE}/conversations/${conversationId}/messages`,
      { content },
    );
    return response.data;
  },

  async markAsRead(conversationId: number) {
    const response = await axiosClient.post(
      `${MESSAGING_API_BASE}/conversations/${conversationId}/read`,
    );
    return response.data;
  },

  async listUsers(keyword?: string) {
    const response = await axiosClient.get<import('../types/messagingTypes').UserSummaryResponse[]>(
      `${MESSAGING_API_BASE}/users`,
      { params: { keyword } },
    );
    return response.data;
  },
};

