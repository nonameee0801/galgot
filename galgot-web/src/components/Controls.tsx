import type { Area } from '../api'
import { REGIONS } from '../labels'

interface Props {
  region: string
  onRegion: (code: string) => void
  areas: Area[]
  area: string | null
  onArea: (code: string | null) => void
}

/** 지역(시도, 구·시군)을 고르는 줄. 날짜는 고르지 않고 항상 오늘 기준이다(다른 날짜는 날씨기반 추천에서 고른다). */
export default function Controls({ region, onRegion, areas, area, onArea }: Props) {
  return (
    <div className="controls">
      <div className="regions" role="group" aria-label="지역 선택">
        {REGIONS.map((r) => (
          <button
            key={r.code}
            type="button"
            className="seg"
            aria-pressed={r.code === region}
            onClick={() => onRegion(r.code)}
          >
            {r.name}
          </button>
        ))}
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
      </div>
    </div>
  )
}
