import { useEffect, useState } from 'react';
import { catalogApi } from '../api/catalogApi';
import { mapCategoryTree } from '../mappers/catalogMapper';
import type { CategoryItem, UpsertCategoryRequest } from '../types/catalogTypes';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export function useCatalog(rootName: string | null) {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function loadCategories() {
    setStatus('loading');
    setError(null);

    try {
      const response = await catalogApi.listCategories(rootName);
      setCategories(mapCategoryTree(response));
      setStatus('success');
    } catch (loadError) {
      setStatus('error');
      setError(extractErrorMessage(loadError));
    }
  }

  useEffect(() => {
    void loadCategories();
  }, [rootName]);

  async function createCategory(payload: UpsertCategoryRequest) {
    await catalogApi.createCategory(payload);
    await loadCategories();
  }

  async function updateCategory(categoryId: number, payload: UpsertCategoryRequest) {
    await catalogApi.updateCategory(categoryId, payload);
    await loadCategories();
  }

  async function deleteCategory(categoryId: number) {
    await catalogApi.deleteCategory(categoryId);
    await loadCategories();
  }

  return {
    status,
    categories,
    error,
    reload: loadCategories,
    createCategory,
    updateCategory,
    deleteCategory,
  };
}

function extractErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null) {
    const maybeAxiosError = error as {
      response?: { data?: { message?: string } | Record<string, string> };
      message?: string;
    };

    const message = maybeAxiosError.response?.data;
    if (message && typeof message === 'object') {
      if ('message' in message && typeof message.message === 'string') {
        return message.message;
      }
      return Object.values(message).join(', ');
    }

    if (typeof maybeAxiosError.message === 'string') {
      return maybeAxiosError.message;
    }
  }

  return 'Không tải được dữ liệu danh mục.';
}
