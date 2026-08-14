import { useMemo } from 'react';
import { hhmmDisplay } from '../../../shared/utils/format';
import {
  ASSIGNMENT_STATUS_LABELS,
  ATTENDANCE_STATUS_LABELS,
  CLASS_STATUS_LABELS,
  type AssignmentResponse,
  type LessonResponse,
} from '../types/teachingTypes';
import './ClassDetailModal.css';

const WEEKDAYS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

const LESSON_MODE_LABELS: Record<string, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};

function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return `${WEEKDAYS[date.getDay()]}, ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
}

interface Props {
  readonly assignment: AssignmentResponse | null;
  readonly lessons: LessonResponse[];
  readonly classTitle: string;
  readonly isClient: boolean;
  readonly onClose: () => void;
}

export function ClassDetailModal({ assignment, lessons, classTitle, isClient, onClose }: Props) {
  const sorted = useMemo(
    () => [...lessons].sort((a, b) => a.lessonDate.localeCompare(b.lessonDate) || a.startTime.localeCompare(b.startTime)),
    [lessons],
  );
  const done = sorted.filter((l) => l.attendanceStatus === 'COMPLETED').length;
  const total = sorted.length;
  const percent = total > 0 ? Math.round((done / total) * 100) : 0;

  const statusLabel =
    isClient && assignment?.status === 'ACTIVE'
      ? 'Đang học'
      : assignment
        ? ASSIGNMENT_STATUS_LABELS[assignment.status]
        : '';
  const classStatusLabel = assignment?.classStatus
    ? (CLASS_STATUS_LABELS[assignment.classStatus] ?? assignment.classStatus)
    : '';

  const mode = assignment?.lessonMode ?? '';
  const modeLabel = LESSON_MODE_LABELS[mode] ?? mode;
  const isOnline = mode === 'ONLINE';

  return (
    <div className="cdm__backdrop" role="presentation" onClick={onClose}>
      <div
        className="cdm"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cdm-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="cdm__head">
          <div className="cdm__headmain">
            <h3 id="cdm-title">{assignment?.classTitle ?? classTitle}</h3>
            {statusLabel && <span className="cdm__badge">{statusLabel}</span>}
            {classStatusLabel && <span className="cdm__badge cdm__badge--class">{classStatusLabel}</span>}
          </div>
          <button className="cdm__x" type="button" onClick={onClose} aria-label="Đóng">
            ×
          </button>
        </div>

        <div className="cdm__body">
          <dl className="cdm__grid">
            <div className="cdm__item">
              <dt>{isClient ? 'Gia sư' : 'Phụ huynh / học viên'}</dt>
              <dd>{isClient ? (assignment?.tutorName ?? '—') : (assignment?.clientName ?? '—')}</dd>
            </div>
            <div className="cdm__item">
              <dt>Môn học</dt>
              <dd>{(assignment?.subjectNames ?? []).join(', ') || '—'}</dd>
            </div>
            <div className="cdm__item">
              <dt>Khối lớp</dt>
              <dd>{assignment?.gradeName ?? '—'}</dd>
            </div>
            <div className="cdm__item">
              <dt>Hình thức</dt>
              <dd>
                {modeLabel || '—'}
                {!isOnline && assignment?.address ? ` · ${assignment.address}` : ''}
              </dd>
            </div>
            <div className="cdm__item cdm__item--wide">
              <dt>Thời gian</dt>
              <dd>
                {assignment?.startDate && assignment?.endDate
                  ? `${formatDate(assignment.startDate)} → ${formatDate(assignment.endDate)}`
                  : '—'}
              </dd>
            </div>
          </dl>

          <div className="cdm__progress">
            <div className="cdm__progresstop">
              <span>Tiến độ điểm danh</span>
              <strong>
                {done}/{total} buổi
              </strong>
            </div>
            <div className="cdm__bar">
              <div className="cdm__barfill" style={{ width: `${percent}%` }} />
            </div>
          </div>

          <div className="cdm__lessons">
            <h4 className="cdm__lessonsh">Các buổi học ({total})</h4>
            {total === 0 ? (
              <p className="cdm__muted">Chưa có buổi học nào.</p>
            ) : (
              <ul className="cdm__list">
                {sorted.map((l) => (
                  <li key={l.lessonId} className="cdm__row">
                    <span className="cdm__seq">#{l.sequenceNo}</span>
                    <span className="cdm__when">
                      {formatDate(l.lessonDate)}
                      <small>
                        {hhmmDisplay(l.startTime)}–{hhmmDisplay(l.endTime)}
                        {l.subjectName ? ` · ${l.subjectName}` : ''}
                      </small>
                    </span>
                    <span className={`cdm__att cdm__att--${l.attendanceStatus.toLowerCase()}`}>
                      {ATTENDANCE_STATUS_LABELS[l.attendanceStatus]}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>

        <div className="cdm__foot">
          <button className="tcs-btn tcs-btn--primary" type="button" onClick={onClose}>
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
