import { useCallback, useEffect, useState } from 'react';
import { marketplaceApi } from '../api/marketplaceApi';
import type {
  CatalogOption,
  ClassRequestPayload,
  ClassResponse,
  LocationOption,
} from '../types/marketplaceTypes';

export type LoadStatus = 'loading' | 'success' | 'error';

export function useMarketplace() {
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [subjects, setSubjects] = useState<CatalogOption[]>([]);
  const [grades, setGrades] = useState<CatalogOption[]>([]);
  const [provinces, setProvinces] = useState<CatalogOption[]>([]);
  const [locations, setLocations] = useState<LocationOption[]>([]);

  /** Tải lại danh sách lớp. */
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

  useEffect(() => {
    marketplaceApi.listSubjects().then(setSubjects).catch(() => setSubjects([]));
    marketplaceApi.listGrades().then(setGrades).catch(() => setGrades([]));
    marketplaceApi.listProvinces().then(setProvinces).catch(() => setProvinces([]));
  }, []);

  /** Tải danh sách địa điểm theo tỉnh (không chọn tỉnh thì xoá danh sách). */
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

  /** Tạo tin mới rồi tải lại danh sách. */
  const createClass = useCallback(
    async (payload: ClassRequestPayload) => {
      const created = await marketplaceApi.createClass(payload);
      reload();
      return created;
    },
    [reload],
  );

  /** Sửa tin rồi tải lại danh sách. */
  const updateClass = useCallback(
    async (classId: number, payload: ClassRequestPayload) => {
      const updated = await marketplaceApi.updateClass(classId, payload);
      reload();
      return updated;
    },
    [reload],
  );

  /** Đăng tin rồi tải lại danh sách. */
  const publishClass = useCallback(
    async (classId: number) => {
      const published = await marketplaceApi.publishClass(classId);
      reload();
      return published;
    },
    [reload],
  );

  /** Gỡ đăng tin rồi tải lại danh sách. */
  const unpublishClass = useCallback(
    async (classId: number) => {
      const updated = await marketplaceApi.unpublishClass(classId);
      reload();
      return updated;
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
    unpublishClass,
  };
}
