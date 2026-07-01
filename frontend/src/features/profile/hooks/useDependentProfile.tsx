import axios from 'axios';
import { useCallback, useEffect, useState } from 'react';
import { profileApi } from '../api/profileApi';
import type {
  CatalogItem,
  ChildProfile,
  ChildProfileRequest,
  DependentLinkStatus,
  GuardianProfile,
  ProfileResponse,
  UpdateProfileRequest,
} from '../types/profileTypes';

export type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

export function extractApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    if (error.code === 'ERR_NETWORK') {
      return 'Không kết nối được máy chủ. Hãy kiểm tra backend đang chạy tại cổng 8080.';
    }
    if (error.response?.status === 401) {
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
    }
    if (error.response?.status === 403) {
      return typeof error.response.data?.message === 'string'
        ? error.response.data.message
        : 'Bạn không có quyền truy cập tính năng này.';
    }
    if (typeof error.response?.data?.message === 'string') {
      return error.response.data.message;
    }
  }
  return fallback;
}

export function useDependentProfile() {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [linkStatus, setLinkStatus] = useState<DependentLinkStatus | null>(null);
  const [children, setChildren] = useState<ChildProfile[]>([]);
  const [guardian, setGuardian] = useState<GuardianProfile | null>(null);
  const [grades, setGrades] = useState<CatalogItem[]>([]);
  const [mutationStatus, setMutationStatus] = useState<LoadStatus>('idle');
  const [mutationError, setMutationError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const [profileRes, statusRes, gradesRes] = await Promise.all([
        profileApi.getMyProfile(),
        profileApi.getDependentLinkStatus(),
        profileApi.getGrades(),
      ]);
      setProfile(profileRes.data);
      setLinkStatus(statusRes.data);
      setGrades(gradesRes.data);

      if (statusRes.data.minorAccount) {
        try {
          const guardianRes = await profileApi.getMyGuardian();
          setGuardian(guardianRes.data);
        } catch {
          setGuardian(null);
        }
        setChildren([]);
      } else if (statusRes.data.childrenLinkOptional) {
        const childrenRes = await profileApi.getMyChildren();
        setChildren(childrenRes.data);
        setGuardian(null);
      } else {
        setChildren([]);
        setGuardian(null);
      }

      setStatus('success');
    } catch (error) {
      console.error('Lỗi tải hồ sơ phụ thuộc:', error);
      setErrorMessage(extractApiErrorMessage(error, 'Không tải được thông tin hồ sơ.'));
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const updateProfile = useCallback(
    async (body: UpdateProfileRequest) => {
      setMutationStatus('loading');
      setMutationError(null);
      try {
        await profileApi.updateMyProfile(body);
        await reload();
        setMutationStatus('success');
        return true;
      } catch (error) {
        setMutationError(extractApiErrorMessage(error, 'Không thể cập nhật hồ sơ.'));
        setMutationStatus('error');
        return false;
      }
    },
    [reload],
  );

  const linkGuardian = useCallback(
    async (parentEmail: string) => {
      setMutationStatus('loading');
      setMutationError(null);
      try {
        const res = await profileApi.linkGuardian({ parentEmail });
        setGuardian(res.data);
        await reload();
        setMutationStatus('success');
        return true;
      } catch (error) {
        setMutationError(extractApiErrorMessage(error, 'Không thể liên kết phụ huynh.'));
        setMutationStatus('error');
        return false;
      }
    },
    [reload],
  );

  const createChild = useCallback(
    async (body: ChildProfileRequest) => {
      setMutationStatus('loading');
      setMutationError(null);
      try {
        await profileApi.createChild(body);
        await reload();
        setMutationStatus('success');
        return true;
      } catch (error) {
        setMutationError(extractApiErrorMessage(error, 'Không thể thêm hồ sơ con.'));
        setMutationStatus('error');
        return false;
      }
    },
    [reload],
  );

  return {
    status,
    errorMessage,
    profile,
    linkStatus,
    children,
    guardian,
    grades,
    mutationStatus,
    mutationError,
    reload,
    updateProfile,
    linkGuardian,
    createChild,
  };
}

export function useDependentLinkStatus() {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [linkStatus, setLinkStatus] = useState<DependentLinkStatus | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setStatus('loading');
    setErrorMessage(null);
    try {
      const res = await profileApi.getDependentLinkStatus();
      setLinkStatus(res.data);
      setStatus('success');
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, 'Không kiểm tra được trạng thái liên kết.'));
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, linkStatus, errorMessage, reload };
}
