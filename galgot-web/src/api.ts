// 백엔드(Spring Boot) 호출을 한곳에 모았다. 주소를 바꾸려면 .env 파일에 VITE_API_BASE=... 를 쓴다.
export const API_BASE: string = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

export type Env = 'INDOOR' | 'OUTDOOR' | 'BOTH'
export type Sky = 'CLEAR' | 'MOSTLY_CLOUDY' | 'OVERCAST'
export type PrecipType = 'NONE' | 'SHOWER' | 'RAIN' | 'RAIN_SNOW' | 'SNOW'

export interface Weather {
  region: string
  date: string
  sky: Sky | null
  precipType: PrecipType | null
  pop: number | null
  tempMax: number | null
  tempMin: number | null
  fetchedAt: string
}

export interface Place {
  id: number
  contentId: string
  contentTypeId: number
  title: string
  addr: string | null
  lat: number | null
  lng: number | null
  imageUrl: string | null
  thumbnailUrl: string | null
  ldongRegnCd: string | null
  ldongSignguCd: string | null
  env: Env
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Recommendation {
  weather: Weather
  outdoorSuitable: boolean
  reasons: string[]
  message: string
  places: PageResponse<Place>
}

export interface Me {
  id: number
  nickname: string
}

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

function defaultMessage(status: number): string {
  switch (status) {
    case 0:
      return '서버에 연결하지 못했어요. 백엔드가 실행 중인지 확인해 주세요.'
    case 400:
      return '이 조건으로는 조회할 수 없어요. 지역이나 날짜를 바꿔 보세요.'
    case 401:
      return '로그인이 필요해요.'
    case 503:
      return '날씨 정보를 가져오지 못했어요. 잠시 뒤에 다시 시도해 주세요.'
    default:
      return '요청을 처리하지 못했어요. 잠시 뒤에 다시 시도해 주세요.'
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response
  try {
    // 로그인 세션 쿠키를 함께 보내야 해서 credentials를 켠다
    res = await fetch(`${API_BASE}${path}`, { credentials: 'include', ...init })
  } catch {
    throw new ApiError(0, defaultMessage(0))
  }
  if (!res.ok) {
    throw new ApiError(res.status, defaultMessage(res.status))
  }
  if (res.status === 204) {
    return undefined as T
  }
  return (await res.json()) as T
}

export interface RecommendationQuery {
  region: string
  date: string
  type: number | null
  page: number
  size: number
}

export function fetchRecommendations(q: RecommendationQuery): Promise<Recommendation> {
  const params = new URLSearchParams({
    region: q.region,
    date: q.date,
    page: String(q.page),
    size: String(q.size),
  })
  if (q.type !== null) {
    params.set('type', String(q.type))
  }
  return request<Recommendation>(`/api/recommendations?${params.toString()}`)
}

/** 로그인하지 않았으면(401) null. */
export async function fetchMe(): Promise<Me | null> {
  try {
    return await request<Me>('/api/users/me')
  } catch {
    return null
  }
}

export function logout(): Promise<void> {
  return request<void>('/api/auth/logout', { method: 'POST' })
}

/** 브라우저를 이 주소로 이동시키면 카카오 로그인이 시작된다. */
export const loginUrl = `${API_BASE}/oauth2/authorization/kakao`
