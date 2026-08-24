import { useCallback, useEffect, useMemo, useState } from 'react';
import { marketplaceApi } from '../api/marketplaceApi';
import { ClassDetailModal } from './ClassDetailModal';
import { ApplyClassModal } from './ApplyClassModal';
import { ClassResultCard } from './ClassResultCard';
import { useClassSearch } from '../hooks/useClassSearch';
import type { CatalogOption, ClassResponse } from '../types/marketplaceTypes';
import { searchClasses } from '../matching/tutorMatching';
import './tutorFindClass.css';

const PAGE_SIZE = 6;

interface Props {
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly provinces: CatalogOption[];
}

export function TutorFindClass({ subjects, grades, provinces }: Props) {
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [applied, setApplied] = useState<Set<number>>(new Set());
  const [notice, setNotice] = useState<string | null>(null);
  const [detailTarget, setDetailTarget] = useState<ClassResponse | null>(null);
  const [applyTarget, setApplyTarget] = useState<ClassResponse | null>(null);
  /** Mức trong hồ sơ — chỉ dùng làm gợi ý cho ô học phí, không phải giá trị mặc định. */
  const [profileFee, setProfileFee] = useState('');
  const [page, setPage] = useState(1);

  const loadClasses = useCallback((silent = false) => {
    if (!silent) setStatus('loading');
    marketplaceApi
      .listOpenClasses()
      .then((data) => {
        setClasses(data);
        setStatus('success');
      })
      .catch(() => {
        if (!silent) setStatus('error');
      });
  }, []);

  useEffect(() => {
    loadClasses();
  }, [loadClasses]);

  useEffect(() => {
    const onFocus = () => loadClasses(true);
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [loadClasses]);

  useEffect(() => {
    let alive = true;
    marketplaceApi
      .listMyAppliedClassIds()
      .then((ids) => alive && setApplied(new Set(ids)))
      .catch(() => {
      });
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    let alive = true;
    marketplaceApi
      .getMyTutorProfile()
      .then((p) => {
        if (!alive || !p.hourlyRate) return;
        // Chỉ GỢI Ý mức trong hồ sơ (đổ vào placeholder), KHÔNG tự điền thành giá trị:
        // tự điền thì tiêu chí P âm thầm trừ điểm dù gia sư chưa hề khai mức nào.
        setProfileFee(String(Math.round(Number(p.hourlyRate))));
      })
      .catch(() => {
      });
    return () => {
      alive = false;
    };
  }, []);

  // Thanh tìm + 5 ô lọc + 5 thanh trượt: dùng chung với màn phụ huynh tìm lớp.
  const search = useClassSearch({ subjects, grades, provinces, classes });
  const { criteria, hasFilter, subjectName, gradeName } = search;

  const results = useMemo(() => searchClasses(classes, criteria), [classes, criteria]);

  // Phân trang: 6 lớp / trang (2 cột × 3 hàng); lớp thứ 7 nhảy sang trang 2.
  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount);
  const pageResults = results.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);
  // Kết quả đổi (tìm mới / lọc khác) -> quay về trang 1.
  useEffect(() => {
    setPage(1);
  }, [criteria]);

  const selectedNames = search.subjectNames;

  function openApply(target: ClassResponse) {
    setNotice(null);
    setApplyTarget(target);
    setDetailTarget(null);
  }

  function handleApplied(classId: number) {
    setApplied((s) => new Set(s).add(classId));
    setNotice('Đã gửi đơn ứng tuyển thành công.');
    setApplyTarget(null);
  }

  return (
    <div className="tfc">
      {search.bar}


      <section className="tfc-results" id="tfc-results">
        <header className="tfc-results__head">
          <h2>{hasFilter ? 'Yêu cầu phù hợp với bạn' : 'Tất cả tin tìm gia sư đã đăng'}</h2>
          <span className="tfc-results__count">
            {status === 'success'
              ? hasFilter && search.selectedCount > 0
                ? `${results.length} lớp cần: ${selectedNames}`
                : `${results.length} lớp đang mở`
              : ''}
          </span>
        </header>

        {notice && <div className="tfc-notice">{notice}</div>}

        {status === 'loading' && <div className="tfc-state">Đang tải danh sách lớp…</div>}
        {status === 'error' && (
          <div className="tfc-state tfc-state--error">Không tải được danh sách lớp.</div>
        )}
        {status === 'success' && results.length === 0 && (
          <div className="tfc-state">
            {hasFilter && search.selectedCount > 0
              ? `Chưa có lớp nào đang cần: ${selectedNames}. Thử môn khác nhé.`
              : 'Chưa có lớp nào đang mở đơn ứng tuyển.'}
          </div>
        )}

        <div className="tfc-list">
          {pageResults.map((r) => (
            <ClassResultCard
              key={r.parsed.raw.classId}
              result={r}
              subjectName={subjectName}
              gradeName={gradeName}
              showScore={hasFilter}
              actions={
                <>
                  <button
                    type="button"
                    className="tfc-btn tfc-btn--ghost"
                    onClick={() => setDetailTarget(r.parsed.raw)}
                  >
                    Xem chi tiết
                  </button>
                  <button
                    type="button"
                    className="tfc-btn tfc-btn--primary"
                    disabled={applied.has(r.parsed.raw.classId)}
                    onClick={() => openApply(r.parsed.raw)}
                  >
                    {applied.has(r.parsed.raw.classId) ? '✓ Đã ứng tuyển' : 'Ứng tuyển'}
                  </button>
                </>
              }
            />
          ))}
        </div>

        {pageCount > 1 && (
          <nav className="tfc-pager" aria-label="Phân trang danh sách lớp">
            <button
              type="button"
              className="tfc-pager__btn"
              disabled={safePage <= 1}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
            >
              ← Trước
            </button>
            {Array.from({ length: pageCount }, (_, i) => i + 1).map((n) => (
              <button
                key={n}
                type="button"
                className={`tfc-pager__num${n === safePage ? ' is-active' : ''}`}
                aria-current={n === safePage ? 'page' : undefined}
                onClick={() => setPage(n)}
              >
                {n}
              </button>
            ))}
            <button
              type="button"
              className="tfc-pager__btn"
              disabled={safePage >= pageCount}
              onClick={() => setPage((p) => Math.min(pageCount, p + 1))}
            >
              Sau →
            </button>
          </nav>
        )}
      </section>

      {detailTarget && (
        <ClassDetailModal
          raw={detailTarget}
          subjects={search.subjects}
          grades={search.grades}
          applied={applied.has(detailTarget.classId)}
          onApply={() => openApply(detailTarget)}
          onClose={() => setDetailTarget(null)}
        />
      )}

      {applyTarget && (
        <ApplyClassModal
          target={applyTarget}
          subjects={search.subjects}
          defaultRate={Number(search.fee || profileFee) || undefined}
          onClose={() => setApplyTarget(null)}
          onSubmitted={handleApplied}
        />
      )}
    </div>
  );
}
