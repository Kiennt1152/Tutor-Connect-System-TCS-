import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import './AdminTimeFilter.css';

export type TimeFilterPreset = '7d' | '30d' | '90d' | 'all' | 'custom';
export type Granularity = 'day' | 'week' | 'month';

export type TimeFilterValue = {
  preset: TimeFilterPreset;
  from: string;
  to: string;
  granularity: Granularity;
};

type AdminTimeFilterProps = {
  onChange?: (val: TimeFilterValue) => void;
  showGranularity?: boolean;
  extraControls?: React.ReactNode;
};

export function AdminTimeFilter({
  onChange,
  showGranularity = true,
  extraControls,
}: AdminTimeFilterProps) {
  const [searchParams, setSearchParams] = useSearchParams();

  const urlPreset = (searchParams.get('preset') as TimeFilterPreset) || '30d';
  const urlFrom = searchParams.get('from') || '';
  const urlTo = searchParams.get('to') || '';
  const urlGranularity = (searchParams.get('granularity') as Granularity) || 'day';

  const [preset, setPreset] = useState<TimeFilterPreset>(urlPreset);
  const [from, setFrom] = useState<string>(urlFrom);
  const [to, setTo] = useState<string>(urlTo);
  const [granularity, setGranularity] = useState<Granularity>(urlGranularity);

  const calculateDates = useCallback((selectedPreset: TimeFilterPreset) => {
    if (selectedPreset === 'all') {
      return { from: '', to: '' };
    }
    const days = selectedPreset === '7d' ? 7 : selectedPreset === '90d' ? 90 : 30;
    const toDate = new Date();
    const fromDate = new Date();
    fromDate.setDate(toDate.getDate() - days);
    return {
      from: fromDate.toISOString().slice(0, 10),
      to: toDate.toISOString().slice(0, 10),
    };
  }, []);

  // Initialize dates if preset is default on load
  useEffect(() => {
    if (!urlFrom && !urlTo && preset !== 'all' && preset !== 'custom') {
      const dates = calculateDates(preset);
      setFrom(dates.from);
      setTo(dates.to);
    }
  }, [urlFrom, urlTo, preset, calculateDates]);

  // Sync to URL & notify parent
  const applyFilter = (
    newPreset: TimeFilterPreset,
    newFrom: string,
    newTo: string,
    newGranularity: Granularity
  ) => {
    setPreset(newPreset);
    setFrom(newFrom);
    setTo(newTo);
    setGranularity(newGranularity);

    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('preset', newPreset);
    if (newFrom) nextParams.set('from', newFrom);
    else nextParams.delete('from');
    if (newTo) nextParams.set('to', newTo);
    else nextParams.delete('to');
    if (showGranularity) nextParams.set('granularity', newGranularity);
    else nextParams.delete('granularity');

    setSearchParams(nextParams, { replace: true });

    if (onChange) {
      onChange({
        preset: newPreset,
        from: newFrom,
        to: newTo,
        granularity: newGranularity,
      });
    }
  };

  const handlePresetClick = (p: TimeFilterPreset) => {
    if (p === 'custom') {
      applyFilter('custom', from, to, granularity);
    } else {
      const dates = calculateDates(p);
      applyFilter(p, dates.from, dates.to, granularity);
    }
  };

  const handleFromChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    applyFilter('custom', val, to, granularity);
  };

  const handleToChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    applyFilter('custom', from, val, granularity);
  };

  const handleGranularityChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value as Granularity;
    applyFilter(preset, from, to, val);
  };

  return (
    <div className="adm-time-filter">
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
        <span style={{ fontWeight: 600, color: '#475569', fontSize: '0.8rem', textTransform: 'uppercase' }}>
          Thời gian:
        </span>
        <div className="adm-time-filter__presets">
          <button
            type="button"
            className={`adm-time-filter__preset-btn ${preset === '7d' ? 'adm-time-filter__preset-btn--active' : ''}`}
            onClick={() => handlePresetClick('7d')}
          >
            7 ngày
          </button>
          <button
            type="button"
            className={`adm-time-filter__preset-btn ${preset === '30d' ? 'adm-time-filter__preset-btn--active' : ''}`}
            onClick={() => handlePresetClick('30d')}
          >
            30 ngày
          </button>
          <button
            type="button"
            className={`adm-time-filter__preset-btn ${preset === '90d' ? 'adm-time-filter__preset-btn--active' : ''}`}
            onClick={() => handlePresetClick('90d')}
          >
            90 ngày
          </button>
          <button
            type="button"
            className={`adm-time-filter__preset-btn ${preset === 'all' ? 'adm-time-filter__preset-btn--active' : ''}`}
            onClick={() => handlePresetClick('all')}
          >
            Toàn bộ
          </button>
        </div>

        <div className="adm-time-filter__custom">
          <input
            type="date"
            className="adm-time-filter__input"
            value={from}
            onChange={handleFromChange}
            placeholder="Từ ngày"
          />
          <span style={{ color: '#94a3b8' }}>→</span>
          <input
            type="date"
            className="adm-time-filter__input"
            value={to}
            onChange={handleToChange}
            placeholder="Đến ngày"
          />
        </div>
      </div>

      <div className="adm-time-filter__right">
        {showGranularity && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Độ phân giải:</span>
            <select
              className="adm-time-filter__granularity"
              value={granularity}
              onChange={handleGranularityChange}
            >
              <option value="day">Theo ngày</option>
              <option value="week">Theo tuần</option>
              <option value="month">Theo tháng</option>
            </select>
          </div>
        )}
        {extraControls}
      </div>
    </div>
  );
}
