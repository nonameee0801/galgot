import { useEffect, useState } from 'react'
import { ApiError, fetchAreas, fetchMe, fetchRecommendations, logout } from './api.ts'
import type { Area, Me, Place, Recommendation } from './api.ts'
import AuthModal from './components/AuthModal.tsx'
import Controls from './components/Controls.tsx'
import Header from './components/Header.tsx'
import Hero from './components/Hero.tsx'
import PlaceList from './components/PlaceList.tsx'
import WithdrawModal from './components/WithdrawModal.tsx'
import { REGIONS, dayOptions, moodOf } from './labels.ts'

const PAGE_SIZE = 12

type OpenModal = 'auth' | 'withdraw' | null

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : '알 수 없는 오류가 났어요. 잠시 뒤에 다시 시도해 주세요.'
}

/** 카카오 로그인에 실패하면 백엔드가 ?login=failed 를 붙여 돌려보낸다. 한 번 읽고 주소에서 지운다. */
function readLoginFailed(): boolean {
  const url = new URL(window.location.href)
  if (url.searchParams.get('login') !== 'failed') return false
  url.searchParams.delete('login')
  window.history.replaceState(null, '', url.pathname + url.search + url.hash)
  return true
}

export default function App() {
  const [days] = useState(() => dayOptions(new Date()))
  const [region, setRegion] = useState(REGIONS[0].code)
  const [areas, setAreas] = useState<Area[]>([])
  const [area, setArea] = useState<string | null>(null)
  const [offset, setOffset] = useState(0)
  const [type, setType] = useState<number | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const [data, setData] = useState<Recommendation | null>(null)
  const [places, setPlaces] = useState<Place[]>([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState<string | null>(null)
  const [me, setMe] = useState<Me | null>(null)
  const [modal, setModal] = useState<OpenModal>(null)
  const [loginFailed, setLoginFailed] = useState(readLoginFailed)

  const date = days[offset].date

  // 시도가 바뀌면 그 시도의 구·시군 목록을 다시 받는다
  useEffect(() => {
    let cancelled = false
    setAreas([])
    fetchAreas(region)
      .then((list) => {
        if (!cancelled) setAreas(list)
      })
      .catch(() => {
        // 목록을 못 받아도 시도 전체 추천은 볼 수 있다
      })
    return () => {
      cancelled = true
    }
  }, [region])

  // 지역, 날짜, 종류가 바뀔 때마다 첫 페이지부터 다시 불러온다
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    setMoreError(null)
    fetchRecommendations({ region, area, date, type, page: 0, size: PAGE_SIZE })
      .then((res) => {
        if (cancelled) return
        setData(res)
        setPlaces(res.places.items)
        setPage(0)
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
  }, [region, area, date, type, reloadKey])

  // 날씨에 따라 페이지 전체 분위기(배경색)를 바꾼다
  useEffect(() => {
    document.documentElement.dataset.mood = data ? moodOf(data.weather) : 'cloud'
  }, [data])

  useEffect(() => {
    fetchMe().then(setMe)
  }, [])

  async function loadMore() {
    setLoadingMore(true)
    setMoreError(null)
    try {
      const res = await fetchRecommendations({ region, area, date, type, page: page + 1, size: PAGE_SIZE })
      setPlaces((prev) => [...prev, ...res.places.items])
      setPage(res.places.page)
    } catch (e) {
      setMoreError(messageOf(e))
    } finally {
      setLoadingMore(false)
    }
  }

  async function handleLogout() {
    try {
      await logout()
      setMe(null)
    } catch {
      // 실패하면 로그인 상태를 그대로 둔다
    }
  }

  function handleAuthSuccess(next: Me) {
    setMe(next)
    setModal(null)
    setLoginFailed(false)
  }

  function handleWithdrawn() {
    setMe(null)
    setModal(null)
  }

  /** 시도를 바꾸면 구·시군은 "전체"로 되돌린다. */
  function handleRegion(code: string) {
    setRegion(code)
    setArea(null)
  }

  const hasMore = data !== null && page + 1 < data.places.totalPages

  return (
    <div className="page">
      <Header
        me={me}
        onLogin={() => setModal('auth')}
        onLogout={handleLogout}
        onWithdraw={() => setModal('withdraw')}
      />
      {loginFailed && (
        <div className="banner" role="alert">
          <span>카카오 로그인에 실패했어요. 다시 시도해 주세요.</span>
          <button type="button" className="link-btn" onClick={() => setLoginFailed(false)}>
            닫기
          </button>
        </div>
      )}
      <Controls
        days={days}
        offset={offset}
        onOffset={setOffset}
        region={region}
        onRegion={handleRegion}
        areas={areas}
        area={area}
        onArea={setArea}
      />

      {error ? (
        <div className="notice">
          <h2>불러오지 못했어요</h2>
          <p>{error}</p>
          <button type="button" className="btn-line" onClick={() => setReloadKey((k) => k + 1)}>
            다시 시도
          </button>
        </div>
      ) : (
        <main className={loading ? 'is-loading' : undefined} aria-busy={loading}>
          <Hero day={days[offset]} data={data} />
          <PlaceList
            places={places}
            total={data?.places.totalElements ?? 0}
            type={type}
            onType={setType}
            loading={loading}
            hasMore={hasMore}
            loadingMore={loadingMore}
            moreError={moreError}
            onMore={loadMore}
          />
        </main>
      )}

      <footer className="footer">날씨는 기상청 단기예보, 장소 정보는 한국관광공사 TourAPI 자료예요.</footer>

      {modal === 'auth' && <AuthModal onClose={() => setModal(null)} onSuccess={handleAuthSuccess} />}
      {modal === 'withdraw' && <WithdrawModal onClose={() => setModal(null)} onDone={handleWithdrawn} />}
    </div>
  )
}
