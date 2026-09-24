/**
 * ============================================================================
 * [UC-08] CUSTOM HOOK QUẢN LÝ DỮ LIỆU HỒ SƠ NGƯỜI DÙNG (USE PROFILE HOOK)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-07-02
 * 
 * 1. Mục đích & Chức năng:
 *    - Quản lý vòng đời dữ liệu hồ sơ người dùng đăng nhập hiện tại (Client, Tutor, Center).
 *    - Cung cấp các thao tác tải lại dữ liệu (reload), cập nhật hồ sơ (updateProfile) và tải ảnh đại diện (uploadAvatar).
 * 
 * 2. Luồng xử lý chính:
 *    - Bước 1: Gọi profileApi.getMe() khi mount component để lấy profile và cờ firstLogin.
 *    - Bước 2: Quản lý các cờ loading, saving, uploadingAvatar cho giao diện hiển thị trạng thái mượt mà.
 *    - Bước 3: Đảm bảo đồng bộ dữ liệu sau khi người dùng lưu thông tin hoặc đổi avatar.
 * ============================================================================
 */

import { useCallback, useEffect, useState } from 'react';
import { profileApi } from '../api/profileApi';
import type { ProfileResponse, UpdateProfileRequest } from '../types/profileTypes';

export interface UseProfileResult {
  profile: ProfileResponse | null;
  loading: boolean;
  error: string | null;
  saving: boolean;
  uploadingAvatar: boolean;
  reload: () => Promise<void>;
  updateProfile: (payload: UpdateProfileRequest) => Promise<ProfileResponse | null>;
  uploadAvatar: (file: File) => Promise<string | null>;
}

export function useProfile(): UseProfileResult {
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await profileApi.getMyProfile();
      setProfile(data);
    } catch (e) {
      setError(extractMessage(e, 'Không thể tải hồ sơ'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const updateProfile = useCallback(
    async (payload: UpdateProfileRequest) => {
      setSaving(true);
      setError(null);
      try {
        const updated = await profileApi.updateMyProfile(payload);
        setProfile(updated);
        return updated;
      } catch (e) {
        setError(extractMessage(e, 'Không thể cập nhật hồ sơ'));
        return null;
      } finally {
        setSaving(false);
      }
    },
    [],
  );

  const uploadAvatar = useCallback(async (file: File) => {
    setUploadingAvatar(true);
    setError(null);
    try {
      const url = await profileApi.uploadAvatar(file);
      setProfile((prev) => (prev ? { ...prev, avatarUrl: url } : prev));
      return url;
    } catch (e) {
      setError(extractMessage(e, 'Không thể tải ảnh lên'));
      return null;
    } finally {
      setUploadingAvatar(false);
    }
  }, []);

  return {
    profile,
    loading,
    error,
    saving,
    uploadingAvatar,
    reload,
    updateProfile,
    uploadAvatar,
  };
}

function extractMessage(err: unknown, fallback: string): string {
  if (typeof err === 'object' && err !== null) {
    const ax = err as {
      response?: {
        data?: {
          message?: string;
          fieldErrors?: Record<string, string>;
        };
      };
      message?: string;
    };
    return ax.response?.data?.message ?? ax.message ?? fallback;
  }
  return fallback;
}

export function extractFieldErrors(err: unknown): Record<string, string> {
  if (typeof err !== 'object' || err === null) return {};
  const ax = err as { response?: { data?: { fieldErrors?: Record<string, string> } } };
  return ax.response?.data?.fieldErrors ?? {};
}
