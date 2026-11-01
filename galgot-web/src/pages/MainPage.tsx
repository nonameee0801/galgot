import { useEffect, useState } from 'react'
import { ApiError, fetchAreas, fetchRecommendations } from '../api.ts'
import type { Area, Place, Recommendation } from '../api.ts'
import Controls from '../components/Controls.tsx'
import MapPlaceholder from '../components/MapPlaceholder.tsx'
import PlaceList from '../components/PlaceList.tsx'
import SidePanel from '../components/SidePanel.tsx'
import WeatherCard from '../components/WeatherCard.tsx'
import { REGIONS, dayOptions, moodOf } from '../labels.ts'

const PAGE_SIZE = 12

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : '알 수 없는 오류가 났어요. 잠시 뒤에 다시 시도해 주세요.'
}

/**
 * 지도 메인 화면. 지도(2단계에서 카카오 지도) 위에 왼쪽 목록 패널과 오른쪽 위 날씨 카드를 띄운다.
 * 목록과 날씨는 항상 오늘 기준이고, 지역과 종류가 바뀔 때마다 추천을 다시 불러온다.
 */
export default function MainPage() {
  const [today] = useState(() => dayOptions(new Date())[0])
  const [region, setRegion] = useState(REGIONS[0].code)
  const [areas, setAreas] = useState<Area[]>([])
  const [area, setArea] = useState<string | null>(null)
  const [type, setType] = useState<number | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const [data, setData] = useState<Recommendation | null>(null)
  const [places, setPlaces] = useState<Place[]>([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState<string | null>(null)

  const date = today.date

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

  // 지역, 종류가 바뀔 때마다 첫 페이지부터 다시 불러온다
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

  /** 시도를 바꾸면 구·시군은 "전체"로 되돌린다. */
  function handleRegion(code: string) {
    setRegion(code)
    setArea(null)
  }

  const hasMore = data !== null && page + 1 < data.places.totalPages

  return (
    <div className="stage">
      <MapPlaceholder />

      <SidePanel title="관광지 목록">
        <Controls
          region={region}
          onRegion={handleRegion}
          areas={areas}
          area={area}
          onArea={setArea}
        />

        {error ? (
          <div className="notice">
            <h3>불러오지 못했어요</h3>
            <p>{error}</p>
            <button type="button" className="btn-line" onClick={() => setReloadKey((k) => k + 1)}>
              다시 시도
            </button>
          </div>
        ) : (
          <div className={loading ? 'panel-list is-loading' : 'panel-list'} aria-busy={loading}>
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
          </div>
        )}

        <footer className="footer">날씨는 기상청 단기예보, 장소 정보는 한국관광공사 TourAPI 자료예요.</footer>
      </SidePanel>

      <WeatherCard day={today} data={error ? null : data} />
    </div>
  )
}
