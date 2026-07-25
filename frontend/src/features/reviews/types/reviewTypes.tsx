export type ReviewCriterionScore = {
  code: string;
  question: string;
  score: number;
};

export type ReviewableAssignment = {
  assignmentId: number;
  classId: number;
  classTitle: string;
  subjectName: string | null;
  classStatus: string;
  tutorUserId: number;
  tutorName: string;
  reviewed: boolean;
  reviewId: number | null;
  rating: number | null;
  comment: string | null;
  criteriaJson: string | null;
  reviewedAt: string | null;
};

export type CreateReviewPayload = {
  assignmentId: number;
  comment?: string;
  criteria: ReviewCriterionScore[];
};

export type ReviewResponse = {
  reviewId: number;
  assignmentId: number;
  reviewerId: number;
  revieweeId: number;
  reviewType: string;
  rating: number;
  comment: string | null;
  createdAt: string;
};
