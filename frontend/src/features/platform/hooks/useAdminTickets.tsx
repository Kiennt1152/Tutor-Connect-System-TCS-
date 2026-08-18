import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import { mapAdminTicketDetail, mapPageAdminTicketList } from '../mappers/platformMapper';
import type {
  AdminTicketDetail,
  AdminTicketFilters,
  CloseTicketApiRequest,
  PageAdminTicketList,
  RespondTicketApiRequest,
  UpdateTicketApiRequest,
} from '../types/platformTypes';

export type ListStatus = 'loading' | 'success' | 'error';
export type MutationStatus = 'idle' | 'loading' | 'success' | 'error';

export function useAdminTicketList(initialFilters: AdminTicketFilters) {
  const [filters, setFilters] = useState<AdminTicketFilters>(initialFilters);
  const [status, setStatus] = useState<ListStatus>('loading');
  const [data, setData] = useState<PageAdminTicketList | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    platformApi
      .getTickets(filters)
      .then((res) => {
        setData(mapPageAdminTicketList(res.data));
        setStatus('success');
      })
      .catch((err: unknown) => {
        setErrorMessage(getApiErrorMessage(err, 'Khong tai duoc danh sach ticket.'));
        setStatus('error');
      });
  }, [filters]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, data, filters, setFilters, errorMessage, reload };
}

export function useAdminTicketDetail(ticketId: string | null) {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [detail, setDetail] = useState<AdminTicketDetail | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    if (!ticketId) return;
    setStatus('loading');
    setErrorMessage(null);
    platformApi
      .getTicketDetail(ticketId)
      .then((res) => {
        setDetail(mapAdminTicketDetail(res.data));
        setStatus('success');
      })
      .catch((err: unknown) => {
        setErrorMessage(getApiErrorMessage(err, 'Khong tai duoc chi tiet ticket.'));
        setStatus('error');
      });
  }, [ticketId]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, detail, errorMessage, reload };
}

export function useTicketMutations(onSuccess: () => void) {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const run = useCallback(
    async (fn: () => Promise<unknown>) => {
      setStatus('loading');
      setErrorMessage(null);
      try {
        await fn();
        setStatus('success');
        onSuccess();
      } catch (err: unknown) {
        setErrorMessage(getApiErrorMessage(err, 'Thao tac that bai.'));
        setStatus('error');
      }
    },
    [onSuccess],
  );

  const respond = useCallback(
    (ticketId: string, payload: RespondTicketApiRequest) =>
      run(() => platformApi.respondToTicket(ticketId, payload)),
    [run],
  );

  const updateTicket = useCallback(
    (ticketId: string, payload: UpdateTicketApiRequest) =>
      run(() => platformApi.updateTicket(ticketId, payload)),
    [run],
  );

  const closeTicket = useCallback(
    (ticketId: string, payload: CloseTicketApiRequest) =>
      run(() => platformApi.closeTicket(ticketId, payload)),
    [run],
  );

  const mergeTicket = useCallback(
    (ticketId: string, payload: { targetTicketId: number; reason?: string }) =>
      run(() => platformApi.mergeTicket(ticketId, payload)),
    [run],
  );

  const redirectTicketToDispute = useCallback(
    (ticketId: string, payload: { targetClassId?: number; notes?: string }) =>
      run(() => platformApi.redirectTicketToDispute(ticketId, payload)),
    [run],
  );

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  return { status, errorMessage, respond, updateTicket, closeTicket, mergeTicket, redirectTicketToDispute, reset };
}
