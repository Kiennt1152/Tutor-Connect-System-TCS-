import axiosClient from '../../../shared/api/axiosClient';
import type {
  ApplicantResponse,
  CatalogOption,
  ClassRequestPayload,
  ClassResponse,
  LocationOption,
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

  // --- Lớp của Client hiện tại ---
  listMyClasses: () =>
    axiosClient.get<ClassResponse[]>('/marketplace/classes/mine').then((r) => r.data),

  getClass: (classId: number) =>
    axiosClient.get<ClassResponse>(`/marketplace/classes/${classId}`).then((r) => r.data),

  // --- Gia sư tìm lớp: danh sách lớp đang mở đơn ứng tuyển ---
  listOpenClasses: () =>
    axiosClient
      .get<ClassResponse[]>('/marketplace/classes', { params: { status: 'OPEN' } })
      .then((r) => r.data),

  applyToClass: (classId: number, payload: { proposedRate?: number; coverLetter?: string }) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/classes/${classId}/apply`, payload)
      .then((r) => r.data),

  // --- Hồ sơ gia sư hiện tại (dùng cho form ứng tuyển) ---
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

  // --- Ứng viên của lớp (Client xem + AI gợi ý top 5) ---
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

  // --- Catalog cho dropdown của form ---
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
};

function toOption(dto: CatalogItemDto): CatalogOption {
  return { id: dto.id, name: dto.name, description: dto.description ?? null };
}

/** Số của "Lớp N" (null nếu không phải khối lớp phổ thông). */
function gradeNumber(name: string): number | null {
  const match = /^Lớp\s+(\d+)/.exec(name.trim());
  return match ? Number(match[1]) : null;
}

/** Sắp xếp: Lớp 1 → Lớp 12 (theo số), các mục khác (Luyện thi…) xếp sau. */
function compareGrade(a: CatalogOption, b: CatalogOption): number {
  const na = gradeNumber(a.name);
  const nb = gradeNumber(b.name);
  if (na !== null && nb !== null) return na - nb;
  if (na !== null) return -1;
  if (nb !== null) return 1;
  return a.name.localeCompare(b.name, 'vi');
}
