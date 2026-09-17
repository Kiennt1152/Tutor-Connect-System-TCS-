import { useCallback, useEffect, useRef, useState } from 'react';
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

  const selectedRef = useRef<AdminDisputeReviewApiResponse | null>(null);
  selectedRef.current = selected;

  const selectDispute = useCallback((item: DisputeReviewItem | null, forceReload = false) => {
    if (!item) {
      setSelected(null);
      setSelectedStatus('success');
      setDetailErrorMessage(null);
      return;
    }

    if (!forceReload && selectedRef.current?.disputeId === item.raw?.disputeId && selectedRef.current?.resolution != null) {
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
        const currentId = selectedRef.current?.disputeId;
        const current = currentId
          ? nextItems.find((item) => item.raw.disputeId === currentId)
          : null;
        if (!selectedRef.current) {
          selectDispute(nextItems[0]);
        } else if (!current) {
          selectDispute(nextItems[0]);
        }
      })
      .catch((error) => {
        console.error('Lỗi tải danh sách tranh chấp:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không thể tải danh sách tranh chấp.'));
        setStatus('error');
      });
  }, [selectDispute, statusFilter]);

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
