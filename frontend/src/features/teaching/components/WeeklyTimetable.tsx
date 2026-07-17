import { useMemo, useState } from 'react';
import { SESSION_OPTIONS } from '../../marketplace/types/marketplaceTypes';
import { ATTENDANCE_STATUS_LABELS, type LessonResponse } from '../types/teachingTypes';
import './WeeklyTimetable.css';

const DAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

/** 'YYYY-MM-DD' của một Date, theo giờ máy (không dùng toISOString để khỏi lệch múi giờ). */
function toIso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Thứ 2 của tuần chứa ngày d (tuần bắt đầu từ Thứ 2, giống lịch VN). */
function mondayOf(d: Date): Date {
  const copy = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const shift = (copy.getDay() + 6) % 7; // CN(0) → 6, T2(1) → 0
  copy.setDate(copy.getDate() - shift);
  return copy;
}

const ddmm = (d: Date) => `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;

/** '18:00:00' → '18:00' */
const hhmm = (t: string) => t.slice(0, 5);

/** Buổi (Sáng/Chiều/Tối) của một giờ bắt đầu — dùng lại mốc giờ của SESSION_OPTIONS. */
function sessionOf(startTime: string): string {
  const hm = hhmm(startTime);
  const found = SESSION_OPTIONS.find((s) => hm < s.max);
  return (found ?? SESSION_OPTIONS[SESSION_OPTIONS.length - 1]).value;
}

interface Props {
  readonly lessons: LessonResponse[];
  /** Chỉ xem (phía Client) — điểm danh là việc của gia sư. */
  readonly readOnly?: boolean;
  readonly onCheckIn?: (lessonId: number) => void;
  readonly onCheckOut?: (lessonId: number) => void;
}

/** Thời khóa biểu theo tuần: cột = thứ, hàng = buổi (Sáng/Chiều/Tối). */
export function WeeklyTimetable({ lessons, readOnly = false, onCheckIn, onCheckOut }: Props) {
  const [weekOffset, setWeekOffset] = useState(0);
  const todayIso = toIso(new Date());

  const days = useMemo(() => {
    const monday = mondayOf(new Date());
    monday.setDate(monday.getDate() + weekOffset * 7);
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      return { iso: toIso(d), label: DAY_LABELS[i], ddmm: ddmm(d) };
    });
  }, [weekOffset]);

  // Tra cứu nhanh theo ô (ngày + buổi); một ô có thể chứa nhiều buổi của các lớp khác nhau.
  const cells = useMemo(() => {
    const map = new Map<string, LessonResponse[]>();
    for (const lesson of lessons) {
      const key = `${lesson.lessonDate}|${sessionOf(lesson.startTime)}`;
      const list = map.get(key) ?? [];
      list.push(lesson);
      map.set(key, list);
    }
    for (const list of map.values()) {
      list.sort((a, b) => a.startTime.localeCompare(b.startTime));
    }
    return map;
  }, [lessons]);

  const weekIsos = new Set(days.map((d) => d.iso));
  const countThisWeek = lessons.filter((l) => weekIsos.has(l.lessonDate)).length;

  // Nhảy nhanh tới tuần có buổi gần nhất kể từ hôm nay — lịch trải nhiều tháng, dò tay rất mệt.
  const nextLessonOffset = useMemo(() => {
    const upcoming = lessons.filter((l) => l.lessonDate >= todayIso).map((l) => l.lessonDate).sort();
    if (upcoming.length === 0) return null;
    const thisMonday = mondayOf(new Date()).getTime();
    const [y, m, d] = upcoming[0].split('-').map(Number);
    const targetMonday = mondayOf(new Date(y, m - 1, d)).getTime();
    const offset = Math.round((targetMonday - thisMonday) / (7 * 24 * 3600 * 1000));
    return offset === weekOffset ? null : offset;
  }, [lessons, todayIso, weekOffset]);

  return (
    <div className="wtt">
      <div className="wtt__bar">
        <div className="wtt__nav">
          <button className="tcs-btn tcs-btn--ghost tcs-btn--sm" type="button" onClick={() => setWeekOffset((w) => w - 1)}>
            ← Tuần trước
          </button>
          <span className="wtt__range">
            {days[0].ddmm} – {days[6].ddmm}/{days[6].iso.slice(0, 4)}
            {weekOffset === 0 && <span className="wtt__now">Tuần này</span>}
          </span>
          <button className="tcs-btn tcs-btn--ghost tcs-btn--sm" type="button" onClick={() => setWeekOffset((w) => w + 1)}>
            Tuần sau →
          </button>
        </div>
        <div className="wtt__jump">
          {weekOffset !== 0 && (
            <button className="tcs-btn tcs-btn--ghost tcs-btn--sm" type="button" onClick={() => setWeekOffset(0)}>
              Về tuần này
            </button>
          )}
          {nextLessonOffset !== null && (
            <button className="tcs-btn tcs-btn--ghost tcs-btn--sm" type="button" onClick={() => setWeekOffset(nextLessonOffset)}>
              Tới buổi gần nhất
            </button>
          )}
        </div>
      </div>

      <div className="wtt__scroll">
        <table className="wtt__table">
          <thead>
            <tr>
              <th className="wtt__corner">Buổi</th>
              {days.map((d) => (
                <th key={d.iso} className={d.iso === todayIso ? 'wtt__th wtt__th--today' : 'wtt__th'}>
                  <span className="wtt__day">{d.label}</span>
                  <span className="wtt__date">{d.ddmm}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {SESSION_OPTIONS.map((session) => (
              <tr key={session.value}>
                <th className="wtt__rowhead">
                  {session.value}
                  <small>
                    {session.min}–{session.max}
                  </small>
                </th>
                {days.map((d) => {
                  const rows = cells.get(`${d.iso}|${session.value}`) ?? [];
                  return (
                    <td key={d.iso} className={d.iso === todayIso ? 'wtt__cell wtt__cell--today' : 'wtt__cell'}>
                      {rows.length === 0 ? (
                        <span className="wtt__empty">-</span>
                      ) : (
                        rows.map((lesson) => (
                          <LessonChip
                            key={lesson.lessonId}
                            lesson={lesson}
                            readOnly={readOnly}
                            onCheckIn={() => onCheckIn?.(lesson.lessonId)}
                            onCheckOut={() => onCheckOut?.(lesson.lessonId)}
                          />
                        ))
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {countThisWeek === 0 && (
        <p className="wtt__none">Tuần này bạn không có buổi dạy nào.</p>
      )}

      <div className="wtt__legend">
        <span>
          <i className="wtt__dot wtt__dot--done" /> Đã hoàn thành
        </span>
        <span>
          <i className="wtt__dot wtt__dot--pending" /> Chưa điểm danh
        </span>
        <span>
          <i className="wtt__dot wtt__dot--today" /> Diễn ra hôm nay
          {!readOnly && ' — điểm danh được'}
        </span>
        <span>
          <strong>-</strong> Không có buổi học
        </span>
      </div>
    </div>
  );
}

function LessonChip({
  lesson,
  readOnly,
  onCheckIn,
  onCheckOut,
}: {
  readonly lesson: LessonResponse;
  readonly readOnly: boolean;
  readonly onCheckIn: () => void;
  readonly onCheckOut: () => void;
}) {
  const done = lesson.attendanceStatus === 'COMPLETED';
  const checkedIn = !!lesson.tutorCheckInAt;
  const tone = done ? 'done' : lesson.canCheckInToday ? 'today' : 'pending';

  return (
    <div className={`wtt-chip wtt-chip--${tone}`}>
      <div className="wtt-chip__subject" title={lesson.subjectName ?? ''}>
        {lesson.subjectName ?? '—'}
      </div>
      <div className="wtt-chip__class" title={lesson.classTitle}>
        {lesson.classTitle}
      </div>
      <div className="wtt-chip__time">
        ({hhmm(lesson.startTime)}–{hhmm(lesson.endTime)}) · #{lesson.sequenceNo}
      </div>
      <div className={`wtt-chip__status wtt-chip__status--${tone}`}>
        {done ? '(đã dạy)' : `(${ATTENDANCE_STATUS_LABELS[lesson.attendanceStatus].toLowerCase()})`}
      </div>
      {!readOnly && !done && lesson.canCheckInToday && (
        <button
          className={`tcs-btn tcs-btn--sm ${checkedIn ? 'tcs-btn--success' : 'tcs-btn--primary'}`}
          type="button"
          onClick={checkedIn ? onCheckOut : onCheckIn}
        >
          {checkedIn ? 'Kết thúc buổi' : 'Vào buổi'}
        </button>
      )}
    </div>
  );
}
