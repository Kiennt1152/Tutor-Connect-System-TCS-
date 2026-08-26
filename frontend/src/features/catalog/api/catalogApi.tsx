import axiosClient from '../../../shared/api/axiosClient';
import type {
  CategoryItem,
  ChatbotAskRequest,
  ChatbotAskResponse,
  FaqEntryApiResponse,
  SystemParameterResponse,
  UpsertCategoryRequest,
  UpsertFaqRequest,
  UpsertSystemParameterRequest,
} from '../types/catalogTypes';

export const CATALOG_API_BASE = '/catalog';

export const catalogApi = {
  http: axiosClient,
  basePath: CATALOG_API_BASE,

  // LUỒNG 1 - BƯỚC 2: Gửi HTTP GET request /api/catalog/faq kèm params (category, keyword)
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
    const response = await axiosClient.patch<FaqEntryApiResponse>(`${CATALOG_API_BASE}/faq/${faqId}`, payload);
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

  async getSystemParameters(prefix?: string, keyword?: string) {
    const params: Record<string, string> = {};
    if (prefix?.trim()) params.prefix = prefix.trim();
    if (keyword?.trim()) params.keyword = keyword.trim();
    const response = await axiosClient.get<SystemParameterResponse[]>(`${CATALOG_API_BASE}/parameters`, { params });
    return response.data;
  },
  async createSystemParameter(payload: UpsertSystemParameterRequest) {
    const response = await axiosClient.post<SystemParameterResponse>(`${CATALOG_API_BASE}/parameters`, payload);
    return response.data;
  },
  async updateSystemParameter(parameterId: number, payload: UpsertSystemParameterRequest) {
    const response = await axiosClient.patch<SystemParameterResponse>(
      `${CATALOG_API_BASE}/parameters/${parameterId}`,
      payload,
    );
    return response.data;
  },
  async deleteSystemParameter(parameterId: number) {
    await axiosClient.delete(`${CATALOG_API_BASE}/parameters/${parameterId}`);
  },
};
