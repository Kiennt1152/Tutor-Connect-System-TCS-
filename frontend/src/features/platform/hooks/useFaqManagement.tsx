import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { catalogApi } from '../../catalog/api/catalogApi';
import type { FaqItem, UpsertFaqRequest } from '../../catalog/types/catalogTypes';

export type ListStatus = 'loading' | 'success' | 'error';
export type MutationStatus = 'idle' | 'loading' | 'success' | 'error';

export type FaqFilters = {
  category?: string;
  keyword?: string;
};

function mapFaqItem(entry: {
  faqId: number;
  question: string;
  answer: string;
  category: string;
  sortOrder: number;
  published?: boolean;
  createdAt?: string;
  updatedAt?: string;
}): FaqItem {
  return {
    faqId: entry.faqId,
    question: entry.question,
    answer: entry.answer,
    category: entry.category,
    sortOrder: entry.sortOrder,
    published: entry.published ?? true,
    createdAt: entry.createdAt ?? null,
    updatedAt: entry.updatedAt ?? null,
  };
}

export function useFaqList(initialFilters: FaqFilters = {}) {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [items, setItems] = useState<FaqItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [filters, setFilters] = useState<FaqFilters>(initialFilters);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    catalogApi
      .getFaqEntriesForAdmin(filters.category, filters.keyword)
      .then((response) => {
        setItems(response.map(mapFaqItem));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải FAQ:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không tải được danh sách FAQ.'));
        setStatus('error');
      });
  }, [filters.category, filters.keyword]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, filters, setFilters, reload };
}

export function useFaqMutations() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  const createFaq = useCallback(async (payload: UpsertFaqRequest) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await catalogApi.createFaqEntry(payload);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể tạo câu hỏi thường gặp.'));
      setStatus('error');
      return false;
    }
  }, []);

  const updateFaq = useCallback(async (faqId: number, payload: UpsertFaqRequest) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await catalogApi.updateFaqEntry(faqId, payload);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể cập nhật câu hỏi thường gặp.'));
      setStatus('error');
      return false;
    }
  }, []);

  const deleteFaq = useCallback(async (faqId: number) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await catalogApi.deleteFaqEntry(faqId);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể xóa câu hỏi thường gặp.'));
      setStatus('error');
      return false;
    }
  }, []);

  return { status, errorMessage, reset, createFaq, updateFaq, deleteFaq };
}
