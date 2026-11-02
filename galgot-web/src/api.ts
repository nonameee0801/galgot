// 백엔드(Spring Boot) 호출을 한곳에 모았다. 주소를 바꾸려면 .env 파일에 VITE_API_BASE=... 를 쓴다.
export const API_BASE: string = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

export type Env = 'INDOOR' | 'OUTDOOR' | 'BOTH'
/** 화면의 [실내] [실외] [둘 다] 선택. 고르지 않으면(null) 날씨에 맞춰 보여 준다. */
export type EnvChoice = 'INDOOR' | 'OUTDOOR' | 'ALL'
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
  /** 장소가 속한 구·시군 지역 코드. 상세(GET /api/places/{id})에서만 오고, 목록에서는 null */
  areaCode?: string | null
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** 지도 마커용 가벼운 장소 정보 */
export interface MarkerItem {
  id: number
  title: string
  lat: number
  lng: number
  env: Env
}

export interface Recommendation {
  weather: Weather
  outdoorSuitable: boolean
  /** 그날 판정에 따른 추천: 적합하면 OUTDOOR, 부적합하면 INDOOR */
  recommendedEnv: Env
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
  /** null이면 날씨에 맞춰(부적합한 날 실외 제외) */
  env: EnvChoice | null
  /** 장소 이름 검색어. 있으면 서울·경기 전체에서 이름으로만 찾는다(지역, 실내/실외, 테마로 거르지 않음) */
  keyword?: string | null
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
  if (q.env !== null) {
    params.set('env', q.env)
  }
  if (q.keyword) {
    params.set('keyword', q.keyword)
  }
  return request<Recommendation>(`/api/recommendations?${params.toString()}`)
}

/** 지도 마커. 목록과 같은 조건(지역, 오늘 날씨, 실내/실외, 종류, 검색어)의 장소를 페이지 없이 받는다. */
export function fetchMarkers(q: Omit<RecommendationQuery, 'page' | 'size'>): Promise<MarkerItem[]> {
  const params = new URLSearchParams({ region: q.region, date: q.date })
  if (q.area !== null) params.set('area', q.area)
  if (q.type !== null) params.set('type', String(q.type))
  if (q.env !== null) params.set('env', q.env)
  if (q.keyword) params.set('keyword', q.keyword)
  return request<MarkerItem[]>(`/api/recommendations/markers?${params.toString()}`)
}

/** 장소 상세 */
export function fetchPlace(id: number): Promise<Place> {
  return request<Place>(`/api/places/${id}`, undefined, { 404: '장소를 찾을 수 없어요.' })
}

/** 장소 상세 정보(TourAPI 상세 API를 정리해 저장한 값). 값이 없는 항목은 null. */
export interface PlaceInfo {
  placeId: number
  overview: string | null
  useTime: string | null
  restDate: string | null
  useFee: string | null
  parking: string | null
  parkingFee: string | null
  contact: string | null
  /** http, https 주소만 */
  homepages: string[]
  openPeriod: string | null
  reservation: string | null
  fetchedAt: string
}

/** 장소 상세 정보. 처음 여는 장소는 서버가 TourAPI에서 받아 오느라 조금 걸린다. */
export function fetchPlaceInfo(id: number): Promise<PlaceInfo> {
  return request<PlaceInfo>(`/api/places/${id}/detail`, undefined, {
    404: '장소를 찾을 수 없어요.',
    503: '자세한 정보를 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.',
  })
}

/** 일차별 코스의 한 장소 */
export interface TripStop {
  /** 그날 방문 순서(1부터) */
  order: number
  /** 고른 지역이 아니라 주변 지역에서 채운 장소 */
  nearby: boolean
  place: Place
}

export interface TripDay {
  dayNo: number
  date: string
  weather: Weather
  outdoorSuitable: boolean
  /** 그날 판정에 따른 추천: 적합하면 OUTDOOR, 부적합하면 INDOOR */
  recommendedEnv: Env
  reasons: string[]
  message: string
  places: TripStop[]
  /** 장소가 모자라거나 주변 지역에서 채웠을 때의 안내 */
  notice: string | null
}

/** 날씨기반 일차별 코스 추천 결과 */
export interface Trip {
  region: string
  area: string
  areaName: string
  start: string
  dayCount: number
  env: EnvChoice | null
  /** 1일차 첫 장소. 주소에 남겨 같은 코스를 다시 만든다. 장소가 하나도 없으면 null */
  startPlaceId: number | null
  /** false면 다른 시작 장소가 없어 "다른 추천 받기"를 숨긴다 */
  rerollAvailable: boolean
  days: TripDay[]
}

export interface TripQuery {
  region: string
  /** 서울은 null(서울 전체), 경기는 시·군 코드 */
  area: string | null
  start: string
  days: number
  /** null이면 일차마다 그날 추천대로 */
  env: EnvChoice | null
  type: number | null
  startPlaceId: number | null
  excludeStartPlaceId: number | null
}

/** 날씨기반 일차별 코스. 경기는 시·군을 꼭 넣고, 서울은 area 없이 부른다. */
export function fetchTrip(q: TripQuery): Promise<Trip> {
  const params = new URLSearchParams({ region: q.region, start: q.start, days: String(q.days) })
  if (q.area !== null) params.set('area', q.area)
  if (q.env !== null) params.set('env', q.env)
  if (q.type !== null) params.set('type', String(q.type))
  if (q.startPlaceId !== null) params.set('startPlaceId', String(q.startPlaceId))
  if (q.excludeStartPlaceId !== null) params.set('excludeStartPlaceId', String(q.excludeStartPlaceId))
  return request<Trip>(`/api/recommendations/trip?${params.toString()}`, undefined, {
    400: '이 조건으로는 코스를 만들 수 없어요. 출발일이나 지역을 바꿔 보세요.',
  })
}

/** 만든 코스(필요한 값만) */
export interface CourseCreated {
  id: number
  title: string
}

/** 내 코스 만들기. 처음에는 비공개로 만든다. */
export function createCourse(title: string, ldongRegnCd: string, travelDate: string): Promise<CourseCreated> {
  return postJson<CourseCreated>('/api/courses', { title, ldongRegnCd, travelDate, publicCourse: false })
}

/** 코스에 장소 담기(담는 순서가 그날 방문 순서가 된다). */
export function addCoursePlace(courseId: number, placeId: number, dayNo: number): Promise<void> {
  return postJson<void>(`/api/courses/${courseId}/places`, { placeId, dayNo })
}

export function deleteCourse(courseId: number): Promise<void> {
  return request<void>(`/api/courses/${courseId}`, { method: 'DELETE' })
}

/** 마이페이지의 코스 카드(담긴 장소는 없고 개수만) */
export interface CourseSummary {
  id: number
  title: string
  ldongRegnCd: string | null
  /** 여행 날짜(1일차). 없을 수 있다 */
  travelDate: string | null
  publicCourse: boolean
  shareCode: string
  createdAt: string
  placeCount: number
  dayCount: number
}

/** 코스에 담긴 장소 한 건 */
export interface CoursePlaceItem {
  /** 코스 안 항목 id(장소 id와 다르다) */
  id: number
  dayNo: number
  visitOrder: number
  memo: string | null
  place: Place
}

/** 코스 상세. places는 일차, 방문 순서대로 온다 */
export interface CourseDetail {
  id: number
  title: string
  ldongRegnCd: string | null
  travelDate: string | null
  publicCourse: boolean
  shareCode: string
  createdAt: string
  places: CoursePlaceItem[]
}

const COURSE_MESSAGES: Messages = {
  403: '내 코스가 아니에요.',
  404: '코스를 찾을 수 없어요.',
}

/** 내 코스 목록(최근에 만든 순). 로그인하지 않았으면 401 */
export function fetchCourses(): Promise<CourseSummary[]> {
  return request<CourseSummary[]>('/api/courses')
}

export function fetchCourse(courseId: number): Promise<CourseDetail> {
  return request<CourseDetail>(`/api/courses/${courseId}`, undefined, COURSE_MESSAGES)
}

/** 이름, 여행 날짜, 공개 여부를 한 번에 바꾼다(서버는 셋을 모두 받는다) */
export function updateCourse(
  course: Pick<CourseDetail, 'id' | 'title' | 'ldongRegnCd' | 'travelDate' | 'publicCourse'>,
): Promise<CourseDetail> {
  return request<CourseDetail>(
    `/api/courses/${course.id}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: course.title,
        ldongRegnCd: course.ldongRegnCd,
        travelDate: course.travelDate,
        publicCourse: course.publicCourse,
      }),
    },
    { ...COURSE_MESSAGES, 400: '코스 이름을 1~100자로 적어 주세요.' },
  )
}

/** 코스에서 장소 빼기. 서버가 그 일차의 남은 순서를 1부터 다시 매긴다 */
export function removeCoursePlace(courseId: number, coursePlaceId: number): Promise<void> {
  return request<void>(`/api/courses/${courseId}/places/${coursePlaceId}`, { method: 'DELETE' }, COURSE_MESSAGES)
}

/** 방문 순서 바꾸기. 바뀌는 항목만 보낸다 */
export function reorderCourse(
  courseId: number,
  items: { coursePlaceId: number; dayNo: number; visitOrder: number }[],
): Promise<CourseDetail> {
  return request<CourseDetail>(
    `/api/courses/${courseId}/places/order`,
    { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ items }) },
    COURSE_MESSAGES,
  )
}

/** 공유 링크로 코스 보기(로그인 불필요). 없는 링크면 404 */
export function fetchSharedCourse(shareCode: string): Promise<CourseDetail> {
  return request<CourseDetail>(`/api/courses/shared/${encodeURIComponent(shareCode)}`, undefined, {
    404: '코스를 찾을 수 없어요. 링크가 맞는지, 코스가 지워지지 않았는지 확인해 주세요.',
  })
}

/** 공유받은 코스를 내 코스로 저장(복사본을 만든다). 로그인 필요 */
export function copySharedCourse(shareCode: string): Promise<CourseDetail> {
  return request<CourseDetail>(
    `/api/courses/shared/${encodeURIComponent(shareCode)}/copy`,
    { method: 'POST' },
    { 404: '코스를 찾을 수 없어요.' },
  )
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

/** 카카오 로그인 뒤 돌아올 화면 주소를 잠깐 저장해 두는 키(sessionStorage). */
export const RETURN_TO_KEY = 'galgot.returnTo'
