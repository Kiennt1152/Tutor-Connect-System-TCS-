import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { tutorApi } from '../api/tutorApi';
import type { LessonMode, ScheduleClass } from '../../center/types/centerTypes';
import '../../center/pages/CenterSchedulePage.css';

const LESSON_MODE_LABELS: Record<LessonMode, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
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

export default function TutorSchedulePage() {
  const navigate = useNavigate();
  const [date, setDate] = useState(todayStr());
  const [classes, setClasses] = useState<ScheduleClass[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setStatus('loading');
    setError('');
    tutorApi
      .getSchedule(date)
      .then((res) => {
        setClasses(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được lịch dạy.'));
        setStatus('error');
      });
  }, [date]);

  useEffect(() => {
    load();
  }, [load]);

  const openAttendance = (classId: number) =>
    navigate(`/tutor/classes/${classId}/attendance?date=${date}`);

  return (
    <>
      <VerificationHeader />
      <div className="cs-page">
        <div className="cs-topbar">
          <Link className="cs-back" to="/">
            ← Trang chủ
          </Link>
        </div>

        <header className="cs-header">
          <div>
            <h1 className="cs-title">Lịch dạy của tôi</h1>
            <p className="cs-subtitle">Các lớp bạn phụ trách có buổi học. Bấm “Điểm danh” để điểm danh buổi đó.</p>
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
            <p>Ngày này bạn không có lớp nào phụ trách.</p>
          </div>
        )}

        {status === 'success' && classes.length > 0 && (
          <div className="cs-list">
            {classes.map((c) => (
              <article className="cs-card cs-card--row" key={c.classId}>
                <div className="cs-card__main">
                  <h2 className="cs-card__title">{c.title}</h2>
                  <div className="cs-chips">
                    {c.subjectName && <span className="cs-chip">{c.subjectName}</span>}
                    {c.gradeName && <span className="cs-chip">{c.gradeName}</span>}
                    <span className="cs-chip">{LESSON_MODE_LABELS[c.lessonMode]}</span>
                  </div>
                  <div className="cs-rowmeta">
                    {c.slots.map((s) => (
                      <span className="cs-time" key={s.slotId}>
                        🕒 {s.startTime.slice(0, 5)}–{s.endTime.slice(0, 5)}
                      </span>
                    ))}
                    <span className="cs-rowmeta__students">👥 {c.studentCount} học sinh</span>
                  </div>
                </div>
                <button
                  className={`cs-btn ${c.attendanceTaken ? 'cs-btn--done' : 'cs-btn--primary'}`}
                  type="button"
                  onClick={() => openAttendance(c.classId)}
                >
                  {c.attendanceTaken ? '✓ Đã điểm danh' : 'Điểm danh'}
                </button>
              </article>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
