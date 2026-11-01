import { useState } from 'react'
import { ChevronUp } from 'lucide-react'
import type { Recommendation } from '../api'
import type { DayOption } from '../labels'
import { REGIONS, weatherText } from '../labels'

interface Props {
  day: DayOption
  data: Recommendation | null
}

/** "서울 전체", "경기 · 가평군"처럼 지금 보고 있는 지역을 한 줄로. */
function placeLabel(data: Recommendation): string {
  const { region, area, areaName } = data.weather
  if (area === region) return areaName
  const regionName = REGIONS.find((r) => r.code === region)?.name ?? ''
  return regionName ? `${regionName} · ${areaName}` : areaName
}

/**
 * 지도 오른쪽 위에 뜨는 날씨 카드. 숫자보다 "그래서 오늘 어떻게 하면 되는지"를 문장으로 먼저 말한다.
 * 접으면 지역과 판정 문장만 남는다.
 */
export default function WeatherCard({ day, data }: Props) {
  const [open, setOpen] = useState(true)

  if (!data) {
    return (
      <section className="weather-card" aria-label="날씨">
        <p className="wc-verdict" aria-live="polite">
          날씨를 확인하는 중이에요
        </p>
      </section>
    )
  }

  const { weather, outdoorSuitable, message } = data
  const headline = outdoorSuitable ? `${day.topic} 밖에서 걸어도 좋아요` : `${day.topic} 실내에서 보내요`
  const label = open ? '날씨 자세히 접기' : '날씨 자세히 펼치기'

  return (
    <section className="weather-card" data-open={open} aria-label="날씨">
      <div className="wc-head">
        <p className="where">
          {placeLabel(data)} · {day.name}
        </p>
        <button
          type="button"
          className="icon-btn wc-toggle"
          aria-expanded={open}
          aria-controls="weather-detail"
          aria-label={label}
          title={label}
          onClick={() => setOpen((o) => !o)}
        >
          <ChevronUp size={18} aria-hidden="true" />
        </button>
      </div>
      <p className="wc-verdict" aria-live="polite">
        {headline}
      </p>
      <div id="weather-detail" hidden={!open}>
        {/* 판정 이유와 지금 고른 실내/실외에 맞춘 안내는 서버가 만든다 */}
        <p className="wc-sub">{message}</p>
        <dl className="facts">
          <div>
            <dt>최고</dt>
            <dd>{weather.tempMax ?? '–'}°</dd>
          </div>
          <div>
            <dt>최저</dt>
            <dd>{weather.tempMin ?? '–'}°</dd>
          </div>
          <div>
            <dt>강수확률</dt>
            <dd>{weather.pop ?? '–'}%</dd>
          </div>
          <div>
            <dt>하늘</dt>
            <dd>{weatherText(weather)}</dd>
          </div>
        </dl>
      </div>
    </section>
  )
}
