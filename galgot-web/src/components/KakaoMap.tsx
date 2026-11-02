import { useEffect, useRef, useState } from 'react'
import type { Env, MarkerItem, TripDay } from '../api'
import { hasKakaoKey, loadKakaoMaps } from '../kakao/loadKakaoMaps'
import { DAY_STYLES, ENV_LABEL } from '../labels'
import MapPlaceholder from './MapPlaceholder'

interface Props {
  markers: MarkerItem[]
  /** 날씨기반 추천 결과. 있으면 묶음 마커 대신 일차별 번호 마커와 방문 순서 선을 그린다 */
  route: TripDay[] | null
  /** 넓은 화면에서 지도 범위를 맞출 때 왼쪽에 비울 폭(패널이 펼쳐져 있을 때). 기본 440 */
  insetLeft?: number
  selectedId: number | null
  /**
   * 고른 장소(좌표가 서울·경기 범위 안일 때). 실내/실외나 테마 조건 때문에 마커 목록에 없으면
   * 이 정보로 그 장소 마커 하나를 따로 그려 강조한다(상세 주소를 바로 연 경우 등)
   */
  selectedPlace?: MarkerItem | null
  /** 패널이 접혀 있으면 마커를 골랐을 때 지도 위에 작은 정보창을 띄운다 */
  panelCollapsed: boolean
  onSelect: (id: number) => void
  /** 정보창의 "상세 보기": 패널을 펼친다 */
  onOpenPanel: () => void
}

type Status = 'loading' | 'ready' | 'error'

// 마커 모양: 실내는 채운 원, 실외는 빈 원, 실내외는 왼쪽 반만 채운 원 (목록의 점과 같은 규칙, 색이 아니라 모양으로도 구분)
const INK = '#1b2530'
const FILL = '#2f6fa5'
const imageCache = new Map<string, kakao.maps.MarkerImage>()

function markerImage(k: typeof kakao, env: Env, selected: boolean): kakao.maps.MarkerImage {
  const key = `${env}-${selected}`
  const cached = imageCache.get(key)
  if (cached) return cached

  const r = selected ? 10 : 6
  const sw = selected ? 3 : 2
  const s = (r + sw) * 2
  const c = s / 2
  const ring = `<circle cx="${c}" cy="${c}" r="${r}" fill="none" stroke="${INK}" stroke-width="${sw}"/>`
  const body =
    env === 'INDOOR'
      ? `<circle cx="${c}" cy="${c}" r="${r}" fill="${FILL}"/>`
      : env === 'OUTDOOR'
        ? `<circle cx="${c}" cy="${c}" r="${r}" fill="#fff"/>`
        : `<circle cx="${c}" cy="${c}" r="${r}" fill="#fff"/><path d="M ${c} ${c - r} A ${r} ${r} 0 0 0 ${c} ${c + r} Z" fill="${FILL}"/>`
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${s}" height="${s}" viewBox="0 0 ${s} ${s}">${body}${ring}</svg>`
  const image = new k.maps.MarkerImage('data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg), new k.maps.Size(s, s), {
    offset: new k.maps.Point(c, c),
  })
  imageCache.set(key, image)
  return image
}

/** 일차별 코스의 번호 마커: 일차 색 원 안에 그날 방문 순서. 고르면 크게 그린다. */
function routeImage(k: typeof kakao, dayIndex: number, order: number, selected: boolean): kakao.maps.MarkerImage {
  const color = DAY_STYLES[dayIndex % DAY_STYLES.length].color
  const key = `route-${dayIndex}-${order}-${selected}`
  const cached = imageCache.get(key)
  if (cached) return cached

  const r = selected ? 15 : 11
  const s = (r + 3) * 2
  const c = s / 2
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="${s}" height="${s}" viewBox="0 0 ${s} ${s}">` +
    `<circle cx="${c}" cy="${c}" r="${r}" fill="${color}" stroke="#fff" stroke-width="${selected ? 3 : 2}"/>` +
    `<text x="${c}" y="${c}" dy="0.35em" text-anchor="middle" font-family="sans-serif" font-weight="700" ` +
    `font-size="${selected ? 15 : 12}" fill="#fff">${order}</text></svg>`
  const image = new k.maps.MarkerImage('data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg), new k.maps.Size(s, s), {
    offset: new k.maps.Point(c, c),
  })
  imageCache.set(key, image)
  return image
}

type Drawn = { marker: kakao.maps.Marker; item: MarkerItem; image: (selected: boolean) => kakao.maps.MarkerImage }

/** 정보창 내용. 장소 이름은 외부 데이터라 innerHTML이 아니라 textContent로 넣는다. */
function infoElement(item: MarkerItem, onDetail: () => void): HTMLElement {
  const box = document.createElement('div')
  box.className = 'map-info'
  const title = document.createElement('strong')
  title.textContent = item.title
  const env = document.createElement('span')
  env.textContent = ENV_LABEL[item.env]
  const button = document.createElement('button')
  button.type = 'button'
  button.textContent = '상세 보기'
  button.addEventListener('click', onDetail)
  box.append(title, env, button)
  return box
}

/**
 * 카카오 지도. 마커는 묶음 표시(클러스터)로 그리고, 마커가 바뀌면 모두 보이도록 범위를 맞춘다.
 * 고른 장소는 크게 강조하고 그 위치로 이동한다. 키가 없거나 불러오지 못하면 안내 문구로 대신한다.
 * 날씨기반 추천 결과(route)가 있으면 그 코스만 일차별 번호 마커로 그리고, 일차마다 방문 순서대로 선을 잇는다
 * (실제 도로가 아니라 직선이다).
 */
export default function KakaoMap({
  markers,
  route,
  insetLeft = 440,
  selectedId,
  selectedPlace = null,
  panelCollapsed,
  onSelect,
  onOpenPanel,
}: Props) {
  const box = useRef<HTMLDivElement>(null)
  const mapRef = useRef<kakao.maps.Map | null>(null)
  const clustererRef = useRef<kakao.maps.MarkerClusterer | null>(null)
  const overlayRef = useRef<kakao.maps.CustomOverlay | null>(null)
  // 마커 목록에 없는 고른 장소를 따로 그린 마커
  const extraRef = useRef<kakao.maps.Marker | null>(null)
  const drawn = useRef(new Map<number, Drawn>())
  // 코스 표시용 마커와 선(묶음 표시 없이 지도에 바로 올린다)
  const routeShapes = useRef<{ setMap: (map: kakao.maps.Map | null) => void }[]>([])
  const prevSelected = useRef<number | null>(null)
  const handlers = useRef({ onSelect, onOpenPanel })
  const [status, setStatus] = useState<Status>(hasKakaoKey() ? 'loading' : 'error')

  // 지도 SDK의 이벤트에서 항상 최신 함수를 부르도록 맞춰 둔다
  useEffect(() => {
    handlers.current = { onSelect, onOpenPanel }
  }, [onSelect, onOpenPanel])

  // 1) 지도 만들기 (개발 모드에서 두 번 붙었다 떨어져도 지도는 한 번만 만든다)
  useEffect(() => {
    let cancelled = false
    const container = box.current
    const drawnMarkers = drawn.current
    let observer: ResizeObserver | null = null
    loadKakaoMaps()
      .then((k) => {
        if (cancelled || !container) return
        const map = new k.maps.Map(container, { center: new k.maps.LatLng(37.5665, 126.978), level: 8 })
        mapRef.current = map
        clustererRef.current = new k.maps.MarkerClusterer({ map, averageCenter: true, minLevel: 6, gridSize: 60 })
        observer = new ResizeObserver(() => map.relayout())
        observer.observe(container)
        setStatus('ready')
      })
      .catch(() => {
        if (!cancelled) setStatus('error')
      })
    return () => {
      cancelled = true
      observer?.disconnect()
      overlayRef.current?.setMap(null)
      extraRef.current?.setMap(null)
      clustererRef.current?.clear()
      routeShapes.current.forEach((s) => s.setMap(null))
      routeShapes.current = []
      mapRef.current = null
      clustererRef.current = null
      drawnMarkers.clear()
      if (container) container.innerHTML = ''
    }
  }, [])

  // 2) 마커 그리기와 범위 맞추기 (패널과 날씨 카드에 가리지 않게 여백을 둔다)
  useEffect(() => {
    const k = window.kakao
    const map = mapRef.current
    const clusterer = clustererRef.current
    if (status !== 'ready' || !k || !map || !clusterer) return

    clusterer.clear()
    routeShapes.current.forEach((s) => s.setMap(null))
    routeShapes.current = []
    overlayRef.current?.setMap(null)
    drawn.current.clear()
    prevSelected.current = null

    const bounds = new k.maps.LatLngBounds()
    if (route) {
      route.forEach((day, dayIndex) => {
        const style = DAY_STYLES[dayIndex % DAY_STYLES.length]
        const path: kakao.maps.LatLng[] = []
        day.places.forEach((stop) => {
          const { place } = stop
          if (place.lat === null || place.lng === null) return
          const item: MarkerItem = { id: place.id, title: place.title, lat: place.lat, lng: place.lng, env: place.env }
          const position = new k.maps.LatLng(item.lat, item.lng)
          const image = (selected: boolean) => routeImage(k, dayIndex, stop.order, selected)
          const marker = new k.maps.Marker({
            position,
            image: image(false),
            title: `${day.dayNo}일차 ${stop.order}번째 · ${place.title}`,
          })
          marker.setMap(map)
          k.maps.event.addListener(marker, 'click', () => handlers.current.onSelect(item.id))
          drawn.current.set(item.id, { marker, item, image })
          routeShapes.current.push(marker)
          path.push(position)
          bounds.extend(position)
        })
        if (path.length > 1) {
          const line = new k.maps.Polyline({
            map,
            path,
            strokeWeight: 4,
            strokeColor: style.color,
            strokeOpacity: 0.85,
            strokeStyle: style.line,
          })
          routeShapes.current.push(line)
        }
      })
    } else {
      const list = markers.map((item) => {
        const position = new k.maps.LatLng(item.lat, item.lng)
        const image = (selected: boolean) => markerImage(k, item.env, selected)
        const marker = new k.maps.Marker({ position, image: image(false), title: item.title })
        k.maps.event.addListener(marker, 'click', () => handlers.current.onSelect(item.id))
        drawn.current.set(item.id, { marker, item, image })
        bounds.extend(position)
        return marker
      })
      clusterer.addMarkers(list)
    }

    if (!bounds.isEmpty() && box.current) {
      const narrow = box.current.clientWidth < 768
      // 좁은 화면에서는 위쪽에 날씨 카드나 날씨 패널이 뜬다
      const top = narrow ? 160 : 60
      const bottom = narrow && !panelCollapsed ? Math.round(box.current.clientHeight * 0.6) + 20 : 40
      const left = !narrow && !panelCollapsed ? insetLeft : 40
      map.setBounds(bounds, top, 40, bottom, left)
    }
    // 범위는 마커가 바뀔 때만 다시 맞춘다(패널을 접고 펼 때마다 지도가 움직이지 않게)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [markers, route, status])

  // 3) 고른 장소 강조, 이동, 패널이 접혀 있으면 정보창
  useEffect(() => {
    const k = window.kakao
    const map = mapRef.current
    if (status !== 'ready' || !k || !map) return

    const prev = prevSelected.current !== null ? drawn.current.get(prevSelected.current) : undefined
    if (prev) {
      prev.marker.setImage(prev.image(false))
      prev.marker.setZIndex(0)
    }
    overlayRef.current?.setMap(null)
    overlayRef.current = null
    extraRef.current?.setMap(null)
    extraRef.current = null
    prevSelected.current = selectedId

    let current = selectedId !== null ? drawn.current.get(selectedId) : undefined
    // 조건(실내/실외, 테마) 때문에 마커 목록에 없으면 그 장소 마커만 따로 그린다
    if (!current && selectedPlace !== null && selectedPlace.id === selectedId) {
      const place = selectedPlace
      const image = (selected: boolean) => markerImage(k, place.env, selected)
      const marker = new k.maps.Marker({
        position: new k.maps.LatLng(place.lat, place.lng),
        image: image(true),
        title: place.title,
      })
      marker.setMap(map)
      extraRef.current = marker
      current = { marker, item: place, image }
    }
    if (!current) return
    current.marker.setImage(current.image(true))
    current.marker.setZIndex(10)
    const position = current.marker.getPosition()
    // 묶음이 풀리는 확대 수준(5)까지 들어가서 그 장소로 이동한다
    if (map.getLevel() > 5) map.setLevel(5)
    map.panTo(position)

    if (panelCollapsed) {
      const overlay = new k.maps.CustomOverlay({
        position,
        content: infoElement(current.item, () => handlers.current.onOpenPanel()),
        yAnchor: 1.35,
        zIndex: 20,
        clickable: true,
      })
      overlay.setMap(map)
      overlayRef.current = overlay
    }
  }, [selectedId, selectedPlace, markers, route, status, panelCollapsed])

  return (
    <>
      <div ref={box} className="kmap" aria-label="관광지 지도" hidden={status === 'error'} />
      {status === 'loading' && <MapPlaceholder />}
      {status === 'error' && (
        <MapPlaceholder
          message={
            hasKakaoKey()
              ? '지도를 불러오지 못했어요. 목록은 그대로 볼 수 있어요.'
              : '지도 키(VITE_KAKAO_MAP_KEY)가 설정되지 않아 지도를 보여 줄 수 없어요. 목록은 그대로 볼 수 있어요.'
          }
        />
      )}
    </>
  )
}
