import type { FormEvent } from 'react';
import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { profileApi } from '../api/profileApi';
import { ClientLayout } from '../components/ClientLayout';
import { extractApiErrorMessage } from '../hooks/useDependentProfile';
import {
  CHILD_PROFILE_LIMITS,
  buildChildUpdateRequest,
  validateUpdateChildForm,
} from '../utils/childProfileValidation';
import type { CatalogItem, ChildProfile, Gender } from '../types/profileTypes';
import './DependentProfileLinkerPage.css';

function formatDate(value?: string) {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('vi-VN');
}

function genderLabel(gender?: Gender) {
  if (gender === 'MALE') return 'Nam';
  if (gender === 'FEMALE') return 'Nữ';
  return '—';
}

export default function ChildProfileDetailPage() {
  const { childProfileId } = useParams<{ childProfileId: string }>();
  const navigate = useNavigate();
  const id = Number(childProfileId);

  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [child, setChild] = useState<ChildProfile | null>(null);
  const [grades, setGrades] = useState<CatalogItem[]>([]);
  const [mutationStatus, setMutationStatus] = useState<'idle' | 'loading' | 'error'>('idle');
  const [mutationError, setMutationError] = useState<string | null>(null);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);
  const [infoMessage, setInfoMessage] = useState<string | null>(null);

  const [fullName, setFullName] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [gender, setGender] = useState<Gender | ''>('');
  const [gradeId, setGradeId] = useState('');
  const [schoolName, setSchoolName] = useState('');
  const [notes, setNotes] = useState('');

  const isLinkedAccount = child?.linkedToUserAccount ?? false;

  const fillForm = useCallback((data: ChildProfile) => {
    setFullName(data.fullName ?? '');
    setDateOfBirth(data.dateOfBirth ? data.dateOfBirth.slice(0, 10) : '');
    setGender(data.gender ?? '');
    setGradeId(data.gradeId != null ? String(data.gradeId) : '');
    setSchoolName(data.schoolName ?? '');
    setNotes(data.notes ?? '');
  }, []);

  const reload = useCallback(async () => {
    if (!id || Number.isNaN(id)) {
      setErrorMessage('Hồ sơ con không hợp lệ.');
      setStatus('error');
      return;
    }
    setStatus('loading');
    setErrorMessage(null);
    try {
      const [childRes, gradesRes] = await Promise.all([
        profileApi.getChildById(id),
        profileApi.getGrades(),
      ]);
      setChild(childRes.data);
      setGrades(gradesRes.data);
      fillForm(childRes.data);
      setStatus('success');
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, 'Không tải được hồ sơ con.'));
      setStatus('error');
    }
  }, [fillForm, id]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!child) return;

    const validationError = validateUpdateChildForm({
      fullName: isLinkedAccount ? undefined : fullName,
      dateOfBirth: isLinkedAccount ? undefined : dateOfBirth || undefined,
      schoolName,
      notes,
      isLinkedAccount,
    });
    if (validationError) {
      setMutationError(validationError);
      setMutationStatus('error');
      setSavedMessage(null);
      setInfoMessage(null);
      return;
    }

    const updateBody = buildChildUpdateRequest(
      child,
      { fullName, dateOfBirth, gender, gradeId, schoolName, notes },
      isLinkedAccount,
    );
    if (!updateBody) {
      setMutationError(null);
      setMutationStatus('idle');
      setSavedMessage(null);
      setInfoMessage('Không có thay đổi nào để cập nhật.');
      return;
    }

    setMutationStatus('loading');
    setMutationError(null);
    setSavedMessage(null);
    setInfoMessage(null);

    try {
      const res = await profileApi.updateChild(child.childProfileId, updateBody);
      setChild(res.data);
      fillForm(res.data);
      setMutationStatus('idle');
      setSavedMessage('Đã lưu hồ sơ con.');
    } catch (error) {
      setMutationError(extractApiErrorMessage(error, 'Không thể cập nhật hồ sơ con.'));
      setMutationStatus('error');
    }
  }

  return (
    <ClientLayout
      title="Quản lý hồ sơ con"
      subtitle={
        isLinkedAccount
          ? 'Hồ sơ liên kết tài khoản đăng ký — thông tin cá nhân do học sinh quản lý.'
          : 'Hồ sơ con thủ công — phụ huynh có thể cập nhật toàn bộ thông tin.'
      }
    >
      {status === 'loading' && <div className="dpl-state">Đang tải hồ sơ con…</div>}

      {status === 'error' && (
        <div className="dpl-card">
          <div className="dpl-alert dpl-alert--error">{errorMessage}</div>
          <div className="dpl-actions">
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
              Thử lại
            </button>
            <Link className="tcs-btn tcs-btn--ghost" to={APP_ROUTES.profileDependents}>
              Quay lại
            </Link>
          </div>
        </div>
      )}

      {status === 'success' && child && (
        <section className="dpl-card">
          <div className="dpl-linked-card__header">
            {isLinkedAccount ? (
              <span className="tcs-badge tcs-badge--active">Tài khoản đăng ký</span>
            ) : (
              <span className="tcs-badge">Hồ sơ thủ công</span>
            )}
          </div>

          {mutationError && <div className="dpl-alert dpl-alert--error">{mutationError}</div>}
          {infoMessage && <div className="dpl-alert dpl-alert--info">{infoMessage}</div>}
          {savedMessage && <div className="dpl-alert dpl-alert--success">{savedMessage}</div>}

          <form className="dpl-form dpl-form--grid" onSubmit={handleSubmit}>
            <label>
              Họ tên con
              <input
                className="dpl-field"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                maxLength={CHILD_PROFILE_LIMITS.fullNameMax}
                readOnly={isLinkedAccount}
                required={!isLinkedAccount}
              />
            </label>

            {isLinkedAccount && (
              <label>
                Email
                <input
                  className="dpl-field dpl-field--readonly"
                  value={child.childEmail ?? '—'}
                  readOnly
                  aria-readonly
                />
              </label>
            )}

            <label>
              Ngày sinh
              <input
                className="dpl-field"
                type={isLinkedAccount ? 'text' : 'date'}
                value={isLinkedAccount ? formatDate(child.dateOfBirth) : dateOfBirth}
                max={isLinkedAccount ? undefined : new Date().toISOString().slice(0, 10)}
                onChange={(e) => setDateOfBirth(e.target.value)}
                readOnly={isLinkedAccount}
              />
            </label>

            <label>
              Giới tính
              {isLinkedAccount ? (
                <input className="dpl-field" value={genderLabel(child.gender)} readOnly />
              ) : (
                <select
                  className="dpl-field"
                  value={gender}
                  onChange={(e) => setGender(e.target.value as Gender | '')}
                >
                  <option value="">— Chọn —</option>
                  <option value="MALE">Nam</option>
                  <option value="FEMALE">Nữ</option>
                </select>
              )}
            </label>

            <div className="dpl-form__full dpl-form__grade-school-row">
              <label className="dpl-form__grade-col">
                Khối / Lớp
                <select
                  className="dpl-field"
                  value={gradeId}
                  onChange={(e) => setGradeId(e.target.value)}
                >
                  <option value="">— Chọn —</option>
                  {grades.map((grade) => (
                    <option key={grade.id} value={grade.id}>
                      {grade.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="dpl-form__school-col">
                Trường học
                <input
                  className="dpl-field"
                  value={schoolName}
                  onChange={(e) => setSchoolName(e.target.value)}
                  maxLength={CHILD_PROFILE_LIMITS.schoolNameMax}
                />
              </label>
            </div>

            <label className="dpl-form__full">
              Ghi chú
              <textarea
                className="dpl-field dpl-field--textarea"
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                maxLength={CHILD_PROFILE_LIMITS.notesMax}
              />
            </label>

            <div className="dpl-form__full dpl-actions">
              <button
                className="tcs-btn tcs-btn--primary"
                type="submit"
                disabled={mutationStatus === 'loading'}
              >
                {mutationStatus === 'loading' ? 'Đang lưu…' : 'Lưu thay đổi'}
              </button>
              <button
                className="tcs-btn tcs-btn--ghost"
                type="button"
                onClick={() => navigate(APP_ROUTES.profileDependents)}
              >
                Quay lại
              </button>
              <button
                className="tcs-btn tcs-btn--danger"
                type="button"
                disabled={mutationStatus === 'loading'}
                onClick={async () => {
                  const label = child.fullName || 'hồ sơ con này';
                  const message = isLinkedAccount
                    ? `Gỡ liên kết tài khoản "${label}" khỏi tài khoản của bạn? Tài khoản học sinh vẫn tồn tại trên hệ thống.`
                    : `Xóa hồ sơ con "${label}"? Hành động này không thể hoàn tác.`;
                  if (!window.confirm(message)) return;
                  setMutationStatus('loading');
                  setMutationError(null);
                  try {
                    await profileApi.deleteChild(child.childProfileId);
                    navigate(APP_ROUTES.profileDependents);
                  } catch (error) {
                    setMutationError(extractApiErrorMessage(error, 'Không thể xóa hồ sơ con.'));
                    setMutationStatus('error');
                  }
                }}
              >
                Xóa
              </button>
            </div>
          </form>
        </section>
      )}
    </ClientLayout>
  );
}
