import { useState } from 'react'
import { ChevronDown, Cloud, CloudRain, CloudSnow, Sun } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { Recommendation } from '../api'
import type { DayOption, WeatherKind } from '../labels'
import { areaLabel, weatherKind, weatherText } from '../labels'

interface Props {
  day: DayOption
  data: Recommendation | null
}

const ICON: Record<WeatherKind, LucideIcon> = { clear: Sun, cloud: Cloud, rain: CloudRain, snow: CloudSnow }

/**
 * 지도 오른쪽 위에 뜨는 작은 날씨 카드. 판정 문장은 패널 표지로 옮겼고(기획서 19장),
 * 여기에는 날씨 아이콘, 지역, 기온, 하늘, 강수확률만 한 줄로 둔다. 펼치면 판정 이유를 보여 준다.
 */
export default function WeatherCard({ day, data }: Props) {
  const [open, setOpen] = useState(false)

  if (!data) {
    return (
      <section className="weather-card" aria-label="날씨">
        <p className="wc-line" aria-live="polite">
          날씨를 확인하는 중이에요
        </p>
      </section>
    )
  }

  const { weather, message } = data
  const Icon = ICON[weatherKind(weather)]
  const label = open ? '판정 이유 접기' : '판정 이유 펼치기'

  return (
    <section className="weather-card" data-open={open} aria-label="날씨">
      <div className="wc-head">
        <span className="wc-icon" aria-hidden="true">
          <Icon size={26} strokeWidth={1.8} />
        </span>
        <div className="wc-main">
          <p className="where">
            {areaLabel(weather)} · {day.name}
          </p>
          <p className="wc-line">
            <strong>{weather.tempMax ?? '–'}°</strong> / {weather.tempMin ?? '–'}° · {weatherText(weather)} · 강수{' '}
            {weather.pop ?? '–'}%
          </p>
        </div>
        <button
          type="button"
          className="icon-btn wc-toggle"
          aria-expanded={open}
          aria-controls="weather-detail"
          aria-label={label}
          title={label}
          onClick={() => setOpen((o) => !o)}
        >
          <ChevronDown size={18} aria-hidden="true" />
        </button>
      </div>
      <div id="weather-detail" hidden={!open}>
        {/* 판정 이유와 지금 고른 실내/실외에 맞춘 안내는 서버가 만든다 */}
        <p className="wc-sub">{message}</p>
      </div>
    </section>
  )
}
