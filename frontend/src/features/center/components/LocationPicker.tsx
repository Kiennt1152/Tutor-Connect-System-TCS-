import { useEffect, useMemo, useState } from 'react';
import './LocationPicker.css';

const GEO_API = 'https://provinces.open-api.vn/api/v2';

interface GeoItem {
  code: number;
  name: string;
}

export interface LocationValue {
  province: string;
  ward: string;
  addressDetail: string;
}

export interface LocationErrors {
  province?: string;
  ward?: string;
  addressDetail?: string;
}

interface Props {
  value: LocationValue;
  onChange: (value: LocationValue) => void;
  errors?: LocationErrors;
  showErrors?: boolean;
}

function sortByName(list: GeoItem[]): GeoItem[] {
  return [...list].sort((a, b) => a.name.localeCompare(b.name, 'vi'));
}

async function getProvinces(): Promise<GeoItem[]> {
  const res = await fetch(`${GEO_API}/p/`);
  return res.json();
}
async function getWards(provinceCode: number): Promise<GeoItem[]> {
  const res = await fetch(`${GEO_API}/p/${provinceCode}?depth=2`);
  const data = await res.json();
  return data.wards ?? [];
}

export function LocationPicker({ value, onChange, errors, showErrors }: Props) {
  const [provinces, setProvinces] = useState<GeoItem[]>([]);
  const [wards, setWards] = useState<GeoItem[]>([]);
  const [loadErr, setLoadErr] = useState('');

  useEffect(() => {
    let alive = true;
    getProvinces()
      .then((list) => alive && setProvinces(sortByName(list)))
      .catch(() => alive && setLoadErr('Không tải được danh sách địa phương. Kiểm tra kết nối mạng.'));
    return () => {
      alive = false;
    };
  }, []);

  const provinceCode = useMemo(
    () => provinces.find((p) => p.name === value.province)?.code ?? null,
    [provinces, value.province],
  );

  useEffect(() => {
    if (provinceCode == null) {
      setWards([]);
      return;
    }
    let alive = true;
    getWards(provinceCode)
      .then((list) => alive && setWards(sortByName(list)))
      .catch(() => alive && setWards([]));
    return () => {
      alive = false;
    };
  }, [provinceCode]);

  const cls = (key: keyof LocationErrors) =>
    `cc-input${showErrors && errors?.[key] ? ' cc-input--error' : ''}`;
  const msg = (key: keyof LocationErrors) =>
    showErrors && errors?.[key] ? <span className="cc-error">{errors[key]}</span> : null;

  return (
    <div className="cc-loc">
      {loadErr && <div className="cc-alert cc-alert--error">{loadErr}</div>}
      <div className="cc-loc__grid">
        <label className="cc-field">
          <span className="cc-label">Tỉnh/Thành phố *</span>
          <select
            className={cls('province')}
            value={value.province}
            onChange={(e) => onChange({ province: e.target.value, ward: '', addressDetail: value.addressDetail })}
          >
            <option value="">— Chọn Tỉnh/Thành phố —</option>
            {provinces.map((p) => (
              <option key={p.code} value={p.name}>
                {p.name}
              </option>
            ))}
          </select>
          {msg('province')}
        </label>

        <label className="cc-field">
          <span className="cc-label">Phường/Xã *</span>
          <select
            className={cls('ward')}
            value={value.ward}
            disabled={!value.province}
            onChange={(e) => onChange({ ...value, ward: e.target.value })}
          >
            <option value="">
              {value.province ? '— Chọn Phường/Xã —' : '— Chọn Tỉnh/Thành phố trước —'}
            </option>
            {wards.map((w) => (
              <option key={w.code} value={w.name}>
                {w.name}
              </option>
            ))}
          </select>
          {msg('ward')}
        </label>

        <label className="cc-field">
          <span className="cc-label">Địa chỉ cụ thể *</span>
          <input
            className={cls('addressDetail')}
            value={value.addressDetail}
            onChange={(e) => onChange({ ...value, addressDetail: e.target.value })}
            placeholder="VD: 123 Lê Lợi"
          />
          {msg('addressDetail')}
        </label>
      </div>
    </div>
  );
}
