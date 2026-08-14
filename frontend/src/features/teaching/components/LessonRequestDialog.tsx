import { useEffect, useMemo, useState } from 'react';
import type {
  ClassOption,
  ExtraLessonPayload,
  LessonResponse,
  RescheduleLessonPayload,
} from '../types/teachingTypes';
import { hhmm, hhmmDisplay, toIsoDate } from '../../../shared/utils/format';
import { SESSION_OPTIONS } from '../../marketplace/types/marketplaceTypes';
import './LessonRequestDialog.css';

const NO_CLASSES: ClassOption[] = [];

const toMinutes = (t: string) => {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + m;
};

function buildTimeSlots(min: string, max: string, step = 30): string[] {
  const out: string[] = [];
  for (let x = toMinutes(min); x <= toMinutes(max); x += step) {
    out.push(`${String(Math.floor(x / 60)).padStart(2, '0')}:${String(x % 60).padStart(2, '0')}`);
  }
  if (out.length > 0 && out[out.length - 1] !== max) out.push(max);
  return out;
}

function durationLabel(start: string, end: string): string {
  const endMin = end === '23:59' ? 24 * 60 : toMinutes(end);
  const diff = endMin - toMinutes(start);
  if (diff <= 0) return '';
  const h = Math.floor(diff / 60);
  const m = diff % 60;
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
} & (
  | {
      readonly mode: 'RESCHEDULE';
      readonly lesson: LessonResponse;
      readonly onSubmit: (payload: RescheduleLessonPayload) => Promise<boolean>;
    }
  | {
      readonly mode: 'EXTRA';
      readonly classes: ClassOption[];
      readonly onSubmit: (payload: ExtraLessonPayload) => Promise<boolean>;
    }
);

export function LessonRequestDialog(props: Props) {
  const { mode, onClose } = props;
  const isReschedule = mode === 'RESCHEDULE';
  const lesson = props.mode === 'RESCHEDULE' ? props.lesson : null;
  const classes = props.mode === 'EXTRA' ? props.classes : NO_CLASSES;

  const [classId, setClassId] = useState<number | ''>(classes[0]?.classId ?? '');
  const [date, setDate] = useState(lesson ? lesson.lessonDate : '');
  const [startTime, setStartTime] = useState(lesson ? hhmm(lesson.startTime) : '08:00');
  const [endTime, setEndTime] = useState(lesson ? hhmm(lesson.endTime) : '10:00');
  const [session, setSession] = useState(lesson ? sessionOf(hhmm(lesson.startTime)) : 'Sáng');
  const [subjectId, setSubjectId] = useState<number | ''>('');
  const [reason, setReason] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const subjects = useMemo(
    () => classes.find((c) => c.classId === classId)?.subjects ?? [],
    [classes, classId],
  );

  const todayIso = useMemo(() => toIsoDate(new Date()), []);
  const nowHm = useMemo(() => new Date().toTimeString().slice(0, 5), []);
  const isToday = date === todayIso;

  const sess = SESSION_OPTIONS.find((o) => o.value === session) ?? SESSION_OPTIONS[0];
  const allSlots = useMemo(() => buildTimeSlots(sess.min, sess.max), [sess.min, sess.max]);
  const startOptions = useMemo(
    () => (isToday ? allSlots.filter((t) => t > nowHm) : allSlots),
    [allSlots, isToday, nowHm],
  );
  const endOptions = allSlots.filter((t) => t > startTime);

  // Cảnh báo trùng lịch ngay khi chọn ngày/giờ (không đợi bấm gửi).
  const excludeId = props.mode === 'RESCHEDULE' ? props.lesson.lessonId : null;
  const conflict = useMemo(() => {
    if (!date || !startTime || !endTime || startTime >= endTime) return null;
    const clash = (props.existingLessons ?? []).find(
      (l) =>
        l.lessonId !== excludeId &&
        l.lessonDate === date &&
        startTime < hhmm(l.endTime) &&
        hhmm(l.startTime) < endTime,
    );
    return clash
      ? `Khung giờ này trùng với buổi "${clash.classTitle}" ngày ${date} (${hhmm(clash.startTime)}–${hhmm(clash.endTime)}). Vui lòng chọn giờ hoặc ngày khác.`
      : null;
  }, [date, startTime, endTime, props.existingLessons, excludeId]);

  useEffect(() => {
    if (startOptions.length > 0 && !startOptions.includes(startTime)) {
      const ns = startOptions[0];
      setStartTime(ns);
      setEndTime(allSlots.find((t) => t > ns) ?? ns);
    }
  }, [startOptions, startTime, allSlots]);

  function changeSession(value: string) {
    const preset = SESSION_OPTIONS.find((o) => o.value === value) ?? SESSION_OPTIONS[0];
    setSession(value);
    setStartTime(preset.start);
    setEndTime(preset.end);
    setLocalError(null);
  }

  function changeStart(value: string) {
    setStartTime(value);
    if (endTime <= value) {
      const next = buildTimeSlots(sess.min, sess.max).find((t) => t > value);
      setEndTime(next ?? value);
    }
    setLocalError(null);
  }

  function validate(): string | null {
    if (!isReschedule && classId === '') return 'Chọn lớp cần thêm buổi.';
    if (!isReschedule && subjects.length > 0 && subjectId === '') return 'Vui lòng chọn môn học.';
    if (!reason.trim())
      return isReschedule ? 'Vui lòng nhập lý do đổi lịch.' : 'Vui lòng nhập lý do thêm buổi.';
    if (!date) return 'Chọn ngày học.';
    if (!startTime || !endTime) return 'Nhập giờ bắt đầu và giờ kết thúc.';
    if (startTime >= endTime) return 'Giờ kết thúc phải sau giờ bắt đầu.';
    if (date < todayIso) return 'Không thể xếp buổi học vào ngày đã qua.';
    if (date === todayIso && startTime <= nowHm)
      return 'Giờ học hôm nay đã qua — chọn giờ muộn hơn.';
    if (
      lesson &&
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
    const ok =
      props.mode === 'RESCHEDULE'
        ? await props.onSubmit({
            newDate: date,
            newStartTime: startTime,
            newEndTime: endTime,
            reason: reason.trim() || undefined,
          })
        : await props.onSubmit({
            classId: Number(classId),
            lessonDate: date,
            startTime,
            endTime,
            subjectId: subjectId === '' ? null : Number(subjectId),
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
          <h3 id="lrd-title">{isReschedule ? 'Đổi lịch buổi học' : 'Thêm buổi học'}</h3>
          <button className="lrd__x" type="button" onClick={onClose} aria-label="Đóng">
            ×
          </button>
        </div>

        {lesson && (
          <p className="lrd__current">
            Buổi {lesson.sequenceNo} · {lesson.classTitle}
            <br />
            Hiện tại: <strong>{lesson.lessonDate}</strong> ({hhmmDisplay(lesson.startTime)}–
            {hhmmDisplay(lesson.endTime)})
          </p>
        )}

        <form className="lrd__form" onSubmit={(e) => void handleSubmit(e)}>
          {!isReschedule && (
            <label className="tcs-field">
              <span className="tcs-field__label">Lớp</span>
              <select
                className="tcs-input"
                value={classId}
                onChange={(e) => {
                  setClassId(e.target.value === '' ? '' : Number(e.target.value));
                  setSubjectId('');
                }}
              >
                <option value="">— Chọn lớp —</option>
                {classes.map((c) => (
                  <option key={c.classId} value={c.classId}>
                    {c.classTitle}
                  </option>
                ))}
              </select>
            </label>
          )}

          {!isReschedule && subjects.length > 0 && (
            <label className="tcs-field">
              <span className="tcs-field__label">Môn học *</span>
              <select
                className="tcs-input"
                value={subjectId}
                onChange={(e) => setSubjectId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <option value="">— Chọn môn học —</option>
                {subjects.map((s) => (
                  <option key={s.subjectId} value={s.subjectId}>
                    {s.subjectName}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="tcs-field">
            <span className="tcs-field__label">{isReschedule ? 'Ngày mới' : 'Ngày học'}</span>
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
              Buổi {sess.label} hôm nay đã hết giờ — chọn buổi khác hoặc ngày khác.
            </p>
          ) : (
            <div className="lrd__row">
              <label className="tcs-field">
                <span className="tcs-field__label">Từ</span>
                <select
                  className="tcs-input"
                  value={startTime}
                  onChange={(e) => changeStart(e.target.value)}
                >
                  {startOptions.map((t) => (
                    <option key={t} value={t}>
                      {hhmmDisplay(t)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="tcs-field">
                <span className="tcs-field__label">Đến</span>
                <select
                  className="tcs-input"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                >
                  {endOptions.map((t) => (
                    <option key={t} value={t}>
                      {hhmmDisplay(t)}
                      {durationLabel(startTime, t) ? ` (${durationLabel(startTime, t)})` : ''}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          )}

          <label className="tcs-field">
            <span className="tcs-field__label">
              Lý do <em>*</em>
            </span>
            <textarea
              className="lrd__textarea"
              rows={3}
              value={reason}
              maxLength={500}
              placeholder={
                isReschedule ? 'Ví dụ: hôm đó em có lịch thi ở trường…' : 'Ví dụ: học bù buổi đã nghỉ…'
              }
              onChange={(e) => setReason(e.target.value)}
            />
          </label>

          {!localError && conflict && <p className="lrd__err">⚠ {conflict}</p>}
          {(localError || (submitted && props.submitError)) && (
            <p className="lrd__err">{localError || props.submitError}</p>
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
