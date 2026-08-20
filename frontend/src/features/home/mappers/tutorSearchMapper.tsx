import type { TutorSearchApiResponse, TutorSearchItem } from '../types/tutorSearchTypes';
import type { FeaturedTutor } from '../types/homeTypes';

export function mapTutorSearchItem(item: TutorSearchApiResponse): TutorSearchItem {
  return {
    id: String(item.tutorId),
    fullName: item.fullName,
    bio: item.bio,
    hourlyRate: Number(item.hourlyRate),
    ratingAvg: Number(item.ratingAvg),
    experienceYears: item.experienceYears,
    verificationStatus: item.verificationStatus ?? null,
  };
}

export function tutorSearchToFeatured(tutor: TutorSearchItem): FeaturedTutor {
  return {
    id: tutor.id,
    fullName: tutor.fullName,
    gender: null,
    bio: tutor.bio,
    hourlyRate: tutor.hourlyRate,
    ratingAvg: tutor.ratingAvg,
    experienceYears: tutor.experienceYears,
    verificationStatus: tutor.verificationStatus,
  };
}
