import axiosClient from '../../../shared/api/axiosClient';
import type { CategoryItem, UpsertCategoryRequest } from '../types/catalogTypes';

export const CATALOG_API_BASE = '/catalog';

export const catalogApi = {
  http: axiosClient,
  basePath: CATALOG_API_BASE,
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
