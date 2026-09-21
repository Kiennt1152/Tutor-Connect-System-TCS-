import { useEffect, useMemo, useState } from 'react';
import type { LessonResponse, RescheduleLessonPayload } from '../types/teachingTypes';
import {
  MIDNIGHT_END,
  endMinutes,
  formatDateVi,
  hhmm,
  hhmmDisplay,
  isValidTimeRange,
  slotOverlaps,
  startMinutes,
  toIsoDate,
} from '../../../shared/utils/format';
import { SESSION_OPTIONS } from '../../marketplace/types/marketplaceTypes';
import './LessonRequestDialog.css';

/**
 * Đổi số phút trong ngày thành "HH:mm"; 1440 phút là mốc NỬA ĐÊM nên trả về "00:00"
 * (quy ước chung của hệ thống — xem MIDNIGHT_END, bản backend là SlotTime).
 */
const minutesToHhmm = (mins: number) =>
  mins >= 24 * 60
    ? MIDNIGHT_END
    : `${String(Math.floor(mins / 60)).padStart(2, '0')}:${String(mins % 60).padStart(2, '0')}`;

/**
 * Các mốc GIỜ BẮT ĐẦU chọn được trong một buổi: từ đầu buổi tới mốc muộn nhất mà buổi học
 * dài `durationMin` vẫn kết thúc trước (hoặc đúng) lúc hết buổi.
 *
 * <p>Cuối buổi Tối là "00:00" — tức 24:00 (1440 phút) của chính ngày hôm đó, KHÔNG phải 0h
 * đầu ngày. Vì vậy mốc cuối buổi phải đi qua {@link endMinutes}; so sánh chuỗi hoặc tính tay
 * sẽ ra 0 phút và buổi Tối không còn mốc giờ nào để chọn.</p>
 */
function buildStartOptions(
  sessionMin: string,
  sessionMax: string,
  durationMin: number,
  step = 30,
): string[] {
  const from = startMinutes(sessionMin);
  const latestStart = endMinutes(sessionMax) - durationMin;
  if (!Number.isFinite(from) || !Number.isFinite(latestStart) || latestStart < from) return [];

  const out: string[] = [];
  for (let x = from; x <= latestStart; x += step) out.push(minutesToHhmm(x));
  // Bước nhảy có thể không rơi đúng mốc muộn nhất -> bổ sung để không mất giờ hợp lệ cuối cùng.
  const last = minutesToHhmm(latestStart);
  if (out[out.length - 1] !== last) out.push(last);
  return out;
}

/** Nhãn thời lượng, ví dụ "1 giờ 30 phút". */
function durationLabel(mins: number): string {
  if (mins <= 0) return '';
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return [h ? `${h} giờ` : '', m ? `${m} phút` : ''].filter(Boolean).join(' ');
}

/** Suy ra buổi (Sáng/Chiều/Tối) từ giờ bắt đầu. */
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
    // Qua endMinutes vì buổi có thể kết thúc lúc nửa đêm (00:00 = 24:00).
    () => endMinutes(lesson.endTime) - startMinutes(lesson.startTime),
    [lesson.endTime, lesson.startTime],
  );

  const [date, setDate] = useState(lesson.lessonDate);
  const [startTime, setStartTime] = useState(hhmm(lesson.startTime));
  const [session, setSession] = useState(sessionOf(hhmm(lesson.startTime)));
  const [reason, setReason] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  /** Ngày hôm nay dạng "yyyy-MM-dd" (tính một lần khi mở hộp thoại). */
  const todayIso = useMemo(() => toIsoDate(new Date()), []);
  /** Giờ hiện tại "HH:mm" (để chặn chọn giờ đã qua trong hôm nay). */
  const nowHm = useMemo(() => new Date().toTimeString().slice(0, 5), []);
  const isToday = date === todayIso;

  const sess = SESSION_OPTIONS.find((o) => o.value === session) ?? SESSION_OPTIONS[0];

  // Giờ bắt đầu hợp lệ: nằm trong buổi và start + độ dài không vượt quá cuối buổi.
  const startOptions = useMemo(() => {
    const slots = buildStartOptions(sess.min, sess.max, durationMin);
    return isToday ? slots.filter((t) => t > nowHm) : slots;
  }, [sess.min, sess.max, durationMin, isToday, nowHm]);

  /** Giờ kết thúc = giờ bắt đầu + thời lượng buổi (chạm nửa đêm thì là "00:00"). */
  const endTime = useMemo(
    () => minutesToHhmm(startMinutes(startTime) + durationMin),
    [startTime, durationMin],
  );

  // Các khung giờ đã có buổi học trong NGÀY đang chọn -> dùng để khoá lựa chọn bị trùng.
  const busyRanges = useMemo(
    () =>
      (existingLessons ?? [])
        .filter((l) => l.lessonId !== lesson.lessonId && l.lessonDate === date)
        .map((l) => ({
          start: hhmm(l.startTime),
          end: hhmm(l.endTime),
          title: l.classTitle,
        })),
    [existingLessons, lesson.lessonId, date],
  );

  /** Buổi bắt đầu lúc t (dài durationMin) có đè lên buổi nào không. */
  const clashAt = useMemo(
    () => (t: string) => {
      const end = minutesToHhmm(startMinutes(t) + durationMin);
      // Qua slotOverlaps vì giờ kết thúc có thể là "00:00" (nửa đêm) — so chuỗi sẽ sai.
      return busyRanges.find((b) => slotOverlaps(t, end, b.start, b.end)) ?? null;
    },
    [busyRanges, durationMin],
  );

  // Giờ còn trống (không trùng buổi nào) — chỉ những giờ này mới chọn được.
  const freeStartOptions = useMemo(
    () => startOptions.filter((t) => !clashAt(t)),
    [startOptions, clashAt],
  );

  // Cảnh báo trùng lịch ngay khi chọn ngày/giờ (không đợi bấm gửi).
  const conflict = useMemo(() => {
    if (!date || !startTime || !endTime || !isValidTimeRange(startTime, endTime)) return null;
    const clash = (existingLessons ?? []).find(
      (l) =>
        l.lessonId !== lesson.lessonId &&
        l.lessonDate === date &&
        slotOverlaps(startTime, endTime, hhmm(l.startTime), hhmm(l.endTime)),
    );
    return clash
      ? `Khung giờ này trùng với buổi "${clash.classTitle}" ${formatDateVi(date)} (${hhmmDisplay(clash.startTime)}–${hhmmDisplay(clash.endTime)}). Vui lòng chọn giờ hoặc ngày khác.`
      : null;
  }, [date, startTime, endTime, existingLessons, lesson.lessonId]);

  // Nếu giờ bắt đầu hiện tại không hợp lệ hoặc bị trùng (đổi buổi/ngày), chọn giờ trống đầu tiên.
  useEffect(() => {
    if (freeStartOptions.length > 0 && !freeStartOptions.includes(startTime)) {
      setStartTime(freeStartOptions[0]);
    }
  }, [freeStartOptions, startTime]);

  /** Đổi buổi: đặt giờ bắt đầu mặc định của buổi và xoá lỗi đang hiện. */
  function changeSession(value: string) {
    const preset = SESSION_OPTIONS.find((o) => o.value === value) ?? SESSION_OPTIONS[0];
    setSession(value);
    setStartTime(preset.start);
    setLocalError(null);
  }

  /**
   * Kiểm tra form đổi lịch: có lý do, ngày, giờ hợp lệ, không ở quá khứ, khác lịch cũ và không trùng lịch khác;
   * trả về câu lỗi đầu tiên hoặc null.
   */
  function validate(): string | null {
    if (!reason.trim()) return 'Vui lòng nhập lý do đổi lịch.';
    if (!date) return 'Chọn ngày học.';
    if (!startTime || !endTime) return 'Chọn giờ bắt đầu.';
    // isValidTimeRange chứ không so chuỗi: "00:00" ở giờ kết thúc là nửa đêm, không phải 0h.
    if (!isValidTimeRange(startTime, endTime)) return 'Giờ kết thúc phải sau giờ bắt đầu.';
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

  /** Gửi yêu cầu đổi lịch sau khi kiểm tra; thành công thì đóng hộp thoại. */
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
          Hiện tại: <strong>{formatDateVi(lesson.lessonDate)}</strong> ({hhmmDisplay(lesson.startTime)}
          –{hhmmDisplay(lesson.endTime)})
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
            {/* Ô <input type="date"> hiển thị theo ngôn ngữ trình duyệt (có máy ra MM/DD/YYYY),
                nên in lại ngày đã chọn bằng tiếng Việt để người dùng không đọc nhầm. */}
            {date && <span className="lrd__hint">{formatDateVi(date)}</span>}
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
          ) : freeStartOptions.length === 0 ? (
            <p className="lrd__err">
              Buổi {sess.label} {formatDateVi(date)} đã kín lịch — chọn buổi khác hoặc ngày khác.
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
                  {startOptions.map((t) => {
                    const clash = clashAt(t);
                    return (
                      <option key={t} value={t} disabled={!!clash}>
                        {hhmmDisplay(t)}
                        {clash ? ' — đã có buổi học' : ''}
                      </option>
                    );
                  })}
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
