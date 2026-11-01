import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { ApiError, fetchAreas, fetchRecommendations } from '../api.ts'
import type { Area, EnvChoice, Place, Recommendation } from '../api.ts'
import Controls from '../components/Controls.tsx'
import EnvPicker from '../components/EnvPicker.tsx'
import MapPlaceholder from '../components/MapPlaceholder.tsx'
import PlaceList from '../components/PlaceList.tsx'
import SidePanel from '../components/SidePanel.tsx'
import WeatherCard from '../components/WeatherCard.tsx'
import { ENV_CHOICES, REGIONS, TYPES, dayOptions, moodOf } from '../labels.ts'

const PAGE_SIZE = 12
const DEFAULT_REGION = REGIONS[0].code

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : '알 수 없는 오류가 났어요. 잠시 뒤에 다시 시도해 주세요.'
}

// 주소의 값은 누구나 바꿔 넣을 수 있으므로, 모르는 값은 기본값으로 돌린다.
function readRegion(v: string | null): string {
  return REGIONS.some((r) => r.code === v) ? (v as string) : DEFAULT_REGION
}

function readArea(v: string | null, region: string): string | null {
  return v !== null && /^\d{5}$/.test(v) && v.startsWith(region) ? v : null
}

function readEnv(v: string | null): EnvChoice | null {
  return ENV_CHOICES.find((c) => c.code === v)?.code ?? null
}

function readType(v: string | null): number | null {
  return TYPES.find((t) => t.code !== null && String(t.code) === v)?.code ?? null
}

interface Conditions {
  region: string
  area: string | null
  env: EnvChoice | null
  type: number | null
}

/**
 * 지도 메인 화면. 지도(2단계에서 카카오 지도) 위에 왼쪽 목록 패널과 오른쪽 위 날씨 카드를 띄운다.
 * 목록과 날씨는 항상 오늘 기준이다. 고른 조건(지역, 실내/실외, 종류)은 주소(?region=41&area=41820&env=INDOOR&type=14)에
 * 남겨 새로고침, 뒤로 가기, 공유 때 그대로 보인다.
 */
export default function MainPage() {
  const [params, setParams] = useSearchParams()
  const region = readRegion(params.get('region'))
  const area = readArea(params.get('area'), region)
  const env = readEnv(params.get('env'))
  const type = readType(params.get('type'))

  const [today] = useState(() => dayOptions(new Date())[0])
  const [areas, setAreas] = useState<Area[]>([])
  const [reloadKey, setReloadKey] = useState(0)

  const [data, setData] = useState<Recommendation | null>(null)
  const [places, setPlaces] = useState<Place[]>([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState<string | null>(null)

  const date = today.date

  /** 조건을 바꾸면 주소에 쓴다. 기본값(서울, 선택 없음)은 주소에서 뺀다. */
  function update(next: Partial<Conditions>) {
    setParams((prev) => {
      const p = new URLSearchParams(prev)
      const put = (key: string, value: string | number | null) => {
        if (value === null) p.delete(key)
        else p.set(key, String(value))
      }
      if (next.region !== undefined) put('region', next.region === DEFAULT_REGION ? null : next.region)
      if (next.area !== undefined) put('area', next.area)
      if (next.env !== undefined) put('env', next.env)
      if (next.type !== undefined) put('type', next.type)
      return p
    })
  }

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

  // 주소에 목록에 없는 구·시군 코드가 들어 있으면 "전체"로 되돌린다
  useEffect(() => {
    if (area === null || areas.length === 0 || areas.some((a) => a.code === area)) return
    setParams(
      (prev) => {
        const p = new URLSearchParams(prev)
        p.delete('area')
        return p
      },
      { replace: true },
    )
  }, [area, areas, setParams])

  // 조건이 바뀔 때마다 첫 페이지부터 다시 불러온다
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    setMoreError(null)
    fetchRecommendations({ region, area, date, type, env, page: 0, size: PAGE_SIZE })
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
  }, [region, area, date, type, env, reloadKey])

  // 날씨에 따라 페이지 전체 분위기(배경색)를 바꾼다
  useEffect(() => {
    document.documentElement.dataset.mood = data ? moodOf(data.weather) : 'cloud'
  }, [data])

  async function loadMore() {
    setLoadingMore(true)
    setMoreError(null)
    try {
      const res = await fetchRecommendations({ region, area, date, type, env, page: page + 1, size: PAGE_SIZE })
      setPlaces((prev) => [...prev, ...res.places.items])
      setPage(res.places.page)
    } catch (e) {
      setMoreError(messageOf(e))
    } finally {
      setLoadingMore(false)
    }
  }

  const hasMore = data !== null && page + 1 < data.places.totalPages

  return (
    <div className="stage">
      <MapPlaceholder />

      <SidePanel title="관광지 목록">
        <Controls
          region={region}
          onRegion={(code) => update({ region: code, area: null })}
          areas={areas}
          area={area}
          onArea={(code) => update({ area: code })}
        />
        <EnvPicker
          value={env}
          recommended={error ? null : (data?.recommendedEnv ?? null)}
          onChange={(next) => update({ env: next })}
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
              onType={(t) => update({ type: t })}
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
