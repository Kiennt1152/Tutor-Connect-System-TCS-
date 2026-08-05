import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { catalogApi } from '../../catalog/api/catalogApi';
import type { SystemParameterItem, UpsertSystemParameterRequest } from '../../catalog/types/catalogTypes';

export type ListStatus = 'loading' | 'success' | 'error';
export type MutationStatus = 'idle' | 'loading' | 'success' | 'error';

export type ParameterFilters = {
  prefix?: string;
  keyword?: string;
};

function mapParameterItem(entry: {
  parameterId: number;
  paramKey: string;
  paramValue: string;
  description?: string | null;
}): SystemParameterItem {
  return {
    parameterId: entry.parameterId,
    paramKey: entry.paramKey,
    paramValue: entry.paramValue,
    description: entry.description ?? null,
  };
}

export function useSystemParameterList(initialFilters: ParameterFilters = {}) {
  const [status, setStatus] = useState<ListStatus>('loading');
  const [items, setItems] = useState<SystemParameterItem[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [filters, setFilters] = useState<ParameterFilters>(initialFilters);

  const reload = useCallback(() => {
    setStatus('loading');
    setErrorMessage(null);
    catalogApi
      .getSystemParameters(filters.prefix, filters.keyword)
      .then((response) => {
        setItems(response.map(mapParameterItem));
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải tham số hệ thống:', error);
        setErrorMessage(getApiErrorMessage(error, 'Không tải được danh sách tham số.'));
        setStatus('error');
      });
  }, [filters.prefix, filters.keyword]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, items, errorMessage, filters, setFilters, reload };
}

export function useSystemParameterMutations() {
  const [status, setStatus] = useState<MutationStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reset = useCallback(() => {
    setStatus('idle');
    setErrorMessage(null);
  }, []);

  const createParameter = useCallback(async (payload: UpsertSystemParameterRequest) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await catalogApi.createSystemParameter(payload);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể tạo tham số hệ thống.'));
      setStatus('error');
      return false;
    }
  }, []);

  const updateParameter = useCallback(async (parameterId: number, payload: UpsertSystemParameterRequest) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await catalogApi.updateSystemParameter(parameterId, payload);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể cập nhật tham số hệ thống.'));
      setStatus('error');
      return false;
    }
  }, []);

  const deleteParameter = useCallback(async (parameterId: number) => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      await catalogApi.deleteSystemParameter(parameterId);
      setStatus('success');
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể xóa tham số hệ thống.'));
      setStatus('error');
      return false;
    }
  }, []);

  return { status, errorMessage, reset, createParameter, updateParameter, deleteParameter };
}
