import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import type { AdminReviewApiResponse, ReviewModerationStatus } from '../types/platformTypes';

export type ListStatus = 'loading' | 'success' | 'error';

export function useReviewModeration() {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [items, setItems] = useState<AdminReviewApiResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    platformApi
      .getReviews()
      .then((response) => {
        setItems(response.data);
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải danh sách đánh giá:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không thể tải danh sách đánh giá.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const moderate = useCallback((reviewId: number, next: ReviewModerationStatus) => {
    return platformApi.moderateReview(reviewId, next).then((response) => {
      setItems((prev) => prev.map((r) => (r.reviewId === reviewId ? response.data : r)));
    });
  }, []);

  const remove = useCallback((reviewId: number) => {
    return platformApi.deleteReview(reviewId).then(() => {
      setItems((prev) => prev.filter((r) => r.reviewId !== reviewId));
    });
  }, []);

  return { status, items, errorMessage, reload, moderate, remove };
}
