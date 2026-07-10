import { useCallback, useEffect, useState } from 'react';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import type {
  CatalogOption,
  ClassRequestPayload,
  LocationOption,
} from '../../marketplace/types/marketplaceTypes';

/**
 * Hook nhẹ cho form "đăng yêu cầu tìm gia sư" trên trang public /tim-gia-su.
 * Chỉ nạp catalog (các endpoint /catalog/** là public) và tạo yêu cầu.
 * KHÔNG gọi /classes/mine (cần đăng nhập) để tránh interceptor 401 đá về /login.
 */
export function useTutorRequestForm() {
  const [subjects, setSubjects] = useState<CatalogOption[]>([]);
  const [grades, setGrades] = useState<CatalogOption[]>([]);
  const [provinces, setProvinces] = useState<CatalogOption[]>([]);
  const [locations, setLocations] = useState<LocationOption[]>([]);

  useEffect(() => {
    marketplaceApi.listSubjects().then(setSubjects).catch(() => setSubjects([]));
    marketplaceApi.listGrades().then(setGrades).catch(() => setGrades([]));
    marketplaceApi.listProvinces().then(setProvinces).catch(() => setProvinces([]));
  }, []);

  const loadLocations = useCallback((provinceId: number) => {
    if (!provinceId) {
      setLocations([]);
      return;
    }
    marketplaceApi
      .listLocations(provinceId)
      .then(setLocations)
      .catch(() => setLocations([]));
  }, []);

  const createRequest = useCallback(
    (payload: ClassRequestPayload) => marketplaceApi.createClass(payload),
    [],
  );

  return { subjects, grades, provinces, locations, loadLocations, createRequest };
}
