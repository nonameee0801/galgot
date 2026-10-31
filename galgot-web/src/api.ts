// 백엔드(Spring Boot) 호출을 한곳에 모았다. 주소를 바꾸려면 .env 파일에 VITE_API_BASE=... 를 쓴다.
export const API_BASE: string = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

export type Env = 'INDOOR' | 'OUTDOOR' | 'BOTH'
export type Sky = 'CLEAR' | 'MOSTLY_CLOUDY' | 'OVERCAST'
export type PrecipType = 'NONE' | 'SHOWER' | 'RAIN' | 'RAIN_SNOW' | 'SNOW'

export interface Weather {
  region: string
  /** 날씨를 받은 지역 코드. 시도 전체면 region과 같다. */
  area: string
  /** 예: "서울 전체", "가평군" */
  areaName: string
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

/** 시도 안의 구·시군 */
export interface Area {
  code: string
  name: string
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

/** 요청마다 상태 코드별 안내 문구를 바꾸고 싶을 때 쓴다. 없으면 defaultMessage. */
type Messages = Partial<Record<number, string>>

async function request<T>(path: string, init?: RequestInit, messages?: Messages): Promise<T> {
  let res: Response
  try {
    // 로그인 세션 쿠키를 함께 보내야 해서 credentials를 켠다
    res = await fetch(`${API_BASE}${path}`, { credentials: 'include', ...init })
  } catch {
    throw new ApiError(0, defaultMessage(0))
  }
  if (!res.ok) {
    throw new ApiError(res.status, messages?.[res.status] ?? defaultMessage(res.status))
  }
  if (res.status === 204) {
    return undefined as T
  }
  return (await res.json()) as T
}

function postJson<T>(path: string, body: unknown, messages?: Messages): Promise<T> {
  return request<T>(
    path,
    { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) },
    messages,
  )
}

export interface RecommendationQuery {
  region: string
  /** null이면 시도 전체 */
  area: string | null
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
  if (q.area !== null) {
    params.set('area', q.area)
  }
  if (q.type !== null) {
    params.set('type', String(q.type))
  }
  return request<Recommendation>(`/api/recommendations?${params.toString()}`)
}

/** 시도 안의 구·시군 목록(가나다순). */
export function fetchAreas(region: string): Promise<Area[]> {
  return request<Area[]>(`/api/areas?region=${encodeURIComponent(region)}`)
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

/** 이메일 회원가입. 성공하면 바로 로그인 상태가 된다. */
export function signup(email: string, password: string, nickname: string): Promise<Me> {
  return postJson<Me>(
    '/api/auth/signup',
    { email, password, nickname: nickname.trim() === '' ? null : nickname.trim() },
    {
      400: '입력한 내용을 다시 확인해 주세요. 비밀번호는 영문과 숫자를 섞어 8~64자예요.',
      409: '이미 가입된 이메일이에요. 로그인해 주세요.',
    },
  )
}

/** 이메일 로그인. 실패 이유는 서버가 일부러 알려 주지 않는다. */
export function login(email: string, password: string): Promise<Me> {
  return postJson<Me>('/api/auth/login', { email, password }, {
    401: '이메일 또는 비밀번호가 맞지 않아요.',
  })
}

/** 회원 탈퇴. 코스를 포함해 모두 지워지고 세션도 끊긴다. */
export function withdraw(): Promise<void> {
  return request<void>('/api/users/me', { method: 'DELETE' })
}

/** 화면에서 미리 확인하는 비밀번호 규칙(서버 CredentialPolicy와 같다). */
export function isValidPassword(pw: string): boolean {
  return /^[\x21-\x7E]{8,64}$/.test(pw) && /[A-Za-z]/.test(pw) && /[0-9]/.test(pw)
}

/** 브라우저를 이 주소로 이동시키면 카카오 로그인이 시작된다. */
export const loginUrl = `${API_BASE}/oauth2/authorization/kakao`
