import { useCallback, useEffect, useState } from 'react';
import { catalogApi } from '../../catalog/api/catalogApi';
import type { ChatbotAskResponse, FaqEntryApiResponse } from '../../catalog/types/catalogTypes';
import { getApiErrorMessage } from '../../../shared/api/apiError';

export type HelpStatus = 'idle' | 'loading' | 'success' | 'error';

// LUỒNG 1 - BƯỚC 2: Hook quản lý tải & tìm kiếm dữ liệu FAQ theo Category và Keyword
export function useFaqSearch(initialKeyword = '') {
  const [keyword, setKeyword] = useState(initialKeyword);
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState<HelpStatus>('loading');
  const [items, setItems] = useState<FaqEntryApiResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Hàm gọi API lấy danh sách FAQ dựa trên category và keyword
  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    catalogApi
      .getFaqEntries(category || undefined, keyword || undefined)
      .then((data) => {
        // Cập nhật danh sách câu hỏi nhận được từ server
        setItems(data);
        setStatus('success');
      })
      .catch((err: unknown) => {
        setErrorMessage(getApiErrorMessage(err, 'Không tải được FAQ.'));
        setStatus('error');
      });
  }, [keyword, category]);

  // Tự động gọi reload mỗi khi keyword hoặc category thay đổi
  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, keyword, setKeyword, category, setCategory, errorMessage, reload };
}

export type ChatbotStatus = 'idle' | 'loading' | 'success' | 'error';

export function useChatbot() {
  const [input, setInput] = useState('');
  const [status, setChatStatus] = useState<ChatbotStatus>('idle');
  const [result, setResult] = useState<ChatbotAskResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const ask = useCallback((question: string) => {
    if (!question.trim()) return;
    setChatStatus('loading');
    setResult(null);
    setErrorMessage(null);
    catalogApi
      .askChatbot({ question })
      .then((data) => {
        setResult(data);
        setChatStatus('success');
      })
      .catch((err: unknown) => {
        setErrorMessage(getApiErrorMessage(err, 'Không thể xử lý câu hỏi.'));
        setChatStatus('error');
      });
  }, []);

  const reset = useCallback(() => {
    setInput('');
    setChatStatus('idle');
    setResult(null);
    setErrorMessage(null);
  }, []);

  return { input, setInput, status, result, errorMessage, ask, reset };
}
