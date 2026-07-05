import axiosClient from '../../../shared/api/axiosClient';
import type {
  ClassSummary,
  TutorApplication,
  TutorApplicationReviewRequest,
} from '../types/marketplaceTypes';

export const MARKETPLACE_API_BASE = '/marketplace';

function asString(value: number | null | undefined): string | undefined {
  return value == null ? undefined : String(value);
}

export const marketplaceApi = {
  listClasses(): Promise<ClassSummary[]> {
    return axiosClient
      .get(`${MARKETPLACE_API_BASE}/classes`)
      .then((res) => res.data as ClassSummary[]);
  },

  applyToClass(
    classId: number,
    payload: { proposedRate?: number | null; coverLetter?: string | null },
  ): Promise<void> {
    return axiosClient
      .post(`${MARKETPLACE_API_BASE}/classes/${classId}/apply`, {
        proposedRate: payload.proposedRate ?? null,
        coverLetter: payload.coverLetter ?? null,
      })
      .then(() => undefined);
  },

  listApplicationsByClass(classId: number): Promise<TutorApplication[]> {
    return axiosClient
      .get(`${MARKETPLACE_API_BASE}/classes/${classId}/applications`)
      .then((res) => res.data as TutorApplication[]);
  },

  listMyApplications(): Promise<TutorApplication[]> {
    return axiosClient
      .get(`${MARKETPLACE_API_BASE}/applications/me`)
      .then((res) => res.data as TutorApplication[]);
  },

  getApplication(applicationId: number): Promise<TutorApplication> {
    return axiosClient
      .get(`${MARKETPLACE_API_BASE}/applications/${applicationId}`)
      .then((res) => res.data as TutorApplication);
  },

  withdrawApplication(applicationId: number): Promise<void> {
    return axiosClient
      .delete(`${MARKETPLACE_API_BASE}/applications/${applicationId}`)
      .then(() => undefined);
  },

  reviewApplication(
    applicationId: number,
    payload: TutorApplicationReviewRequest,
  ): Promise<TutorApplication> {
    return axiosClient
      .patch(
        `${MARKETPLACE_API_BASE}/applications/${applicationId}/review`,
        payload,
      )
      .then((res) => res.data as TutorApplication);
  },
};

export const _internal = { asString };