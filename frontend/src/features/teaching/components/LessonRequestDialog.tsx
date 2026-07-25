import { useMemo, useState } from 'react';
import type {
  ClassOption,
  ExtraLessonPayload,
  LessonResponse,
  RescheduleLessonPayload,
} from '../types/teachingTypes';
import { hhmm, toIsoDate } from '../../../shared/utils/format';
import './LessonRequestDialog.css';

/** Tham chiếu cố định cho chế độ RESCHEDULE — xem ghi chú ở chỗ dùng. */
const NO_CLASSES: ClassOption[] = [];

/**
 * Hai chế độ loại trừ nhau: dời buổi thì buộc phải có `lesson`, thêm buổi thì buộc phải có
 * `classes`. Dùng union để TypeScript tự chặn, khỏi phải guard bằng tay ở khắp thân hàm.
 */
type Props = { readonly onClose: () => void } & (
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

/**
 * Dialog gửi yêu cầu đổi lịch hoặc thêm buổi (UC-36).
 * Chỉ GỬI yêu cầu — lịch thật chỉ đổi khi bên còn lại duyệt.
 */
export function LessonRequestDialog(props: Props) {
  const { mode, onClose } = props;
  const isReschedule = mode === 'RESCHEDULE';
  const lesson = props.mode === 'RESCHEDULE' ? props.lesson : null;
  // Hằng số ở ngoài component: nếu viết [] tại đây thì mỗi render là một mảng mới,
  // dep của useMemo bên dưới đổi liên tục và memo thành vô nghĩa.
  const classes = props.mode === 'EXTRA' ? props.classes : NO_CLASSES;

  const [classId, setClassId] = useState<number | ''>(classes[0]?.classId ?? '');
  const [date, setDate] = useState(lesson ? lesson.lessonDate : '');
  const [startTime, setStartTime] = useState(lesson ? hhmm(lesson.startTime) : '');
  const [endTime, setEndTime] = useState(lesson ? hhmm(lesson.endTime) : '');
  const [subjectId, setSubjectId] = useState<number | ''>('');
  const [reason, setReason] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const subjects = useMemo(
    () => classes.find((c) => c.classId === classId)?.subjects ?? [],
    [classes, classId],
  );

  /** Chặn tại chỗ những lỗi rõ ràng; phần còn lại để server quyết (trùng lịch, quyền…). */
  function validate(): string | null {
    if (!isReschedule && classId === '') return 'Chọn lớp cần thêm buổi.';
    if (!date) return 'Chọn ngày học.';
    if (!startTime || !endTime) return 'Nhập giờ bắt đầu và giờ kết thúc.';
    if (startTime >= endTime) return 'Giờ kết thúc phải sau giờ bắt đầu.';
    if (date < toIsoDate(new Date())) return 'Không thể xếp buổi học vào ngày đã qua.';
    if (
      lesson &&
      date === lesson.lessonDate &&
      startTime === hhmm(lesson.startTime) &&
      endTime === hhmm(lesson.endTime)
    ) {
      return 'Lịch mới trùng với lịch hiện tại — chưa có gì để đổi.';
    }
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
    // Lỗi từ server đã hiện ở banner của trang — chỉ đóng dialog khi thành công.
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
            Hiện tại: <strong>{lesson.lessonDate}</strong> ({hhmm(lesson.startTime)}–
            {hhmm(lesson.endTime)})
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
              <span className="tcs-field__label">Môn học</span>
              <select
                className="tcs-input"
                value={subjectId}
                onChange={(e) => setSubjectId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <option value="">— Không chỉ định —</option>
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
              value={date}
              min={toIsoDate(new Date())}
              onChange={(e) => setDate(e.target.value)}
            />
          </label>

          <div className="lrd__row">
            <label className="tcs-field">
              <span className="tcs-field__label">Từ</span>
              <input className="tcs-input" type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
            </label>
            <label className="tcs-field">
              <span className="tcs-field__label">Đến</span>
              <input className="tcs-input" type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
            </label>
          </div>

          <label className="tcs-field">
            <span className="tcs-field__label">Lý do {isReschedule ? '' : '(không bắt buộc)'}</span>
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

          {localError && <p className="lrd__err">{localError}</p>}

          <p className="lrd__hint">
            Yêu cầu sẽ được gửi tới bên còn lại. Lịch chỉ thay đổi sau khi được duyệt.
          </p>

          <div className="lrd__actions">
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={onClose} disabled={busy}>
              Huỷ
            </button>
            <button className="tcs-btn tcs-btn--primary" type="submit" disabled={busy}>
              {busy ? 'Đang gửi…' : 'Gửi yêu cầu'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
