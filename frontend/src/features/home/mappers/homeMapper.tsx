import type { HomeApiResponse, HomeData } from '../types/homeTypes';

export function mapHomeResponse(response: HomeApiResponse): HomeData {
  return {
    totalTutors: response.totalTutors,
    totalSubjects: response.totalSubjects,
    totalClasses: response.totalClasses,
    subjects: response.subjects ?? [],
    featuredTutors: (response.featuredTutors ?? []).map((tutor) => ({
      ...tutor,
      hourlyRate: Number(tutor.hourlyRate),
      ratingAvg: Number(tutor.ratingAvg),
    })),
  };
}
