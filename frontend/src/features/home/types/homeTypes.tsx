export interface SubjectItem {
  id: string;
  name: string;
}

export interface FeaturedTutor {
  id: string;
  fullName: string;
  gender: string | null;
  bio: string | null;
  hourlyRate: number;
  ratingAvg: number;
  experienceYears: number;
}

export interface HomeData {
  totalTutors: number;
  totalSubjects: number;
  totalClasses: number;
  subjects: SubjectItem[];
  featuredTutors: FeaturedTutor[];
}

export interface HomeRequest {}

export interface HomeApiResponse {
  totalTutors: number;
  totalSubjects: number;
  totalClasses: number;
  subjects: SubjectItem[];
  featuredTutors: FeaturedTutor[];
}
