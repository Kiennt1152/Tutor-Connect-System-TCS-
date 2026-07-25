import axiosClient from '../../../shared/api/axiosClient';
import type {
  CategoryItem,
  ChatbotAskRequest,
  ChatbotAskResponse,
  FaqEntryApiResponse,
  UpsertCategoryRequest,
  UpsertFaqRequest,
} from '../types/catalogTypes';

export const CATALOG_API_BASE = '/catalog';

export const catalogApi = {
  http: axiosClient,
  basePath: CATALOG_API_BASE,

  async getFaqEntries(category?: string, keyword?: string) {
    const params: Record<string, string> = {};
    if (category) params.category = category;
    if (keyword?.trim()) params.keyword = keyword.trim();
    const response = await axiosClient.get<FaqEntryApiResponse[]>(`${CATALOG_API_BASE}/faq`, { params });
    return response.data;
  },

  async askChatbot(payload: ChatbotAskRequest) {
    const response = await axiosClient.post<ChatbotAskResponse>(`${CATALOG_API_BASE}/chatbot/ask`, payload);
    return response.data;
  },

  async getFaqEntriesForAdmin(category?: string, keyword?: string) {
    const params: Record<string, string> = {};
    if (category) params.category = category;
    if (keyword?.trim()) params.keyword = keyword.trim();
    const response = await axiosClient.get<FaqEntryApiResponse[]>(`${CATALOG_API_BASE}/faq/admin`, { params });
    return response.data;
  },

  async createFaqEntry(payload: UpsertFaqRequest) {
    const response = await axiosClient.post<FaqEntryApiResponse>(`${CATALOG_API_BASE}/faq`, payload);
    return response.data;
  },

  async updateFaqEntry(faqId: number, payload: UpsertFaqRequest) {
    const response = await axiosClient.put<FaqEntryApiResponse>(`${CATALOG_API_BASE}/faq/${faqId}`, payload);
    return response.data;
  },

  async deleteFaqEntry(faqId: number) {
    await axiosClient.delete(`${CATALOG_API_BASE}/faq/${faqId}`);
  },

  async listCategories(root?: string | null) {
    const response = await axiosClient.get<CategoryItem[]>(`${CATALOG_API_BASE}/categories`, {
      params: root ? { root } : undefined,
    });
    return response.data;
  },
  async getCategory(categoryId: number) {
    const response = await axiosClient.get<CategoryItem>(`${CATALOG_API_BASE}/categories/${categoryId}`);
    return response.data;
  },
  async createCategory(payload: UpsertCategoryRequest) {
    const response = await axiosClient.post<CategoryItem>(`${CATALOG_API_BASE}/categories`, payload);
    return response.data;
  },
  async updateCategory(categoryId: number, payload: UpsertCategoryRequest) {
    const response = await axiosClient.put<CategoryItem>(`${CATALOG_API_BASE}/categories/${categoryId}`, payload);
    return response.data;
  },
  async deleteCategory(categoryId: number) {
    await axiosClient.delete(`${CATALOG_API_BASE}/categories/${categoryId}`);
  },
};
