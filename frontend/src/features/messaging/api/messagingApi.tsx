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
};
