import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ClassBusyConflict } from '../types/marketplaceTypes';
import { useNavigate } from 'react-router-dom';
import { marketplaceApi } from '../api/marketplaceApi';
import { ClassDetailModal } from './ClassDetailModal';
import { ApplyClassModal } from './ApplyClassModal';
import { ClassResultCard } from './ClassResultCard';
import { useClassSearch } from '../hooks/useClassSearch';
import { APP_ROUTES } from '../../../shared/constants/routes';
import type { CatalogOption, ClassResponse } from '../types/marketplaceTypes';
import { searchClasses } from '../matching/tutorMatching';
import './tutorFindClass.css';

const PAGE_SIZE = 6;

interface Props {
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly provinces: CatalogOption[];
}

/**
 * Người điều phối của màn gia sư tìm lớp. Bản thân nó không chấm điểm và cũng không
 * dựng thanh tìm — hai việc đó giao cho tutorMatching.tsx và useClassSearch.tsx.
 * Việc của tệp này gói gọn: lấy dữ liệu về, ráp lại, phân trang, mở/đóng modal.
 */
export function TutorFindClass({ subjects, grades, provinces }: Props) {
  const navigate = useNavigate();
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [applied, setApplied] = useState<Set<number>>(new Set());
  const [notice, setNotice] = useState<string | null>(null);
  const [detailTarget, setDetailTarget] = useState<ClassResponse | null>(null);
  const [applyTarget, setApplyTarget] = useState<ClassResponse | null>(null);
  /** Mức trong hồ sơ — chỉ dùng làm gợi ý cho ô học phí, không phải giá trị mặc định. */
  const [profileFee, setProfileFee] = useState('');
  const [profileVerified, setProfileVerified] = useState<boolean | null>(null);
  const [page, setPage] = useState(1);
  const [busyConflicts, setBusyConflicts] = useState<Map<number, ClassBusyConflict>>(new Map());

  /**
   * Tải danh sách lớp. `silent = true` là lần tải lại ngầm: giữ nguyên danh sách cũ trên
   * màn, không hiện "Đang tải…" và lỗi cũng nuốt luôn — tránh làm màn nhấp nháy hoặc
   * đang xem thì bỗng thành trang lỗi.
   */
  const loadClasses = useCallback((silent = false) => {
    if (!silent) setStatus('loading');
    marketplaceApi
      .listOpenClasses()
      .then((data) => {
        // Lớp của trung tâm do trung tâm tự bố trí gia sư -> không hiện cho gia sư tìm/ứng tuyển.
        setClasses(data.filter((c) => c.classType !== 'CENTER'));
        setStatus('success');
      })
      .catch(() => {
        if (!silent) setStatus('error');
      });
  }, []);

  useEffect(() => {
    loadClasses();
  }, [loadClasses]);

  // Quay lại tab thì tải lại ngầm: gia sư thường mở tab khác xem hồ sơ rồi quay về,
  // lúc đó danh sách cần cập nhật (lớp mới đăng, lớp đã bị người khác nhận).
  useEffect(() => {
    const onFocus = () => loadClasses(true);
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [loadClasses]);

  // Các lớp gia sư này đã nộp đơn -> nút đổi thành "✓ Đã ứng tuyển" thay vì "Ứng tuyển".
  // Cờ `alive` chặn setState sau khi component đã bị gỡ (đổi trang giữa chừng).
  // Lỗi thì bỏ qua: coi như chưa nộp đơn lớp nào, màn vẫn dùng được bình thường.
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
        if (!alive) return;
        setProfileVerified(p.verificationStatus === 'VERIFIED');
        if (!p.hourlyRate) return;
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

  // Chỗ chấm điểm thật sự. searchClasses() chạy hoàn toàn trong trình duyệt: soi từng lớp
  // theo `criteria` rồi sắp giảm dần theo điểm. Phụ thuộc là [classes, criteria] nên chỉ
  // tính lại khi tải xong lớp mới hoặc khi gia sư bấm Tìm — gõ chữ trong ô không kích hoạt.
  const results = useMemo(() => searchClasses(classes, criteria), [classes, criteria]);

  // Phân trang: 6 lớp / trang (2 cột × 3 hàng); lớp thứ 7 nhảy sang trang 2.
  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount);
  const pageResults = results.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);
  // Kết quả đổi (tìm mới / lọc khác) -> quay về trang 1.
  useEffect(() => {
    setPage(1);
  }, [criteria]);

  // Đối chiếu các lớp đang hiện với thời gian bận gia sư đã đăng ký. Gọi RIÊNG và nuốt lỗi:
  // backend cũ chưa có API này (404) thì màn vẫn chạy bình thường, chỉ không có cảnh báo.
  const pageIdsKey = pageResults.map((r) => r.parsed.raw.classId).join(',');
  // Bỏ lịch bận ở tab khác rồi quay lại thì phải mở khoá nút ngay -> tính lại khi cửa sổ được focus.
  const [busyCheckTick, setBusyCheckTick] = useState(0);
  useEffect(() => {
    const onFocus = () => setBusyCheckTick((t) => t + 1);
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, []);
  useEffect(() => {
    if (!pageIdsKey) return;
    let alive = true;
    marketplaceApi
      .listMyBusyConflicts(pageIdsKey.split(',').map(Number))
      .then((rows) => {
        if (!alive) return;
        setBusyConflicts((prev) => {
          const next = new Map(prev);
          for (const id of pageIdsKey.split(',').map(Number)) next.delete(id);
          for (const row of rows) next.set(row.classId, row);
          return next;
        });
      })
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, [pageIdsKey, busyCheckTick]);

  const selectedNames = search.subjectNames;

  function openApply(target: ClassResponse) {
    setNotice(null);
    if (profileVerified === false) {
      goVerify('Bạn cần xác minh hồ sơ gia sư trước khi ứng tuyển lớp học.');
      return;
    }
    setApplyTarget(target);
    setDetailTarget(null);
  }

  function goVerify(message: string) {
    setApplyTarget(null);
    setDetailTarget(null);
    navigate(APP_ROUTES.verification, { state: { notice: message } });
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
              busyConflict={busyConflicts.get(r.parsed.raw.classId) ?? null}
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
                    disabled={applied.has(r.parsed.raw.classId) || busyConflicts.has(r.parsed.raw.classId)}
                    title={
                      !applied.has(r.parsed.raw.classId) && busyConflicts.has(r.parsed.raw.classId)
                        ? 'Trùng thời gian bận bạn đã đăng ký — bỏ lịch bận để ứng tuyển'
                        : undefined
                    }
                    onClick={() => openApply(r.parsed.raw)}
                  >
                    {applied.has(r.parsed.raw.classId)
                      ? '✓ Đã ứng tuyển'
                      : busyConflicts.has(r.parsed.raw.classId)
                        ? 'Trùng lịch bận'
                        : 'Ứng tuyển'}
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
          busyConflict={busyConflicts.get(detailTarget.classId) ?? null}
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
          onVerificationRequired={goVerify}
          busyConflict={busyConflicts.get(applyTarget.classId) ?? null}
        />
      )}
    </div>
  );
}
