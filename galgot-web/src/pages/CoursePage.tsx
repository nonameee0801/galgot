import { useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ArrowLeft, Copy, Map as MapIcon } from 'lucide-react'
import { ApiError, deleteCourse, fetchCourse, removeCoursePlace, reorderCourse, updateCourse } from '../api'
import type { CourseDetail, CoursePlaceItem, Me } from '../api'
import CourseDays, { ConfirmModal } from '../components/CourseDays'
import CourseMapModal from '../components/CourseMapModal'
import { LoginNeeded } from './MyPage'
import { TRIP_LENGTHS, tripLengthName } from '../labels'

interface Props {
  me: Me | null
  meLoaded: boolean
  onLogin: () => void
}

type Load = { kind: 'loading' } | { kind: 'ready'; course: CourseDetail } | { kind: 'error'; message: string; status: number }

function messageOf(e: unknown, fallback: string): string {
  return e instanceof ApiError ? e.message : fallback
}

/**
 * 내 코스 상세(/my/courses/:id, 기획서 18장 E).
 * 일차별 장소(위·아래로 순서 바꾸기, 빼기, 지도에서 보기), 이름·날짜 수정, 공유 링크 복사, 지도 팝업, 코스 삭제.
 * 공개/비공개는 없다. 공유 링크를 아는 사람은 비회원도 볼 수 있다(2026-11-03 결정).
 */
export default function CoursePage({ me, meLoaded, onLogin }: Props) {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const [load, setLoad] = useState<Load>({ kind: 'loading' })
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [editingInfo, setEditingInfo] = useState(false)
  const [title, setTitle] = useState('')
  const [travelDate, setTravelDate] = useState('')
  const [tripDays, setTripDays] = useState(1)
  const [confirmDelete, setConfirmDelete] = useState(false)
  // 지도 팝업: null이면 닫힘, placeId가 있으면 그 장소를 고른 채로 연다
  const [map, setMap] = useState<{ placeId: number | null } | null>(null)
  const userId = me?.id ?? null

  useEffect(() => {
    if (userId === null || !Number.isInteger(id) || id <= 0) return
    let cancelled = false
    fetchCourse(id)
      .then((course) => {
        if (!cancelled) setLoad({ kind: 'ready', course })
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setLoad({
            kind: 'error',
            message: messageOf(e, '코스를 불러오지 못했어요.'),
            status: e instanceof ApiError ? e.status : 0,
          })
        }
      })
    return () => {
      cancelled = true
    }
  }, [id, userId])

  /** 바꾸는 요청을 하나씩 보내고(겹치지 않게), 실패하면 안내만 하고 화면은 그대로 둔다. 성공하면 true */
  async function run(action: () => Promise<CourseDetail | void>, done?: string): Promise<boolean> {
    setBusy(true)
    setNotice(null)
    try {
      const next = await action()
      const course = next ?? (await fetchCourse(id))
      setLoad({ kind: 'ready', course })
      if (done) setNotice(done)
      return true
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) onLogin()
      setNotice(messageOf(e, '바꾸지 못했어요. 잠시 뒤에 다시 시도해 주세요.'))
      return false
    } finally {
      setBusy(false)
    }
  }

  if (!meLoaded) return <PageShell>불러오는 중이에요</PageShell>
  if (me === null) return <PageShell><LoginNeeded onLogin={onLogin} /></PageShell>
  if (!Number.isInteger(id) || id <= 0) return <PageShell><p role="alert">코스를 찾을 수 없어요.</p></PageShell>
  if (load.kind === 'loading') return <PageShell><p className="muted">불러오는 중이에요</p></PageShell>
  if (load.kind === 'error') return <PageShell><p role="alert">{load.message}</p></PageShell>

  const course = load.course
  const shareUrl = `${window.location.origin}/s/${course.shareCode}`

  /** 같은 일차의 바로 위(아래) 장소와 순서를 맞바꾼다 */
  function move(item: CoursePlaceItem, step: -1 | 1) {
    const day = course.places.filter((p) => p.dayNo === item.dayNo)
    const i = day.findIndex((p) => p.id === item.id)
    const other = day[i + step]
    if (!other) return
    void run(() =>
      reorderCourse(course.id, [
        { coursePlaceId: item.id, dayNo: item.dayNo, visitOrder: other.visitOrder },
        { coursePlaceId: other.id, dayNo: other.dayNo, visitOrder: item.visitOrder },
      ]),
    )
  }

  function remove(item: CoursePlaceItem) {
    void run(() => removeCoursePlace(course.id, item.id), `"${item.place.title}"을(를) 삭제했어요.`)
  }

  function startEdit() {
    setTitle(course.title)
    setTravelDate(course.travelDate ?? '')
    setTripDays(course.tripDays)
    setEditingInfo(true)
  }

  function saveInfo(e: FormEvent) {
    e.preventDefault()
    const nextTitle = title.trim()
    if (nextTitle === '') {
      setNotice('코스 이름을 적어 주세요.')
      return
    }
    void run(
      () => updateCourse({ ...course, title: nextTitle, travelDate: travelDate === '' ? null : travelDate, tripDays }),
      '코스 정보를 바꿨어요.',
    ).then((ok) => {
      if (ok) setEditingInfo(false)
    })
  }

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(shareUrl)
      setNotice('공유 링크를 복사했어요. 링크를 받은 사람은 로그인하지 않아도 볼 수 있어요.')
    } catch {
      setNotice(`링크를 복사하지 못했어요. 직접 복사해 주세요: ${shareUrl}`)
    }
  }

  async function removeCourse() {
    setBusy(true)
    try {
      await deleteCourse(course.id)
      navigate('/my', { replace: true })
    } catch (e) {
      setNotice(messageOf(e, '코스를 지우지 못했어요.'))
      setBusy(false)
      setConfirmDelete(false)
    }
  }

  return (
    <PageShell>
      <header className="course-head">
        {editingInfo ? (
          <form className="course-edit" onSubmit={saveInfo}>
            <label>
              <span className="field-label">코스 이름</span>
              <input value={title} maxLength={100} onChange={(e) => setTitle(e.target.value)} required />
            </label>
            <label>
              <span className="field-label">여행 날짜(1일차)</span>
              <input type="date" value={travelDate} onChange={(e) => setTravelDate(e.target.value)} />
            </label>
            <div>
              <span className="field-label">일정</span>
              <div className="regions" role="group" aria-label="일정">
                {TRIP_LENGTHS.map((t) => {
                  // 장소가 든 일차보다 짧게는 줄일 수 없다(서버도 400)
                  const tooShort = course.places.some((p) => p.dayNo > t.days)
                  return (
                    <button
                      key={t.days}
                      type="button"
                      className="seg"
                      aria-pressed={t.days === tripDays}
                      disabled={tooShort}
                      title={tooShort ? '뒤 일차에 장소가 있어 줄일 수 없어요' : undefined}
                      onClick={() => setTripDays(t.days)}
                    >
                      {t.name}
                    </button>
                  )
                })}
              </div>
            </div>
            <div className="course-edit-actions">
              <button type="submit" className="btn-more" disabled={busy}>
                저장
              </button>
              <button type="button" className="link-btn" onClick={() => setEditingInfo(false)}>
                취소
              </button>
            </div>
          </form>
        ) : (
          <>
            <h1 className="page-title">{course.title}</h1>
            <p className="meta">
              <span>{course.travelDate ? `${course.travelDate} 출발` : '여행 날짜 없음'}</span>
              <span>{tripLengthName(course.tripDays)}</span>
              <button type="button" className="link-btn" onClick={startEdit} disabled={busy}>
                이름·날짜·일정 바꾸기
              </button>
            </p>
          </>
        )}
      </header>

      <section className="page-section course-share" aria-label="공유">
        <button
          type="button"
          className="btn-line"
          onClick={() => setMap({ placeId: null })}
          disabled={course.places.length === 0}
        >
          <MapIcon size={14} aria-hidden="true" />
          지도로 보기
        </button>
        <button type="button" className="btn-line" onClick={copyLink}>
          <Copy size={14} aria-hidden="true" />
          공유 링크 복사
        </button>
      </section>

      <p className="course-notice" aria-live="polite">
        {notice}
      </p>

      <CourseDays
        course={course}
        onShowOnMap={(placeId) => setMap({ placeId })}
        editing={{ onMove: move, onRemove: remove, busy }}
      />

      <section className="page-section course-danger" aria-label="코스 삭제">
        <button type="button" className="link-btn danger-text" onClick={() => setConfirmDelete(true)} disabled={busy}>
          코스 삭제
        </button>
      </section>
      {confirmDelete && (
        <ConfirmModal
          title="코스 삭제"
          message="코스를 삭제할까요? 삭제하면 되돌릴 수 없어요."
          onYes={removeCourse}
          onNo={() => setConfirmDelete(false)}
          busy={busy}
        />
      )}
      {map && (
        <CourseMapModal
          key={map.placeId ?? 'all'}
          course={course}
          initialPlaceId={map.placeId}
          onClose={() => setMap(null)}
        />
      )}
    </PageShell>
  )
}

function PageShell({ children }: { children: ReactNode }) {
  return (
    <main className="page">
      <div className="page-inner">
        <Link className="link-btn back-btn" to="/my">
          <ArrowLeft size={16} aria-hidden="true" />
          마이페이지
        </Link>
        {children}
      </div>
    </main>
  )
}
