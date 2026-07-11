import { useCallback, useEffect, useState } from 'react';
import { marketplaceApi } from '../api/marketplaceApi';
import type {
  CatalogOption,
  ClassRequestPayload,
  ClassResponse,
  LocationOption,
} from '../types/marketplaceTypes';

export type LoadStatus = 'loading' | 'success' | 'error';

/** Quản lý danh sách lớp của Client + catalog cho form + các thao tác tạo/sửa/đăng. */
export function useMarketplace() {
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [subjects, setSubjects] = useState<CatalogOption[]>([]);
  const [grades, setGrades] = useState<CatalogOption[]>([]);
  const [provinces, setProvinces] = useState<CatalogOption[]>([]);
  const [locations, setLocations] = useState<LocationOption[]>([]);

  const reload = useCallback(() => {
    setStatus('loading');
    marketplaceApi
      .listMyClasses()
      .then((data) => {
        setClasses(data);
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải danh sách lớp:', error);
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  // Catalog cho form (tải một lần).
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

  const createClass = useCallback(
    async (payload: ClassRequestPayload) => {
      const created = await marketplaceApi.createClass(payload);
      reload();
      return created;
    },
    [reload],
  );

  const updateClass = useCallback(
    async (classId: number, payload: ClassRequestPayload) => {
      const updated = await marketplaceApi.updateClass(classId, payload);
      reload();
      return updated;
    },
    [reload],
  );

  const publishClass = useCallback(
    async (classId: number) => {
      const published = await marketplaceApi.publishClass(classId);
      reload();
      return published;
    },
    [reload],
  );

  return {
    status,
    classes,
    subjects,
    grades,
    provinces,
    locations,
    reload,
    loadLocations,
    createClass,
    updateClass,
    publishClass,
  };
}
