import { useEffect, useMemo, useState } from 'react';
import type { CSSProperties } from 'react';

export interface BankOption {
  code: string;
  shortName: string;
  name: string;
  logoUrl: string;
  color?: string;
}

export const BANK_OPTIONS: BankOption[] = [
  { code: 'VCB', shortName: 'Vietcombank', name: 'Ngân hàng TMCP Ngoại Thương Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/VCB.png' },
  { code: 'TCB', shortName: 'Techcombank', name: 'Ngân hàng TMCP Kỹ Thương Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/TCB.png' },
  { code: 'TPB', shortName: 'TPBank', name: 'Ngân hàng TMCP Tiên Phong', logoUrl: 'https://cdn.vietqr.io/img/TPB.png' },
  { code: 'VPB', shortName: 'VPBank', name: 'Ngân hàng TMCP Việt Nam Thịnh Vượng', logoUrl: 'https://cdn.vietqr.io/img/VPB.png' },
  { code: 'MB', shortName: 'MBBank', name: 'Ngân hàng TMCP Quân Đội', logoUrl: 'https://cdn.vietqr.io/img/MB.png' },
  { code: 'BIDV', shortName: 'BIDV', name: 'Ngân hàng TMCP Đầu Tư và Phát Triển Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/BIDV.png' },
  { code: 'ICB', shortName: 'VietinBank', name: 'Ngân hàng TMCP Công Thương Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/ICB.png' },
  { code: 'VBA', shortName: 'Agribank', name: 'Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/VBA.png' },
  { code: 'ACB', shortName: 'ACB', name: 'Ngân hàng TMCP Á Châu', logoUrl: 'https://cdn.vietqr.io/img/ACB.png' },
  { code: 'OCB', shortName: 'OCB', name: 'Ngân hàng TMCP Phương Đông', logoUrl: 'https://cdn.vietqr.io/img/OCB.png' },
  { code: 'STB', shortName: 'Sacombank', name: 'Ngân hàng TMCP Sài Gòn Thương Tín', logoUrl: 'https://cdn.vietqr.io/img/STB.png' },
  { code: 'HDB', shortName: 'HDBank', name: 'Ngân hàng TMCP Phát triển Thành phố Hồ Chí Minh', logoUrl: 'https://cdn.vietqr.io/img/HDB.png' },
  { code: 'VIB', shortName: 'VIB', name: 'Ngân hàng TMCP Quốc Tế Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/VIB.png' },
  { code: 'SHB', shortName: 'SHB', name: 'Ngân hàng TMCP Sài Gòn - Hà Nội', logoUrl: 'https://cdn.vietqr.io/img/SHB.png' },
  { code: 'MSB', shortName: 'MSB', name: 'Ngân hàng TMCP Hàng Hải Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/MSB.png' },
  { code: 'EIB', shortName: 'Eximbank', name: 'Ngân hàng TMCP Xuất Nhập Khẩu Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/EIB.png' },
  { code: 'ABB', shortName: 'ABBANK', name: 'Ngân hàng TMCP An Bình', logoUrl: 'https://cdn.vietqr.io/img/ABB.png' },
  { code: 'BAB', shortName: 'BacABank', name: 'Ngân hàng TMCP Bắc Á', logoUrl: 'https://cdn.vietqr.io/img/BAB.png' },
  { code: 'BVB', shortName: 'BaoVietBank', name: 'Ngân hàng TMCP Bảo Việt', logoUrl: 'https://cdn.vietqr.io/img/BVB.png' },
  { code: 'CBB', shortName: 'CBBank', name: 'Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/CBB.png' },
  { code: 'CIMB', shortName: 'CIMB', name: 'Ngân hàng TNHH MTV CIMB Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/CIMB.png' },
  { code: 'DBS', shortName: 'DBSBank', name: 'DBS Bank Ltd - Chi nhánh Thành phố Hồ Chí Minh', logoUrl: 'https://cdn.vietqr.io/img/DBS.png' },
  { code: 'Vikki', shortName: 'Vikki', name: 'Ngân hàng TNHH MTV Số Vikki', logoUrl: 'https://cdn.vietqr.io/img/Vikki.png' },
  { code: 'GPB', shortName: 'GPBank', name: 'Ngân hàng Thương mại TNHH MTV Dầu Khí Toàn Cầu', logoUrl: 'https://cdn.vietqr.io/img/GPB.png' },
  { code: 'HLBVN', shortName: 'HongLeong', name: 'Ngân hàng TNHH MTV Hong Leong Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/HLBVN.png' },
  { code: 'HSBC', shortName: 'HSBC', name: 'Ngân hàng TNHH MTV HSBC Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/HSBC.png' },
  { code: 'IBK - HN', shortName: 'IBK Hà Nội', name: 'Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh Hà Nội', logoUrl: 'https://cdn.vietqr.io/img/IBK.png' },
  { code: 'IBK - HCM', shortName: 'IBK TP.HCM', name: 'Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh TP. Hồ Chí Minh', logoUrl: 'https://cdn.vietqr.io/img/IBK.png' },
  { code: 'IVB', shortName: 'IndovinaBank', name: 'Ngân hàng TNHH Indovina', logoUrl: 'https://cdn.vietqr.io/img/IVB.png' },
  { code: 'KLB', shortName: 'KienLongBank', name: 'Ngân hàng TMCP Kiên Long', logoUrl: 'https://cdn.vietqr.io/img/KLB.png' },
  { code: 'LPB', shortName: 'LPBank', name: 'Ngân hàng TMCP Lộc Phát Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/LPB.png' },
  { code: 'NAB', shortName: 'NamABank', name: 'Ngân hàng TMCP Nam Á', logoUrl: 'https://cdn.vietqr.io/img/NAB.png' },
  { code: 'NCB', shortName: 'NCB', name: 'Ngân hàng TMCP Quốc Dân', logoUrl: 'https://cdn.vietqr.io/img/NCB.png' },
  { code: 'NHB HN', shortName: 'Nonghyup', name: 'Ngân hàng Nonghyup - Chi nhánh Hà Nội', logoUrl: 'https://cdn.vietqr.io/img/NHB.png' },
  { code: 'MBV', shortName: 'MBV', name: 'Ngân hàng TNHH MTV Việt Nam Hiện Đại', logoUrl: 'https://cdn.vietqr.io/img/MBV.png' },
  { code: 'PBVN', shortName: 'PublicBank', name: 'Ngân hàng TNHH MTV Public Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/PBVN.png' },
  { code: 'PGB', shortName: 'PGBank', name: 'Ngân hàng TMCP Thịnh Vượng và Phát Triển', logoUrl: 'https://cdn.vietqr.io/img/PGB.png' },
  { code: 'PVCB', shortName: 'PVcomBank', name: 'Ngân hàng TMCP Đại Chúng Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/PVCB.png' },
  { code: 'SCB', shortName: 'SCB', name: 'Ngân hàng TMCP Sài Gòn', logoUrl: 'https://cdn.vietqr.io/img/SCB.png' },
  { code: 'SCVN', shortName: 'Standard Chartered', name: 'Ngân hàng TNHH MTV Standard Chartered Bank Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/SCVN.png' },
  { code: 'SEAB', shortName: 'SeABank', name: 'Ngân hàng TMCP Đông Nam Á', logoUrl: 'https://cdn.vietqr.io/img/SEAB.png' },
  { code: 'SGICB', shortName: 'SaigonBank', name: 'Ngân hàng TMCP Sài Gòn Công Thương', logoUrl: 'https://cdn.vietqr.io/img/SGICB.png' },
  { code: 'SHBVN', shortName: 'ShinhanBank', name: 'Ngân hàng TNHH MTV Shinhan Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/SHBVN.png' },
  { code: 'UOB', shortName: 'UOB', name: 'Ngân hàng United Overseas - Chi nhánh TP. Hồ Chí Minh', logoUrl: 'https://cdn.vietqr.io/img/UOB.png' },
  { code: 'VAB', shortName: 'VietABank', name: 'Ngân hàng TMCP Việt Á', logoUrl: 'https://cdn.vietqr.io/img/VAB.png' },
  { code: 'VCCB', shortName: 'BVBank', name: 'Ngân hàng TMCP Bản Việt', logoUrl: 'https://cdn.vietqr.io/img/VCCB.png' },
  { code: 'VIETBANK', shortName: 'VietBank', name: 'Ngân hàng TMCP Việt Nam Thương Tín', logoUrl: 'https://cdn.vietqr.io/img/VIETBANK.png' },
  { code: 'VRB', shortName: 'VRB', name: 'Ngân hàng Liên doanh Việt - Nga', logoUrl: 'https://cdn.vietqr.io/img/VRB.png' },
  { code: 'WVN', shortName: 'Woori', name: 'Ngân hàng TNHH MTV Woori Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/WVN.png' },
  { code: 'KBHN', shortName: 'Kookmin Hà Nội', name: 'Ngân hàng Kookmin - Chi nhánh Hà Nội', logoUrl: 'https://cdn.vietqr.io/img/KBHN.png' },
  { code: 'KBHCM', shortName: 'Kookmin TP.HCM', name: 'Ngân hàng Kookmin - Chi nhánh Thành phố Hồ Chí Minh', logoUrl: 'https://cdn.vietqr.io/img/KBHCM.png' },
  { code: 'COOPBANK', shortName: 'Co-opBank', name: 'Ngân hàng Hợp tác xã Việt Nam', logoUrl: 'https://cdn.vietqr.io/img/COOPBANK.png' },
  { code: 'CAKE', shortName: 'CAKE', name: 'Ngân hàng số CAKE by VPBank', logoUrl: 'https://cdn.vietqr.io/img/CAKE.png' },
  { code: 'Ubank', shortName: 'Ubank', name: 'Ngân hàng số Ubank by VPBank', logoUrl: 'https://cdn.vietqr.io/img/UBANK.png' },
  { code: 'KBank', shortName: 'KBank', name: 'Ngân hàng Đại chúng TNHH Kasikornbank', logoUrl: 'https://cdn.vietqr.io/img/KBANK.png' },
  { code: 'TIMO', shortName: 'Timo', name: 'Ngân hàng số Timo by BVBank', logoUrl: 'https://vietqr.net/portal-service/resources/icons/TIMO.png' },
  { code: 'CITIBANK', shortName: 'Citibank', name: 'Ngân hàng Citibank - Chi nhánh Hà Nội', logoUrl: 'https://cdn.vietqr.io/img/CITIBANK.png' },
  { code: 'KEBHANAHCM', shortName: 'KEB Hana TP.HCM', name: 'Ngân hàng KEB Hana - Chi nhánh Thành phố Hồ Chí Minh', logoUrl: 'https://cdn.vietqr.io/img/KEBHANAHCM.png' },
  { code: 'KEBHANAHN', shortName: 'KEB Hana Hà Nội', name: 'Ngân hàng KEB Hana - Chi nhánh Hà Nội', logoUrl: 'https://cdn.vietqr.io/img/KEBHANAHN.png' },
  { code: 'VBSP', shortName: 'VBSP', name: 'Ngân hàng Chính sách Xã hội', logoUrl: 'https://cdn.vietqr.io/img/VBSP.png' },
  { code: 'PVDB', shortName: 'PVcomBank Pay', name: 'Ngân hàng số PVcomBank Pay', logoUrl: 'https://cdn.vietqr.io/img/PVCB.png' },
];

const BANK_BADGE_COLORS = ['#0f766e', '#1d4ed8', '#dc2626', '#ea580c', '#7c3aed', '#0891b2', '#16a34a', '#be123c'];

function getBankBadgeColor(code: string) {
  const total = Array.from(code).reduce((sum, char) => sum + char.charCodeAt(0), 0);
  return BANK_BADGE_COLORS[total % BANK_BADGE_COLORS.length];
}

function getBankLogoText(bank: BankOption) {
  return bank.code.replace(/[^a-zA-Z0-9]/g, '').slice(0, 4).toUpperCase();
}

function normalizeSearchText(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .toLowerCase();
}

export function findBankByName(value: string | null | undefined) {
  const keyword = normalizeSearchText((value ?? '').trim());
  if (!keyword) {
    return undefined;
  }

  return BANK_OPTIONS.find((bank) => {
    const code = normalizeSearchText(bank.code);
    const shortName = normalizeSearchText(bank.shortName);
    const name = normalizeSearchText(bank.name);
    return keyword === code || keyword === shortName || keyword === name;
  });
}

export function BankLogo({ bank }: { bank: BankOption }) {
  const logoStyle = { '--bank-color': bank.color ?? getBankBadgeColor(bank.code) } as CSSProperties;

  return (
    <span className="bank-logo" style={logoStyle}>
      <span>{getBankLogoText(bank)}</span>
      <img
        src={bank.logoUrl}
        alt=""
        loading="lazy"
        onError={(event) => {
          event.currentTarget.style.display = 'none';
        }}
      />
    </span>
  );
}

export function BankSelectField({
  id,
  selectedBank,
  onOpen,
}: {
  id: string;
  selectedBank: BankOption | undefined;
  onOpen: () => void;
}) {
  return (
    <button
      id={id}
      type="button"
      className={`bank-select-field${selectedBank ? ' bank-select-field--selected' : ''}`}
      onClick={onOpen}
      aria-haspopup="dialog"
    >
      {selectedBank ? (
        <>
          <BankLogo bank={selectedBank} />
          <span className="bank-select-field__text">
            <strong>{selectedBank.shortName}</strong>
            <small>{selectedBank.name}</small>
          </span>
        </>
      ) : (
        <span className="bank-select-field__placeholder">Chọn ngân hàng</span>
      )}
      <span className="bank-select-field__arrow">⌄</span>
    </button>
  );
}

export function BankPickerDialog({
  open,
  selectedBankCode,
  onSelect,
  onClose,
}: {
  open: boolean;
  selectedBankCode: string;
  onSelect: (bank: BankOption) => void;
  onClose: () => void;
}) {
  const [bankQuery, setBankQuery] = useState('');
  const filteredBanks = useMemo(() => {
    const keyword = normalizeSearchText(bankQuery.trim());
    if (!keyword) {
      return BANK_OPTIONS;
    }

    return BANK_OPTIONS.filter((bank) =>
      normalizeSearchText(`${bank.shortName} ${bank.name} ${bank.code}`).includes(keyword)
    );
  }, [bankQuery]);

  useEffect(() => {
    if (!open) {
      setBankQuery('');
    }
  }, [open]);

  if (!open) {
    return null;
  }

  function handleClose() {
    setBankQuery('');
    onClose();
  }

  function handleSelect(bank: BankOption) {
    setBankQuery('');
    onSelect(bank);
  }

  return (
    <div className="bank-dialog-overlay" onClick={handleClose}>
      <div
        className="bank-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="bank-dialog-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="bank-dialog__header">
          <h3 id="bank-dialog-title">Chọn ngân hàng</h3>
          <button type="button" className="modal__close" onClick={handleClose}>
            ×
          </button>
        </div>
        <div className="bank-dialog__body">
          <input
            type="text"
            className="form-input"
            placeholder="Tìm ngân hàng"
            value={bankQuery}
            onChange={(event) => setBankQuery(event.target.value)}
            autoFocus
          />
          <div className="bank-list">
            {filteredBanks.map((bank) => {
              const active = selectedBankCode === bank.code;

              return (
                <button
                  key={bank.code}
                  type="button"
                  className={`bank-option${active ? ' bank-option--active' : ''}`}
                  onClick={() => handleSelect(bank)}
                >
                  <BankLogo bank={bank} />
                  <span className="bank-option__text">
                    <strong>{bank.shortName}</strong>
                    <small>{bank.name}</small>
                  </span>
                </button>
              );
            })}
            {filteredBanks.length === 0 && (
              <div className="bank-list__empty">Không tìm thấy ngân hàng</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
