import { useState } from 'react'
import { Sun, Umbrella } from 'lucide-react'
import { ApiError, addCoursePlace, createCourse, deleteCourse } from '../api'
import type { Me, Trip } from '../api'
import { DAY_STYLES, ENV_LABEL, REGIONS, shortAddr, tripLengthName, typeName } from '../labels'

interface Props {
  trip: Trip
  me: Me | null
  onLogin: () => void
  selectedId: number | null
  onSelect: (id: number) => void
  /** 지금 시작 장소를 빼고 다시 짠다 */
  onReroll: () => void
  rerolling: boolean
  /** 조건 입력으로 돌아간다 */
  onEdit: () => void
}

type SaveState = { kind: 'idle' } | { kind: 'saving' } | { kind: 'saved'; title: string } | { kind: 'error'; message: string }

function shortDate(date: string): string {
  const [, m, d] = date.split('-')
  return `${Number(m)}/${Number(d)}`
}

/** "가평군 1박2일 11/3"처럼 코스 이름을 정한다. */
function courseTitle(trip: Trip): string {
  return `${trip.areaName} ${tripLengthName(trip.dayCount)} ${shortDate(trip.start)}`
}

/**
 * 날씨기반 추천 결과. 일차 탭마다 그날 날씨, 판정, 방문 순서 카드를 보여 준다.
 * 지도에는 일차별 번호 마커와 선이 함께 그려진다(KakaoMap의 route).
 * 일차별 날씨와 판정 안내는 옆 날씨 패널(TripWeatherPanel)이 보여 준다.
 */
export default function TripResult({ trip, me, onLogin, selectedId, onSelect, onReroll, rerolling, onEdit }: Props) {
  const [active, setActive] = useState(0)
  const [save, setSave] = useState<SaveState>({ kind: 'idle' })
  const day = trip.days[Math.min(active, trip.days.length - 1)]
  const regionName = REGIONS.find((r) => r.code === trip.region)?.name ?? ''
  const where = trip.area === trip.region ? trip.areaName : `${regionName} · ${trip.areaName}`
  const total = trip.days.reduce((n, d) => n + d.places.length, 0)

  /** 코스를 만들고 장소를 일차, 방문 순서대로 담는다. 중간에 실패하면 만든 코스를 지운다. */
  async function saveCourse() {
    if (!me) {
      onLogin()
      return
    }
    setSave({ kind: 'saving' })
    const title = courseTitle(trip)
    let courseId: number | null = null
    try {
      const course = await createCourse(title, trip.region, trip.start)
      courseId = course.id
      for (const d of trip.days) {
        for (const stop of d.places) {
          await addCoursePlace(course.id, stop.place.id, d.dayNo)
        }
      }
      setSave({ kind: 'saved', title })
    } catch (e) {
      if (courseId !== null) {
        await deleteCourse(courseId).catch(() => {
          // 지우지 못해도 화면에서는 실패로 안내한다
        })
      }
      if (e instanceof ApiError && e.status === 401) {
        setSave({ kind: 'idle' })
        onLogin()
        return
      }
      setSave({ kind: 'error', message: '코스를 저장하지 못했어요. 잠시 뒤에 다시 시도해 주세요.' })
    }
  }

  return (
    <section className="trip-result" aria-busy={rerolling}>
      <div className="trip-summary">
        <p>
          <strong>{where}</strong> · {tripLengthName(trip.dayCount)} · {shortDate(trip.start)} 출발
          {trip.env !== null && <> · {trip.env === 'ALL' ? '실내와 실외' : ENV_LABEL[trip.env]}</>}
        </p>
        <button type="button" className="link-btn" onClick={onEdit}>
          조건 바꾸기
        </button>
      </div>

      {trip.days.length > 1 && (
        <div className="day-tabs" role="tablist" aria-label="일차">
          {trip.days.map((d, i) => (
            <button
              key={d.dayNo}
              type="button"
              role="tab"
              id={`day-tab-${d.dayNo}`}
              aria-selected={i === active}
              aria-controls="day-panel"
              className="day-tab"
              style={{ borderBottomColor: i === active ? DAY_STYLES[i % DAY_STYLES.length].color : undefined }}
              onClick={() => setActive(i)}
            >
              {d.dayNo}일차 <span className="day-date">{shortDate(d.date)}</span>
              {/* 일차마다 그날 날씨로 판정한 추천을 아이콘으로 보여 준다 */}
              <span className="day-rec" title={`${ENV_LABEL[d.recommendedEnv]} 추천`}>
                {d.recommendedEnv === 'INDOOR' ? (
                  <Umbrella size={14} aria-label="실내 추천" />
                ) : (
                  <Sun size={14} aria-label="실외 추천" />
                )}
              </span>
            </button>
          ))}
        </div>
      )}

      <div id="day-panel" role={trip.days.length > 1 ? 'tabpanel' : undefined} aria-labelledby={trip.days.length > 1 ? `day-tab-${day.dayNo}` : undefined}>
        {day.notice && <p className="trip-notice">{day.notice}</p>}
        <ol className="stops">
          {day.places.map((stop) => (
            <li key={stop.place.id}>
              <button
                type="button"
                className={stop.place.id === selectedId ? 'stop is-selected' : 'stop'}
                aria-current={stop.place.id === selectedId ? 'true' : undefined}
                onClick={() => onSelect(stop.place.id)}
              >
                <span className="stop-no" style={{ background: DAY_STYLES[active % DAY_STYLES.length].color }}>
                  {stop.order}
                </span>
                <span className="tile-body">
                  <span className="tile-title">{stop.place.title}</span>
                  <span className="addr">{shortAddr(stop.place.addr)}</span>
                  <span className="meta">
                    <span className={`env env-${stop.place.env}`}>{ENV_LABEL[stop.place.env]}</span>
                    <span>{typeName(stop.place.contentTypeId)}</span>
                    {stop.nearby && <span className="nearby">주변 지역</span>}
                  </span>
                </span>
              </button>
            </li>
          ))}
        </ol>
      </div>

      <div className="trip-actions">
        {trip.rerollAvailable && (
          <button type="button" className="btn-line" onClick={onReroll} disabled={rerolling}>
            {rerolling ? '다시 짜는 중' : '다른 추천 받기'}
          </button>
        )}
        <button
          type="button"
          className="btn-more"
          onClick={saveCourse}
          disabled={total === 0 || save.kind === 'saving' || save.kind === 'saved'}
        >
          {save.kind === 'saving' ? '저장하는 중' : save.kind === 'saved' ? '저장했어요' : '내 코스에 저장'}
        </button>
      </div>
      <p className="trip-foot" aria-live="polite">
        {save.kind === 'saved' && `"${save.title}" 코스를 내 코스에 저장했어요(비공개).`}
        {save.kind === 'error' && save.message}
        {save.kind === 'idle' && !me && '저장하려면 로그인이 필요해요.'}
      </p>
    </section>
  )
}
