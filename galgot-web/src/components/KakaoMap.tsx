import { useEffect, useRef, useState } from 'react'
import type { Env, MarkerItem, Place } from '../api'
import { hasKakaoKey, loadKakaoMaps } from '../kakao/loadKakaoMaps'
import { DAY_STYLES, ENV_LABEL } from '../labels'
import MapPlaceholder from './MapPlaceholder'

/**
 * 지도에 일차별 번호 마커와 방문 순서 선으로 그릴 코스. 날씨기반 추천 결과(TripDay)와 저장한 코스(지도 팝업)가 함께 쓴다.
 */
export interface RouteDay {
  dayNo: number
  places: { order: number; place: Place }[]
}

interface Props {
  markers: MarkerItem[]
  /** 날씨기반 추천 결과나 저장한 코스. 있으면 묶음 마커 대신 일차별 번호 마커와 방문 순서 선을 그린다 */
  route: RouteDay[] | null
  /** 넓은 화면에서 지도 범위를 맞출 때 왼쪽에 비울 폭(패널이 펼쳐져 있을 때). 기본 440 */
  insetLeft?: number
  selectedId: number | null
  /**
   * 고른 장소(좌표가 서울·경기 범위 안일 때). 실내/실외나 테마 조건 때문에 마커 목록에 없으면
   * 이 정보로 그 장소 마커 하나를 따로 그려 강조한다(상세 주소를 바로 연 경우 등)
   */
  selectedPlace?: MarkerItem | null
  /**
   * 장소 이름 검색 결과의 첫 장소. 있으면 마커 전체가 보이게 맞추는 대신 이 장소 주변으로 옮긴다
   * (검색 결과가 서울과 경기 여러 곳에 흩어져도 찾던 장소를 바로 보이게, 기획서 18장 B)
   */
  focus?: { lat: number; lng: number } | null
  /** 패널이 접혀 있으면 마커를 골랐을 때 지도 위에 작은 정보창을 띄운다 */
  panelCollapsed: boolean
  onSelect: (id: number) => void
  /** 정보창의 "상세 보기": 패널을 펼친다 */
  onOpenPanel: () => void
}

type Status = 'loading' | 'ready' | 'error'

/*
 * 마커(기획서 19장): 매끈한 물방울 핀. 둥근 머리 안에 실내는 건물, 실외는 나무 아이콘.
 * 실내는 청록(위가 살짝 밝은 그라데이션)에 흰 테두리, 실외는 흰 바탕에 초록 테두리, 실내외는 왼쪽 반을 주황으로
 * (목록의 점과 같은 규칙, 색이 아니라 채움 모양으로도 구분). 고르면 핀이 커지고 옆에 흰 이름표가 뜬다.
 * 색은 index.css의 --accent(실내), --outdoor(실외), --rec(실내외)와 같다.
 */
const ENV_COLOR: Record<Env, string> = { INDOOR: '#0e6f73', OUTDOOR: '#4e8a2f', BOTH: '#f4a340' }
const INK = '#16242b'
// lucide 아이콘(Building2, Trees)의 선. 24칸 기준
const BUILDING =
  '<path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z"/><path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2"/>' +
  '<path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2"/><path d="M10 6h4M10 10h4M10 14h4M10 18h4"/>'
const TREES =
  '<path d="M10 10v.2A3 3 0 0 1 8.9 16H5a3 3 0 0 1-1-5.8V10a3 3 0 0 1 6 0Z"/><path d="M7 16v6M13 19v3"/>' +
  '<path d="M12 19h8.3a1 1 0 0 0 .7-1.7L18 14h.3a1 1 0 0 0 .7-1.7L16 9h.2a1 1 0 0 0 .8-1.7L13 3l-1.4 1.5"/>'
const DEFS =
  '<defs><filter id="s" x="-40%" y="-40%" width="180%" height="180%">' +
  '<feDropShadow dx="0" dy="1.5" stdDeviation="1.6" flood-color="#0a2832" flood-opacity="0.3"/></filter>' +
  '<linearGradient id="t" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#169198"/>' +
  '<stop offset="1" stop-color="#0b5f63"/></linearGradient></defs>'
const imageCache = new Map<string, kakao.maps.MarkerImage>()

// 묶음 표시 안의 작은 청록 핀
const CLUSTER_PIN =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 30 36"><path d="M15 34 C12 29 2 24 2 15 A13 13 0 1 1 28 15 ' +
      'C28 24 18 29 15 34Z" fill="#0e6f73"/><circle cx="15" cy="15" r="5" fill="#fff"/></svg>',
  )

/** 핀 크기: 머리 반지름. 고르면 1.35배 */
const HEAD = 13
const SCALE_SELECTED = 1.35

/** 핀 그림의 자리: 폭, 높이, 꼬리 끝, 머리 가운데 */
function pinBox(selected: boolean) {
  const s = selected ? SCALE_SELECTED : 1
  const r = HEAD * s
  const w = Math.ceil(r * 2 + 14)
  const cx = w / 2
  const headY = 5 + r // 위 여백 5
  const tipY = headY + r + 8 * s
  const h = Math.ceil(tipY + 5)
  return { s, r, w, h, cx, headY, tipY }
}

/** 물방울 모양(둥근 머리 + 짧은 꼬리). 꼬리 끝이 (cx, tipY) */
function dropPath(cx: number, headY: number, r: number, tipY: number, s: number): string {
  return (
    `M${cx} ${tipY} C${cx - 3 * s} ${tipY - 5 * s} ${cx - r} ${headY + r * 0.55} ${cx - r} ${headY} ` +
    `A${r} ${r} 0 1 1 ${cx + r} ${headY} C${cx + r} ${headY + r * 0.55} ${cx + 3 * s} ${tipY - 5 * s} ${cx} ${tipY}Z`
  )
}

function iconSvg(env: Env, color: string, x: number, y: number, size: number): string {
  return (
    `<g transform="translate(${x - size / 2} ${y - size / 2}) scale(${size / 24})" fill="none" stroke="${color}" ` +
    `stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">${env === 'OUTDOOR' ? TREES : BUILDING}</g>`
  )
}

function toImage(k: typeof kakao, svg: string, w: number, h: number, tipX: number, tipY: number): kakao.maps.MarkerImage {
  return new k.maps.MarkerImage('data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg), new k.maps.Size(w, h), {
    offset: new k.maps.Point(tipX, tipY),
  })
}

function markerImage(k: typeof kakao, env: Env, selected: boolean): kakao.maps.MarkerImage {
  const key = `${env}-${selected}`
  const cached = imageCache.get(key)
  if (cached) return cached

  const color = ENV_COLOR[env]
  const { s, r, w, h, cx, headY, tipY } = pinBox(selected)
  const path = dropPath(cx, headY, r, tipY, s)
  const fill = env === 'INDOOR' ? 'url(#t)' : '#fff'
  const stroke = env === 'INDOOR' ? '#fff' : color
  // 실내외: 왼쪽 반을 주황으로 채운다(테두리 안쪽만)
  const half =
    env === 'BOTH'
      ? `<clipPath id="c"><path d="${path}"/></clipPath><rect x="0" y="0" width="${cx}" height="${h}" fill="${color}" clip-path="url(#c)"/>`
      : ''
  const icon = iconSvg(env, env === 'INDOOR' ? '#fff' : env === 'OUTDOOR' ? color : INK, cx, headY, 14 * s)
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">${DEFS}` +
    `<ellipse cx="${cx}" cy="${tipY + 1}" rx="${5 * s}" ry="${1.8 * s}" fill="rgba(10,40,50,0.25)"/>` +
    `<g filter="url(#s)"><path d="${path}" fill="${fill}"/></g>${half}` +
    `<path d="${path}" fill="none" stroke="${stroke}" stroke-width="${2.2 * s}"/>${icon}</svg>`
  const image = toImage(k, svg, w, h, cx, tipY)
  imageCache.set(key, image)
  return image
}

/** 일차별 코스의 번호 핀: 일차 색 물방울 안에 그날 방문 순서. 고르면 크게 그린다. */
function routeImage(k: typeof kakao, dayIndex: number, order: number, selected: boolean): kakao.maps.MarkerImage {
  const color = DAY_STYLES[dayIndex % DAY_STYLES.length].color
  const key = `route-${dayIndex}-${order}-${selected}`
  const cached = imageCache.get(key)
  if (cached) return cached

  const { s, r, w, h, cx, headY, tipY } = pinBox(selected)
  const path = dropPath(cx, headY, r, tipY, s)
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">${DEFS}` +
    `<ellipse cx="${cx}" cy="${tipY + 1}" rx="${5 * s}" ry="${1.8 * s}" fill="rgba(10,40,50,0.25)"/>` +
    `<g filter="url(#s)"><path d="${path}" fill="${color}" stroke="#fff" stroke-width="${2.2 * s}"/></g>` +
    `<text x="${cx}" y="${headY}" dy="0.36em" text-anchor="middle" font-family="sans-serif" font-weight="800" ` +
    `font-size="${Math.round(13 * s)}" fill="#fff">${order}</text></svg>`
  const image = toImage(k, svg, w, h, cx, tipY)
  imageCache.set(key, image)
  return image
}

/**
 * 고른 장소의 흰 이름표. 커진 핀 머리 오른쪽에 붙는다.
 * 장소 이름은 외부 데이터라 innerHTML이 아니라 textContent로 넣는다.
 */
function nameLabel(item: MarkerItem): HTMLElement {
  const { r, headY, tipY } = pinBox(true)
  const box = document.createElement('div')
  box.className = 'map-pin-label'
  // 위치 기준점은 꼬리 끝. 머리 가운데 높이로 올리고 머리 오른쪽으로 비킨다
  box.style.transform = `translate(${Math.round(r + 8)}px, ${-Math.round(tipY - headY)}px)`
  box.textContent = item.title
  return box
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
  focus = null,
  panelCollapsed,
  onSelect,
  onOpenPanel,
}: Props) {
  const box = useRef<HTMLDivElement>(null)
  const mapRef = useRef<kakao.maps.Map | null>(null)
  const clustererRef = useRef<kakao.maps.MarkerClusterer | null>(null)
  const overlayRef = useRef<kakao.maps.CustomOverlay | null>(null)
  // 고른 장소의 이름표
  const labelRef = useRef<kakao.maps.CustomOverlay | null>(null)
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
        clustererRef.current = new k.maps.MarkerClusterer({
          map,
          averageCenter: true,
          minLevel: 6,
          gridSize: 60,
          // 묶음 표시(기획서 19장): 흰 알약 안에 청록 핀 아이콘과 "24곳". 고른 장소의 흰 이름표와 같은 모양.
          // 10곳 이상, 50곳 이상이면 글자를 조금씩 크게
          calculator: [10, 50],
          texts: (count) => `${count}곳`,
          styles: [13, 14, 15].map((font) => ({
            width: 'auto',
            height: `${font + 17}px`,
            lineHeight: `${font + 17}px`,
            padding: `0 12px 0 ${font + 17}px`,
            borderRadius: '999px',
            background: `#fff url("${CLUSTER_PIN}") no-repeat 10px center / ${font}px ${font + 3}px`,
            boxShadow: '0 3px 12px rgba(10, 40, 50, 0.25)',
            color: '#0e6f73',
            fontFamily: 'inherit',
            fontWeight: '800',
            fontSize: `${font}px`,
            whiteSpace: 'nowrap',
            textAlign: 'center',
          })),
        })
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
      labelRef.current?.setMap(null)
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
    labelRef.current?.setMap(null)
    labelRef.current = null
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

    // 검색 중이면 첫 결과 주변(위아래, 좌우 약 1km)으로 범위를 좁혀, 패널에 가리지 않는 자리의 가운데에 오게 한다
    const target = focus !== null && route === null ? new k.maps.LatLngBounds() : bounds
    if (focus !== null && route === null) {
      target.extend(new k.maps.LatLng(focus.lat - 0.01, focus.lng - 0.012))
      target.extend(new k.maps.LatLng(focus.lat + 0.01, focus.lng + 0.012))
    }
    if (!target.isEmpty() && box.current) {
      const narrow = box.current.clientWidth < 768
      // 좁은 화면에서는 위쪽에 날씨 카드나 날씨 패널이 뜬다
      const top = narrow ? 160 : 60
      const bottom = narrow && !panelCollapsed ? Math.round(box.current.clientHeight * 0.6) + 20 : 40
      const left = !narrow && !panelCollapsed ? insetLeft : 40
      map.setBounds(target, top, 40, bottom, left)
    }
    // 범위는 마커나 검색 첫 장소가 바뀔 때만 다시 맞춘다(패널을 접고 펼 때마다 지도가 움직이지 않게)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [markers, route, status, focus])

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
    labelRef.current?.setMap(null)
    labelRef.current = null
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
      // 패널이 접혀 있으면 이름표 대신 상세 보기 버튼이 있는 정보창을 핀 위에 띄운다
      const overlay = new k.maps.CustomOverlay({
        position,
        content: infoElement(current.item, () => handlers.current.onOpenPanel()),
        yAnchor: 1.75,
        zIndex: 20,
        clickable: true,
      })
      overlay.setMap(map)
      overlayRef.current = overlay
    } else {
      const label = new k.maps.CustomOverlay({ position, content: nameLabel(current.item), xAnchor: 0, yAnchor: 0.5, zIndex: 15 })
      label.setMap(map)
      labelRef.current = label
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
