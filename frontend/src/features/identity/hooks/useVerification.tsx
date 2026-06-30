import { useCallback, useEffect, useState } from 'react';
import { verificationApi } from '../api/verificationApi';
import type { Verification, VerificationDecision, VerificationSubmission } from '../types/verificationTypes';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

function extractErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null) {
    const maybeAxiosError = error as {
      response?: { data?: { message?: string } | Record<string, string> };
      message?: string;
    };

    const message = maybeAxiosError.response?.data;
    if (message && typeof message === 'object') {
      if ('message' in message && typeof message.message === 'string') {
        return message.message;
      }
      return Object.values(message).join(', ');
    }

    if (typeof maybeAxiosError.message === 'string') {
      return maybeAxiosError.message;
    }
  }

  return 'An error occurred. Please try again.';
}

export function useVerification(userId: number) {
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [verifications, setVerifications] = useState<Verification[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadVerifications = useCallback(async () => {
    setStatus('loading');
    setError(null);
    try {
      const data = await verificationApi.getVerificationsByUser(userId);
      setVerifications(data);
      setStatus('success');
    } catch (err) {
      setStatus('error');
      setError(extractErrorMessage(err));
    }
  }, [userId]);

  useEffect(() => {
    void loadVerifications();
  }, [loadVerifications]);

  async function submitVerification(payload: VerificationSubmission) {
    setIsSubmitting(true);
    setError(null);
    try {
      await verificationApi.submitVerification(userId, payload);
      await loadVerifications();
    } catch (err) {
      setError(extractErrorMessage(err));
      throw err;
    } finally {
      setIsSubmitting(false);
    }
  }

  async function reviewVerification(verificationId: number, adminId: number, decision: VerificationDecision) {
    setError(null);
    try {
      await verificationApi.reviewVerification(verificationId, adminId, decision);
      await loadVerifications();
    } catch (err) {
      setError(extractErrorMessage(err));
      throw err;
    }
  }

  return {
    status,
    verifications,
    error,
    isSubmitting,
    reload: loadVerifications,
    submitVerification,
    reviewVerification,
  };
}

export function useVerificationStatus(userId: number) {
  const [currentVerification, setCurrentVerification] = useState<Verification | null>(null);
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setStatus('loading');
    setError(null);
    try {
      const all = await verificationApi.getVerificationsByUser(userId);
      const latest = all[0] ?? null;
      setCurrentVerification(latest);
      setStatus('success');
    } catch (err) {
      setStatus('error');
      setError(extractErrorMessage(err));
    }
  }, [userId]);

  useEffect(() => {
    void load();
  }, [load]);

  return { status, currentVerification, error, reload: load };
}

export function useModerationQueue() {
  const [queue, setQueue] = useState<Verification[]>([]);
  const [status, setStatus] = useState<LoadStatus>('idle');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setStatus('loading');
    setError(null);
    try {
      const data = await verificationApi.getModerationQueue();
      setQueue(data);
      setStatus('success');
    } catch (err) {
      setStatus('error');
      setError(extractErrorMessage(err));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return { status, queue, error, reload: load };
}

export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes == null) return 'Unknown';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
