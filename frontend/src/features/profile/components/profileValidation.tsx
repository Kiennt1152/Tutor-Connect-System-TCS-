import { VIETNAM_PHONE } from './profileConstants';

export function validatePhone(phone: string): string | null {
  if (!phone) return null;
  if (!VIETNAM_PHONE.test(phone.replace(/\s/g, ''))) {
    return 'Số điện thoại không hợp lệ (10 số, đầu 0 hoặc +84)';
  }
  return null;
}

export function validateDateOfBirth(dob: string): string | null {
  if (!dob) return null;
  const d = new Date(dob);
  if (Number.isNaN(d.getTime())) return 'Ngày sinh không hợp lệ';
  if (d > new Date()) return 'Ngày sinh không thể vượt quá hôm nay';
  return null;
}

export function validatePersonName(name: string): string | null {
  const v = name.trim();
  if (!v) return null;
  if (/[^a-zA-ZÀ-ỹ\s]/.test(v)) {
    return 'Họ và tên không được chứa ký tự đặc biệt hoặc số';
  }
  return null;
}

export function validateLegalName(name: string): string | null {
  const v = name.trim();
  if (v.length < 2 || v.length > 50) return 'Phải từ 2 đến 50 ký tự';
  return null;
}

export function validateAddress(address: string): string | null {
  if (address && address.length > 255) return 'Địa chỉ tối đa 255 ký tự';
  return null;
}

export function validateBio(bio: string): string | null {
  if (bio && bio.length > 1000) return 'Mô tả tối đa 1000 ký tự';
  return null;
}

export function validateExperienceYears(value: string): string | null {
  if (!value) return null;
  const n = Number(value);
  if (!Number.isInteger(n) || n < 0 || n > 60) {
    return 'Số năm kinh nghiệm không hợp lệ (0-60)';
  }
  return null;
}

export function validateHourlyRate(value: string): string | null {
  if (!value) return null;
  const n = Number(value);
  if (!Number.isFinite(n) || n < 0) return 'Học phí không hợp lệ';
  return null;
}
