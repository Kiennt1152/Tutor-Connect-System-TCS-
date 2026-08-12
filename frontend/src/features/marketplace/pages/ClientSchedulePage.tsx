import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import axiosClient from '../../../shared/api/axiosClient';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import type { LessonMode, ScheduleClass } from '../../center/types/centerTypes';
import '../../center/pages/CenterSchedulePage.css';
import '../../tutor/pages/TutorSchedulePage.css';

const LESSON_MODE_LABELS: Record<LessonMode, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};

const DOW_LABELS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'CN'];
const ATT_LABELS: Record<string, string> = {
  PRESENT: 'Có mặt',
  ABSENT: 'Vắng',
  EXCUSED: 'Có phép',
};

function toISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate(),
  ).padStart(2, '0')}`;
}
function addDays(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}
function startOfWeek(d: Date): Date {
  const r = new Date(d);
  const wd = (r.getDay() + 6) % 7; // 0 = Thứ 2
  r.setDate(r.getDate() - wd);
  r.setHours(0, 0, 0, 0);
  return r;
}
function ddmm(d: Date): string {
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
}

const TODAY_ISO = toISO(new Date());

export default function ClientSchedulePage() {
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const [week, setWeek] = useState<ScheduleClass[][]>([[], [], [], [], [], [], []]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');

  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

  const load = useCallback(() => {
    setStatus('loading');
    setError('');
    const dayList = Array.from({ length: 7 }, (_, i) => toISO(addDays(weekStart, i)));
    Promise.all(
      dayList.map((d) =>
        axiosClient.get<ScheduleClass[]>(`/marketplace/center-schedule?date=${d}`),
      ),
    )
      .then((res) => {
        setWeek(res.map((r) => r.data));
        setStatus('success');
      })
      .catch((err) => {
        setError(
          axios.isAxiosError(err) && typeof err.response?.data?.message === 'string'
            ? err.response.data.message
            : 'Không tải được lịch học.',
        );
        setStatus('error');
      });
  }, [weekStart]);

  useEffect(() => {
    load();
  }, [load]);

  const rangeLabel = `${ddmm(days[0])} — ${ddmm(days[6])}`;

  return (
    <>
      <VerificationHeader />
      <div className="cs-bg">
        <div className="cs-page">
          <div className="cs-topbar">
            <Link className="cs-back" to="/">
              ← Trang chủ
            </Link>
          </div>

          <header className="cs-header">
            <div>
              <h1 className="cs-title">Lịch học lớp trung tâm</h1>
              <p className="cs-subtitle">
                Các lớp của trung tâm mà bạn đã ghi danh — xem thời khóa biểu theo tuần.
              </p>
            </div>
          </header>

          <div className="tw-weekbar">
            <div className="tw-weeknav">
              <button
                className="tw-navbtn"
                type="button"
                onClick={() => setWeekStart((w) => addDays(w, -7))}
                aria-label="Tuần trước"
              >
                ←
              </button>
              <span className="tw-weekrange">{rangeLabel}</span>
              <button
                className="tw-navbtn"
                type="button"
                onClick={() => setWeekStart((w) => addDays(w, 7))}
                aria-label="Tuần sau"
              >
                →
              </button>
            </div>
            <button
              className="tw-today-btn"
              type="button"
              onClick={() => setWeekStart(startOfWeek(new Date()))}
            >
              Về tuần này
            </button>
          </div>

          {error && <div className="cs-alert cs-alert--error">{error}</div>}
          {status === 'loading' && <div className="cs-state">Đang tải lịch…</div>}

          {status === 'success' && (
            <div className="tw-grid">
              {days.map((day, i) => {
                const iso = toISO(day);
                const sessions = week[i] ?? [];
                return (
                  <div className="tw-col" key={iso}>
                    <div className={`tw-colhead${iso === TODAY_ISO ? ' tw-colhead--today' : ''}`}>
                      <span className="tw-dow">{DOW_LABELS[i]}</span>
                      <span className="tw-date">{ddmm(day)}</span>
                    </div>
                    <div className="tw-cells">
                      {sessions.length === 0 ? (
                        <div className="tw-empty">—</div>
                      ) : (
                        sessions.map((c) => (
                          <div
                            className={`tw-card${c.classCompleted ? ' tw-card--done' : ''}`}
                            key={c.classId}
                          >
                            <div className="tw-card__time">
                              {c.slots
                                .map((s) => `${s.startTime.slice(0, 5)}–${s.endTime.slice(0, 5)}`)
                                .join(', ')}
                            </div>
                            <div className="tw-card__title">{c.title}</div>
                            <div className="tw-card__chips">
                              {c.subjectName && <span className="tw-chip">{c.subjectName}</span>}
                              {c.gradeName && <span className="tw-chip">{c.gradeName}</span>}
                              <span className="tw-chip">{LESSON_MODE_LABELS[c.lessonMode]}</span>
                            </div>
                            {c.assignedTutorName && (
                              <div className="tw-card__meta">👩‍🏫 GV: {c.assignedTutorName}</div>
                            )}
                            {c.students && c.students.length > 0 && (
                              <div className="tw-card__meta">
                                {c.students
                                  .map(
                                    (s) =>
                                      `${s.studentName}${
                                        s.status ? ` — ${ATT_LABELS[s.status] ?? s.status}` : ''
                                      }`,
                                  )
                                  .join('; ')}
                              </div>
                            )}
                            {c.classCompleted && (
                              <div className="tw-card__meta">✓ Đã hoàn thành</div>
                            )}
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </>
  );
}
