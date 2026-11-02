import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router'
import { ApiError, fetchAreas, fetchMarkers, fetchRecommendations } from '../api.ts'
import type { Area, EnvChoice, MarkerItem, Me, Place, Recommendation, TripDay } from '../api.ts'
import Controls from '../components/Controls.tsx'
import EnvPicker from '../components/EnvPicker.tsx'
import KakaoMap from '../components/KakaoMap.tsx'
import PlaceDetail from '../components/PlaceDetail.tsx'
import PlaceList from '../components/PlaceList.tsx'
import SidePanel from '../components/SidePanel.tsx'
import TripTab from '../components/TripTab.tsx'
import type { TripConditions, TripWeatherInfo } from '../components/TripTab.tsx'
import TripWeatherPanel from '../components/TripWeatherPanel.tsx'
import WeatherCard from '../components/WeatherCard.tsx'
import { ENV_CHOICES, REGIONS, TYPES, dayOptions, moodOf, startOptions } from '../labels.ts'
import type { DayOption } from '../labels.ts'

const PAGE_SIZE = 12
const DEFAULT_REGION = REGIONS[0].code
const PANEL_KEY = 'galgot.panelCollapsed'
const TABS = [
  { key: 'list', label: '관광지 목록' },
  { key: 'trip', label: '날씨기반 추천' },
]

/** 저장소를 쓸 수 없는 환경(사생활 보호 모드 등)에서도 화면은 그대로 동작해야 한다. */
function readCollapsed(): boolean {
  try {
    return window.localStorage.getItem(PANEL_KEY) === '1'
  } catch {
    return false
  }
}

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

function readPlaceId(v: string | null | undefined): number | null {
  return v !== undefined && v !== null && /^\d{1,12}$/.test(v) ? Number(v) : null
}

function readTab(v: string | null): 'list' | 'trip' {
  return v === 'trip' ? 'trip' : 'list'
}

function readDays(v: string | null): number {
  return v === '2' || v === '3' ? Number(v) : 1
}

/** 일정의 마지막 날까지 예보가 있는 출발일만 받는다. 아니면 오늘. */
function readStart(v: string | null, days: number, dayList: DayOption[]): string {
  return startOptions(dayList, days).some((d) => d.date === v) ? (v as string) : dayList[0].date
}

/**
 * 지도에 따로 그릴 수 있는 좌표인지(서울·경기 범위). 서버의 RecommendationService.inServiceArea와 같은 범위다.
 * TourAPI에 좌표가 엉뚱하게 들어간 장소가 있어, 그런 장소는 지도에 따로 그리지 않는다.
 */
function inServiceArea(lat: number | null, lng: number | null): boolean {
  return lat !== null && lng !== null && lat >= 36.8 && lat <= 38.4 && lng >= 126.0 && lng <= 128.2
}

type Conditions = TripConditions & { tab: 'list' | 'trip' }

/** 날씨기반 추천 결과에 영향을 주는 조건. 이 중 하나가 바뀌면 결과의 시작 장소(sp)를 지운다. */
const TRIP_KEYS: (keyof Conditions)[] = ['region', 'area', 'env', 'type', 'start', 'days']

interface Props {
  me: Me | null
  onLogin: () => void
}

/**
 * 지도 메인 화면. 카카오 지도 위에 왼쪽 패널(관광지 목록, 날씨기반 추천 탭)과 오른쪽 위 날씨 카드를 띄운다.
 * 목록과 날씨 카드는 항상 오늘 기준이다. 고른 조건(지역, 실내/실외, 종류)은 주소(?region=41&area=41820&env=INDOOR&type=14)에
 * 남겨 새로고침, 뒤로 가기, 공유 때 그대로 보인다.
 * 날씨기반 추천 탭(tab=trip)은 출발일(start), 일정(days), 결과의 시작 장소(sp)를 더 남기고,
 * 지역, 실내/실외, 종류는 관광지 목록 탭과 함께 쓴다.
 * /places/:id 에서는 같은 화면에서 패널에 장소 상세를 열고 지도에서 그 장소를 강조한다(조건은 주소에 그대로 남는다).
 */
export default function MainPage({ me, onLogin }: Props) {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const location = useLocation()
  const selectedId = readPlaceId(useParams().id)
  const region = readRegion(params.get('region'))
  const area = readArea(params.get('area'), region)
  const env = readEnv(params.get('env'))
  const type = readType(params.get('type'))
  const tab = readTab(params.get('tab'))

  const [dayList] = useState(() => dayOptions(new Date()))
  const today = dayList[0]
  const days = readDays(params.get('days'))
  const start = readStart(params.get('start'), days, dayList)
  const sp = readPlaceId(params.get('sp'))

  const [collapsed, setCollapsed] = useState(readCollapsed)
  const [areas, setAreas] = useState<Area[]>([])
  const [reloadKey, setReloadKey] = useState(0)
  const [markers, setMarkers] = useState<MarkerItem[]>([])
  const [route, setRoute] = useState<TripDay[] | null>(null)
  const [tripWeather, setTripWeather] = useState<TripWeatherInfo | null>(null)
  // 상세로 연 장소의 기본 정보(지역 조건 맞추기와 지도 강조에 쓴다)
  const [selectedPlace, setSelectedPlace] = useState<Place | null>(null)

  const [data, setData] = useState<Recommendation | null>(null)
  const [places, setPlaces] = useState<Place[]>([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState<string | null>(null)

  const date = today.date

  // 패널 접힘 상태를 이 브라우저에 기억한다
  useEffect(() => {
    try {
      window.localStorage.setItem(PANEL_KEY, collapsed ? '1' : '0')
    } catch {
      // 저장하지 못해도 이번 화면에서는 그대로 동작한다
    }
  }, [collapsed])

  /**
   * 조건을 바꾸면 주소에 쓴다. 기본값(서울, 선택 없음, 관광지 목록, 당일치기)은 주소에서 뺀다.
   * 추천 결과에 영향을 주는 조건이 바뀌면 결과의 시작 장소(sp)도 지워 조건 입력 화면으로 돌아간다.
   */
  const update = useCallback(
    (next: Partial<Conditions>, options?: { replace?: boolean }) => {
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
        if (next.tab !== undefined) put('tab', next.tab === 'list' ? null : next.tab)
        if (next.start !== undefined) put('start', next.start)
        if (next.days !== undefined) put('days', next.days === 1 ? null : next.days)
        if (next.sp !== undefined) put('sp', next.sp)
        else if (TRIP_KEYS.some((k) => next[k] !== undefined)) p.delete('sp')
        return p
      }, options)
    },
    [setParams],
  )

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

  // 주소에 목록에 없는 구·시군 코드가 들어 있으면 "전체"로 되돌린다.
  // 시도가 막 바뀐 순간에는 구·시군 목록이 아직 이전 시도의 것이므로, 목록이 지금 시도의 것일 때만 검사한다
  // (지역 코드는 시도 코드로 시작한다. 예: 41150)
  useEffect(() => {
    if (area === null || areas.length === 0 || !areas[0].code.startsWith(region)) return
    if (areas.some((a) => a.code === area)) return
    setParams(
      (prev) => {
        const p = new URLSearchParams(prev)
        p.delete('area')
        p.delete('sp')
        return p
      },
      { replace: true },
    )
  }, [area, areas, region, setParams])

  /*
   * 상세 주소를 바로 열었을 때(공유 링크, 즐겨찾기 등) 지역 조건이 장소와 다르면 장소에 맞춘다.
   * - 관광지 목록 탭에서만. 날씨기반 추천 탭은 결과의 주변 지역 장소를 눌러도 결과가 유지돼야 하므로 바꾸지 않는다
   * - 장소가 지금 조건 안이면 바꾸지 않는다(서울 전체를 보다가 종로구 장소를 열면 그대로 서울 전체)
   * - 실내/실외와 테마는 사용자가 고른 것이라 바꾸지 않는다. 그 때문에 마커가 없으면 지도가 그 장소만 따로 그린다
   * 주소 기록은 덮어써서 뒤로 가기가 꼬이지 않게 한다.
   */
  useEffect(() => {
    if (tab !== 'list' || selectedId === null || selectedPlace === null || selectedPlace.id !== selectedId) return
    const placeRegion = selectedPlace.ldongRegnCd
    if (placeRegion === null || !REGIONS.some((r) => r.code === placeRegion)) return
    const placeArea = selectedPlace.areaCode ?? null
    const outside = placeRegion !== region || (area !== null && placeArea !== area)
    if (outside) update({ region: placeRegion, area: placeArea }, { replace: true })
  }, [tab, selectedId, selectedPlace, region, area, update])

  // 지도에 따로 그릴 고른 장소(좌표가 서울·경기 범위 안일 때만)
  const selectedMarker = useMemo<MarkerItem | null>(() => {
    if (selectedPlace === null || selectedPlace.id !== selectedId) return null
    const { id, title, lat, lng, env: placeEnv } = selectedPlace
    return lat !== null && lng !== null && inServiceArea(lat, lng) ? { id, title, lat, lng, env: placeEnv } : null
  }, [selectedPlace, selectedId])

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

  // 지도 마커는 목록과 같은 조건으로 페이지 없이 받는다. 실패하면 마커만 비우고 목록은 그대로 쓴다.
  useEffect(() => {
    let cancelled = false
    fetchMarkers({ region, area, date, type, env })
      .then((list) => {
        if (!cancelled) setMarkers(list)
      })
      .catch(() => {
        if (!cancelled) setMarkers([])
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

  /** 목록 카드나 지도 마커를 고르면 /places/:id 로 간다. 조건(?...)은 그대로 둔다. */
  const openPlace = useCallback(
    (id: number) => navigate({ pathname: `/places/${id}`, search: location.search }),
    [navigate, location.search],
  )
  const openPanel = useCallback(() => setCollapsed(false), [setCollapsed])
  const backToList = () => navigate({ pathname: '/', search: location.search })
  // 날씨기반 추천 탭에서는 왼쪽 패널 옆에 날씨 패널이 붙는다(왼쪽 패널을 접으면 함께 숨긴다)
  const showTripWeather = tab === 'trip' && tripWeather !== null && !collapsed

  return (
    <div className="stage">
      <KakaoMap
        markers={markers}
        route={tab === 'trip' ? route : null}
        insetLeft={showTripWeather ? 760 : 440}
        selectedId={selectedId}
        selectedPlace={selectedMarker}
        panelCollapsed={collapsed}
        onSelect={openPlace}
        onOpenPanel={openPanel}
      />

      <SidePanel
        title={tab === 'trip' ? '날씨기반 추천' : '관광지 목록'}
        tabs={TABS}
        tab={tab}
        onTab={(key) => update({ tab: key === 'trip' ? 'trip' : 'list' })}
        collapsed={collapsed}
        onToggle={() => setCollapsed((c) => !c)}
      >
        {selectedId !== null && <PlaceDetail id={selectedId} onBack={backToList} onLoaded={setSelectedPlace} />}

        {/* 상세를 보는 동안에도 탭 내용은 숨겨 둔 채 유지한다(더 불러온 장소, 스크롤, 추천 결과가 그대로 남게) */}
        {tab === 'trip' ? (
          <div hidden={selectedId !== null}>
            <TripTab
              region={region}
              area={area}
              env={env}
              type={type}
              start={start}
              days={days}
              sp={sp}
              areas={areas}
              dayList={dayList}
              onUpdate={update}
              me={me}
              onLogin={onLogin}
              selectedId={selectedId}
              onSelect={openPlace}
              onRoute={setRoute}
              onWeather={setTripWeather}
            />
          </div>
        ) : (
          <div hidden={selectedId !== null}>
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
                  selectedId={selectedId}
                  onSelect={openPlace}
                />
              </div>
            )}
          </div>
        )}

        <footer className="footer">날씨는 기상청 단기예보, 장소 정보는 한국관광공사 TourAPI 자료예요.</footer>
      </SidePanel>

      {/* 오른쪽 위 날씨 카드는 관광지 목록 탭(오늘 날씨)에서만. 날씨기반 추천 탭은 옆 날씨 패널이 대신한다 */}
      {tab === 'list' && <WeatherCard day={today} data={error ? null : data} />}
      {showTripWeather && <TripWeatherPanel where={tripWeather.where} items={tripWeather.items} />}
    </div>
  )
}
