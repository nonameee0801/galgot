import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ArrowLeft, BookmarkPlus, Map as MapIcon } from 'lucide-react'
import { ApiError, copySharedCourse, fetchSharedCourse } from '../api'
import type { CourseDetail, Me } from '../api'
import CourseDays from '../components/CourseDays'
import CourseMapModal from '../components/CourseMapModal'
import SavedModal from '../components/SavedModal'

interface Props {
  me: Me | null
  meLoaded: boolean
  onLogin: () => void
}

type Load = { code: string; course: CourseDetail } | { code: string; error: string }

type Save =
  | { kind: 'idle' }
  | { kind: 'saving' }
  | { kind: 'saved'; courseId: number }
  | { kind: 'error'; message: string }
  /** 코스 수 상한(409, 기획서 18장 G) */
  | { kind: 'limit'; message: string }

/**
 * 공유 코스 보기(/s/:shareCode, 기획서 18장 E). 링크를 아는 사람은 비회원도 읽기 전용으로 본다.
 * [지도로 보기]는 지도 팝업을 열고, 로그인했으면 [내 코스로 저장]으로 복사본을 내 코스에 만든다
 * (한 번 저장하면 이 화면에서는 버튼을 잠근다).
 */
export default function SharedCoursePage({ me, meLoaded, onLogin }: Props) {
  const code = useParams().shareCode ?? ''
  const [load, setLoad] = useState<Load | null>(null)
  // 지도 팝업: null이면 닫힘, placeId가 있으면 그 장소를 고른 채로 연다
  const [map, setMap] = useState<{ placeId: number | null } | null>(null)
  const [save, setSave] = useState<Save>({ kind: 'idle' })
  // 저장 완료 팝업(2026-11-04 사용자 결정). [괜찮아요]는 팝업만 닫는다
  const [savedPopup, setSavedPopup] = useState(false)

  useEffect(() => {
    let cancelled = false
    fetchSharedCourse(code)
      .then((course) => {
        if (!cancelled) setLoad({ code, course })
      })
      .catch((e: unknown) => {
        if (!cancelled) setLoad({ code, error: e instanceof ApiError ? e.message : '코스를 불러오지 못했어요.' })
      })
    return () => {
      cancelled = true
    }
  }, [code])

  async function saveToMine() {
    setSave({ kind: 'saving' })
    try {
      const copy = await copySharedCourse(code)
      setSave({ kind: 'saved', courseId: copy.id })
      setSavedPopup(true)
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        setSave({ kind: 'idle' })
        onLogin()
        return
      }
      if (e instanceof ApiError && e.status === 409) {
        setSave({ kind: 'limit', message: e.message })
        return
      }
      setSave({ kind: 'error', message: e instanceof ApiError ? e.message : '내 코스로 저장하지 못했어요.' })
    }
  }

  const current = load !== null && load.code === code ? load : null

  return (
    <main className="page">
      <div className="page-inner">
        <Link className="link-btn back-btn" to="/">
          <ArrowLeft size={16} aria-hidden="true" />
          갈곳 지도로
        </Link>
        {current === null && <p className="muted">불러오는 중이에요</p>}
        {current !== null && 'error' in current && <p role="alert">{current.error}</p>}
        {current !== null && 'course' in current && (
          <>
            <p className="eyebrow">공유받은 코스</p>
            <h1 className="page-title">{current.course.title}</h1>
            {current.course.travelDate && <p className="meta">{current.course.travelDate} 출발</p>}

            <div className="share-save">
              <button
                type="button"
                className="btn-line map-open-btn"
                onClick={() => setMap({ placeId: null })}
                disabled={current.course.places.length === 0}
              >
                <MapIcon size={16} aria-hidden="true" />
                지도로 보기
              </button>
              {meLoaded && me !== null && (
                <button
                  type="button"
                  className="btn-more map-open-btn"
                  onClick={saveToMine}
                  disabled={save.kind === 'saving' || save.kind === 'saved'}
                >
                  <BookmarkPlus size={16} aria-hidden="true" />
                  {save.kind === 'saving' ? '저장하는 중' : save.kind === 'saved' ? '저장했어요' : '내 코스로 저장'}
                </button>
              )}
              {meLoaded && me === null && (
                <span className="muted">
                  로그인하면 내 코스로 저장할 수 있어요.{' '}
                  <button type="button" className="link-btn" onClick={onLogin}>
                    로그인
                  </button>
                </span>
              )}
            </div>
            <p className="course-notice" aria-live="polite">
              {save.kind === 'error' && save.message}
              {save.kind === 'limit' && (
                <>
                  {save.message}{' '}
                  <Link className="link-btn" to="/my">
                    내 코스 정리하기
                  </Link>
                </>
              )}
            </p>

            {save.kind === 'saved' && savedPopup && (
              <SavedModal title={current.course.title} courseId={save.courseId} onLater={() => setSavedPopup(false)} />
            )}
            <CourseDays course={current.course} onShowOnMap={(placeId) => setMap({ placeId })} />
            {map && (
              <CourseMapModal
                key={map.placeId ?? 'all'}
                course={current.course}
                initialPlaceId={map.placeId}
                onClose={() => setMap(null)}
              />
            )}
          </>
        )}
      </div>
    </main>
  )
}
