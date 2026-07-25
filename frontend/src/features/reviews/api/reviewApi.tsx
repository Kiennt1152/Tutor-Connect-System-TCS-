import axiosClient from '../../../shared/api/axiosClient';
import type {
  CreateReviewPayload,
  ReviewResponse,
  ReviewableAssignment,
} from '../types/reviewTypes';

const REVIEW_API_BASE = '/contract/reviews';

export const reviewApi = {
  /** Cac lop da hoan thanh cua khach hang hien tai, kem trang thai da danh gia. */
  getReviewable() {
    return axiosClient.get<ReviewableAssignment[]>(`${REVIEW_API_BASE}/reviewable`);
  },
  /** Gui danh gia + phan hoi cho gia su. */
  create(payload: CreateReviewPayload) {
    return axiosClient.post<ReviewResponse>(REVIEW_API_BASE, payload);
  },
};
