import { useMemo, useState } from 'react';
import { SESSION_OPTIONS } from '../../marketplace/types/marketplaceTypes';
import { hhmm, hhmmDisplay, toIsoDate } from '../../../shared/utils/format';
import { ATTENDANCE_STATUS_LABELS, type LessonResponse } from '../types/teachingTypes';
import './WeeklyTimetable.css';

const DAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

function mondayOf(d: Date): Date {
  const copy = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const shift = (copy.getDay() + 6) % 7;
  copy.setDate(copy.getDate() - shift);
  return copy;
}

const ddmm = (d: Date) => `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;

function sessionOf(startTime: string): string {
  const hm = hhmm(startTime);
  const found = SESSION_OPTIONS.find((s) => hm < s.max);
  return (found ?? SESSION_OPTIONS[SESSION_OPTIONS.length - 1]).value;
}

interface Props {
  readonly lessons: LessonResponse[];
  readonly readOnly?: boolean;
  readonly onAttend?: (lessonId: number, present: boolean) => void;
  readonly onReschedule?: (lesson: LessonResponse) => void;
  readonly onOpenDetail?: (lesson: LessonResponse) => void;
  readonly onReview?: (lesson: LessonResponse) => void;
  readonly pendingLessonIds?: ReadonlySet<number>;
  readonly reviewedLessonIds?: ReadonlySet<number>;
}

export function WeeklyTimetable({
  lessons,
  readOnly = false,
  onAttend,
  onReschedule,
  onOpenDetail,
  onReview,
  pendingLessonIds,
  reviewedLessonIds,
}: Props) {
  const [weekOffset, setWeekOffset] = useState(0);
  const todayIso = toIsoDate(new Date());

  const days = useMemo(() => {
    const monday = mondayOf(new Date());
    monday.setDate(monday.getDate() + weekOffset * 7);
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      return { iso: toIsoDate(d), label: DAY_LABELS[i], ddmm: ddmm(d) };
    });
  }, [weekOffset]);

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
                    {hhmmDisplay(session.min)}–{hhmmDisplay(session.max)}
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
                            onAttend={(present) => onAttend?.(lesson.lessonId, present)}
                            onReschedule={onReschedule}
                            onOpenDetail={onOpenDetail}
                            onReview={onReview}
                            hasPendingRequest={pendingLessonIds?.has(lesson.lessonId) ?? false}
                            reviewed={reviewedLessonIds?.has(lesson.lessonId) ?? false}
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
          <i className="wtt__dot wtt__dot--done" /> Đã điểm danh
        </span>
        <span>
          <i className="wtt__dot wtt__dot--todaywait" /> Diễn ra trong ngày (chưa tới giờ điểm danh)
        </span>
        <span>
          <i className="wtt__dot wtt__dot--today" /> Đang diễn ra buổi học
        </span>
        <span>
          <i className="wtt__dot wtt__dot--absent" /> Vắng mặt
        </span>
        <span>
          <i className="wtt__dot wtt__dot--pending" /> Lịch học
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
  onAttend,
  onReschedule,
  onOpenDetail,
  onReview,
  hasPendingRequest,
  reviewed,
}: {
  readonly lesson: LessonResponse;
  readonly readOnly: boolean;
  readonly onAttend: (present: boolean) => void;
  readonly onReschedule?: (lesson: LessonResponse) => void;
  readonly onOpenDetail?: (lesson: LessonResponse) => void;
  readonly onReview?: (lesson: LessonResponse) => void;
  readonly hasPendingRequest: boolean;
  readonly reviewed: boolean;
}) {
  const done = lesson.attendanceStatus === 'COMPLETED';
  const absent = lesson.attendanceStatus === 'ABSENT';
  const isTodayLesson = lesson.lessonDate === toIsoDate(new Date());
  // done=xanh lá · absent=đỏ · today(đang tới giờ điểm danh)=xanh dương ·
  // todaywait(hôm nay nhưng chưa tới giờ)=vàng · pending(lịch tương lai)=cam.
  const tone = done
    ? 'done'
    : absent
      ? 'absent'
      : lesson.canCheckInToday
        ? 'today'
        : isTodayLesson
          ? 'todaywait'
          : 'pending';
  const canReschedule = onReschedule && lesson.attendanceStatus === 'PENDING';
  const [attendOpen, setAttendOpen] = useState(false);

  const info = (
    <>
      <div className="wtt-chip__subject" title={lesson.subjectName ?? ''}>
        {lesson.subjectName ?? '—'}
      </div>
      <div className="wtt-chip__class" title={lesson.classTitle}>
        {lesson.classTitle}
      </div>
      <div className="wtt-chip__time">
        ({hhmmDisplay(lesson.startTime)}–{hhmmDisplay(lesson.endTime)})
      </div>
      <div className={`wtt-chip__status wtt-chip__status--${tone}`}>
        {done ? '(đã dạy)' : `(${ATTENDANCE_STATUS_LABELS[lesson.attendanceStatus].toLowerCase()})`}
      </div>
    </>
  );

  return (
    <div className={`wtt-chip wtt-chip--${tone}`}>
      {onOpenDetail ? (
        <button
          type="button"
          className="wtt-chip__info"
          title="Xem chi tiết lớp"
          onClick={() => onOpenDetail(lesson)}
        >
          {info}
        </button>
      ) : (
        info
      )}
      {!readOnly && !done && (
        <div className="wtt-chip__attend">
          <button
            className="tcs-btn tcs-btn--sm tcs-btn--primary"
            type="button"
            onClick={() => setAttendOpen((v) => !v)}
            disabled={!lesson.canCheckInToday}
            aria-haspopup="menu"
            aria-expanded={attendOpen}
            title={
              lesson.canCheckInToday
                ? undefined
                : `Chỉ điểm danh được từ giờ bắt đầu buổi học (${hhmmDisplay(lesson.startTime)}) đến hết ngày hôm đó`
            }
          >
            Điểm danh
          </button>
          {attendOpen && lesson.canCheckInToday ? (
            <div className="wtt-attend-menu" role="menu">
              <button
                type="button"
                role="menuitem"
                className="wtt-attend-menu__item wtt-attend-menu__item--present"
                onClick={() => {
                  setAttendOpen(false);
                  onAttend(true);
                }}
              >
                ✓ Có mặt
              </button>
              <button
                type="button"
                role="menuitem"
                className="wtt-attend-menu__item wtt-attend-menu__item--absent"
                onClick={() => {
                  setAttendOpen(false);
                  onAttend(false);
                }}
              >
                ✕ Vắng mặt
              </button>
            </div>
          ) : null}
        </div>
      )}
      {hasPendingRequest ? (
        <span className="wtt-chip__pendingreq" title="Đang chờ bên còn lại duyệt">
          ⏳ chờ duyệt
        </span>
      ) : (
        canReschedule && (
          <button
            className="tcs-btn tcs-btn--sm tcs-btn--ghost"
            type="button"
            onClick={() => onReschedule(lesson)}
          >
            Đổi lịch
          </button>
        )
      )}
      {onReview && done && (
        reviewed ? (
          <span className="wtt-chip__reviewed" title="Bạn đã đánh giá gia sư cho buổi học này">
            ✓ Đã đánh giá
          </span>
        ) : (
          <button
            className="tcs-btn tcs-btn--sm tcs-btn--primary"
            type="button"
            onClick={() => onReview(lesson)}
          >
            Đánh giá gia sư
          </button>
        )
      )}
    </div>
  );
}
