import axiosClient from '../../../shared/api/axiosClient';
import type { AvatarUploadResponse, UpdateProfileRequest, ProfileResponse } from '../types/profileTypes';

export const PROFILE_API_BASE = '/profile';

export const profileApi = {
  http: axiosClient,
  basePath: PROFILE_API_BASE,

  async getMyProfile(): Promise<ProfileResponse> {
    const res = await axiosClient.get<ProfileResponse>('/profile/me');
    return res.data;
  },

  async updateMyProfile(payload: UpdateProfileRequest): Promise<ProfileResponse> {
    const res = await axiosClient.put<ProfileResponse>('/profile/me', payload);
    return res.data;
  },

  async uploadAvatar(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await axiosClient.post<AvatarUploadResponse>('/profile/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.avatarUrl;
  },

  async submitVerification(): Promise<void> {
    await axiosClient.post('/profile/verification/submit');
  },
};
