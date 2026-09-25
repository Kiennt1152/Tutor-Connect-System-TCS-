import axiosClient from '../../../shared/api/axiosClient';
import type {
  ApplicantResponse,
  CatalogOption,
  ClassTerminationResponse,
  CreateClassTerminationRequest,
  CenterProfile,
  CenterSummary,
  ClassRequest,
  ClassRequestPayload,
  ClassResponse,
  CreateClassRequestPayload,
  LocationOption,
  MarketplaceClass,
  TutorProfileCard,
  ClassBusyConflict,
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

  /** Lấy các tin/lớp do người đăng nhập tạo. */
  listMyClasses: () =>
    axiosClient.get<ClassResponse[]>('/marketplace/classes/mine').then((r) => r.data),

  /** Lấy các lớp đang mở (OPEN) cho màn tìm lớp. */
  listOpenClasses: () =>
    axiosClient
      .get<ClassResponse[]>('/marketplace/classes', { params: { status: 'OPEN' } })
      .then((r) => r.data),

  /**
   * Tin cho bảng "Danh sách tin đã đăng": gồm lớp đang mở VÀ lớp đã chọn gia sư nhưng
   * chưa ký xong hợp đồng / chưa chuyển cọc — tin chỉ bị gỡ khi cả hai việc đó hoàn tất.
   */
  listBoardClasses: () =>
    axiosClient.get<ClassResponse[]>('/marketplace/classes/board').then((r) => r.data),

  /** Gia sư nộp đơn ứng tuyển với học phí đề xuất từng môn và thư ngỏ. */
  applyToClass: (
    classId: number,
    payload: { proposedRates: Record<string, number>; coverLetter?: string },
  ) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/classes/${classId}/apply`, payload)
      .then((r) => r.data),

  /** Id các lớp gia sư đã ứng tuyển (đơn còn hiệu lực). */
  listMyAppliedClassIds: () =>
    axiosClient.get<number[]>('/marketplace/applications/mine').then((r) => r.data),

  /** Gia sư: lớp nào (trong danh sách) có buổi trùng thời gian bận đã đăng ký. Chỉ trả lớp CÓ trùng. */
  listMyBusyConflicts: (classIds: number[]) =>
    axiosClient
      .get<ClassBusyConflict[]>('/marketplace/busy-conflicts', { params: { classIds: classIds.join(',') } })
      .then((r) => r.data),

  /** Hồ sơ của người đăng nhập (dùng lấy trạng thái xác minh và mức phí gợi ý). */
  getMyTutorProfile: () =>
    axiosClient.get<TutorProfileCard>('/profile/me').then((r) => r.data),

  /**
   * Tạo tin mới. Lưu ý: backend luôn lưu ở trạng thái DRAFT (nháp) — gia sư chưa thấy được.
   * Muốn công khai phải gọi tiếp publishClass() bên dưới.
   */
  createClass: (payload: ClassRequestPayload) =>
    axiosClient.post<ClassResponse>('/marketplace/classes', payload).then((r) => r.data),

  /** Chủ lớp sửa tin tìm gia sư. */
  updateClass: (classId: number, payload: ClassRequestPayload) =>
    axiosClient.put<ClassResponse>(`/marketplace/classes/${classId}`, payload).then((r) => r.data),

  /** Đăng lớp (Nháp -> Đang mở). */
  publishClass: (classId: number) =>
    axiosClient
      .post<ClassResponse>(`/marketplace/classes/${classId}/publish`)
      .then((r) => r.data),

  /** Gỡ đăng lớp (Đang mở -> Nháp). */
  unpublishClass: (classId: number) =>
    axiosClient
      .post<ClassResponse>(`/marketplace/classes/${classId}/unpublish`)
      .then((r) => r.data),

  /** Danh sách ứng viên của lớp (đã chấm điểm, xếp hạng). */
  listApplicants: (classId: number) =>
    axiosClient
      .get<ApplicantResponse[]>(`/marketplace/classes/${classId}/applications`)
      .then((r) => r.data),

  /** Chủ lớp chọn một ứng viên cho lớp. */
  chooseApplicant: (classId: number, applicationId: number) =>
    axiosClient
      .post<{ message: string }>(
        `/marketplace/classes/${classId}/applications/${applicationId}/choose`,
      )
      .then((r) => r.data),

  /** Chủ lớp từ chối một ứng viên kèm lý do. */
  rejectApplicant: (classId: number, applicationId: number, reason: string) =>
    axiosClient
      .post<{ message: string }>(
        `/marketplace/classes/${classId}/applications/${applicationId}/reject`,
        { reason },
      )
      .then((r) => r.data),

  /** Danh mục môn học. */
  listSubjects: () =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/subjects')
      .then((r) => r.data.map(toOption)),

  /** Danh mục khối lớp, sắp theo số lớp tăng dần. */
  listGrades: () =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/grades')
      .then((r) => r.data.map(toOption).sort(compareGrade)),

  /** Danh mục tỉnh/thành. */
  listProvinces: () =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/provinces')
      .then((r) => r.data.map(toOption)),

  /** Danh mục quận/huyện của một tỉnh. */
  listDistricts: (provinceId: number) =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/districts', { params: { provinceId } })
      .then((r) => r.data.map(toOption)),

  /** Danh mục phường/xã của một quận. */
  listWards: (districtId: number) =>
    axiosClient
      .get<CatalogItemDto[]>('/catalog/wards', { params: { districtId } })
      .then((r) => r.data.map(toOption)),

  /** Danh sách địa điểm có sẵn của một tỉnh. */
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

  /** UC "Hoàn thành lớp": gia sư bấm hoàn thành; lớp đóng sau khi học viên đánh giá gia sư. */
  confirmCompletion(classId: number) {
    return axiosClient.post<{ message: string }>(
      `${MARKETPLACE_API_BASE}/classes/${classId}/complete`,
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
  getCenterProfile(centerId: number | string) {
    return axiosClient.get<CenterProfile>(`${MARKETPLACE_API_BASE}/centers/${centerId}`);
  },
  createClassRequest(centerId: number, payload: CreateClassRequestPayload) {
    return axiosClient.post<ClassRequest>(
      `${MARKETPLACE_API_BASE}/centers/${centerId}/class-requests`,
      payload,
    ).then((r) => r.data);
  },
  getMyClassRequests() {
    return axiosClient.get<ClassRequest[]>(`${MARKETPLACE_API_BASE}/class-requests/mine`)
      .then((r) => r.data);
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

/** Đổi mục danh mục từ API sang dạng lựa chọn dùng trong form. */
function toOption(dto: CatalogItemDto): CatalogOption {
  return { id: dto.id, name: dto.name, description: dto.description ?? null };
}

/** Số lớp trong tên khối ("Lớp 9" -> 9); khối không có số thì null. */
function gradeNumber(name: string): number | null {
  const match = /^Lớp\s+(\d+)/.exec(name.trim());
  return match ? Number(match[1]) : null;
}

/** So sánh để sắp khối lớp: khối có số theo thứ tự tăng dần, rồi đến khối khác theo tên. */
function compareGrade(a: CatalogOption, b: CatalogOption): number {
  const na = gradeNumber(a.name);
  const nb = gradeNumber(b.name);
  if (na !== null && nb !== null) return na - nb;
  if (na !== null) return -1;
  if (nb !== null) return 1;
  return a.name.localeCompare(b.name, 'vi');
}
