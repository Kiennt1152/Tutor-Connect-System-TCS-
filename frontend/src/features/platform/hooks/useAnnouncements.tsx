import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import type {
  AnnouncementApiResponse,
  AnnouncementItem,
  UpsertAnnouncementApiRequest,
} from '../types/platformTypes';

export type ListStatus = 'loading' | 'success' | 'error';
export type MutationStatus = 'idle' | 'loading' | 'success' | 'error';

function mapAnnouncement(entry: AnnouncementApiResponse): AnnouncementItem {
  return {
    announcementId: entry.announcementId,
    title: entry.title,
    content: entry.content,
    targetRole: entry.targetRole ?? null,
    active: entry.active,
    startsAt: entry.startsAt ?? null,
    endsAt: entry.endsAt ?? null,
    createdByName: entry.createdByName ?? null,
    createdAt: entry.createdAt ?? null,
    updatedAt: entry.updatedAt ?? null,
  };
}

export function usePublicAnnouncements() {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [items, setItems] = useState<AnnouncementItem[]>([]);

  const reload = useCallback(() => {
    setStatus('loading');
    platformApi
      .getPublicAnnouncements()
      .then((response) => {
        setItems(response.data.map(mapAnnouncement));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Loi tai thong bao cong khai:', error);
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, reload };
}

export function useAnnouncementList() {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [items, setItems] = useState<AnnouncementItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    platformApi
      .getAnnouncements()
      .then((response) => {
        setItems(response.data.map(mapAnnouncement));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Loi tai thong bao:', error);
        setErrorMessage(getApiErrorMessage(error, 'Khong tai duoc danh sach thong bao.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, reload };
}

export function useAnnouncementMutations() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  const createAnnouncement = useCallback(async (payload: UpsertAnnouncementApiRequest) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await platformApi.createAnnouncement(payload);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Khong the tao thong bao.'));
      setStatus('error');
      return false;
    }
  }, []);

  const updateAnnouncement = useCallback(async (announcementId: number, payload: UpsertAnnouncementApiRequest) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await platformApi.updateAnnouncement(announcementId, payload);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Khong the cap nhat thong bao.'));
      setStatus('error');
      return false;
    }
  }, []);

  const deleteAnnouncement = useCallback(async (announcementId: number) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await platformApi.deleteAnnouncement(announcementId);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Khong the xoa thong bao.'));
      setStatus('error');
      return false;
    }
  }, []);

  return { status, errorMessage, reset, createAnnouncement, updateAnnouncement, deleteAnnouncement };
}