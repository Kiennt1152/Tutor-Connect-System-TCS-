import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { teachingApi } from '../api/teachingApi';
import type {
  AssignmentResponse,
  ExtraLessonPayload,
  LessonResponse,
  RescheduleLessonPayload,
  RescheduleRequestResponse,
} from '../types/teachingTypes';

type LoadStatus = 'loading' | 'success' | 'error';

function extractError(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  return fallback;
}

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

export function useTeaching() {
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [assignments, setAssignments] = useState<AssignmentResponse[]>([]);
  const [lessons, setLessons] = useState<LessonResponse[]>([]);
  const [requests, setRequests] = useState<RescheduleRequestResponse[]>([]);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

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
    accept: (id: number) => run(() => teachingApi.acceptAssignment(id), 'Nhận lớp thất bại.'),
    decline: (id: number) => run(() => teachingApi.declineAssignment(id), 'Từ chối lớp thất bại.'),
    attend: (id: number, present: boolean) =>
      run(() => teachingApi.markAttendance(id, present), 'Điểm danh thất bại.'),

    requestReschedule: (lessonId: number, payload: RescheduleLessonPayload) =>
      run(
        () =>
          teachingApi
            .requestReschedule(lessonId, payload)
            .then(() => ({ message: 'Đã gửi yêu cầu đổi lịch — chờ bên còn lại duyệt.' })),
        'Gửi yêu cầu đổi lịch thất bại.',
      ),
    requestExtraLesson: (payload: ExtraLessonPayload) =>
      run(
        () =>
          teachingApi
            .requestExtraLesson(payload)
            .then(() => ({ message: 'Đã gửi yêu cầu thêm buổi — chờ bên còn lại duyệt.' })),
        'Gửi yêu cầu thêm buổi thất bại.',
      ),
    decideRequest: (requestId: number, approve: boolean, note?: string) =>
      run(() => teachingApi.decideRequest(requestId, approve, note), 'Xử lý yêu cầu thất bại.'),
    cancelRequest: (requestId: number) =>
      run(() => teachingApi.cancelRequest(requestId), 'Thu hồi yêu cầu thất bại.'),
  };
}
