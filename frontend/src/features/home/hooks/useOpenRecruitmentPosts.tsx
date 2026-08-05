import { useCallback, useEffect, useState } from 'react';
import { centerApi } from '../../center/api/centerApi';
import type { RecruitmentPost } from '../../center/types/centerTypes';

export type OpenRecruitmentStatus = 'loading' | 'success' | 'error';

/** Tin tuyển gia sư đang mở (ACTIVE) — hiển thị ở mục "Trung tâm" trên trang chủ. */
export function useOpenRecruitmentPosts(enabled: boolean) {
  const [status, setStatus] = useState<OpenRecruitmentStatus>('loading');
  const [posts, setPosts] = useState<RecruitmentPost[]>([]);

  const reload = useCallback(() => {
    if (!enabled) {
      return;
    }
    setStatus('loading');
    centerApi
      .getOpenPosts()
      .then((response) => {
        setPosts(response.data);
        setStatus('success');
      })
      .catch((error) => {
        console.error('Lỗi tải tin tuyển gia sư:', error);
        setStatus('error');
      });
  }, [enabled]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { status, posts, reload };
}
