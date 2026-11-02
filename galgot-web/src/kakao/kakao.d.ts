// 카카오 지도 JavaScript SDK(https://apis.map.kakao.com/web/documentation/) 중 이 프로젝트가 쓰는 부분만 적은 최소 타입 선언.
// SDK는 loadKakaoMaps.ts가 스크립트로 불러오며, 불러온 뒤 window.kakao에 생긴다.

declare namespace kakao.maps {
  function load(callback: () => void): void

  class LatLng {
    constructor(lat: number, lng: number)
    getLat(): number
    getLng(): number
  }

  class LatLngBounds {
    constructor()
    extend(latlng: LatLng): void
    isEmpty(): boolean
  }

  class Size {
    constructor(width: number, height: number)
  }

  class Point {
    constructor(x: number, y: number)
  }

  class MarkerImage {
    constructor(src: string, size: Size, options?: { offset?: Point })
  }

  interface MapOptions {
    center: LatLng
    level?: number
  }

  class Map {
    constructor(container: HTMLElement, options: MapOptions)
    setBounds(bounds: LatLngBounds, paddingTop?: number, paddingRight?: number, paddingBottom?: number, paddingLeft?: number): void
    panTo(latlng: LatLng): void
    setLevel(level: number, options?: { animate?: boolean }): void
    getLevel(): number
    relayout(): void
  }

  interface MarkerOptions {
    position: LatLng
    image?: MarkerImage
    title?: string
    zIndex?: number
    clickable?: boolean
  }

  class Marker {
    constructor(options: MarkerOptions)
    setMap(map: Map | null): void
    setImage(image: MarkerImage): void
    setZIndex(zIndex: number): void
    getPosition(): LatLng
  }

  interface CustomOverlayOptions {
    position: LatLng
    content: HTMLElement | string
    xAnchor?: number
    yAnchor?: number
    zIndex?: number
    clickable?: boolean
  }

  class CustomOverlay {
    constructor(options: CustomOverlayOptions)
    setMap(map: Map | null): void
  }

  interface MarkerClustererOptions {
    map: Map
    averageCenter?: boolean
    minLevel?: number
    gridSize?: number
  }

  class MarkerClusterer {
    constructor(options: MarkerClustererOptions)
    addMarkers(markers: Marker[]): void
    clear(): void
  }

  namespace event {
    function addListener(target: object, type: string, handler: () => void): void
  }
}

interface Window {
  kakao?: typeof kakao
}
