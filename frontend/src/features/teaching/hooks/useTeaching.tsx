import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { teachingApi } from '../api/teachingApi';
import type {
  AssignmentResponse,
  LessonResponse,
  RescheduleLessonPayload,
  RescheduleRequestResponse,
} from '../types/teachingTypes';

type LoadStatus = 'loading' | 'success' | 'error';

/** Lấy câu lỗi từ phản hồi API; không có thì dùng câu dự phòng. */
function extractError(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  return fallback;
}

/** Hook tải các buổi học của một lớp (bỏ qua kết quả nếu component đã bị gỡ). */
export function useClassLessons(classId: number) {
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [lessons, setLessons] = useState<LessonResponse[]>([]);

  useEffect(() => {
    let alive = true;
    setStatus('loading');
    teachingApi
      .listMyLessons()
      .then((all) => {
        if (!alive) return;
        setLessons(all.filter((l) => l.classId === classId));
        setStatus('success');
      })
      .catch(() => alive && setStatus('error'));
    return () => {
      alive = false;
    };
  }, [classId]);

  return { status, lessons };
}

/** Hook đếm số lời mời nhận lớp đang chờ của gia sư (để hiện số trên menu); tắt thì luôn 0. */
export function usePendingInviteCount(enabled: boolean) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    if (!enabled) {
      return;
    }
    let alive = true;
    teachingApi
      .listMyAssignments()
      .then((list) => {
        if (alive) setCount(list.filter((a) => a.status === 'PENDING').length);
      })
      .catch(() => alive && setCount(0));
    return () => {
      alive = false;
    };
  }, [enabled]);

  return enabled ? count : 0;
}

/**
 * Hook dữ liệu màn lịch dạy/lịch học: phân công, buổi học, yêu cầu đổi lịch và các thao tác
 * (nhận/từ chối lớp, điểm danh, đổi lịch, duyệt/thu hồi, hoàn thành lớp) kèm thông báo kết quả.
 */
export function useTeaching() {
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [assignments, setAssignments] = useState<AssignmentResponse[]>([]);
  const [lessons, setLessons] = useState<LessonResponse[]>([]);
  const [requests, setRequests] = useState<RescheduleRequestResponse[]>([]);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  /** Tải lại phân công, buổi học và yêu cầu đổi lịch cùng lúc (lỗi ở buổi học/yêu cầu không làm hỏng cả trang). */
  const reload = useCallback(async () => {
    setStatus('loading');
    try {
      const [a, l, r] = await Promise.all([
        teachingApi.listMyAssignments(),
        teachingApi.listMyLessons().catch(() => [] as LessonResponse[]),
        teachingApi.listRescheduleRequests().catch(() => [] as RescheduleRequestResponse[]),
      ]);
      setAssignments(a);
      setLessons(l);
      setRequests(r);
      setStatus('success');
    } catch {
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  /**
   * Chạy một thao tác: xoá thông báo cũ, gọi API, hiện câu kết quả và tải lại; lỗi thì hiện câu lỗi, trả về true/false.
   */
  const run = useCallback(
    async (action: () => Promise<{ message: string }>, fallbackError: string) => {
      setNotice(null);
      setError(null);
      try {
        const res = await action();
        setNotice(res.message);
        await reload();
        return true;
      } catch (err) {
        setError(extractError(err, fallbackError));
        return false;
      }
    },
    [reload],
  );

  return {
    status,
    assignments,
    lessons,
    requests,
    notice,
    error,
    reload,
    /** Nhận lớp. */
    accept: (id: number) => run(() => teachingApi.acceptAssignment(id), 'Nhận lớp thất bại.'),
    /** Từ chối lời mời nhận lớp. */
    decline: (id: number) => run(() => teachingApi.declineAssignment(id), 'Từ chối lớp thất bại.'),
    /** Điểm danh có mặt/vắng cho buổi học. */
    attend: (id: number, present: boolean) =>
      run(() => teachingApi.markAttendance(id, present), 'Điểm danh thất bại.'),

    /** Gửi yêu cầu đổi lịch một buổi. */
    requestReschedule: (lessonId: number, payload: RescheduleLessonPayload) =>
      run(
        () =>
          teachingApi
            .requestReschedule(lessonId, payload)
            .then(() => ({ message: 'Đã gửi yêu cầu đổi lịch — chờ bên còn lại duyệt.' })),
        'Gửi yêu cầu đổi lịch thất bại.',
      ),
    /** Duyệt/từ chối yêu cầu đổi lịch. */
    decideRequest: (requestId: number, approve: boolean, note?: string) =>
      run(() => teachingApi.decideRequest(requestId, approve, note), 'Xử lý yêu cầu thất bại.'),
    /** Thu hồi yêu cầu đổi lịch. */
    cancelRequest: (requestId: number) =>
      run(() => teachingApi.cancelRequest(requestId), 'Thu hồi yêu cầu thất bại.'),
    /** Gia sư bấm hoàn thành lớp. */
    confirmCompletion: (classId: number) =>
      run(() => teachingApi.confirmClassCompletion(classId), 'Hoàn thành lớp thất bại.'),
  };
}
