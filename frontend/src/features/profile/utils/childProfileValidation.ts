import type { ChildProfile, Gender, UpdateChildProfileRequest } from '../types/profileTypes';

export const CHILD_PROFILE_LIMITS = {
  fullNameMin: 2,
  fullNameMax: 100,
  schoolNameMax: 200,
  notesMax: 2000,
} as const;

function ageYears(dateOfBirth: string, onDate = new Date()): number {
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return -1;
  let age = onDate.getFullYear() - dob.getFullYear();
  const monthDiff = onDate.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && onDate.getDate() < dob.getDate())) {
    age -= 1;
  }
  return age;
}

export function validateChildFullName(fullName: string): string | null {
  const trimmed = fullName.trim().replace(/\s+/g, ' ');
  if (!trimmed) return 'Tên con là bắt buộc';
  if (trimmed.length < CHILD_PROFILE_LIMITS.fullNameMin) {
    return `Tên con phải có ít nhất ${CHILD_PROFILE_LIMITS.fullNameMin} ký tự`;
  }
  if (trimmed.length > CHILD_PROFILE_LIMITS.fullNameMax) {
    return `Tên con không được vượt quá ${CHILD_PROFILE_LIMITS.fullNameMax} ký tự`;
  }
  return null;
}

export function validateChildDateOfBirth(dateOfBirth: string | undefined, required = false): string | null {
  if (!dateOfBirth) {
    return required ? 'Ngày sinh là bắt buộc' : null;
  }
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return 'Ngày sinh không hợp lệ';
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (dob > today) return 'Ngày sinh không được ở tương lai';
  const age = ageYears(dateOfBirth);
  if (age >= 18) return 'Ngày sinh phải thuộc độ tuổi vị thành niên (dưới 18 tuổi)';
  if (age < 0) return 'Ngày sinh không hợp lệ';
  return null;
}

export function validateSchoolName(schoolName: string | undefined): string | null {
  if (!schoolName) return null;
  if (schoolName.trim().length > CHILD_PROFILE_LIMITS.schoolNameMax) {
    return `Tên trường không được vượt quá ${CHILD_PROFILE_LIMITS.schoolNameMax} ký tự`;
  }
  return null;
}

export function validateNotes(notes: string | undefined): string | null {
  if (!notes) return null;
  if (notes.trim().length > CHILD_PROFILE_LIMITS.notesMax) {
    return `Ghi chú không được vượt quá ${CHILD_PROFILE_LIMITS.notesMax} ký tự`;
  }
  return null;
}

export type CreateChildForm = {
  fullName: string;
  dateOfBirth?: string;
  schoolName?: string;
};

export type UpdateChildForm = {
  fullName?: string;
  dateOfBirth?: string;
  schoolName?: string;
  notes?: string;
  isLinkedAccount: boolean;
};

export function validateCreateChildForm(form: CreateChildForm): string | null {
  return (
    validateChildFullName(form.fullName) ??
    validateChildDateOfBirth(form.dateOfBirth, false) ??
    validateSchoolName(form.schoolName)
  );
}

export function validateUpdateChildForm(form: UpdateChildForm): string | null {
  if (!form.isLinkedAccount) {
    const nameError = form.fullName != null ? validateChildFullName(form.fullName) : null;
    if (nameError) return nameError;
    const dobError = validateChildDateOfBirth(form.dateOfBirth, false);
    if (dobError) return dobError;
  }
  return validateSchoolName(form.schoolName) ?? validateNotes(form.notes);
}

function normalizeText(value?: string | null): string {
  return (value ?? '').trim();
}

export type ChildProfileFormState = {
  fullName: string;
  dateOfBirth: string;
  gender: Gender | '';
  gradeId: string;
  schoolName: string;
  notes: string;
};

export function buildChildUpdateRequest(
  child: ChildProfile,
  form: ChildProfileFormState,
  isLinkedAccount: boolean,
): UpdateChildProfileRequest | null {
  const body: UpdateChildProfileRequest = {};
  let hasChanges = false;

  const formSchool = normalizeText(form.schoolName);
  if (formSchool !== normalizeText(child.schoolName)) {
    body.schoolName = formSchool;
    hasChanges = true;
  }

  const formNotes = normalizeText(form.notes);
  if (formNotes !== normalizeText(child.notes)) {
    body.notes = formNotes;
    hasChanges = true;
  }

  const formGradeId = form.gradeId ? Number(form.gradeId) : null;
  const childGradeId = child.gradeId ?? null;
  if (formGradeId !== childGradeId) {
    body.gradeId = formGradeId ?? 0;
    hasChanges = true;
  }

  if (!isLinkedAccount) {
    const formName = normalizeText(form.fullName).replace(/\s+/g, ' ');
    const childName = normalizeText(child.fullName).replace(/\s+/g, ' ');
    if (formName !== childName) {
      body.fullName = formName;
      hasChanges = true;
    }

    const childDob = child.dateOfBirth ? child.dateOfBirth.slice(0, 10) : '';
    if (form.dateOfBirth !== childDob) {
      body.dateOfBirth = form.dateOfBirth || undefined;
      hasChanges = true;
    }

    const formGender = form.gender || undefined;
    const childGender = child.gender ?? undefined;
    if (formGender !== childGender) {
      body.gender = formGender;
      hasChanges = true;
    }
  }

  return hasChanges ? body : null;
}
