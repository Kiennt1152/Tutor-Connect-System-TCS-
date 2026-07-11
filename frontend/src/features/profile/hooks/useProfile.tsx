import { useCallback, useEffect, useState } from 'react';
import { profileApi } from '../api/profileApi';
import type { ProfileResponse, UpdateProfileRequest } from '../types/profileTypes';

interface UseProfileResult {
  profile: ProfileResponse | null;
  loading: boolean;
  error: string | null;
  saving: boolean;
  uploadingAvatar: boolean;
  reload: () => Promise<void>;
  updateProfile: (payload: UpdateProfileRequest) => Promise<ProfileResponse | null>;
  uploadAvatar: (file: File) => Promise<string | null>;
  submitVerification: () => Promise<boolean>;
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

  const submitVerification = useCallback(async () => {
    try {
      await profileApi.submitVerification();
      return true;
    } catch (e) {
      setError(extractMessage(e, 'Không thể nộp hồ sơ xác minh'));
      return false;
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
    submitVerification,
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
