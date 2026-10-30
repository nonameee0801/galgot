import type { Recommendation } from '../api'
import type { DayOption } from '../labels'
import { weatherText } from '../labels'

interface Props {
  day: DayOption
  data: Recommendation | null
}

/** 첫 화면의 중심. 날씨를 숫자로 나열하기 전에 "그래서 오늘 어떻게 하면 되는지"를 문장으로 먼저 말한다. */
export default function Hero({ day, data }: Props) {
  if (!data) {
    return (
      <section className="hero">
        <h2 className="verdict">날씨를 확인하는 중이에요</h2>
      </section>
    )
  }

  const { weather, outdoorSuitable, reasons } = data
  const headline = outdoorSuitable ? `${day.topic} 밖에서 걸어도 좋아요` : `${day.topic} 실내에서 보내요`
  const because = reasons.length > 0 ? `${reasons.join(', ')} 때문에 ` : ''
  const sub = outdoorSuitable
    ? '야외 활동에 큰 문제가 없어요. 모든 장소를 보여드려요.'
    : `${because}실내 장소를 먼저 보여드려요.`

  return (
    <section className="hero">
      <div>
        <h2 className="verdict" aria-live="polite">
          {headline}
        </h2>
        <p className="verdict-sub">{sub}</p>
      </div>
      <div>
        <p className="temp">
          <span className="temp-num">{weather.tempMax ?? '–'}°</span>
          <span className="temp-cap">최고기온</span>
        </p>
        <dl className="facts">
          <div>
            <dt>최저기온</dt>
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
