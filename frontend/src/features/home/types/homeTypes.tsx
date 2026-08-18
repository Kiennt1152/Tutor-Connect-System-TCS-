export interface SubjectItem {
  id: string;
  name: string;
}

export interface TutorEducation {
  educationId: number;
  institution: string | null;
  degree: string | null;
  fieldOfStudy: string | null;
  startYear: number | null;
  endYear: number | null;
}

export interface TutorCertificate {
  certificateId: number;
  name: string | null;
  issuer: string | null;
  issueDate: string | null;
}

export interface TutorExperienceItem {
  experienceId: number;
  role: string | null;
  organization: string | null;
  startDate: string | null;
  endDate: string | null;
  description: string | null;
}

export interface PublicTutorProfile {
  tutorId: number;
  userId: number;
  fullName: string;
  avatarUrl: string | null;
  gender: string | null;
  bio: string | null;
  experienceYears: number | null;
  hourlyRate: number | null;
  ratingAvg: number | null;
  verificationStatus: string | null;
  educations: TutorEducation[];
  certificates: TutorCertificate[];
  experiences: TutorExperienceItem[];
}

export interface FeaturedTutor {
  id: string;
  fullName: string;
  gender: string | null;
  bio: string | null;
  hourlyRate: number;
  ratingAvg: number;
  experienceYears: number;
  verificationStatus: string | null;
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
