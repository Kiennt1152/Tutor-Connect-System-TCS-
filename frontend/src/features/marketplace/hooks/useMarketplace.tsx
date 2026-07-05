import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { marketplaceApi } from '../api/marketplaceApi';
import type {
  ClassSummary,
  TutorApplication,
  TutorApplicationReviewRequest,
  TutorApplicationStatus,
} from '../types/marketplaceTypes';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Record<string, string> | undefined;
    if (data) {
      return data.message ?? Object.values(data)[0] ?? fallback;
    }
  }
  if (error instanceof Error) return error.message;
  return fallback;
}

export function useMarketplace() {
  const { user } = useAuth();
  const role = user?.role;

  // ---- Tutor view state ----
  const [myApplications, setMyApplications] = useState<TutorApplication[]>([]);
  const [myStatus, setMyStatus] = useState<LoadStatus>('idle');
  const [myError, setMyError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<TutorApplicationStatus | 'ALL'>('ALL');

  // ---- Client view state ----
  const [myClasses, setMyClasses] = useState<ClassSummary[]>([]);
  const [classesStatus, setClassesStatus] = useState<LoadStatus>('idle');
  const [classesError, setClassesError] = useState<string | null>(null);
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [classApplications, setClassApplications] = useState<TutorApplication[]>([]);
  const [classApplicationsStatus, setClassApplicationsStatus] =
    useState<LoadStatus>('idle');
  const [classApplicationsError, setClassApplicationsError] = useState<string | null>(null);

  // ---- Mutating state ----
  const [mutatingId, setMutatingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const reloadMyApplications = useCallback(async () => {
    setMyStatus('loading');
    setMyError(null);
    try {
      const data = await marketplaceApi.listMyApplications();
      setMyApplications(data);
      setMyStatus('success');
    } catch (err) {
      setMyError(extractError(err, 'Không thể tải danh sách đơn ứng tuyển'));
      setMyStatus('error');
    }
  }, []);

  const reloadMyClasses = useCallback(async () => {
    setClassesStatus('loading');
    setClassesError(null);
    try {
      // Backend returns full ClassResponse; we only need a few fields.
      const data = (await marketplaceApi.listClasses()) as unknown as ClassSummary[];
      setMyClasses(data);
      setClassesStatus('success');
    } catch (err) {
      setClassesError(extractError(err, 'Không thể tải danh sách lớp'));
      setClassesStatus('error');
    }
  }, []);

  const reloadClassApplications = useCallback(async (classId: number) => {
    setClassApplicationsStatus('loading');
    setClassApplicationsError(null);
    try {
      const data = await marketplaceApi.listApplicationsByClass(classId);
      setClassApplications(data);
      setClassApplicationsStatus('success');
    } catch (err) {
      setClassApplicationsError(
        extractError(err, 'Không thể tải danh sách ứng tuyển'),
      );
      setClassApplicationsStatus('error');
    }
  }, []);

  // Initial loads per role
  useEffect(() => {
    if (role === 'TUTOR') {
      void reloadMyApplications();
    } else if (role === 'CLIENT') {
      void reloadMyClasses();
    }
  }, [role, reloadMyApplications, reloadMyClasses]);

  // When user picks a class in client view, load its applications
  useEffect(() => {
    if (selectedClassId != null) {
      void reloadClassApplications(selectedClassId);
    }
  }, [selectedClassId, reloadClassApplications]);

  // ---- Mutations ----

  async function withdrawApplication(applicationId: number) {
    setMutatingId(applicationId);
    setActionError(null);
    try {
      await marketplaceApi.withdrawApplication(applicationId);
      if (role === 'TUTOR') {
        await reloadMyApplications();
      } else if (selectedClassId != null) {
        await reloadClassApplications(selectedClassId);
      }
    } catch (err) {
      setActionError(extractError(err, 'Không thể rút đơn'));
      throw err;
    } finally {
      setMutatingId(null);
    }
  }

  async function reviewApplication(
    applicationId: number,
    payload: TutorApplicationReviewRequest,
  ) {
    setMutatingId(applicationId);
    setActionError(null);
    try {
      await marketplaceApi.reviewApplication(applicationId, payload);
      if (selectedClassId != null) {
        await reloadClassApplications(selectedClassId);
      }
      await reloadMyClasses();
    } catch (err) {
      setActionError(extractError(err, 'Không thể duyệt đơn'));
      throw err;
    } finally {
      setMutatingId(null);
    }
  }

  const filteredMyApplications =
    statusFilter === 'ALL'
      ? myApplications
      : myApplications.filter((a) => a.status === statusFilter);

  return {
    role,

    // Tutor
    myApplications,
    filteredMyApplications,
    myStatus,
    myError,
    statusFilter,
    setStatusFilter,
    reloadMyApplications,

    // Client
    myClasses,
    classesStatus,
    classesError,
    selectedClassId,
    setSelectedClassId,
    classApplications,
    classApplicationsStatus,
    classApplicationsError,
    reloadClassApplications,
    reloadMyClasses,

    // Mutations
    mutatingId,
    actionError,
    clearActionError: () => setActionError(null),
    withdrawApplication,
    reviewApplication,
  };
}