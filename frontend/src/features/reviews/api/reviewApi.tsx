import axiosClient from '../../../shared/api/axiosClient';
import type {
  CreateReviewPayload,
  ReviewResponse,
  ReviewableAssignment,
  TutorReputation,
  UpdateReviewPayload,
} from '../types/reviewTypes';

const REVIEW_API_BASE = '/contract/reviews';

export const reviewApi = {
  /** Danh sách lớp khách có thể đánh giá / đã đánh giá ("Đánh giá của tôi"). */
  getReviewable() {
    return axiosClient.get<ReviewableAssignment[]>(`${REVIEW_API_BASE}/reviewable`);
  },
  /** Gửi đánh giá mới cho gia sư. */
  create(payload: CreateReviewPayload) {
    return axiosClient.post<ReviewResponse>(REVIEW_API_BASE, payload);
  },
  /** Sửa đánh giá đã gửi. */
  update(reviewId: number, payload: UpdateReviewPayload) {
    return axiosClient.put<ReviewResponse>(`${REVIEW_API_BASE}/${reviewId}`, payload);
  },
  /** Danh tiếng công khai của một gia sư. */
  getTutorReputation(tutorId: number | string) {
    return axiosClient.get<TutorReputation>(`${REVIEW_API_BASE}/reputation/${tutorId}`);
  },
  /** Danh tiếng của gia sư đang đăng nhập ("Nhận xét về tôi"). */
  getMyReputation() {
    return axiosClient.get<TutorReputation>(`${REVIEW_API_BASE}/my-reputation`);
  },
  /** Gia sư gửi/sửa phản hồi cho một đánh giá. */
  replyToReview(reviewId: number, reply: string) {
    return axiosClient.post<ReviewResponse>(`${REVIEW_API_BASE}/${reviewId}/reply`, { reply });
  },
};
