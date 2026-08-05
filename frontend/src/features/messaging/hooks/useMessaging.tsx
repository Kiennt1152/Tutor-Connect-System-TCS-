import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { messagingApi } from '../api/messagingApi';
import { mapTicketDetail, mapTicketItem } from '../mappers/messagingMapper';
import type {
  CreateSupportTicketApiRequest,
  SupportTicketDetail,
  SupportTicketItem,
} from '../types/messagingTypes';

export type ListStatus = 'loading' | 'success' | 'error';
export type MutationStatus = 'idle' | 'loading' | 'success' | 'error';

export function useTicketList() {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [items, setItems] = useState<SupportTicketItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    messagingApi
      .getMySupportTickets()
      .then((data) => {
        setItems(data.map(mapTicketItem));
        setStatus('success');
      })
      .catch((err: unknown) => {
        setErrorMessage(getApiErrorMessage(err, 'Không tải được danh sách yêu cầu hỗ trợ.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, reload };
}

export function useTicketDetail(ticketId: string | null) {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [detail, setDetail] = useState<SupportTicketDetail | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    if (!ticketId) return;
    setStatus('loading');
    setErrorMessage(null);
    messagingApi
      .getMySupportTicketDetail(ticketId)
      .then((data) => {
        setDetail(mapTicketDetail(data));
        setStatus('success');
      })
      .catch((err: unknown) => {
        setErrorMessage(getApiErrorMessage(err, 'Không tải được chi tiết yêu cầu.'));
        setStatus('error');
      });
  }, [ticketId]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, detail, errorMessage, reload };
}

export function useCreateTicket(onSuccess: () => void) {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const submit = useCallback(
    async (payload: CreateSupportTicketApiRequest) => {
      setStatus('loading');
      setErrorMessage(null);
      try {
        await messagingApi.createSupportTicket(payload);
        setStatus('success');
        onSuccess();
      } catch (err: unknown) {
        setErrorMessage(getApiErrorMessage(err, 'Không thể gửi yêu cầu hỗ trợ.'));
        setStatus('error');
      }
    },
    [onSuccess],
  );

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  return { status, errorMessage, submit, reset };
}

export function useTicketMutations(onSuccess: () => void) {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reply = useCallback(
    async (ticketId: string, content: string) => {
      setStatus('loading');
      setErrorMessage(null);
      try {
        await messagingApi.replySupportTicket(ticketId, content);
        setStatus('success');
        onSuccess();
      } catch (err: unknown) {
        setErrorMessage(getApiErrorMessage(err, 'Không thể gửi phản hồi.'));
        setStatus('error');
      }
    },
    [onSuccess],
  );

  const reopen = useCallback(
    async (ticketId: string, content: string) => {
      setStatus('loading');
      setErrorMessage(null);
      try {
        await messagingApi.reopenSupportTicket(ticketId, content);
        setStatus('success');
        onSuccess();
      } catch (err: unknown) {
        setErrorMessage(getApiErrorMessage(err, 'Không thể mở lại yêu cầu.'));
        setStatus('error');
      }
    },
    [onSuccess],
  );

  return { status, errorMessage, reply, reopen };
}
