import { useEffect, useMemo, useState } from 'react'
import { ApiError, fetchRecommendations, fetchTrip } from '../api'
import type { Area, EnvChoice, Me, Recommendation, Trip, TripDay } from '../api'
import type { DayOption } from '../labels'
import { TRIP_LENGTHS, TYPES, startOptions } from '../labels'
import Controls from './Controls'
import EnvPicker from './EnvPicker'
import TripCalendar from './TripCalendar'
import TripResult from './TripResult'
import type { TripWeatherItem } from './TripWeatherPanel'

/** 날씨기반 추천 탭이 주소에 남기는 조건. region, area, env, type은 관광지 목록 탭과 함께 쓴다. */
export interface TripConditions {
  region: string
  area: string | null
  env: EnvChoice | null
  type: number | null
  start: string
  days: number
  /** 결과의 1일차 첫 장소. 있으면 결과 화면, 없으면 조건 입력 화면 */
  sp: number | null
}

/** 옆 날씨 패널에 넘기는 내용 */
export interface TripWeatherInfo {
  where: string
  items: TripWeatherItem[]
}

interface Props extends TripConditions {
  areas: Area[]
  /** 오늘부터 3일 뒤까지 */
  dayList: DayOption[]
  onUpdate: (next: Partial<TripConditions>, options?: { replace?: boolean }) => void
  me: Me | null
  onLogin: () => void
  selectedId: number | null
  onSelect: (id: number) => void
  /** 지도에 그릴 일차별 코스(결과가 없으면 null) */
  onRoute: (days: TripDay[] | null) => void
  /** 옆 날씨 패널 내용(지역이 정해지지 않았으면 null) */
  onWeather: (info: TripWeatherInfo | null) => void
}

type Preview = { date: string; data: Recommendation } | { date: string; error: true }

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : '알 수 없는 오류가 났어요. 잠시 뒤에 다시 시도해 주세요.'
}

/**
 * 날씨기반 추천 탭(기획서 16장 3단계).
 * 출발일(달력) → 일정 → 지역 → 실내/실외/둘 다 → 테마 → [추천 받기] → 일차별 결과.
 * 날짜별 날씨는 왼쪽 패널 옆의 날씨 패널(TripWeatherPanel)에 보여 준다.
 * 결과의 시작 장소(sp)를 주소에 남겨 새로고침, 공유, 카카오 로그인 뒤에도 같은 코스를 다시 만든다.
 */
export default function TripTab(props: Props) {
  const { region, area, env, type, start, days, sp, areas, dayList, onUpdate, onRoute, onWeather } = props
  // 서울은 구를 고르지 않고 서울 전체로 짠다(서버도 서울에 area가 오면 400). 경기는 시·군을 꼭 고른다.
  const tripArea = region === '11' ? null : area
  const areaName = areas.find((a) => a.code === area)?.name ?? null
  const ready = region === '11' || (area !== null && areaName !== null)
  const where = region === '11' ? '서울 전체' : `경기 · ${areaName ?? ''}`
  const starts = startOptions(dayList, days)
  const key = JSON.stringify([region, tripArea, start, days, env, type])

  // 미리보기는 어떤 조건(지역, 날짜)으로 받은 것인지 함께 둔다. 조건이 바뀌면 이전 지역의 날씨를 보여 주지 않는다
  const [previewState, setPreviewState] = useState<{ key: string; list: Preview[] }>({ key: '', list: [] })
  const [trip, setTrip] = useState<{ key: string; data: Trip } | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const showing = sp !== null && trip !== null && trip.key === key && trip.data.startPlaceId === sp ? trip.data : null

  // 여행 기간의 날짜(출발일부터 일정 길이만큼)
  const dates = useMemo(() => {
    const i = Math.max(0, dayList.findIndex((d) => d.date === start))
    return dayList.slice(i, i + days)
  }, [dayList, start, days])

  // 날짜별 날씨 미리보기: 버튼의 추천 표시(출발일 기준)와 옆 날씨 패널. 날씨는 서버에 저장된 값을 쓴다.
  const previewKey = JSON.stringify([region, tripArea, dates.map((d) => d.date)])
  const previews = useMemo(() => (previewState.key === previewKey ? previewState.list : []), [previewState, previewKey])
  useEffect(() => {
    if (!ready || sp !== null) return
    let cancelled = false
    const list = JSON.parse(previewKey)[2] as string[]
    Promise.all(
      list.map((date) =>
        fetchRecommendations({ region, area: tripArea, date, type: null, env: null, page: 0, size: 1 })
          .then((data): Preview => ({ date, data }))
          .catch((): Preview => ({ date, error: true })),
      ),
    ).then((res) => {
      if (!cancelled) setPreviewState({ key: previewKey, list: res })
    })
    return () => {
      cancelled = true
    }
    // previewKey가 region, tripArea, 날짜를 모두 담고 있다
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [previewKey, ready, sp])

  // 주소에 시작 장소가 있으면(새로고침, 공유, 로그인 뒤) 같은 코스를 다시 만든다
  useEffect(() => {
    if (sp === null || !ready || (trip !== null && trip.key === key && trip.data.startPlaceId === sp)) return
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchTrip({ region, area: tripArea, start, days, env, type, startPlaceId: sp, excludeStartPlaceId: null })
      .then((data) => {
        if (cancelled) return
        setTrip({ key, data })
        // 주소의 시작 장소가 지금 조건의 후보가 아니면 서버가 새로 고른다. 주소도 그 장소로 맞춘다
        if (data.startPlaceId !== sp) onUpdate({ sp: data.startPlaceId }, { replace: true })
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(messageOf(e))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, sp, ready])

  // 옆 날씨 패널: 결과가 있으면 결과의 일차별 날씨와 안내, 없으면 미리보기
  const weatherInfo = useMemo<TripWeatherInfo>(() => {
    const titleOf = (i: number, d: DayOption) => `${days > 1 ? `${i + 1}일차 · ` : ''}${d.name} ${d.short}`
    const items: TripWeatherItem[] = dates.map((d, i) => {
      const day = showing?.days.find((x) => x.date === d.date)
      if (day) {
        return {
          date: d.date, title: titleOf(i, d), status: 'ready', weather: day.weather,
          outdoorSuitable: day.outdoorSuitable, reasons: day.reasons, message: day.message,
        }
      }
      const p = previews.find((x) => x.date === d.date)
      if (!p) return { date: d.date, title: titleOf(i, d), status: 'loading' }
      if ('error' in p) return { date: d.date, title: titleOf(i, d), status: 'error' }
      return {
        date: d.date, title: titleOf(i, d), status: 'ready', weather: p.data.weather,
        outdoorSuitable: p.data.outdoorSuitable, reasons: p.data.reasons,
      }
    })
    return { where, items }
  }, [dates, days, showing, previews, where])

  useEffect(() => {
    onWeather(ready ? weatherInfo : null)
  }, [ready, weatherInfo, onWeather])

  // 지도에는 지금 보이는 결과만 그린다
  useEffect(() => {
    onRoute(showing ? showing.days : null)
  }, [showing, onRoute])
  useEffect(
    () => () => {
      onRoute(null)
      onWeather(null)
    },
    [onRoute, onWeather],
  )

  /** 새로 짜거나(exclude 없음) 지금 시작 장소를 빼고 다시 짠다. */
  async function request(excludeStartPlaceId: number | null) {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchTrip({
        region, area: tripArea, start, days, env, type, startPlaceId: null, excludeStartPlaceId,
      })
      if (data.startPlaceId === null) {
        setError('조건에 맞는 장소가 없어요. 실내/실외 선택이나 테마를 바꿔 보세요.')
        return
      }
      setTrip({ key, data })
      onUpdate({ sp: data.startPlaceId })
    } catch (e) {
      setError(messageOf(e))
    } finally {
      setLoading(false)
    }
  }

  /** 일정을 바꾸면 출발일이 예보 범위를 넘지 않게 맞춘다. */
  function pickDays(next: number) {
    const allowed = startOptions(dayList, next)
    const nextStart = allowed.some((d) => d.date === start) ? start : allowed[allowed.length - 1].date
    onUpdate({ days: next, start: nextStart })
  }

  if (showing) {
    return (
      <TripResult
        key={`${showing.startPlaceId}-${key}`}
        trip={showing}
        me={props.me}
        onLogin={props.onLogin}
        selectedId={props.selectedId}
        onSelect={props.onSelect}
        onReroll={() => request(sp)}
        rerolling={loading}
        onEdit={() => onUpdate({ sp: null })}
      />
    )
  }

  const startPreview = previews.find((p) => p.date === start)
  const recommended = startPreview && 'data' in startPreview ? startPreview.data.recommendedEnv : null

  return (
    <div className="trip-form">
      <div className="field-row">
        <span className="field-label">출발일</span>
        <TripCalendar starts={starts} start={start} days={days} onPick={(date) => onUpdate({ start: date })} />
      </div>

      <div className="field-row">
        <span className="field-label">일정</span>
        <div className="regions" role="group" aria-label="일정">
          {TRIP_LENGTHS.map((t) => (
            <button key={t.days} type="button" className="seg" aria-pressed={t.days === days} onClick={() => pickDays(t.days)}>
              {t.name}
            </button>
          ))}
        </div>
      </div>

      <div className="field-row">
        <span className="field-label">지역</span>
        <Controls
          mode="trip"
          region={region}
          onRegion={(code) => onUpdate({ region: code, area: null })}
          areas={areas}
          area={area}
          onArea={(code) => onUpdate({ area: code })}
        />
        {!ready && <p className="hint">경기는 넓어서 시·군 안에서 코스를 짜요. 시·군을 골라 주세요.</p>}
      </div>

      <div className="field-row">
        <span className="field-label">실내·실외</span>
        <EnvPicker value={env} recommended={recommended} onChange={(next) => onUpdate({ env: next })} />
      </div>

      <div className="field-row">
        <span className="field-label">테마</span>
        <div className="env-picker theme-picker" role="group" aria-label="테마">
          {TYPES.map((t) => (
            <button
              key={t.name}
              type="button"
              className="env-btn"
              aria-pressed={t.code === type}
              onClick={() => onUpdate({ type: t.code })}
            >
              {t.name}
            </button>
          ))}
        </div>
      </div>

      <div className="trip-actions">
        <button type="button" className="btn-more" disabled={!ready || loading} onClick={() => request(null)}>
          {loading ? '코스를 짜는 중' : '추천 받기'}
        </button>
      </div>
      {error && (
        <p className="trip-notice" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
