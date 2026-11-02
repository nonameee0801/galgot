import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import type { Area } from '../api'
import { REGIONS } from '../labels'

interface Props {
  region: string
  onRegion: (code: string) => void
  areas: Area[]
  area: string | null
  onArea: (code: string | null) => void
  /**
   * trip: 날씨기반 추천 탭. 서울은 구를 고르지 않고 서울 전체로 짜므로 구 버튼을 숨기고,
   * 경기는 시·군을 꼭 골라야 해서 "전체"를 빼고 처음에 펼쳐 둔다.
   */
  mode?: 'list' | 'trip'
}

/**
 * 지역(시도, 구·시군)을 고르는 줄. 날짜는 고르지 않고 항상 오늘 기준이다(다른 날짜는 날씨기반 추천에서 고른다).
 * 구·시군은 "종로구 ▾" 버튼을 눌러 펼치는 버튼 격자에서 고르고, 고르면 다시 접힌다.
 */
export default function Controls({ region, onRegion, areas, area, onArea, mode = 'list' }: Props) {
  const trip = mode === 'trip'
  const [open, setOpen] = useState(trip && area === null)
  const unit = region === '11' ? '구' : '시·군'
  const current = areas.find((a) => a.code === area)?.name ?? (trip ? `${unit} 고르기` : '전체')
  const hideArea = trip && region === '11'

  function pickRegion(code: string) {
    // 추천 탭에서 경기로 바꾸면 시·군을 바로 고를 수 있게 펼친다
    setOpen(trip && code !== '11')
    onRegion(code)
  }

  function pickArea(code: string | null) {
    setOpen(false)
    onArea(code)
  }

  return (
    <div className="controls">
      <div className="regions" role="group" aria-label="시도 선택">
        {REGIONS.map((r) => (
          <button
            key={r.code}
            type="button"
            className="seg"
            aria-pressed={r.code === region}
            onClick={() => pickRegion(r.code)}
          >
            {r.name}
          </button>
        ))}
        {/*
          select-box 형태 (2026-11-02 접었다 펴는 버튼 나열식으로 바꾸면서 주석 처리)
          되돌리려면 이 주석을 풀고, 아래 area-toggle과 area-grid 부분을 지운다. index.css의 .area-select 주석도 함께 푼다.

        <label className="area-select">
          <span className="sr-only">구·시군</span>
          <select
            value={area ?? ''}
            onChange={(e) => onArea(e.target.value === '' ? null : e.target.value)}
            disabled={areas.length === 0}
          >
            <option value="">전체</option>
            {areas.map((a) => (
              <option key={a.code} value={a.code}>
                {a.name}
              </option>
            ))}
          </select>
        </label>
        */}
      </div>

      {/* 버튼 나열식: 펼치기 버튼 + 구·시군 버튼 격자 */}
      {!hideArea && (
      <button
        type="button"
        className="area-toggle"
        aria-expanded={open}
        aria-controls="area-grid"
        aria-label={`${unit} 선택: ${current}`}
        disabled={areas.length === 0}
        onClick={() => setOpen((o) => !o)}
      >
        <strong>{current}</strong>
        <ChevronDown size={18} aria-hidden="true" />
      </button>
      )}
      {open && !hideArea && (
        <div className="area-grid" id="area-grid" role="group" aria-label={`${unit} 선택`}>
          {!trip && (
            <button type="button" className="area-chip" aria-pressed={area === null} onClick={() => pickArea(null)}>
              전체
            </button>
          )}
          {areas.map((a) => (
            <button
              key={a.code}
              type="button"
              className="area-chip"
              aria-pressed={a.code === area}
              title={a.name}
              onClick={() => pickArea(a.code)}
            >
              {a.name}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
