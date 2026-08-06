import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import { mapDisputeReviewItem } from '../mappers/platformMapper';
import type {
  AdminDisputeReviewApiResponse,
  DisputeReviewItem,
  DisputeStatus,
} from '../types/platformTypes';

export type DisputeReviewListStatus = 'loading' | 'success' | 'error';

export function useDisputeReviewList(statusFilter?: DisputeStatus) {
  const [status, setStatus] = useState<DisputeReviewListStatus>('loading');
  const [items, setItems] = useState<DisputeReviewItem[]>([]);
  const [selected, setSelected] = useState<AdminDisputeReviewApiResponse | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<DisputeReviewListStatus>('loading');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [detailErrorMessage, setDetailErrorMessage] = useState<string | null>(null);

  const selectDispute = useCallback((item: DisputeReviewItem | null) => {
    if (!item) {
      setSelected(null);
      setSelectedStatus('success');
      setDetailErrorMessage(null);
      return;
    }

    setSelected(item.raw);
    setSelectedStatus('loading');
    setDetailErrorMessage(null);
    platformApi
      .getDispute(item.id)
      .then((response) => {
        setSelected(response.data);
        setSelectedStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải chi tiết tranh chấp:', error);
        setDetailErrorMessage(getApiErrorMessage(error, 'Không thể tải chi tiết tranh chấp.'));
        setSelectedStatus('error');
      });
  }, []);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    platformApi
      .getDisputes(statusFilter)
      .then((response) => {
        const nextItems = response.data.map(mapDisputeReviewItem);
        setItems(nextItems);
        setStatus('success');
        if (nextItems.length === 0) {
          setSelected(null);
          setSelectedStatus('success');
          return;
        }
        const current = selected?.disputeId
          ? nextItems.find((item) => item.raw.disputeId === selected.disputeId)
          : nextItems[0];
        selectDispute(current ?? nextItems[0]);
      })
      .catch((error) => {
        console.error('Lỗi tải danh sách tranh chấp:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không thể tải danh sách tranh chấp.'));
        setStatus('error');
      });
  }, [selectDispute, selected?.disputeId, statusFilter]);

  useEffect(() => {
    reload();
  }, [reload]);

  return {
    status,
    items,
    selected,
    selectedStatus,
    errorMessage,
    detailErrorMessage,
    selectDispute,
    reload,
  };
}
