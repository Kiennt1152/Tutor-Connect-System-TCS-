import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { centerApi } from '../api/centerApi';
import type { LessonMode, ScheduleClass } from '../types/centerTypes';
import './CenterSchedulePage.css';

const LESSON_MODE_LABELS: Record<LessonMode, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};

const ATT_LABELS: Record<string, { label: string; cls: string }> = {
  PRESENT: { label: 'Có mặt', cls: 'present' },
  ABSENT: { label: 'Vắng', cls: 'absent' },
  EXCUSED: { label: 'Có phép', cls: 'excused' },
};

function todayStr(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

export default function CenterSchedulePage() {
  const [date, setDate] = useState(todayStr());
  const [classes, setClasses] = useState<ScheduleClass[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [expanded, setExpanded] = useState<Record<number, boolean>>({});

  const load = useCallback(() => {
    setStatus('loading');
    setError('');
    centerApi
      .getSchedule(date)
      .then((res) => {
        setClasses(res.data);
        setExpanded({});
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được lịch lớp.'));
        setStatus('error');
      });
  }, [date]);

  useEffect(() => {
    load();
  }, [load]);

  const toggle = (classId: number) =>
    setExpanded((prev) => ({ ...prev, [classId]: !prev[classId] }));

  return (
    <>
      <VerificationHeader />
      <div className="cs-bg">
      <div className="cs-page">
        <div className="cs-topbar">
          <Link className="cs-back" to="/center">
            ← Lớp học của tôi
          </Link>
        </div>

        <header className="cs-header">
          <div>
            <h1 className="cs-title">Lịch lớp hôm nay</h1>
            <p className="cs-subtitle">
              Theo dõi lớp học, gia sư phụ trách và trạng thái điểm danh. Điểm danh do gia sư phụ
              trách thực hiện.
            </p>
          </div>
          <label className="cs-datepick">
            <span>Ngày</span>
            <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </label>
        </header>

        {error && <div className="cs-alert cs-alert--error">{error}</div>}
        {status === 'loading' && <div className="cs-state">Đang tải lịch…</div>}
        {status === 'success' && classes.length === 0 && (
          <div className="cs-empty">
            <div className="cs-empty__emoji">🗓️</div>
            <p>Ngày này không có lớp nào học.</p>
          </div>
        )}

        {status === 'success' && classes.length > 0 && (
          <div className="cs-list">
            {classes.map((c) => {
              const isOpen = !!expanded[c.classId];
              const presentCount = c.students.filter((s) => s.status === 'PRESENT').length;
              return (
                <article className={`cs-card${isOpen ? ' is-open' : ''}`} key={c.classId}>
                  <div className="cs-card__head">
                    <div className="cs-card__headmain">
                      <h2 className="cs-card__title">{c.title}</h2>
                      <div className="cs-chips">
                        {c.subjectName && <span className="cs-chip">{c.subjectName}</span>}
                        {c.gradeName && <span className="cs-chip">{c.gradeName}</span>}
                        <span className="cs-chip">{LESSON_MODE_LABELS[c.lessonMode]}</span>
                        {c.rescheduled && (
                          <span className="cs-chip cs-chip--resched">🔄 {c.rescheduleNote}</span>
                        )}
                      </div>
                    </div>
                    <div className="cs-times">
                      {c.slots.map((s) => (
                        <span className="cs-time" key={s.slotId}>
                          🕒 {s.startTime.slice(0, 5)}–{s.endTime.slice(0, 5)}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="cs-meta">
                    <div className="cs-meta__item">
                      <span className="cs-meta__label">Gia sư phụ trách</span>
                      {c.assignedTutorName ? (
                        <span className="cs-meta__value">{c.assignedTutorName}</span>
                      ) : (
                        <span className="cs-meta__value cs-meta__value--none">Chưa gán gia sư</span>
                      )}
                    </div>
                    <div className="cs-meta__item">
                      <span className="cs-meta__label">Điểm danh</span>
                      <span
                        className={`cs-attstate ${c.attendanceTaken ? 'cs-attstate--done' : 'cs-attstate--pending'}`}
                      >
                        {c.attendanceTaken ? '✓ Đã điểm danh' : 'Chưa điểm danh'}
                      </span>
                    </div>
                    <div className="cs-meta__item">
                      <span className="cs-meta__label">Sĩ số</span>
                      <span className="cs-meta__value">{c.studentCount} học sinh</span>
                    </div>
                  </div>

                  <button
                    type="button"
                    className="cs-toggle"
                    onClick={() => toggle(c.classId)}
                    aria-expanded={isOpen}
                  >
                    <span>
                      👥 Danh sách học sinh & điểm danh
                      {c.attendanceTaken && c.students.length > 0 && (
                        <span className="cs-toggle__count">
                          {presentCount}/{c.students.length} có mặt
                        </span>
                      )}
                    </span>
                    <span className={`cs-toggle__chev${isOpen ? ' is-open' : ''}`}>▾</span>
                  </button>

                  {isOpen && (
                    <div className="cs-students">
                      {c.students.length === 0 ? (
                        <p className="cs-muted">Chưa có học sinh ghi danh.</p>
                      ) : (
                        <ul className="cs-roster">
                          {c.students.map((st) => {
                            const att = st.status ? ATT_LABELS[st.status] : null;
                            return (
                              <li className="cs-student" key={st.classStudentId}>
                                <div className="cs-student__info">
                                  <span className="cs-student__name">{st.studentName}</span>
                                  {st.studentPhone && (
                                    <span className="cs-student__phone">{st.studentPhone}</span>
                                  )}
                                </div>
                                {att ? (
                                  <span className={`cs-attbadge cs-attbadge--${att.cls}`}>
                                    {att.label}
                                  </span>
                                ) : (
                                  <span className="cs-attbadge cs-attbadge--none">
                                    Chưa điểm danh
                                  </span>
                                )}
                              </li>
                            );
                          })}
                        </ul>
                      )}
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </div>
      </div>
    </>
  );
}
