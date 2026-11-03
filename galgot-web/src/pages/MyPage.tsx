import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { ArrowLeft, Map as MapIcon } from 'lucide-react'
import { ApiError, NICKNAME_MAX, fetchCourse, fetchCourses, updateNickname } from '../api'
import type { CourseDetail, CourseSummary, Me } from '../api'
import CourseMapModal from '../components/CourseMapModal'
import { REGIONS, tripLengthName } from '../labels'

interface Props {
  me: Me | null
  /** 로그인 여부를 아직 확인하는 중이면 false(그동안 "로그인이 필요해요"를 띄우지 않는다) */
  meLoaded: boolean
  onLogin: () => void
  onWithdraw: () => void
  /** 닉네임을 바꾸면 헤더의 닉네임도 함께 바뀌게 앱의 내 정보를 바꾼다 */
  onMeChange: (me: Me) => void
}

type Courses = { kind: 'loading' } | { kind: 'ready'; list: CourseSummary[] } | { kind: 'error'; message: string }

function shortDate(date: string | null): string | null {
  if (date === null) return null
  const [y, m, d] = date.slice(0, 10).split('-')
  return `${y}.${Number(m)}.${Number(d)}`
}

/** 로그인이 필요한 화면에서 로그인 전이면 보여 주는 안내 */
export function LoginNeeded({ onLogin }: { onLogin: () => void }) {
  return (
    <div className="notice page-notice">
      <h2>로그인이 필요해요</h2>
      <p>내 코스와 내 정보는 로그인한 뒤에 볼 수 있어요.</p>
      <button type="button" className="btn-more" onClick={onLogin}>
        로그인
      </button>
    </div>
  )
}

/** 내 정보의 닉네임. [바꾸기]를 누르면 그 자리가 입력칸과 [저장] [취소]로 바뀐다(기획서 18장 F). */
function NicknameRow({ me, onMeChange }: { me: Me; onMeChange: (me: Me) => void }) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(me.nickname)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function start() {
    setDraft(me.nickname)
    setError(null)
    setEditing(true)
  }

  function cancel() {
    setEditing(false)
    setError(null)
  }

  async function save(e: FormEvent) {
    e.preventDefault()
    const next = draft.trim()
    if (next === '') {
      setError('닉네임을 입력해 주세요.')
      return
    }
    if (next === me.nickname) {
      setEditing(false)
      return
    }
    setSaving(true)
    setError(null)
    try {
      onMeChange(await updateNickname(next))
      setEditing(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '닉네임을 바꾸지 못했어요.')
    } finally {
      setSaving(false)
    }
  }

  if (!editing) {
    return (
      <p className="nickname-row">
        닉네임 <strong>{me.nickname}</strong>
        <button type="button" className="link-btn" onClick={start}>
          바꾸기
        </button>
      </p>
    )
  }

  return (
    <form className="nickname-form" onSubmit={save}>
      <label htmlFor="nickname-input">닉네임</label>
      <input
        id="nickname-input"
        value={draft}
        maxLength={NICKNAME_MAX}
        autoFocus
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Escape') cancel()
        }}
        aria-describedby={error ? 'nickname-error' : undefined}
      />
      <span className="muted nickname-count">
        {draft.trim().length}/{NICKNAME_MAX}
      </span>
      <button type="submit" className="btn-more" disabled={saving}>
        {saving ? '저장 중' : '저장'}
      </button>
      <button type="button" className="btn-line" onClick={cancel} disabled={saving}>
        취소
      </button>
      {error && (
        <p id="nickname-error" className="form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  )
}

/**
 * 마이페이지(/my, 기획서 18장 E·F): 내 정보(닉네임 바꾸기), 내 코스 목록, 회원 탈퇴.
 * 코스 카드를 누르면 내 코스 상세(/my/courses/:id)로 가고, [지도로 보기]는 그 코스를 지도 팝업으로 연다.
 */
export default function MyPage({ me, meLoaded, onLogin, onWithdraw, onMeChange }: Props) {
  const [courses, setCourses] = useState<Courses>({ kind: 'loading' })
  const [mapCourse, setMapCourse] = useState<CourseDetail | null>(null)
  const [opening, setOpening] = useState<number | null>(null)
  const [mapError, setMapError] = useState<string | null>(null)
  const userId = me?.id ?? null

  useEffect(() => {
    if (userId === null) return
    let cancelled = false
    fetchCourses()
      .then((list) => {
        if (!cancelled) setCourses({ kind: 'ready', list })
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setCourses({ kind: 'error', message: e instanceof ApiError ? e.message : '내 코스를 불러오지 못했어요.' })
        }
      })
    return () => {
      cancelled = true
    }
  }, [userId])

  /** 카드에는 담긴 장소가 없어서, 지도 팝업을 열 때 코스 상세를 받아 온다 */
  async function openMap(courseId: number) {
    setOpening(courseId)
    setMapError(null)
    try {
      setMapCourse(await fetchCourse(courseId))
    } catch (e) {
      setMapError(e instanceof ApiError ? e.message : '코스를 불러오지 못했어요.')
    } finally {
      setOpening(null)
    }
  }

  return (
    <main className="page">
      <div className="page-inner">
        <Link className="link-btn back-btn" to="/">
          <ArrowLeft size={16} aria-hidden="true" />
          지도로
        </Link>
        <h1 className="page-title">마이페이지</h1>

        {!meLoaded ? (
          <p className="muted">불러오는 중이에요</p>
        ) : me === null ? (
          <LoginNeeded onLogin={onLogin} />
        ) : (
          <>
            <section className="page-section" aria-labelledby="my-info">
              <h2 id="my-info">내 정보</h2>
              <NicknameRow me={me} onMeChange={onMeChange} />
            </section>

            <section className="page-section" aria-labelledby="my-courses">
              <h2 id="my-courses">내 코스</h2>
              {courses.kind === 'loading' && <p className="muted">불러오는 중이에요</p>}
              {courses.kind === 'error' && <p role="alert">{courses.message}</p>}
              {courses.kind === 'ready' && courses.list.length === 0 && (
                <p className="muted">
                  아직 저장한 코스가 없어요. 날씨기반 추천에서 코스를 받아 [내 코스에 저장]을 눌러 보세요.
                </p>
              )}
              {courses.kind === 'ready' && courses.list.length > 0 && (
                <ul className="course-cards">
                  {courses.list.map((c) => {
                    const region = REGIONS.find((r) => r.code === c.ldongRegnCd)?.name
                    const travel = shortDate(c.travelDate)
                    return (
                      <li key={c.id} className="course-card-row">
                        <Link className="course-card" to={`/my/courses/${c.id}`}>
                          <strong className="course-card-title">{c.title}</strong>
                          <span className="meta">
                            {region && <span>{region}</span>}
                            {travel && <span>{travel} 출발</span>}
                            <span>
                              장소 {c.placeCount}곳 · {tripLengthName(c.tripDays)}
                            </span>
                            <span>{shortDate(c.createdAt)} 만듦</span>
                          </span>
                        </Link>
                        <button
                          type="button"
                          className="btn-line map-open-btn"
                          onClick={() => openMap(c.id)}
                          disabled={opening !== null || c.placeCount === 0}
                          aria-label={`${c.title} 지도로 보기`}
                        >
                          <MapIcon size={16} aria-hidden="true" />
                          {opening === c.id ? '여는 중' : '지도로 보기'}
                        </button>
                      </li>
                    )
                  })}
                </ul>
              )}
              {mapError && <p role="alert">{mapError}</p>}
            </section>

            <section className="page-section" aria-labelledby="my-account">
              <h2 id="my-account">계정</h2>
              <button type="button" className="btn-line danger" onClick={onWithdraw}>
                회원 탈퇴
              </button>
            </section>
          </>
        )}
      </div>
      {mapCourse && <CourseMapModal course={mapCourse} onClose={() => setMapCourse(null)} />}
    </main>
  )
}
