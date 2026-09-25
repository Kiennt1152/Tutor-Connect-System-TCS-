import axiosClient from '../../../shared/api/axiosClient';
import type { BusyTimePayload, BusyTimeResponse } from '../types/teachingTypes';

/**
 * Thời gian bận của gia sư. Mô hình chỉ lưu lúc BẬN — ngày nào không có lịch dạy và không
 * đăng ký bận thì hiểu là rảnh, nên không có API "lịch rảnh".
 */
export const busyTimeApi = {
  /** `month` dạng `yyyy-MM`, ví dụ `2026-09`. */
  listByMonth: (month: string) =>
    axiosClient
      .get<BusyTimeResponse[]>('/profile/busy-times', { params: { month } })
      .then((r) => r.data),

  /** Nhiều ngày một lượt, cùng một khung giờ; lỗi một ngày thì không lưu ngày nào. */
  create: (payload: BusyTimePayload) =>
    axiosClient.post<BusyTimeResponse[]>('/profile/busy-times', payload).then((r) => r.data),

  /** Xoá một lịch bận của gia sư. */
  remove: (busyTimeId: number) =>
    axiosClient.delete<void>(`/profile/busy-times/${busyTimeId}`).then(() => undefined),
};
