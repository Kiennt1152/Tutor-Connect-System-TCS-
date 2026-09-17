import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { SiteHeader } from '../../home/components/SiteHeader';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { MIDNIGHT_END, hhmm, slotOverlaps, toIsoDate } from '../../../shared/utils/format';
import { teachingApi } from '../api/teachingApi';
import { busyTimeApi } from '../api/busyTimeApi';
import type { BusyTimeResponse, LessonResponse } from '../types/teachingTypes';
import './BusyTimePage.css';

/** Tuần bắt đầu từ thứ 2, như lịch treo tường Việt Nam. */
const WEEKDAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

type Session = 'MORNING' | 'AFTERNOON' | 'EVENING';

/** Buổi trùng khung với form đăng lớp (SESSION_OPTIONS) để gia sư và phụ huynh nói cùng một ngôn ngữ. */
const SESSIONS: readonly { value: Session; label: string; start: string; end: string }[] = [
  { value: 'MORNING', label: 'Sáng (6h–12h)', start: '06:00', end: '12:00' },
  { value: 'AFTERNOON', label: 'Chiều (12h–18h)', start: '12:00', end: '18:00' },
  // 00:00 ở giờ kết thúc là nửa đêm (24:00) của chính ngày đó.
  { value: 'EVENING', label: 'Tối (18h–0h)', start: '18:00', end: MIDNIGHT_END },
];

const pad = (n: number) => String(n).padStart(2, '0');
const monthKey = (year: number, month0: number) => `${year}-${pad(month0 + 1)}`;
const ddmm = (iso: string) => `${iso.slice(8, 10)}/${iso.slice(5, 7)}`;

function rangeLabel(start: string | null, end: string | null): string {
  if (!start || !end) return 'Cả ngày';
  return `${hhmm(start)}–${hhmm(end)}`;
}

export default function BusyTimePage() {
  const today = toIsoDate(new Date());
  const [view, setView] = useState(() => {
    const now = new Date();
    return { year: now.getFullYear(), month0: now.getMonth() };
  });

  const [lessons, setLessons] = useState<LessonResponse[]>([]);
  const [busy, setBusy] = useState<BusyTimeResponse[]>([]);
  const [busyStatus, setBusyStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadError, setLoadError] = useState('');

  const [selected, setSelected] = useState<Set<string>>(new Set());
  // Chọn được nhiều buổi (vd. sáng + tối, rảnh chiều). Đủ cả 3 buổi = bận cả ngày.
  // Mặc định chưa tick buổi nào: gia sư tự chọn buổi bận cho ngày vừa bấm.
  const [sessions, setSessions] = useState<Set<Session>>(() => new Set());
  const [note, setNote] = useState('');
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const key = monthKey(view.year, view.month0);

  // Lịch dạy và lịch bận tải RIÊNG: một bên lỗi (vd. backend cũ chưa có API lịch bận) không
  // được làm mất bên kia.
  useEffect(() => {
    teachingApi
      .listMyLessons()
      .then(setLessons)
      .catch(() => setLessons([]));
  }, []);

  const loadBusy = useCallback(() => {
    setBusyStatus('loading');
    busyTimeApi
      .listByMonth(key)
      .then((rows) => {
        setBusy(rows);
        setBusyStatus('ready');
      })
      .catch((e) => {
        setBusy([]);
        setLoadError(getApiErrorMessage(e, 'Không tải được lịch bận.'));
        setBusyStatus('error');
      });
  }, [key]);

  useEffect(() => {
    loadBusy();
  }, [loadBusy]);

  // Đổi tháng thì bỏ chọn — ngày của tháng cũ không còn hiện trên lưới.
  useEffect(() => {
    setSelected(new Set());
  }, [key]);

  /** Các ô của lưới: null là ô đệm trước ngày 1. */
  const cells = useMemo(() => {
    const first = new Date(view.year, view.month0, 1);
    const offset = (first.getDay() + 6) % 7;
    const days = new Date(view.year, view.month0 + 1, 0).getDate();
    const out: (string | null)[] = Array.from({ length: offset }, () => null);
    for (let d = 1; d <= days; d += 1) out.push(`${key}-${pad(d)}`);
    while (out.length % 7 !== 0) out.push(null);
    return out;
  }, [view, key]);

  const lessonsByDate = useMemo(() => {
    const map = new Map<string, LessonResponse[]>();
    for (const l of lessons) {
      if (!l.lessonDate?.startsWith(key)) continue;
      const list = map.get(l.lessonDate) ?? [];
      list.push(l);
      map.set(l.lessonDate, list);
    }
    for (const list of map.values()) list.sort((a, b) => a.startTime.localeCompare(b.startTime));
    return map;
  }, [lessons, key]);

  const busyByDate = useMemo(() => {
    const map = new Map<string, BusyTimeResponse[]>();
    for (const b of busy) {
      const list = map.get(b.busyDate) ?? [];
      list.push(b);
      map.set(b.busyDate, list);
    }
    return map;
  }, [busy]);

  const busyDays = [...busyByDate.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, rows]) => [
      date,
      [...rows].sort((x, y) => (x.startTime ?? '').localeCompare(y.startTime ?? '')),
    ] as const);

  const monthDays = cells.filter((c): c is string => c !== null);
  const upcomingDays = monthDays.filter((d) => d >= today);
  const summary = {
    teaching: upcomingDays.filter((d) => lessonsByDate.has(d)).length,
    busy: upcomingDays.filter((d) => busyByDate.has(d)).length,
    free: upcomingDays.filter((d) => !lessonsByDate.has(d) && !busyByDate.has(d)).length,
  };

  const allDay = sessions.size === SESSIONS.length;
  const noSession = sessions.size === 0;
  // Gộp các buổi liền nhau thành một khoảng: sáng + chiều = 06:00–18:00, sáng + tối = 2 khoảng.
  const ranges = SESSIONS.filter((s) => sessions.has(s.value)).reduce<{ start: string; end: string }[]>(
    (acc, s) => {
      const last = acc[acc.length - 1];
      if (last && last.end === s.start) last.end = s.end;
      else acc.push({ start: s.start, end: s.end });
      return acc;
    },
    [],
  );
  const chosenLabel = allDay ? 'Cả ngày' : ranges.map((r) => rangeLabel(r.start, r.end)).join(' + ');

  const toggleSession = (value: Session) =>
    setSessions((prev) => {
      const next = new Set(prev);
      if (next.has(value)) next.delete(value);
      else next.add(value);
      return next;
    });
  const toggleAllDay = () =>
    setSessions(allDay ? new Set() : new Set(SESSIONS.map((s) => s.value)));

  const selectedList = [...selected].sort();

  /** Trùng khung đã chọn: bận cả ngày đụng mọi thứ, còn lại so theo phút (có tính nửa đêm). */
  const hits = (start: string | null, end: string | null) =>
    allDay ||
    start === null ||
    end === null ||
    ranges.some((r) => slotOverlaps(r.start, r.end, hhmm(start), hhmm(end)));

  const busyClashes = selectedList.filter((d) =>
    (busyByDate.get(d) ?? []).some((b) => hits(b.startTime, b.endTime)),
  );
  const lessonClashes = selectedList.filter((d) =>
    (lessonsByDate.get(d) ?? []).some((l) => hits(l.startTime, l.endTime)),
  );

  const toggleDay = (iso: string) => {
    if (iso < today) return;
    setNotice('');
    setError('');
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(iso)) next.delete(iso);
      else next.add(iso);
      return next;
    });
  };

  /** Chọn nhanh mọi ngày cùng thứ trong tháng (còn tới); bấm lần nữa để bỏ. */
  const toggleWeekday = (weekdayIndex: number) => {
    const targets = upcomingDays.filter((d) => {
      const [y, m, day] = d.split('-').map(Number);
      return (new Date(y, m - 1, day).getDay() + 6) % 7 === weekdayIndex;
    });
    if (targets.length === 0) return;
    setNotice('');
    setError('');
    setSelected((prev) => {
      const next = new Set(prev);
      const allOn = targets.every((d) => next.has(d));
      for (const d of targets) {
        if (allOn) next.delete(d);
        else next.add(d);
      }
      return next;
    });
  };

  const shiftMonth = (delta: number) => {
    setNotice('');
    setError('');
    setView((v) => {
      const d = new Date(v.year, v.month0 + delta, 1);
      return { year: d.getFullYear(), month0: d.getMonth() };
    });
  };

  const submit = async () => {
    if (selectedList.length === 0 || noSession || busyClashes.length > 0) return;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const created = await busyTimeApi.create({
        dates: selectedList,
        startTime: null,
        endTime: null,
        ranges: allDay ? undefined : ranges.map((r) => ({ startTime: r.start, endTime: r.end })),
        note: note.trim() || undefined,
      });
      // Backend cũ (chưa restart) không biết `ranges` nên lưu nhầm thành cả ngày. Chọn lẻ buổi mà
      // nhận về dòng "cả ngày" thì gỡ ngay các dòng đó và báo, không để dữ liệu sai nằm lại.
      const wrong = allDay ? [] : created.filter((b) => b.allDay);
      if (wrong.length > 0) {
        await Promise.allSettled(wrong.map((b) => busyTimeApi.remove(b.busyTimeId)));
        setError(
          'Máy chủ chưa cập nhật tính năng chọn nhiều buổi nên đã lưu nhầm thành cả ngày — đã tự huỷ. ' +
            'Hãy khởi động lại backend rồi đánh dấu lại.',
        );
        loadBusy();
        return;
      }
      setNotice(
        `Đã đánh dấu bận ${selectedList.length} ngày (${chosenLabel}) — ${created.length} khung giờ.`,
      );
      setSelected(new Set());
      setNote('');
      loadBusy();
    } catch (e) {
      setError(getApiErrorMessage(e, 'Không lưu được lịch bận.'));
    } finally {
      setSaving(false);
    }
  };

  /** Xoá mọi khung giờ bận của một ngày — ngày đó quay về rảnh. */
  const removeBusyDay = async (date: string, rows: readonly BusyTimeResponse[]) => {
    setError('');
    setNotice('');
    const results = await Promise.allSettled(rows.map((b) => busyTimeApi.remove(b.busyTimeId)));
    const removedIds = new Set(
      rows.filter((_, i) => results[i].status === 'fulfilled').map((b) => b.busyTimeId),
    );
    setBusy((prev) => prev.filter((x) => !removedIds.has(x.busyTimeId)));
    const failed = results.find((r): r is PromiseRejectedResult => r.status === 'rejected');
    if (failed) {
      setError(getApiErrorMessage(failed.reason, 'Không xoá được hết lịch bận của ngày này.'));
    } else {
      setNotice(`Đã bỏ lịch bận ngày ${ddmm(date)} — ngày đó quay về rảnh.`);
    }
  };


  return (
    <div className="bsy-page">
      <SiteHeader />
      <main className="tcs-container bsy-main">
        {/* Tách hẳn khỏi lịch dạy: quay về trang chủ chứ không về /teaching. */}
        <Link className="bsy-back" to={APP_ROUTES.home}>
          ← Trang chủ
        </Link>
        <div className="bsy-heading">
          <h1>Đăng ký thời gian bận</h1>
          <p>
            Chỉ cần đánh dấu lúc bạn <strong>bận</strong>. Ngày nào không có lịch dạy và không đánh dấu
            bận thì hệ thống tự hiểu là <strong>rảnh</strong>.
          </p>
        </div>

        {notice && <div className="bsy-alert bsy-alert--ok">{notice}</div>}
        {error && <div className="bsy-alert bsy-alert--err">{error}</div>}
        {busyStatus === 'error' && <div className="bsy-alert bsy-alert--err">{loadError}</div>}

        <div className="bsy-layout">
          <section className="bsy-calendar" aria-label="Lịch tháng">
            <div className="bsy-calendar__bar">
              <button type="button" className="bsy-nav" onClick={() => shiftMonth(-1)} aria-label="Tháng trước">
                ‹
              </button>
              <h2 className="bsy-calendar__title">
                Tháng {view.month0 + 1}/{view.year}
              </h2>
              <button type="button" className="bsy-nav" onClick={() => shiftMonth(1)} aria-label="Tháng sau">
                ›
              </button>
            </div>

            <div className="bsy-summary">
              <span className="bsy-legend bsy-legend--teach">{summary.teaching} ngày có lịch dạy</span>
              <span className="bsy-legend bsy-legend--busy">{summary.busy} ngày bận</span>
              <span className="bsy-legend bsy-legend--free">{summary.free} ngày rảnh</span>
            </div>

            <div className="bsy-grid">
              {WEEKDAY_LABELS.map((w, i) => (
                <button
                  key={w}
                  type="button"
                  className="bsy-weekday"
                  onClick={() => toggleWeekday(i)}
                  title={`Chọn tất cả ${w} còn lại trong tháng`}
                >
                  {w}
                </button>
              ))}

              {cells.map((iso, i) => {
                if (!iso) return <div key={`pad-${i}`} className="bsy-cell bsy-cell--pad" />;
                const dayLessons = lessonsByDate.get(iso) ?? [];
                const dayBusy = busyByDate.get(iso) ?? [];
                const past = iso < today;
                const isSelected = selected.has(iso);
                const free = dayLessons.length === 0 && dayBusy.length === 0;
                const items = [
                  ...dayLessons.map((l) => ({
                    k: `l${l.lessonId}`,
                    tone: 'teach',
                    text: `Dạy ${rangeLabel(l.startTime, l.endTime)}`,
                  })),
                  ...dayBusy.map((b) => ({
                    k: `b${b.busyTimeId}`,
                    tone: 'busy',
                    text: b.allDay ? 'Bận cả ngày' : `Bận ${rangeLabel(b.startTime, b.endTime)}`,
                  })),
                ];
                return (
                  <button
                    key={iso}
                    type="button"
                    aria-pressed={isSelected}
                    aria-label={`Ngày ${ddmm(iso)}${free ? ', rảnh' : ''}`}
                    disabled={past}
                    onClick={() => toggleDay(iso)}
                    className={[
                      'bsy-cell',
                      past ? 'bsy-cell--past' : '',
                      iso === today ? 'bsy-cell--today' : '',
                      isSelected ? 'bsy-cell--selected' : '',
                      dayBusy.length > 0 ? 'bsy-cell--busy' : '',
                    ]
                      .filter(Boolean)
                      .join(' ')}
                  >
                    <span className="bsy-cell__day">{Number(iso.slice(8, 10))}</span>
                    {items.slice(0, 2).map((it) => (
                      <span key={it.k} className={`bsy-tag bsy-tag--${it.tone}`}>
                        {it.text}
                      </span>
                    ))}
                    {items.length > 2 && <span className="bsy-tag bsy-tag--more">+{items.length - 2}</span>}
                    {free && !past && busyStatus === 'ready' && <span className="bsy-tag bsy-tag--free">Rảnh</span>}
                  </button>
                );
              })}
            </div>
            <p className="bsy-hint">
              Bấm vào ngày để chọn (chọn được nhiều ngày). Bấm vào <strong>T2…CN</strong> để chọn nhanh cả
              thứ đó trong tháng.
            </p>
          </section>

          <aside className="bsy-panel">
            <h3 className="bsy-panel__title">Đánh dấu bận</h3>

            <div className="bsy-panel__block">
              <div className="bsy-panel__row">
                <span className="bsy-label">Ngày đã chọn</span>
                {selectedList.length > 0 && (
                  <button type="button" className="bsy-link" onClick={() => setSelected(new Set())}>
                    Bỏ chọn
                  </button>
                )}
              </div>
              {selectedList.length === 0 ? (
                <p className="bsy-muted">Chưa chọn ngày nào trên lịch.</p>
              ) : (
                <div className="bsy-chips">
                  {selectedList.map((d) => (
                    <button
                      key={d}
                      type="button"
                      className="bsy-chip"
                      onClick={() => toggleDay(d)}
                      aria-label={`Bỏ chọn ngày ${ddmm(d)}`}
                    >
                      {ddmm(d)} ✕
                    </button>
                  ))}
                </div>
              )}
            </div>

            <fieldset className="bsy-panel__block bsy-modes">
              <legend className="bsy-label">Khung giờ bận (chọn được nhiều buổi)</legend>
              <label className={`bsy-mode bsy-mode--all${allDay ? ' is-active' : ''}`}>
                <input type="checkbox" checked={allDay} onChange={toggleAllDay} />
                Cả ngày
              </label>
              {/* Chưa đủ 3 buổi thì hiện từng buổi để tick. Tick đủ Sáng + Chiều + Tối là chuyển hẳn
                  thành một ô "Cả ngày" (ẩn 3 buổi, không để 4 ô cùng tick); bỏ tick "Cả ngày" để chọn lẻ lại. */}
              {!allDay &&
                SESSIONS.map((m) => (
                  <label key={m.value} className={`bsy-mode${sessions.has(m.value) ? ' is-active' : ''}`}>
                    <input
                      type="checkbox"
                      value={m.value}
                      checked={sessions.has(m.value)}
                      onChange={() => toggleSession(m.value)}
                    />
                    {m.label}
                  </label>
                ))}
              {noSession ? (
                <p className="bsy-warn">Chọn ít nhất một buổi bận.</p>
              ) : (
                <p className="bsy-muted">
                  Sẽ lưu: <strong>{chosenLabel}</strong>
                </p>
              )}
            </fieldset>

            <label className="bsy-panel__block">
              <span className="bsy-label">Ghi chú (không bắt buộc)</span>
              <input
                className="bsy-input"
                value={note}
                maxLength={255}
                placeholder="VD: đi thi, về quê…"
                onChange={(e) => setNote(e.target.value)}
              />
            </label>

            {busyClashes.length > 0 && (
              <p className="bsy-warn">
                Ngày {busyClashes.map(ddmm).join(', ')} đã có lịch bận trùng giờ — bỏ chọn hoặc xoá lịch cũ trước.
              </p>
            )}
            {lessonClashes.length > 0 && (
              <p className="bsy-info">
                Lưu ý: ngày {lessonClashes.map(ddmm).join(', ')} bạn đang có lịch dạy trong khung giờ này. Đánh
                dấu bận không tự huỷ buổi dạy — cần đổi lịch với phụ huynh.
              </p>
            )}

            <button
              type="button"
              className="bsy-submit"
              disabled={saving || selectedList.length === 0 || noSession || busyClashes.length > 0}
              onClick={submit}
            >
              {saving
                ? 'Đang lưu…'
                : selectedList.length > 0
                  ? `Đánh dấu bận ${selectedList.length} ngày`
                  : 'Đánh dấu bận'}
            </button>

            <div className="bsy-panel__block bsy-list">
              <span className="bsy-label">Lịch bận tháng {view.month0 + 1}</span>
              {busyStatus === 'loading' ? (
                <p className="bsy-muted">Đang tải…</p>
              ) : busy.length === 0 ? (
                <p className="bsy-muted">Chưa có ngày bận nào — cả tháng đang rảnh ngoài lịch dạy.</p>
              ) : (
                <ul>
                  {/* Mỗi ngày một dòng: gộp các khung giờ bận của ngày đó, vd. "Bận 06:00–12:00 + 18:00–00:00". */}
                  {busyDays.map(([date, rows]) => {
                    const notes = [...new Set(rows.map((b) => b.note).filter(Boolean))];
                    return (
                      <li key={date}>
                        <span className="bsy-list__date">{ddmm(date)}</span>
                        <span className="bsy-list__time">
                          {rows.some((b) => b.allDay)
                            ? 'Bận cả ngày'
                            : `Bận ${rows.map((b) => rangeLabel(b.startTime, b.endTime)).join(' + ')}`}
                          {notes.length > 0 && <em> · {notes.join(', ')}</em>}
                        </span>
                        {date >= today && (
                          <button
                            type="button"
                            className="bsy-list__del"
                            onClick={() => removeBusyDay(date, rows)}
                            aria-label={`Xoá lịch bận ngày ${ddmm(date)}`}
                          >
                            ✕
                          </button>
                        )}
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}
