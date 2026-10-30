import { useEffect, useState } from 'react'
import { ApiError, fetchMe, fetchRecommendations, logout } from './api.ts'
import type { Me, Place, Recommendation } from './api.ts'
import Controls from './assets/Controls.tsx'
import Header from './assets/Header.tsx'
import Hero from './assets/Hero.tsx'
import PlaceList from './assets/PlaceList.tsx'
import { REGIONS, dayOptions, moodOf } from './labels.ts'

const PAGE_SIZE = 12

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : '알 수 없는 오류가 났어요. 잠시 뒤에 다시 시도해 주세요.'
}

export default function App() {
  const [days] = useState(() => dayOptions(new Date()))
  const [region, setRegion] = useState(REGIONS[0].code)
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

  const date = days[offset].date

  // 지역, 날짜, 종류가 바뀔 때마다 첫 페이지부터 다시 불러온다
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    setMoreError(null)
    fetchRecommendations({ region, date, type, page: 0, size: PAGE_SIZE })
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
  }, [region, date, type, reloadKey])

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
      const res = await fetchRecommendations({ region, date, type, page: page + 1, size: PAGE_SIZE })
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

  const hasMore = data !== null && page + 1 < data.places.totalPages

  return (
    <div className="page">
      <Header me={me} onLogout={handleLogout} />
      <Controls days={days} offset={offset} onOffset={setOffset} region={region} onRegion={setRegion} />

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
    </div>
  )
}
