import type { ClassBusyConflict } from '../types/marketplaceTypes';
import './tutorFindClass.css';

/** Đổi ngày ISO thành "dd/MM". */
const ddmm = (iso: string) => `${iso.slice(8, 10)}/${iso.slice(5, 7)}`;
/** Cắt chuỗi giờ về "HH:mm" (null -> rỗng). */
const hhmm = (t: string | null) => (t ?? '').slice(0, 5);

/**
 * Lớp có buổi trùng thời gian bận gia sư đã tự đăng ký — backend không cho ứng tuyển. Bỏ lịch bận
 * các ngày đó ở trang Đăng ký thời gian bận là ứng tuyển được.
 */
export function BusyConflictNotice({
  conflict,
  hint = 'Bạn chưa thể ứng tuyển lớp này. Hãy bỏ lịch bận các ngày trên ở trang Đăng ký thời gian bận rồi quay lại.',
}: {
  readonly conflict: ClassBusyConflict;
  readonly hint?: string;
}) {
  return (
    <div className="tfc-busy-notice" role="alert">
      <strong>
        Lớp này có {conflict.conflictCount} buổi trùng thời gian bận bạn đã đăng ký
      </strong>
      <ul>
        {conflict.conflicts.slice(0, 5).map((c) => (
          <li key={`${c.date}-${c.startTime}`}>
            {ddmm(c.date)}: buổi học {hhmm(c.startTime)}–{hhmm(c.endTime)} · bạn bận{' '}
            {c.allDay ? 'cả ngày' : `${hhmm(c.busyStartTime)}–${hhmm(c.busyEndTime)}`}
            {c.note ? ` (${c.note})` : ''}
          </li>
        ))}
        {conflict.conflicts.length > 5 && <li>và {conflict.conflicts.length - 5} buổi khác</li>}
      </ul>
      <span>{hint}</span>
    </div>
  );
}
