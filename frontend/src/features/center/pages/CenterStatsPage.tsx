import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { CenterSidebar } from '../components/CenterSidebar';
import { centerApi } from '../api/centerApi';
import type { CenterStats } from '../types/centerTypes';

const C_PRESENT = '#16a34a';
const C_ABSENT = '#dc2626';
const C_EXCUSED = '#f59e0b';

function StackedBar({ present, absent, excused }: { present: number; absent: number; excused: number }) {
  const total = present + absent + excused;
  const pct = (n: number) => (total <= 0 ? 0 : (n / total) * 100);
  return (
    <div
      style={{
        display: 'flex',
        height: 14,
        borderRadius: 7,
        overflow: 'hidden',
        background: '#eef2f7',
        minWidth: 120,
      }}
      title={`Có mặt ${present} · Vắng ${absent} · Có phép ${excused}`}
    >
      <div style={{ width: `${pct(present)}%`, background: C_PRESENT }} />
      <div style={{ width: `${pct(absent)}%`, background: C_ABSENT }} />
      <div style={{ width: `${pct(excused)}%`, background: C_EXCUSED }} />
    </div>
  );
}

function Card({ label, value, color }: { label: string; value: string | number; color?: string }) {
  return (
    <div
      style={{
        flex: '1 1 130px',
        background: '#fff',
        border: '1px solid var(--color-border, #e5e7eb)',
        borderRadius: 12,
        padding: '12px 14px',
      }}
    >
      <div style={{ fontSize: 12, color: '#6b7280', fontWeight: 600 }}>{label}</div>
      <div style={{ fontSize: 22, fontWeight: 800, color: color ?? '#111827' }}>{value}</div>
    </div>
  );
}

export default function CenterStatsPage() {
  const [data, setData] = useState<CenterStats | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [tutorFilter, setTutorFilter] = useState<string>('all');
  const [classFilter, setClassFilter] = useState<string>('all');
  const [studentQuery, setStudentQuery] = useState<string>('');

  useEffect(() => {
    centerApi
      .getStats()
      .then((r) => {
        setData(r.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(
          axios.isAxiosError(err) && typeof err.response?.data?.message === 'string'
            ? err.response.data.message
            : 'Không tải được thống kê.',
        );
        setStatus('error');
      });
  }, []);

  const tutors = useMemo(() => {
    const m = new Map<number, string>();
    data?.classes.forEach((c) => {
      if (c.tutorId != null) m.set(c.tutorId, c.tutorName ?? `GV #${c.tutorId}`);
    });
    return Array.from(m.entries());
  }, [data]);

  const filteredClasses = useMemo(() => {
    let list = data?.classes ?? [];
    if (tutorFilter !== 'all') list = list.filter((c) => String(c.tutorId) === tutorFilter);
    if (classFilter !== 'all') list = list.filter((c) => String(c.classId) === classFilter);
    return list;
  }, [data, tutorFilter, classFilter]);

  const filteredStudents = useMemo(() => {
    let list = data?.students ?? [];
    if (tutorFilter !== 'all') list = list.filter((s) => String(s.tutorId) === tutorFilter);
    if (classFilter !== 'all') list = list.filter((s) => String(s.classId) === classFilter);
    const q = studentQuery.trim().toLowerCase();
    if (q) list = list.filter((s) => (s.studentName ?? '').toLowerCase().includes(q));
    return list;
  }, [data, tutorFilter, classFilter, studentQuery]);

  const t = data?.totals;
  const selInput: React.CSSProperties = {
    padding: '6px 10px',
    borderRadius: 8,
    border: '1px solid #d1d5db',
    fontSize: 13,
  };
  const th: React.CSSProperties = {
    textAlign: 'left',
    padding: '8px 10px',
    fontSize: 12,
    color: '#6b7280',
    borderBottom: '1px solid #e5e7eb',
  };
  const td: React.CSSProperties = { padding: '8px 10px', fontSize: 13, borderBottom: '1px solid #f1f5f9' };

  return (
    <>
      <VerificationHeader />
      <div className="cc-area-bg">
      <div className="cc-shell">
      <CenterSidebar />
      <div className="cc-shell__main">
      <div style={{ width: '100%' }}>
        <h1 style={{ fontSize: 24, fontWeight: 800, margin: '0 0 4px' }}>Thống kê lớp học</h1>
        <p style={{ color: '#6b7280', margin: '0 0 16px', fontSize: 14 }}>
          Tình trạng điểm danh (có mặt / vắng / có phép) theo lớp và học sinh, kèm số liệu tổng hợp.
        </p>

        {status === 'loading' && <div>Đang tải…</div>}
        {status === 'error' && (
          <div style={{ background: '#fef2f2', color: '#b91c1c', padding: 12, borderRadius: 8 }}>
            {error}
          </div>
        )}

        {status === 'success' && t && (
          <>
            {/* Số liệu tổng hợp */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 14 }}>
              <Card label="Tổng số lớp" value={t.classCount} />
              <Card label="Đang chạy" value={t.activeClassCount} />
              <Card label="Đã hoàn thành" value={t.completedClassCount} />
              <Card label="Học sinh" value={t.studentCount} />
              <Card label="Tỉ lệ có mặt" value={`${t.attendanceRate}%`} color={C_PRESENT} />
            </div>

            {/* Phân bố điểm danh tổng */}
            <div
              style={{
                background: '#fff',
                border: '1px solid #e5e7eb',
                borderRadius: 12,
                padding: 14,
                marginBottom: 18,
              }}
            >
              <div style={{ display: 'flex', gap: 18, marginBottom: 8, fontSize: 13, fontWeight: 600 }}>
                <span style={{ color: C_PRESENT }}>● Có mặt: {t.present}</span>
                <span style={{ color: C_ABSENT }}>● Vắng: {t.absent}</span>
                <span style={{ color: C_EXCUSED }}>● Có phép: {t.excused}</span>
                <span style={{ color: '#6b7280' }}>Tổng lượt điểm danh: {t.totalMarks}</span>
              </div>
              <StackedBar present={t.present} absent={t.absent} excused={t.excused} />
            </div>

            {/* Bộ lọc */}
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 12 }}>
              <label style={{ fontSize: 13 }}>
                Giáo viên:{' '}
                <select style={selInput} value={tutorFilter} onChange={(e) => setTutorFilter(e.target.value)}>
                  <option value="all">Tất cả</option>
                  {tutors.map(([id, name]) => (
                    <option key={id} value={String(id)}>
                      {name}
                    </option>
                  ))}
                </select>
              </label>
              <label style={{ fontSize: 13 }}>
                Lớp:{' '}
                <select style={selInput} value={classFilter} onChange={(e) => setClassFilter(e.target.value)}>
                  <option value="all">Tất cả</option>
                  {(data?.classes ?? []).map((c) => (
                    <option key={c.classId} value={String(c.classId)}>
                      {c.title}
                    </option>
                  ))}
                </select>
              </label>
              <label style={{ fontSize: 13 }}>
                Học sinh:{' '}
                <input
                  style={selInput}
                  type="text"
                  placeholder="Tìm theo tên…"
                  value={studentQuery}
                  onChange={(e) => setStudentQuery(e.target.value)}
                />
              </label>
            </div>

            {/* Biểu đồ theo lớp */}
            <h2 style={{ fontSize: 16, fontWeight: 700, margin: '10px 0 8px' }}>Điểm danh theo lớp</h2>
            <div
              style={{
                background: '#fff',
                border: '1px solid #e5e7eb',
                borderRadius: 12,
                padding: 14,
                marginBottom: 20,
              }}
            >
              {filteredClasses.length === 0 ? (
                <div style={{ color: '#6b7280', fontSize: 13 }}>Không có lớp phù hợp bộ lọc.</div>
              ) : (
                filteredClasses.map((c) => (
                  <div
                    key={c.classId}
                    style={{ display: 'grid', gridTemplateColumns: '1fr 2fr auto', gap: 12, alignItems: 'center', padding: '6px 0' }}
                  >
                    <div style={{ fontSize: 13, fontWeight: 600 }}>
                      {c.title}
                      {c.tutorName ? <span style={{ color: '#9ca3af', fontWeight: 400 }}> · {c.tutorName}</span> : null}
                    </div>
                    <StackedBar present={c.present} absent={c.absent} excused={c.excused} />
                    <div style={{ fontSize: 12, color: '#6b7280', whiteSpace: 'nowrap' }}>
                      {c.attendanceRate}% có mặt
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Bảng theo học sinh */}
            <h2 style={{ fontSize: 16, fontWeight: 700, margin: '10px 0 8px' }}>Chi tiết theo học sinh</h2>
            <div style={{ overflowX: 'auto', background: '#fff', border: '1px solid #e5e7eb', borderRadius: 12 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    <th style={th}>Học sinh</th>
                    <th style={th}>Lớp</th>
                    <th style={th}>Giáo viên</th>
                    <th style={{ ...th, textAlign: 'center', color: C_PRESENT }}>Có mặt</th>
                    <th style={{ ...th, textAlign: 'center', color: C_ABSENT }}>Vắng</th>
                    <th style={{ ...th, textAlign: 'center', color: C_EXCUSED }}>Có phép</th>
                    <th style={{ ...th, textAlign: 'center' }}>Tỉ lệ</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStudents.length === 0 ? (
                    <tr>
                      <td style={td} colSpan={7}>
                        Không có học sinh phù hợp bộ lọc.
                      </td>
                    </tr>
                  ) : (
                    filteredStudents.map((s) => (
                      <tr key={`${s.classId}-${s.classStudentId}`}>
                        <td style={td}>{s.studentName}</td>
                        <td style={td}>{s.className}</td>
                        <td style={td}>{s.tutorName ?? '—'}</td>
                        <td style={{ ...td, textAlign: 'center' }}>{s.present}</td>
                        <td style={{ ...td, textAlign: 'center' }}>{s.absent}</td>
                        <td style={{ ...td, textAlign: 'center' }}>{s.excused}</td>
                        <td style={{ ...td, textAlign: 'center', fontWeight: 700 }}>{s.attendanceRate}%</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
      </div>
      </div>
      </div>
    </>
  );
}
