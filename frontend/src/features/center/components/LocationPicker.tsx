import { useEffect, useMemo, useState } from 'react';

// Nguồn dữ liệu hành chính công khai — API v2 theo mô hình 2 cấp (Tỉnh → Phường/Xã).
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

interface Props {
  value: LocationValue;
  onChange: (value: LocationValue) => void;
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

/** Chọn Tỉnh/Thành phố → Phường/Xã → nhập địa chỉ cụ thể. Với tin tuyển dụng, địa điểm là tuỳ chọn. */
export function LocationPicker({ value, onChange }: Props) {
  const [provinces, setProvinces] = useState<GeoItem[]>([]);
  const [wards, setWards] = useState<GeoItem[]>([]);
  const [loadErr, setLoadErr] = useState('');

  // Tải danh sách tỉnh một lần.
  useEffect(() => {
    let alive = true;
    getProvinces()
      .then((list) => alive && setProvinces(sortByName(list)))
      .catch(
        () =>
          alive && setLoadErr('Không tải được danh sách địa phương. Kiểm tra kết nối mạng.'),
      );
    return () => {
      alive = false;
    };
  }, []);

  // Suy ra mã tỉnh theo tên đang chọn (khớp dữ liệu tải về).
  const provinceCode = useMemo(
    () => provinces.find((p) => p.name === value.province)?.code ?? null,
    [provinces, value.province],
  );

  // Tỉnh đổi -> tải Phường/Xã.
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

  return (
    <>
      {loadErr && <div className="rc-alert rc-alert--error">{loadErr}</div>}
      <div className="rc-field2">
        <label className="rc-field">
          <span>Tỉnh/Thành phố</span>
          <select
            value={value.province}
            onChange={(e) =>
              // Đổi tỉnh thì phường cũ không còn hợp lệ.
              onChange({ province: e.target.value, ward: '', addressDetail: value.addressDetail })
            }
          >
            <option value="">— Chọn Tỉnh/Thành phố —</option>
            {provinces.map((p) => (
              <option key={p.code} value={p.name}>
                {p.name}
              </option>
            ))}
          </select>
        </label>

        <label className="rc-field">
          <span>Phường/Xã</span>
          <select
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
        </label>
      </div>

      <label className="rc-field">
        <span>Địa chỉ cụ thể</span>
        <input
          type="text"
          value={value.addressDetail}
          onChange={(e) => onChange({ ...value, addressDetail: e.target.value })}
          placeholder="VD: 123 Lê Lợi"
        />
      </label>
    </>
  );
}
