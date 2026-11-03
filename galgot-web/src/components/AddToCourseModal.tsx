import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Plus } from 'lucide-react'
import { ApiError, addCoursePlace, createCourse, fetchCourses } from '../api'
import type { CourseSummary, Place } from '../api'
import { MAX_PLACES_PER_COURSE, defaultDay, firstAddableCourse } from '../course'
import { TRIP_LENGTHS, tripLengthName } from '../labels'
import Modal from './Modal'
import TripCalendar from './TripCalendar'

/** 넣은 결과. 장소 상세가 "'코스' 2일차에 넣었어요 [코스 보기]"로 보여 준다 */
export interface Added {
  courseId: number
  title: string
  dayNo: number
}

interface Props {
  place: Place
  onClose: () => void
  onAdded: (added: Added) => void
  /** 로그인이 풀렸을 때(401) */
  onLoginNeeded: () => void
}

type Load = { kind: 'loading' } | { kind: 'ready'; list: CourseSummary[] } | { kind: 'error'; message: string }

/** 고른 대상: 기존 코스 id 또는 새 코스 */
type Target = number | 'new'

function localDateString(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function shortDate(date: string): string {
  const [, m, d] = date.split('-')
  return `${Number(m)}/${Number(d)}`
}

/** "2026-11-03"에서 n일 뒤를 "11/4"처럼 */
function dayDate(travelDate: string | null, dayNo: number): string | null {
  if (travelDate === null) return null
  const [y, m, d] = travelDate.split('-').map(Number)
  const date = new Date(y, m - 1, d + dayNo - 1)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

function messageOf(e: unknown, fallback: string): string {
  return e instanceof ApiError ? e.message : fallback
}

/**
 * 나만의 코스에 넣기 팝업(기획서 18장 D). 화면 안 팝업이다(브라우저 alert를 쓰지 않는다).
 * 내 코스 목록(그 장소가 이미 담긴 코스와 50곳이 찬 코스는 고를 수 없음)과 [+ 새 코스 만들기] 중에서 고르고,
 * 일정 안의 일차를 고르면 그 일차 맨 뒤에 넣는다. 새 코스는 이름, 출발일(오늘 이후), 일정을 정하고 장소와 함께 한 번에 저장한다.
 */
export default function AddToCourseModal({ place, onClose, onAdded, onLoginNeeded }: Props) {
  // 오늘 날짜는 팝업을 연 때 한 번만 정한다(렌더마다 바뀌지 않게)
  const [today] = useState(() => localDateString(new Date()))
  const [load, setLoad] = useState<Load>({ kind: 'loading' })
  const [reloadKey, setReloadKey] = useState(0)
  const [target, setTarget] = useState<Target | null>(null)
  const [dayNo, setDayNo] = useState(1)
  const [title, setTitle] = useState(`나만의 코스 ${shortDate(today)}`)
  const [travelDate, setTravelDate] = useState(today)
  const [tripDays, setTripDays] = useState(1)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // 부모가 렌더마다 새 함수를 넘겨도 목록을 다시 부르지 않도록 ref로 받는다
  const loginNeeded = useRef(onLoginNeeded)
  useEffect(() => {
    loginNeeded.current = onLoginNeeded
  })

  useEffect(() => {
    let cancelled = false
    fetchCourses(place.id)
      .then((list) => {
        if (cancelled) return
        setLoad({ kind: 'ready', list })
        // 처음 골라 둘 코스와 일차. 고를 수 있는 코스가 없으면 새 코스 만들기
        const first = firstAddableCourse(list)
        setTarget(first ? first.id : 'new')
        setDayNo(first ? defaultDay(first) : 1)
      })
      .catch((e: unknown) => {
        if (cancelled) return
        if (e instanceof ApiError && e.status === 401) loginNeeded.current()
        setLoad({ kind: 'error', message: messageOf(e, '내 코스를 불러오지 못했어요.') })
      })
    return () => {
      cancelled = true
    }
  }, [place.id, reloadKey])

  const list = load.kind === 'ready' ? load.list : []
  const selected = typeof target === 'number' ? (list.find((c) => c.id === target) ?? null) : null
  const days = target === 'new' ? tripDays : (selected?.tripDays ?? 1)
  const dateOfDays = target === 'new' ? travelDate : (selected?.travelDate ?? null)

  function pickCourse(c: CourseSummary) {
    setTarget(c.id)
    setDayNo(defaultDay(c))
    setError(null)
  }

  function pickNew() {
    setTarget('new')
    setDayNo((d) => Math.min(d, tripDays))
    setError(null)
  }

  function pickTripDays(n: number) {
    setTripDays(n)
    setDayNo((d) => Math.min(d, n))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (target === null) return
    setBusy(true)
    setError(null)
    try {
      if (target === 'new') {
        const name = title.trim()
        if (name === '') {
          setError('코스 이름을 적어 주세요.')
          return
        }
        const created = await createCourse({ title: name, travelDate, tripDays, placeId: place.id, dayNo })
        onAdded({ courseId: created.id, title: created.title, dayNo })
      } else if (selected) {
        await addCoursePlace(selected.id, place.id, dayNo)
        onAdded({ courseId: selected.id, title: selected.title, dayNo })
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onLoginNeeded()
        return
      }
      setError(messageOf(err, '넣지 못했어요. 잠시 뒤에 다시 시도해 주세요.'))
      // 다른 탭에서 이미 넣었으면(409) 목록을 다시 불러와 "이미 담김"을 보여 준다
      if (err instanceof ApiError && err.status === 409) setReloadKey((k) => k + 1)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title={`나만의 코스에 넣기 · ${place.title}`} onClose={onClose} medium>
      <form className="add-course" onSubmit={submit}>
        {/* 내용만 위아래로 스크롤하고, 아래 [취소] [넣기]는 늘 보이게 둔다 */}
        <div className="add-course-scroll">
        {load.kind === 'loading' && <p className="muted">내 코스를 불러오는 중이에요</p>}
        {load.kind === 'error' && <p role="alert">{load.message}</p>}
        {load.kind === 'ready' && (
          <>
            <fieldset className="add-course-list">
              <legend className="field-label">어느 코스에 넣을까요?</legend>
              {/* 코스가 많아도 바로 보이게 맨 위에 둔다 */}
              <label className="add-course-option">
                <input type="radio" name="course" checked={target === 'new'} onChange={pickNew} />
                <span className="add-course-name">
                  <Plus size={14} aria-hidden="true" /> 새 코스 만들기
                </span>
              </label>
              {list.map((c) => {
                const full = c.placeCount >= MAX_PLACES_PER_COURSE
                const blocked = c.containsPlace === true || full
                return (
                  <label key={c.id} className={blocked ? 'add-course-option is-blocked' : 'add-course-option'}>
                    <input
                      type="radio"
                      name="course"
                      checked={target === c.id}
                      disabled={blocked}
                      onChange={() => pickCourse(c)}
                    />
                    <span className="add-course-name">{c.title}</span>
                    <span className="muted">
                      {tripLengthName(c.tripDays)} · {c.placeCount}곳
                    </span>
                    {c.containsPlace === true && <span className="add-course-tag">이미 담김</span>}
                    {c.containsPlace !== true && full && <span className="add-course-tag">가득 참(50곳)</span>}
                  </label>
                )
              })}
            </fieldset>

            {target === 'new' && (
              // 넓은 화면에서는 왼쪽에 이름·일정, 오른쪽에 달력(높이를 줄여 화면 안에 들어오게)
              <div className="add-course-new">
                <div className="add-course-new-fields">
                  <label>
                    <span className="field-label">코스 이름</span>
                    <input value={title} maxLength={100} onChange={(e) => setTitle(e.target.value)} required />
                  </label>
                  <div>
                    <span className="field-label">일정</span>
                  <div className="regions" role="group" aria-label="일정">
                    {TRIP_LENGTHS.map((t) => (
                      <button
                        key={t.days}
                        type="button"
                        className="seg"
                        aria-pressed={t.days === tripDays}
                        onClick={() => pickTripDays(t.days)}
                      >
                        {t.name}
                      </button>
                    ))}
                  </div>
                  </div>
                </div>
                <div>
                  <span className="field-label">출발일</span>
                  <TripCalendar minDate={today} start={travelDate} days={tripDays} onPick={setTravelDate} />
                </div>
              </div>
            )}

            {target !== null && (
              <div>
                <span className="field-label">몇 일차에 넣을까요?</span>
                <div className="regions" role="group" aria-label="일차">
                  {Array.from({ length: days }, (_, i) => i + 1).map((n) => {
                    const date = dayDate(dateOfDays, n)
                    return (
                      <button
                        key={n}
                        type="button"
                        className="seg"
                        aria-pressed={n === dayNo}
                        onClick={() => setDayNo(n)}
                      >
                        {n}일차{date ? ` · ${date}` : ''}
                      </button>
                    )
                  })}
                </div>
              </div>
            )}
          </>
        )}

        </div>

        <p className="add-course-error" role="alert">
          {error}
        </p>
        <div className="modal-actions">
          <button type="button" className="btn-line" onClick={onClose}>
            취소
          </button>
          <button type="submit" className="btn-more" disabled={busy || load.kind !== 'ready' || target === null}>
            {busy ? '넣는 중' : '넣기'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
