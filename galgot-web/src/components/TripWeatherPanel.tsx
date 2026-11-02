import { useState } from 'react'
import { ChevronUp, Sun, Umbrella } from 'lucide-react'
import type { Weather } from '../api'
import { weatherText } from '../labels'

/** 날씨 패널의 하루. 아직 불러오는 중이거나 실패하면 weather가 없다. */
export interface TripWeatherItem {
  date: string
  /** 예: "1일차 · 내일 11/3" */
  title: string
  status: 'loading' | 'error' | 'ready'
  weather?: Weather
  outdoorSuitable?: boolean
  reasons?: string[]
  /** 결과 화면에서만: 고른 실내/실외에 맞춘 안내(날씨를 꼭 확인하세요 등) */
  message?: string
}

interface Props {
  /** 예: "경기 · 가평군" */
  where: string
  items: TripWeatherItem[]
}

/** 좁은 화면(휴대폰)에서는 지도 위쪽에 뜨는데, 아래 시트와 함께 지도를 거의 덮으므로 접힌 채로 시작한다. */
function startsOpen(): boolean {
  try {
    return !window.matchMedia('(max-width: 767px)').matches
  } catch {
    return true
  }
}

/**
 * 날씨기반 추천 탭에서 왼쪽 패널 옆에 붙어 나오는 날씨 패널(좁은 화면에서는 지도 위쪽).
 * 여행 기간의 날마다 판정과 최고·최저기온, 강수확률, 하늘, 부적합 이유를 보여 준다. 접을 수 있다.
 * 넓은 화면은 펼친 채로, 좁은 화면은 접힌 채로 시작한다.
 */
export default function TripWeatherPanel({ where, items }: Props) {
  const [open, setOpen] = useState(startsOpen)
  const label = open ? '날씨 패널 접기' : '날씨 패널 펼치기'

  return (
    <section className="trip-weather-panel" data-open={open} aria-label="여행 기간 날씨">
      <div className="twp-head">
        <h2 className="twp-title">
          날씨 <span className="twp-where">{where}</span>
        </h2>
        <button
          type="button"
          className="icon-btn wc-toggle"
          aria-expanded={open}
          aria-controls="trip-weather-days"
          aria-label={label}
          title={label}
          onClick={() => setOpen((o) => !o)}
        >
          <ChevronUp size={18} aria-hidden="true" />
        </button>
      </div>
      <ol className="twp-days" id="trip-weather-days" hidden={!open}>
        {items.map((it) => (
          <li key={it.date} className="twp-day">
            <p className="twp-day-title">{it.title}</p>
            {it.status === 'loading' && <p className="twp-sub">날씨를 확인하는 중이에요</p>}
            {it.status === 'error' && <p className="twp-sub">날씨를 불러오지 못했어요</p>}
            {it.status === 'ready' && it.weather && (
              <>
                <p className="twp-verdict">
                  {it.outdoorSuitable ? <Sun size={18} aria-hidden="true" /> : <Umbrella size={18} aria-hidden="true" />}
                  {it.outdoorSuitable ? '밖에서 걸어도 좋아요' : '실내에서 보내기 좋아요'}
                </p>
                <dl className="facts">
                  <div>
                    <dt>최고</dt>
                    <dd>{it.weather.tempMax ?? '–'}°</dd>
                  </div>
                  <div>
                    <dt>최저</dt>
                    <dd>{it.weather.tempMin ?? '–'}°</dd>
                  </div>
                  <div>
                    <dt>강수확률</dt>
                    <dd>{it.weather.pop ?? '–'}%</dd>
                  </div>
                  <div>
                    <dt>하늘</dt>
                    <dd>{weatherText(it.weather)}</dd>
                  </div>
                </dl>
                {it.reasons && it.reasons.length > 0 && (
                  <p className="twp-sub">야외가 어려운 이유: {it.reasons.join(', ')}</p>
                )}
                {it.message && <p className="twp-sub">{it.message}</p>}
              </>
            )}
          </li>
        ))}
      </ol>
    </section>
  )
}
