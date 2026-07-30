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
  anonymous: boolean;
  reviewerDisplayName: string | null;
  reviewedAt: string | null;
  tutorReply: string | null;
  tutorReplyAt: string | null;
  reviewable: boolean;
  reviewsSubmitted: number;
  reviewOverdue: boolean;
};

export type CreateReviewPayload = {
  assignmentId: number;
  comment?: string;
  anonymous: boolean;
  displayName?: string;
  criteria: ReviewCriterionScore[];
};

export type UpdateReviewPayload = Omit<CreateReviewPayload, 'assignmentId'>;

export type ReviewResponse = {
  reviewId: number;
  assignmentId: number;
  reviewerId: number;
  revieweeId: number;
  reviewType: string;
  rating: number;
  comment: string | null;
  tutorReply: string | null;
  tutorReplyAt: string | null;
  criteriaJson: string | null;
  classTitle: string | null;
  subjectName: string | null;
  anonymous: boolean;
  reviewerDisplayName: string;
  createdAt: string;
};

export type CriterionAverage = {
  code: string;
  question: string | null;
  average: number;
  count: number;
};

export type TutorReputation = {
  tutorId: number;
  tutorUserId: number;
  fullName: string;
  avatar: string | null;
  bio: string | null;
  experienceYears: number;
  hourlyRate: number;
  verificationStatus: string | null;
  ratingAvg: number;
  totalReviews: number;
  ratingDistribution: Record<string, number>;
  criteriaAverages: CriterionAverage[];
  reviews: ReviewResponse[];
};
