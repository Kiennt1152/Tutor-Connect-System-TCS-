import { useEffect, useMemo, useState } from 'react';
import type { LessonResponse, RescheduleLessonPayload } from '../types/teachingTypes';
import { hhmm, hhmmDisplay, toIsoDate } from '../../../shared/utils/format';
import { SESSION_OPTIONS } from '../../marketplace/types/marketplaceTypes';
import './LessonRequestDialog.css';

const toMinutes = (t: string) => {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + m;
};

const minutesToHhmm = (mins: number) =>
  `${String(Math.floor(mins / 60)).padStart(2, '0')}:${String(mins % 60).padStart(2, '0')}`;

function buildTimeSlots(min: string, max: string, step = 30): string[] {
  const out: string[] = [];
  for (let x = toMinutes(min); x <= toMinutes(max); x += step) {
    out.push(`${String(Math.floor(x / 60)).padStart(2, '0')}:${String(x % 60).padStart(2, '0')}`);
  }
  if (out.length > 0 && out[out.length - 1] !== max) out.push(max);
  return out;
}

function durationLabel(mins: number): string {
  if (mins <= 0) return '';
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return [h ? `${h} giờ` : '', m ? `${m} phút` : ''].filter(Boolean).join(' ');
}

function sessionOf(start: string): string {
  if (start && start >= '18:00') return 'Tối';
  if (start && start >= '12:00') return 'Chiều';
  return 'Sáng';
}

type Props = {
  readonly onClose: () => void;
  readonly submitError?: string | null;
  /** Các buổi học hiện có để cảnh báo trùng lịch ngay khi chọn ngày/giờ. */
  readonly existingLessons?: readonly LessonResponse[];
  readonly lesson: LessonResponse;
  readonly onSubmit: (payload: RescheduleLessonPayload) => Promise<boolean>;
};

/**
 * Đổi lịch buổi học: chỉ cho đổi NGÀY và BUỔI/giờ bắt đầu.
 * Độ dài buổi học được GIỮ NGUYÊN theo buổi gốc — không cho thêm/bớt thời gian.
 */
export function LessonRequestDialog({
  onClose,
  submitError,
  existingLessons,
  lesson,
  onSubmit,
}: Props) {
  // Độ dài buổi gốc (phút) — cố định, không đổi.
  const durationMin = useMemo(
    () => toMinutes(hhmm(lesson.endTime)) - toMinutes(hhmm(lesson.startTime)),
    [lesson.endTime, lesson.startTime],
  );

  const [date, setDate] = useState(lesson.lessonDate);
  const [startTime, setStartTime] = useState(hhmm(lesson.startTime));
  const [session, setSession] = useState(sessionOf(hhmm(lesson.startTime)));
  const [reason, setReason] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const todayIso = useMemo(() => toIsoDate(new Date()), []);
  const nowHm = useMemo(() => new Date().toTimeString().slice(0, 5), []);
  const isToday = date === todayIso;

  const sess = SESSION_OPTIONS.find((o) => o.value === session) ?? SESSION_OPTIONS[0];
  const maxEndMin = toMinutes(sess.max);

  // Giờ bắt đầu hợp lệ: nằm trong buổi và start + độ dài không vượt quá cuối buổi.
  const startOptions = useMemo(() => {
    const slots = buildTimeSlots(sess.min, sess.max).filter(
      (t) => toMinutes(t) + durationMin <= maxEndMin,
    );
    return isToday ? slots.filter((t) => t > nowHm) : slots;
  }, [sess.min, sess.max, maxEndMin, durationMin, isToday, nowHm]);

  const endTime = useMemo(
    () => minutesToHhmm(toMinutes(startTime) + durationMin),
    [startTime, durationMin],
  );

  // Cảnh báo trùng lịch ngay khi chọn ngày/giờ (không đợi bấm gửi).
  const conflict = useMemo(() => {
    if (!date || !startTime || !endTime || startTime >= endTime) return null;
    const clash = (existingLessons ?? []).find(
      (l) =>
        l.lessonId !== lesson.lessonId &&
        l.lessonDate === date &&
        startTime < hhmm(l.endTime) &&
        hhmm(l.startTime) < endTime,
    );
    return clash
      ? `Khung giờ này trùng với buổi "${clash.classTitle}" ngày ${date} (${hhmm(clash.startTime)}–${hhmm(clash.endTime)}). Vui lòng chọn giờ hoặc ngày khác.`
      : null;
  }, [date, startTime, endTime, existingLessons, lesson.lessonId]);

  // Nếu giờ bắt đầu hiện tại không còn hợp lệ (đổi buổi/ngày), chọn giờ hợp lệ đầu tiên.
  useEffect(() => {
    if (startOptions.length > 0 && !startOptions.includes(startTime)) {
      setStartTime(startOptions[0]);
    }
  }, [startOptions, startTime]);

  function changeSession(value: string) {
    const preset = SESSION_OPTIONS.find((o) => o.value === value) ?? SESSION_OPTIONS[0];
    setSession(value);
    setStartTime(preset.start);
    setLocalError(null);
  }

  function validate(): string | null {
    if (!reason.trim()) return 'Vui lòng nhập lý do đổi lịch.';
    if (!date) return 'Chọn ngày học.';
    if (!startTime || !endTime) return 'Chọn giờ bắt đầu.';
    if (startTime >= endTime) return 'Giờ kết thúc phải sau giờ bắt đầu.';
    if (date < todayIso) return 'Không thể xếp buổi học vào ngày đã qua.';
    if (date === todayIso && startTime <= nowHm)
      return 'Giờ học hôm nay đã qua — chọn giờ muộn hơn.';
    if (
      date === lesson.lessonDate &&
      startTime === hhmm(lesson.startTime) &&
      endTime === hhmm(lesson.endTime)
    ) {
      return 'Lịch mới trùng với lịch hiện tại — chưa có gì để đổi.';
    }
    if (conflict) return conflict;
    return null;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const problem = validate();
    if (problem) {
      setLocalError(problem);
      return;
    }
    setLocalError(null);
    setSubmitted(true);
    setBusy(true);
    const ok = await onSubmit({
      newDate: date,
      newStartTime: startTime,
      newEndTime: endTime,
      reason: reason.trim() || undefined,
    });
    setBusy(false);
    if (ok) onClose();
  }

  return (
    <div className="lrd__backdrop" role="presentation" onClick={onClose}>
      <div
        className="lrd"
        role="dialog"
        aria-modal="true"
        aria-labelledby="lrd-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="lrd__head">
          <h3 id="lrd-title">Đổi lịch buổi học</h3>
          <button className="lrd__x" type="button" onClick={onClose} aria-label="Đóng">
            ×
          </button>
        </div>

        <p className="lrd__current">
          Buổi {lesson.sequenceNo} · {lesson.classTitle}
          <br />
          Hiện tại: <strong>{lesson.lessonDate}</strong> ({hhmmDisplay(lesson.startTime)}–
          {hhmmDisplay(lesson.endTime)})
        </p>

        <form className="lrd__form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="tcs-field">
            <span className="tcs-field__label">Ngày mới</span>
            <input
              className="tcs-input"
              type="date"
              lang="vi-VN"
              value={date}
              min={toIsoDate(new Date())}
              onChange={(e) => setDate(e.target.value)}
            />
          </label>

          <label className="tcs-field">
            <span className="tcs-field__label">Buổi</span>
            <select
              className="tcs-input"
              value={session}
              onChange={(e) => changeSession(e.target.value)}
            >
              {SESSION_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>

          {startOptions.length === 0 ? (
            <p className="lrd__err">
              Buổi {sess.label} không đủ chỗ cho buổi học dài {durationLabel(durationMin)} — chọn buổi
              khác hoặc ngày khác.
            </p>
          ) : (
            <div className="lrd__row">
              <label className="tcs-field">
                <span className="tcs-field__label">Giờ bắt đầu</span>
                <select
                  className="tcs-input"
                  value={startTime}
                  onChange={(e) => {
                    setStartTime(e.target.value);
                    setLocalError(null);
                  }}
                >
                  {startOptions.map((t) => (
                    <option key={t} value={t}>
                      {hhmmDisplay(t)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="tcs-field">
                <span className="tcs-field__label">Giờ kết thúc</span>
                <input
                  className="tcs-input"
                  type="text"
                  value={`${hhmmDisplay(endTime)}${
                    durationLabel(durationMin) ? ` (${durationLabel(durationMin)})` : ''
                  }`}
                  readOnly
                  aria-label="Giờ kết thúc (giữ nguyên độ dài buổi)"
                />
              </label>
            </div>
          )}

          <p className="lrd__hint">Độ dài buổi học được giữ nguyên, chỉ đổi ngày và giờ bắt đầu.</p>

          <label className="tcs-field">
            <span className="tcs-field__label">
              Lý do <em>*</em>
            </span>
            <textarea
              className="lrd__textarea"
              rows={3}
              value={reason}
              maxLength={500}
              placeholder="Ví dụ: hôm đó em có lịch thi ở trường…"
              onChange={(e) => setReason(e.target.value)}
            />
          </label>

          {!localError && conflict && <p className="lrd__err">⚠ {conflict}</p>}
          {(localError || (submitted && submitError)) && (
            <p className="lrd__err">{localError || submitError}</p>
          )}

          <p className="lrd__hint">
            Yêu cầu sẽ được gửi tới bên còn lại. Lịch chỉ thay đổi sau khi được duyệt.
          </p>

          <div className="lrd__actions">
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={onClose} disabled={busy}>
              Huỷ
            </button>
            <button className="tcs-btn tcs-btn--primary" type="submit" disabled={busy || !!conflict}>
              {busy ? 'Đang gửi…' : 'Gửi yêu cầu'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
