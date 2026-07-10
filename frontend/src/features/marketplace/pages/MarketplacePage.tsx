import { useState } from 'react';
import axios from 'axios';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { useMarketplace } from '../hooks/useMarketplace';
import { ClassRequestForm } from '../components/ClassRequestForm';
import { classToForm, emptyForm } from '../mappers/marketplaceMapper';
import {
  CLASS_STATUS_LABELS,
  type ClassFormValues,
  type ClassRequestPayload,
  type ClassResponse,
} from '../types/marketplaceTypes';
import './MarketplacePage.css';

const currency = new Intl.NumberFormat('vi-VN');

type Mode = { kind: 'list' } | { kind: 'create' } | { kind: 'edit'; target: ClassResponse };

export default function MarketplacePage() {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';
  const {
    status,
    classes,
    subjects,
    grades,
    provinces,
    createClass,
    updateClass,
    publishClass,
  } = useMarketplace();

  const [mode, setMode] = useState<Mode>({ kind: 'list' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function openCreate() {
    setError(null);
    setMode({ kind: 'create' });
  }

  function openEdit(target: ClassResponse) {
    setError(null);
    setMode({ kind: 'edit', target });
  }

  async function handleSubmit(payload: ClassRequestPayload) {
    setSubmitting(true);
    setError(null);
    try {
      if (mode.kind === 'edit') {
        await updateClass(mode.target.classId, payload);
      } else {
        await createClass(payload);
      }
      setMode({ kind: 'list' });
    } catch (err) {
      setError(extractError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handlePublish(classId: number) {
    if (!window.confirm('Đăng lớp này để gia sư có thể ứng tuyển?')) return;
    try {
      await publishClass(classId);
    } catch (err) {
      window.alert(extractError(err));
    }
  }

  const initialForm: ClassFormValues =
    mode.kind === 'edit' ? classToForm(mode.target) : emptyForm();

  return (
    <div className="tcs-page mkt-page">
      <main>
        <div className="tcs-container mkt-container">
          <header className="mkt-header">
            <div>
              <span className="mkt-eyebrow">Lớp học gia sư</span>
              <h1 className="mkt-title">Yêu cầu tìm gia sư của tôi</h1>
              <p className="mkt-subtitle">
                Đăng nhu cầu tìm gia sư cho con em bạn — chọn môn, lớp, mục tiêu, học phí. Gia sư
                phù hợp sẽ ứng tuyển sau khi bạn đăng lớp.
              </p>
            </div>
            {mode.kind === 'list' && isClient && (
              <button type="button" className="mkt-btn mkt-btn--primary" onClick={openCreate}>
                + Tạo lớp mới
              </button>
            )}
          </header>

          {!isClient && (
            <div className="mkt-alert mkt-alert--info">
              Chỉ tài khoản Phụ huynh / Khách hàng (Client) mới có thể tạo yêu cầu tìm gia sư.
            </div>
          )}

          {mode.kind !== 'list' ? (
            <section className="mkt-card">
              <div className="mkt-card__head">
                <h2>{mode.kind === 'edit' ? 'Chỉnh sửa lớp' : 'Tạo lớp gia sư mới'}</h2>
              </div>
              <div className="mkt-card__body">
                <ClassRequestForm
                  initial={initialForm}
                  subjects={subjects}
                  grades={grades}
                  provinces={provinces}
                  isEdit={mode.kind === 'edit'}
                  submitting={submitting}
                  error={error}
                  onSubmit={handleSubmit}
                  onCancel={() => setMode({ kind: 'list' })}
                />
              </div>
            </section>
          ) : (
            <ClassList
              status={status}
              classes={classes}
              onEdit={openEdit}
              onPublish={handlePublish}
            />
          )}
        </div>
      </main>
    </div>
  );
}

interface ClassListProps {
  readonly status: ReturnType<typeof useMarketplace>['status'];
  readonly classes: ClassResponse[];
  readonly onEdit: (c: ClassResponse) => void;
  readonly onPublish: (classId: number) => void;
}

function ClassList({ status, classes, onEdit, onPublish }: ClassListProps) {
  if (status === 'loading') {
    return <div className="mkt-state">Đang tải danh sách lớp…</div>;
  }
  if (status === 'error') {
    return <div className="mkt-alert mkt-alert--error">Không tải được danh sách lớp.</div>;
  }
  if (classes.length === 0) {
    return (
      <div className="mkt-empty">
        Bạn chưa có lớp nào. Nhấn <strong>“Tạo lớp mới”</strong> để đăng yêu cầu tìm gia sư đầu tiên.
      </div>
    );
  }

  return (
    <div className="mkt-grid">
      {classes.map((c) => (
        <article key={c.classId} className="mkt-class-card">
          <div className="mkt-class-card__top">
            <span className={`mkt-status mkt-status--${c.status.toLowerCase()}`}>
              {CLASS_STATUS_LABELS[c.status] ?? c.status}
            </span>
            <span className="mkt-class-card__id">#{c.classId}</span>
          </div>
          <h3 className="mkt-class-card__title">{c.title}</h3>
          <dl className="mkt-class-card__meta">
            <div>
              <dt>Môn</dt>
              <dd>{c.subjectName ?? '—'}</dd>
            </div>
            <div>
              <dt>Lớp</dt>
              <dd>{c.gradeName ?? '—'}</dd>
            </div>
            <div>
              <dt>Hình thức</dt>
              <dd>{c.lessonMode === 'ONLINE' ? 'Online' : 'Offline'}</dd>
            </div>
            <div>
              <dt>Học phí/giờ</dt>
              <dd>{c.tuitionFee != null ? `${currency.format(c.tuitionFee)} đ` : '—'}</dd>
            </div>
          </dl>
          {c.learningGoal && <p className="mkt-class-card__goal">🎯 {c.learningGoal}</p>}
          {(c.locationName || c.address) && (
            <p className="mkt-class-card__loc">
              📍 {[c.address, c.locationName].filter(Boolean).join(', ')}
            </p>
          )}
          <div className="mkt-class-card__actions">
            {(c.status === 'DRAFT' || c.status === 'OPEN') && (
              <button type="button" className="mkt-btn mkt-btn--ghost" onClick={() => onEdit(c)}>
                Sửa
              </button>
            )}
            {c.status === 'DRAFT' && (
              <button
                type="button"
                className="mkt-btn mkt-btn--primary"
                onClick={() => onPublish(c.classId)}
              >
                Đăng lớp
              </button>
            )}
          </div>
        </article>
      ))}
    </div>
  );
}

function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  if (err instanceof Error) return err.message;
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}
