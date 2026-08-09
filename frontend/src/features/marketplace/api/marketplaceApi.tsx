import axiosClient from '../../../shared/api/axiosClient';
import type {
  ApplicantResponse,
  CatalogOption,
  ClassTerminationResponse,
  CreateClassTerminationRequest,
  CenterSummary,
  ClassRequest,
  ClassRequestPayload,
  ClassResponse,
  CreateClassRequestPayload,
  LocationOption,
  MarketplaceClass,
  TutorProfileCard,
} from '../types/marketplaceTypes';

export const MARKETPLACE_API_BASE = '/marketplace';

interface CatalogItemDto {
  id: number;
  name: string;
  description?: string | null;
}

interface LocationDto {
  locationId: number;
  provinceId: number;
  provinceName: string;
  districtName: string | null;
  wardName: string | null;
}

export const marketplaceApi = {
  http: axiosClient,
  basePath: MARKETPLACE_API_BASE,

  listMyClasses: () =>
    axiosClient.get<ClassResponse[]>('/marketplace/classes/mine').then((r) => r.data),

  listOpenClasses: () =>
    axiosClient
      .get<ClassResponse[]>('/marketplace/classes', { params: { status: 'OPEN' } })
      .then((r) => r.data),

  applyToClass: (
    classId: number,
    payload: { proposedRates: Record<string, number>; coverLetter?: string },
  ) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/classes/${classId}/apply`, payload)
      .then((r) => r.data),

  listMyAppliedClassIds: () =>
    axiosClient.get<number[]>('/marketplace/applications/mine').then((r) => r.data),

  getMyTutorProfile: () =>
    axiosClient.get<TutorProfileCard>('/profile/me').then((r) => r.data),

  createClass: (payload: ClassRequestPayload) =>
    axiosClient.post<ClassResponse>('/marketplace/classes', payload).then((r) => r.data),

  updateClass: (classId: number, payload: ClassRequestPayload) =>
    axiosClient.put<ClassResponse>(`/marketplace/classes/${classId}`, payload).then((r) => r.data),

  publishClass: (classId: number) =>
    axiosClient
      .post<ClassResponse>(`/marketplace/classes/${classId}/publish`)
      .then((r) => r.data),

  unpublishClass: (classId: number) =>
    axiosClient
      .post<ClassResponse>(`/marketplace/classes/${classId}/unpublish`)
      .then((r) => r.data),

  listApplicants: (classId: number) =>
    axiosClient
      .get<ApplicantResponse[]>(`/marketplace/classes/${classId}/applications`)
      .then((r) => r.data),

  chooseApplicant: (classId: number, applicationId: number) =>
    axiosClient
      .post<{ message: string }>(
        `/marketplace/classes/${classId}/applications/${applicationId}/choose`,
      )
      .then((r) => r.data),

  rejectApplicant: (classId: number, applicationId: number, reason: string) =>
    axiosClient
      .post<{ message: string }>(
        `/marketplace/classes/${classId}/applications/${applicationId}/reject`,
        { reason },
      )
      .then((r) => r.data),

  listSubjects: () =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/subjects')
      .then((r) => r.data.map(toOption)),

  listGrades: () =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/grades')
      .then((r) => r.data.map(toOption).sort(compareGrade)),

  listProvinces: () =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/provinces')
      .then((r) => r.data.map(toOption)),

  listDistricts: (provinceId: number) =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/districts', { params: { provinceId } })
      .then((r) => r.data.map(toOption)),

  listWards: (districtId: number) =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/wards', { params: { districtId } })
      .then((r) => r.data.map(toOption)),

  listLocations: (provinceId: number) =>
    axiosClient
      .get<LocationDto[]>('/catalog/locations', { params: { provinceId } })
      .then((r) => r.data as LocationOption[]),

  // ----- Đăng ký lớp trực tiếp (main) -----
  getOpenClasses() {
    return axiosClient.get<MarketplaceClass[]>(`${MARKETPLACE_API_BASE}/classes?status=OPEN`);
  },

  getClass(
    classId: number,
    target?: { assignmentId?: number; classStudentId?: number },
  ) {
    return axiosClient.get<MarketplaceClass>(`${MARKETPLACE_API_BASE}/classes/${classId}`, {
      params: {
        assignmentId: target?.assignmentId,
        classStudentId: target?.classStudentId,
      },
    });
  },

  register(classId: number) {
    return axiosClient.post<{ message: string }>(
      `${MARKETPLACE_API_BASE}/classes/${classId}/register`,
    );
  },

  async requestClassTermination(
    classId: number,
    payload: CreateClassTerminationRequest,
  ): Promise<ClassTerminationResponse> {
    const response = await axiosClient.post<ClassTerminationResponse>(
      `${MARKETPLACE_API_BASE}/classes/${classId}/termination`,
      payload,
    );
    return response.data;
  },

  // ----- Yêu cầu mở lớp gửi tới một trung tâm (phía phụ huynh) -----
  listCenters() {
    return axiosClient.get<CenterSummary[]>(`${MARKETPLACE_API_BASE}/centers`);
  },
  createClassRequest(centerId: number, payload: CreateClassRequestPayload) {
    return axiosClient.post<ClassRequest>(
      `${MARKETPLACE_API_BASE}/centers/${centerId}/class-requests`,
      payload,
    );
  },
  getMyClassRequests() {
    return axiosClient.get<ClassRequest[]>(`${MARKETPLACE_API_BASE}/class-requests/mine`);
  },
  cancelClassRequest(requestId: string) {
    return axiosClient.delete<{ message: string }>(
      `${MARKETPLACE_API_BASE}/class-requests/${requestId}`,
    );
  },
  // Phụ huynh chọn 1 gia sư từ shortlist -> tạo lớp + phân công (vào luồng ký hợp đồng).
  chooseTutorForRequest(requestId: string, tutorId: number) {
    return axiosClient.post(
      `${MARKETPLACE_API_BASE}/class-requests/${requestId}/choose-tutor/${tutorId}`,
    );
  },
};

function toOption(dto: CatalogItemDto): CatalogOption {
  return { id: dto.id, name: dto.name, description: dto.description ?? null };
}

function gradeNumber(name: string): number | null {
  const match = /^Lớp\s+(\d+)/.exec(name.trim());
  return match ? Number(match[1]) : null;
}

function compareGrade(a: CatalogOption, b: CatalogOption): number {
  const na = gradeNumber(a.name);
  const nb = gradeNumber(b.name);
  if (na !== null && nb !== null) return na - nb;
  if (na !== null) return -1;
  if (nb !== null) return 1;
  return a.name.localeCompare(b.name, 'vi');
}
