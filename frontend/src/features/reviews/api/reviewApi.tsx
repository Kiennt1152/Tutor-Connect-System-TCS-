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
  getReviewable() {
    return axiosClient.get<ReviewableAssignment[]>(`${REVIEW_API_BASE}/reviewable`);
  },
  create(payload: CreateReviewPayload) {
    return axiosClient.post<ReviewResponse>(REVIEW_API_BASE, payload);
  },
  update(reviewId: number, payload: UpdateReviewPayload) {
    return axiosClient.put<ReviewResponse>(`${REVIEW_API_BASE}/${reviewId}`, payload);
  },
  getTutorReputation(tutorId: number | string) {
    return axiosClient.get<TutorReputation>(`${REVIEW_API_BASE}/reputation/${tutorId}`);
  },
  getMyReputation() {
    return axiosClient.get<TutorReputation>(`${REVIEW_API_BASE}/my-reputation`);
  },
  replyToReview(reviewId: number, reply: string) {
    return axiosClient.post<ReviewResponse>(`${REVIEW_API_BASE}/${reviewId}/reply`, { reply });
  },
};
