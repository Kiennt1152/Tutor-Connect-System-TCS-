import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { tutorApi } from '../api/tutorApi';
import type { LessonMode, ScheduleClass } from '../../center/types/centerTypes';
import '../../center/pages/CenterSchedulePage.css';
import './TutorSchedulePage.css';

const LESSON_MODE_LABELS: Record<LessonMode, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};

const DOW_LABELS = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];

function toISO(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}
function startOfWeek(d: Date): Date {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  const dow = (x.getDay() + 6) % 7; // 0 = Thứ Hai
  x.setDate(x.getDate() - dow);
  return x;
}
function addDays(d: Date, n: number): Date {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
}
function ddmm(d: Date): string {
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

const TODAY_ISO = toISO(new Date());

export default function TutorSchedulePage() {
  const navigate = useNavigate();
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  // 7 mảng lịch, tương ứng Thứ Hai..Chủ Nhật.
  const [week, setWeek] = useState<ScheduleClass[][]>([[], [], [], [], [], [], []]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');

  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

  const load = useCallback(() => {
    setStatus('loading');
    setError('');
    const dayList = Array.from({ length: 7 }, (_, i) => toISO(addDays(weekStart, i)));
    Promise.all(dayList.map((d) => tutorApi.getSchedule(d)))
      .then((results) => {
        setWeek(results.map((r) => r.data));
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được lịch dạy.'));
        setStatus('error');
      });
  }, [weekStart]);

  useEffect(() => {
    load();
  }, [load]);

  const openAttendance = (classId: number, dateIso: string) =>
    navigate(`/tutor/classes/${classId}/attendance?date=${dateIso}`);

  // Đổi lịch (báo ốm)
  const [reschedFor, setReschedFor] = useState<{ cls: ScheduleClass; date: string } | null>(null);
  const [newDate, setNewDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [reason, setReason] = useState('');
  const [reschedBusy, setReschedBusy] = useState(false);
  const [reschedError, setReschedError] = useState('');
  const [reschedOk, setReschedOk] = useState('');

  const openResched = (cls: ScheduleClass, date: string) => {
    setReschedFor({ cls, date });
    setNewDate('');
    // Điền sẵn khung giờ theo buổi hiện tại cho tiện.
    const first = cls.slots[0];
    setStartTime(first ? first.startTime.slice(0, 5) : '');
    setEndTime(first ? first.endTime.slice(0, 5) : '');
    setReason('');
    setReschedError('');
    setReschedOk('');
  };
  const closeResched = () => setReschedFor(null);

  const submitResched = async () => {
    if (!reschedFor) return;
    if (!newDate) {
      setReschedError('Vui lòng chọn ngày dạy mới.');
      return;
    }
    if (!startTime || !endTime || endTime <= startTime) {
      setReschedError('Khung giờ mới không hợp lệ (giờ kết thúc phải sau giờ bắt đầu).');
      return;
    }
    setReschedBusy(true);
    setReschedError('');
    try {
      await tutorApi.requestReschedule(reschedFor.cls.classId, {
        originalDate: reschedFor.date,
        newDate,
        newStartTime: startTime,
        newEndTime: endTime,
        reason: reason.trim(),
      });
      setReschedOk('Đã gửi yêu cầu đổi lịch, chờ trung tâm duyệt.');
      setReschedFor(null);
      load();
    } catch (err) {
      setReschedError(extractError(err, 'Không gửi được yêu cầu đổi lịch.'));
    } finally {
      setReschedBusy(false);
    }
  };

  // Báo bận/ốm: chọn giữa "đổi lịch" và "nhờ gia sư phụ dạy thay".
  const [sickFor, setSickFor] = useState<{ cls: ScheduleClass; date: string } | null>(null);
  const openSick = (cls: ScheduleClass, date: string) => setSickFor({ cls, date });
  const closeSick = () => setSickFor(null);

  // Nhờ gia sư phụ dạy thay
  const [subsFor, setSubsFor] = useState<{ cls: ScheduleClass; date: string } | null>(null);
  const [subReason, setSubReason] = useState('');
  const [subBusy, setSubBusy] = useState(false);
  const [subError, setSubError] = useState('');

  const openSubstitute = (cls: ScheduleClass, date: string) => {
    setSickFor(null);
    setSubsFor({ cls, date });
    setSubReason('');
    setSubError('');
  };
  const closeSubstitute = () => setSubsFor(null);

  const submitSubstitute = async () => {
    if (!subsFor) return;
    setSubBusy(true);
    setSubError('');
    try {
      await tutorApi.requestSubstitute(subsFor.cls.classId, {
        date: subsFor.date,
        reason: subReason.trim(),
      });
      setReschedOk('Đã gửi yêu cầu nhờ gia sư phụ dạy thay, chờ trung tâm duyệt.');
      setSubsFor(null);
      load();
    } catch (err) {
      setSubError(extractError(err, 'Không gửi được yêu cầu dạy thay.'));
    } finally {
      setSubBusy(false);
    }
  };

  const rangeLabel = `${ddmm(days[0])} – ${ddmm(days[6])}`;

  return (
    <>
      <VerificationHeader />
      <div className="tw-page">
        <div className="tw-topbar">
          <Link className="cs-back" to="/">
            ← Trang chủ
          </Link>
        </div>

        <header className="cs-header">
          <div>
            <h1 className="cs-title">Lịch dạy của tôi</h1>
            <p className="cs-subtitle">
              Lịch dạy trong tuần theo từng thứ. Bấm “Điểm danh”, hoặc 🤒 khi bận/ốm để xin đổi
              lịch hoặc nhờ gia sư phụ dạy thay.
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
        {reschedOk && <div className="cs-alert cs-alert--ok">{reschedOk}</div>}
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
                          className={`tw-card${c.attendanceTaken ? ' tw-card--done' : ''}`}
                          key={`${c.classId}-${c.rescheduled ? 'r' : 'n'}`}
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
                            {c.rescheduled && (
                              <span className="tw-chip tw-chip--resched">🔄 {c.rescheduleNote}</span>
                            )}
                            {c.substituted && !c.handedOff && (
                              <span className="tw-chip tw-chip--resched">🔁 {c.substituteNote}</span>
                            )}
                          </div>
                          <div className="tw-card__meta">👥 {c.studentCount} học sinh</div>
                          {c.handedOff ? (
                            <div className="tw-card__handed">🔁 {c.substituteNote}</div>
                          ) : (
                            <div className="tw-card__actions">
                              <button
                                className={`tw-btn${c.attendanceTaken ? ' tw-btn--done' : ''}`}
                                type="button"
                                onClick={() => openAttendance(c.classId, iso)}
                              >
                                {c.attendanceTaken ? '✓ Đã điểm danh' : 'Điểm danh'}
                              </button>
                              <button
                                className="tw-btn tw-btn--icon"
                                type="button"
                                title="Báo bận/ốm buổi này"
                                onClick={() => openSick(c, iso)}
                              >
                                🤒
                              </button>
                            </div>
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

      {sickFor && (
        <div className="cs-modal" role="dialog" aria-modal="true">
          <div className="cs-modal__backdrop" onClick={closeSick} />
          <div className="cs-modal__card">
            <h2 className="cs-modal__title">Báo bận/ốm buổi này</h2>
            <p className="cs-modal__sub">
              Lớp: <b>{sickFor.cls.title}</b> — buổi ngày {sickFor.date}
            </p>
            <p className="cs-modal__sub">Bạn muốn xử lý buổi này thế nào?</p>
            <div className="tw-choice">
              <button
                className="tw-choice__btn"
                type="button"
                onClick={() => {
                  const s = sickFor;
                  closeSick();
                  openResched(s.cls, s.date);
                }}
              >
                <span className="tw-choice__emoji">🗓️</span>
                <span className="tw-choice__title">Xin đổi lịch (dạy bù ngày khác)</span>
                <span className="tw-choice__desc">
                  Dời buổi này sang một ngày khác để dạy bù.
                </span>
              </button>
              <button
                className="tw-choice__btn"
                type="button"
                disabled={!sickFor.cls.assistantTutorName || !!sickFor.cls.rescheduled}
                onClick={() => openSubstitute(sickFor.cls, sickFor.date)}
              >
                <span className="tw-choice__emoji">🔁</span>
                <span className="tw-choice__title">Nhờ gia sư phụ dạy thay</span>
                <span className="tw-choice__desc">
                  {sickFor.cls.rescheduled
                    ? 'Buổi này đã được dời lịch nên không thể nhờ dạy thay.'
                    : sickFor.cls.assistantTutorName
                      ? `Giữ nguyên lịch, ${sickFor.cls.assistantTutorName} dạy thay buổi này.`
                      : 'Lớp chưa có gia sư phụ. Đề nghị trung tâm gán trước.'}
                </span>
              </button>
            </div>
            <div className="cs-modal__actions">
              <button className="cs-btn cs-btn--ghost" type="button" onClick={closeSick}>
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {subsFor && (
        <div className="cs-modal" role="dialog" aria-modal="true">
          <div className="cs-modal__backdrop" onClick={closeSubstitute} />
          <div className="cs-modal__card">
            <h2 className="cs-modal__title">Nhờ gia sư phụ dạy thay</h2>
            <p className="cs-modal__sub">
              Lớp: <b>{subsFor.cls.title}</b> — buổi ngày {subsFor.date}
            </p>
            <p className="cs-modal__sub">
              Gia sư phụ: <b>{subsFor.cls.assistantTutorName}</b>. Buổi học giữ nguyên ngày giờ.
            </p>
            {subError && <div className="cs-alert cs-alert--error">{subError}</div>}
            <label className="cs-field">
              <span>Lý do (tuỳ chọn)</span>
              <textarea
                rows={3}
                value={subReason}
                onChange={(e) => setSubReason(e.target.value)}
                placeholder="VD: Bận việc đột xuất, nhờ gia sư phụ dạy hôm nay"
              />
            </label>
            <div className="cs-modal__actions">
              <button className="cs-btn cs-btn--ghost" type="button" onClick={closeSubstitute}>
                Huỷ
              </button>
              <button
                className="cs-btn cs-btn--primary"
                type="button"
                disabled={subBusy}
                onClick={submitSubstitute}
              >
                {subBusy ? 'Đang gửi…' : 'Gửi yêu cầu'}
              </button>
            </div>
          </div>
        </div>
      )}

      {reschedFor && (
        <div className="cs-modal" role="dialog" aria-modal="true">
          <div className="cs-modal__backdrop" onClick={closeResched} />
          <div className="cs-modal__card">
            <h2 className="cs-modal__title">Đổi lịch dạy (báo ốm)</h2>
            <p className="cs-modal__sub">
              Lớp: <b>{reschedFor.cls.title}</b> — buổi ngày {reschedFor.date}
            </p>
            {reschedError && <div className="cs-alert cs-alert--error">{reschedError}</div>}
            <label className="cs-field">
              <span>Dời sang ngày *</span>
              <input
                type="date"
                min={TODAY_ISO}
                value={newDate}
                onChange={(e) => setNewDate(e.target.value)}
              />
            </label>
            <div className="cs-field2">
              <label className="cs-field">
                <span>Giờ bắt đầu *</span>
                <input
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                />
              </label>
              <label className="cs-field">
                <span>Giờ kết thúc *</span>
                <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
              </label>
            </div>
            <label className="cs-field">
              <span>Lý do (tuỳ chọn)</span>
              <textarea
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="VD: Bị ốm, xin dạy bù sang ngày khác"
              />
            </label>
            <div className="cs-modal__actions">
              <button className="cs-btn cs-btn--ghost" type="button" onClick={closeResched}>
                Huỷ
              </button>
              <button
                className="cs-btn cs-btn--primary"
                type="button"
                disabled={reschedBusy}
                onClick={submitResched}
              >
                {reschedBusy ? 'Đang gửi…' : 'Gửi yêu cầu'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
