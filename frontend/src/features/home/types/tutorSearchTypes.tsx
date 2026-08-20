export type TutorSearchApiResponse = {
  tutorId: number;
  userId: number;
  fullName: string;
  bio: string | null;
  experienceYears: number;
  hourlyRate: number;
  ratingAvg: number;
  verificationStatus: string;
};

export type TutorSearchItem = {
  id: string;
  fullName: string;
  bio: string | null;
  hourlyRate: number;
  ratingAvg: number;
  experienceYears: number;
  verificationStatus: string | null;
};

export type TutorSearchParams = {
  keyword?: string;
  subjectId?: number;
};
