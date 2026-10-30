import type { DayOption } from '../labels'
import { REGIONS } from '../labels'

interface Props {
  days: DayOption[]
  offset: number
  onOffset: (i: number) => void
  region: string
  onRegion: (code: string) => void
}

/** 날짜(오늘~3일 뒤)와 지역을 고르는 줄. 고르면 페이지 분위기도 함께 바뀐다. */
export default function Controls({ days, offset, onOffset, region, onRegion }: Props) {
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
      </div>
    </div>
  )
}
