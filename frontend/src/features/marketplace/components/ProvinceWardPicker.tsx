import { useEffect, useMemo, useState } from 'react';
import { provinceKey, wardKey } from '../matching/tutorMatching';

const GEO_API = 'https://provinces.open-api.vn/api/v2';

interface GeoItem {
  code: number;
  name: string;
}

interface Props {
  readonly province: string;
  readonly ward: string;
  readonly disabled?: boolean;
  readonly onChange: (value: { province: string; ward: string }) => void;
}

function sortByName(list: GeoItem[]): GeoItem[] {
  return [...list].sort((a, b) => a.name.localeCompare(b.name, 'vi'));
}

/**
 * Hai ô chọn Tỉnh/Thành phố → Phường/Xã cho màn "Gia sư tìm lớp".
 *
 * Dùng chung nguồn dữ liệu với form đăng tin của khách (provinces.open-api.vn) để tên
 * địa danh hai bên khớp nhau. Giá trị vào có thể là tên rút gọn ("Hà Nội") do câu tìm
 * bóc ra, nên chỗ chọn đối chiếu bằng {@link provinceKey}/{@link wardKey} chứ không so
 * chuỗi thô — nếu không select sẽ hiện rỗng dù đã có tỉnh.
 */
export function ProvinceWardPicker({ province, ward, disabled, onChange }: Props) {
  const [provinces, setProvinces] = useState<GeoItem[]>([]);
  const [wards, setWards] = useState<GeoItem[]>([]);

  useEffect(() => {
    let alive = true;
    fetch(`${GEO_API}/p/`)
      .then((r) => r.json())
      .then((list: GeoItem[]) => alive && setProvinces(sortByName(list)))
      .catch(() => alive && setProvinces([]));
    return () => {
      alive = false;
    };
  }, []);

  const selectedProvince = useMemo(() => {
    const key = provinceKey(province);
    if (!key) return null;
    return provinces.find((p) => provinceKey(p.name) === key) ?? null;
  }, [provinces, province]);

  useEffect(() => {
    if (!selectedProvince) {
      setWards([]);
      return;
    }
    let alive = true;
    fetch(`${GEO_API}/p/${selectedProvince.code}?depth=2`)
      .then((r) => r.json())
      .then((data: { wards?: GeoItem[] }) => alive && setWards(sortByName(data.wards ?? [])))
      .catch(() => alive && setWards([]));
    return () => {
      alive = false;
    };
  }, [selectedProvince]);

  const selectedWardName = useMemo(() => {
    const key = wardKey(ward);
    if (!key) return '';
    return wards.find((w) => wardKey(w.name) === key)?.name ?? '';
  }, [wards, ward]);

  return (
    <>
      <label className="tfc-field">
        <span className="tfc-field__label">Tỉnh/Thành phố</span>
        <select
          className="tfc-field__control"
          value={selectedProvince?.name ?? ''}
          disabled={disabled}
          onChange={(e) => onChange({ province: e.target.value, ward: '' })}
        >
          <option value="">— Mọi tỉnh/thành —</option>
          {provinces.map((p) => (
            <option key={p.code} value={p.name}>
              {p.name}
            </option>
          ))}
        </select>
      </label>

      <label className="tfc-field">
        <span className="tfc-field__label">Phường/Xã</span>
        <select
          className="tfc-field__control"
          value={selectedWardName}
          disabled={disabled || !selectedProvince}
          onChange={(e) => onChange({ province: selectedProvince?.name ?? '', ward: e.target.value })}
        >
          <option value="">
            {selectedProvince ? '— Mọi phường/xã —' : '— Chọn tỉnh trước —'}
          </option>
          {wards.map((w) => (
            <option key={w.code} value={w.name}>
              {w.name}
            </option>
          ))}
        </select>
      </label>
    </>
  );
}
