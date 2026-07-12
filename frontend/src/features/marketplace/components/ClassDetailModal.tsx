import { useMemo } from 'react';
import {
  DAY_OF_WEEK_OPTIONS,
  LESSON_MODE_OPTIONS,
  OTHER_SUBJECT,
  type CatalogOption,
  type ClassFormValues,
  type ClassResponse,
} from '../types/marketplaceTypes';
import {
  classToForm,
  cycleLabelOf,
  estimatedSessions,
  hoursPerWeekForSubject,
  resolveLearningGoal,
  resolveTutorRequirement,
  totalBudget,
  weekdayVi,
} from '../mappers/marketplaceMapper';

const currency = new Intl.NumberFormat('vi-VN');

interface Props {
  readonly raw: ClassResponse;
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly applied: boolean;
  readonly applying: boolean;
  readonly onApply: () => void;
  readonly onClose: () => void;
}

export function ClassDetailModal({
  raw,
  subjects,
  grades,
  applied,
  applying,
  onApply,
  onClose,
}: Props) {
  // Bóc dữ liệu đầy đủ từ detailsJson (fallback cột phẳng nếu lớp cũ không có JSON).
  const form: ClassFormValues = useMemo(() => classToForm(raw), [raw]);
  const hasDetails = !!raw.detailsJson;

  const subjectName = useMemo(() => {
    const m = new Map(subjects.map((s) => [String(s.id), s.name]));
    return (id: string) =>
      id === OTHER_SUBJECT ? form.subjectOther.trim() || 'Môn khác' : (m.get(id) ?? `#${id}`);
  }, [subjects, form.subjectOther]);
  const gradeName = useMemo(() => {
    const m = new Map(grades.map((g) => [String(g.id), g.name]));
    return m.get(form.gradeId) ?? raw.gradeName ?? '—';
  }, [grades, form.gradeId, raw.gradeName]);

  const dayLabel = (v: string) => DAY_OF_WEEK_OPTIONS.find((d) => d.value === v)?.label ?? v;
  const whenOf = (s: ClassFormValues['slots'][number]) =>
    form.scheduleMode === 'WEEKLY'
      ? `${dayLabel(s.day)} hàng tuần`
      : `${weekdayVi(s.date)} ${s.date}`;

  const learningGoal = resolveLearningGoal(form);
  const tutorRequirement = resolveTutorRequirement(form);
  const lessonModeLabel =
    LESSON_MODE_OPTIONS.find((o) => o.value === form.lessonMode)?.label ?? form.lessonMode;
  const fullAddress = [form.address, form.wardName, form.districtName, form.provinceName]
    .map((s) => s.trim())
    .filter(Boolean)
    .join(', ');
  const budget = totalBudget(form);
  const sessions = estimatedSessions(form);

  return (
    <div className="cdm-overlay" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="cdm" onClick={(e) => e.stopPropagation()}>
        <button type="button" className="cdm__close" aria-label="Đóng" onClick={onClose}>
          ✕
        </button>

        <header className="cdm__head">
          <span className="cdm__id">Lớp #{raw.classId}</span>
          <h2 className="cdm__title">{raw.title}</h2>
        </header>

        <div className="cdm__body">
          {/* Thông tin chung */}
          <dl className="cdm-grid">
            <div>
              <dt>Khối lớp</dt>
              <dd>{gradeName}</dd>
            </div>
            <div>
              <dt>Hình thức</dt>
              <dd>{lessonModeLabel}</dd>
            </div>
            <div>
              <dt>Mục tiêu học tập</dt>
              <dd>{learningGoal || '—'}</dd>
            </div>
            <div>
              <dt>Yêu cầu gia sư</dt>
              <dd>{tutorRequirement || '—'}</dd>
            </div>
            <div className="cdm-grid__full">
              <dt>Địa điểm</dt>
              <dd>{form.lessonMode === 'ONLINE' ? 'Học Online' : fullAddress || raw.address || '—'}</dd>
            </div>
          </dl>

          {/* Môn học + học phí/giờ từng môn */}
          <section className="cdm-section">
            <h3>Môn học &amp; học phí</h3>
            {hasDetails && form.subjectIds.length > 0 ? (
              <ul className="cdm-subj-list">
                {form.subjectIds.map((sid) => {
                  const fee = Number(form.subjectFees[sid]) || 0;
                  const hrs = hoursPerWeekForSubject(form, sid);
                  return (
                    <li key={sid}>
                      <span className="cdm-subj-list__name">📚 {subjectName(sid)}</span>
                      <span className="cdm-subj-list__fee">
                        {fee > 0 ? `${currency.format(fee)}đ/giờ` : 'Chưa nêu học phí'}
                        {hrs > 0 && <em> · {hrs} giờ/tuần</em>}
                      </span>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <p className="cdm-muted">
                📚 {raw.subjectName ?? '—'}
                {raw.tuitionFee != null && ` · ${currency.format(raw.tuitionFee)}đ/giờ`}
              </p>
            )}
          </section>

          {/* Lịch học chi tiết theo môn */}
          {hasDetails && form.slots.length > 0 && (
            <section className="cdm-section">
              <h3>
                Lịch học <span className="cdm-muted">({cycleLabelOf(form)})</span>
              </h3>
              <div className="cdm-schedule">
                {form.subjectIds.map((sid) => {
                  const rows = form.slots.filter((s) => s.subjectId === sid);
                  if (rows.length === 0) return null;
                  return (
                    <div key={sid} className="cdm-schedule__group">
                      <div className="cdm-schedule__subj">{subjectName(sid)}</div>
                      <ul>
                        {rows.map((s, i) => (
                          <li key={i}>
                            🗓️ {whenOf(s)} — <strong>{s.session}</strong> ({s.start}–{s.end})
                          </li>
                        ))}
                      </ul>
                    </div>
                  );
                })}
              </div>
            </section>
          )}

          {/* Ghi chú */}
          {form.note.trim() && (
            <section className="cdm-section">
              <h3>Ghi chú thêm</h3>
              <p className="cdm-note">{form.note.trim()}</p>
            </section>
          )}

          {/* Tổng học phí ước tính */}
          {hasDetails && budget > 0 && (
            <div className="cdm-budget">
              <span>Tổng học phí ước tính ({cycleLabelOf(form)})</span>
              <strong>{currency.format(budget)}đ</strong>
              <small>{sessions} buổi</small>
            </div>
          )}

          {/* Lớp cũ không có snapshot → hiện mô tả gốc */}
          {!hasDetails && raw.description && (
            <section className="cdm-section">
              <h3>Mô tả</h3>
              <p className="cdm-note" style={{ whiteSpace: 'pre-line' }}>
                {raw.description}
              </p>
            </section>
          )}
        </div>

        <footer className="cdm__foot">
          <button type="button" className="tfc-btn tfc-btn--ghost" onClick={onClose}>
            Đóng
          </button>
          <button
            type="button"
            className="tfc-btn tfc-btn--primary"
            disabled={applied || applying}
            onClick={onApply}
          >
            {applied ? '✓ Đã ứng tuyển' : applying ? 'Đang gửi…' : 'Ứng tuyển nhận lớp'}
          </button>
        </footer>
      </div>
    </div>
  );
}
