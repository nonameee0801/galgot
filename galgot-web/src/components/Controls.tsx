import type { Area } from '../api'
import type { DayOption } from '../labels'
import { REGIONS } from '../labels'

interface Props {
  days: DayOption[]
  offset: number
  onOffset: (i: number) => void
  region: string
  onRegion: (code: string) => void
  areas: Area[]
  area: string | null
  onArea: (code: string | null) => void
}

/** 날짜(오늘~3일 뒤)와 지역(시도, 구·시군)을 고르는 줄. 고르면 페이지 분위기도 함께 바뀐다. */
export default function Controls({ days, offset, onOffset, region, onRegion, areas, area, onArea }: Props) {
  return (
    <div className="controls">
      <div className="days" role="group" aria-label="날짜 선택">
        {days.map((d, i) => (
          <button
            key={d.date}
            type="button"
            className="seg"
            aria-pressed={i === offset}
            onClick={() => onOffset(i)}
          >
            {d.name}
            <small>{d.short}</small>
          </button>
        ))}
      </div>
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
