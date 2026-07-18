import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { teachingApi } from '../api/teachingApi';
import type { AssignmentResponse, LessonResponse } from '../types/teachingTypes';

type LoadStatus = 'loading' | 'success' | 'error';

function extractError(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  return fallback;
}

/**
 * Lịch của MỘT lớp. `/lessons/mine` đã giới hạn theo người đang đăng nhập
 * (gia sư → lớp mình dạy, Client → lớp mình tạo) nên chỉ cần lọc theo classId.
 */
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

/** Lời mời nhận lớp + lịch dạy + điểm danh của gia sư đang đăng nhập. */
export function useTeaching() {
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [assignments, setAssignments] = useState<AssignmentResponse[]>([]);
  const [lessons, setLessons] = useState<LessonResponse[]>([]);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setStatus('loading');
    try {
      const [a, l] = await Promise.all([
        teachingApi.listMyAssignments(),
        teachingApi.listMyLessons(),
      ]);
      setAssignments(a);
      setLessons(l);
      setStatus('success');
    } catch {
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  /** Bọc mọi thao tác: xoá thông báo cũ, gọi API, nạp lại dữ liệu. */
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
    notice,
    error,
    reload,
    accept: (id: number) => run(() => teachingApi.acceptAssignment(id), 'Nhận lớp thất bại.'),
    decline: (id: number) => run(() => teachingApi.declineAssignment(id), 'Từ chối lớp thất bại.'),
    attend: (id: number) => run(() => teachingApi.markAttendance(id), 'Điểm danh thất bại.'),
  };
}
